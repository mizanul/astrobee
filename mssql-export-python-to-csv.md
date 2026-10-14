# Python script using `pyodbc` to export MSSQL table data to CSV.

## 1. Install the dependency

PowerShell

```
pip install pyodbc
```

You also need the Microsoft ODBC Driver for SQL Server installed.

## 2. Create `mssql_to_csv.py`

Python

Run

```
import argparse
import csv
import sys
from pathlib import Path

import pyodbc


def quote_identifier(name):
    """Safely quote a SQL Server identifier."""
    if not name or not name.strip():
        raise ValueError("Identifier cannot be empty")
    return "[" + name.replace("]", "]]") + "]"


def parse_table_name(name):
    """Accept TableName or schema.TableName."""
    parts = name.split(".")

    if len(parts) == 1:
        schema, table = "dbo", parts[0]
    elif len(parts) == 2:
        schema, table = parts
    else:
        raise ValueError("Use TableName or schema.TableName")

    return f"{quote_identifier(schema)}.{quote_identifier(table)}"


def export_table(
    server,
    database,
    table,
    output,
    trusted_connection=True,
    username=None,
    password=None,
    batch_size=10000,
    driver="ODBC Driver 18 for SQL Server",
):
    connection_parts = [
        f"DRIVER={{{driver}}}",
        f"SERVER={server}",
        f"DATABASE={database}",
    ]

    if trusted_connection:
        connection_parts.append("Trusted_Connection=yes")
    else:
        if not username or password is None:
            raise ValueError("SQL username and password are required")

        # Escape ODBC connection-string values.
        def escape(value):
            return "{" + value.replace("}", "}}") + "}"

        connection_parts.extend([
            f"UID={escape(username)}",
            f"PWD={escape(password)}",
        ])

    connection_parts.append("Encrypt=yes")
    connection_parts.append("TrustServerCertificate=no")
    connection_string = ";".join(connection_parts) + ";"

    output_path = Path(output).expanduser()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    query = f"SELECT * FROM {parse_table_name(table)}"
    total_rows = 0

    with pyodbc.connect(connection_string) as conn:
        cursor = conn.cursor()
        cursor.arraysize = batch_size
        cursor.execute(query)

        columns = [column[0] for column in cursor.description]

        # utf-8-sig helps Excel recognize UTF-8 files.
        with output_path.open(
            "w", encoding="utf-8-sig", newline=""
        ) as file:
            writer = csv.writer(file, lineterminator="\n")
            writer.writerow(columns)

            while True:
                rows = cursor.fetchmany(batch_size)

                if not rows:
                    break

                writer.writerows(rows)
                total_rows += len(rows)

                print(
                    f"\rExported {total_rows:,} rows",
                    end="",
                    flush=True,
                )

        cursor.close()

    print(f"\nExport completed: {total_rows:,} rows")
    print(f"Output file: {output_path.resolve()}")


def main():
    parser = argparse.ArgumentParser(
        description="Export an MSSQL table to CSV"
    )

    parser.add_argument("--server", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--table", required=True)
    parser.add_argument("--output", required=True)

    parser.add_argument("--username")
    parser.add_argument("--password")
    parser.add_argument("--sql-auth", action="store_true")

    parser.add_argument("--batch-size", type=int, default=10000)
    parser.add_argument(
        "--driver",
        default="ODBC Driver 18 for SQL Server",
    )

    args = parser.parse_args()

    if args.batch_size < 1:
        parser.error("--batch-size must be greater than zero")

    if args.sql_auth and (
        not args.username or args.password is None
    ):
        parser.error("--sql-auth requires --username and --password")

    try:
        export_table(
            server=args.server,
            database=args.database,
            table=args.table,
            output=args.output,
            trusted_connection=not args.sql_auth,
            username=args.username,
            password=args.password,
            batch_size=args.batch_size,
            driver=args.driver,
        )
    except (pyodbc.Error, OSError, ValueError) as exc:
        print(f"\nExport failed: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
```

## 3. Run the script

### Windows authentication

PowerShell

