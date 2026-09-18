#!/usr/bin/env node
/**
 * controller-audit.js —— 服务端控制器体检（P2-08）
 *
 * 目的：把「控制器只做参数适配与转发」这条规范变成可复现的机械检查，
 *       输出可执行的工作清单，而不是靠人逐个翻 26 个控制器。
 *
 * 检查项（与 STD-ARCH-02 / STD-CONTRACT-04 / STD-NAME-02 对应）：
 *   A. 入参校验：@RequestBody 形参是否带 @Valid
 *   B. 接口文档：类/方法是否有 Swagger 注解（@Tag / @Operation）
 *   C. 路径语义：类级 @RequestMapping 是否为名词复数、是否含动词
 *   D. 业务逻辑下渗：方法体内是否出现仓储访问、循环、复杂分支（启发式）
 *   E. 返回实体：方法返回泛型中是否直接出现领域实体（与 ArchUnit 规则互补）
 *
 * 用法：
 *   node tools/audit/controller-audit.js             # 文本报告
 *   node tools/audit/controller-audit.js --json      # JSON 输出（便于 CI/任务分配）
 *   node tools/audit/controller-audit.js --strict    # 存在 A/B/C 类问题则退出码 1
 */

const fs = require('fs');
const path = require('path');

const SERVER_ROOT = path.resolve(__dirname, '..', '..');
const CONTROLLER_DIR = path.join(
    SERVER_ROOT, 'wise-deopt-api', 'src', 'main', 'java', 'com', 'huicang', 'wise', 'api', 'controller');

const jsonMode = process.argv.includes('--json');
const strictMode = process.argv.includes('--strict');

/** 路径中常见的动词，出现在 REST 路径里通常意味着设计问题 */
const VERB_PATTERN = /\b(get|create|update|delete|add|remove|save|query|search|list|find|do|exec|process)\b/i;
/** 允许的单数/不可数路径段（白名单，避免误报） */
const ALLOWED_SINGULAR = new Set(['api', 'auth', 'profile', 'dashboard', 'oss', 'minio', 'health', 'i18n', 'sync', 'captcha', 'search']);
/** 允许出现动词的路径段（这些是语义化的业务名词，不是动作） */
const VERB_WHITELIST = new Set(['files', 'inventory', 'stock-orders', 'in-out']);

/**
 * 读取控制器源码并做机械检查。
 *
 * @returns {Array<object>} 每个控制器的检查结果
 */
