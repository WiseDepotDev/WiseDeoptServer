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
 *   node tools/gen/swagger-drift.js --json      # 输出 JSON，便于 CI 消费
 */

const fs = require('fs');
const path = require('path');

const SERVER_ROOT = path.resolve(__dirname, '..', '..');
const SWAGGER_FILE = path.join(SERVER_ROOT, 'wise-depot-plan', '默认模块.swagger.json');
const CONTROLLER_DIR = path.join(SERVER_ROOT, 'wise-deopt-api', 'src', 'main', 'java', 'com', 'huicang', 'wise', 'api', 'controller');

const jsonMode = process.argv.includes('--json');

/**
 * 读取 swagger 文件中的路由集合。
 *
 * @returns {Set<string>} 形如 "get /api/users" 的集合
 */
function loadSwaggerRoutes() {
    const raw = fs.readFileSync(SWAGGER_FILE, 'utf8');
    const spec = JSON.parse(raw);
    const routes = new Set();
    const paths = spec.paths || {};
    for (const p of Object.keys(paths)) {
        for (const method of Object.keys(paths[p])) {
            if (['get', 'post', 'put', 'patch', 'delete'].includes(method.toLowerCase())) {
                routes.add(`${method.toLowerCase()} ${p}`);
            }
        }
    }
    return { routes, spec };
}

/**
 * 从控制器源码提取路由声明。
 *
 * @returns {Set<string>} 形如 "get /api/users/{id}" 的集合
 */
function loadSourceRoutes() {
    const routes = new Set();
    const files = fs.readdirSync(CONTROLLER_DIR).filter((f) => f.endsWith('.java'));

    for (const file of files) {
        const text = fs.readFileSync(path.join(CONTROLLER_DIR, file), 'utf8');
        const classMapping = /@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/.exec(text);
        const base = classMapping ? classMapping[1].replace(/\/$/, '') : '';

        const re = /@(Get|Post|Put|Patch|Delete)Mapping\s*\(([^)]*)\)/g;
        let m;
        while ((m = re.exec(text)) !== null) {
            const method = m[1].toLowerCase();
            let sub = '';
            const valueMatch = /(?:value\s*=\s*)?"([^"]*)"/.exec(m[2]);
            if (valueMatch) {
                sub = valueMatch[1];
            } else {
                const listMatch = /\{\s*"([^"]*)"/.exec(m[2]);
                if (listMatch) {
                    sub = listMatch[1];
                }
            }
            let full = `${base}${sub}` || base;
            if (!full.startsWith('/')) {
                full = `/${full}`;
            }
            // 统一路径变量写法：Spring 的 {id:regex} 归一为 {id}
            full = full.replace(/\{([^}:]+)[^}]*\}/g, '{$1}');
            routes.add(`${method} ${full}`);
        }
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
            swaggerFile: path.relative(SERVER_ROOT, SWAGGER_FILE),
            swaggerVersion: spec.info ? spec.info.version : null,
            swaggerPaths: swaggerRoutes.size,
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
