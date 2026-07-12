from typing import Annotated

from fastapi import APIRouter, Header, Query, status
from sqlmodel import select

from projects.order_inventory_api.app.database import SessionDep
from projects.order_inventory_api.app.models import (
    Order,
    OrderCreate,
    OrderDetail,
    OrderRead,
)
from projects.order_inventory_api.app.services import order_service


router = APIRouter(prefix="/orders", tags=["orders"])
IdempotencyKey = Annotated[
    str,
    Header(alias="Idempotency-Key", min_length=8, max_length=100),
]


@router.post("", response_model=OrderDetail, status_code=status.HTTP_201_CREATED)
def create_order(
    payload: OrderCreate,
    session: SessionDep,
    idempotency_key: IdempotencyKey,
) -> OrderDetail:
    return order_service.create_order(session, payload, idempotency_key)


@router.get("", response_model=list[OrderRead])
def list_orders(
    session: SessionDep,
    offset: int = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[Order]:
    return list(session.exec(select(Order).offset(offset).limit(limit)).all())


@router.get("/{order_id}", response_model=OrderDetail)
def get_order(order_id: int, session: SessionDep) -> OrderDetail:
    return order_service.get_order_detail(session, order_id)


@router.post("/{order_id}/cancel", response_model=OrderDetail)
def cancel_order(order_id: int, session: SessionDep) -> OrderDetail:
    return order_service.cancel_order(session, order_id)
