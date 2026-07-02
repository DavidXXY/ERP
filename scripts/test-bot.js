#!/usr/bin/env node

/**
 * 🤖 ERP Test Bot
 * 
 * 自动测试 ERP 系统所有板块的前端和后端，检查 TypeScript 错误、构建错误
 * 和常见逻辑问题。带有 --fix 模式，可自动修复常见问题并重新检测。
 * 
 * 用法:
 *   node scripts/test-bot.js          # 仅检测
 *   node scripts/test-bot.js --fix    # 检测 + 自动修复 + 重测
 *   node scripts/test-bot.js --skip-backend  # 仅测前端
 *   node scripts/test-bot.js --skip-frontend # 仅测后端
 */

import { execSync, spawnSync } from "child_process";
import { existsSync, readFileSync, writeFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "..");
const ADMIN_DIR = resolve(ROOT, "apps/admin");
const API_DIR = resolve(ROOT, "services/api");

// ── ANSI Colors ──
const C = {
  reset: "\x1b[0m",
  red: "\x1b[31m",
  green: "\x1b[32m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
  cyan: "\x1b[36m",
  gray: "\x1b[90m",
  bold: "\x1b[1m",
};

// ── Results Collector ──
const results = { passed: 0, failed: 0, fixed: 0, warnings: 0 };
const details = [];

function pass(name, msg = "") {
  results.passed++;
  console.log(`  ${C.green}✓${C.reset} ${name}${msg ? C.gray + " — " + msg + C.reset : ""}`);
}

function fail(name, msg, error = "") {
  results.failed++;
  console.log(`  ${C.red}✗${C.reset} ${C.bold}${name}${C.reset}`);
  if (msg) console.log(`    ${C.red}${msg}${C.reset}`);
  if (error) console.log(`    ${C.gray}${error.slice(0, 500)}${C.reset}`);
  details.push({ name, msg, error });
}

function warn(name, msg) {
  results.warnings++;
  console.log(`  ${C.yellow}⚠${C.reset} ${name}${msg ? C.gray + " — " + msg + C.reset : ""}`);
}

// ── Utilities ──
function run(cmd, opts = {}) {
  const result = spawnSync("sh", ["-c", cmd], {
    cwd: opts.cwd || ROOT,
    encoding: "utf-8",
    maxBuffer: 10 * 1024 * 1024,
    env: { ...process.env, FORCE_COLOR: "0" },
    ...opts,
  });
  return {
    stdout: (result.stdout || "").trim(),
    stderr: (result.stderr || "").trim(),
    status: result.status ?? -1,
    error: result.error,
  };
}

function fileContains(path, patterns) {
  if (!existsSync(path)) return false;
  const content = readFileSync(path, "utf-8");
  return patterns.some((p) => content.includes(p));
}

function findAllFiles(dir, extensions) {
  const files = [];
  function walk(d) {
    if (!existsSync(d)) return;
    for (const name of readdirSync(d)) {
      const p = resolve(d, name);
      if (name.startsWith(".") || name === "node_modules" || name === "target" || name === "dist") continue;
      if (statSync(p).isDirectory()) walk(p);
      else if (extensions.some((ext) => name.endsWith(ext))) files.push(p);
    }
  }
  walk(dir);
  return files;
}

// ── Section Printers ──
function section(title) {
  console.log(`\n${C.cyan}${C.bold}━━━ ${title}${C.reset}`);
}

// ════════════════════════════════════════════
//  CHECK 1: TypeScript Type Checking
// ════════════════════════════════════════════
function checkTypeScript() {
  section("TypeScript 类型检查");
  
  const result = run(`npx vue-tsc --noEmit 2>&1`, { cwd: ADMIN_DIR });
  const lines = result.stdout.split("\n").filter((l) => l.includes("error TS"));
  
  if (result.status === 0 && lines.length === 0) {
    pass("TypeScript 类型检查", "零错误通过");
    return [];
  }
  
  // Parse errors
  const errors = [];
  for (const line of lines) {
    const match = line.match(/^(.+?)\((\d+),(\d+)\):\s*error\s+(TS\d+):\s*(.+)$/);
    if (match) {
      errors.push({
        file: match[1],
        line: parseInt(match[2]),
        col: parseInt(match[3]),
        code: match[4],
        message: match[5],
      });
    }
  }
  
  if (errors.length === 0) {
    fail("TypeScript 类型检查", "执行失败（无法解析输出）", result.stdout.slice(0, 300));
    return [];
  }
  
  // Group by file
  const byFile = {};
  for (const e of errors) {
    if (!byFile[e.file]) byFile[e.file] = [];
    byFile[e.file].push(e);
  }
  
  for (const [file, errs] of Object.entries(byFile)) {
    const shortFile = file.replace("src/", "");
    fail(`TypeScript → ${shortFile}`, `发现 ${errs.length} 个错误`);
    for (const e of errs) {
      console.log(`    ${C.gray}第 ${e.line} 行: [${e.code}] ${e.message}${C.reset}`);
    }
  }
  
  return { errors, byFile };
}

