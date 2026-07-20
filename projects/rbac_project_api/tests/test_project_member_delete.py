from fastapi.testclient import TestClient

from .helpers import (
    add_project_member,
    bootstrap_admin,
    create_project,
    create_user,
    login,
)


def prepare_project_with_member(
    client: TestClient,
) -> tuple[dict[str, str], dict[str, str], dict[str, str], dict, dict]:
    _, admin_headers = bootstrap_admin(client)
    create_user(
        client,
        admin_headers,
        username="manager",
        password="manager-pass-123",
        role="manager",
    )
    member = create_user(
        client,
        admin_headers,
        username="member",
        password="member-pass-123",
        role="member",
    )
    manager_headers = login(client, "manager", "manager-pass-123")
    member_headers = login(client, "member", "member-pass-123")
    project = create_project(client, manager_headers)
    add_project_member(client, manager_headers, project["id"], member["id"])
    return admin_headers, manager_headers, member_headers, project, member


def test_project_owner_can_remove_member_and_visibility_is_revoked(
    client: TestClient,
) -> None:
    _, owner_headers, member_headers, project, member = prepare_project_with_member(client)

    forbidden = client.delete(
        f"/projects/{project['id']}/members/{member['id']}",
        headers=member_headers,
    )
    assert forbidden.status_code == 403

    removed = client.delete(
        f"/projects/{project['id']}/members/{member['id']}",
        headers=owner_headers,
    )
    assert removed.status_code == 200
    assert removed.json() == {"message": "Member removed successfully"}

    members = client.get(
        f"/projects/{project['id']}/members",
        headers=owner_headers,
    )
    assert members.status_code == 200
    assert all(item["id"] != member["id"] for item in members.json())
    assert client.get(
        f"/projects/{project['id']}",
        headers=member_headers,
    ).status_code == 404


def test_admin_can_remove_member_and_missing_membership_returns_404(
    client: TestClient,
) -> None:
    admin_headers, owner_headers, _, project, member = prepare_project_with_member(client)

    removed = client.delete(
        f"/projects/{project['id']}/members/{member['id']}",
        headers=admin_headers,
    )
    assert removed.status_code == 200

    missing = client.delete(
        f"/projects/{project['id']}/members/{member['id']}",
        headers=owner_headers,
    )
    assert missing.status_code == 404
    assert missing.json()["detail"] == "Project member not found"
