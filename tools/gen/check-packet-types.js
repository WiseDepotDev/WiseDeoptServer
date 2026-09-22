#!/usr/bin/env node
/**
 * 跨端 packet_type 一致性门禁（离线，无依赖）。
 *
 * 背景（真实风险，不是理论风险）：
 *   服务端 `GlobalRequestAdvice` 只检查请求体里有没有 `header`/`payload`，
 *   **不校验 `header.packet_type` 的取值**；写错类型既不会 4xx，也不会打 WARN，
 *   只会在响应侧体现为 `packet_type: "0x0000"`（UNKNOWN），属于典型的"静默失败"。
 *   因此把"设备端/APP 里出现的 packet_type 字面量必须存在于服务端 PacketType 枚举"
 *   做成可执行门禁，而不是靠人肉比对。
 *
 * 唯一来源：`WiseDeoptServer/.../common/protocol/PacketType.java`（STD-CONTRACT-03）。
 *
 * 采集规则（标记式，避免误抓无关字符串）：
 *   - 设备端：`envelope_wrap_request("<TYPE>", ...)` 的第一个实参；
 *   - APP   ：`packetType = "<TYPE>"` 或 `PACKET_TYPE = "<TYPE>"` 的字符串字面量
 *             （P3-20 落地后自动生效；当前 APP 尚未请求侧信封化，故命中 0 条）。
 *
 * 用法：
 *   node check-packet-types.js [--repo <工作区根>] [--quiet]
 * 退出码：0 = 全部命中枚举；1 = 有未知类型或形状非法；2 = 找不到枚举/工作区。
 */

'use strict';

const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
let repoRoot = path.resolve(__dirname, '..', '..', '..');
let quiet = false;
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--repo') repoRoot = path.resolve(args[++i]);
  else if (args[i] === '--quiet') quiet = true;
  else {
    console.error(`未知参数: ${args[i]}`);
    process.exit(2);
  }
}

const TYPE_DEF = path.join(
  repoRoot,
  'WiseDeoptServer',
  'wise-deopt-common',
  'src',
  'main',
  'java',
  'com',
  'huicang',
  'wise',
  'common',
  'protocol',
  'PacketType.java'
);

if (!fs.existsSync(TYPE_DEF)) {
  console.error(`找不到 PacketType 枚举（这是唯一来源）: ${TYPE_DEF}`);
  process.exit(2);
}

/** 枚举名 → 枚举码（0x 字符串） */
const known = new Map();
const enumSrc = fs.readFileSync(TYPE_DEF, 'utf8');
for (const line of enumSrc.split(/\r?\n/)) {
  const m = /^\s*([A-Z][A-Z0-9_]*)\s*\(\s*"([^"]+)"/.exec(line);
  if (m) known.set(m[1], m[2]);
}
if (known.size === 0) {
  console.error('PacketType 枚举解析出 0 个成员，脚本规则可能已失效（不是"通过"）。');
  process.exit(2);
}

/** 递归收集文件（跳过构建产物） */
function walk(dir, exts, out) {
  if (!fs.existsSync(dir)) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (['obj', 'obj-release', 'obj-asan', 'obj-tsan', 'bin', 'build', '.git', 'node_modules'].includes(entry.name)) continue;
      walk(full, exts, out);
    } else if (exts.some((e) => entry.name.endsWith(e))) {
      out.push(full);
    }
  }
  return out;
}

const SCAN_RULES = [
  {
    label: '设备端',
    dir: path.join(repoRoot, 'WiseDepotDevice', 'src'),
    exts: ['.c'],
    pattern: /envelope_wrap_request\s*\(\s*"([^"]*)"/g,
  },
  {
    label: 'APP',
    dir: path.join(repoRoot, 'WiseDepotApp', 'wise-depot-android-refactor', 'app', 'src', 'main', 'java'),
    exts: ['.kt', '.java'],
    pattern: /(?:packetType|PACKET_TYPE)\s*=\s*"([^"]*)"/g,
  },
];

const findings = [];
let scanned = 0;

for (const rule of SCAN_RULES) {
  const files = walk(rule.dir, rule.exts, []);
  for (const file of files) {
    const src = fs.readFileSync(file, 'utf8');
    const rel = path.relative(repoRoot, file).replace(/\\/g, '/');
    let m;
    rule.pattern.lastIndex = 0;
    while ((m = rule.pattern.exec(src)) !== null) {
      scanned++;
      const type = m[1];
      const line = src.slice(0, m.index).split(/\r?\n/).length;
      const problems = [];
      if (!/^[A-Z][A-Z0-9_]*$/.test(type)) {
        problems.push('形状不符合 schema 的 ^[A-Z][A-Z0-9_]*$');
      }
      if (type.length > 64) problems.push('长度超过 schema 上限 64');
      if (!known.has(type)) {
        problems.push(`不在服务端 PacketType 枚举中（未知类型必须写 UNKNOWN）`);
      }
      findings.push({ rule: rule.label, rel, line, type, problems });
    }
  }
}

if (!quiet) {
  console.log(`packet_type 一致性检查：枚举成员 ${known.size} 个，采集到跨端字面量 ${scanned} 处`);
  // 按文件汇总（生成的大表可能有上百条，逐条打印会淹没输出）；明细只打印"有问题"的。
  const byFile = new Map();
  for (const f of findings) {
    const key = `[${f.rule}] ${f.rel}`;
    byFile.set(key, (byFile.get(key) || 0) + 1);
  }
  for (const [key, count] of byFile) {
    console.log(`  ${key}: ${count} 处`);
  }
  const bad = findings.filter((f) => f.problems.length > 0);
  for (const f of bad) {
    console.log(`  ❌ ${f.rel}:${f.line} ${f.type} —— ${f.problems.join('；')}`);
  }
}

const bad = findings.filter((f) => f.problems.length > 0);
if (bad.length > 0) {
  console.error(`\ncheck-packet-types FAIL: ${bad.length}/${scanned} 处 packet_type 不合法`);
  for (const f of bad) {
    console.error(`  ${f.rel}:${f.line} "${f.type}" —— ${f.problems.join('；')}`);
  }
  console.error('处置：改用 PacketType 中已有的类型；确实没有对应类型时写 UNKNOWN（并在《待确认决策清单》登记新增类型的诉求）。');
  process.exit(1);
}

if (scanned === 0) {
  console.log('check-packet-types OK（当前 0 处采集点：APP 侧待 P3-20 落地后自动纳入）');
} else {
  console.log(`check-packet-types OK: ${scanned} 处 packet_type 均在 PacketType 枚举中且形状合法`);
}