// ════════════════════════════════════════════
//  CHECK 2: Vite Build
// ════════════════════════════════════════════
function checkBuild() {
  section("前端构建检查");
  
  const result = run(`npx vite build 2>&1`, { cwd: ADMIN_DIR });
  
  if (result.status !== 0) {
    // Extract error lines
    const errorLines = result.stdout.split("\n").filter((l) => l.includes("ERROR") || l.includes("error"));
    fail("Vite 构建", "构建失败", errorLines.join("\n").slice(0, 500));
    return false;
  }
  
  // Check for warnings
  if (result.stdout.includes("Some chunks are larger than")) {
    warn("构建分块过大", "部分 chunks > 500KB，建议优化代码拆分");
  }
  
  pass("Vite 构建", "构建成功");
  return true;
}

// ════════════════════════════════════════════
//  CHECK 3: Spring Boot Backend Compilation
// ════════════════════════════════════════════
function checkBackend() {
  section("后端编译检查");
  
  if (!existsSync(resolve(API_DIR, "pom.xml"))) {
    warn("后端 Maven 项目", "services/api/pom.xml 不存在，跳过");
    return null;
  }
  
  const result = run(`mvn compile -q 2>&1`, { cwd: API_DIR });
  
  if (result.status !== 0) {
    const errors = result.stderr || result.stdout;
    fail("Maven 编译", "后端编译失败", errors.slice(0, 500));
    return false;
  }
  
  pass("Maven 编译", "后端代码编译通过");
  return true;
}

// ════════════════════════════════════════════
//  CHECK 4: Vue 文件常见逻辑扫描
// ════════════════════════════════════════════
function scanVueFiles() {
  section("Vue 文件静态分析");
  
  const vueFiles = findAllFiles(resolve(ADMIN_DIR, "src"), [".vue"]);
  let issues = 0;
  
  // Check for unmounted refs before use
  // Check for potential reactivity issues
  // Check for template expression issues
  
  for (const file of vueFiles) {
    const content = readFileSync(file, "utf-8");
    const relative = file.replace(ADMIN_DIR + "/src/", "");
    let hasIssues = false;
    
    // Check: script section has onMounted but no loadData call inside it
    if (content.includes("onMounted") && content.includes("loadData()") && !content.includes("onMounted(loadData)") && !content.includes("onMounted(async")) {
      // This might be fine if loadData is called inside an async onMounted wrapper
      if (!content.includes("onMounted(async () =>")) {
        warn(`可能的问题: ${relative}`, "loadData 不在 onMounted 中直接调用");
        hasIssues = true;
      }
    }
    
    // Check: empty catch blocks
    const catchMatches = content.match(/catch\s*\{\s*\}/g);
    if (catchMatches && catchMatches.length > 3) {
      const emptyCatches = catchMatches.length;
      warn(`可能的问题: ${relative}`, `有 ${emptyCatches} 个空的 catch 块`);
      hasIssues = true;
    }
    
    // Check: template with empty sections
    if (content.includes("<template>") && content.trim().endsWith("</template>")) {
      // basic template existence check
    }
    
    if (!hasIssues) issues++;
  }
  
  if (issues > 0) {
    // Only count files with no issues as "good"
  }
  
  pass("Vue 文件扫描", `${vueFiles.length} 个文件已检查`);
}

