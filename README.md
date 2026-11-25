# 校园 Agent 平台 MCP 网关管理后台

校园 Agent 平台用于注册和管理 MCP Server、工具原语与调用审计，并通过 JWT 和租户上下文隔离每个租户的数据。

## 架构关系

```text
浏览器（登录页 / Server / Tool / Invocation 页面）
  -> Vue 3 + Element Plus + Axios
  -> 开发模式：Vite /api 代理到 127.0.0.1:8080
  -> Docker模式：Nginx /api 代理到 backend:8080

Spring Boot 3.5 后端
  -> Controller：HTTP 路由、参数校验、统一响应
  -> Security：JWT / ADMIN 写权限 / USER 读权限
  -> Service：租户业务编排、缓存失效、连通性与工具调用
  -> Caffeine：Server/Tool 分页查询本地缓存
  -> MyBatis-Plus Mapper：按 tenant_id 隔离的 SQL 与分页
  -> PostgreSQL：mcp_server / mcp_tool / mcp_invocation / mcp_user

/internal/v1/**
  -> 独立内部服务密钥拦截器
  -> Tool Execution：mock 或 HTTP 实际下游调用
```

后端分层为：`Controller -> Service -> Mapper -> Entity/DTO/Response`。缓存位于管理Service，key包含租户和查询条件；写操作成功后按租户失效。统一响应为`{ code, message, data }`，业务异常由全局异常处理器转换。

## 技术栈

- 后端：Spring Boot 3.5、Java 25、MyBatis-Plus、PostgreSQL 17、Maven
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router
- 数据库容器：Docker Compose + PostgreSQL 17 官方镜像

## 快速开始 TL;DR

准备 PostgreSQL 二选一。

### A. Docker 启动完整栈

```bash
cp .env.example .env
docker compose up -d --build
```

Windows PowerShell 可使用：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

服务启动顺序为 PostgreSQL 健康检查通过后启动 backend，backend 健康检查通过后启动 frontend。访问 `http://127.0.0.1`（或 `.env` 中的 `FRONTEND_PORT`），后端公开在 `http://127.0.0.1:8080`，PostgreSQL公开在 `5432`。

可复制 `.env.example` 为 `.env`；`.env` 不会提交到 Git。Docker Compose 会读取 `.env`，后端容器通过环境变量读取 `DB_*` 和 `MCP_*`，不会写入镜像。

### B. 本机 PostgreSQL

连接本机 PostgreSQL 后手动创建数据库：

```sql
CREATE DATABASE campus_agent;
```

数据库准备好后启动后端：

```bash
# macOS/Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

本地直接启动后端时，需要在 IDE Run Configuration 或终端导出环境变量：

PowerShell：

```powershell
$env:DB_URL="jdbc:postgresql://127.0.0.1:5432/campus_agent"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:MCP_INTERNAL_REQUIRE_SERVICE_KEY="false"
$env:MCP_EXECUTION_MODE="mock"
```

macOS/Linux：

```bash
export DB_URL="jdbc:postgresql://127.0.0.1:5432/campus_agent"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
export MCP_INTERNAL_REQUIRE_SERVICE_KEY="false"
export MCP_EXECUTION_MODE="mock"
```

再启动前端：

```bash
cd frontend
npm install
npm run dev
```

访问 `http://127.0.0.1:5173`。

## 现有接口

认证接口：

- `POST /api/v1/auth/login`：登录并返回JWT。
- `GET /api/v1/auth/me`：读取当前用户、角色和租户。

MCP管理接口：

- `GET /api/v1/mcp/servers`：分页查询Server。
- `POST /api/v1/mcp/servers`：注册Server。
- `GET /api/v1/mcp/servers/{serverId}`：Server详情。
- `PUT /api/v1/mcp/servers/{serverId}`：编辑Server。
- `DELETE /api/v1/mcp/servers/{serverId}`：逻辑删除Server及其工具。
- `POST /api/v1/mcp/servers/{serverId}/connectivity-tests`：连通性测试。
- `GET /api/v1/mcp/tools`：分页查询Tool。
- `GET /api/v1/mcp/tools/{toolId}`：Tool详情。
- `PATCH /api/v1/mcp/tools/{toolId}/status`：工具上线或下线。
- `GET /api/v1/mcp/invocations`：调用审计分页查询。

内部接口：

