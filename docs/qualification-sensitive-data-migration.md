# 资质敏感资料迁移

证件、身份证明和历史导入清单不得放入前端 `public`、构建产物、Git 仓库或应用 JAR。仓库中的旧资料已移至本机忽略目录 `.local-data/quarantine/`，该目录只用于迁移，不能作为长期共享存储。

## 迁移到私有对象存储

1. 安装 `mc` 和 `jq`，并设置 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET`。
2. 执行 `scripts/migrate-qualification-assets.sh`。脚本会关闭 bucket 匿名访问、上传到 `qualification/legacy/`，并生成权限为 `600` 的 `.local-data/qualification-import.private.json`。
3. 在受控维护窗口内仅启动一次 API：`QUALIFICATION_IMPORT_ENABLED=true QUALIFICATION_IMPORT_FILE=.local-data/qualification-import.private.json npm run api:dev`。
4. 检查资质附件只能通过已认证的 `/api/qualification-files/**` 获取，然后关闭导入开关并删除迁移机上的临时清单和隔离副本。

生产环境应保留私有 bucket、短时预签名链接、访问日志和备份。旧 Git 历史仍可能包含敏感数据，必须在团队停写、备份并通知所有协作者后，使用历史重写工具统一清除并轮换远端引用；本次常规代码改动不自动重写共享历史。
