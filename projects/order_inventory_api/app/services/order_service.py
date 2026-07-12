import hashlib

from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlmodel import Session, col, select

from projects.order_inventory_api.app.models import (
    IdempotencyRecord,
    Order,
    OrderCreate,
    OrderDetail,
    OrderItem,
    OrderItemRead,
    OrderStatus,
    Product,
    StockMovement,
)


def request_fingerprint(payload: OrderCreate) -> str:
    request_body = payload.model_dump_json()
    return hashlib.sha256(request_body.encode("utf-8")).hexdigest()


def build_order_detail(session: Session, order: Order) -> OrderDetail:
    items = session.exec(
        select(OrderItem).where(OrderItem.order_id == order.id)
    ).all()
    return OrderDetail(
        id=order.id,
        customer_name=order.customer_name,
        status=order.status,
        total_cents=order.total_cents,
        created_at=order.created_at,
        items=[OrderItemRead.model_validate(item) for item in items],
    )


def get_order_detail(session: Session, order_id: int) -> OrderDetail:
    order = session.get(Order, order_id)
    if order is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Order not found",
        )
    return build_order_detail(session, order)


def read_idempotent_result(
    session: Session,
    key: str,
    fingerprint: str,
) -> OrderDetail | None:
    record = session.exec(
        select(IdempotencyRecord).where(IdempotencyRecord.key == key)
    ).one_or_none()
    if record is None:
        return None
    if record.request_hash != fingerprint:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Idempotency key was already used with a different request",
        )
    return get_order_detail(session, record.order_id)


def create_order(
    session: Session,
    payload: OrderCreate,
    idempotency_key: str,
) -> OrderDetail:
    fingerprint = request_fingerprint(payload)
    existing_result = read_idempotent_result(session, idempotency_key, fingerprint)
    if existing_result is not None:
        return existing_result

    product_ids = [item.product_id for item in payload.items]
    if len(set(product_ids)) != len(product_ids):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="Each product can appear only once in an order",
        )

    try:
        products = session.exec(
            select(Product)
            .where(col(Product.id).in_(product_ids))
            .with_for_update()
        ).all()
        products_by_id = {
            product.id: product for product in products if product.id is not None
        }
        missing_ids = [
            product_id
            for product_id in product_ids
            if product_id not in products_by_id
        ]
        if missing_ids:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"message": "Products not found", "product_ids": missing_ids},
            )

        total_cents = 0
        for item in payload.items:
            product = products_by_id[item.product_id]
            if product.stock < item.quantity:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail={
                        "message": "Insufficient stock",
                        "product_id": product.id,
                        "available": product.stock,
                        "requested": item.quantity,
                    },
                )
            total_cents += product.price_cents * item.quantity

        order = Order(
            customer_name=payload.customer_name,
            total_cents=total_cents,
        )
        session.add(order)
        session.flush()

        for item in payload.items:
            product = products_by_id[item.product_id]
            product.stock -= item.quantity
            session.add(product)
            session.add(
                OrderItem(
                    order_id=order.id,
                    product_id=product.id,
                    quantity=item.quantity,
                    unit_price_cents=product.price_cents,
                )
            )
            session.add(
                StockMovement(
                    product_id=product.id,
                    delta=-item.quantity,
                    reason=f"order:{order.id}:reserve",
                )
            )

        session.add(
            IdempotencyRecord(
                key=idempotency_key,
                request_hash=fingerprint,
                order_id=order.id,
            )
        )
        session.commit()
        session.refresh(order)
        return build_order_detail(session, order)
    except IntegrityError as exc:
        session.rollback()
        concurrent_result = read_idempotent_result(
            session,
            idempotency_key,
            fingerprint,
        )
        if concurrent_result is not None:
            return concurrent_result
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Order could not be created due to a concurrent update",
        ) from exc
    except Exception:
        session.rollback()
        raise


def cancel_order(session: Session, order_id: int) -> OrderDetail:
    try:
        order = session.exec(
            select(Order).where(Order.id == order_id).with_for_update()
        ).one_or_none()
        if order is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Order not found",
            )
        if order.status == OrderStatus.CANCELED:
            return build_order_detail(session, order)

        items = session.exec(
            select(OrderItem).where(OrderItem.order_id == order_id)
        ).all()
        product_ids = [item.product_id for item in items]
        products = session.exec(
            select(Product)
            .where(col(Product.id).in_(product_ids))
            .with_for_update()
        ).all()
        products_by_id = {
            product.id: product for product in products if product.id is not None
        }
        if len(products_by_id) != len(set(product_ids)):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Order data is inconsistent with inventory",
            )

        for item in items:
            product = products_by_id[item.product_id]
            product.stock += item.quantity
            session.add(product)
            session.add(
                StockMovement(
                    product_id=product.id,
                    delta=item.quantity,
                    reason=f"order:{order.id}:cancel",
                )
            )

        order.status = OrderStatus.CANCELED
        session.add(order)
        session.commit()
        session.refresh(order)
        return build_order_detail(session, order)
    except Exception:
        session.rollback()
        raise
