# PetCare AI Web 前端

PetCare AI 的 Web 客户端使用 Vue 3、Vite 和 TypeScript 构建，面向养宠用户提供宠物档案、日常照护记录和消费管理等页面。整体采用简洁、明亮、卡片式布局。

项目级的后端、数据库、Docker 和环境变量说明请查看根目录 [README.md](../README.md)。

## 开发环境

- Node.js 24 LTS
- npm
- 正常运行的 PetCare 后端；涉及数据库的功能还需要后端可连接 MySQL

## 本地启动

在 `frontend` 目录执行：

```powershell
npm ci
npm run dev
```

Vite 默认把 `/api` 请求代理到 `http://localhost:8080`。如果后端地址不同，可以将 `frontend/.env.example` 复制为未纳入版本控制的 `frontend/.env.local`，并设置：

```dotenv
API_PROXY_TARGET=http://localhost:8080
```

启动后打开 Vite 在终端显示的本地地址。用户通过注册或登录获得 JWT；认证状态保存在当前浏览器标签页的 `sessionStorage` 中。刷新页面会尝试恢复当前会话，退出登录会清除会话。

前端只调用本地 PetCare API，不应保存数据库凭据、JWT 签名密钥或 AI Provider API Key。AI Provider 凭据由后端本地环境配置。

## 常用命令

```powershell
npm run dev
npm run build
npm run test:unit
npm run preview
```

- `npm run dev`：启动 Vite 开发服务器。
- `npm run build`：先执行 Vue/TypeScript 类型检查，再生成生产构建到 `frontend/dist/`。
- `npm run test:unit`：运行 Vitest 单元测试。
- `npm run preview`：预览已生成的生产构建。

## 项目结构

```text
src/
  api/          Axios 实例和后端接口封装
  components/   可复用组件
  layouts/      登录后主布局
  router/       页面路由与访问守卫
  stores/       Pinia 状态管理
  views/        页面
```

实际目录会随功能开发继续扩展。页面通过 Axios 请求后端，统一 API 前缀为 `/api`；JWT 由请求拦截器自动附加，认证失效由统一响应处理和路由守卫引导用户重新登录。
