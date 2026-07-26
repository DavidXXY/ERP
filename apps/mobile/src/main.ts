import { createSSRApp } from "vue";
import { createPinia } from "pinia";
import AppIcon from "@/components/AppIcon.vue";
import App from "./App.vue";
import "./styles/theme.scss";

export function createApp() {
  const app = createSSRApp(App);
  app.use(createPinia());
  app.component("uni-icons", AppIcon);
  return { app };
}