- `POST /internal/v1/mcp/tools/calls`：内部工具调用，需要内部服务密钥和可信租户上下文。
- `GET /actuator/health`：容器健康检查，仅暴露health端点。

默认开发账号：`admin` / `admin123`。生产profile不初始化该账号；生产密码和JWT密钥必须外部配置。

## 本地启动

要求JDK25、Node18+、PostgreSQL17可用：

```bash
CREATE DATABASE campus_agent;
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend
npm ci
npm run dev
```

Windows下使用`mvnw.cmd`；开发前端地址为`http://127.0.0.1:5173`，后端为`http://127.0.0.1:8080`。

## Docker启动

```bash
Copy-Item .env.example .env
docker compose up -d --build
```

Linux/macOS可使用`cp .env.example .env`。前端默认访问`http://127.0.0.1`，后端健康检查访问`/actuator/health`。停止容器使用`docker compose down`；保留PostgreSQL卷时不要加`-v`。

## 前置环境

| 环境 | 要求 | 校验命令 |
|---|---|---|
| JDK | 25 | `java -version` |
| Maven | 3.9+，推荐使用仓库内 Wrapper，无需预装 | `./mvnw -v` 或 `mvnw.cmd -v` |
| Node.js | 18+ | `node -v` |
| npm | 与 Node 配套版本 | `npm -v` |
| PostgreSQL | 17，或使用 Docker Compose | `docker --version` |

Windows 使用 `mvnw.cmd`；macOS/Linux 使用 `./mvnw`。首次使用时 Wrapper 会下载配置的 Maven 版本。

## 数据库准备

默认连接参数与项目配置一致：

| 配置项 | 默认值 | 配置来源 |
|---|---|---|
| JDBC URL | `jdbc:postgresql://127.0.0.1:5432/campus_agent` | `application.yml` |
| 端口 | `5432` | PostgreSQL / Docker Compose |
| 数据库名 | `campus_agent` | Compose 或手动创建 |
| 用户名 | `postgres` | `application.yml` / Compose |
| 密码 | `postgres` | `DB_PASSWORD`，默认 postgres |

开发 profile 启动时会执行：

- `schema.sql`：建表和索引
- `schema-dev.sql`：灌入开发 Server、Tool、Invocation 种子

全新空库在 `dev` profile 下启动时会自动建表并灌种子，无需先手动建表或先执行种子脚本。

生产 profile 不自动建表、不加载种子。部署生产环境前，应使用受控迁移流程手动执行 `src/main/resources/schema.sql`。

项目中有 5 个 PostgreSQL Testcontainers 集成测试；运行时需要 Docker Desktop。若 Docker 不可用，这些测试会因 `disabledWithoutDocker = true` 自动跳过，而不是连接本机数据库。

## 环境变量对照表

| 变量名 | 作用 | 必填 | 默认值 | 示例 |
|---|---|---|---|---|
| `DB_URL` | 后端 PostgreSQL JDBC URL | 可选 | `jdbc:postgresql://127.0.0.1:5432/campus_agent` | `jdbc:postgresql://127.0.0.1:5432/campus_agent` |
| `DB_USERNAME` | 后端和 Compose 的 PostgreSQL 用户名 | 可选 | `postgres` | `postgres` |
| `DB_NAME` | Compose 创建的 PostgreSQL 数据库名 | 可选 | `campus_agent` | `campus_agent` |
| `DB_PASSWORD` | PostgreSQL 密码 | 可选 | `postgres` | `change-me` |
| `POSTGRES_PORT` | Compose 发布的 PostgreSQL 主机端口 | 可选 | `5432` | `5432` |
| `BACKEND_PORT` | Compose 发布的后端主机端口 | 可选 | `8080` | `8080` |
| `FRONTEND_PORT` | Compose 发布的前端主机端口 | 可选 | `80` | `8081` |
| `SPRING_PROFILES_ACTIVE` | 后端容器 Spring profile | 可选 | `dev` | `dev` |
| `MCP_INTERNAL_SERVICE_KEY` | 内部工具调用服务密钥 | 生产部署建议必填 | 空 | `<strong-random-key>` |
| `MCP_INTERNAL_REQUIRE_SERVICE_KEY` | dev 是否校验内部服务密钥 | 可选 | `false`（dev） | `true` |
| `MCP_JWT_SECRET` | JWT HMAC 签名密钥；生产环境必须使用高熵值 | 生产必填，dev可选 | dev 默认值，仅本地使用 | `replace-with-a-long-random-secret` |
| `MCP_JWT_EXPIRATION` | JWT 有效期，单位毫秒 | 可选 | `3600000`（1小时） | `3600000` |
| `MCP_EXECUTION_MODE` | 工具执行模式 | 可选 | dev：`mock`，prod：`http` | `mock` |
| `MCP_CONNECTIVITY_CONNECT_TIMEOUT` | 连通性连接超时 | 可选 | `PT2S` | `PT5S` |
| `MCP_CONNECTIVITY_READ_TIMEOUT` | 连通性读取超时 | 可选 | `PT5S` | `PT10S` |
| `MCP_EXECUTION_CONNECT_TIMEOUT` | 工具执行连接超时 | 可选 | `PT2S` | `PT5S` |
| `MCP_EXECUTION_CALL_TIMEOUT` | 工具调用超时 | 可选 | `PT10S` | `PT20S` |
| `MCP_CACHE_SERVER_TTL` | Server分页查询缓存TTL | 可选 | `5m` | `10m` |
| `MCP_CACHE_SERVER_MAX_SIZE` | Server分页查询缓存最大条数 | 可选 | `1000` | `2000` |
| `MCP_CACHE_TOOL_TTL` | Tool分页查询缓存TTL | 可选 | `5m` | `10m` |
| `MCP_CACHE_TOOL_MAX_SIZE` | Tool分页查询缓存最大条数 | 可选 | `1000` | `2000` |

