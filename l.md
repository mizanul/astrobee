# Logging Framework Implementation Plan

## React Query Builder / SDA Filtering System

## 1. Overview

This document describes the proposed approach for implementing a scalable logging framework for the SDA React Query Builder application.

The goal is to introduce logging that captures:

* User interactions
* Application workflow events
* Query generation activities
* Backend/API execution lifecycle
* Errors and failures

The implementation will be introduced incrementally in three phases:

1. **Phase 1: Browser Console Logging**

   * Establish logging architecture
   * Capture filtering system events
   * Validate event naming and log structure

2. **Phase 2: Central Logging API**

   * Send logs from frontend to backend
   * Introduce server-side log processing

3. **Phase 3: Database Persistence and Analytics**

   * Store logs permanently
   * Enable auditing, troubleshooting, and reporting

---

# 2. Logging Design Principles

The logging architecture follows an approach similar to OpenTelemetry.

The design separates:

### Standard Log Metadata

These fields have a stable schema and will become database columns later.

Example:

```json
{
  "ts": "2026-08-06T12:51:14Z",
  "event": "filter.created",
  "level": "info",
  "session_id": "is3001.GLDBAE054631",
  "trace_id": "abc123",
  "schema_version": 1
}
```

---

### Event Attributes (`attrs`)

The `attrs` object contains application-specific information.

The logging system treats this as an opaque JSON object.

Example:

```json
{
  "attrs": {
    "filter_id": "f1001",
    "filter_type": "dropdown",
    "source_cube": "sales_cube",
    "source_field": "country",
    "operator": "=",
    "value": "USA"
  }
}
```

Advantages:

* No database schema changes when new attributes are introduced.
* Different application modules can log different information.
* The logging service remains generic.

---

# 3. Log Event Naming Convention

Events will use dot-separated names.

Format:

```
<module>.<action>.<state>
```

Examples:

```
filter.created

filter.updated

filter.removed

filter.cleared

query.generated

query.execution.started

query.execution.completed

query.execution.failed
```

Benefits:

Allows flexible querying:

Find all filter events:

```sql
WHERE event LIKE 'filter.%'
```

Find failures:

```sql
WHERE event LIKE '%.failed'
```

Find query activity:

```sql
WHERE event LIKE 'query.%'
```

---

# 4. Log Entry Structure

The standard log format:

```json
{
  "ts": "2026-08-06T12:51:14Z",

  "event": "filter.created",

  "level": "info",

  "message": "User created filter",

  "session_id": "session123",

  "trace_id": "trace456",

  "schema_version": 1,

  "attrs": {

  }
}
```

---

# Phase 1: Browser Console Logging (Detailed)

## Objective

Implement a client-side logging framework inside the React application.

The purpose of Phase 1 is:

* Identify important application events.
* Validate event naming.
* Understand logging frequency.
* Confirm useful attributes.
* Avoid premature database design.

At this stage:

```
React Application

      |

      |

LoggerService

      |

      |

Browser Console
```

No backend changes are required.

---

# Phase 1 Implementation Tasks

## 1. Create LoggerService

Create:

```
src/services/LoggerService.ts
```

Responsibilities:

* Generate timestamps.
* Maintain session ID.
* Generate trace IDs.
* Create standard log structure.
* Output logs to browser console.

Example:

```typescript
logger.info(
    "filter.created",
    "User created filter",
    traceId,
    {
       field:"country",
       value:"USA"
    }
);
```

---

# 2. Initialize Application Session

When the application starts:

```typescript
logger.startSession();
```

The logger creates:

```
session_id
```

Example:

```
web.20260806T125114.v1.x8J9
```

All logs during the user session contain this ID.

Purpose:

* Group all activities from one user session.
* Support troubleshooting.

---

# 3. Implement Trace ID Support

A trace ID groups related operations.

Example workflow:

```
User clicks Run Query

        |
        |
query.generated

        |
        |
query.execution.started

        |
        |
query.execution.completed
```

All events share:

```
trace_id = abc123
```

This allows tracking a complete workflow.

---

# 4. Identify Filtering Components

The first logging scope is the filtering system.

Components to review:

| Component        | Responsibility        | Logging               |
| ---------------- | --------------------- | --------------------- |
| QueryBuilder     | Main query state      | Query changes         |
| FilterPanel      | Filter collection     | Add/remove/clear      |
| FilterRow        | Individual filter     | Edit values/operators |
| Cube API Service | Backend communication | Execution lifecycle   |

---

# 5. Filtering Events to Capture

## User Actions

### Create Filter

Event:

```
filter.created
```

Example:

```json
{
 "attrs":{
   "field":"country",
   "operator":"=",
   "value":"USA"
 }
}
```

---

### Update Filter

Event:

```
filter.updated
```

Attributes:

```json
{
 "old_value":"USA",
 "new_value":"Canada"
}
```

---

### Remove Filter

Event:

```
filter.removed
```

Attributes:

```json
{
 "filter_id":"123"
}
```

---

### Clear Filters

Event:

```
filter.cleared
```

Attributes:

```json
{
 "previous_filter_count":5
}
```

---

# 6. Query Generation Logging

When the application creates the Cube query:

Event:

```
query.generated
```

Example:

```json
{
 "attrs":{
    "query":{
       "filters":[]
    }
 }
}
```

Purpose:

* Debug incorrect queries.
* Understand user workflow.

---

# 7. Query Execution Logging

## Execution Started

Event:

```
query.execution.started
```

Attributes:

```json
{
 "request_id":"REQ001"
}
```

---

## Execution Completed

Event:

```
query.execution.completed
```

Attributes:

```json
{
 "duration_ms":520,
 "status":"success"
}
```

---

## Execution Failed

Event:

```
query.execution.failed
```

Attributes:

```json
{
 "error":"Timeout"
}
```

---

# Phase 1 Deliverables

At the end of Phase 1:

## Code

Implemented:

```
LoggerService.ts

events.ts

loggingHelpers.ts
```

---

## Documentation

Completed:

* Event naming convention
* Component logging map
* List of filter events
* Sample JSON logs

---

## Validation

Verify:

