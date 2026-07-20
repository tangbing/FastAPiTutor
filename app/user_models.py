

from sqlmodel import Field, SQLModel


class UserBase(SQLModel):
    name: str = Field(index=True, min_length=1, max_length=120)
    age: int = Field(ge=10, le=100)
    isMale: bool = False



class User(UserBase, table=True):
    id: int | None = Field(default=None, primary_key=True)


class UserCreate(UserBase):
    pass


class UserRead(UserBase):
    id: int


class UserUpdate(SQLModel):
    name: str | None = Field(default=None, min_length=1, max_length=120)
    age: int | None = Field(default=None, ge=10, le=100)
    isMale: bool | None = None
