from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request

from projects.rbac_project_api.app.database import create_db_and_tables
from projects.rbac_project_api.app.routers import auth, projects, users


@asynccontextmanager
async def lifespan(_: FastAPI):
    create_db_and_tables()
    yield


app = FastAPI(
    title="RBAC Project Management API",
    version="1.0.0",
    lifespan=lifespan,
)


@app.middleware("http")
async def add_request_id(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID", str(uuid4()))
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response


@app.get("/health", tags=["operations"])
def health_check() -> dict[str, str]:
    return {"status": "ok"}


app.include_router(auth.router)
app.include_router(users.router)
app.include_router(projects.router)