* Filter creation logs correctly.
* Filter editing logs correctly.
* Filter removal logs correctly.
* Query generation logs correctly.
* Query failures are captured.

---

# Phase 2: Central Logging API (High Level)

## Objective

Move logs from the browser into a centralized backend service.

Architecture:

```
React

 |

LoggerService

 |

POST /api/logs

 |

Backend Logging Service
```

Changes:

* LoggerService sends HTTP requests.
* Backend validates log format.
* Backend attaches server metadata.

The React components do not change.

Example:

Before:

```typescript
console.log(logEntry)
```

After:

```typescript
axios.post(
 "/api/logs",
 logEntry
)
```

---

# Phase 2 Goals

* Centralize logs.
* Support multiple users.
* Preserve session and trace relationships.
* Enable backend monitoring.

---

# Phase 3: Database Persistence and Analytics (High Level)

## Objective

Persist logs for long-term usage.

Database design:

Fixed columns:

```
log_id

timestamp

event

level

session_id

trace_id

schema_version

message

attrs(JSON)
```

---

Example:

| Column     | Purpose             |
| ---------- | ------------------- |
| timestamp  | When event happened |
| event      | Event name          |
| level      | Severity            |
| session_id | User session        |
| trace_id   | Workflow tracking   |
| attrs      | Flexible JSON data  |

---

# Phase 3 Capabilities

After database storage:

## Troubleshooting

Example:

Find failed queries:

```sql
WHERE event LIKE 'query%.failed'
```

---

## User Activity Analysis

Example:

```
Which filters are used most?
```

---

## Performance Monitoring

Example:

```
Average query execution time
```

---

## Audit History

Example:

```
Who changed a dashboard filter?
```

---

# Final Architecture

```
                 React Application

                       |

                LoggerService

                       |

          ----------------------------

          |                          |

     Phase 1                    Phase 2

     Console                    API

                                     |

                                  Phase 3

                                Database


```

---

# Summary

The proposed logging implementation starts with a lightweight browser-based logging framework and evolves into a complete observability solution.

Phase 1 establishes the event model, naming convention, and logging patterns. Phase 2 introduces centralized collection through backend APIs. Phase 3 enables persistent storage, monitoring, auditing, and analytics.

The design keeps the core schema stable while allowing application-specific data to evolve through the flexible `attrs` JSON structure. This prevents frequent database changes and provides a scalable foundation for logging across the SDA platform.



============END DOC==========










































I think this is **more scalable and industry-standard design** than the original schema. This is very similar to how **OpenTelemetry**, **Datadog**, **Elastic**, and **Splunk** structure logs.

The biggest change is this:

> **Instead of creating a rigid schema with `filter`, `query`, `execution`, etc., keep only a few standardized top-level fields and put everything else into `attrs`.**

This has several advantages:

* New features don't require database schema changes.
* Different modules can log different data.
* The logging service stays generic.
* Queries remain efficient because important fields (`event`, `level`, `timestamp`, etc.) are indexed.

---

# I would simplify your schema to something like this

```typescript
export interface LogEntry {
    ts: string;
    event: string;
    level: "debug" | "info" | "warn" | "error";

    session_id: string;
    trace_id?: string;

    schema_version: number;

    message?: string;

    attrs?: Record<string, unknown>;
}
```

Notice there is **no FilterInfo**, **no QueryInfo**, **no ExecutionInfo**.

Everything goes into attrs.

Example

```json
{
    "ts": "...",
    "event": "filter.created",
    "level": "info",

    "session_id": "...",

    "trace_id": "...",

    "schema_version": 1,

    "message": "User created filter",

    "attrs": {
        "filter_id": "123",
        "field": "Country",
        "operator": "=",
        "value": "USA"
    }
}
```

---

# I also agree with dot-separated event names

Instead of

```
FILTER_CREATED
```

use

```
filter.created
```

Instead of

```
QUERY_EXECUTION_STARTED
```

use

```
query.execution.started
```

Instead of

```
QUERY_EXECUTION_FAILED
```

use

```
query.execution.failed
```

Then later your database can do

```sql
WHERE event LIKE '%.failed'
```

or

```sql
WHERE event LIKE 'filter.%'
```

which is very powerful.

---

# I would even create constants

```typescript
export const ev = {

    filter: {

        created: "filter.created",

        updated: "filter.updated",

        removed: "filter.removed",

        cleared: "filter.cleared",

        prefetch: {

            started: "filter.prefetch.started",

            completed: "filter.prefetch.completed",

            failed: "filter.prefetch.failed"

        }

    },

    query: {

        generated: "query.generated",

        execution: {

            started: "query.execution.started",

            completed: "query.execution.completed",

            failed: "query.execution.failed"

        }

    }

};
```

Now nobody mistypes event names.

---

# I like his Trace ID idea too

Imagine

```
User clicks Refresh
```

This causes

```
Generate query

↓

Call Cube

↓

Receive data

↓

Render Chart
```

All those logs should have the **same trace_id**

```
trace_id = a4b782
```

Then later

```
SELECT *

WHERE trace_id='a4b782'
```

shows the whole request.

---

# Session ID

I also agree.

On startup

```typescript
Logger.startSession();
```

might generate

```
session_id =
is3001.GLDBAE054631.20260806T125114.v0.1.1.x6J8
```

Every log automatically contains it.

The React code never passes it.

---

# attrs helper

I especially like this idea.

Instead of

```typescript
attrs: {

    filter.field,

    filter.operator,

    filter.value,

    filter.id

}
```

everywhere

you do

```typescript
attrs: {

    ...getFilterInfo(filter),

    cube: cubeName

}
```

Now if the Filter object changes later,

only

```
getFilterInfo()
```

changes.

---

# Overall architecture I'd recommend

```text
React Component
       │
       ▼
Logger.info(
    ev.filter.created,
    "User created filter",
    traceId,
    {
        ...getFilterInfo(filter),
        cube: cubeName
    }
)
       │
       ▼
LoggerService
       │
       ├── Phase 1
       │      console.log(logEntry)
       │
       ├── Phase 2
       │      POST /logs
       │
       └── Phase 3
              Database
```

## My only suggestion to your lead

The only thing I'd consider adding is a **`component`** field at the top level. It makes it much easier to identify where logs originate without inspecting `attrs`.

