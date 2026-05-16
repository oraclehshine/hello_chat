from collections.abc import Mapping
from typing import Any

from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError

from app.core.config import get_settings

settings = get_settings()
engine = create_engine(settings.sqlalchemy_url, pool_pre_ping=True, pool_size=3, max_overflow=2)


def fetch_all(sql: str, params: Mapping[str, Any] | None = None) -> list[dict[str, Any]] | None:
    try:
        with engine.connect() as connection:
            result = connection.execute(text(sql), params or {})
            return [dict(row) for row in result.mappings().all()]
    except SQLAlchemyError:
        return None


def fetch_one(sql: str, params: Mapping[str, Any] | None = None) -> dict[str, Any] | None:
    rows = fetch_all(sql, params)
    if not rows:
        return None
    return rows[0]


def execute_one(sql: str, params: Mapping[str, Any] | None = None) -> dict[str, Any] | None:
    try:
        with engine.begin() as connection:
            result = connection.execute(text(sql), params or {})
            if not result.returns_rows:
                return {"affected": result.rowcount}
            row = result.mappings().first()
            if row is None:
                return {"affected": result.rowcount}
            return dict(row)
    except SQLAlchemyError:
        return None
