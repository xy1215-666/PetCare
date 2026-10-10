# PetCare AI Web

Vue 3 + Vite + TypeScript 前端。开发环境通过 Vite 将 `/api` 请求代理至 Spring Boot。

## 本地开发

1. 使用 Node.js 24 LTS 和 npm。
2. 复制 `.env.example` 为 `.env.local`，按需修改 `API_PROXY_TARGET`。
3. 安装依赖并启动后端服务。
4. 运行 `npm ci`，然后运行 `npm run dev`。

生产构建：`npm run build`。构建文件输出到 `dist/`。
