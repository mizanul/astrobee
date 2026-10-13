# Disk check

## 1. Check memory and disk usage now

Run:

Bash

```
free -h
df -h
uptime
```

## 2. Check whether the kernel killed processes

Bash

```
sudo journalctl -k --since "2026-10-12 00:00:00" --no-pager \
  | grep -Ei 'out of memory|oom-kill|killed process|panic|watchdog'
```

## 3. Check which processes consume the most resources

Bash

```
ps aux --sort=-%mem | head -15
```

Bash

```
ps aux --sort=-%cpu | head -15
```

## 4. Check your STEMX365 services

Bash

```
systemctl --failed
systemctl list-units --type=service --state=running | grep -E 'stemx-prod|nginx'
```

If you use Docker, also run:

Bash

```
docker stats --no-stream
docker ps -a
```

# check docker


This output is useful. Your Docker volumes include several Astrobee build and development volumes, but the listed volumes alone do not explain the 596 GB of disk usage. The largest shown are about 4.2 GB each.

Also, `LINKS 0` means those volumes are not currently attached to a container. That does not automatically mean they are safe to delete. Some may contain data you want to preserve.

Let's identify where the remaining disk space is going before cleaning anything.

## 1. Check Docker images and containers

Run:

Bash

```
docker system df
```

Then:

Bash

```
docker ps -a --size
```

This will help identify large images, stopped containers, and writable container layers.

## 2. Find the largest directories on your server

Run:

Bash

```
sudo du -xhd1 / 2>/dev/null | sort -h
```

Pay particular attention to `/var`, `/opt`, `/root`, and `/home`.

Then inspect Docker's storage directory:

Bash

```
sudo du -xhd1 /var/lib/docker 2>/dev/null | sort -h
```

This will help determine whether the space is being consumed by Docker images, container logs, writable layers, or volumes.

## 3. Check whether container logs are growing excessively

Run:

Bash

```
sudo du -ah /var/lib/docker/containers \
  --include='*-json.log' 2>/dev/null | sort -h | tail -20
```

Large JSON log files can consume substantial disk space if Docker log rotation is not configured.

## 4. Check the CPU count

You haven't shared the output of this command yet:

Bash

```
nproc
```

Your load average was 10.99 shortly after rebooting. If the VPS has only 2 or 4 CPU cores, that is more concerning than if it has 16 cores.

Please send me the output of these three commands first:

Bash

```
sudo du -xhd1 / 2>/dev/null | sort -h
```

Bash

```
sudo du -xhd1 /var/lib/docker 2>/dev/null | sort -h
```

Bash

```
nproc
```


---

This output gives us a significant clue: your Int-Ball simulator container, `541c04e8_intball_krpc`, has a 14.6 GB writable container size. Your other Int-Ball user container is unhealthy, although its writable size is only 1.58 MB.

That 14.6 GB is worth investigating because you recently worked on the Int-Ball simulator and its build process. It may contain generated build artifacts, logs, or other files accumulating inside the container.

However, it does not explain all 141 GB in `overlay2`, so we should check the other large layers too.

## 1. Find what is consuming 14.6 GB inside the Int-Ball container

Run:

Bash

```
docker exec 541c04e8_intball_krpc \
  bash -lc 'du -xhd1 / 2>/dev/null | sort -h'
```

Then inspect likely locations:

Bash

```
docker exec 541c04e8_intball_krpc \
  bash -lc 'du -xhd2 /users /root /tmp /opt 2>/dev/null | sort -h | tail -30'
```

These commands only inspect files; they do not delete anything.

## 2. Check the unhealthy user container

Bash

```
docker inspect 541c04e8_intball_krpc_user \
  --format 'Status={{.State.Status}} Health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} ExitCode={{.State.ExitCode}}'
```

View its health-check failures:

Bash

```
docker inspect 541c04e8_intball_krpc_user \
  --format '{{json .State.Health}}'
```

And inspect its recent logs:

Bash

```
docker logs --tail 100 541c04e8_intball_krpc_user
```

## 3. Check the simulator's logs and disk usage

Bash

```
docker logs --tail 100 541c04e8_intball_krpc
```

Also inspect the container's writable-layer size directly:

Bash

```
docker inspect 541c04e8_intball_krpc \
  --format '{{.GraphDriver.Data.UpperDir}}'
```

Do not delete files directly from Docker's `overlay2` directory. We should identify the large files from inside the container first.

One important distinction: your containers all showing `Up 35 minutes` is consistent with the VPS having been restarted. It does not establish that Int-Ball caused the restart. Let's find out whether its 14.6 GB consists of build files or something else before deciding what to clean up.






