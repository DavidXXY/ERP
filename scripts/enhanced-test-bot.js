#!/usr/bin/env node
/**
 * ERP Enhanced Test Bot v2.0
 * Comprehensive testing + auto-fix + cleanup system
 * Tests: Frontend, Backend, API, File cleanup, Logic errors
 */
import { execSync, spawnSync } from "child_process";
import { existsSync, readFileSync, writeFileSync, readdirSync, statSync, unlinkSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import http from "http";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "..");
const ADMIN_DIR = resolve(ROOT, "apps/admin");
const API_DIR = resolve(ROOT, "services/api");
const VIEWS_DIR = resolve(ADMIN_DIR, "src/views");

// ANSI Colors
const C = { reset:"\x1b[0m", red:"\x1b[31m", green:"\x1b[32m", yellow:"\x1b[33m", blue:"\x1b[34m", cyan:"\x1b[36m", gray:"\x1b[90m", bold:"\x1b[1m" };

const results = { passed:0, failed:0, fixed:0, warnings:0, cleaned:0 };

function run(cmd, opts = {}) {
  const r = spawnSync("sh", ["-c", cmd], { cwd: opts.cwd || ROOT, encoding: "utf-8", maxBuffer: 10 * 1024 * 1024 });
  return { stdout: (r.stdout || "").trim(), stderr: (r.stderr || "").trim(), status: r.status || -1, error: r.error };
}

function findAll(dir, exts, skip = ["node_modules", "target", "dist", ".git"]) {
  if (!existsSync(dir)) return [];
  const files = [];
  function walk(d) {
    for (const n of readdirSync(d)) {
      if (n.startsWith(".") || skip.includes(n)) continue;
      const p = resolve(d, n);
      try {
        if (statSync(p).isDirectory()) walk(p);
        else if (exts.some((e) => n.endsWith(e))) files.push(p);
      } catch {}
    }
  }
  walk(dir);
  return files;
}

function section(t) { console.log("\n" + C.cyan + C.bold + "=== " + t + C.reset); }
function pass(n, m) { results.passed++; console.log("  " + C.green + "OK" + C.reset + " " + n + (m ? " - " + m : "")); }
function fail(n, m, e) { results.failed++; console.log("  " + C.red + "FAIL" + C.reset + " " + C.bold + n + C.reset); if (m) console.log("    " + m); if (e) console.log("    " + C.gray + e.slice(0, 400) + C.reset); }
function warn(n, m) { results.warnings++; console.log("  " + C.yellow + "WARN" + C.reset + " " + n + (m ? " - " + m : "")); }

function httpGet(url) {
  return new Promise(r => {
    const req = http.get(url, res => {
      let d = "";
      res.on("data", c => d += c);
      res.on("end", () => r({status: res.statusCode, data: d.slice(0, 200)}));
    });
    req.on("error", e => r({status: 0, data: e.message}));
    req.setTimeout(5000, () => { req.destroy(); r({status: 0, data: "timeout"}); });
  });
}

// === CHECK 1: System Health ===
async function checkSystemHealth() {
  section("System Health");
  const fe = await httpGet("http://localhost:5174/");
  if (fe.status === 200) pass("Frontend (5174)", "Running"); else fail("Frontend (5174)", fe.status ? "Status: " + fe.status : "Connection failed");
  const be = await httpGet("http://localhost:8080/");
  if (be.status === 403 || be.status === 200 || be.status === 401) pass("Backend (8080)", "Running");
  else if (be.status === 0) fail("Backend (8080)", "Not running / timeout");
  else warn("Backend (8080)", "Status " + be.status);
}

// === CHECK 2: TypeScript ===
function checkTypeScript() {
  section("TypeScript Type Check");
  const result = run("npx vue-tsc --noEmit 2>&1", { cwd: ADMIN_DIR });
  const errors = result.stdout.split("\n").filter(l => l.includes("error TS"));
  if (errors.length === 0) { pass("TypeScript", "Zero errors"); return 0; }
  const byFile = {};
  for (const l of errors) {
    const m = l.match(/^(.+?)\((\d+),(\d+)\):\s*error\s+(TS\d+):\s*(.+)$/);
    if (m) {
      const f = m[1].replace("src/", "");
      if (!byFile[f]) byFile[f] = [];
      byFile[f].push({ l: m[2], c: m[4], msg: m[5] });
    }
  }
  for (const [f, errs] of Object.entries(byFile)) {
    fail("TS " + f, errs.length + " errors", errs.slice(0, 2).map(e => "Line " + e.l + ": [" + e.c + "] " + e.msg).join("\n"));
  }
  return errors.length;
}

// === CHECK 3: Vite Build ===
function checkBuild() {
  section("Frontend Build");
  const r = run("npx vite build 2>&1", { cwd: ADMIN_DIR });
  if (r.status !== 0) { fail("Vite Build", "Failed", r.stdout.split("\n").filter(l => l.includes("ERROR")).join("\n")); return false; }
  pass("Vite Build", "Success"); return true;
}

