from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlmodel import Session, select

from projects.order_inventory_api.app.models import (
    Product,
    ProductCreate,
    StockAdjustment,
    StockMovement,
)


def get_product_or_404(session: Session, product_id: int) -> Product:
    product = session.get(Product, product_id)
    if product is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Product not found",
        )
    return product


def create_product(session: Session, payload: ProductCreate) -> Product:
    existing = session.exec(
        select(Product).where(Product.sku == payload.sku)
    ).one_or_none()
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="SKU already exists",
        )

    product = Product.model_validate(payload)
    try:
        session.add(product)
        session.flush()
        if product.stock:
            session.add(
                StockMovement(
                    product_id=product.id,
                    delta=product.stock,
                    reason="initial stock",
                )
            )
        session.commit()
    except IntegrityError as exc:
        session.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="SKU already exists",
        ) from exc

    session.refresh(product)
    return product


def adjust_stock(
    session: Session,
    product_id: int,
    payload: StockAdjustment,
) -> Product:
    if payload.delta == 0:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="Stock delta cannot be zero",
        )

    statement = select(Product).where(Product.id == product_id).with_for_update()
    product = session.exec(statement).one_or_none()
    if product is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Product not found",
        )
    if product.stock + payload.delta < 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Stock cannot become negative",
        )

    try:
        product.stock += payload.delta
        session.add(product)
        session.add(
            StockMovement(
                product_id=product_id,
                delta=payload.delta,
                reason=payload.reason,
            )
        )
        session.commit()
    except Exception:
        session.rollback()
        raise

    session.refresh(product)
    return product
