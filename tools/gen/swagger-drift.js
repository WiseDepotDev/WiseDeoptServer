#!/usr/bin/env node
/**
 * swagger-drift.js —— OpenAPI 契约与实现的偏差量化（P1-08 的离线检查部分）
 *
 * 用途：在不启动服务的前提下，比较 `wise-depot-plan/默认模块.swagger.json` 中登记的路由
 *       与 `wise-deopt-api` 控制器源码中实际声明的路由，输出差异清单。
 *
 * 说明：本工具只做「路由级」比对，不校验字段/schema 级差异；schema 级一致性需要在环境就绪后
 *       启动服务导出 /v3/api-docs 再与文件 diff（见 docs/standards/基线记录.md P1-08）。
 *
 * 用法：
 *   node tools/gen/swagger-drift.js
 *   node tools/gen/swagger-drift.js --json          # 输出 JSON，便于 CI 消费
 *   node tools/gen/swagger-drift.js --spec <file>   # 指定规格文件（默认 wise-depot-plan/默认模块.swagger.json）
 *
 * 2026-02-27 修正（P1-08 复核）——原实现有两处会把**非路径字符串**当成路径，制造大量假差异
 * （实测假差异：仅存在于 swagger 102 条 / 仅存在于源码 32 条）：
 *   1. 方法级注解里没有路径、只有 `params = "alertLevel"` 这类属性时，原正则把属性**值**当路径
 *      → 产出 `get /alertLevel` 这种不存在的路由；
 *   2. 类级 `@RequestMapping({"/api/alerts", "/alert"})` 的多基路径没有做笛卡尔积，
 *      且方法级多路径数组只取第一个。
 * 现在只从 `value=` / `path=` / 裸字符串（含数组）提取路径，忽略 params/produces/consumes/headers。
 */

const fs = require('fs');
const path = require('path');

const SERVER_ROOT = path.resolve(__dirname, '..', '..');
const CONTROLLER_DIR = path.join(SERVER_ROOT, 'wise-deopt-api', 'src', 'main', 'java', 'com', 'huicang', 'wise', 'api', 'controller');

const jsonMode = process.argv.includes('--json');
const specArgIdx = process.argv.indexOf('--spec');
const SWAGGER_FILE = specArgIdx >= 0 && process.argv[specArgIdx + 1]
    ? path.resolve(process.argv[specArgIdx + 1])
    : path.join(SERVER_ROOT, 'wise-depot-plan', '默认模块.swagger.json');

/** 会被误当成路径的注解属性名（这些属性里的字符串不是 URL） */
const NON_PATH_ATTR = /^(params|produces|consumes|headers|name)$/;

/**
 * 从一个注解的括号内容里提取路径（可能是 0 条、1 条或多条）。
 *
 * @param {string} argText 注解括号内的原文
 * @returns {string[]} 路径列表（无路径时返回空数组）
 */
function extractPaths(argText) {
    const text = argText.trim();
    if (!text) {
        return [];
    }

    // 形式一：value = "..." / path = "..."（可带其他属性）
    const attr = /(?:^|[,(\s])(?:value|path)\s*=\s*(\{[^}]*\}|"[^"]*")/.exec(text);
    if (attr) {
        return stringsIn(attr[1]);
    }

    // 形式二：裸字符串或裸数组，且必须出现在**属性赋值之前**（否则就是 params= 之类）
    const firstAttr = text.search(/[A-Za-z_$][\w$]*\s*=/);
    const head = firstAttr >= 0 ? text.slice(0, firstAttr) : text;
    if (/^\s*(\{|")/.test(head)) {
        return stringsIn(head);
    }
    return [];
}

/** 从 `"x"` 或 `{"x","y"}` 里取出全部字符串 */
function stringsIn(fragment) {
    const out = [];
    const re = /"([^"]*)"/g;
    let m;
    while ((m = re.exec(fragment)) !== null) {
        out.push(m[1]);
    }
    return out;
}

/** 归一化路径：Spring 的 {id:regex} → {id}，并去掉结尾多余的 / */
function normalizePath(p) {
    let out = (p || '').replace(/\{([^}:]+)[^}]*\}/g, '{$1}');
    if (out.length > 1 && out.endsWith('/')) {
        out = out.slice(0, -1);
    }
    return out;
}

