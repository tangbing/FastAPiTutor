from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlmodel import select

from projects.rbac_project_api.app.database import SessionDep
from projects.rbac_project_api.app.models import User, UserRole
from projects.rbac_project_api.app.security import decode_access_token


oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/token")


def authentication_error() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_current_user(
    token: Annotated[str, Depends(oauth2_scheme)],
    session: SessionDep,
) -> User:
    username = decode_access_token(token)
    if username is None:
        raise authentication_error()

    user = session.exec(select(User).where(User.username == username)).one_or_none()
    if user is None or not user.is_active:
        raise authentication_error()
    return user


CurrentUserDep = Annotated[User, Depends(get_current_user)]


def require_roles(*roles: UserRole):
    def role_checker(current_user: CurrentUserDep) -> User:
        if current_user.role not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Insufficient permissions",
            )
        return current_user

    return role_checker


AdminUserDep = Annotated[User, Depends(require_roles(UserRole.ADMIN))]
ProjectCreatorDep = Annotated[
    User,
    Depends(require_roles(UserRole.ADMIN, UserRole.MANAGER)),
]
