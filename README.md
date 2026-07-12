# FastAPI Starter API

这是一个面向零 Python 经验开发者的 FastAPI 学习仓库。主教程以 Flutter/Dart 作为对照，从任务 CRUD 开始，逐步进入权限和事务型业务；默认使用 SQLite，也可以切换到 PostgreSQL。

第一次学习请从 [`FASTAPI_TUTORIAL.md`](FASTAPI_TUTORIAL.md) 开始，不要直接跳到 JWT 或订单事务。完成任务项目及其动手题后，再进入 [`PROJECTS_GUIDE.md`](PROJECTS_GUIDE.md)。

## 三阶段学习项目

| 阶段 | 项目 | 重点 |
| --- | --- | --- |
| 1 | 根目录任务 CRUD | 路由、参数校验、SQLModel、依赖注入、测试 |
| 2 | `projects/rbac_project_api` | JWT 登录、密码哈希、RBAC、资源权限、服务层 |
| 3 | `projects/order_inventory_api` | 订单事务、库存锁、幂等键、金额快照、库存流水 |

两个实战项目的学习顺序、启动命令和练习任务见 [`PROJECTS_GUIDE.md`](PROJECTS_GUIDE.md)。

## 快速开始

```bash
python3.12 --version
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install -U pip
python -m pip install -e ".[dev]"
fastapi dev app/main.py
```

打开：

- http://127.0.0.1:8000/health
- http://127.0.0.1:8000/docs
- http://127.0.0.1:8000/redoc

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 健康检查 |
| POST | `/tasks` | 创建任务 |
| GET | `/tasks` | 查询任务列表 |
| GET | `/tasks/{task_id}` | 查询单个任务 |
| PATCH | `/tasks/{task_id}` | 更新任务 |
| DELETE | `/tasks/{task_id}` | 删除任务 |

## 测试

```bash
pytest
```

## 切换数据库

默认使用：

```text
sqlite:///./app.db
```

切换 PostgreSQL：

```bash
python -m pip install "psycopg[binary]"
export DATABASE_URL="postgresql+psycopg://user:password@localhost:5432/fastapi_demo"
fastapi dev app/main.py
```
