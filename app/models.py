from sqlmodel import Field, SQLModel


class TaskBase(SQLModel):
    title: str = Field(index=True, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    done: bool = False


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
