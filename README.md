# 课安

校园临时代课互助平台。当前完成 Phase 3：申请代课、选人、双方确认、完成与状态机。

## 部署关系

- 

## 启动

后端（先启动，连虚拟机数据库）：

```bash
cd kean
mvn -DskipTests spring-boot:run
```

启动时会自动读取仓库根目录 `.env`。也可执行 `kean/run.ps1`。

用户端 H5：

```bash
cd uni-kean
npm install
npm run dev:h5
```

浏览器打开 `http://localhost:5173/`。H5 开发代理到本机 `http://127.0.0.1:8080`。

手机 App 使用主机 WLAN 地址：

`

管理端：

```bash
cd web-kean
npm install
npm run dev
```

浏览器打开 `http://localhost:5174/login`。管理员默认 `admin` / `ChangeMe_Admin_123`。

接口说明：`docs/api/auth.md`、`docs/api/dict.md`、`docs/api/tasks.md`、`docs/api/applications.md`  
建表 SQL：`docs/sql/V1__init_base_tables.sql`  
课程种子：`docs/sql/V2__seed_courses.sql`
