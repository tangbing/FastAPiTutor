# FastAPI 企业实战项目指南

这两个项目对应主教程的阶段二和阶段三，不是简单地再写两遍 CRUD，而是分别训练企业后台最常见的两类难点：认证授权和事务型业务。它们都能独立运行，默认使用 SQLite，切换 PostgreSQL 时只需修改数据库连接并安装驱动。

如果没有 Python 开发经验，请先完成 [`FASTAPI_TUTORIAL.md`](FASTAPI_TUTORIAL.md) 的第 1 到 10 章，确保已经能修改任务 CRUD 并运行测试，再继续本指南。

## 安装

在仓库根目录执行：

```bash
python3.12 --version
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install -U pip
python -m pip install -e ".[dev]"
```

## 项目一：RBAC 项目协作后台

目录：`projects/rbac_project_api`

这是一个团队项目管理接口，包含管理员、项目经理和普通成员三种角色。它覆盖 JWT 登录、Argon2 密码哈希、角色权限、项目归属权限、成员可见范围、分层目录和请求追踪 ID。

启动：

```bash
python -m uvicorn projects.rbac_project_api.app.main:app --reload --port 8001
```

打开 `http://127.0.0.1:8001/docs`，建议按下面顺序操作：

1. 调用 `POST /auth/bootstrap` 创建唯一的初始管理员。
2. 在 Swagger UI 点击 Authorize，填写管理员用户名和密码；Swagger 会自动调用 `POST /auth/token`。
3. 管理员调用 `POST /users` 创建 manager 和 member。
4. manager 调用 `POST /projects` 创建项目。
5. 项目负责人调用 `POST /projects/{id}/members` 添加成员。
6. 分别使用不同账号验证项目列表和修改权限。

核心阅读顺序：

1. `app/models.py`：表模型和请求/响应模型为什么分开。
2. `app/security.py`：密码只存哈希，JWT 只存用户标识和过期时间。
3. `app/dependencies.py`：认证和角色判断如何成为可复用依赖。
4. `app/services/project_service.py`：资源级权限为什么不能只靠角色判断。
5. `tests/test_projects.py`：如何替换数据库依赖并测试完整权限链路。

## 项目二：订单与库存后台

目录：`projects/order_inventory_api`

这是一个小型交易后台，覆盖商品、人工库存调整、下单、库存扣减、订单取消和库存恢复。金额使用整数分，订单项保存成交价快照；创建订单必须提供幂等键，重复请求不会重复扣库存。

启动：

```bash
python -m uvicorn projects.order_inventory_api.app.main:app --reload --port 8002
```

打开 `http://127.0.0.1:8002/docs`，建议按下面顺序操作：

1. 调用 `POST /products` 创建两个商品并设置初始库存。
2. 调用 `POST /products/{id}/stock-adjustments` 模拟入库或盘点。
3. 调用 `POST /orders`，在 `Idempotency-Key` 请求头填写唯一值。
4. 使用相同请求体和相同幂等键再次请求，观察订单 ID 和库存不变。
5. 使用相同幂等键提交不同请求体，观察 `409` 冲突。
6. 调用 `POST /orders/{id}/cancel`，观察库存恢复且重复取消不会重复入库。

核心阅读顺序：

1. `app/models.py`：金额、状态、价格快照、库存流水和幂等记录如何建模。
2. `app/services/order_service.py`：一次提交如何涵盖订单、明细、库存和流水。
3. `app/services/inventory_service.py`：库存不能小于零的业务约束。
4. `app/routers/orders.py`：HTTP 层只负责协议，业务规则放在服务层。
5. `tests/test_orders.py`：如何验证幂等、库存不足回滚和取消恢复。

## 企业化升级清单

两个项目已经保留了可升级的边界，但为了让本地学习保持轻量，没有直接加入全部基础设施。掌握代码后，建议依次完成：

1. 使用 PostgreSQL 替代 SQLite，并用 Alembic 管理数据库迁移。
2. 使用 `pydantic-settings` 管理开发、测试、生产配置。
3. 为接口增加结构化日志、请求耗时、异常监控和 OpenTelemetry 链路。
4. 将 JWT 密钥放入密钥管理服务，并增加 refresh token、撤销和登录限流。
5. 为订单表加入版本号或采用 PostgreSQL 行锁，编写并发扣库存测试。
6. 增加 Dockerfile、健康检查、CI 测试、代码格式检查和部署配置。
7. 把订单通知、报表等非核心流程放入任务队列，避免阻塞请求。

## 运行测试

```bash
pytest
```

也可以单独运行：

```bash
pytest projects/rbac_project_api/tests -q
pytest projects/order_inventory_api/tests -q
```
