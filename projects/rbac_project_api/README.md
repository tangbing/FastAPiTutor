# RBAC Project Management API

一个用于学习企业权限体系的 FastAPI 项目。详细学习步骤见仓库根目录的 `PROJECTS_GUIDE.md`。

## 启动

```bash
export RBAC_SECRET_KEY="replace-with-a-long-random-secret"
python -m uvicorn projects.rbac_project_api.app.main:app --reload --port 8001
```

可用配置：

| 环境变量 | 默认值 |
| --- | --- |
| `RBAC_DATABASE_URL` | `sqlite:///./rbac_project.db` |
| `RBAC_SECRET_KEY` | 仅用于本地开发的默认密钥 |
| `RBAC_ACCESS_TOKEN_EXPIRE_MINUTES` | `30` |

## 主要接口

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| POST | `/auth/bootstrap` | 仅数据库无用户时可用 |
| POST | `/auth/token` | 公开 |
| GET | `/auth/me` | 已登录 |
| POST | `/users` | admin |
| GET | `/users` | admin、manager |
| POST | `/projects` | admin、manager |
| GET | `/projects` | 按用户可见范围过滤 |
| PATCH | `/projects/{id}` | admin、项目负责人 |
| POST | `/projects/{id}/members` | admin、项目负责人 |

生产环境必须设置独立强密钥，并用迁移工具创建数据库结构。
