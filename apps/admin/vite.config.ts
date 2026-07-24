import { defineConfig } from "vite";
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
