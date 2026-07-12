import pytest
from fastapi.testclient import TestClient
from sqlmodel import Session, SQLModel, create_engine
from sqlmodel.pool import StaticPool

from projects.order_inventory_api.app.database import get_session
from projects.order_inventory_api.app.main import app


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


def create_product(
    client: TestClient,
    sku: str,
    price_cents: int,
    stock: int,
) -> dict:
    response = client.post(
        "/products",
        json={
            "sku": sku,
            "name": f"Product {sku}",
            "price_cents": price_cents,
            "stock": stock,
        },
    )
    assert response.status_code == 201
    return response.json()


def test_order_idempotency_inventory_and_cancel_flow(client: TestClient) -> None:
    keyboard = create_product(client, "KB-001", 12900, 10)
    mouse = create_product(client, "MS-001", 5900, 5)
    payload = {
        "customer_name": "Acme Ltd",
        "items": [
            {"product_id": keyboard["id"], "quantity": 2},
            {"product_id": mouse["id"], "quantity": 1},
        ],
    }
    headers = {"Idempotency-Key": "order-request-0001"}

    created = client.post("/orders", headers=headers, json=payload)
    assert created.status_code == 201
    assert created.json()["total_cents"] == 31700
    order_id = created.json()["id"]

    repeated = client.post("/orders", headers=headers, json=payload)
    assert repeated.status_code == 201
    assert repeated.json()["id"] == order_id
    assert client.get(f"/products/{keyboard['id']}").json()["stock"] == 8

    changed_payload = {
        "customer_name": "Acme Ltd",
        "items": [{"product_id": keyboard["id"], "quantity": 1}],
    }
    conflict = client.post("/orders", headers=headers, json=changed_payload)
    assert conflict.status_code == 409

    insufficient = client.post(
        "/orders",
        headers={"Idempotency-Key": "order-request-0002"},
        json={
            "customer_name": "Big Buyer",
            "items": [{"product_id": mouse["id"], "quantity": 100}],
        },
    )
    assert insufficient.status_code == 409
    assert client.get(f"/products/{mouse['id']}").json()["stock"] == 4

    canceled = client.post(f"/orders/{order_id}/cancel")
    assert canceled.status_code == 200
    assert canceled.json()["status"] == "canceled"
    assert client.get(f"/products/{keyboard['id']}").json()["stock"] == 10
    assert client.get(f"/products/{mouse['id']}").json()["stock"] == 5

    canceled_again = client.post(f"/orders/{order_id}/cancel")
    assert canceled_again.status_code == 200
    assert client.get(f"/products/{keyboard['id']}").json()["stock"] == 10