```json
{
  "ts": "...",
  "event": "filter.prefetch.started",
  "level": "info",
  "component": "QueryBuilder",
  "session_id": "...",
  "trace_id": "...",
  "schema_version": 1,
  "message": "Filter prefetch started",
  "attrs": {
    "filter_id": "...",
    "source_cube": "...",
    "field": "Country"
  }
}
```

This keeps the schema lightweight while making log filtering and debugging easier. Overall, your lead's proposal is well aligned with modern observability practices and is a strong foundation for a logging system that can grow with the application.




## difference 


The main difference is **where the structure lives**.

### My original proposal: Strongly typed, domain-specific schema

I originally suggested something like:

```json
{
  "event": {
    "category": "FILTER",
    "action": "FILTER_CREATED"
  },
  "filter": {
    "field": "Country",
    "operator": "=",
    "value": "USA"
  },
  "query": {
    ...
  },
  "execution": {
    ...
  }
}
```

with TypeScript interfaces like

```ts
interface FilterInfo {}
interface QueryInfo {}
interface ExecutionInfo {}
```

### Advantages

* Very readable.
* Strong compile-time type checking.
* Easy for new developers to understand.
* Great for a single application with a stable schema.

### Disadvantages

Every time a new feature needs to log something, you end up asking:

> Should I add another interface?

For example:

```
VisualizationInfo
ExportInfo
DatasetInfo
AuthenticationInfo
CacheInfo
```

The logger keeps growing.

---

# Your lead's proposal

Instead, your lead says:

> **Don't make the logger understand every domain.**

The logger should only know things like

```json
{
    "ts": "...",
    "event": "...",
    "level": "...",
    "trace_id": "...",
    "attrs": { }
}
```

Everything else goes into

```json
attrs
```

Example

```json
{
    "event":"filter.created",

    "attrs":{
        "field":"Country",
        "operator":"=",
        "value":"USA"
    }
}
```

Next week another developer logs

```json
{
    "event":"chart.export.started",

    "attrs":{
        "chart":"Sales",
        "format":"pdf",
        "pageCount":5
    }
}
```

No Logger changes.

No DB changes.

No interface changes.

---

# Biggest architectural difference

## Mine

The logger understands the business objects.

```
Logger

├── FilterInfo

├── QueryInfo

├── ExecutionInfo

├── BrowserInfo

├── ErrorInfo

└── MetadataInfo
```

The logger knows your application.

---

## His

The logger understands almost nothing.

```
Logger

├── timestamp

├── event

├── level

├── session

├── trace

└── attrs
```

Everything else belongs to the caller.

---

# Example

Suppose tomorrow somebody implements

```
Export to Excel
```

### My first design

Need

```ts
interface ExportInfo {

    fileName

    format

    rows

}
```

Need

```
LogEntry

↓

export?: ExportInfo
```

Need

```
Logger.exportStarted(...)
```

Need to update schema.

---

### His design

Nothing changes.

Just

```ts
logger.info(

    ev.export.started,

    "Started export",

    traceId,

    {

        filename,

        rows,

        format

    }

);
```

Done.

---

# Database difference

### My original

Database stores

```
filter.field

filter.operator

execution.duration

browser.name

...
```

Very structured.

---

### His

Database stores

```
event

ts

trace_id

session_id

attrs(JSON)
```

The DB doesn't care what's inside attrs.

---

# Why companies like OpenTelemetry do this

Imagine a company has

* React
* Java
* Python
* Go
* C++
* Rust

Every language logs different information.

If the schema were rigid, every team would constantly ask:

> Can we add another field?

Instead they standardized only

```
timestamp

level

event

trace

attributes
```

Everything else is free-form.

That's exactly what your lead is suggesting.

---

# What I would do today

After seeing your lead's comments, **I would change my recommendation**.

I would keep only these top-level fields:

```ts
interface LogEntry {

    ts: string;

    event: string;

    level: LogLevel;

    message?: string;

    session_id: string;

    trace_id?: string;

    schema_version: number;

    attrs: Record<string, unknown>;
}
```

Then build helper functions:

```ts
getFilterInfo(filter)

getQueryInfo(query)

getDatasetInfo(dataset)

getChartInfo(chart)
```

so components become

```ts
logger.info(
    ev.filter.created,
    "User created filter",
    traceId,
    {
        ...getFilterInfo(filter),
        cube: cubeName
    }
);
```

I think this gives you the best of both worlds:

* **The logger stays generic** and stable.
* **The application code stays typed** through helper functions like `getFilterInfo()`.
* **The database schema remains simple** with just a handful of indexed columns and a JSON `attrs` column.
* **Adding new features doesn't require modifying the logger or the database schema.**

That's why I think your lead's design is a better long-term architecture for a logging framework that will be used across multiple parts of the application.



## New Logger Service:


I like your lead's direction. I'd make one small improvement: instead of hard-coding `info()`, I'd support all log levels (`debug`, `info`, `warn`, `error`) from the start, since it's only a few extra lines and avoids future refactoring.

Here's a complete `LoggerService.ts` that follows your lead's architecture.

