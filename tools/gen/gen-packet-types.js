#!/usr/bin/env node
/**
 * gen-packet-types.js —— 从服务端控制器生成 APP 侧「请求 packet_type 映射」（P3-20）。
 *
 * 为什么需要生成（STD-CONTRACT-03 契约单一来源）：
 *   `packet_type` 的取值来自服务端 `PacketType` 枚举，而"哪个接口用哪个类型"这件事
 *   **只有服务端知道**——答案是控制器方法上的 `@ApiPacketType(PacketType.X)` 注解。
 *   让 APP 手抄 160 条映射必然漂移，因此由本脚本读源码生成 Kotlin 表。
 *
 * 已按方法粒度对齐注解：取「上一个方法注解之后、本方法注解之前」这段文本里最近的
 *   `@ApiPacketType(...)`；没有该注解的方法（如 /api/auth/refresh-token）按 schema 约定
 *   记为 `UNKNOWN`（"未知类型固定为 UNKNOWN"）。
 *
 * 用法：
 *   node tools/gen/gen-packet-types.js            # 生成/覆盖 APP 侧 PacketTypeMap.kt
 *   node tools/gen/gen-packet-types.js --check    # 只校验是否与源码一致（差异即退出 1）
 *   node tools/gen/gen-packet-types.js --print    # 打印到 stdout，不写文件
 */

'use strict';

const fs = require('fs');
const path = require('path');

const SERVER_ROOT = path.resolve(__dirname, '..', '..');
const CONTROLLER_DIR = path.join(
    SERVER_ROOT, 'wise-deopt-api', 'src', 'main', 'java', 'com', 'huicang', 'wise', 'api', 'controller');

const WORKSPACE_ROOT = path.resolve(SERVER_ROOT, '..');
const OUT_FILE = path.join(
    WORKSPACE_ROOT,
    'WiseDepotApp', 'wise-depot-android-refactor', 'app', 'src', 'main', 'java',
    'com', 'huicang', 'wise', 'network', 'PacketTypeMap.kt');

const MODE = process.argv.includes('--check') ? 'check'
    : process.argv.includes('--print') ? 'print'
        : 'write';

/** 会被误当成路径的注解属性名 */
const NON_PATH_ATTR = /^(params|produces|consumes|headers|name)$/;

/** 从注解括号内容里取路径（与 swagger-drift.js 同一套规则，见该文件注释） */
function extractPaths(argText) {
    const text = (argText || '').trim();
    if (!text) {
        return [];
    }
    const attr = /(?:^|[,(\s])(?:value|path)\s*=\s*(\{[^}]*\}|"[^"]*")/.exec(text);
    if (attr) {
        return stringsIn(attr[1]);
    }
    const firstAttr = text.search(/[A-Za-z_$][\w$]*\s*=/);
    const head = firstAttr >= 0 ? text.slice(0, firstAttr) : text;
    if (/^\s*(\{|")/.test(head)) {
        return stringsIn(head);
    }
    if (NON_PATH_ATTR.test(text.split('=')[0].trim())) {
        return [];
    }
    return [];
}

function stringsIn(fragment) {
    const out = [];
    const re = /"([^"]*)"/g;
    let m;
    while ((m = re.exec(fragment)) !== null) {
        out.push(m[1]);
    }
    return out;
}

/** `{id:\\d+}` → `{id}`；去尾斜杠 */
function normalizePath(p) {
    let out = (p || '').replace(/\{([^}:]+)[^}]*\}/g, '{$1}');
    if (out.length > 1 && out.endsWith('/')) {
        out = out.slice(0, -1);
    }
    return out;
}

/** 拼接类级基路径与方法级子路径 */
function joinPath(base, sub) {
    const b = base === '/' ? '' : base;
    const s = sub === '/' ? '' : sub;
    const joined = `${b}${s}`;
    const out = joined.startsWith('/') ? joined : `/${joined}`;
    return normalizePath(out);
}