/**
 * 读取 swagger 文件中的路由集合。
 *
 * @returns {{routes: Set<string>, spec: object}} 形如 "get /api/users" 的集合
 */
function loadSwaggerRoutes() {
    const raw = fs.readFileSync(SWAGGER_FILE, 'utf8');
    const spec = JSON.parse(raw);
    const routes = new Set();
    const paths = spec.paths || {};
    for (const p of Object.keys(paths)) {
        for (const method of Object.keys(paths[p])) {
            if (['get', 'post', 'put', 'patch', 'delete'].includes(method.toLowerCase())) {
                routes.add(`${method.toLowerCase()} ${normalizePath(p)}`);
            }
        }
    }
    return { routes, spec };
}

/**
 * 从控制器源码提取路由声明（类级基路径 × 方法级子路径）。
 *
 * @returns {Set<string>} 形如 "get /api/users/{id}" 的集合
 */
function loadSourceRoutes() {
    const routes = new Set();
    const files = fs.readdirSync(CONTROLLER_DIR).filter((f) => f.endsWith('.java'));

    for (const file of files) {
        const text = fs.readFileSync(path.join(CONTROLLER_DIR, file), 'utf8');

        const classMapping = /@RequestMapping\s*\(([\s\S]*?)\)/.exec(text);
        const bases = classMapping ? extractPaths(classMapping[1]) : [];
        const baseList = bases.length > 0 ? bases : [''];

        const re = /@(Get|Post|Put|Patch|Delete)Mapping\s*(?:\(([\s\S]*?)\))?/g;
        let m;
        while ((m = re.exec(text)) !== null) {
            const method = m[1].toLowerCase();
            const subs = m[2] === undefined ? [] : extractPaths(m[2]);
            const subList = subs.length > 0 ? subs : [''];

            for (const base of baseList) {
                for (const sub of subList) {
                    let full = `${base}${sub}`;
                    if (!full.startsWith('/')) {
                        full = `/${full}`;
                    }
                    routes.add(`${method} ${normalizePath(full)}`);
                }
            }
        }
        void NON_PATH_ATTR;
    }
    return routes;
}

function main() {
    if (!fs.existsSync(SWAGGER_FILE)) {
        console.error(`[drift] 缺少 swagger 文件: ${SWAGGER_FILE}`);
        process.exit(2);
    }
    if (!fs.existsSync(CONTROLLER_DIR)) {
        console.error(`[drift] 缺少控制器目录: ${CONTROLLER_DIR}`);
        process.exit(2);
    }

    const { routes: swaggerRoutes, spec } = loadSwaggerRoutes();
    const sourceRoutes = loadSourceRoutes();

    const onlyInSwagger = [...swaggerRoutes].filter((r) => !sourceRoutes.has(r)).sort();
    const onlyInSource = [...sourceRoutes].filter((r) => !swaggerRoutes.has(r)).sort();
    const both = [...sourceRoutes].filter((r) => swaggerRoutes.has(r)).sort();

    if (jsonMode) {
        console.log(JSON.stringify({
            specFile: path.relative(SERVER_ROOT, SWAGGER_FILE),
            specVersion: spec.info ? spec.info.version : null,
            swaggerRoutes: swaggerRoutes.size,
            sourceRoutes: sourceRoutes.size,
            matched: both.length,
            onlyInSwagger,
            onlyInSource
        }, null, 2));
        return;
    }

    console.log(`[drift] swagger: ${path.relative(SERVER_ROOT, SWAGGER_FILE)} (version=${spec.info ? spec.info.version : 'n/a'})`);
    console.log(`[drift] swagger 路由 ${swaggerRoutes.size} 条 / 源码路由 ${sourceRoutes.size} 条 / 匹配 ${both.length} 条`);
    console.log(`[drift] 仅存在于 swagger（实现已删除或改名）: ${onlyInSwagger.length} 条`);
    onlyInSwagger.forEach((r) => console.log(`    - ${r}`));
    console.log(`[drift] 仅存在于源码（swagger 未登记）: ${onlyInSource.length} 条`);
    onlyInSource.forEach((r) => console.log(`    + ${r}`));

    if (onlyInSwagger.length === 0 && onlyInSource.length === 0) {
        console.log('[drift] 路由级一致。');
        process.exit(0);
    }
    // 路由不一致视为需要重新导出 swagger（P1-08）
    process.exit(1);
}

main();