可复制 `.env.example` 为 `.env`，但 `.env` 不会提交到 Git。Docker Compose 会读取 `.env`；Spring Boot 不会自动读取 `.env`，本地直接运行后端时需要在 IDE 或终端导出变量。

PowerShell：

```powershell
$env:DB_PASSWORD="change-me"; $env:DB_USERNAME="postgres"
```

bash/zsh：

```bash
export DB_PASSWORD=change-me DB_USERNAME=postgres
```

上述变量只在当前终端会话或由该终端启动的进程中生效。若从 IDE 启动后端，请在 IDE 的运行配置中填写对应环境变量。

## MCP 查询缓存设计

Server和Tool分页查询使用进程内Caffeine缓存，不引入Redis等外部存储。缓存粒度为一次分页查询结果：

- Server key：租户ID、状态、健康状态、页码、每页条数。
- Tool key：租户ID、分类、作用域、状态、关键字、页码、每页条数。
- 默认TTL为5分钟，每类缓存最大1000条；可通过上述`MCP_CACHE_*`环境变量调整。

注册、更新、删除和连通性健康更新成功后，失效当前租户的Server缓存；工具上下线成功后失效当前租户的Tool缓存。删除Server会同时失效Server和Tool缓存，因为该Server下的工具随之逻辑删除。

缓存采用写后主动失效的最终一致策略：缓存只加速管理查询，数据库仍是事实来源。并发写入和失效之间可能短暂返回旧页、旧状态或旧总数；缓存TTL到期后会重新回源。未执行主动失效的外部数据库变更也可能最多保留一个TTL；跨租户不会共享缓存条目。

`DB_URL` 中的数据库路径名要与 `DB_NAME` 保持一致。例如使用 `campus_agent` 时：

```text
DB_NAME=campus_agent
DB_URL=jdbc:postgresql://127.0.0.1:5432/campus_agent
```

修改数据库名时，必须同时修改 `DB_NAME` 和 `DB_URL` 的路径部分。

## 后端常用命令

```bash
# 开发 profile 本地运行
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 打包（跳过测试）
./mvnw package -DskipTests

# 运行打包后的 Jar
java -jar target/mcp-gateway-data-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 运行测试
./mvnw test

# 完整验证
./mvnw clean verify
```

Windows 将 `./mvnw` 替换为 `mvnw.cmd`。

## 前端常用命令

```bash
cd frontend
npm install
npm run dev
npm run build
```

Vite 开发端口固定为 `5173`，并将 `/api`代理到 `http://127.0.0.1:8080`。

## 已知限制

