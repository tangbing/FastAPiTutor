from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlmodel import select

from projects.rbac_project_api.app.database import SessionDep
from projects.rbac_project_api.app.dependencies import (
    AdminUserDep,
    CurrentUserDep,
    require_roles,
)
from projects.rbac_project_api.app.models import (
    User,
    UserActiveUpdate,
    UserCreate,
    UserRead,
    UserRole,
)
from projects.rbac_project_api.app.security import hash_password
from projects.rbac_project_api.app.services.user_service import update_user_active_status

router = APIRouter(prefix="/users", tags=["users"])
UserReaderDep = Annotated[
    User,
    Depends(require_roles(UserRole.ADMIN, UserRole.MANAGER)),
]

@router.patch("/{user_id}/active", response_model=UserRead)
def patch_user_active_status(
        user_id: int,
        active_update: UserActiveUpdate,
        session: SessionDep,
        current_user: CurrentUserDep
) -> User:
    return update_user_active_status(
        session = session,
        user_id=user_id,
        is_active=active_update.is_active,
        current_user=current_user
    )


@router.post("", response_model=UserRead, status_code=status.HTTP_201_CREATED)
def create_user(
    payload: UserCreate,
    session: SessionDep,
    _admin: AdminUserDep,
) -> User:
    existing = session.exec(
        select(User).where(User.username == payload.username)
    ).one_or_none()
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Username already exists",
        )

    user = User(
        username=payload.username,
        role=payload.role,
        password_hash=hash_password(payload.password),
    )
    session.add(user)
    session.commit()
    session.refresh(user)
    return user


@router.get("", response_model=list[UserRead])
def list_users(
    session: SessionDep,
    _reader: UserReaderDep,
    offset: int = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[User]:
    return list(session.exec(select(User).offset(offset).limit(limit)).all())