```typescript
// src/services/LoggerService.ts

export type LogLevel = "debug" | "info" | "warn" | "error";

export interface LogEntry {
  ts: string;
  event: string;
  level: LogLevel;

  session_id: string;
  trace_id?: string;

  schema_version: number;

  message?: string;

  attrs?: Record<string, unknown>;
}

class LoggerService {
  private static readonly SCHEMA_VERSION = 1;

  private sessionId = "";

  constructor() {
    this.startSession();
  }

  /**
   * Creates a new session.
   * Call once when the application starts.
   */
  public startSession(): void {
    const now = new Date();

    const timestamp =
      now.getFullYear().toString() +
      String(now.getMonth() + 1).padStart(2, "0") +
      String(now.getDate()).padStart(2, "0") +
      "T" +
      String(now.getHours()).padStart(2, "0") +
      String(now.getMinutes()).padStart(2, "0") +
      String(now.getSeconds()).padStart(2, "0");

    const random = Math.random().toString(36).substring(2, 8);

    this.sessionId = `web.${timestamp}.v1.${random}`;
  }

  public getSessionId(): string {
    return this.sessionId;
  }

  /**
   * Generate a trace id for a workflow.
   * Example:
   * const traceId = logger.newTraceId();
   */
  public newTraceId(): string {
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
      return crypto.randomUUID();
    }

    return (
      Date.now().toString(36) +
      "-" +
      Math.random().toString(36).substring(2, 10)
    );
  }

  private createLog(
    level: LogLevel,
    event: string,
    message?: string,
    traceId?: string,
    attrs?: Record<string, unknown>
  ): LogEntry {
    return {
      ts: new Date().toISOString(),

      event,

      level,

      session_id: this.sessionId,

      trace_id: traceId,

      schema_version: LoggerService.SCHEMA_VERSION,

      message,

      attrs,
    };
  }

  private write(log: LogEntry): void {
    // -------- Phase 1 --------
    console.log(log);

    // -------- Phase 2 --------
    // await axios.post("/api/logs", log);

    // -------- Phase 3 --------
    // backend stores in DB
  }

  public debug(
    event: string,
    message?: string,
    traceId?: string,
    attrs?: Record<string, unknown>
  ): void {
    this.write(
      this.createLog(
        "debug",
        event,
        message,
        traceId,
        attrs
      )
    );
  }

  public info(
    event: string,
    message?: string,
    traceId?: string,
    attrs?: Record<string, unknown>
  ): void {
    this.write(
      this.createLog(
        "info",
        event,
        message,
        traceId,
        attrs
      )
    );
  }

  public warn(
    event: string,
    message?: string,
    traceId?: string,
    attrs?: Record<string, unknown>
  ): void {
    this.write(
      this.createLog(
        "warn",
        event,
        message,
        traceId,
        attrs
      )
    );
  }

  public error(
    event: string,
    message?: string,
    traceId?: string,
    attrs?: Record<string, unknown>
  ): void {
    this.write(
      this.createLog(
        "error",
        event,
        message,
        traceId,
        attrs
      )
    );
  }
}

const logger = new LoggerService();

export default logger;
```

---

# Event Constants

I would keep event names in one place instead of writing strings throughout the application.

```typescript
// events.ts

export const ev = {
  filter: {
    created: "filter.created",
    updated: "filter.updated",
    removed: "filter.removed",
    cleared: "filter.cleared",

    prefetch: {
      started: "filter.prefetch.started",
      completed: "filter.prefetch.completed",
      failed: "filter.prefetch.failed",
    },
  },

  query: {
    generated: "query.generated",

    execution: {
      started: "query.execution.started",
      completed: "query.execution.completed",
      failed: "query.execution.failed",
    },
  },

  api: {
    request: {
      started: "api.request.started",
      completed: "api.request.completed",
      failed: "api.request.failed",
    },
  },
} as const;
```

---

# Helper Functions

Instead of repeatedly building attribute objects, create reusable helpers.

```typescript
// loggingHelpers.ts

export function getFilterInfo(filter: any) {
  return {
    filter_id: filter.id,
    filter_type: filter.type,
    source_cube: filter.cube,
    source_field: filter.field,
    operator: filter.operator,
    value: filter.value,
  };
}
```

---

# Example Usage

```typescript
import logger from "./LoggerService";
import { ev } from "./events";
import { getFilterInfo } from "./loggingHelpers";

const traceId = logger.newTraceId();

logger.info(
  ev.filter.created,
  "User added Country filter",
  traceId,
  {
    ...getFilterInfo(filter),
    page: "Dashboard",
  }
);
```

Console output:

```json
{
  "ts": "2026-08-06T14:31:05.152Z",
  "event": "filter.created",
  "level": "info",
  "session_id": "web.20260806T143105.v1.k8j3hf",
  "trace_id": "4b77e1d4-79a8-4b3c-bd27-f01fb8f06d73",
  "schema_version": 1,
  "message": "User added Country filter",
  "attrs": {
    "filter_id": "f123",
    "filter_type": "dropdown",
    "source_cube": "Orders",
    "source_field": "country",
    "operator": "=",
    "value": "USA",
    "page": "Dashboard"
  }
}
```

This design stays close to your lead's proposal while keeping the service small, generic, and easy to evolve. The only fixed contract is the top-level metadata; everything application-specific lives in `attrs`.






==================== END ====================

==================== END ====================

==================== END ====================
































For a **1-day exploration task**, I would **not start by thinking about the database**. Instead, design the logging in a way that the logging destination can be changed later.

The phases should be:

> **UI Event → Logging Service → Console (Phase 1) → API (Phase 2) → Database (Phase 3)**

This way you only change one place later.

---

# Phase 1 - Identify Scope

First understand how React Query Builder works in your application.

Typical flow looks like this:

```
React Page
    │
    ▼
<QueryBuilder />
    │
    ▼
onQueryChange()
    │
    ▼
React State
    │
    ▼
Cube Query Generation
    │
    ▼
Cube API
```

The logging should exist in **multiple layers**, not just the UI.

---

# Components to Log

I would divide it into four layers.

## Layer 1 – User Interaction

Logs what the user actually did.

Example:

```
User clicked Add Filter
User changed Operator
User changed Value
User deleted Filter
User cleared Filters
```

This is usually inside React components.

Possible files:

```
QueryBuilder.jsx
FilterBuilder.jsx
FilterRow.jsx
FilterPanel.jsx
```

---

## Layer 2 – Application State

Whenever the filter state changes.

Example

Before

```
Country = USA
```

After

```
Country = Canada
```

or

```
Before:
2 filters

After:
3 filters
```

Usually this happens in

```
onQueryChange()

setQuery()

dispatch()

Reducer
```

---

## Layer 3 – Query Generation

This is very valuable.

When React generates Cube query.

Example

```
{
  dimensions: [],
  measures: [],
  filters:[
      ...
  ]
}
```

Log

```
Generated Cube Query
```

This helps debugging enormously.

---

## Layer 4 – API Execution

When Cube API is called.

Log

```
Request Started

Request Finished

Execution Time

Status Code

Failure
```

Example

```
Start:
16:20:10

End:
16:20:10.452

Duration:
452 ms
```

---

