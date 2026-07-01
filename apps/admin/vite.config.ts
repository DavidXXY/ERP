import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  cacheDir: "../../node_modules/.vite-admin",
  plugins: [vue()],
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: "vue-vendor",
              test: /node_modules[\\/](?:vue|vue-router|pinia)[\\/]/,
              priority: 30,
            },
            {
              name: "ant-design-vendor",
              test: /node_modules[\\/](?:ant-design-vue|@ant-design)[\\/]/,
              priority: 20,
              maxSize: 450_000,
            },
            {
              name: "vendor",
              test: /node_modules[\\/]/,
              priority: 10,
              maxSize: 450_000,
            },
          ],
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
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/qualification-files": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
