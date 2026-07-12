from typing import Annotated

from fastapi import APIRouter, Query, status
from sqlmodel import select

from projects.order_inventory_api.app.database import SessionDep
from projects.order_inventory_api.app.models import (
    Product,
    ProductCreate,
    ProductRead,
    StockAdjustment,
)
from projects.order_inventory_api.app.services import inventory_service


router = APIRouter(prefix="/products", tags=["products and inventory"])


@router.post("", response_model=ProductRead, status_code=status.HTTP_201_CREATED)
def create_product(payload: ProductCreate, session: SessionDep) -> Product:
    return inventory_service.create_product(session, payload)


@router.get("", response_model=list[ProductRead])
def list_products(
    session: SessionDep,
    offset: int = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> list[Product]:
    return list(session.exec(select(Product).offset(offset).limit(limit)).all())


@router.get("/{product_id}", response_model=ProductRead)
def get_product(product_id: int, session: SessionDep) -> Product:
    return inventory_service.get_product_or_404(session, product_id)


@router.post("/{product_id}/stock-adjustments", response_model=ProductRead)
def adjust_stock(
    product_id: int,
    payload: StockAdjustment,
    session: SessionDep,
) -> Product:
    return inventory_service.adjust_stock(session, product_id, payload)
