#!/usr/bin/env node
/*
 * secret-scan.js —— 敏感信息入库门禁（STD-SEC-01 / P2-10）
 *
 * 目标：任何明文口令、密钥、签名盐、内网服务器地址都不得出现在 git 跟踪的文件中。
 * 扫描范围：git ls-files 的文本文件（只扫入库内容，工作区未跟踪文件不参与）。
 *
 * 规则：
 *   CRED_CONFIG_VALUE  配置文件里敏感键的值不是 ${占位符}
 *   CRED_CODE_LITERAL  源码里敏感字段被赋值为字面量字符串，且该字面量像凭据
 *   PLACEHOLDER_DEFAULT 敏感字段的 ${KEY:default} 占位符带了非空默认值（等于硬编码）
 *   INTERNAL_IP        内网/私有服务器地址硬编码
 *   KNOWN_DEMO_SECRET  历史明文口令/密钥字面量回归
 *
 * 用法：
 *   node tools/audit/secret-scan.js            # 扫描，有命中则退出码 1
 *   node tools/audit/secret-scan.js --list     # 额外打印跳过统计
 *
 * 退出码：0 = 无命中；1 = 有命中（门禁失败）；2 = 运行环境错误。
 */
'use strict';

const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const REPO_ROOT = path.resolve(__dirname, '..', '..');

const BINARY_EXT = new Set([
    '.png', '.jpg', '.jpeg', '.gif', '.ico', '.webp', '.bmp', '.pdf',
    '.zip', '.gz', '.tar', '.jar', '.war', '.class', '.so', '.a', '.o',
    '.woff', '.woff2', '.ttf', '.eot', '.mp4', '.mp3', '.xlsx', '.docx',
]);

const CONFIG_EXT = new Set([
    '.yml', '.yaml', '.properties', '.env', '.conf', '.ini', '.toml', '.cfg', '.xml',
]);

const CODE_EXT = new Set([
    '.java', '.kt', '.kts', '.c', '.h', '.cpp', '.hpp', '.js', '.mjs', '.cjs', '.ts',
    '.py', '.cs', '.go', '.rs', '.sh', '.ps1', '.sql', '.gradle', '.json',
]);

/** 敏感字段名（不含 username：账号名本身不是凭据，避免噪声）。
 *
 * 2026-02-27 补 `encryption-key`：P2-19 删除孤儿类 `DataEncryptionService` 时发现它带着
 * `@Value("${encryption.key:<32 字节默认值>}")` —— 这是"占位符带非空默认值"（等于硬编码），
 * 但原规则只认 password/secret/token 等词，`encryption.key` 既不匹配 SENSITIVE_KEY
 * 也不匹配 CODE_ASSIGN，于是**漏检**。已补 `encryption[-_.]?key` 与 `jwt[-_.]?secret`。
 */
const SENSITIVE_KEY = /(password|passwd|secret-key|secretkey|secret|access-key|accesskey|api-key|apikey|private-key|privatekey|token|credential|salt|signing-key|encryption[-_.]?key|jwt[-_.]?secret)/i;

/**
 * 规则命中豁免：路径 + 规则名 + 原因。每条豁免都必须说明原因。
 */