# User Events to Capture

For the exploration I'd recommend this table.

| Event            | Description         |
| ---------------- | ------------------- |
| Filter Created   | User added filter   |
| Filter Edited    | Field changed       |
| Operator Changed | = → IN              |
| Value Changed    | USA → Canada        |
| Filter Removed   | One filter removed  |
| Clear Filters    | All removed         |
| Query Generated  | Cube JSON created   |
| Query Started    | API request started |
| Query Completed  | Success             |
| Query Failed     | Exception           |

---

# Phase 1 Implementation

Do **NOT** sprinkle `console.log()` everywhere.

Instead create one logging service.

Example

```
src/
    services/
        Logger.js
```

Example

```javascript
class Logger {

    log(event, data = {}) {

        console.log({
            time: new Date().toISOString(),
            event,
            data
        });

    }

}

export default new Logger();
```

Now everywhere else simply call

```javascript
Logger.log("FILTER_CREATED", {
    field: "Country",
    operator: "=",
    value: "USA"
});
```

Instead of

```
console.log(...)
console.log(...)
console.log(...)
```

This makes Phase 2 almost free.

---

# Example Flow

User clicks

```
+ Add Filter
```

Console

```
[16:20:11]

Event:
FILTER_CREATED

Field:
Country

Operator:
=

Value:
USA
```

---

User edits

```
USA

↓

Canada
```

Console

```
FILTER_UPDATED

Old Value:
USA

New Value:
Canada
```

---

User removes

```
FILTER_REMOVED

Field:
Country
```

---

User clicks

```
Clear Filters
```

Console

```
FILTERS_CLEARED

Previous Count:
5
```

---

# Phase 2 - Replace Console with API

At this point **nothing changes in the components**.

Instead

```
Logger.log(...)
```

becomes

```javascript
await axios.post("/api/logs", {

    event,
    data,
    timestamp

});
```

Every component still calls

```
Logger.log(...)
```

No code changes elsewhere.

---

# Phase 3 - Store in Database

Backend

```
POST /api/logs
```

↓

Flask

↓

Database

Example table

```
ActivityLog

-------------
id

timestamp

user_id

page

event

action

details

duration

status

ip_address
```

Example record

| time  | user   | event          |
| ----- | ------ | -------------- |
| 16:20 | jsmith | FILTER_CREATED |

---

# Suggested Folder Structure

```
src/

    components/
        QueryBuilder/
        FilterPanel/
        FilterRow/

    services/
        Logger.js

    api/
        CubeApi.js

    pages/
        Dashboard.jsx
```

---

# Files Likely to Update

| File                | Responsibility              | Logging Events                        |
| ------------------- | --------------------------- | ------------------------------------- |
| `QueryBuilder.jsx`  | Main query builder          | Query changed, clear filters          |
| `FilterPanel.jsx`   | Filter container            | Add/remove filter                     |
| `FilterRow.jsx`     | Individual filter           | Edit field, operator, value           |
| `CubeApi.js`        | API communication           | Request start/end, duration, failures |
| `Logger.js` *(new)* | Centralized logging service | Console now, API later                |

---

# One-Day Deliverable (A/C)

For your exploration, I would produce a short design document with:

1. **Scope**

   * Filter creation
   * Filter editing
   * Filter removal
   * Clear filters
   * Cube query generation
   * Cube API execution

2. **Components/Files**

   * `QueryBuilder.jsx`
   * `FilterPanel.jsx`
   * `FilterRow.jsx`
   * `CubeApi.js`
   * `Logger.js` (new)

3. **Logging Events**

   * `FILTER_CREATED`
   * `FILTER_UPDATED`
   * `FILTER_REMOVED`
   * `FILTERS_CLEARED`
   * `QUERY_GENERATED`
   * `QUERY_EXECUTION_STARTED`
   * `QUERY_EXECUTION_COMPLETED`
   * `QUERY_EXECUTION_FAILED`

4. **Implementation Plan**

   * Introduce a centralized `Logger` service that initially writes structured logs to the browser console.
   * Replace the console implementation with an API call in Phase 2 without changing calling components.
   * Persist logs in a backend database in Phase 3 through a `/api/logs` endpoint.

This approach is clean, extensible, and minimizes future refactoring because the UI components remain unaware of where logs are ultimately stored.





=============================================

For a logging service, I would **not** implement it as `React.FC`. A logging service is not a UI component, so it should be a plain TypeScript module or class. `React.FC` is intended for components that render JSX.

A recommended implementation is:

```tsx
// src/services/LoggingService.ts

export enum LogEvent {
  FILTER_CREATED = "FILTER_CREATED",
  FILTER_UPDATED = "FILTER_UPDATED",
  FILTER_REMOVED = "FILTER_REMOVED",
  FILTERS_CLEARED = "FILTERS_CLEARED",
  QUERY_GENERATED = "QUERY_GENERATED",
  QUERY_EXECUTION_STARTED = "QUERY_EXECUTION_STARTED",
  QUERY_EXECUTION_COMPLETED = "QUERY_EXECUTION_COMPLETED",
  QUERY_EXECUTION_FAILED = "QUERY_EXECUTION_FAILED",
}

export interface LogEntry {
  event: LogEvent;
  message?: string;
  data?: Record<string, unknown>;
}

class LoggingService {
  public log(entry: LogEntry): void {
    console.log({
      timestamp: new Date().toISOString(),
      event: entry.event,
      message: entry.message,
      data: entry.data,
    });
  }

  public info(message: string, data?: Record<string, unknown>): void {
    this.log({
      event: LogEvent.QUERY_GENERATED,
      message,
      data,
    });
  }

  public error(message: string, data?: Record<string, unknown>): void {
    console.error({
      timestamp: new Date().toISOString(),
      message,
      data,
    });
  }
}

export default new LoggingService();
```

Usage:

```tsx
import LoggingService, { LogEvent } from "../services/LoggingService";

LoggingService.log({
  event: LogEvent.FILTER_CREATED,
  message: "User added a filter",
  data: {
    field: "Country",
    operator: "=",
    value: "USA",
  },
});
```

---

### If your team specifically asked for a `React.FC`

Although it's not recommended for a service, you could wrap the service in a React Context provider:

```tsx
// LoggingService.tsx

import React, { createContext, ReactNode, useContext } from "react";

export interface LoggingContextType {
  log: (event: string, data?: unknown) => void;
}

const LoggingContext = createContext<LoggingContextType | null>(null);

export const LoggingProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const log = (event: string, data?: unknown) => {
    console.log({
      timestamp: new Date().toISOString(),
      event,
      data,
    });
  };

  return (
    <LoggingContext.Provider value={{ log }}>
      {children}
    </LoggingContext.Provider>
  );
};

export const useLogger = (): LoggingContextType => {
  const context = useContext(LoggingContext);

  if (!context) {
    throw new Error("useLogger must be used within LoggingProvider");
  }

  return context;
};
```

Then use it in components:

```tsx
const { log } = useLogger();

log("FILTER_CREATED", {
  field: "Country",
  operator: "=",
  value: "USA",
});
```

For your exploration task, I recommend the **first approach (plain `LoggingService.ts`)**. It is simpler, follows common React/TypeScript architecture, and can later be extended to send logs to your Flask backend without changing the calling components.








A good logging schema should be **generic enough to work for any UI event**, not just filtering. The filtering events become one category of log events. This allows the same schema to later log chart creation, dashboard actions, authentication, API calls, etc.

## JSON Schema

```json
{
  "id": "uuid",
  "timestamp": "2026-08-05T17:05:21.123Z",

  "application": {
    "name": "SDA",
    "version": "1.0.0",
    "environment": "development"
  },

  "session": {
    "sessionId": "4e5d1f9d-3f1f-44f4-b6c4-0f8d1c0f56ab",
    "userId": "jsmith",
    "userName": "John Smith",
    "role": "Analyst"
  },

  "page": {
    "url": "/dashboard",
    "component": "QueryBuilder",
    "module": "Filtering"
  },

  "event": {
    "category": "FILTER",
    "action": "FILTER_CREATED",
    "description": "User created a new filter",
    "severity": "INFO"
  },

  "filter": {
    "filterId": "filter-001",
    "field": "Country",
    "operator": "=",
    "oldValue": null,
    "newValue": "USA",
    "filterCount": 3
  },

  "query": {
    "generatedQuery": {
      "filters": [
        {
          "member": "Country",
          "operator": "equals",
          "values": ["USA"]
        }
      ],
      "measures": [
        "Sales.count"
      ]
    }
  },

  "execution": {
    "requestId": "abc-123",
    "startTime": "2026-08-05T17:05:21.123Z",
    "endTime": "2026-08-05T17:05:21.845Z",
    "durationMs": 722,
    "status": "SUCCESS"
  },

  "browser": {
    "name": "Chrome",
    "version": "138",
    "platform": "macOS"
  },

  "error": {
    "code": null,
    "message": null,
    "stackTrace": null
  },

  "metadata": {
    "ipAddress": "",
    "feature": "Query Builder",
    "notes": ""
  }
}
```

---

# Example 1 - Filter Created

```json
{
  "timestamp": "2026-08-05T17:08:30Z",
  "event": {
    "category": "FILTER",
    "action": "FILTER_CREATED",
    "severity": "INFO"
  },
  "filter": {
    "field": "Country",
    "operator": "=",
    "newValue": "USA"
  }
}
```

Console

```
[2026-08-05 17:08:30]

FILTER_CREATED

Country = USA
```

---

# Example 2 - Filter Edited

```json
{
  "timestamp": "2026-08-05T17:10:15Z",
  "event": {
    "category": "FILTER",
    "action": "FILTER_UPDATED",
    "severity": "INFO"
  },
  "filter": {
    "field": "Country",
    "operator": "=",
    "oldValue": "USA",
    "newValue": "Canada"
  }
}
```

---

# Example 3 - Filter Removed

```json
{
  "timestamp": "2026-08-05T17:11:00Z",
  "event": {
    "category": "FILTER",
    "action": "FILTER_REMOVED",
    "severity": "INFO"
  },
  "filter": {
    "field": "Country"
  }
}
```

---

# Example 4 - Clear All Filters

```json
{
  "timestamp": "2026-08-05T17:12:40Z",
  "event": {
    "category": "FILTER",
    "action": "FILTERS_CLEARED",
    "severity": "INFO"
  },
  "filter": {
    "filterCount": 7
  }
}
```

---

# Example 5 - Query Generated

```json
{
  "timestamp": "2026-08-05T17:13:20Z",
  "event": {
    "category": "QUERY",
    "action": "QUERY_GENERATED"
  },
  "query": {
    "generatedQuery": {
      "filters": [
        {
          "member": "Country",
          "operator": "equals",
          "values": [
            "USA"
          ]
        }
      ]
    }
  }
}
```

---

# Example 6 - Query Execution

```json
{
  "timestamp": "2026-08-05T17:14:10Z",
  "event": {
    "category": "API",
    "action": "QUERY_EXECUTION_COMPLETED"
  },
  "execution": {
    "startTime": "2026-08-05T17:14:10.000Z",
    "endTime": "2026-08-05T17:14:10.652Z",
    "durationMs": 652,
    "status": "SUCCESS"
  }
}
```

---

# Recommended Event Types

| Category | Actions                                                                    |
| -------- | -------------------------------------------------------------------------- |
| FILTER   | FILTER_CREATED, FILTER_UPDATED, FILTER_REMOVED, FILTERS_CLEARED            |
| QUERY    | QUERY_GENERATED, QUERY_VALIDATED                                           |
| API      | QUERY_EXECUTION_STARTED, QUERY_EXECUTION_COMPLETED, QUERY_EXECUTION_FAILED |
| AUTH     | LOGIN, LOGOUT, SESSION_EXPIRED                                             |
| CHART    | CHART_CREATED, CHART_UPDATED, CHART_DELETED                                |
| DATASET  | DATASET_SELECTED, DATASET_CHANGED                                          |
| SYSTEM   | ERROR, WARNING, INFO, DEBUG                                                |

This schema is scalable: for **Phase 1**, you can log it directly to the browser console as JSON; for **Phase 2**, send the same payload to a backend API; and for **Phase 3**, persist the JSON (or map its fields to relational columns) in your logging database without changing the frontend logging interface.