- `MCP_EXECUTION_MODE=mock`为开发/演示默认模式；它只模拟连通性和工具调用，不代表真实下游服务可用。
- HTTP执行模式需要自行配置真实MCP下游；没有内置下游发现、熔断、重试、限流或多实例负载均衡。
- JWT实现是基础登录与角色闭环；当前未包含刷新令牌、用户管理、密码重置、登录失败锁定、审计告警和密钥轮换。
- 当前管理接口按JWT角色区分读写，但生产租户仍依赖可信网关注入上下文；客户端不能自行设置可信租户身份。
- Caffeine为进程内缓存；多实例部署时各实例缓存独立，写操作只失效当前实例，外部数据库变更可能短暂不一致。
- PostgreSQL是单实例Compose设计，未提供生产级备份、主从、迁移框架、密钥管理或TLS配置。
- 前端按需加载Element Plus组件，模板组件由Vite构建期解析；脚本服务仍直接按需导入`ElMessage`等服务。

## 常见问题

- **端口被占用**：检查 `8080`、`5173`、`5432`。停止占用进程或修改对应服务端口后同步更新配置。
- **JDK 不是 25**：项目编译目标为 Java 25，旧 JDK 启动或编译会报版本错误。切换到 JDK 25 后重启终端和 IDE。
- **种子数据没出现**：确认以 `dev` profile 启动、数据库账号可访问、启动日志执行了 `schema.sql` 和 `schema-dev.sql`，必要时重启后端。全新空库应在首次启动时自动建表并灌种子。
- **表已存在冲突**：`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`、列和索引使用幂等语句；`schema-dev.sql` 的 Server/Tool/Invocation 插入均有重复检查。
- **mock 模式不是真实连通性**：dev 默认 `MCP_EXECUTION_MODE=mock`。连通性测试和健康状态为模拟结果，不能证明真实 MCP 下游可达；切换到真实执行需要自行配置下游并使用对应 HTTP 执行模式。

## Docker 镜像与健康检查

基础镜像选型：

- 后端构建：`maven:3.9.11-eclipse-temurin-25-alpine`
- 后端运行：`eclipse-temurin:25-jre-alpine`
- 前端构建：`node:20-alpine`
- 前端运行：`nginx:1.27-alpine`
- 数据库：`postgres:17`

容器健康端点只暴露 `health`，不暴露敏感 Actuator端点：

```bash
curl http://127.0.0.1:8080/actuator/health
docker compose ps
docker compose logs -f backend frontend postgres
```

停止并清理容器及匿名网络：

```bash
docker compose down
```

需要保留数据库卷时不要执行 `docker compose down -v`。

以下环节当前环境无法替代真实 Docker Desktop 验证，人工需在 Docker 环境中确认：

- JDK25/Maven Wrapper 多阶段构建和 JRE25启动。
- PostgreSQL、backend、frontend 跨容器 DNS 与健康检查启动顺序。
- `POSTGRES_PORT`、`BACKEND_PORT`、`FRONTEND_PORT` 自定义端口映射。
- 首次启动时 `schema.sql`/`schema-dev.sql` 自动建库、建表和灌种子。
- 浏览器访问前端时 `/api` 是否经 nginx 代理到 backend。
- JWT 登录、`/api/v1/auth/me` 和ADMIN写权限在容器网络中的行为。

## 登录与认证

开发 profile 初始化一个默认管理员账号：

- 用户名：`admin`
- 密码：`admin123`
- 角色：`ADMIN`

登录接口为 `POST /api/v1/auth/login`，登录成功返回 `accessToken`；当前用户接口为 `GET /api/v1/auth/me`，请求头使用 `Authorization: Bearer <accessToken>`。
生产 profile 不加载默认 admin 账号。密码只以 BCrypt hash 存入 `mcp_user.password_hash`，不要提交明文密码或真实 JWT 密钥。

## 主要接口

管理接口位于 `/api/v1/mcp`：

- `/servers`：Server 注册、分页查询、详情、编辑、删除和连通性测试
- `/tools`：工具分页查询、详情和上下线状态变更
- `/invocations`：调用审计分页查询

内部工具调用接口位于：

- `/internal/v1/mcp/tools/calls`

内部调用需使用服务端配置的内部服务密钥，并由可信网关提供租户上下文。

## 页面功能

- MCP Server 管理：注册、编辑、删除、连通性测试和分页
- MCP 工具管理：分类、作用域、状态筛选、分页、详情抽屉和上下线
- 调用记录：状态、错误码、时间、耗时列展示和分页

## 租户上下文

`X-Tenant-ID` 仅用于本地开发联调，由前端 Vite dev 代理注入；前端生产构建和 Axios 不携带该请求头。
生产环境租户必须由可信网关注入后端请求上下文，不应由客户端或请求参数指定。
