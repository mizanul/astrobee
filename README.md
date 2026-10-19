# Astrobee
		 	 	 		
			
				
					
## Running on your own machine
					
You can also run the program on your own machine. 

This provides a procedure to set up the Astrobee simulator. 
					

### Requirements
					
The following requirements are needed to set up a simulation environment on your machine.
					
64-bit processor
8GBRAM(16GBRAM recommended)
Ubuntu16.04(64-bitversion)(http://releases.ubuntu.com/16.04/)

#Note: You will need 4 GBs of RAM to compile the software. If you don't have that much RAM available, please use swap space.

#Note: Preferably install Ubuntu 16.04. At this time we do not officially support any other operating system or Ubuntu version. Experimental #instructions steps for Ubuntu 18 installation are included

#Note: Please ensure you install the 64-bit version of Ubuntu. We do not support running Astrobee Robot Software on 32-bit systems.
					
### Setting up the Astrobee Robot Software
					
					
Run the follwing script in the Ubuntu terminal
								
```
#!/bin/bash

# Author: Mizanul Hoq Chowdhury, MIT




---


If you only have access to Microsoft SQL Server Management Studio (SSMS), you can still collect useful evidence for IT without installing monitoring software or needing administrator access to the VM.

I recommend focusing on SQL execution time, CPU time, logical reads, query waits, and SQL Server resource statistics. These can help determine whether the export is slow because of database processing or because of the application, network, or CSV file writing.

One important limitation: SSMS alone cannot tell you the VM's complete CPU utilization, disk latency, or operating-system memory pressure. Those measurements will need IT's assistance.

## 1. Measure SQL query execution time

In SSMS, open a new query window, select the correct database, and run the following on a controlled test if the DBA approves it.

```
SET STATISTICS IO ON;
SET STATISTICS TIME ON;

SELECT *
FROM dbo.YourTable;

SET STATISTICS IO OFF;
SET STATISTICS TIME OFF;
```

Replace `dbo.YourTable` with your actual table name and preferably use the same columns and query as your CSV export.

In the Messages tab, record:

- CPU time
- Elapsed time
- Logical reads
- Physical reads, if reported
- Any scan counts and warnings

Caution: This reads the table and sends 92 million rows to SSMS. It can create substantial load and may take a long time. Do not run it on production just for measurement without DBA approval. If possible, use the execution statistics from the existing export query or a representative smaller sample.

## 2. Monitor the export while it is running

If the export runs as a SQL query on the server, open a second SSMS query window and execute:

```
SELECT
    SYSDATETIME() AS sample_time,
    r.session_id,
    r.status,
    r.command,
    r.start_time,
    r.total_elapsed_time / 1000.0 AS elapsed_seconds,
    r.cpu_time / 1000.0 AS cpu_seconds,
    r.reads,
    r.logical_reads,
    r.row_count,
    r.wait_type,
    r.wait_time AS wait_time_ms,
    r.last_wait_type,
    r.blocking_session_id,
    DB_NAME(r.database_id) AS database_name
FROM sys.dm_exec_requests AS r
WHERE r.session_id <> @@SPID
ORDER BY r.total_elapsed_time DESC;
```

Save the results periodically while the export runs. This captures the currently executing SQL requests and their progress indicators. The `row_count` value is not guaranteed to equal the number of rows written to the CSV.

If you receive a permission error, ask your DBA to run it. Server-level visibility requires appropriate permissions, which vary by SQL Server version.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn

+1



## 3. Collect wait statistics

Run this query before and after an approved export:

```
SELECT TOP (20)
    wait_type,
    waiting_tasks_count,
    wait_time_ms,
    signal_wait_time_ms
FROM sys.dm_os_wait_stats
WHERE wait_type NOT LIKE '%SLEEP%'
ORDER BY wait_time_ms DESC;
```

Save both result sets and compare the differences. These statistics accumulate at the SQL Server instance level, so unrelated activity can affect the results.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn



## 4. Calculate the export performance

Use this table in your report.

| Measurement          | Value                          |
| -------------------- | ------------------------------ |
| Total rows           | 92,000,000                     |
| Start time           | Record actual time             |
| Finish time          | Record actual time             |
| Elapsed hours        | Finish − start                 |
| Rows per second      | 92,000,000 ÷ elapsed seconds   |
| CSV file size        | Obtain from the export machine |
| CPU and disk metrics | Request from IT                |

For example, a 10-hour export averages approximately 2,556 rows per second. SSMS alone cannot confirm whether the CSV writer or the VM's storage is the bottleneck.

## 5. What to send IT

You can use this message:

> The VM has been upgraded to 8 CPU cores, but exporting approximately 92 million MSSQL rows to CSV still takes over 10 hours in some cases. I have access to SSMS and will collect query runtime, CPU time, I/O statistics, and SQL Server wait information.
>
> Please help collect VM-level CPU utilization, available memory, disk throughput and latency, and network performance during the same export window. I would also appreciate confirmation of the VM's CPU allocation and storage performance limits.
>
> The objective is to establish the bottleneck and quantify whether the 8-core upgrade improved throughput before requesting further infrastructure changes.

One question so I can tailor the next step: When you export the 92 million rows, which method are you using?

SSMS — right-click table or use Save Results As

Python script using `pyodbc`

SQL Server `bcp` utility

SSIS or another export tool

Another method

Where is SQL Server running relative to the export process?

On the same VM

On a separate server

I am not sure

Continue

sudo apt-get install build-essential git
export ASTROBEE_WS=$HOME/astrobee

git clone https://github.com/nasa/astrobee.git $ASTROBEE_WS/src
pushd $ASTROBEE_WS/src
git submodule update --init --depth 1 description/media
popd

git submodule update --init --depth 1 submodules/android

pushd $ASTROBEE_WS
cd src/scripts/setup
./add_ros_repository.sh
sudo apt-get update
cd debians
./build_install_debians.sh
cd ../
./install_desktop_packages.sh
sudo rosdep init
rosdep update
popd

export WORKSPACE_PATH=$ASTROBEE_WS
export INSTALL_PATH=$ASTROBEE_WS/install

source /opt/ros/kinetic/setup.sh
cd /src/astrobee
catkin clean --setup-files
ls -la $ASTROBEE_WS/src/cmake
CMAKE_PREFIX_PATH=$CMAKE_PREFIX_PATH:$ASTROBEE_WS/src/cmake
cd $ASTROBEE_WS
./src/scripts/configure.sh -l -F -D -p -T $INSTALL_PATH -w $WORKSPACE_PATH
catkin build --status-rate 0.01


```
					
			
				
### Run Astrobee Simulator				

```
pushd $BUILD_PATH
source devel/setup.bash
popd
roslaunch astrobee sim.launch dds:=false robot:=sim_pub rviz:=true

```




## Implementation

A clean enterprise architecture is to keep **React responsible only for UI and authentication state**, while **server.js** owns the authentication flow with Keycloak and the corporate IdP.

The flow should look like this:

```
+-------------------+
| React             |
|-------------------|
| ThemeProvider     |
| AuthProvider      |
| AppLayout         |
| AppContent        |
+---------+---------+
          |
          | REST
          |
+---------v------------------+
| server.js (Express)        |
|----------------------------|
| Passport/OpenID Connect    |
| Session/JWT                |
| Authorization              |
+---------+------------------+
          |
          |
    +-----+------+
    |            |
+---v----+   +---v----------------+
|Keycloak|   |Corporate IdP       |
|Broker  |   |Azure AD/Okta/ADFS  |
+--------+   +--------------------+
```

This keeps all OpenID Connect secrets away from React.

---

# Folder structure

```
src/
    App.tsx
    index.tsx

    auth/
        AuthContext.tsx
        AuthProvider.tsx
        PrivateRoute.tsx
        types.ts
        authApi.ts

    components/
        AppLayout.tsx
        AppContent.tsx
```

---

# Auth Types

**auth/types.ts**

```typescript
export interface User {
    id: string;
    username: string;
    fullName: string;
    email: string;

    roles: string[];

    authenticated: boolean;
}

export interface AuthContextType {

    user: User | null;

    loading: boolean;

    login: () => void;

    logout: () => void;

    refresh: () => Promise<void>;

    hasRole: (role: string) => boolean;
}
```

---

# API

**auth/authApi.ts**

```typescript
export async function getCurrentUser() {

    const response = await fetch("/api/auth/me", {
        credentials: "include"
    });

    if (response.status === 401)
        return null;

    return await response.json();
}
```

---

# Auth Context

**AuthContext.tsx**

```typescript
import { createContext } from "react";
import { AuthContextType } from "./types";

export const AuthContext = createContext<AuthContextType>(
    {} as AuthContextType
);
```

---

# Auth Provider

**AuthProvider.tsx**

```typescript
import React, {
    useState,
    useEffect,
    useCallback
} from "react";

import { AuthContext } from "./AuthContext";
import { User } from "./types";
import { getCurrentUser } from "./authApi";

interface Props {
    children: React.ReactNode;
}

const LOGIN_URL = "/api/auth/login";
const LOGOUT_URL = "/api/auth/logout";

export const AuthProvider: React.FC<Props> = ({ children }) => {

    const [user, setUser] = useState<User | null>(null);

    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {

        setLoading(true);

        const me = await getCurrentUser();

        setUser(me);

        setLoading(false);

    }, []);

    useEffect(() => {
        refresh();
    }, [refresh]);

    const login = () => {

        window.location.href = LOGIN_URL;

    };

    const logout = () => {

        window.location.href = LOGOUT_URL;

    };

    const hasRole = (role: string) => {

        return user?.roles.includes(role) ?? false;

    };

    return (

        <AuthContext.Provider
            value={{
                user,
                loading,
                login,
                logout,
                refresh,
                hasRole
            }}
        >

            {children}

        </AuthContext.Provider>

    );

};
```

---

# Hook

```typescript
import { useContext } from "react";
import { AuthContext } from "./AuthContext";

export const useAuth = () => useContext(AuthContext);
```

---

# Private Route

```typescript
import React from "react";
import { useAuth } from "./useAuth";

interface Props {
    children: React.ReactNode;
}

export const PrivateRoute: React.FC<Props> = ({ children }) => {

    const auth = useAuth();

    if (auth.loading)
        return <div>Loading...</div>;

    if (!auth.user) {

        auth.login();

        return null;
    }

    return <>{children}</>;
};
```

---

# App.tsx

```tsx
import { ThemeProvider } from "./theme";
import { AuthProvider } from "./auth/AuthProvider";
import { PrivateRoute } from "./auth/PrivateRoute";

import AppLayout from "./components/AppLayout";
import AppContent from "./components/AppContent";

const App: React.FC = () => {

    return (

        <ThemeProvider>

            <AuthProvider>

                <PrivateRoute>

                    <AppLayout>

                        <AppContent />

                    </AppLayout>

                </PrivateRoute>

            </AuthProvider>

        </ThemeProvider>

    );

};

export default App;
```

---

# Login Button

```tsx
import { useAuth } from "../auth/useAuth";

export default function LoginButton() {

    const auth = useAuth();

    if (auth.user) {

        return (

            <button onClick={auth.logout}>
                Logout
            </button>

        );

    }

    return (

        <button onClick={auth.login}>
            Login
        </button>

    );

}
```

---

# Server.js API

Your React application never talks directly to Keycloak.

```
GET  /api/auth/login
```

Redirects to

```
Keycloak
```

Keycloak lets the user choose

```
Corporate Login

or

Local Login
```

After login

```
Keycloak
    ↓
server.js callback

/api/auth/callback
```

server.js creates

```
HTTP Only Cookie

or

JWT Cookie
```

React then calls

```
GET /api/auth/me
```

Example response:

```json
{
    "id":"123",
    "username":"john",
    "fullName":"John Smith",
    "email":"john@company.com",
    "roles":[
        "Admin",
        "Report.View"
    ],
    "authenticated":true
}
```

---

# Authorization

Once the authenticated user is available, authorization is straightforward:

```tsx
const auth = useAuth();

if (auth.hasRole("Admin")) {
    return <AdminPage />;
}

return <AccessDenied />;
```

or

```tsx
{auth.hasRole("Report.View") && (
    <ReportMenu />
)}
```

---

## Recommended production architecture

The backend middleware (`server.js` in TypeScript) will manage both authentication and authorization, I recommend **not** using the Keycloak JavaScript adapter in React. Instead:

* React is a thin client that only knows whether a user is authenticated and what roles/permissions have been granted.
* `server.js` handles the full OpenID Connect Authorization Code Flow with PKCE, exchanges tokens with Keycloak, validates them, and stores only a secure, HTTP-only session cookie.
* `server.js` exposes endpoints such as `/api/auth/login`, `/api/auth/callback`, `/api/auth/logout`, and `/api/auth/me`.
* Keycloak is configured as an identity broker so users can authenticate with either local Keycloak accounts or a federated corporate IdP (such as Azure AD, Okta, or ADFS). React does not need to know which identity provider the user chose.
* All application APIs go through `server.js`, which performs role and permission checks before accessing downstream services such as your Cube API or databases.

This architecture scales well for enterprise applications because it centralizes security concerns, keeps tokens out of the browser, and makes it easy to support multiple identity providers without changing the React application.


---
# Investigation

To demonstrate whether the VM upgrade to 8 CPU cores has actually improved the export of 92 million rows from Microsoft SQL Server (MSSQL) to CSV, I recommend conducting a controlled performance investigation and preparing a report based on measurable evidence.

The important point is that exporting 92 million rows is not necessarily a CPU-intensive task. It may be limited by SQL Server query execution, disk I/O, network throughput, Python or .NET processing, CSV serialization, or the database server's own resource constraints. Adding CPU cores will not solve a bottleneck elsewhere.

I would approach this as a small performance benchmarking project with three deliverables:

1. A repeatable export benchmark.
2. A set of CPU, memory, disk, database, and throughput measurements.
3. An evidence-based report for IT showing the bottleneck, the effect of the upgrade, and recommended next steps.

# 1. Establish the baseline performance

Start by recording the current export characteristics. Do not rely only on the total duration.

For 92 million rows, the export speed can be measured as:

\\[ \text{Rows/sec}=\frac{92,000,000}{\text{Elapsed seconds}} \\]

For example, the following table shows hypothetical performance at different durations.

| Export duration | Average rows/sec | Assessment                         |
| --------------- | ---------------- | ---------------------------------- |
| 10 hours        | 2,556            | Very slow throughput               |
| 5 hours         | 5,111            | Better, but potentially improvable |
| 2 hours         | 12,778           | Higher throughput                  |
| 1 hour          | 25,556           | Much higher throughput             |

These are illustrative figures, not measurements from your VM. The actual speed depends on row width, column types, indexes, query complexity, CSV formatting, and output file size.

Also measure the total CSV size and calculate MB/sec. Two exports with the same number of rows can have very different workloads if one table contains large text fields and the other contains mostly numeric values.

# 2. Collect these performance metrics

Measure the entire export process, not just the VM's overall CPU usage. Windows Performance Monitor (PerfMon) can record resource utilization over time, while SQL Server's dynamic management views (DMVs) can help identify database-side waits and bottlenecks.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn

+2



| Area                | Metric to collect                                                                       | What it tells IT                                                          |
| ------------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| CPU                 | Average and peak CPU utilization; per-core usage                                        | Whether the eight cores are being used effectively                        |
| Memory              | Total RAM, available RAM, paging activity                                               | Whether the VM has sufficient memory                                      |
| SQL Server          | Query elapsed time, CPU time, logical and physical reads                                | Whether database query execution is slow                                  |
| SQL Server waits    | `PAGEIOLATCH`, `ASYNC_NETWORK_IO`, `CXPACKET`, `CXCONSUMER` and other significant waits | Whether SQL Server is waiting on storage, the client, or parallel workers |
| Source storage      | Read throughput, latency, queue length                                                  | Whether database reads are constrained by storage                         |
| Destination storage | Write throughput, latency, queue length                                                 | Whether the CSV disk is the bottleneck                                    |
| Network             | Bytes/sec, throughput, latency where available                                          | Whether transferring query results is constrained by the network          |
| Export application  | Rows/sec, MB/sec, elapsed time, CPU and memory                                          | Whether the exporter is the bottleneck                                    |
| Output              | Total rows, CSV size, successful completion                                             | Whether the export completed correctly                                    |

The wait types are clues, not definitive diagnoses. For example, `ASYNC_NETWORK_IO` can mean SQL Server is waiting for the client to consume rows; the client could be slow at fetching, converting, or writing them. Interpret waits alongside application and resource metrics.

## 3. Capture the export application's own metrics

This is particularly important if you are using Python with `pyodbc`, or another application to export the table.

For each export, log the following:

- Start and finish timestamps.
- Source server, database, table and query identifier.
- Total rows exported.
- Total CSV file size in bytes.
- Elapsed time in seconds.
- Average rows per second.
- Average MB/sec.
- Application CPU and peak memory.
- Fetch batch size and CSV-writing method.
- Errors, retries and completion status.

Here is the basic calculation:

\\[ \text{MB/sec}=\frac{\text{CSV bytes}}{1,048,576 \times \text{elapsed seconds}} \\]

Record the number of rows actually written, rather than assuming the table's reported row count equals the exported row count.

# 4. Monitor the Windows VM during export

If your export runs on Windows, use Performance Monitor to collect data throughout the run. Microsoft documents how to create a Data Collector Set and save performance counters for later analysis.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn

+1



Recommended counters:

| Counter                                      | Suggested interval |
| -------------------------------------------- | ------------------ |
| `\Processor Information(*)\% Processor Time` | 15 seconds         |
| `\Memory\Available MBytes`                   | 15 seconds         |
| `\Memory\Pages/sec`                          | 15 seconds         |
| `\PhysicalDisk(*)\Disk Read Bytes/sec`       | 15 seconds         |
| `\PhysicalDisk(*)\Disk Write Bytes/sec`      | 15 seconds         |
| `\PhysicalDisk(*)\Avg. Disk sec/Read`        | 15 seconds         |
| `\PhysicalDisk(*)\Avg. Disk sec/Write`       | 15 seconds         |
| `\PhysicalDisk(*)\Current Disk Queue Length` | 15 seconds         |
| `\Network Interface(*)\Bytes Total/sec`      | 15 seconds         |
| `\Process(*)\% Processor Time`               | 15 seconds         |
| `\Process(*)\Working Set - Private`          | 15 seconds         |

Use the correct physical or logical disk instances for the database files and CSV destination. If SQL Server runs on a different VM, collect its server-side counters too; the exporter VM alone cannot establish whether the database server is constrained.

For an initial investigation, you can start with Task Manager and PerfMon. Keep the full counter log so IT can correlate resource utilization with export progress.

## 5. Collect SQL Server evidence

Ask the DBA or IT team to run the following queries while the export is in progress. These queries are read-only, but access to some server-wide DMVs may require additional permissions.

### A. Check SQL Server configuration and database file locations

```
-- SQL Server version and edition
SELECT
    @@VERSION AS SqlServerVersion;

-- CPU and memory configuration
SELECT
    cpu_count,
    scheduler_count,
    hyperthread_ratio,
    physical_memory_kb / 1024 AS physical_memory_mb,
    max_workers_count
FROM sys.dm_os_sys_info;

-- Database files and their physical locations
SELECT
    DB_NAME(database_id) AS database_name,
    name AS logical_file_name,
    type_desc,
    physical_name,
    size * 8.0 / 1024 AS size_mb
FROM sys.master_files
ORDER BY database_id, file_id;
```

This helps establish whether SQL Server can use the allocated processors and memory and whether database files reside on storage separate from the CSV output.

### B. Identify the export query while it is running

Run this during an active export, adjusting the filter for your application login, database or session ID.

```
SELECT
    r.session_id,
    s.login_name,
    s.host_name,
    s.program_name,
    DB_NAME(r.database_id) AS database_name,
    r.status,
    r.command,
    r.start_time,
    r.total_elapsed_time / 1000.0 AS elapsed_seconds,
    r.cpu_time / 1000.0 AS cpu_seconds,
    r.logical_reads,
    r.reads AS physical_reads,
    r.writes,
    r.wait_type,
    r.wait_time AS current_wait_ms,
    r.wait_resource,
    r.blocking_session_id,
    r.percent_complete,
    SUBSTRING(
        t.text,
        (r.statement_start_offset / 2) + 1,
        (
            (
                CASE
                    WHEN r.statement_end_offset = -1
                    THEN DATALENGTH(t.text)
                    ELSE r.statement_end_offset
                END
                - r.statement_start_offset
            ) / 2
        ) + 1
    ) AS running_statement
FROM sys.dm_exec_requests AS r
JOIN sys.dm_exec_sessions AS s
    ON r.session_id = s.session_id
OUTER APPLY sys.dm_exec_sql_text(r.sql_handle) AS t
WHERE s.is_user_process = 1
ORDER BY r.total_elapsed_time DESC;
```

Important: A normal `SELECT` query may not report meaningful progress in `percent_complete`. Do not use that field alone to judge the percentage of rows exported.

### C. Compare SQL Server wait statistics before and after the export

Capture the following query immediately before the benchmark and again immediately after it finishes.

```
SELECT
    wait_type,
    waiting_tasks_count,
    wait_time_ms,
    signal_wait_time_ms
FROM sys.dm_os_wait_stats
WHERE wait_type NOT LIKE 'SLEEP%'
ORDER BY wait_time_ms DESC;
```

These are cumulative server-wide counters, not counters for just your export. Compare the two snapshots and calculate the changes during the run. Ideally, isolate the benchmark in a controlled window, accounting for other database activity.

Look for evidence of:

- I/O waits: `PAGEIOLATCH_*` may indicate waits for data pages to be read from storage.
- Client consumption waits: `ASYNC_NETWORK_IO` may indicate SQL Server is waiting for the client to consume query results.
- Parallel execution waits: `CXPACKET` and `CXCONSUMER` need interpretation alongside the query plan and CPU usage.
- Memory or worker pressure: memory-grant waits and worker-thread waits may indicate resource constraints.

Microsoft documents these wait statistics as a diagnostic source for SQL Server performance investigations.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn

+1



Do not reset production wait statistics just to simplify this test. Use snapshots and differences instead.

# 6. Prove whether the eight-core upgrade helped

The strongest evidence is a comparison of the same workload before and after the upgrade. If pre-upgrade logs do not exist, establish a baseline now and compare controlled changes going forward.

Use this benchmark plan:

Test 1 — Baseline

Run the current export script unchanged. Capture elapsed time, rows/sec, MB/sec, CPU, memory, disk activity and SQL waits.

Test 2 — Isolate database performance

Have the DBA measure the same query's execution time and resource usage. Compare fetching results with writing CSV where practical.

Test 3 — Check exporter efficiency

Test reasonable fetch batch sizes, such as 10,000, 50,000 and 100,000 rows, where supported by your driver and memory limits. Keep CSV formatting and correctness identical.

Test 4 — Repeat and validate

Run each test two or three times if practical, under comparable system load. Check row counts, data integrity and output size.

Do not run multiple full-table exports simultaneously on a production database. That can distort the results and increase the workload on the same resources you are investigating.

## 7. Interpret the results

Use the following as diagnostic patterns, not absolute thresholds.

| Observation                                             | Likely investigation                                                          |
| ------------------------------------------------------- | ----------------------------------------------------------------------------- |
| CPU consistently near 90–100%, most cores busy          | CPU capacity, query execution and CSV serialization                           |
| One core very busy, other cores mostly idle             | Single-threaded application or query bottleneck                               |
| CPU low, source disk latency high                       | SQL storage throughput or I/O contention                                      |
| CPU low, destination disk saturated                     | CSV output storage bottleneck                                                 |
| SQL `ASYNC_NETWORK_IO` rises while exporter CPU is busy | Client consumption rate and application processing                            |
| Low CPU and disk activity, but low throughput           | Blocking, network limitations, inefficient fetch pattern or application waits |
| Memory low and paging high                              | RAM pressure or excessive memory allocation                                   |
| Eight cores available but overall CPU low               | More CPU cores may not address the limiting resource                          |

A VM with eight cores can still be slow if the export program fetches and writes rows inefficiently. Likewise, high CPU utilization alone does not prove that more cores would help.

# 8. Prepare a performance report for IT

Use a report that presents the actual measurements, not just the observation that an export takes several hours.

Here is a report template you can populate after testing.

## MSSQL-to-CSV Export Performance Investigation

Technical assessment and infrastructure capacity review

1\. Executive summary

The VM has been upgraded to 8 CPU cores. However, exporting approximately 92 million rows from Microsoft SQL Server to CSV continues to take more than 10 hours in some runs. The objective is to determine whether the current infrastructure or export implementation is limiting throughput and whether the CPU upgrade has improved performance.

2\. Workload details

| Item                     | Value                         |
| ------------------------ | ----------------------------- |
| Database platform        | Microsoft SQL Server          |
| Export volume            | Approximately 92 million rows |
| Output format            | CSV                           |
| Export method            | [Python/pyodbc or other]      |
| VM CPU allocation        | 8 cores                       |
| VM memory                | [Record RAM]                  |
| Export destination       | [Local disk/network storage]  |
| Database server location | [Same VM/separate server]     |

3\. Benchmark results

| Metric                   | Before upgrade | After upgrade |
| ------------------------ | -------------- | ------------- |
| CPU cores                | [Value]        | 8             |
| RAM                      | [Value]        | [Value]       |
| Export duration          | [Value]        | [Value]       |
| Average rows/sec         | [Value]        | [Value]       |
| Average MB/sec           | [Value]        | [Value]       |
| Average CPU utilization  | [Value]        | [Value]       |
| Peak CPU utilization     | [Value]        | [Value]       |
| Available memory         | [Value]        | [Value]       |
| Source disk latency      | [Value]        | [Value]       |
| Destination disk latency | [Value]        | [Value]       |
| SQL wait findings        | [Value]        | [Value]       |
| CSV file size            | [Value]        | [Value]       |

4\. Findings

- CPU utilization: [Insert measured evidence]
- Memory utilization: [Insert measured evidence]
- Database query performance: [Insert measured evidence]
- Source and destination storage: [Insert measured evidence]
- Network and exporter behavior: [Insert measured evidence]

5\. Conclusion

Based on the collected metrics, the primary performance constraint appears to be [identified bottleneck]. The effect of the eight-core upgrade is [measured improvement or no measurable improvement]. Further infrastructure changes should be based on the identified constraint rather than CPU allocation alone.

6\. Recommended actions

1. Address the measured bottleneck.
2. Optimize the export process where appropriate.
3. Repeat the benchmark under comparable conditions.
4. Verify data integrity and document the resulting throughput improvement.

## 9. Calculate the improvement objectively

Once you have before-and-after results, report both elapsed-time reduction and throughput improvement.

\\[ \text{Time reduction (\\%)} = \frac{T\_{\text{before}}-T\_{\text{after}}} {T\_{\text{before}}}\times100 \\]

\\[ \text{Throughput improvement (\\%)} = \frac{R\_{\text{after}}-R\_{\text{before}}} {R\_{\text{before}}}\times100 \\]

For example, if the export duration decreased from 12 hours to 10 hours, the time reduction is 16.7%. The throughput improvement is 20%, assuming the same amount of data was exported.

If there are no measurements from before the upgrade, do not claim that the upgrade failed. Report the current performance and establish a baseline for subsequent controlled tests.

## 10. Let's tailor the investigation to your environment

The next step is to identify exactly where the export runs and how it reads the SQL Server data. That determines which monitoring scripts will be most useful.

### Export environment details

Select what applies. Approximate answers are fine.

1\. What operating system runs on the 8-core VM?

Linux (Ubuntu or another distribution)

Windows Server

2\. Where does SQL Server run?

On the same VM as the exporter

On a separate database server or VM

On a managed cloud database

Not sure

3\. What program exports the data?

Python with pyodbc

PowerShell

C# / .NET

SQL Server BCP utility

Another method

4\. Where is the CSV written?

Local VM disk

Network drive or shared storage

Other destination

5\. How much RAM does the VM have?

8 GB

16 GB

32 GB

64 GB or more

Not sure

Prepare tailored investigation scripts

With those details, the most useful next deliverable is a repeatable monitoring script that records export progress and resource utilization into CSV logs, along with a report generator that summarizes the findings for IT.



---

