from datetime import datetime, timezone
from enum import Enum

from sqlalchemy import UniqueConstraint
from sqlmodel import Field, SQLModel


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class ProductBase(SQLModel):
    sku: str = Field(index=True, min_length=1, max_length=64)
    name: str = Field(index=True, min_length=1, max_length=120)
    price_cents: int = Field(ge=0)
    stock: int = Field(default=0, ge=0)


class Product(ProductBase, table=True):
    __tablename__ = "inventory_products"
    __table_args__ = (
        UniqueConstraint("sku", name="uq_inventory_products_sku"),
    )

    id: int | None = Field(default=None, primary_key=True)


class ProductCreate(ProductBase):
    pass


class ProductRead(ProductBase):
    id: int


class StockAdjustment(SQLModel):
    delta: int
    reason: str = Field(min_length=3, max_length=200)


class StockMovement(SQLModel, table=True):
    __tablename__ = "inventory_stock_movements"

    id: int | None = Field(default=None, primary_key=True)
    product_id: int = Field(foreign_key="inventory_products.id", index=True)
    delta: int
    reason: str = Field(max_length=200)
    created_at: datetime = Field(default_factory=utc_now, index=True)


class OrderStatus(str, Enum):
    CONFIRMED = "confirmed"
    CANCELED = "canceled"


class OrderBase(SQLModel):
    customer_name: str = Field(min_length=1, max_length=120)


class Order(OrderBase, table=True):
    __tablename__ = "inventory_orders"

    id: int | None = Field(default=None, primary_key=True)
    status: OrderStatus = Field(default=OrderStatus.CONFIRMED, index=True)
    total_cents: int = Field(default=0, ge=0)
    created_at: datetime = Field(default_factory=utc_now, index=True)


class OrderItem(SQLModel, table=True):
    __tablename__ = "inventory_order_items"

    id: int | None = Field(default=None, primary_key=True)
    order_id: int = Field(foreign_key="inventory_orders.id", index=True)
    product_id: int = Field(foreign_key="inventory_products.id", index=True)
    quantity: int = Field(gt=0)
    unit_price_cents: int = Field(ge=0)


class OrderItemCreate(SQLModel):
    product_id: int
    quantity: int = Field(gt=0, le=10000)


class OrderCreate(OrderBase):
    items: list[OrderItemCreate] = Field(min_length=1, max_length=50)


class OrderItemRead(SQLModel):
    id: int
    product_id: int
    quantity: int
    unit_price_cents: int


class OrderRead(OrderBase):
    id: int
    status: OrderStatus
    total_cents: int
    created_at: datetime


class OrderDetail(OrderRead):
    items: list[OrderItemRead]


class IdempotencyRecord(SQLModel, table=True):
    __tablename__ = "inventory_idempotency_records"
    __table_args__ = (
        UniqueConstraint("key", name="uq_inventory_idempotency_records_key"),
    )

    id: int | None = Field(default=None, primary_key=True)
    key: str = Field(index=True, min_length=8, max_length=100)
    request_hash: str = Field(max_length=64)
    order_id: int = Field(foreign_key="inventory_orders.id", index=True)
    created_at: datetime = Field(default_factory=utc_now)