```
python mssql_to_csv.py `
  --server "localhost" `
  --database "MyDatabase" `
  --table "dbo.MyTable" `
  --output "C:\Temp\MyTable.csv"
```

### SQL Server authentication

PowerShell

---

# Cleaning data.

## 1. Example of how special characters are handled

Suppose your MSSQL table contains these values:

| MSSQL text value | CSV output | |---|---| | `Hello, World` | `"Hello, World"` | | `He said "Hello"` | `"He said ""Hello"""` | | `First line` followed by a newline and `Second line` | `"First line` followed by a newline and `Second line"` | | `A;B|C` | `A;B|C` | | `Text with, commas and "quotes"` | `"Text with, commas and ""quotes"""` |

The CSV format uses double quotes to enclose fields when necessary. Any double quote inside a quoted field is escaped by doubling it.

## 2. Use this code in your export script

Python

Run

```
import csv

with open(
    output_path,
    "w",
    encoding="utf-8-sig",
    newline=""
) as csv_file:

    writer = csv.writer(
        csv_file,
        delimiter=",",
        quotechar='"',
        quoting=csv.QUOTE_MINIMAL,
        doublequote=True,
        lineterminator="\n"
    )

    # Write column headers
    writer.writerow(columns)

    # Export rows
    while True:
        rows = cursor.fetchmany(batch_size)

        if not rows:
            break

        writer.writerows(rows)
```

### What these options do

* `delimiter=","`: Uses commas to separate columns.

* `quotechar='"'`: Uses double quotes to enclose fields when necessary.

* `quoting=csv.QUOTE_MINIMAL`: Quotes only fields that require it.

* `doublequote=True`: Escapes embedded double quotes by doubling them.

* `newline=""`: Prevents Python from altering newline characters inside CSV fields.

* `encoding="utf-8-sig"`: Writes UTF-8 with a byte-order mark, which can help Microsoft Excel recognize the encoding.

## 3. What about pipes, semicolons, tabs, or other characters?

Those characters do not need special handling when your delimiter is a comma. The CSV writer only needs to quote a field when it contains the selected delimiter, a quote character, or a newline.

For example, this value:

```
A|B;C<TAB>D
```

can remain unquoted in a comma-delimited CSV because it contains no comma, quote, or newline.

If you use a different delimiter, change the `delimiter` argument accordingly.

## 4. Important: Preserve NULL values and data types

There is one additional consideration for MSSQL-to-PostgreSQL migration.

By default, Python's CSV writer writes both SQL `NULL` and an empty string as an empty CSV field. If you need to distinguish them, define an explicit convention, such as a dedicated null marker, and configure PostgreSQL's `COPY` import to use the same marker. Make sure the marker cannot be confused with actual text values.

For most migrations, I recommend:

1. Use `csv.writer` for export.

2. Use PostgreSQL `COPY ... FORMAT CSV` for import.

3. Verify row counts and test fields containing commas, quotes, newlines, Unicode, empty strings, and `NULL` values.

The key point: Do not remove, replace, or manually escape commas in your text. Proper CSV quoting preserves the original content without breaking the column structure.


```
python mssql_to_csv.py `
  --server "localhost" `
  --database "MyDatabase" `
  --table "dbo.MyTable" `
  --output "C:\Temp\MyTable.csv" `
  --sql-auth `
  --username "myuser" `
  --password "yourpassword"
```

### Change the batch size

For a table with millions of rows:

PowerShell

```
python mssql_to_csv.py `
  --server "localhost" `
  --database "MyDatabase" `
  --table "dbo.MyTable" `
  --output "C:\Temp\MyTable.csv" `
  --batch-size 20000
```

## 4. Important considerations

* Memory: `fetchmany()` limits the number of rows held by Python at a time.

* CSV formatting: The `csv` module handles commas, quotation marks, and embedded newlines correctly.

* Data types: Python's CSV writer serializes values as text. Review date/time, decimal, binary, and `NULL` handling if the CSV will be imported into PostgreSQL.

* Security: Avoid passing passwords on the command line in production because they may be exposed in process listings or shell history. Prefer an environment variable or a secure credential store.

