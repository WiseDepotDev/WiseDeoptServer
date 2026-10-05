# 慧仓智控（WiseDeoptServer）

面向智能仓储场景的服务端：设备接入与心跳、巡检任务下发与结果回收、库存与标签管理、告警与站内消息、仪表盘统计。

## 技术栈

- Java 21 + Spring Boot 3.2（Maven 多模块）
- MySQL（JPA，实体映射见 `META-INF/orm.xml`）
- Redis（缓存、限流、未读数）
- MinIO（对象存储）、MQTT（设备下行指令）
- 统一信封协议（header / payload）与集中式错误码表

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `wise-deopt-api` | HTTP 入口：控制器、过滤器、拦截器、WebSocket、全局异常与响应包装 |
| `wise-deopt-application` | 应用服务：用例编排与请求 / 响应模型 |
| `wise-deopt-domain` | 领域模型与仓储接口 |
| `wise-deopt-infrastructure` | 基础设施实现：持久化、Redis 缓存、MQTT、MinIO、安全与请求签名 |
| `wise-deopt-common` | 通用契约：错误码、注解与工具 |
| `wise-depot-plan` | 配套数据：错误码对照表等 |
| `config` | 本地配置模板与 Checkstyle 规则 |
| `tools` | 生成器与校验脚本 |

## 环境要求

JDK 21、Maven 3.9+、MySQL 8、Redis 7；MinIO 与 MQTT Broker 按需部署。

## 配置

数据库、Redis、MinIO、JWT 与签名密钥等一律通过环境变量注入，源码内不保留默认凭据。本地开发可复制
`config/application-local.yml.example` 为 `config/application-local.yml`（已被 `.gitignore` 排除）并填入本机地址与凭据。

主要环境变量：`WISE_DB_URL` / `WISE_DB_USERNAME` / `WISE_DB_PASSWORD`、`WISE_REDIS_*`、`WISE_MINIO_*`、
`WISE_JWT_SECRET`、`WISE_API_SIGNATURE_SECRET`、`WISE_MQTT_*`、`WISE_CORS_ALLOWED_ORIGINS`。

## 构建与运行

```bash
mvn clean package          # 构建全部模块
mvn test                   # 单元测试与 WebMvc 切片测试
mvn -pl wise-deopt-api spring-boot:run   # 本地启动 API 模块
```

## 许可证

本项目采用 **AGPL-3.0** 许可证（见 `LICENSE`）：

- 使用源代码或其修改版本时，须遵守 AGPL-3.0 条款；
- 以网络部署或提供服务的方式使用本项目，须公开对应源代码；
- 严禁任何形式的商业用途，如需商业合作须事先取得作者书面授权。
