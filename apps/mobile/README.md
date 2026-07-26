# 微信小程序移动端

基于 uni-app、Vue 3 和 TypeScript，与 `services/api` 共用认证、权限和业务数据。

## 本地运行

```bash
npm install
npm run mobile:dev
```

H5 调试地址默认是 `http://localhost:5180`，`/api` 代理到 `http://localhost:8080`。

微信开发者工具联调：

```bash
VITE_API_BASE_URL=https://your-api.example.com/api npm run mobile:dev:mp-weixin
```

随后用微信开发者工具导入 `apps/mobile/dist/dev/mp-weixin`。

## 发布前配置

1. 将 `src/manifest.json` 中的 `mp-weixin.appid` 替换为正式 AppID。
2. 设置 `VITE_API_BASE_URL` 为已备案且启用 HTTPS 的正式 API 地址。
3. 在微信公众平台配置 request、uploadFile、downloadFile 合法域名。
4. 在小程序后台声明位置、相机、相册相关隐私用途。
5. 后端配置 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET`。

## 构建

```bash
npm run mobile:build
npm run mobile:build:h5
```

正式微信产物位于 `apps/mobile/dist/build/mp-weixin`。
