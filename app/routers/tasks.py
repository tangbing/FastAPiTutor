from typing import Annotated

from fastapi import APIRouter, HTTPException, Query, status
from sqlmodel import select

from app.database import SessionDep
from app.models import Task, TaskCreate, TaskRead, TaskUpdate


router = APIRouter(prefix="/tasks", tags=["tasks"])


@router.post("", response_model=TaskRead, status_code=status.HTTP_201_CREATED)
# task：TaskCreate 从请求体 JSON 读取并校验数据，例如标题，描述等
# session：SessionDep 通过依赖注入取得数据库会话
# -> Task: 函数内部实际返回数据库模型对象，再由 TaskRead 转成对外 JSON
def create_task(task: TaskCreate, session: SessionDep) -> Task:
    # 把请求模型 TtaskCreate 转成数据库模型 Task，通常这里的 Task 是 SQLModel 定义的表模型
    db_task = Task.model_validate(task)
    # 把新任务加入当前数据库会话
    session.add(db_task)
    # 提交事务，真正写入SQLite
    session.commit()
    # 从数据库重新取回数据，得到数据库生成的字段，例如 id，默认时间等
    session.refresh(db_task)
    # 返回刚创建的任务，FastAPI 根据 TaskRead 序列化响应给客户端
    return db_task


@router.get("", response_model=list[TaskRead])
def list_tasks(
    session: SessionDep,
    offset: int = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[Task]:
    statement = select(Task).offset(offset).limit(limit)
    return list(session.exec(statement).all())


@router.get("/{task_id}", response_model=TaskRead)
def get_task(task_id: int, session: SessionDep) -> Task:
    task = session.get(Task, task_id)
    if task is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found")
    return task


@router.patch("/{task_id}", response_model=TaskRead)
def update_task(task_id: int, task_update: TaskUpdate, session: SessionDep) -> Task:
    task = session.get(Task, task_id)
    if task is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found")

    task_data = task_update.model_dump(exclude_unset=True)
    task.sqlmodel_update(task_data)
    session.add(task)
    session.commit()
    session.refresh(task)
    return task


@router.delete("/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_task(task_id: int, session: SessionDep) -> None:
    task = session.get(Task, task_id)
    if task is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found")

    session.delete(task)
    session.commit()
