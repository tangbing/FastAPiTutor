import os
from typing import Annotated, Generator

from fastapi import Depends
from sqlmodel import SQLModel, Session, create_engine

# 数据库会话管理，让每一个FastAPI 请求都方便，劝劝地活得一个 Session

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./app.db")
# SQLite 默认要求一个数据库连接只能在创建它的线程中使用。FastAPI 处理请求时可能跨线程，因此通常需要关闭这个限制
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
# 创建数据库引擎
engine = create_engine(DATABASE_URL, connect_args=connect_args)

# 创建数据库表
def create_db_and_tables() -> None:
    SQLModel.metadata.create_all(engine)

# 为每个请求提供 Session
def get_session() -> Generator[Session, None, None]:
    with Session(engine) as session:
        yield session

# FastAPI 收到请求后会自动调用 get_session()，不需要客户端传递 session。

SessionDep = Annotated[Session, Depends(get_session)]
