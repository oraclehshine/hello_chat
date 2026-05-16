from functools import lru_cache
from pathlib import Path

from dotenv import dotenv_values
from pydantic_settings import BaseSettings, SettingsConfigDict

CODE_ENV_FILE = Path(__file__).resolve().parents[4] / ".env"
LOCAL_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    admin_api_host: str = "0.0.0.0"
    admin_api_port: int = 8090
    admin_cors_origins: str = "http://localhost:3100,http://127.0.0.1:3100"
    admin_auth_secret: str = "hello-chat-admin-local-secret"
    db_url: str = "postgresql://postgres:change-me@localhost:5433/hello_chat"
    db_username: str = ""
    db_password: str = ""
    redis_url: str = "redis://localhost:6379/0"
    kafka_bootstrap_servers: str = "localhost:9092"

    model_config = SettingsConfigDict(
        env_file=(CODE_ENV_FILE, LOCAL_ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @property
    def cors_origins(self) -> list[str]:
        return [
            origin.strip()
            for origin in self.admin_cors_origins.split(",")
            if origin.strip()
        ]

    @property
    def sqlalchemy_url(self) -> str:
        if self.db_url.startswith("jdbc:postgresql://"):
            raw = self.db_url.replace("jdbc:postgresql://", "postgresql+psycopg://", 1)
            if self.db_username:
                auth = self.db_username
                if self.db_password:
                    auth = f"{auth}:{self.db_password}"
                return raw.replace("postgresql+psycopg://", f"postgresql+psycopg://{auth}@", 1)
            return raw
        if self.db_url.startswith("postgresql://"):
            return self.db_url.replace("postgresql://", "postgresql+psycopg://", 1)
        return self.db_url


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    values = dotenv_values(CODE_ENV_FILE) if CODE_ENV_FILE.exists() else {}
    bom_db_url = values.get("\ufeffDB_URL")
    if bom_db_url and settings.db_url == Settings.model_fields["db_url"].default:
        settings.db_url = bom_db_url
    return settings
