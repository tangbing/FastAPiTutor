from enum import Enum

from sqlalchemy import UniqueConstraint
from sqlmodel import Field, SQLModel


class UserRole(str, Enum):
    ADMIN = "admin"
    MANAGER = "manager"
    MEMBER = "member"


class UserBase(SQLModel):
    username: str = Field(index=True, min_length=3, max_length=50)
    role: UserRole = Field(default=UserRole.MEMBER, index=True)
    is_active: bool = True


class User(UserBase, table=True):
    __tablename__ = "rbac_users"
    __table_args__ = (
        UniqueConstraint("username", name="uq_rbac_users_username"),
    )

    id: int | None = Field(default=None, primary_key=True)
    password_hash: str


class BootstrapAdmin(SQLModel):
    username: str = Field(min_length=3, max_length=50)
    password: str = Field(min_length=8, max_length=128)


class UserCreate(BootstrapAdmin):
    role: UserRole = UserRole.MEMBER


class UserRead(UserBase):
    id: int


class Token(SQLModel):
    access_token: str
    token_type: str = "bearer"


class ProjectStatus(str, Enum):
    PLANNING = "planning"
    ACTIVE = "active"
    ARCHIVED = "archived"


class ProjectBase(SQLModel):
    name: str = Field(index=True, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    status: ProjectStatus = Field(default=ProjectStatus.PLANNING, index=True)


class Project(ProjectBase, table=True):
    __tablename__ = "rbac_projects"

    id: int | None = Field(default=None, primary_key=True)
    owner_id: int = Field(foreign_key="rbac_users.id", index=True)


class ProjectCreate(ProjectBase):
    pass


class ProjectRead(ProjectBase):
    id: int
    owner_id: int


class ProjectUpdate(SQLModel):
    name: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=1000)
    status: ProjectStatus | None = None


class ProjectMember(SQLModel, table=True):
    __tablename__ = "rbac_project_members"
    __table_args__ = (
        UniqueConstraint(
            "project_id",
            "user_id",
            name="uq_rbac_project_members_project_user",
        ),
    )

    id: int | None = Field(default=None, primary_key=True)
    project_id: int = Field(foreign_key="rbac_projects.id", index=True)
    user_id: int = Field(foreign_key="rbac_users.id", index=True)


class ProjectMemberCreate(SQLModel):
    user_id: int
