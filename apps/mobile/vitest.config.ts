import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
    coverage: {
      provider: "v8",
      include: ["src/utils/http.ts", "src/utils/offline.ts"],
      thresholds: {
        lines: 30,
        functions: 30,
        statements: 30,
        branches: 25,
      },
    },
  },
});
