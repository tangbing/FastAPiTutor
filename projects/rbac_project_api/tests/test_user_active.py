from fastapi.testclient import TestClient

from .helpers import bootstrap_admin, create_user, login


def test_admin_can_deactivate_user_and_existing_token_becomes_invalid(
    client: TestClient,
) -> None:
    _, admin_headers = bootstrap_admin(client)
    member = create_user(
        client,
        admin_headers,
        username="member",
        password="member-pass-123",
        role="member",
    )
    member_headers = login(client, "member", "member-pass-123")

    assert client.get("/auth/me", headers=member_headers).status_code == 200

    deactivate = client.patch(
        f"/users/{member['id']}/active",
        headers=admin_headers,
        json={"is_active": False},
    )

    assert deactivate.status_code == 200
    assert deactivate.json()["is_active"] is False
    assert "password_hash" not in deactivate.json()

    old_token_response = client.get("/auth/me", headers=member_headers)
    assert old_token_response.status_code == 401
    assert old_token_response.headers["WWW-Authenticate"] == "Bearer"

    disabled_login = client.post(
        "/auth/token",
        data={"username": "member", "password": "member-pass-123"},
    )
    assert disabled_login.status_code == 401


def test_non_admin_cannot_change_user_active_status(client: TestClient) -> None:
    _, admin_headers = bootstrap_admin(client)
    manager = create_user(
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

    response = client.patch(
        f"/users/{member['id']}/active",
        headers=manager_headers,
        json={"is_active": False},
    )

    assert response.status_code == 403
    assert client.get(f"/users", headers=manager_headers).status_code == 200
    assert manager["is_active"] is True
