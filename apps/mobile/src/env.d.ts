/// <reference types="@dcloudio/types" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module "@vue/runtime-core" {
  interface ComponentCustomProperties {
    uni: typeof uni;
  }
}

export {};
