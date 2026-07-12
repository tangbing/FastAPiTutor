import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv("RBAC_DATABASE_URL", "sqlite:///./rbac_project.db")
    secret_key: str = os.getenv(
        "RBAC_SECRET_KEY",
        "change-this-development-secret-before-production",
    )
    access_token_expire_minutes: int = int(
        os.getenv("RBAC_ACCESS_TOKEN_EXPIRE_MINUTES", "30")
    )


settings = Settings()