===================

Below is a production-style `LoggerService.tsx` that implements the schema you described. For **Phase 1**, it logs the JSON to the browser console. Later, you can replace the `console.log()` with an API call without changing the components that use it.

```tsx
// src/services/LoggerService.ts

export type Severity = "DEBUG" | "INFO" | "WARNING" | "ERROR";

export interface ApplicationInfo {
  name: string;
  version: string;
  environment: string;
}

export interface SessionInfo {
  sessionId: string;
  userId?: string;
  userName?: string;
  role?: string;
}

export interface PageInfo {
  url: string;
  component: string;
  module: string;
}

export interface EventInfo {
  category: string;
  action: string;
  description?: string;
  severity: Severity;
}

export interface FilterInfo {
  filterId?: string;
  field?: string;
  operator?: string;
  oldValue?: unknown;
  newValue?: unknown;
  filterCount?: number;
}

export interface QueryInfo {
  generatedQuery?: unknown;
}

export interface ExecutionInfo {
  requestId?: string;
  startTime?: string;
  endTime?: string;
  durationMs?: number;
  status?: "SUCCESS" | "FAILED" | "RUNNING";
}

export interface BrowserInfo {
  name?: string;
  version?: string;
  platform?: string;
}

export interface ErrorInfo {
  code?: string;
  message?: string;
  stackTrace?: string;
}

export interface MetadataInfo {
  ipAddress?: string;
  feature?: string;
  notes?: string;
}

export interface LogEntry {
  id: string;
  timestamp: string;

  application: ApplicationInfo;

  session: SessionInfo;

  page: PageInfo;

  event: EventInfo;

  filter?: FilterInfo;

  query?: QueryInfo;

  execution?: ExecutionInfo;

  browser?: BrowserInfo;

  error?: ErrorInfo;

  metadata?: MetadataInfo;
}

class LoggerService {
  private readonly application: ApplicationInfo = {
    name: "SDA",
    version: "1.0.0",
    environment: process.env.NODE_ENV ?? "development",
  };

  private generateId(): string {
    return crypto.randomUUID();
  }

  private getBrowser(): BrowserInfo {
    return {
      name: navigator.appName,
      version: navigator.appVersion,
      platform: navigator.platform,
    };
  }

  public log(
    event: EventInfo,
    options?: {
      session?: Partial<SessionInfo>;
      page?: Partial<PageInfo>;
      filter?: FilterInfo;
      query?: QueryInfo;
      execution?: ExecutionInfo;
      error?: ErrorInfo;
      metadata?: MetadataInfo;
    }
  ): void {
    const entry: LogEntry = {
      id: this.generateId(),
      timestamp: new Date().toISOString(),

      application: this.application,

      session: {
        sessionId: options?.session?.sessionId ?? "unknown",
        userId: options?.session?.userId,
        userName: options?.session?.userName,
        role: options?.session?.role,
      },

      page: {
        url: options?.page?.url ?? window.location.pathname,
        component: options?.page?.component ?? "",
        module: options?.page?.module ?? "",
      },

      event,

      filter: options?.filter,

      query: options?.query,

      execution: options?.execution,

      browser: this.getBrowser(),

      error: options?.error,

      metadata: options?.metadata,
    };

    console.groupCollapsed(
      `[${entry.event.severity}] ${entry.event.action}`
    );
    console.log(entry);
    console.groupEnd();

    // Phase 2
    // axios.post("/api/logs", entry);
  }

  //---------------------------------------------
  // Convenience methods
  //---------------------------------------------

  public filterCreated(
    field: string,
    operator: string,
    value: unknown
  ) {
    this.log(
      {
        category: "FILTER",
        action: "FILTER_CREATED",
        description: "User created filter",
        severity: "INFO",
      },
      {
        filter: {
          field,
          operator,
          newValue: value,
        },
      }
    );
  }

  public filterUpdated(
    field: string,
    operator: string,
    oldValue: unknown,
    newValue: unknown
  ) {
    this.log(
      {
        category: "FILTER",
        action: "FILTER_UPDATED",
        description: "User updated filter",
        severity: "INFO",
      },
      {
        filter: {
          field,
          operator,
          oldValue,
          newValue,
        },
      }
    );
  }

  public filterRemoved(field: string) {
    this.log(
      {
        category: "FILTER",
        action: "FILTER_REMOVED",
        description: "User removed filter",
        severity: "INFO",
      },
      {
        filter: {
          field,
        },
      }
    );
  }

  public filtersCleared(filterCount: number) {
    this.log(
      {
        category: "FILTER",
        action: "FILTERS_CLEARED",
        description: "User cleared all filters",
        severity: "INFO",
      },
      {
        filter: {
          filterCount,
        },
      }
    );
  }

  public queryGenerated(query: unknown) {
    this.log(
      {
        category: "QUERY",
        action: "QUERY_GENERATED",
        description: "Cube query generated",
        severity: "INFO",
      },
      {
        query: {
          generatedQuery: query,
        },
      }
    );
  }

  public queryExecutionStarted(requestId: string) {
    this.log(
      {
        category: "API",
        action: "QUERY_EXECUTION_STARTED",
        description: "Query execution started",
        severity: "INFO",
      },
      {
        execution: {
          requestId,
          startTime: new Date().toISOString(),
          status: "RUNNING",
        },
      }
    );
  }

  public queryExecutionCompleted(
    requestId: string,
    durationMs: number
  ) {
    this.log(
      {
        category: "API",
        action: "QUERY_EXECUTION_COMPLETED",
        description: "Query execution completed",
        severity: "INFO",
      },
      {
        execution: {
          requestId,
          endTime: new Date().toISOString(),
          durationMs,
          status: "SUCCESS",
        },
      }
    );
  }

  public queryExecutionFailed(
    requestId: string,
    error: Error
  ) {
    this.log(
      {
        category: "API",
        action: "QUERY_EXECUTION_FAILED",
        description: "Query execution failed",
        severity: "ERROR",
      },
      {
        execution: {
          requestId,
          endTime: new Date().toISOString(),
          status: "FAILED",
        },
        error: {
          message: error.message,
          stackTrace: error.stack,
        },
      }
    );
  }
}

const logger = new LoggerService();

export default logger;
```