// ════════════════════════════════════════════
//  CHECK 5: 重复代码检测 (简单版)
// ════════════════════════════════════════════
function checkDuplicateKeys() {
  section("常见代码问题检测");
  
  const tsFiles = findAllFiles(resolve(ADMIN_DIR, "src"), [".ts", ".vue"]);
  let dupObjects = 0;
  
  for (const file of tsFiles) {
    const content = readFileSync(file, "utf-8");
    const relative = file.replace(ADMIN_DIR + "/src/", "");
    
    // Check for duplicate object literal keys
    // Simple heuristic: check for repeated property names in objects
    const objMatches = content.match(/(\w+):\s*[^,}]+,\s*\n\s*\1\s*:/g);
    if (objMatches) {
      for (const match of objMatches) {
        dupObjects++;
        if (match.length < 80) {
          fail(`重复属性: ${relative}`, `检测到重复属性: ${match.replace(/\s+/g, " ").trim()}`);
        }
      }
    }
  }
  
  if (dupObjects === 0) {
    pass("对象重复属性检查", "无重复键发现");
  }
}

// ════════════════════════════════════════════
//  CHECK 6: 导入检查
// ════════════════════════════════════════════
function checkImports() {
  section("模块导入完整性检查");
  
  const vueFiles = findAllFiles(resolve(ADMIN_DIR, "src"), [".vue", ".ts"]);
  let importIssues = 0;
  
  for (const file of vueFiles) {
    const content = readFileSync(file, "utf-8");
    const relative = file.replace(ADMIN_DIR + "/src/", "");
    
    // Check named exports from @/api/* - verify they're actually used
    const apiImports = content.match(/import\s+\{([^}]+)\}\s+from\s+["']@\/api\/[^"']+["']/gs);
    if (!apiImports) continue;
    
    for (const imp of apiImports) {
      // Get the API module path
      const apiPath = imp.match(/from\s+["'](@\/api\/[^"']+)["']/)?.[1];
      if (!apiPath) continue;
      
      // Get the imported names (excluding 'type' imports)
      const imports = imp.match(/\{\s*([^}]+)\s*\}/)?.[1];
      if (!imports) continue;
      
      const names = imports.split(",").map((s) => s.trim().replace(/^type\s+/, "")).filter(Boolean);
      
      // Check if each import is referenced in the code outside the import statement
      const codeWithoutImport = content.replace(imp, "");
      for (const name of names) {
        // Skip simple property names that might be used as types
        if (name === name.toUpperCase() || name.endsWith("Type") || name.endsWith("Status")) continue;
        if (!codeWithoutImport.includes(name) && !name.startsWith("type ")) {
          warn(`未使用导入: ${relative}`, `${name} 从 ${apiPath} 导入但未使用`);
          importIssues++;
        }
      }
    }
  }
  
  if (importIssues === 0) {
    pass("导入检查", "所有导入均被使用");
  }
}

// ════════════════════════════════════════════
//  AUTO-FIX Logic
// ════════════════════════════════════════════
function autoFix() {
  section("自动修复模式 🤖");
  
  let fixed = 0;
  const vueFiles = findAllFiles(resolve(ADMIN_DIR, "src"), [".vue", ".ts"]);
  
  for (const file of vueFiles) {
    let content = readFileSync(file, "utf-8");
    let changed = false;
    
    // Fix 1: Missing 'computed' import
    if (content.includes("computed(") || content.includes("computed(")) {
      if (content.includes('from "vue"') && !content.includes("computed")) {
        const oldImport = content.match(/from "vue"/);
        if (oldImport) {
          const lineMatch = content.match(/^import\s*\{[^}]*\}\s*from\s*"vue"/m);
          if (lineMatch) {
            const newLine = lineMatch[0].replace(/(\}?\s*from\s*"vue")/, ", computed $1").replace(/(,\s*,)/g, ",");
            if (newLine !== lineMatch[0]) {
              content = content.replace(lineMatch[0], newLine);
              changed = true;
            }
          }
        }
      }
    }
    
    // Fix 2: Template arrow function missing type annotations
    // (f) => handleUpload -> (f: File) => handleUpload
    const uploadHandlerRegex = /\(f\)\s*=>\s*handleUpload\(['"](\w+)['"],\s*f\)/g;
    if (uploadHandlerRegex.test(content)) {
      uploadHandlerRegex.lastIndex = 0;
      content = content.replace(uploadHandlerRegex, '(f: File) => handleUpload(\'$1\', f)');
      changed = true;
    }
    
    // Fix 3: (record) => record.id in template -> (record: any) => record.id
    const rowKeyRegex = /:row-key="\(record\)\s*=>\s*record\.id"/g;
    if (rowKeyRegex.test(content)) {
      rowKeyRegex.lastIndex = 0;
      content = content.replace(rowKeyRegex, ':row-key="(record: any) => record.id"');
      changed = true;
    }
    
    // Fix 4: Duplicate object literal keys (simple version)
    const dupKeyRegex = /(\w+):\s*[^,}]+,\s*\n\s*(\1)\s*:/;
    let m;
    while ((m = dupKeyRegex.exec(content)) !== null) {
      // Remove the first occurrence (before the duplicate)
      const firstKey = m[0].split("\n")[0];
      content = content.replace(firstKey + "\n", "");
      changed = true;
      fixed++;
    }
    
    if (changed) {
      writeFileSync(file, content, "utf-8");
      const relative = file.replace(ADMIN_DIR + "/src/", "");
      console.log(`  ${C.green}✎${C.reset} 已修复: ${relative}`);
      fixed++;
    }
  }
  
  console.log(`  ${C.green}修复了 ${fixed} 个问题${C.reset}`);
  results.fixed += fixed;
  return fixed;
}

