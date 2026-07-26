# 微信小程序发布与运维

## 1. 平台准备

- 使用企业主体注册微信小程序并完成认证。
- 准备已备案域名及有效 HTTPS 证书，生产接口建议使用 `https://api.erp.example.com/api`。
- 在微信公众平台配置 request、uploadFile、downloadFile 合法域名。
- 在“用户隐私保护指引”中声明位置、相机、相册用途。
- 在 `apps/mobile/src/manifest.json` 替换正式 AppID。

小程序不能访问 `localhost`、内网 IP 或未配置到微信后台的域名。

## 2. 后端配置

在 `/etc/ops-erp/ops-erp.env` 配置：

```text
WECHAT_ENABLED=true
WECHAT_APP_ID=wx_your_app_id
WECHAT_APP_SECRET=your_server_side_secret
```

`WECHAT_APP_SECRET` 只能保存在后端环境变量中，不得写入小程序源码、构建产物或前端日志。更新后重启后端：

```bash
sudo systemctl restart ops-erp-api
```

首次使用时，员工先用 ERP 账号密码登录，在“个人中心”选择“绑定当前微信”。绑定完成后可使用微信快捷登录。

## 3. 生产构建

```bash
MOBILE_API_BASE_URL=https://api.erp.example.com/api ./deploy/build-mobile.sh
```

构建脚本会执行类型检查、单元测试和微信构建。产物位于：

```text
apps/mobile/dist/build/mp-weixin
```

使用微信开发者工具导入该目录，完成真机预览、体验版验证和上传。

## 4. 发布检查

- 账号密码登录、微信绑定、微信快捷登录均可用。
- 首页待办、审批处理和通知已按当前用户隔离。
- 使用两个账号验证同一全员通知的已读状态互不影响，审批私有通知不可被无关账号访问。
- 超过 200 条审批或通知时，列表能够继续逐页加载且无重复、遗漏。
- 工单仅对管理员或当前指派工程师可见。
- 定位拒绝、相机拒绝、无网络和请求超时均有明确反馈。
- 签到、完工的离线任务在恢复网络后按顺序补传，重复提交由后端幂等号拦截。
- 现场照片和客户签字必须通过带 JWT 的附件接口访问。
- 备件领用、归还和报废使用实际库存权限，库存不足时后端拒绝提交。
- iOS、Android 微信真机分别验证定位、签字画布和图片上传。

## 5. 灰度与回滚

先发布给内部体验成员。确认审批、工单和库存数据与 PC 端一致后再提交正式审核。

小程序版本回滚在微信公众平台完成。后端 V85 增加移动端字段和表，V86 增加通知回执、审批申请人、认证版本和查询索引；两者均为向前增量迁移，但数据库不会随小程序版本自动回滚。停用微信登录可设置 `WECHAT_ENABLED=false`，账号密码登录仍可使用。