## Example Usage

```tsx
import LoggerService from "../services/LoggerService";

// User adds a filter
LoggerService.filterCreated(
    "Country",
    "=",
    "USA"
);

// User edits a filter
LoggerService.filterUpdated(
    "Country",
    "=",
    "USA",
    "Canada"
);

// User removes a filter
LoggerService.filterRemoved("Country");

// User clears all filters
LoggerService.filtersCleared(5);

// Cube query generated
LoggerService.queryGenerated(cubeQuery);

// API execution
LoggerService.queryExecutionStarted("REQ-1001");

// Success
LoggerService.queryExecutionCompleted("REQ-1001", 425);

// Failure
LoggerService.queryExecutionFailed("REQ-1001", error);
```

This design follows the **Facade pattern**: React components only call methods like `filterCreated()` or `queryGenerated()`, while the service encapsulates the JSON schema. In **Phase 2**, you can replace the commented `axios.post("/api/logs", entry)` with a backend call, and in **Phase 3**, the backend can persist the same JSON structure to a database without requiring changes to the React components.




===============================================

// loggingHelpers.ts

/**
 * Common component information for UI interaction logs.
 */
export interface ComponentInfo {
  component: string;
  interaction?: string;
}

/**
 * Information extracted from a filter.
 *
 * This object is intentionally used as logging attributes (`attrs`)
 * rather than as a strict database schema.
 */
export interface FilterInfo {
  filter_id?: string;
  filter_type?: string;

  source_cube?: string;
  source_field?: string;
  source_field_type?: string;

  operator?: string;

  value?: unknown;
}


/**
 * Creates a reusable component-level logging object.
 *
 * Example:
 *
 * getComponentInfo(
 *   "FilterRow",
 *   "filter.updated"
 * )
 */
export function getComponentInfo(
  component: string,
  interaction?: string
): ComponentInfo {
  return {
    component,
    ...(interaction && {
      interaction,
    }),
  };
}


/**
 * Extracts logging information from a filter.
 *
 * The logger does not need to know the structure of the
 * application's Filter object. This helper translates it
 * into logging attributes.
 */
export function getFilterInfo(
  filter: any
): FilterInfo {
  if (!filter) {
    return {};
  }

  return {
    filter_id: filter.id,
    filter_type: filter.type,

    source_cube:
      filter.cube ??
      filter.sourceCube,

    source_field:
      filter.field ??
      filter.sourceField,

    source_field_type:
      filter.fieldType ??
      filter.sourceFieldType,

    operator: filter.operator,

    value: filter.value,
  };
}


/**
 * Creates attributes for a filter interaction.
 *
 * This combines component information with filter information.
 *
 * Example:
 *
 * const attrs = getFilterInteractionInfo(
 *   "FilterRow",
 *   "field.changed",
 *   filter
 * );
 */
export function getFilterInteractionInfo(
  component: string,
  interaction: string,
  filter: any
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      interaction
    ),

    ...getFilterInfo(filter),
  };
}


/**
 * Creates attributes for a filter value change.
 *
 * Useful when we want to capture both the previous
 * and new value.
 */
export function getFilterValueChangeInfo(
  component: string,
  filter: any,
  oldValue: unknown,
  newValue: unknown
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      "value.changed"
    ),

    ...getFilterInfo(filter),

    old_value: oldValue,
    new_value: newValue,
  };
}


/**
 * Creates attributes for a filter field change.
 */
export function getFilterFieldChangeInfo(
  component: string,
  filter: any,
  oldField: unknown,
  newField: unknown
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      "field.changed"
    ),

    ...getFilterInfo(filter),

    old_field: oldField,
    new_field: newField,
  };
}


/**
 * Creates attributes for a filter operator change.
 */
export function getFilterOperatorChangeInfo(
  component: string,
  filter: any,
  oldOperator: unknown,
  newOperator: unknown
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      "operator.changed"
    ),

    ...getFilterInfo(filter),

    old_operator: oldOperator,
    new_operator: newOperator,
  };
}


/**
 * Creates attributes for clearing filters.
 */
export function getFilterClearInfo(
  component: string,
  filterCount: number
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      "filters.cleared"
    ),

    previous_filter_count: filterCount,
  };
}


/**
 * Creates attributes for filter prefetch operations.
 */
export function getFilterPrefetchInfo(
  component: string,
  filter: any,
  extraAttributes?: Record<string, unknown>
): Record<string, unknown> {
  return {
    ...getComponentInfo(
      component,
      "prefetch"
    ),

    ...getFilterInfo(filter),

    ...extraAttributes,
  };
}



=============================



export const ev = {
  filter: {
    // -----------------------------
    // User interactions
    // -----------------------------

    created: "filter.created",
    updated: "filter.updated",
    removed: "filter.removed",
    cleared: "filter.cleared",

    field: {
      changed: "filter.field.changed",
    },

    operator: {
      changed: "filter.operator.changed",
    },

    value: {
      changed: "filter.value.changed",
    },

    // -----------------------------
    // Option fetching
    // -----------------------------

    options: {
      fetch: {
        started: "filter.options.fetch.started",
        completed: "filter.options.fetch.completed",
        failed: "filter.options.fetch.failed",
      },

      page: {
        requested: "filter.options.page.requested",
        completed: "filter.options.page.completed",
        failed: "filter.options.page.failed",
      },
    },

    // -----------------------------
    // Data resolution
    // -----------------------------

    data: {
      resolve: {
        started: "filter.data.resolve.started",
        completed: "filter.data.resolve.completed",
        failed: "filter.data.resolve.failed",
      },
    },

    // -----------------------------
    // Prefetch
    // -----------------------------

    prefetch: {
      started: "filter.prefetch.started",
      completed: "filter.prefetch.completed",
      failed: "filter.prefetch.failed",
    },
  },

  // -----------------------------
  // Query
  // -----------------------------

  query: {
    generated: "query.generated",

    execution: {
      started: "query.execution.started",
      completed: "query.execution.completed",
      failed: "query.execution.failed",
    },
  },
} as const;

