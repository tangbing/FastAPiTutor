import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv(
        "ORDER_DATABASE_URL",
        "sqlite:///./order_inventory.db",
    )


settings = Settings()
