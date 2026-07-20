from fastapi.testclient import TestClient


def bootstrap_admin(
    client: TestClient,
    username: str = "admin",
    password: str = "admin-pass-123",
) -> tuple[dict, dict[str, str]]:
    response = client.post(
        "/auth/bootstrap",
        json={"username": username, "password": password},
    )
    assert response.status_code == 201, response.text
    return response.json(), login(client, username, password)


def login(client: TestClient, username: str, password: str) -> dict[str, str]:
    response = client.post(
        "/auth/token",
        data={"username": username, "password": password},
    )
    assert response.status_code == 200, response.text
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def create_user(
    client: TestClient,
    admin_headers: dict[str, str],
    username: str,
    password: str,
    role: str,
) -> dict:
    response = client.post(
        "/users",
        headers=admin_headers,
        json={"username": username, "password": password, "role": role},
    )
    assert response.status_code == 201, response.text
    return response.json()


def create_project(
    client: TestClient,
    owner_headers: dict[str, str],
    name: str = "Permission Lab",
) -> dict:
    response = client.post(
        "/projects",
        headers=owner_headers,
        json={"name": name, "status": "active"},
    )
    assert response.status_code == 201, response.text
    return response.json()


def add_project_member(
    client: TestClient,
    owner_headers: dict[str, str],
    project_id: int,
    user_id: int,
) -> dict:
    response = client.post(
        f"/projects/{project_id}/members",
        headers=owner_headers,
        json={"user_id": user_id},
    )
    assert response.status_code == 201, response.text
    return response.json()
