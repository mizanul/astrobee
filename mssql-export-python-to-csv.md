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
