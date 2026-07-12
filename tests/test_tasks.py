import pytest
from fastapi.testclient import TestClient
from sqlmodel import Session, SQLModel, create_engine
from sqlmodel.pool import StaticPool

from app.database import get_session
from app.main import app


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


def test_task_crud_flow(client: TestClient) -> None:
    create_response = client.post(
        "/tasks",
        json={"title": "Learn FastAPI", "description": "Build a CRUD API"},
    )
    assert create_response.status_code == 201
    created_task = create_response.json()
    assert created_task["title"] == "Learn FastAPI"
    assert created_task["done"] is False

    task_id = created_task["id"]

    list_response = client.get("/tasks")
    assert list_response.status_code == 200
    assert list_response.json()[0]["id"] == task_id

    update_response = client.patch(f"/tasks/{task_id}", json={"done": True})
    assert update_response.status_code == 200
    assert update_response.json()["done"] is True

    delete_response = client.delete(f"/tasks/{task_id}")
    assert delete_response.status_code == 204

    missing_response = client.get(f"/tasks/{task_id}")
    assert missing_response.status_code == 404