* TLS: The script validates the SQL Server certificate. If your environment uses a self-signed certificate, configure a trusted certificate rather than disabling validation by default.

My recommendation: For your MSSQL-to-PostgreSQL migration, use this script for the export stage and PostgreSQL's `COPY` command for the import stage. For recurring migrations, you may want to add incremental export using a timestamp or primary key.



---


Below is a Python script to transfer table data directly from Microsoft SQL Server (MSSQL) to PostgreSQL without creating an intermediate CSV file.

It uses `pyodbc` to read MSSQL data and `psycopg2` to insert data into PostgreSQL in batches. It is designed for tables with millions of rows and preserves text containing commas, quotes, newlines, and other special characters.

Key features

* Streams MSSQL rows in batches to limit memory usage.

* Uses parameterized SQL to handle text safely.

* Supports Windows authentication or SQL Server authentication.

* Supports PostgreSQL schemas and configurable batch sizes.

* Can create the target table automatically using a basic MSSQL-to-PostgreSQL type mapping.

* Supports an existing PostgreSQL table as the destination.

## 1. Install dependencies

Bash

```
pip install pyodbc psycopg2-binary
```

You must also install Microsoft's ODBC Driver for SQL Server.

## 2. Create `mssql_to_postgres.py`

This version automatically creates the destination table if it does not exist. If the PostgreSQL table already exists, it inserts into that table instead.

Python

Run

