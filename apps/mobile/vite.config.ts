import { defineConfig } from "vite";
import uniModule from "@dcloudio/vite-plugin-uni";

const uni = (uniModule as unknown as { default: typeof uniModule }).default;

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
});
