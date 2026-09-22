#!/usr/bin/env node
/**
 * openapi-schema-diff.js —— 两份 OpenAPI 3 规格的**字段级**差异量化（P1-08 的 schema 级检查）
 *
 * 与 `swagger-drift.js`（代码注解 vs 规格，路由级）互补：本工具比较**两份规格文件**，
 * 用于回答"仓库里的契约快照与运行中的服务导出的 /v3/api-docs 是否一致"。
 *
 * 比较维度：
 *   1. 路由集合（method + path）；
 *   2. 每个操作的参数（name/in/required）；
 *   3. 请求体 schema 名（$ref 末段）；
 *   4. 各状态码的响应 schema 名；
 *   5. components.schemas 的名字集合与每个 schema 的**属性名集合**。
 *
 * 明确不比较：描述文本、示例、顺序、非 schema 的扩展字段（这些属于文档噪声，不构成契约差异）。
 *
 * 用法：
 *   node tools/gen/openapi-schema-diff.js --a <spec1.json> --b <spec2.json>
 *   node tools/gen/openapi-schema-diff.js --a a.json --b b.json --json
 * 退出码：0 = 无差异；1 = 有差异；2 = 运行环境错误。
 */

const fs = require('fs');

const args = process.argv.slice(2);
const jsonMode = args.includes('--json');
function argOf(name) {
    const i = args.indexOf(name);
    return i >= 0 ? args[i + 1] : null;
}
const fileA = argOf('--a');
const fileB = argOf('--b');

if (!fileA || !fileB) {
    console.error('用法: node tools/gen/openapi-schema-diff.js --a <spec1.json> --b <spec2.json> [--json]');
    process.exit(2);
}
if (!fs.existsSync(fileA) || !fs.existsSync(fileB)) {
    console.error(`[schema-diff] 规格文件不存在: ${!fs.existsSync(fileA) ? fileA : fileB}`);
    process.exit(2);
}

const specA = JSON.parse(fs.readFileSync(fileA, 'utf8'));
const specB = JSON.parse(fs.readFileSync(fileB, 'utf8'));

const METHODS = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options'];

/** $ref 末段名（"#/components/schemas/AlertDTO" → "AlertDTO"）；非 $ref 时返回 JSON 形态摘要 */
function schemaName(schema) {
    if (!schema) {
        return null;
    }
    if (schema.$ref) {
        return schema.$ref.split('/').pop();
    }
    if (schema.type === 'array' && schema.items) {
        return `array<${schemaName(schema.items) || '?'}>`;
    }
    return schema.type || 'inline';
}

/** 操作签名：参数 + 请求体 + 响应 schema（用于字段级比较） */
function operationSignature(op) {
    const params = (op.parameters || [])
        .map((p) => `${p.in}:${p.name}${p.required ? '!' : ''}`)
        .sort()
        .join(',');
    const bodyName = schemaName(op.requestBody && op.requestBody.content
        ? (op.requestBody.content['application/json'] || {}).schema
        : null);
    const responses = Object.keys(op.responses || {}).sort().map((code) => {
        const content = (op.responses[code].content || {})['application/json'];
        return `${code}=${schemaName(content ? content.schema : null) || 'none'}`;
    }).join(',');
    return { params, body: bodyName || 'none', responses };
}

/** 收集 路由 → 签名 */
function routesOf(spec) {
    const out = new Map();
    for (const [p, item] of Object.entries(spec.paths || {})) {
        for (const m of Object.keys(item)) {
            if (!METHODS.includes(m.toLowerCase())) {
                continue;
            }
            out.set(`${m.toLowerCase()} ${p}`, operationSignature(item[m]));
        }
    }
    return out;
}

/** 收集 schema → 属性名集合 */
function schemasOf(spec) {
    const out = new Map();
    const schemas = (spec.components && spec.components.schemas) || {};
    for (const [name, s] of Object.entries(schemas)) {
        const props = Object.keys(s.properties || {}).sort();
        out.set(name, props);
    }
    return out;
}

const rA = routesOf(specA);
const rB = routesOf(specB);
const sA = schemasOf(specA);
const sB = schemasOf(specB);

