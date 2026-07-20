from fastapi import HTTPException, status
from sqlalchemy import or_
from sqlmodel import Session, col, select

from projects.rbac_project_api.app.models import (
    Project,
    ProjectCreate,
    ProjectMember,
    ProjectUpdate,
    User,
    UserRole,
)


def list_visible_projects(
    session: Session,
    current_user: User,
    offset: int,
    limit: int,
) -> list[Project]:
    statement = select(Project)
    if current_user.role != UserRole.ADMIN:
        membership_ids = session.exec(
            select(ProjectMember.project_id).where(
                ProjectMember.user_id == current_user.id
            )
        ).all()
        statement = statement.where(
            or_(
                Project.owner_id == current_user.id,
                col(Project.id).in_(membership_ids),
            )
        )

    return list(session.exec(statement.offset(offset).limit(limit)).all())


def get_visible_project(
    session: Session,
    project_id: int,
    current_user: User,
) -> Project:
    project = session.get(Project, project_id)
    if project is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found",
        )

    if current_user.role == UserRole.ADMIN or project.owner_id == current_user.id:
        return project

    membership = session.exec(
        select(ProjectMember).where(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id == current_user.id,
        )
    ).one_or_none()
    if membership is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found",
        )
    return project


def create_project(
    session: Session,
    project_create: ProjectCreate,
    owner: User,
) -> Project:
    project = Project.model_validate(project_create, update={"owner_id": owner.id})
    session.add(project)
    session.commit()
    session.refresh(project)
    return project


def ensure_can_manage(project: Project, current_user: User) -> None:
    if current_user.role != UserRole.ADMIN and project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the project owner or an admin can manage this project",
        )


def update_project(
    session: Session,
    project: Project,
    project_update: ProjectUpdate,
    current_user: User,
) -> Project:
    ensure_can_manage(project, current_user)
    project.sqlmodel_update(project_update.model_dump(exclude_unset=True))
    session.add(project)
    session.commit()
    session.refresh(project)
    return project


def add_member(
    session: Session,
    project: Project,
    user_id: int,
    current_user: User,
) -> User:
    ensure_can_manage(project, current_user)
    user = session.get(User, user_id)
    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Active user not found",
        )

    existing = session.exec(
        select(ProjectMember).where(
            ProjectMember.project_id == project.id,
            ProjectMember.user_id == user_id,
        )
    ).one_or_none()
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="User is already a project member",
        )

    session.add(ProjectMember(project_id=project.id, user_id=user_id))
    session.commit()
    return user


def delete_member(
    session: Session,
    project: Project,
    user_id: int,
    current_user: User,
) -> None:
    # 权限检查
    ensure_can_manage(project, current_user)

    membership = session.exec(
        select(ProjectMember).where(
            ProjectMember.project_id == project.id,
            ProjectMember.user_id == user_id,
        )
    ).one_or_none()
    if membership is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project member not found",
        )
    session.delete(membership)
    session.commit()


def list_members(session: Session, project_id: int) -> list[User]:
    statement = (
        select(User)
        .join(ProjectMember, User.id == ProjectMember.user_id)
        .where(ProjectMember.project_id == project_id)
    )
    return list(session.exec(statement).all())
