from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlmodel import select

from projects.rbac_project_api.app.database import SessionDep
from projects.rbac_project_api.app.dependencies import CurrentUserDep
from projects.rbac_project_api.app.models import (
    BootstrapAdmin,
    Token,
    User,
    UserRead,
    UserRole,
)
from projects.rbac_project_api.app.security import (
    create_access_token,
    hash_password,
    verify_password,
)


router = APIRouter(prefix="/auth", tags=["authentication"])


@router.post(
    "/bootstrap",
    response_model=UserRead,
    status_code=status.HTTP_201_CREATED,
)
def bootstrap_admin(payload: BootstrapAdmin, session: SessionDep) -> User:
    if session.exec(select(User)).first() is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Bootstrap is disabled after the first user is created",
        )

    admin = User(
        username=payload.username,
        password_hash=hash_password(payload.password),
        role=UserRole.ADMIN,
    )
    session.add(admin)
    session.commit()
    session.refresh(admin)
    return admin


@router.post("/token", response_model=Token)
def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    session: SessionDep,
) -> Token:
    user = session.exec(
        select(User).where(User.username == form_data.username)
    ).one_or_none()
    if (
        user is None
        or not user.is_active
        or not verify_password(form_data.password, user.password_hash)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return Token(access_token=create_access_token(user.username))


@router.get("/me", response_model=UserRead)
def read_current_user(current_user: CurrentUserDep) -> User:
    return current_user