const onlyA = [...rA.keys()].filter((k) => !rB.has(k)).sort();
const onlyB = [...rB.keys()].filter((k) => !rA.has(k)).sort();

const opDiffs = [];
for (const key of [...rA.keys()].filter((k) => rB.has(k)).sort()) {
    const a = rA.get(key);
    const b = rB.get(key);
    if (a.params !== b.params || a.body !== b.body || a.responses !== b.responses) {
        opDiffs.push({ route: key, a, b });
    }
}

const schemaOnlyA = [...sA.keys()].filter((k) => !sB.has(k)).sort();
const schemaOnlyB = [...sB.keys()].filter((k) => !sA.has(k)).sort();
const propDiffs = [];
for (const name of [...sA.keys()].filter((k) => sB.has(k)).sort()) {
    const pa = sA.get(name);
    const pb = sB.get(name);
    const missing = pa.filter((x) => !pb.includes(x));
    const extra = pb.filter((x) => !pa.includes(x));
    if (missing.length || extra.length) {
        propDiffs.push({ schema: name, onlyInA: missing, onlyInB: extra });
    }
}

const total = onlyA.length + onlyB.length + opDiffs.length + schemaOnlyA.length + schemaOnlyB.length + propDiffs.length;

if (jsonMode) {
    console.log(JSON.stringify({
        specA: fileA,
        specB: fileB,
        routesA: rA.size,
        routesB: rB.size,
        schemasA: sA.size,
        schemasB: sB.size,
        onlyInA, onlyInB, operationDiffs: opDiffs, schemasOnlyInA: schemaOnlyA,
        schemasOnlyInB: schemaOnlyB, propertyDiffs: propDiffs, differenceCount: total
    }, null, 2));
    process.exit(total === 0 ? 0 : 1);
}

console.log(`[schema-diff] A=${fileA} (${rA.size} 路由 / ${sA.size} schema)`);
console.log(`[schema-diff] B=${fileB} (${rB.size} 路由 / ${sB.size} schema)`);
if (onlyA.length) {
    console.log(`[schema-diff] 仅在 A 的路由: ${onlyA.length}`);
    onlyA.forEach((r) => console.log(`    - ${r}`));
}
if (onlyB.length) {
    console.log(`[schema-diff] 仅在 B 的路由: ${onlyB.length}`);
    onlyB.forEach((r) => console.log(`    + ${r}`));
}
if (opDiffs.length) {
    console.log(`[schema-diff] 参数/请求体/响应不一致的操作: ${opDiffs.length}`);
    opDiffs.forEach((d) => {
        console.log(`    ~ ${d.route}`);
        if (d.a.params !== d.b.params) console.log(`        params : A[${d.a.params}] vs B[${d.b.params}]`);
        if (d.a.body !== d.b.body) console.log(`        body   : A[${d.a.body}] vs B[${d.b.body}]`);
        if (d.a.responses !== d.b.responses) console.log(`        resp   : A[${d.a.responses}] vs B[${d.b.responses}]`);
    });
}
if (schemaOnlyA.length) {
    console.log(`[schema-diff] 仅在 A 的 schema: ${schemaOnlyA.join(', ')}`);
}
if (schemaOnlyB.length) {
    console.log(`[schema-diff] 仅在 B 的 schema: ${schemaOnlyB.join(', ')}`);
}
if (propDiffs.length) {
    console.log(`[schema-diff] 属性名不一致的 schema: ${propDiffs.length}`);
    propDiffs.forEach((d) => {
        if (d.onlyInA.length) console.log(`    ~ ${d.schema} 仅 A 有: ${d.onlyInA.join(', ')}`);
        if (d.onlyInB.length) console.log(`    ~ ${d.schema} 仅 B 有: ${d.onlyInB.join(', ')}`);
    });
}

if (total === 0) {
    console.log('[schema-diff] 字段级一致（路由/参数/请求体/响应/属性名）。');
    process.exit(0);
}
console.log(`[schema-diff] 共 ${total} 处差异。`);
process.exit(1);