---
```
# SQL Server connection settings
$ServerInstance = "YOUR_SERVER\YOUR_INSTANCE"
$Database       = "YourDatabase"
$Schema         = "dbo"
$Table          = "YourTable"

# CSV output file
$OutputFile = "C:\Export\$Table.csv"

# Create output directory if it does not exist
$OutputDirectory = Split-Path $OutputFile
if (!(Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force |
        Out-Null
}

# Load the SqlServer PowerShell module
Import-Module SqlServer -ErrorAction Stop

# Build SQL query
$Query = @"
SELECT *
FROM [$Schema].[$Table];
"@

try {
    Write-Host "Exporting [$Schema].[$Table] from $Database..."

    Invoke-Sqlcmd `
        -ServerInstance $ServerInstance `
        -Database $Database `
        -Query $Query `
        -ErrorAction Stop |
        Export-Csv -Path $OutputFile `
                   -NoTypeInformation `
                   -Encoding UTF8

    Write-Host "Export completed successfully."
    Write-Host "Output file: $OutputFile"
}
catch {
    Write-Error "Export failed: $($_.Exception.Message)"
    exit 1
}
```

---

```
param(
    [Parameter(Mandatory = $true)]
    [string]$Database,

    [Parameter(Mandatory = $true)]
    [string]$Table,

    [string]$Schema = "dbo",

    [string]$ServerInstance = "YOUR_SERVER\YOUR_INSTANCE",

    [string]$OutputDirectory = "C:\Export"
)

$OutputFile = Join-Path $OutputDirectory "$Table.csv"

# Create output directory if needed
if (!(Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force |
        Out-Null
}

Import-Module SqlServer -ErrorAction Stop

# Validate identifiers to prevent SQL injection
foreach ($Name in @($Database, $Schema, $Table)) {
    if ($Name -match '[\[\]]') {
        throw "Database, schema, and table names cannot contain brackets."
    }
}

$Query = "SELECT * FROM [$Schema].[$Table];"

try {
    Invoke-Sqlcmd `
        -ServerInstance $ServerInstance `
        -Database $Database `
        -Query $Query `
        -ErrorAction Stop |
        Export-Csv `
            -Path $OutputFile `
            -NoTypeInformation `
            -Encoding UTF8

    Write-Host "Export completed: $OutputFile"
}
catch {
    Write-Error "Export failed: $($_.Exception.Message)"
    exit 1
}
```




You can pass the database name and table name as command-line parameters to your PowerShell script.

## 1. Modify the script to accept parameters

Save this as `Export-SqlTableToCsv.ps1`.

PowerShell

```
param(
    [Parameter(Mandatory = $true)]
    [string]$Database,

    [Parameter(Mandatory = $true)]
    [string]$Table,

    [string]$Schema = "dbo",

    [string]$ServerInstance = "YOUR_SERVER\YOUR_INSTANCE",

    [string]$OutputDirectory = "C:\Export"
)

$OutputFile = Join-Path $OutputDirectory "$Table.csv"

# Create output directory if needed
if (!(Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force |
        Out-Null
}

Import-Module SqlServer -ErrorAction Stop

# Validate identifiers to prevent SQL injection
foreach ($Name in @($Database, $Schema, $Table)) {
    if ($Name -match '[\[\]]') {
        throw "Database, schema, and table names cannot contain brackets."
    }
}

$Query = "SELECT * FROM [$Schema].[$Table];"

try {
    Invoke-Sqlcmd `
        -ServerInstance $ServerInstance `
        -Database $Database `
        -Query $Query `
        -ErrorAction Stop |
        Export-Csv `
            -Path $OutputFile `
            -NoTypeInformation `
            -Encoding UTF8

    Write-Host "Export completed: $OutputFile"
}
catch {
    Write-Error "Export failed: $($_.Exception.Message)"
    exit 1
}
```

## 2. Run the script with parameters

Pass the database and table names like this:

PowerShell

```
.\Export-SqlTableToCsv.ps1 `
    -Database "SalesDB" `
    -Table "Orders"
```

Specify a different server, schema, or output directory:

PowerShell

```
.\Export-SqlTableToCsv.ps1 `
    -ServerInstance "SQLPROD01" `
    -Database "SalesDB" `
    -Schema "dbo" `
    -Table "Orders" `
    -OutputDirectory "D:\CSVExports"
```

You can also run it on one line:

PowerShell

```
.\Export-SqlTableToCsv.ps1 -Database "SalesDB" -Table "Orders"
```

## 3. Example output

```
Export completed: C:\Export\Orders.csv
```