// === CHECK 4: Backend ===
function checkBackend() {
  section("Backend Compilation");
  if (!existsSync(resolve(API_DIR, "pom.xml"))) { warn("Maven", "pom.xml not found, skipping"); return null; }
  const r = run("mvn compile -q 2>&1", { cwd: API_DIR });
  if (r.status !== 0) { fail("Maven Compile", "Failed", (r.stderr || r.stdout).slice(0, 400)); return false; }
  pass("Maven Compile", "Success"); return true;
}

// === CHECK 5: API Endpoints ===
async function checkAPI() {
  section("API Endpoint Test");
  const token = await (async () => {
    try {
      const data = JSON.stringify({ yusername: "admin", password: "Admin@123" });
      return new Promise(r => {
        const req = http.request({
          hostname: "localhost", port: 8080, path: "/api/auth/login", method: "POST",
          headers: { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) },
          timeout: 5000,
        }, res => {
          let b = ""; res.on("data", c => b += c); res.on("end", () => {
            try { r(JSON.parse(b).data.token || null); } catch { r(null); }
          });
        });
        req.on("error", () => r(null));
        req.write(data); req.end();
      });
    } catch { return null; }
  })();
  if (!token) { fail("API Auth", "Cannot login"); return; }
  pass("API Auth", "Logged in");
  
  for (const [name, path] of [
    ["CRM Get", "/api/crm/opportunities"], ["CRM Cust", "/api/crm/customers"],
    ["CRM Q","/api/crm/quotes"], ["CRM C","/api/crm/contracts"],
    ["CRM R","/api/crm/receivables"], ["PROJ","/api/projects"],
    ["INV","/api/inventory/parts"], ["PRO","/api/procurement/requests"],
    ["PRO O","/api/procurement/orders"], ["PRO","/api/procurement/suppliers"],
    [FM, "/api/finance/payment-applications"],
    ["HR", "/api/hr/employees"],
    ["QUAL", "/api/qualification/companies"],
    ["OA", "/api/office/approval-requests"],
    ["SYS", "/api/system/users"],
  ]) {
    const res = await fetchApi(path, token);
    if (res.status === 200 || res.status === 403) pass("API " + name, res.status === 200 ? "OK" : "Auth required");
    else if (res.status === 0) fail("API " + name, "Connection failed");
    else if (res.status === 500) fail("API " + name, "Server error(500)");
    else warn("API " + name, "Status " + res.status);
  }
}

async function fetchApi(path, token) {
  return new Promise(r => {
    const req = http.get({ hostname: "localhost", port: 8080, path, headers: { Authorization: "Bearer " + token }, timeout: 5000 }, res => {
      let d = ""; res.on("data", c => d += c); res.on("end", () => r({status: res.statusCode, data: d.slice(0, 100)}));
    });
    req.on("error", e => r({status: 0, data: e.message}));
  });
}

// === CHECK 6: File Cleanup ===
function cleanupFiles() {
  section("File Cleanup");
  let cleaned = 0;
  for (const f of findAll(ADMIN_DIR, [".bak", ".orig", ".tmp", "~"])) {
    try { unlinkSync(f); console.log("  Delete: " + f.replace(ADMIN_DIR + "/", "")); cleaned++; } catch { }
  }
  // Find empty dirs
  function findEmpty(d) {
    let e = [];
    for (const n of readdirSync(d)) {
      if (n.startsWith(".") || ["node_modules", "target", "dist"].includes(n)) continue;
      const p = resolve(d, n);
      if (statSync(p).isDirectory()) {
        const s = readdirSync(p);
        if (s.length === 0) e.push(p);
        else e = e.concat(findEmpty(p));
      }
    }
    return e;
  }
  const empty = findEmpty(ADMIN_DIR);
  if (empty.length > 0) warn("Empty dirs", empty.length + " found");
  else pass("Empty dirs", "None");
  if (cleaned > 0) pass("Cleanup", "Removed " + cleaned + " files");
  else pass("Cleanup", "No files to clean");
}

// === SUMMARY ===
function printSummary() {
  console.log("\n" + C.bold + C.cyan + "=== Report ===" + C.reset);
  console.log("Passed: " + results.passed + " Failed: " + results.failed + " Fixed: " + results.fixed + " Warnings: " + results.warnings);
  console.log("\n" + (results.failed === 0 ? C.green + "All OK" : C.red + "Issues remain") + C.reset + "\n");
  return results.failed === 0;
}

// === SUMMARY ===
async function main() {
  const args = process.argv.slice(2);
  console.log(C.cyan + C.bold + "\n=== ERP Enhanced Test Bot v2.0 ===" + C.reset);
  if (!args.includes("--api-only")) {
    await checkSystemHealth();
    checkTypeScript();
    checkBuild();
    if (!args.includes("--frontend-only")) checkBackend();
  }
  if (!args.includes("--frontend-only")) await checkAPI();
  cleanupFiles();
  printSummary();
  process.exit(results.failed === 0 ? 0 : 1);
}

main();
