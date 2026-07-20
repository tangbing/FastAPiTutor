from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI

from app.database import create_db_and_tables # 通常负责创建 SQLite 数据库和表
from app.routers import tasks  # tasks：导入前面定义了 APIRouter 的任务模块。
from app.routers import users

@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    create_db_and_tables()
    yield
    # 应用关闭时执行清理工作

# title：Swagger 文档 /docs 中显示的 API 名称。
# version：Swagger 文档中的版本号。
# lifespan=lifespan：告诉 FastAPI 在启动和关闭时使用上面定义的生命周期函数。
app = FastAPI(title="FastAPI Starter API", version="0.1.0", lifespan=lifespan)
# 把任务模块的路由注册到整个应用
app.include_router(tasks.router)
app.include_router(users.router)



@app.get("/health", tags=["health"])
def health_check() -> dict[str, str]:
    return {"status": "ok"}
