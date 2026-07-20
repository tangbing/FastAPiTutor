# RBAC Lab Android

一个配套 `projects/rbac_project_api` 的原生 Android 学习项目。它不只是给接口套一层列表，而是把 RBAC 的结果直接呈现在界面上：不同角色会看到不同的数据范围、导航入口、可用按钮和 HTTP 状态反馈。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- 单 Activity、单向 `StateFlow` 状态管理
- OkHttp + `org.json`，便于直接观察请求格式和 Bearer Token
- ViewModel + Repository + API Client 分层
- Gradle 8.11.1、AGP 8.10.1、Java 17、compileSdk 36

## 接口覆盖

| 页面 | 接口 | UI 中的权限表现 |
| --- | --- | --- |
| 登录 | `GET /health` | 显示 API 在线/离线 |
| 首次启动 | `POST /auth/bootstrap` | 已有用户时展示 409 |
| 登录 | `POST /auth/token` | OAuth2 表单登录并保存 JWT |
| 权限中心 | `GET /auth/me` | 显示当前账号、角色和能力清单 |
| 团队 | `GET /users` | 仅 admin、manager 出现团队入口 |
| 团队 | `POST /users` | 仅 admin 解锁创建按钮 |
| 项目工作台 | `GET /projects` | admin 看全部，其他角色只看拥有/加入的项目 |
| 项目详情 | `GET /projects/{id}` | 不可见资源由服务端返回 404 |
| 新建项目 | `POST /projects` | 仅 admin、manager 解锁 |
| 项目详情 | `PATCH /projects/{id}` | 仅 admin、项目所有者解锁 |
| 项目成员 | `GET /projects/{id}/members` | 可见项目均可读取 |
| 添加成员 | `POST /projects/{id}/members` | 仅 admin、项目所有者解锁，重复添加展示 409 |

“权限”页面保留最近 30 条接口记录，直接展示方法、路径和 HTTP 状态，适合对照 2xx、401、403、404、409、422 的含义。

## 运行后端

从仓库根目录执行：

```bash
cd projects/rbac_project_api
export RBAC_SECRET_KEY="replace-with-a-long-random-secret"
.venv/bin/python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8005
```

确认电脑浏览器能打开 `http://127.0.0.1:8005/docs`。

## 构建 Android App

```bash
cd projects/rbac_android_app
./gradlew :app:assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

已连接设备时可以安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## API 地址

登录页可以直接修改 Base URL：

- Android 模拟器：`http://10.0.2.2:8005`
- Android 真机：`http://电脑的局域网IP:8005`

真机和电脑需要位于同一局域网，后端必须使用 `--host 0.0.0.0`。

## 推荐练习流程

1. 清空测试数据库后，用 App 初始化 admin。
2. admin 登录，创建一个 manager 和一个 member。
3. admin 创建项目并添加 member，观察管理员能看到全部项目。
4. manager 登录并创建自己的项目，观察只能管理自己拥有的项目。
5. member 登录，观察“团队”入口消失、“新建项目”和管理操作变成锁定提示。
6. 回到“权限”页面，对照每次操作产生的接口状态。

## 项目结构

```text
app/src/main/java/com/edy/rbaclab/
├── data/                 # API 模型、OkHttp 客户端、Repository、Token 存储
├── domain/               # 客户端权限展示规则
├── ui/components/        # 通用状态、角色、权限组件
├── ui/screens/           # 登录、项目、成员、团队、权限页面
├── ui/theme/             # Material 3 主题
├── MainActivity.kt
└── RbacViewModel.kt
```

## 学习项目与生产实现的区别

为了让网络流程容易阅读，本项目使用 SharedPreferences 保存 Token，并允许 HTTP 明文访问本地服务器。生产应用应使用 HTTPS，并根据安全需求改用 Android Keystore 支持的凭据存储方案。