const ALLOWLIST = [
    { path: '**/secret-scan.js', rule: '*', reason: '门禁脚本自身持有规则样例' },
    { path: '**/*.md', rule: 'KNOWN_DEMO_SECRET', reason: '文档引用历史反例，不承载可用凭据' },
    { path: '**/*.md', rule: 'INTERNAL_IP', reason: '文档记录环境拓扑' },
    { path: '**/*.md', rule: 'CRED_CONFIG_VALUE', reason: '文档中的示例配置' },
    // 文档/规划材料：允许出现环境拓扑与历史反例（不含可用于登录的凭据文件）
    { path: 'docs/**', rule: 'INTERNAL_IP', reason: '文档记录环境拓扑与网段规划' },
    { path: 'docs/**', rule: 'KNOWN_DEMO_SECRET', reason: '文档与任务清单引用历史反例' },
    { path: 'docs/**', rule: 'PIN_LITERAL', reason: '文档引用规则反例（该规则正是为捕获此写法而设）' },
    // 测试源码：允许私有网段样例地址（真实部署地址不在此列）
    { path: '**/src/test/**', rule: 'INTERNAL_IP', reason: '测试用私有网段样例，非真实地址' },
    {
        path: '**/src/test/**',
        rule: 'PIN_LITERAL',
        reason: '测试夹具需要 PIN 形状的取值；真实工牌 PIN 的暴露面在生成工具与 SQL（已清理）',
    },
    { path: '**/*.example', rule: '*', reason: '示例配置只放占位符，允许展示键名' },
    // APP 本机注入文件：规范指定的 BASE_URL 注入位置，已被 .gitignore 排除
    { path: '**/local.properties', rule: '*', reason: 'APP 本机注入文件（规范指定的 BASE_URL 来源），未入库' },
    // 已登记债务：APP debug 专用网络安全配置里的真实地址（仅 debug 资源，不进 release）
    {
        path: '**/src/debug/res/xml/network_security_config.xml',
        rule: 'INTERNAL_IP',
        reason: 'APP debug 专用配置；改由构建期生成属 APP 轮次（P6/P3）范围，登记于 P2-14',
    },
    // 故意保留在本机、且已由 .gitignore 排除的真实凭据文件（工作区模式会扫到）
    { path: 'WiseDeoptServer/config/application-local.yml', rule: '*', reason: '本机真实配置，未入库（模板见 .example）' },
    { path: 'deploy/.env', rule: '*', reason: '本机真实中间件凭据，未入库（模板见 .env.example）' },
    { path: 'deploy/.env.local', rule: '*', reason: '本机 Docker 全栈凭据，未入库（模板见 .env.local.example）' },
    { path: 'deploy/mosquitto/passwd*', rule: '*', reason: 'MQTT 口令哈希文件，非明文凭据且本机生成' },
];

/**
 * 工作区模式（`--workspace`）跳过：构建产物、依赖、备份与版本库元数据。
 * 这些目录中的命中不属于"入库泄漏"，且体量大。
 */
const SKIP_DIRS = new Set([
    '.git', '.backup', 'target', 'build', 'node_modules', '.gradle', 'obj', 'bin',
    'coverage', '.idea', '.vscode', 'dist', '__pycache__', '.mvn', 'temp-device-src',
]);

