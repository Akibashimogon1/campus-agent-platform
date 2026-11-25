# 校园 Agent 平台 MCP 网关管理后台

校园 Agent 平台的 MCP 网关管理后台，用于管理 MCP Server、工具原语和调用审计记录。

## 技术栈

- 后端：Spring Boot 3.5、Java 25、MyBatis-Plus、PostgreSQL 17、Maven
- 前端：Vue 3、Vite、Element Plus、Axios、Vue Router
- 数据库容器：Docker Compose + PostgreSQL 17 官方镜像

## 快速开始 TL;DR

准备 PostgreSQL 二选一。

### A. Docker 启动数据库

```bash
docker compose up -d
```

`.env` 只会被 Docker Compose 自动读取，不会被 Spring Boot 自动读取；本项目没有 dotenv 依赖。

Compose 只启动 PostgreSQL，后端和前端仍在本机启动。

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
| `MCP_INTERNAL_SERVICE_KEY` | 内部工具调用服务密钥 | 生产部署建议必填 | 空 | `<strong-random-key>` |
| `MCP_INTERNAL_REQUIRE_SERVICE_KEY` | dev 是否校验内部服务密钥 | 可选 | `false`（dev） | `true` |
| `MCP_EXECUTION_MODE` | 工具执行模式 | 可选 | dev：`mock`，prod：`http` | `mock` |
| `MCP_CONNECTIVITY_CONNECT_TIMEOUT` | 连通性连接超时 | 可选 | `PT2S` | `PT5S` |
| `MCP_CONNECTIVITY_READ_TIMEOUT` | 连通性读取超时 | 可选 | `PT5S` | `PT10S` |
| `MCP_EXECUTION_CONNECT_TIMEOUT` | 工具执行连接超时 | 可选 | `PT2S` | `PT5S` |
| `MCP_EXECUTION_CALL_TIMEOUT` | 工具调用超时 | 可选 | `PT10S` | `PT20S` |

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

## 常见问题

- **端口被占用**：检查 `8080`、`5173`、`5432`。停止占用进程或修改对应服务端口后同步更新配置。
- **JDK 不是 25**：项目编译目标为 Java 25，旧 JDK 启动或编译会报版本错误。切换到 JDK 25 后重启终端和 IDE。
- **种子数据没出现**：确认以 `dev` profile 启动、数据库账号可访问、启动日志执行了 `schema.sql` 和 `schema-dev.sql`，必要时重启后端。全新空库应在首次启动时自动建表并灌种子。
- **表已存在冲突**：`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`、列和索引使用幂等语句；`schema-dev.sql` 的 Server/Tool/Invocation 插入均有重复检查。
- **mock 模式不是真实连通性**：dev 默认 `MCP_EXECUTION_MODE=mock`。连通性测试和健康状态为模拟结果，不能证明真实 MCP 下游可达；切换到真实执行需要自行配置下游并使用对应 HTTP 执行模式。

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
