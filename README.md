# PetCare AI

PetCare AI 是面向普通养宠用户的宠物档案与日常照护管理 Web 应用。用户可以集中管理宠物资料、健康与预防护理记录、就医和消费信息，并在 AI 模块启用后向宠物助手提问。

## 当前功能

- 用户注册、登录、JWT 身份认证和当前用户信息
- 宠物档案的新增、列表、详情和用户间数据隔离
- 健康记录、体重记录与体重趋势
- 疫苗、驱虫和就医记录
- 消费记录、月度趋势、分类统计和宠物维度统计
- Dashboard 与即将到期提醒
- AI 对话模块骨架、OpenAI-compatible Provider 配置，以及第一个宠物档案查询工具 `getPetProfile`

AI 默认关闭。真实模型调用需要由开发者在本地提供 Provider 地址和 API Key；仓库不包含模型凭据。当前 AI Tool 只能读取当前登录用户的宠物基本档案，尚不能查询其他业务记录，也不执行数据写入。Fake ChatModel 测试覆盖 Tool Calling 与 MySQL 查询链路。

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 4.1.1、Spring Security、Spring AI 2.0.1、MyBatis-Plus Spring Boot 4 Starter |
| 数据库 | MySQL 8.4、Flyway；测试还使用 H2 和 Testcontainers |
| 前端 | Vue 3、Vite、TypeScript、Element Plus、Vue Router、Pinia、Axios |
| 开发环境 | Docker Compose；前端通过 Vite 将 `/api` 代理到后端 |

## 项目结构

```text
backend/                 Spring Boot API、Flyway migrations、后端测试
frontend/                Vue Web 应用、页面与前端测试
compose.yml              后端开发容器配置
docs/                    消费记录功能说明等补充文档
frontend/README.md       前端开发、代理与验证说明
.env.example             安全的环境变量模板
```

## 环境准备

- Java 21（本机运行后端时需要）
- Node.js 24 LTS 和 npm
- Docker Desktop（运行开发容器和 MySQL Testcontainers 测试时需要）
- MySQL 8.4 数据库，并确保后端运行环境可以连接它

Compose 配置只启动后端 API，不会创建 MySQL 容器。数据库可以由现有 Docker 开发环境提供。容器内连接宿主机上的 MySQL 时，`DB_URL` 通常应使用 `host.docker.internal`；若数据库运行在容器中，则应使用容器网络可访问的主机名和端口。

## 配置本地环境变量

根目录 `.env.example` 是安全模板，不含真实凭据。PowerShell 中可以只在目标文件不存在时复制，避免覆盖已有本地配置：

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
if (-not (Test-Path .env.docker.local)) { Copy-Item .env.example .env.docker.local }
```

Compose 使用根目录 `.env` 读取 AI 相关变量；后端容器从未纳入版本控制的 `.env.docker.local` 读取数据库和 JWT 配置。请在本地文件中填写：

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | JDBC 数据库地址，例如 `jdbc:mysql://host.docker.internal:3306/petcare?serverTimezone=Asia/Shanghai&characterEncoding=utf8` |
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET_BASE64` | Base64 编码的 JWT 签名密钥，解码后至少 32 字节 |
| `PETCARE_AI_ENABLED` | 是否启用 AI；默认为 `false` |
| `PETCARE_AI_BASE_URL` | OpenAI-compatible Provider 地址 |
| `DASHSCOPE_API_KEY` | 百炼 API Key；仅在本地设置，不要提交或粘贴到源码 |
| `PETCARE_AI_MODEL` | 模型名称，默认 `qwen3.7-plus` |

`.env`、`.env.*` 和本地 Docker 配置已由 `.gitignore` 排除；`.env.example` 会保留在 Git 中。不要将本地 Secret 写入源码、文档、Compose 文件或前端变量。

## 启动后端

### Docker Compose

确认 MySQL 已启动、`.env.docker.local` 已配置后，在项目根目录执行：

```powershell
docker compose up -d
docker compose logs -f api
```

后端地址为 `http://localhost:8080`，健康检查接口为 `GET /api/v1/health`。修改环境变量后需重新创建容器以加载新值：

```powershell
docker compose up -d --force-recreate
```

停止开发容器：

```powershell
docker compose down
```

### 本机运行

确保 Java 21、MySQL 和后端所需环境变量已经配置，然后：

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

## 启动前端

```powershell
Set-Location frontend
npm ci
npm run dev
```

Vite 默认将 `/api` 请求代理到 `http://localhost:8080`。如后端使用其他地址，可在 `frontend/.env.local` 中设置 `API_PROXY_TARGET`。开发服务器启动后，使用终端输出的本地地址访问 Web 应用。

构建和前端单元测试：

```powershell
npm run build
npm run test:unit
```

## 后端测试

在 `backend` 目录运行：

```powershell
.\mvnw.cmd test
```

MySQL 集成测试通过 Testcontainers 启动 MySQL 8.4，因此测试需要 Docker 正常运行。AI 测试使用 Fake/Stub 模型，不要求百炼凭据，也不会默认调用真实模型。

## API 概览

除注册、登录和健康检查外，API 请求需要携带登录后获得的 Bearer JWT。数据接口按当前用户校验资源归属。

| 功能 | 路由 |
| --- | --- |
| 健康检查 | `GET /api/v1/health` |
| 注册、登录 | `POST /api/auth/register`、`POST /api/auth/login` |
| 当前用户 | `GET /api/users/me` |
| 宠物档案 | `/api/pets`、`/api/pets/{petId}` |
| 健康记录 | `/api/pets/{petId}/health-records` |
| 体重记录 | `/api/pets/{petId}/weight-records` |
| 疫苗 | `/api/pets/{petId}/vaccinations` |
| 驱虫 | `/api/pets/{petId}/dewormings` |
| 就医记录 | `/api/pets/{petId}/medical-records` |
| 消费记录与统计 | `/api/expenses` 及其 `/statistics/*` 路由 |
| 即将到期提醒 | `GET /api/reminders/upcoming` |
| AI 对话 | `POST /api/ai/chat` |

各记录资源提供与业务对应的查询、新增、更新或删除接口；具体请求字段以 Controller DTO 和前端 API 封装为准。

## 数据库迁移

Flyway 迁移位于 `backend/src/main/resources/db/migration/`，按 MySQL 和 H2 分目录维护。已经应用的迁移不应改写；数据库结构变更请追加新的版本迁移。

## 提交约定

建议使用以下提交前缀：`feat:`、`fix:`、`test:`、`docs:`、`refactor:`、`chore:`。
