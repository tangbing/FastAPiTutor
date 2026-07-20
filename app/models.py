from sqlmodel import Field, SQLModel

#TaskCreate：校验创建请求
#TaskRead：控制响应内容
#TaskUpdate：校验更新请求
#Task：真正与数据库表对应

class TaskBase(SQLModel):
    title: str = Field(index=True, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    done: bool = False

# 会映射到数据库表，默认表名通常是 task，
# 可以用 session.add(task) 保存
# 可以用select(Task) 查询
# 可以用session.get(Task, id) 按主键查询
class Task(TaskBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class TaskCreate(TaskBase):
    pass


class TaskRead(TaskBase):
    id: int


class TaskUpdate(SQLModel):
    title: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    done: bool | None = None
