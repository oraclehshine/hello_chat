from pathlib import Path
import sys

BACKEND_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND_ROOT))

from app.db import engine  # noqa: E402


def main() -> None:
    sql_file = BACKEND_ROOT / "db" / "admin_schema.sql"
    sql = sql_file.read_text(encoding="utf-8")
    with engine.begin() as connection:
        connection.exec_driver_sql(sql)
    print(f"Applied {sql_file}")


if __name__ == "__main__":
    main()
