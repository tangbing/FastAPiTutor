from typing import Annotated

from fastapi import APIRouter, Query, status

from projects.rbac_project_api.app.database import SessionDep
from projects.rbac_project_api.app.dependencies import (
    CurrentUserDep,
    ProjectCreatorDep,
)
from projects.rbac_project_api.app.models import (
    Project,
    ProjectCreate,
    ProjectMemberCreate,
    ProjectRead,
    ProjectUpdate,
    User,
    UserRead,
)
from projects.rbac_project_api.app.services import project_service


router = APIRouter(prefix="/projects", tags=["projects"])


@router.post("", response_model=ProjectRead, status_code=status.HTTP_201_CREATED)
def create_project(
    payload: ProjectCreate,
    session: SessionDep,
    current_user: ProjectCreatorDep,
) -> Project:
    return project_service.create_project(session, payload, current_user)


@router.get("", response_model=list[ProjectRead])
def list_projects(
    session: SessionDep,
    current_user: CurrentUserDep,
    offset: int = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[Project]:
    return project_service.list_visible_projects(
        session,
        current_user,
        offset,
        limit,
    )


@router.get("/{project_id}", response_model=ProjectRead)
def get_project(
    project_id: int,
    session: SessionDep,
    current_user: CurrentUserDep,
) -> Project:
    return project_service.get_visible_project(session, project_id, current_user)


@router.patch("/{project_id}", response_model=ProjectRead)
def update_project(
    project_id: int,
    payload: ProjectUpdate,
    session: SessionDep,
    current_user: CurrentUserDep,
) -> Project:
    project = project_service.get_visible_project(session, project_id, current_user)
    return project_service.update_project(session, project, payload, current_user)


@router.post(
    "/{project_id}/members",
    response_model=UserRead,
    status_code=status.HTTP_201_CREATED,
)
def add_project_member(
    project_id: int,
    payload: ProjectMemberCreate,
    session: SessionDep,
    current_user: CurrentUserDep,
) -> User:
    project = project_service.get_visible_project(session, project_id, current_user)
    return project_service.add_member(
        session,
        project,
        payload.user_id,
        current_user,
    )


@router.get("/{project_id}/members", response_model=list[UserRead])
def list_project_members(
    project_id: int,
    session: SessionDep,
    current_user: CurrentUserDep,
) -> list[User]:
    project_service.get_visible_project(session, project_id, current_user)
    return project_service.list_members(session, project_id)