/** 值是否显然不是凭据。 */
function valueLooksSafe(raw) {
    const v = String(raw).trim().replace(/^["']|["'],?$/g, '').trim();
    if (v === '') return true;
    if (v.startsWith('${') || v.startsWith('#{') || v.startsWith('$') || v.startsWith('%')) return true;
    if (v.startsWith('*')) return true;
    if (/^[<>{}\[\]()]*$/.test(v)) return true;
    if (/^(true|false|null|none|nil|~|-\d+|\d+)$/i.test(v)) return true;
    // 占位词必须是"整个值"：占位词 [+ 分隔符] [+ 凭据类词] [+ 数字]。
    // 早期写法用 ^(your|test|...) 前缀匹配，导致 Test-Admin-Passw0rd 这类真口令被放过（自测暴露）。
    if (
        /^(your|my|the|some|dummy|fake|test|example|sample|placeholder|changeme|change-me|redacted|masked|todo|tbd)[-_]?(password|passwd|secret|token|pin|pwd|key|value|credential)?[-_]?\d{0,6}$/i.test(
            v
        )
    )
        return true;
    if (/^(value|string|list|map|object|class|file|path|name|type|enabled)$/i.test(v)) return true;
    // 尖括号占位符，如 <PIN_SALT> / <NFC_UID>
    if (/^<[^>]*>$/.test(v)) return true;
    // 低熵占位：password123 / test456 之类"单词+数字"，明显不是真凭据
    if (/^(password|passwd|secret|token|pin|pwd|key|test|demo)[-_]?\d{0,6}$/i.test(v)) return true;
    return false;
}

/** 字面量是否"像"一个真凭据（用于源码赋值，压低噪声）。 */
function literalLooksLikeSecret(v) {
    if (valueLooksSafe(v)) return false;
    // 标识符 / HTTP 头名 / 键名，如 X-Access-Key、access_key（不是凭据）。
    // 必须同时"不含数字"才排除：否则 Zx9-Kf21Qm 这类带连字符的口令会被误当作标识符放过
    // （实证漏洞：该排除曾导致植入的 `api_password = "Zx9-Kf21Qm"` 漏检）。
    if (!/\d/.test(v) && /^[A-Za-z][A-Za-z0-9]*([-_][A-Za-z0-9]+)+$/.test(v)) return false;
    if (/\d/.test(v) && v.length >= 6) return true;
    if (/[-_!@#$%^&*+=]/.test(v) && /[A-Za-z]/.test(v) && v.length >= 8) return true;
    if (v.length >= 16 && /[a-z]/.test(v) && /[A-Z]/.test(v)) return true;
    return false;
}

const PLACEHOLDER = /\$\{([^}:]+):([^}]*)\}/g;
const YAML_LINE = /^\s*(?:-\s*)?["']?([A-Za-z0-9_.\-]+)["']?\s*[:=]\s*(.*)$/;
// 字段名大小写不敏感，覆盖 SECRET_KEY / access-key / token 等常见写法。
// token 此前被排除以降低噪声；但当前规则要求"带引号的字面量"，`token = token.substring(7)`
// 一类赋值不会命中，故可安全纳入（自测用例：String token = "9f2a-Kd81";）。
const CODE_ASSIGN = /(?<![\w.])([A-Za-z0-9_$]*(?:password|passwd|secret[-_]?key|secretkey|secret|access[-_]?key|accesskey|api[-_]?key|apikey|private[-_]?key|privatekey|token|credential|salt|encryption[-_.]?key|jwt[-_.]?secret)[A-Za-z0-9_$]*)\s*[:=]\s*(["'])((?:(?!\2)[^\n]){3,160}?)\2/gi;
const INTERNAL_IP = /\b(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})\b/g;
const KNOWN_SECRET = /(Key-1122|admin123|operator123|visitor123|qq18742489354|wise-depot-secret-key-for-jwt-token-generation-2024|wise-depot-secret-key-2026|wise-depot-secret)/g;
// PIN 是纯数字凭据，会被"数字视为安全值"的通用判断放过，因此单独设规则：
// pin/pincode 被赋值为 4-8 位数字字符串即命中（工牌 PIN 属于可用凭据）。
const PIN_LITERAL = /(?<![\w.])pin[-_]?(?:code|number|no)?\s*[:=]\s*["'](\d{4,8})["']/gi;

function listTrackedFiles() {
    const out = execFileSync('git', ['ls-files', '-z'], {
        cwd: REPO_ROOT,
        encoding: 'utf8',
        maxBuffer: 64 * 1024 * 1024,
    });
    return out.split('\0').filter(Boolean);
}

/** 工作区模式：递归枚举整个工作区的文件（相对工作区根，POSIX 分隔符）。 */
function listWorkspaceFiles(root) {
    const result = [];
    const walk = (absDir, relDir) => {
        let entries;
        try {
            entries = fs.readdirSync(absDir, { withFileTypes: true });
        } catch (e) {
            return;
        }
        for (const entry of entries) {
            const rel = relDir ? relDir + '/' + entry.name : entry.name;
            if (entry.isDirectory()) {
                if (SKIP_DIRS.has(entry.name)) continue;
                walk(path.join(absDir, entry.name), rel);
            } else if (entry.isFile()) {
                result.push(rel);
            }
        }
    };
    walk(root, '');
    return result;
}

function isAllowed(file, rule) {
    const posix = file.split(path.sep).join('/');
    return ALLOWLIST.some((a) => {
        if (a.rule !== '*' && a.rule !== rule) return false;
        return globToRegex(a.path).test(posix);
    });
}

/**
 * 把 glob 编译为正则并缓存。支持 `*`（不跨目录）、`**`（跨目录，含双星斜杠前缀与斜杠双星后缀）。
 * 早期实现用 endsWith(pattern) 做匹配，导致双星斜杠与目录前缀两类模式静默失效（豁免形同虚设）。
 */
const globCache = new Map();
function globToRegex(pattern) {
    if (globCache.has(pattern)) return globCache.get(pattern);
    let re = '';
    for (let i = 0; i < pattern.length; i++) {
        const c = pattern[i];
        if (c === '*') {
            if (pattern[i + 1] === '*') {
                i++;
                if (pattern[i + 1] === '/') {
                    i++;
                    re += '(?:.*/)?';
                } else {
                    re += '.*';
                }
            } else {
                re += '[^/]*';
            }
        } else if ('\\^$.|?+()[]{}'.includes(c)) {
            re += '\\' + c;
        } else {
            re += c;
        }
    }
    const compiled = new RegExp('^' + re + '$');
    globCache.set(pattern, compiled);
    return compiled;
}

function lineOf(text, index) {
    return text.slice(0, index).split('\n').length;
}

function scan(text, file, ext, findings) {
    const report = (rule, index, excerpt) =>
        findings.push({ rule, file, line: lineOf(text, index), excerpt: String(excerpt).replace(/\s+/g, ' ').trim().slice(0, 110) });

    // CRED_CONFIG_VALUE —— 仅配置文件
    if (CONFIG_EXT.has(ext) && !isAllowed(file, 'CRED_CONFIG_VALUE')) {
        let offset = 0;
        for (const lineText of text.split('\n')) {
            const m = YAML_LINE.exec(lineText);
            if (m && SENSITIVE_KEY.test(m[1]) && !valueLooksSafe(m[2])) {
                report('CRED_CONFIG_VALUE', offset, m[1] + ': ' + m[2]);
            }
            offset += lineText.length + 1;
        }
    }

    // CRED_CODE_LITERAL —— 仅源码
    if (CODE_EXT.has(ext) && !isAllowed(file, 'CRED_CODE_LITERAL')) {
        CODE_ASSIGN.lastIndex = 0;
        let m;
        while ((m = CODE_ASSIGN.exec(text)) !== null) {
            if (literalLooksLikeSecret(m[3])) report('CRED_CODE_LITERAL', m.index, m[1] + ' = "' + m[3] + '"');
            if (m.index === CODE_ASSIGN.lastIndex) CODE_ASSIGN.lastIndex++;
        }
    }

    // PLACEHOLDER_DEFAULT —— 任意文本文件
    if (!isAllowed(file, 'PLACEHOLDER_DEFAULT')) {
        PLACEHOLDER.lastIndex = 0;
        let m;
        while ((m = PLACEHOLDER.exec(text)) !== null) {
            // docker compose 的 `${VAR:?错误信息}` 表示"必需变量"，`?` 后是报错文案而非默认值
            if (m[2].startsWith('?')) continue;
            if (SENSITIVE_KEY.test(m[1]) && !valueLooksSafe(m[2])) {
                report('PLACEHOLDER_DEFAULT', m.index, m[0]);
            }
            if (m.index === PLACEHOLDER.lastIndex) PLACEHOLDER.lastIndex++;
        }
    }

    // INTERNAL_IP
    if (!isAllowed(file, 'INTERNAL_IP')) {
        INTERNAL_IP.lastIndex = 0;
        let m;
        while ((m = INTERNAL_IP.exec(text)) !== null) {
            report('INTERNAL_IP', m.index, m[1]);
            if (m.index === INTERNAL_IP.lastIndex) INTERNAL_IP.lastIndex++;
        }
    }

    // KNOWN_DEMO_SECRET
    if (!isAllowed(file, 'KNOWN_DEMO_SECRET')) {
        KNOWN_SECRET.lastIndex = 0;
        let m;
        while ((m = KNOWN_SECRET.exec(text)) !== null) {
            report('KNOWN_DEMO_SECRET', m.index, m[1]);
            if (m.index === KNOWN_SECRET.lastIndex) KNOWN_SECRET.lastIndex++;
        }
    }

    // PIN_LITERAL —— 纯数字凭据，单独规则（见常量处说明）
    if (!isAllowed(file, 'PIN_LITERAL')) {
        PIN_LITERAL.lastIndex = 0;
        let m;
        while ((m = PIN_LITERAL.exec(text)) !== null) {
            report('PIN_LITERAL', m.index, m[0]);
            if (m.index === PIN_LITERAL.lastIndex) PIN_LITERAL.lastIndex++;
        }
    }
}

/**
 * 规则自测：`--self-test`。
 *
 * 每条用例声明的规则必须命中（或必须不命中）。门禁的"规则判断逻辑"本身需要回归防护——
 * 实证教训：一次为消除 `X-Access-Key` 误报而加的排除规则过宽，导致
 * `api_password = "Zx9-Kf21Qm"` 这类带连字符口令**漏检**，而门禁仍显示 PASS。
 */
const SELF_TEST_CASES = [
    { text: 'api_password = "Zx9-Kf21Qm"\n', expect: 'CRED_CODE_LITERAL', ext: '.py' },
    { text: 'String token = "9f2a-Kd81";\n', expect: 'CRED_CODE_LITERAL', ext: '.java' },
    { text: 'ACCESS_KEY_HEADER = "X-Access-Key"\n', expect: null, ext: '.java' },
    { text: 'password = "Test-Admin-Passw0rd";\n', expect: 'CRED_CODE_LITERAL', ext: '.java' },
    { text: 'pin = "123456"\n', expect: 'PIN_LITERAL', ext: '.py' },
    { text: 'String pin = args[0];\n', expect: null, ext: '.java' },
    { text: 'secret = "changeme"\n', expect: null, ext: '.py' },
    { text: 'jwt:\n    secret: ${WISE_JWT_SECRET}\n', expect: null, ext: '.yml' },
    { text: 'jwt:\n    secret: hardcoded-jwt-secret-2024\n', expect: 'CRED_CONFIG_VALUE', ext: '.yml' },
    { text: '@Value("${jwt.secret:wise-depot-secret-key-2024}")\n', expect: 'PLACEHOLDER_DEFAULT', ext: '.java' },
    { text: 'url: ${WD_HOST:?missing}\n', expect: null, ext: '.yml' },
    { text: 'host: 10.0.0.4\n', expect: 'INTERNAL_IP', ext: '.yml' },
    { text: 'pin_salt = "<PIN_SALT>"\n', expect: null, ext: '.py' },
];

function runSelfTest() {
    let failed = 0;
    for (const testCase of SELF_TEST_CASES) {
        const findings = [];
        scan(testCase.text, 'selftest/fixture' + testCase.ext, testCase.ext, findings);
        const rules = new Set(findings.map((f) => f.rule));
        const label = JSON.stringify(testCase.text.trim());
        if (testCase.expect === null) {
            if (rules.size > 0) {
                failed++;
                console.log('  ✘ 期望无命中，实际命中 ' + [...rules].join(',') + ' —— ' + label);
            }
        } else if (!rules.has(testCase.expect)) {
            failed++;
            console.log('  ✘ 期望命中 ' + testCase.expect + '，实际 ' + ([...rules].join(',') || '无命中') + ' —— ' + label);
        }
    }
    const total = SELF_TEST_CASES.length;
    if (failed === 0) {
        console.log('secret-scan 自测: PASS —— ' + total + '/' + total + ' 条用例通过');
        process.exit(0);
    }
    console.log('secret-scan 自测: FAIL —— ' + failed + '/' + total + ' 条用例未通过');
    process.exit(1);
}

function main() {
    const listSkipped = process.argv.includes('--list');
    const workspaceMode = process.argv.includes('--workspace');
    if (process.argv.includes('--self-test')) runSelfTest();
    const baseDir = workspaceMode ? path.resolve(REPO_ROOT, '..') : REPO_ROOT;

    let files;
    if (workspaceMode) {
        files = listWorkspaceFiles(baseDir);
    } else {
        try {
            files = listTrackedFiles();
        } catch (e) {
            console.error('无法读取 git 跟踪文件列表：' + e.message);
            process.exit(2);
        }
    }

    const findings = [];
    let scanned = 0;
    let skipped = 0;

    for (const file of files) {
        const ext = path.extname(file).toLowerCase();
        if (BINARY_EXT.has(ext)) {
            skipped++;
            continue;
        }
        let text;
        try {
            text = fs.readFileSync(path.join(baseDir, file), 'utf8');
        } catch (e) {
            skipped++;
            continue;
        }
        if (text.includes('\u0000')) {
            skipped++;
            continue;
        }
        scanned++;
        scan(text, file, ext, findings);
    }

    const byRule = {};
    for (const f of findings) byRule[f.rule] = (byRule[f.rule] || 0) + 1;

    console.log(
        'secret-scan: 扫描 ' +
            scanned +
            ' 个文本文件' +
            (workspaceMode ? '（工作区模式，已跳过构建产物/备份）' : '') +
            (listSkipped ? '（跳过 ' + skipped + ' 个二进制/不可读）' : ''));
    if (findings.length === 0) {
        console.log('secret-scan: PASS —— 未发现明文凭据或内网地址入库');
        process.exit(0);
    }

    console.log('secret-scan: FAIL —— 发现 ' + findings.length + ' 处，明细：');
    for (const f of findings) {
        console.log('  [' + f.rule + '] ' + f.file + ':' + f.line + '  ' + f.excerpt);
    }
    console.log('按规则统计：' + JSON.stringify(byRule));
    console.log('处置要求：改为 ${ENV_VAR} 占位并写入未跟踪的本地覆盖文件，或加入 ALLOWLIST 并说明原因。');
    process.exit(1);
}

main();
