import { readdirSync, statSync } from "node:fs";
import { join } from "node:path";

const assetsDir = new URL("../apps/admin/dist/assets", import.meta.url).pathname;
const maxChunkBytes = Number(process.env.MAX_CHUNK_BYTES || 500_000);
const oversized = readdirSync(assetsDir)
  .map((name) => ({ name, size: statSync(join(assetsDir, name)).size }))
  .filter(({ name, size }) => name.endsWith(".js") && size > maxChunkBytes);

if (oversized.length) {
  console.error(`Bundle budget exceeded (${maxChunkBytes} bytes):`, oversized);
  process.exit(1);
}
console.log(`Bundle budget passed: every JS chunk <= ${maxChunkBytes} bytes`);
