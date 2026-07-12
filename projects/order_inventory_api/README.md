# Order and Inventory API

一个用于学习事务型业务的 FastAPI 项目。详细学习步骤见仓库根目录的 `PROJECTS_GUIDE.md`。

## 启动

```bash
python -m uvicorn projects.order_inventory_api.app.main:app --reload --port 8002
```

可用配置：

| 环境变量 | 默认值 |
| --- | --- |
| `ORDER_DATABASE_URL` | `sqlite:///./order_inventory.db` |

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/products` | 创建商品及初始库存流水 |
| GET | `/products` | 商品分页列表 |
| POST | `/products/{id}/stock-adjustments` | 人工调整库存并记录原因 |
| POST | `/orders` | 幂等创建订单并扣减库存 |
| GET | `/orders/{id}` | 查询订单和成交价快照 |
| POST | `/orders/{id}/cancel` | 取消订单并恢复库存 |

`POST /orders` 必须携带 `Idempotency-Key` 请求头。SQLite 适合本地学习；并发生产环境应使用 PostgreSQL，并配合事务隔离和行锁。
