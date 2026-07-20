from fastapi.testclient import TestClient

from .helpers import login


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
