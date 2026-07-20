
from fastapi import HTTPException, status
from sqlmodel import Session

from projects.rbac_project_api.app.models import User, UserRole



def update_user_active_status(
        session: Session,
        user_id: int,
        is_active: bool,
        current_user: User
) -> User:
    if current_user.role != UserRole.ADMIN:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                            detail="Only admins can update user active status"
                            )

    user = session.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND,  detail="User not found")

    if user.id == current_user.id and not is_active:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin cannot deactivate themselves")


    user.is_active = is_active

    session.add(user)
    session.commit()
    session.refresh(user)

    return user
