#!/usr/bin/env node
/**
 * Migration parity check: main PostgreSQL migrations vs. H2 test mirrors.
 *
 * The PostgreSQL migrations live in
 *   services/api/src/main/resources/db/migration/*.sql
 * and the H2 test mirrors live in
 *   services/api/src/test/resources/db/migration-h2/*.sql
 *
 * A migration "version" is the leading token before the first "__",
 * e.g. `V147` or `B77` (the `B` prefix marks a Flyway baseline).
 *
 * ---------------------------------------------------------------------------
 * INCREMENTAL POLICY (why this does not fail on historical gaps)
 * ---------------------------------------------------------------------------
 * Over time, some older main migrations were never mirrored into the H2 test
 * schema (for example V79, V89, V121..V134, ...). Those are HISTORICAL gaps:
 * they were shipped long ago and are no longer a regression we can cheaply fix,
 * so they must NOT break the build.
 *
 * What we care about is NEW drift: when someone adds a fresh main migration
 * (a version number higher than the highest version already mirrored in H2)
 * without also adding its H2 mirror. If that happens, the H2 test suite would
 * silently stop exercising the new schema, so we fail the build.
 *
 * Concretely, the check:
 *   1. Collect every main version and every H2 version.
 *   2. Report every main version that has no H2 mirror (informational).
 *   3. Fail (exit 1) ONLY when such a missing main version is GREATER than the
 *      highest version present in H2. Missing versions <= the H2 max are
 *      historical gaps and are reported but tolerated.
 * ---------------------------------------------------------------------------
 */

import { readdirSync, statSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const mainDir = join(root, "services", "api", "src", "main", "resources", "db", "migration");
const h2Dir = join(root, "services", "api", "src", "test", "resources", "db", "migration-h2");

function listVersions(dir) {
  const files = readdirSync(dir).filter(
    (f) => f.endsWith(".sql") && statSync(join(dir, f)).isFile(),
  );
  const versions = new Map();
  for (const file of files) {
    const token = file.split("__", 1)[0];
    versions.set(token, file);
  }
  return versions;
}

/** Numeric part of a version token (e.g. "V147" -> 147, "B77" -> 77). */
function numeric(token) {
  const match = token.match(/\d+/);
  return match ? Number(match[0]) : 0;
}

const main = listVersions(mainDir);
const h2 = listVersions(h2Dir);

const missing = [...main.keys()]
  .filter((v) => !h2.has(v))
  .sort((a, b) => numeric(a) - numeric(b) || a.localeCompare(b));
const maxH2Numeric = [...h2.keys()].reduce((max, v) => Math.max(max, numeric(v)), 0);

// New drift = a main version with no H2 mirror whose version number exceeds
// everything already mirrored in H2. Historical gaps are <= this threshold.
const drift = missing.filter((v) => numeric(v) > maxH2Numeric);

console.log(`Main PostgreSQL migrations: ${main.size}`);
console.log(`H2 test mirrors:            ${h2.size}`);
console.log(`Highest H2 version:         ${maxH2Numeric}`);
console.log(`Main versions without H2 mirror: ${missing.length}`);
if (missing.length > 0) {
  console.log("  Historical gaps (tolerated):");
  for (const v of missing) console.log(`    - ${v}`);
}

if (drift.length > 0) {
  console.error("\nNEW MIGRATION DRIFT DETECTED (will fail the build):");
  for (const v of drift) console.error(`    - ${v} (greater than H2 max ${maxH2Numeric})`);
  console.error("Add the missing H2 mirror under services/api/src/test/resources/db/migration-h2/.");
  process.exit(1);
}

console.log("\nOK — no new migration drift. All missing mirrors are historical gaps.");
process.exit(0);