```
import argparse
import sys

import pyodbc
import psycopg2
from psycopg2 import sql
from psycopg2.extras import execute_values


def quote_mssql_identifier(name):
    """Safely quote a SQL Server identifier."""
    return "[" + name.replace("]", "]]") + "]"


def parse_table_name(name):
    """Accept TableName or schema.TableName."""
    parts = name.split(".")

    if len(parts) == 1:
        return "dbo", parts[0]
    if len(parts) == 2:
        return parts[0], parts[1]

    raise ValueError("Use TableName or schema.TableName")


def pg_type(column):
    """Map common MSSQL types to PostgreSQL types."""
    data_type = column["data_type"].lower()
    length = column["max_length"]
    precision = column["precision"]
    scale = column["scale"]

    if data_type in ("varchar", "char", "nvarchar", "nchar"):
        if length == -1:
            return "TEXT"
        if data_type in ("nvarchar", "nchar"):
            length //= 2
        return f"VARCHAR({length})" if length > 0 else "TEXT"

    if data_type in ("text", "ntext", "xml"):
        return "TEXT"
    if data_type == "bit":
        return "BOOLEAN"
    if data_type == "tinyint":
        return "SMALLINT"
    if data_type == "smallint":
        return "SMALLINT"
    if data_type == "int":
        return "INTEGER"
    if data_type == "bigint":
        return "BIGINT"
    if data_type in ("real",):
        return "REAL"
    if data_type in ("float",):
        return "DOUBLE PRECISION"
    if data_type in ("decimal", "numeric", "money", "smallmoney"):
        if data_type == "money":
            return "NUMERIC(19,4)"
        if data_type == "smallmoney":
            return "NUMERIC(10,4)"
        return f"NUMERIC({precision},{scale})"
    if data_type == "date":
        return "DATE"
    if data_type == "time":
        return "TIME"
    if data_type in ("datetime", "datetime2", "smalldatetime"):
        return "TIMESTAMP"
    if data_type == "datetimeoffset":
        return "TIMESTAMPTZ"
    if data_type == "uniqueidentifier":
        return "UUID"
    if data_type in ("binary", "varbinary", "image", "timestamp", "rowversion"):
        return "BYTEA"

    raise ValueError(
        f"Unsupported MSSQL type '{data_type}'. "
        "Add an explicit PostgreSQL mapping before migrating."
    )


def get_columns(cursor, schema, table):
    query = """
        SELECT
            COLUMN_NAME,
            DATA_TYPE,
            CHARACTER_MAXIMUM_LENGTH,
            NUMERIC_PRECISION,
            NUMERIC_SCALE,
            IS_NULLABLE,
            ORDINAL_POSITION
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
        ORDER BY ORDINAL_POSITION
    """
    cursor.execute(query, schema, table)

    columns = []
    for row in cursor.fetchall():
        columns.append({
            "name": row.COLUMN_NAME,
            "data_type": row.DATA_TYPE,
            "max_length": row.CHARACTER_MAXIMUM_LENGTH,
            "precision": row.NUMERIC_PRECISION,
            "scale": row.NUMERIC_SCALE,
            "nullable": row.IS_NULLABLE == "YES",
        })

    if not columns:
        raise ValueError(
            f"Source table not found: {schema}.{table}"
        )

    return columns


def create_target_table(pg_conn, schema, table, columns):
    """Create a basic table if it does not already exist."""
    with pg_conn.cursor() as cur:
        cur.execute(
            "CREATE SCHEMA IF NOT EXISTS " +
            sql.Identifier(schema).as_string(pg_conn)
        )

        cur.execute(
            """
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = %s
                  AND table_name = %s
            )
            """,
            (schema, table),
        )

        if cur.fetchone()[0]:
            return False

        definitions = []
        for col in columns:
            column_definition = sql.SQL("{} {}").format(
                sql.Identifier(col["name"]),
                sql.SQL(pg_type(col)),
            )
            if not col["nullable"]:
                column_definition += sql.SQL(" NOT NULL")
            definitions.append(column_definition)

        statement = sql.SQL(
            "CREATE TABLE {}.{} ({})"
        ).format(
            sql.Identifier(schema),
            sql.Identifier(table),
            sql.SQL(", ").join(definitions),
        )

        cur.execute(statement)

    return True


def transfer_table(args):
    src_schema, src_table = parse_table_name(args.source_table)
    dst_schema, dst_table = parse_table_name(args.target_table)

    source_conn_string = (
        f"DRIVER={{{args.driver}}};"
        f"SERVER={args.mssql_server};"
        f"DATABASE={args.mssql_database};"
    )

    if args.trusted_connection:
        source_conn_string += "Trusted_Connection=yes;"
    else:
        if args.mssql_username is None or args.mssql_password is None:
            raise ValueError(
                "Provide MSSQL username and password, "
                "or use --trusted-connection."
            )
        source_conn_string += (
            f"UID={{{args.mssql_username}}};"
            f"PWD={{{args.mssql_password}}};"
        )

    source_conn_string += (
        "Encrypt=yes;TrustServerCertificate=no;"
    )

    pg_conn = psycopg2.connect(
        host=args.pg_host,
        port=args.pg_port,
        dbname=args.pg_database,
        user=args.pg_username,
        password=args.pg_password,
    )

    total_rows = 0

    try:
        with pyodbc.connect(source_conn_string) as source_conn:
            source_cursor = source_conn.cursor()
            source_cursor.arraysize = args.batch_size

            source_cursor.execute(
                f"SELECT * FROM "
                f"{quote_mssql_identifier(src_schema)}."
                f"{quote_mssql_identifier(src_table)}"
            )

            columns = get_columns(
                source_cursor, src_schema, src_table
            )

            # Source query and metadata must use the same column order.
            column_names = [c["name"] for c in columns]

            created = create_target_table(
                pg_conn, dst_schema, dst_table, columns
            )
            pg_conn.commit()

            if created:
                print(
                    f"Created PostgreSQL table "
                    f"{dst_schema}.{dst_table}"
                )

            insert_query = sql.SQL(
                "INSERT INTO {}.{} ({}) VALUES %s"
            ).format(
                sql.Identifier(dst_schema),
                sql.Identifier(dst_table),
                sql.SQL(", ").join(
                    sql.Identifier(name)
                    for name in column_names
                ),
            ).as_string(pg_conn)

            while True:
                rows = source_cursor.fetchmany(args.batch_size)
                if not rows:
                    break

                values = [tuple(row) for row in rows]

                with pg_conn.cursor() as pg_cursor:
                    execute_values(
                        pg_cursor,
                        insert_query,
                        values,
                        page_size=args.batch_size,
                    )

                # Commit each batch so the entire table is not held
                # in one large transaction.
                pg_conn.commit()

                total_rows += len(rows)
                print(
                    f"\rTransferred {total_rows:,} rows",
                    end="",
                    flush=True,
                )

            source_cursor.close()

        print(f"\nTransfer completed: {total_rows:,} rows")
        print(f"Destination: {dst_schema}.{dst_table}")

    except Exception:
        pg_conn.rollback()
        raise
    finally:
        pg_conn.close()


def main():
    parser = argparse.ArgumentParser(
        description="Transfer an MSSQL table to PostgreSQL."
    )

    parser.add_argument("--mssql-server", required=True)
    parser.add_argument("--mssql-database", required=True)
    parser.add_argument("--source-table", required=True)

    auth = parser.add_mutually_exclusive_group(required=True)
    auth.add_argument("--trusted-connection", action="store_true")
    auth.add_argument("--mssql-username")

    parser.add_argument("--mssql-password")
    parser.add_argument(
        "--driver",
        default="ODBC Driver 18 for SQL Server",
    )

    parser.add_argument("--pg-host", required=True)
    parser.add_argument("--pg-port", type=int, default=5432)
    parser.add_argument("--pg-database", required=True)
    parser.add_argument("--pg-username", required=True)
    parser.add_argument("--pg-password", required=True)

    parser.add_argument(
        "--target-table",
        required=True,
        help="PostgreSQL table, optionally schema.table",
    )
    parser.add_argument("--batch-size", type=int, default=5000)

    args = parser.parse_args()

    if args.batch_size < 1:
        parser.error("--batch-size must be greater than zero")

    if args.mssql_username and args.mssql_password is None:
        parser.error(
            "--mssql-username requires --mssql-password"
        )

    try:
        transfer_table(args)
    except (pyodbc.Error, psycopg2.Error, ValueError, OSError) as exc:
        print(f"\nTransfer failed: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
```

