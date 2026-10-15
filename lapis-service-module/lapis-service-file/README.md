# lapis-service-file（文件服务）

## 当前状态：⚠️ 未接入主链路，请勿直接对外提供服务

该模块目前处于**半成品**状态，2026-10 代码走查时确认存在以下缺口，尚未一并改造
（因为改动需要同步调整网关与配置中心，且本机无 JDK/Maven 无法做编译与启动验证）：

| 缺口 | 说明 |
|---|---|
| 未注册网关路由 | `lapis-service-config/src/main/resources/dev/service-gateway-dev.yml` 的 `routes` 中只有 uaa / admin / eventlog / toolbox 四条，**没有 service-file**，因此外部无法经 `:8700` 访问到本服务 |
| 未接入配置中心 | 本模块使用本地 `src/main/resources/application.yml`，而不是 `bootstrap.yml`，不参与 Spring Cloud Config 的配置下发 |
| 未接入统一鉴权 | `pom.xml` 未引入 `lapis-security-spring-boot-starter`，接口当前**不做任何认证与鉴权** |

在补齐上述三项之前，即使把服务启动起来，也**不应该**在网关暴露它的路由 ——
否则等于对外提供一个未鉴权的文件上传/下载端点。

## 已完成的加固

- MinIO 的 `accessKey` / `secretKey` 已从明文改为环境变量占位：
  `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`，不再随仓库泄露。

## 接入清单（后续若要启用）

1. `pom.xml` 增加 `lapis-security-spring-boot-starter`、`lapis-web-spring-boot-starter` 依赖；
2. 把 `application.yml` 改名为 `bootstrap.yml`，按其他服务的方式声明 `spring.cloud.config.discovery`；
3. 在 `lapis-service-config/src/main/resources/dev/` 下新增 `service-file-dev.yml`，
   至少包含 `spring.security.oauth2.resourceserver.opaquetoken.*` 与 MinIO 配置；
4. 在 `service-gateway-dev.yml` 的 `routes` 中追加：
   ```yaml
   - id: service-file
     uri: lb://service-file
     predicates:
       - Path=/api/file/**
     filters:
       - StripPrefix=2
   ```
5. 在 README 的"启动后端微服务"步骤中补上本服务的启动顺序。
