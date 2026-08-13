
```typescript
// loggingHelpers.ts

// ============================================================
// TYPES
// ============================================================

/**
 * Arbitrary attributes associated with a log event.
 *
 * This intentionally has no fixed schema.
 */
export type LogAttributes = Record<string, unknown>;


/**
 * Common information about the component performing
 * an operation or interaction.
 */
export interface ComponentInfo {
  component: string;
  operation?: string;
}


// ============================================================
// GENERIC LOG ATTRIBUTES
// ============================================================

/**
 * Creates attributes for any application activity.
 *
 * This is the primary helper that application code should use.
 *
 * Examples:
 *
 * getLogInfo("FilterOptions", "options.fetch", {
 *   page: 1
 * });
 *
 * getLogInfo("FilterSerializer", "serialization", {
 *   filter_count: 3
 * });
 *
 * getLogInfo("CubeAPI", "request", {
 *   endpoint: "/cube/load"
 * });
 */
export function getLogInfo(
  component: string,
  operation?: string,
  attrs?: LogAttributes
): LogAttributes {
  return {
    component,

    ...(operation !== undefined && {
      operation,
    }),

    ...(attrs ?? {}),
  };
}

// ============================================================
// ERROR
// ============================================================

/**
 * Converts an unknown error into logging attributes.
 */
export function getErrorInfo(
  error: unknown
): LogAttributes {
  if (!error) {
    return {};
  }

  if (error instanceof Error) {
    return {
      error_name: error.name,
      error_message: error.message,
      error_stack: error.stack,
    };
  }

  if (typeof error === "string") {
    return {
      error_message: error,
    };
  }

  return {
    error,
  };
}


// ============================================================
// PERFORMANCE
// ============================================================

/**
 * Adds duration information to a log.
 */
export function getPerformanceInfo(
  startTime: number,
  attrs?: LogAttributes
): LogAttributes {
  return {
    duration_ms:
      performance.now() - startTime,

    ...(attrs ?? {}),
  };
}
```


---







```typescript
import {
  logger,
  ev,
  getLogInfo,
  getErrorInfo,
  getPerformanceInfo,
} from "./logging";
```

## 1. Add Filter

Suppose your code has:

```typescript
const addFilter = (filter: Filter) => {
  // existing logic
  setFilters((current) => [
    ...current,
    filter,
  ]);
};
```

Add the log at the point where the user action occurs:

```typescript
const addFilter = (filter: Filter) => {
  const traceId = logger.newTraceId();

  logger.info(
    ev.filter.created,
    "User added filter",
    traceId,
    getLogInfo(
      "FilterPanel",
      "filter.created",
      {
        filter_id: filter.id,
        filter_type: filter.type,
        source_cube: filter.cube,
        source_field: filter.field,
        source_field_type: filter.fieldType,
        operator: filter.operator,
      }
    )
  );

  setFilters((current) => [
    ...current,
    filter,
  ]);
};
```

The log will look approximately like:

```json
{
  "event": "filter.created",
  "level": "info",
  "trace_id": "abc123",
  "attrs": {
    "component": "FilterPanel",
    "operation": "filter.created",
    "filter_id": "filter-123",
    "filter_type": "dropdown",
    "source_cube": "Sales",
    "source_field": "Country",
    "source_field_type": "dimension",
    "operator": "="
  }
}
```

---

# 2. Remove Filter

```typescript
const removeFilter = (filter: Filter) => {
  const traceId = logger.newTraceId();

  logger.info(
    ev.filter.removed,
    "User removed filter",
    traceId,
    getLogInfo(
      "FilterRow",
      "filter.removed",
      {
        filter_id: filter.id,
        filter_type: filter.type,
        source_cube: filter.cube,
        source_field: filter.field,
      }
    )
  );

  setFilters((current) =>
    current.filter(
      (f) => f.id !== filter.id
    )
  );
};
```

Result:

