
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query, status
from sqlmodel import select
from app.database import SessionDep
from app.user_models import User, UserRead, UserCreate, UserUpdate


router = APIRouter(prefix="/users", tags=["users"])


@router.post("", response_model=UserRead, status_code=status.HTTP_201_CREATED)
def create_user(user: UserCreate, session: SessionDep) -> User:
    db_user = User.model_validate(user)
    session.add(db_user)
    session.commit()
    session.refresh(db_user)
    return db_user


@router.get("", response_model=list[UserRead])
def list_users(session: SessionDep,
                offset: int =0,
                limit: Annotated[int,Query(ge=1,le=100)] = 20,
                ) -> list[User]:
    statement = select(User).offset(offset).limit(limit)
    return list(session.exec(statement).all())


@router.get("/{user_id}", response_model=UserRead)
def get_user(user_id: int, session: SessionDep) -> User:
    user = session.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="user not found!")
    return user


@router.patch("/{user_id}", response_model=UserRead)
def update_user(user_id: int,
                 user_update: UserUpdate,
                 session: SessionDep) -> User:
    user = session.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="user not found!")

    user_data = user_update.model_dump(exclude_unset=True)
    user.sqlmodel_update(user_data)
    session.add(user)
    session.commit()
    session.refresh(user)
    return user


@router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_user(user_id: int, session: SessionDep) -> None:
    user = session.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="user not found!")
    session.delete(user)
    session.commit()