/** 扫描控制器目录，产出 [{method, path, packetType}] */
function collectRoutes() {
    const routes = [];
    const files = fs.readdirSync(CONTROLLER_DIR).filter((f) => f.endsWith('.java')).sort();

    for (const file of files) {
        const text = fs.readFileSync(path.join(CONTROLLER_DIR, file), 'utf8');
        const classMapping = /@RequestMapping\s*\(([\s\S]*?)\)/.exec(text);
        const bases = classMapping ? extractPaths(classMapping[1]) : [];
        const baseList = bases.length > 0 ? bases : [''];

        const re = /@(Get|Post|Put|Patch|Delete)Mapping\s*(?:\(([\s\S]*?)\))?/g;
        let prevEnd = 0;
        let m;
        while ((m = re.exec(text)) !== null) {
            const declSegment = text.slice(prevEnd, m.index);
            prevEnd = m.index + m[0].length;

            const pkt = /@ApiPacketType\s*\(\s*PacketType\.([A-Z0-9_]+)\s*\)/.exec(declSegment);
            const packetType = pkt ? pkt[1] : 'UNKNOWN';

            // 同一 method+path 可能有多条（如 GET /api/alerts 用 params=alertLevel 细分）：
            // 带 params/headers/consumes/produces 条件的排在后面，resolve() 命中"无条件"那条。
            const conditional = /(?:^|[,(\s])(params|headers|consumes|produces)\s*=/.test(m[2] || '');

            const method = m[1].toUpperCase();
            const subs = m[2] === undefined ? [] : extractPaths(m[2]);
            const subList = subs.length > 0 ? subs : [''];

            for (const base of baseList) {
                for (const sub of subList) {
                    routes.push({ method, path: joinPath(base, sub), packetType, conditional });
                }
            }
        }
    }

    routes.sort((a, b) => {
        if (a.path !== b.path) return a.path.localeCompare(b.path);
        if (a.method !== b.method) return a.method.localeCompare(b.method);
        if (a.conditional !== b.conditional) return a.conditional ? 1 : -1;
        return a.packetType.localeCompare(b.packetType);
    });
    return routes;
}

/** 生成 Kotlin 文件内容 */
function render(routes) {
    const lines = [];
    lines.push('// AUTO-GENERATED FROM WiseDeoptServer/wise-deopt-api/**/controller/*.java — DO NOT EDIT');
    lines.push('// 生成器：WiseDeoptServer/tools/gen/gen-packet-types.js（--check 只校验不写入）');
    lines.push('// 规则：方法级 @ApiPacketType(PacketType.X) 为该接口的请求 packet_type；');
    lines.push('//      没有该注解的接口按 schema 约定记 UNKNOWN（未知类型固定为 UNKNOWN）；');
    lines.push('//      同一 method+path 有多条时，带 params/headers 条件的排在后面，resolve 命中无条件那条。');
    lines.push('package com.huicang.wise.network');
    lines.push('');
    lines.push('/**');
    lines.push(' * 请求 packet_type 映射表（STD-CONTRACT-01 / STD-CONTRACT-03）。');
    lines.push(' *');
    lines.push(' * 由服务端控制器的 `@ApiPacketType` 注解生成，**不要手改**：改接口注解后重跑生成器。');
    lines.push(' * 路径模板里的 `{...}` 段匹配任意一段（如 `{deviceId}`）。');
    lines.push(' */');
    lines.push('object PacketTypeMap {');
    lines.push('    /** 未登记路径的类型（schema：未知类型固定为 UNKNOWN）。 */');
    lines.push('    const val UNKNOWN: String = "UNKNOWN"');
    lines.push('');
    lines.push('    /** 单条映射：`method` 大写 + `template` 路径模板 + `packetType`。 */');
    lines.push('    data class Entry(');
    lines.push('        val method: String,');
    lines.push('        val template: String,');
    lines.push('        val packetType: String,');
    lines.push('    )');
    lines.push('');
    if (routes.length === 0) {
        lines.push('    val entries: List<Entry> = emptyList()');
    } else {
        lines.push('    val entries: List<Entry> =');
        lines.push('        listOf(');
        for (const r of routes) {
            lines.push(`            Entry("${r.method}", "${r.path}", packetType = "${r.packetType}"),`);
        }
        lines.push('        )');
    }
    lines.push('');
    lines.push('    /**');
    lines.push('     * 解析请求对应的 packet_type。');
    lines.push('     *');
    lines.push('     * @param method HTTP 方法（大小写不敏感）');
    lines.push('     * @param urlPath 请求路径（可带查询串与前导斜杠）');
    lines.push('     * @return 命中的 packet_type；未命中返回 [UNKNOWN]');
    lines.push('     */');
    lines.push('    fun resolve(');
    lines.push('        method: String,');
    lines.push('        urlPath: String,');
    lines.push('    ): String {');
    lines.push('        val want = method.uppercase()');
    lines.push('        val actual =');
    lines.push('            urlPath');
    lines.push("                .substringBefore('?')");
    lines.push("                .trimEnd('/')");
    lines.push("                .trimStart('/')");
    lines.push("                .split('/')");
    lines.push('        return entries');
    lines.push('            .firstOrNull { it.method == want && matches(it.template, actual) }');
    lines.push('            ?.packetType');
    lines.push('            ?: UNKNOWN');
    lines.push('    }');
    lines.push('');
    lines.push('    /** `{...}` 段匹配任意一段，其余段逐字相等且段数一致。 */');
    lines.push('    private fun matches(');
    lines.push('        template: String,');
    lines.push('        actual: List<String>,');
    lines.push('    ): Boolean {');
    lines.push("        val segments = template.trimStart('/').split('/')");
    lines.push('        if (segments.size != actual.size) {');
    lines.push('            return false');
    lines.push('        }');
    lines.push('        return segments');
    lines.push('            .withIndex()');
    lines.push('            .all { (index, segment) -> isVariable(segment) || segment == actual[index] }');
    lines.push('    }');
    lines.push('');
    lines.push('    private fun isVariable(segment: String): Boolean = segment.startsWith("{") && segment.endsWith("}")');
    lines.push('}');
    lines.push('');
    return lines.join('\n');
}

const routes = collectRoutes();
const mapped = routes.filter((r) => r.packetType !== 'UNKNOWN').length;
const content = render(routes);

if (MODE === 'print') {
    process.stdout.write(content);
    process.exit(0);
}

if (MODE === 'check') {
    if (!fs.existsSync(OUT_FILE)) {
        console.error(`gen-packet-types: 缺少生成物 ${OUT_FILE}，请先运行生成器。`);
        process.exit(1);
    }
    const current = fs.readFileSync(OUT_FILE, 'utf8');
    if (current.replace(/\r\n/g, '\n') !== content) {
        console.error('gen-packet-types: 生成物与控制器注解不一致（契约漂移）。');
        console.error('处置：重跑 `node WiseDeoptServer/tools/gen/gen-packet-types.js` 并提交生成物。');
        process.exit(1);
    }
    console.log(
        `gen-packet-types OK: ${routes.length} 条路由（有注解 ${mapped} / UNKNOWN ${routes.length - mapped}）与生成物一致`);
    process.exit(0);
}

fs.writeFileSync(OUT_FILE, content, 'utf8');
console.log(`gen-packet-types: 已写入 ${path.relative(WORKSPACE_ROOT, OUT_FILE).replace(/\\/g, '/')}`);
console.log(`  路由 ${routes.length} 条：有 @ApiPacketType 注解 ${mapped} 条 / UNKNOWN ${routes.length - mapped} 条`);