```json
{
  "event": "filter.removed",
  "level": "info",
  "trace_id": "abc123",
  "attrs": {
    "component": "FilterRow",
    "operation": "filter.removed",
    "filter_id": "filter-123",
    "filter_type": "dropdown",
    "source_cube": "Sales",
    "source_field": "Country"
  }
}
```

---

# 3. Cube API

This is where the same generic helper becomes particularly useful.

Suppose your filter options are retrieved from Cube:

```typescript
const fetchFilterOptions = async (
  filter: Filter
) => {
  // API call
};
```

I would log **started / completed / failed**.

### Started

```typescript
const fetchFilterOptions = async (
  filter: Filter
) => {
  const traceId = logger.newTraceId();
  const startTime = performance.now();

  logger.info(
    ev.filter.cubeApi.request.started,
    "Cube API request started",
    traceId,
    getLogInfo(
      "FilterOptions",
      "cube_api.request",
      {
        filter_id: filter.id,
        cube: filter.cube,
        field: filter.field,
        endpoint: "/cube/load",
        method: "POST",
      }
    )
  );

  try {
    const response = await cubeApi.load({
      cube: filter.cube,
      field: filter.field,
    });

    logger.info(
      ev.filter.cubeApi.request.completed,
      "Cube API request completed",
      traceId,
      getLogInfo(
        "FilterOptions",
        "cube_api.request",
        {
          filter_id: filter.id,
          cube: filter.cube,
          field: filter.field,
          status_code: response.status,
          result_count:
            response.data?.length ?? 0,
          ...getPerformanceInfo(
            startTime
          ),
        }
      )
    );

    return response;

  } catch (error) {

    logger.error(
      ev.filter.cubeApi.request.failed,
      "Cube API request failed",
      traceId,
      getLogInfo(
        "FilterOptions",
        "cube_api.request",
        {
          filter_id: filter.id,
          cube: filter.cube,
          field: filter.field,
          ...getErrorInfo(error),
          ...getPerformanceInfo(
            startTime
          ),
        }
      )
    );

    throw error;
  }
};
```

### The resulting sequence

For one API operation you now get:

```text
filter.cube_api.request.started
             │
             │ trace_id = ABC123
             ▼
       Cube API request
             │
             ├──── success ────► filter.cube_api.request.completed
             │
             └──── failure ────► filter.cube_api.request.failed
```

And all three have the same:

```text
session_id
trace_id
```

which is exactly what your lead was describing.

---

# One important improvement

I would **not necessarily create a new `traceId` for every individual log**.

For example, this is good:

```typescript
const traceId = logger.newTraceId();

logger.info(
  ev.filter.cubeApi.request.started,
  ...
  traceId,
  ...
);

await cubeApi.load(...);

logger.info(
  ev.filter.cubeApi.request.completed,
  ...
  traceId,
  ...
);
```

Both events represent **one operation**, so they should share the same trace ID.

For the filter workflow, you could eventually have:

```text
trace_id = ABC123

filter.created
      │
      ├── filter.options.fetch.started
      │
      ├── filter.cube_api.request.started
      │
      ├── filter.cube_api.request.completed
      │
      ├── filter.options.fetch.completed
      │
      └── filter.data.resolve.completed
```

That gives you a very useful end-to-end picture of **what happened because the user added a filter**.

And importantly, your generic helper stays extremely small:

```typescript
getLogInfo(
  "FilterOptions",
  "cube_api.request",
  {
    cube: filter.cube,
    field: filter.field
  }
)
```


```
{
  "ts": "2026-08-13T13:45:21.123Z",
  "level": "info",
  "event": "filter.removed",

  "service": "sda-query-builder",
  "application": "SDA",
  "host": "hostname-or-address",

  "session_id": "is3001.GLDBAE054631.20260806T125114.v0.1.1.x6J8",
  "trace_id": "xxxx",

  "schema_version": 1,

  "message": "User removed filter",

  "attrs": {
    "component": "FilterRow",
    "operation": "filter.removed",
    "filter_id": "filter-123",
    "filter_type": "dropdown",
    "source_cube": "Sales",
    "source_field": "Country"
  }
}
```

