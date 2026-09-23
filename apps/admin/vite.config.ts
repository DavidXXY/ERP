import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import Components from "unplugin-vue-components/vite";
import { AntDesignVueResolver } from "unplugin-vue-components/resolvers";
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { fileURLToPath, URL } from "node:url";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || "http://localhost:8080";
const rootPackage = JSON.parse(
  readFileSync(new URL("../../package.json", import.meta.url), "utf8"),
) as { version: string };

function resolveCommitId() {
  const environmentCommit = process.env.OPS_COMMIT || process.env.GITHUB_SHA;
  if (environmentCommit) {
    return environmentCommit.slice(0, 8);
  }
  try {
    return execFileSync("git", ["rev-parse", "--short=8", "HEAD"], {
      cwd: fileURLToPath(new URL("../..", import.meta.url)),
      encoding: "utf8",
    }).trim();
  } catch {
    return "";
  }
}

const commitId = resolveCommitId();
const appVersion = commitId
  ? `${rootPackage.version}+${commitId}`
  : rootPackage.version;

export default defineConfig({
  cacheDir: "../../node_modules/.vite-admin",
  legacy: {
    skipWebSocketTokenCheck: true,
  },
  define: {
    __APP_VERSION__: JSON.stringify(appVersion),
  },
  plugins: [
    vue(),
    Components({
      resolvers: [
        AntDesignVueResolver({ importStyle: false }),
      ],
    }),
    {
      name: "favicon-redirect",
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === "/favicon.ico") {
            res.writeHead(302, { Location: "/favicon.svg" });
            res.end();
            return;
          }
          next();
        });
      },
    },
  ],
  test: {
    coverage: {
      provider: "v8",
      reporter: ["text", "json-summary", "html"],
      include: ["src/**/*.{ts,vue}"],
      exclude: ["src/**/*.d.ts", "src/main.ts"],
      // Vitest 4 默认将所有匹配 include 的文件（含未测试的视图）纳入覆盖率分母，
      // 与 Vitest 3 的度量口径不同，据此下调阈值保持门禁仍可执行。
      thresholds: {
        lines: 4,
        functions: 5,
        statements: 4,
        branches: 7,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/vue/") || id.includes("node_modules/vue-router/") || id.includes("node_modules/pinia/")) {
            return "vue";
          }
        },
      },
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5174,
    proxy: {
      "/api": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      "/qualification-files": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
