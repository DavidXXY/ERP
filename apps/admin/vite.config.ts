import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import Components from "unplugin-vue-components/vite";
import { AntDesignVueResolver } from "unplugin-vue-components/resolvers";
import { fileURLToPath, URL } from "node:url";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || "http://localhost:8080";

export default defineConfig({
  cacheDir: "../../node_modules/.vite-admin",
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
