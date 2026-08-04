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