function auditControllers() {
    const results = [];
    const files = fs.readdirSync(CONTROLLER_DIR).filter((f) => f.endsWith('Controller.java'));

    for (const file of files) {
        const text = fs.readFileSync(path.join(CONTROLLER_DIR, file), 'utf8');
        const classNameMatch = /public class (\w+)/.exec(text);
        const className = classNameMatch ? classNameMatch[1] : file.replace('.java', '');

        const issues = [];

        // C. 类级路径
        const classMapping = /@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"/.exec(text);
        const basePath = classMapping ? classMapping[1] : null;
        if (!basePath) {
            issues.push({ code: 'C1', severity: 'advisory', message: '缺少类级 @RequestMapping 路径（方法级全路径亦合法；改名会破坏三端契约，默认不动）' });
        } else {
            const segments = basePath.split('/').filter(Boolean);
            for (const seg of segments) {
                if (seg === 'api' || seg.startsWith('{')) continue;
                if (VERB_PATTERN.test(seg) && !VERB_WHITELIST.has(seg)) {
                    issues.push({ code: 'C2', message: `路径段含动词: /${seg}` });
                }
                const isPlural = seg.endsWith('s') || seg.includes('-');
                if (!isPlural && !ALLOWED_SINGULAR.has(seg) && !seg.startsWith('{')) {
                    issues.push({ code: 'C3', message: `路径段疑似单数（REST 建议名词复数）: /${seg}` });
                }
            }
        }

        // B. Swagger 注解
        const hasTag = /@Tag\s*\(/.test(text);
        if (!hasTag) {
            issues.push({ code: 'B1', message: '类缺少 @Tag（Swagger 分组）' });
        }
        const mappings = [...text.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping/g)];
        const operations = [...text.matchAll(/@Operation\s*\(/g)];
        if (mappings.length > 0 && operations.length < mappings.length) {
            issues.push({
                code: 'B2',
                message: `@Operation 数量不足：端点 ${mappings.length} 个，@Operation ${operations.length} 个`
            });
        }

        // A. @Valid —— 注意 @Valid 写在 @RequestBody 之前，必须同时看前面
        const bodyMatches = [...text.matchAll(/@RequestBody/g)];
        const invalidBody = [];
        for (const m of bodyMatches) {
            // 取 @RequestBody 前 80 个字符作为注解窗口
            const window = text.slice(Math.max(0, m.index - 80), m.index + 80);
            if (!/@Valid/.test(window)) {
                invalidBody.push(m.index);
            }
        }
        if (invalidBody.length > 0) {
            issues.push({ code: 'A1', message: `@RequestBody 缺少 @Valid：${invalidBody.length} 处` });
        }

        // E. 返回领域实体（与 ArchUnit 规则互补，这里看方法签名文本）
        const entityReturns = [...text.matchAll(/ApiResponse<(?:List<)?(UserCore|Product|Inventory|DeviceCore|StockOrder|AlertEvent|InspectionTask|ProductTag|MinioFile|Warehouse|Message|ReportTask|NfcBadge|Permission|Role)>/g)];
        if (entityReturns.length > 0) {
            issues.push({ code: 'E1', message: `返回领域实体：${entityReturns.length} 处（应向应用服务/DTO 收敛）` });
        }

        // D. 业务逻辑启发式：统计方法体内的控制流（粗略但足够定位）
        const bodyMatch = text.split(/@(?:Get|Post|Put|Patch|Delete)Mapping[^)]*\)/).slice(1);
        let controlFlow = 0;
        let repositoryCalls = 0;
        for (const body of bodyMatch) {
            controlFlow += (body.match(/\b(for|while|switch)\s*\(/g) || []).length;
            controlFlow += (body.match(/\bif\s*\(/g) || []).length;
            repositoryCalls += (body.match(/Repository\.\w+\(/g) || []).length;
        }
        if (repositoryCalls > 0) {
            issues.push({ code: 'D1', message: `方法体内直接调用仓储：${repositoryCalls} 处` });
        }
        if (controlFlow > 6) {
            issues.push({ code: 'D2', message: `方法体内控制流较多（${controlFlow} 处 if/for/while/switch），疑似业务逻辑下移不足` });
        }

        results.push({ file, className, basePath, endpoints: mappings.length, issues });
    }
    return results;
}

function main() {
    if (!fs.existsSync(CONTROLLER_DIR)) {
        console.error(`[audit] 缺少控制器目录: ${CONTROLLER_DIR}`);
        process.exit(2);
    }
    const results = auditControllers();
    const withIssues = results.filter((r) => r.issues.length > 0);
    const blocking = results.filter((r) => r.issues.some((i) => i.severity !== 'advisory' && ['A1', 'B1', 'B2', 'C1', 'C2'].includes(i.code)));

    if (jsonMode) {
        console.log(JSON.stringify({
            controllers: results.length,
            withIssues: withIssues.length,
            blocking: blocking.length,
            results
        }, null, 2));
    } else {
        console.log(`[audit] 控制器总数: ${results.length}，存在问题: ${withIssues.length}`);
        for (const r of withIssues) {
            console.log(`\n  ${r.className}  (${r.basePath || 'no-path'}, ${r.endpoints} 个端点)`);
            for (const i of r.issues) {
                console.log(`    [${i.code}]${i.severity === 'advisory' ? ' (建议)' : ''} ${i.message}`);
            }
        }
        const clean = results.filter((r) => r.issues.length === 0).map((r) => r.className);
        if (clean.length > 0) {
            console.log(`\n[audit] 无问题: ${clean.join(', ')}`);
        }
        console.log(`\n[audit] 阻断级问题（A1/B1/B2/C1/C2）控制器数: ${blocking.length}`);
    }

    if (strictMode && blocking.length > 0) {
        process.exit(1);
    }
}

main();
