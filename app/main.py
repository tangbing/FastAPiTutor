from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI

from app.database import create_db_and_tables
from app.routers import tasks


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    create_db_and_tables()
    yield


app = FastAPI(title="FastAPI Starter API", version="0.1.0", lifespan=lifespan)
app.include_router(tasks.router)


@app.get("/health", tags=["health"])
def health_check() -> dict[str, str]:
    return {"status": "ok"}