## 3. Run the migration

Example using Windows authentication for MSSQL:

PowerShell

```
python mssql_to_postgres.py `
  --mssql-server "localhost" `
  --mssql-database "MyDatabase" `
  --source-table "dbo.Customers" `
  --trusted-connection `
  --pg-host "localhost" `
  --pg-port 5432 `
  --pg-database "TargetDatabase" `
  --pg-username "postgres" `
  --pg-password "your_password" `
  --target-table "public.customers" `
  --batch-size 5000
```

## 4. Two important corrections before running

The script above needs two adjustments:

A. Use a separate cursor for source metadata. The metadata query currently replaces the pending source `SELECT` result. In `transfer_table()`, replace the section beginning with `source_cursor.execute(...)` and ending with `column_names = ...` with:

Python

Run

```
with source_conn.cursor() as metadata_cursor:
    columns = get_columns(
        metadata_cursor, src_schema, src_table
    )

column_names = [c["name"] for c in columns]

source_cursor.execute(
    f"SELECT * FROM "
    f"{quote_mssql_identifier(src_schema)}."
    f"{quote_mssql_identifier(src_table)}"
)
```

B. Use `public` as the default PostgreSQL schema. Change `parse_table_name` to accept a default schema:

Python

Run

```
def parse_table_name(name, default_schema="dbo"):
    parts = name.split(".")

    if len(parts) == 1:
        return default_schema, parts[0]
    if len(parts) == 2:
        return parts[0], parts[1]

    raise ValueError("Use TableName or schema.TableName")
```

Then update the calls:

Python

Run

```
src_schema, src_table = parse_table_name(
    args.source_table, "dbo"
)

dst_schema, dst_table = parse_table_name(
    args.target_table, "public"
)
```

## 5. Limitations to understand

* Automatic table creation maps common data types but does not migrate primary keys, indexes, foreign keys, triggers, or identity properties.

* If the destination table already contains data, the script appends rows; it does not replace existing data.

* Each batch commits independently. If a later batch fails, earlier batches remain committed.

* For very large tables, PostgreSQL's `COPY` mechanism is usually faster than batched `INSERT` operations.

For a production migration, test on a small table first, then verify the source and destination row counts before migrating millions of records.

