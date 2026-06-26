import { createApp } from "vue";
import { createPinia } from "pinia";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  List,
  Menu,
  Modal,
  Result,
  Row,
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Typography,
} from "ant-design-vue";
import "ant-design-vue/dist/reset.css";
import "./assets/main.css";
import App from "./App.vue";
import router from "./router";

const app = createApp(App);

[
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  List,
  Menu,
  Modal,
  Result,
  Row,
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Typography,
].forEach((component) => app.use(component));

app.use(createPinia()).use(router).mount("#app");
