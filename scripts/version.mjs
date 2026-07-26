import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const rootPackagePath = resolve(rootDir, "package.json");
const adminPackagePath = resolve(rootDir, "apps/admin/package.json");
const mobilePackagePath = resolve(rootDir, "apps/mobile/package.json");
const rootLockPath = resolve(rootDir, "package-lock.json");
const adminLockPath = resolve(rootDir, "apps/admin/package-lock.json");
const mobileLockPath = resolve(rootDir, "apps/mobile/package-lock.json");
const pomPath = resolve(rootDir, "services/api/pom.xml");
const semverPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

function readPomVersion() {
  const pom = readFileSync(pomPath, "utf8");
  const match = pom.match(
    /<artifactId>ops-erp-api<\/artifactId>\s*<version>([^<]+)<\/version>/,
  );
  if (!match) {
    throw new Error("Cannot find the ops-erp-api project version in pom.xml");
  }
  return match[1].trim();
}

function writePomVersion(version) {
  const pom = readFileSync(pomPath, "utf8");
  const updated = pom.replace(
    /(<artifactId>ops-erp-api<\/artifactId>\s*<version>)[^<]+(<\/version>)/,
    `$1${version}$2`,
  );
  if (updated === pom) {
    throw new Error("Cannot update the ops-erp-api project version in pom.xml");
  }
  writeFileSync(pomPath, updated);
}

function collectVersions() {
  const rootPackage = readJson(rootPackagePath);
  const adminPackage = readJson(adminPackagePath);
  const mobilePackage = readJson(mobilePackagePath);
  const rootLock = readJson(rootLockPath);
  const adminLock = readJson(adminLockPath);
  const mobileLock = readJson(mobileLockPath);
  return {
    "package.json": rootPackage.version,
    "apps/admin/package.json": adminPackage.version,
    "apps/mobile/package.json": mobilePackage.version,
    "package-lock.json": rootLock.version,
    "package-lock.json#root": rootLock.packages?.[""]?.version,
    "apps/admin/package-lock.json": adminLock.version,
    "apps/admin/package-lock.json#root": adminLock.packages?.[""]?.version,
    "apps/mobile/package-lock.json": mobileLock.version,
    "apps/mobile/package-lock.json#root": mobileLock.packages?.[""]?.version,
    "services/api/pom.xml": readPomVersion(),
  };
}

function checkVersions() {
  const versions = collectVersions();
  const expected = versions["package.json"];
  const mismatches = Object.entries(versions).filter(
    ([, version]) => version !== expected,
  );
  if (!semverPattern.test(expected)) {
    throw new Error(`Invalid product version in package.json: ${expected}`);
  }
  if (mismatches.length > 0) {
    const details = mismatches
      .map(([path, version]) => `${path}=${version ?? "missing"}`)
      .join(", ");
    throw new Error(`Version mismatch; expected ${expected}: ${details}`);
  }
  console.log(`Version files are consistent: ${expected}`);
  return expected;
}

function nextVersion(current, releaseType) {
  const match = current.match(semverPattern);
  if (!match) {
    throw new Error(`Invalid current version: ${current}`);
  }
  let major = Number(match[1]);
  let minor = Number(match[2]);
  let patch = Number(match[3]);
  if (releaseType === "major") {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (releaseType === "minor") {
    minor += 1;
    patch = 0;
  } else if (releaseType === "patch") {
    patch += 1;
  } else {
    throw new Error(`Unsupported release type: ${releaseType}`);
  }
  return `${major}.${minor}.${patch}`;
}

function updateVersions(version) {
  if (!semverPattern.test(version)) {
    throw new Error(`Version must use MAJOR.MINOR.PATCH format: ${version}`);
  }
  const rootPackage = readJson(rootPackagePath);
  const adminPackage = readJson(adminPackagePath);
  const mobilePackage = readJson(mobilePackagePath);
  const rootLock = readJson(rootLockPath);
  const adminLock = readJson(adminLockPath);
  const mobileLock = readJson(mobileLockPath);
  rootPackage.version = version;
  adminPackage.version = version;
  mobilePackage.version = version;
  for (const lock of [rootLock, adminLock, mobileLock]) {
    lock.version = version;
    lock.packages[""].version = version;
  }
  writeJson(rootPackagePath, rootPackage);
  writeJson(adminPackagePath, adminPackage);
  writeJson(mobilePackagePath, mobilePackage);
  writeJson(rootLockPath, rootLock);
  writeJson(adminLockPath, adminLock);
  writeJson(mobileLockPath, mobileLock);
  writePomVersion(version);
  console.log(`Product version updated to ${version}`);
}

const [command = "check", value] = process.argv.slice(2);

try {
  if (command === "check") {
    checkVersions();
  } else if (["patch", "minor", "major"].includes(command)) {
    updateVersions(nextVersion(checkVersions(), command));
  } else if (command === "set" && value) {
    updateVersions(value);
  } else {
    throw new Error(
      "Usage: node scripts/version.mjs check|patch|minor|major|set <version>",
    );
  }
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
}