// ════════════════════════════════════════════
//  SUMMARY REPORT
// ════════════════════════════════════════════
function printSummary() {
  const total = results.passed + results.failed;
  console.log(`\n${C.bold}${C.cyan}═══════════════════════════════════════════${C.reset}`);
  console.log(`${C.bold}${C.cyan}  测试机器人执行报告${C.reset}`);
  console.log(`${C.bold}${C.cyan}═══════════════════════════════════════════${C.reset}`);
  console.log(`  ${C.bold}总检测项:${C.reset}  ${total}`);
  console.log(`  ${C.green}通过:${C.reset}      ${results.passed}`);
  if (results.fixed > 0) {
    console.log(`  ${C.green}自动修复:${C.reset}  ${results.fixed}`);
  }
  if (results.failed > 0) {
    console.log(`  ${C.red}失败:${C.reset}      ${results.failed}`);
  }
  if (results.warnings > 0) {
    console.log(`  ${C.yellow}警告:${C.reset}     ${results.warnings}`);
  }
  
  const allGood = results.failed === 0;
  console.log(`\n  ${allGood ? C.green + "✓ 全部通过！" + C.reset : C.red + "✗ 存在需要修复的问题" + C.reset}`);
  
  if (results.failed > 0) {
    console.log(`\n  ${C.cyan}建议:${C.reset}`);
    console.log(`    ${C.gray}1. 运行 node scripts/test-bot.js --fix 自动修复常见问题${C.reset}`);
    console.log(`    ${C.gray}2. 修复后脚本会自动重新检测${C.reset}`);
  }
  console.log();
  
  return results.failed === 0;
}

// ════════════════════════════════════════════
//  MAIN
// ════════════════════════════════════════════
function main() {
  const args = process.argv.slice(2);
  const fixMode = args.includes("--fix");
  const skipBackend = args.includes("--skip-backend");
  const skipFrontend = args.includes("--skip-frontend");
  
  console.log(`${C.cyan}${C.bold}
  ╔═══════════════════════════════════════╗
  ║     ERP 测试机器人                    ║
  ║     ${C.reset}Engineering Ops ERP Test Bot${C.cyan}       ║
  ╚═══════════════════════════════════════╝${C.reset}
  `);

  console.log(`  ${C.gray}模式: ${fixMode ? "检测 + 自动修复" : "仅检测"}${C.reset}`);
  console.log(`  ${C.gray}项目根目录: ${ROOT}${C.reset}\n`);

  // ── First pass: test ──
  let tsResult, buildOk, backendOk;
  
  if (!skipFrontend) {
    tsResult = checkTypeScript();
    buildOk = checkBuild();
    scanVueFiles();
    checkDuplicateKeys();
    checkImports();
  }
  
  if (!skipBackend) {
    backendOk = checkBackend();
  }

  // ── Fix mode ──
  if (fixMode && results.failed > 0) {
    console.log(`\n  ${C.yellow}检测到错误，进入自动修复...${C.reset}`);
    const fixedCount = autoFix();
    
    if (fixedCount > 0) {
      console.log(`\n  ${C.cyan}重检测修复后的代码...${C.reset}`);
      
      // Reset and re-run
      results.passed = 0;
      results.failed = 0;
      results.warnings = 0;
      details.length = 0;
      
      if (!skipFrontend) {
        tsResult = checkTypeScript();
        buildOk = checkBuild();
      }
    } else {
      console.log(`\n  ${C.yellow}无法自动修复所有问题，请手动检查。${C.reset}`);
    }
  }

  // ── Print summary ──
  const success = printSummary();
  process.exit(success ? 0 : 1);
}

main();
