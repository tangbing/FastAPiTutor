import pytest
from fastapi.testclient import TestClient
from sqlmodel import Session, SQLModel, create_engine
from sqlmodel.pool import StaticPool

from projects.rbac_project_api.app.database import get_session
from projects.rbac_project_api.app.main import app


@pytest.fixture
def client() -> TestClient:
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    SQLModel.metadata.create_all(engine)

    def get_test_session():
        with Session(engine) as session:
            yield session

    app.dependency_overrides[get_session] = get_test_session
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def login(client: TestClient, username: str, password: str) -> dict[str, str]:
    response = client.post(
        "/auth/token",
        data={"username": username, "password": password},
    )
    assert response.status_code == 200
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def test_rbac_and_project_membership_flow(client: TestClient) -> None:
    bootstrap = client.post(
        "/auth/bootstrap",
        json={"username": "admin", "password": "admin-pass-123"},
    )
    assert bootstrap.status_code == 201
    assert client.post(
        "/auth/bootstrap",
        json={"username": "other", "password": "other-pass-123"},
    ).status_code == 409

    admin_headers = login(client, "admin", "admin-pass-123")
    manager = client.post(
        "/users",
        headers=admin_headers,
        json={
            "username": "manager",
            "password": "manager-pass-123",
            "role": "manager",
        },
    )
    member = client.post(
        "/users",
        headers=admin_headers,
        json={
            "username": "member",
            "password": "member-pass-123",
            "role": "member",
        },
    )
    assert manager.status_code == 201
    assert member.status_code == 201

    manager_headers = login(client, "manager", "manager-pass-123")
    member_headers = login(client, "member", "member-pass-123")
    created = client.post(
        "/projects",
        headers=manager_headers,
        json={"name": "Customer Portal", "status": "active"},
    )
    assert created.status_code == 201
    project_id = created.json()["id"]

    forbidden_create = client.post(
        "/projects",
        headers=member_headers,
        json={"name": "Unauthorized Project"},
    )
    assert forbidden_create.status_code == 403
    assert client.get(
        f"/projects/{project_id}", headers=member_headers
    ).status_code == 404

    add_member = client.post(
        f"/projects/{project_id}/members",
        headers=manager_headers,
        json={"user_id": member.json()["id"]},
    )
    assert add_member.status_code == 201

    visible = client.get(f"/projects/{project_id}", headers=member_headers)
    assert visible.status_code == 200
    assert visible.json()["name"] == "Customer Portal"
