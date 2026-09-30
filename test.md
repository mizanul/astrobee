
Yes. Since `ev` is a nested object with many events, you can write a **recursive Jest test** that walks through the entire object and verifies every leaf event.

For example, if:

```js
export const ev = {
  api: {
    requestStarted: "api.request.started",
    requestCompleted: "api.request.completed",
    requestFailed: "api.request.failed",
  },
  query: {
    started: "query.started",
    completed: "query.completed",
  },
};
```

you can test everything with:

```js
import { ev } from "./event";

describe("event definitions", () => {
  function validateEvents(obj, path = "ev") {
    Object.entries(obj).forEach(([key, value]) => {
      const currentPath = `${path}.${key}`;

      if (typeof value === "object" && value !== null) {
        validateEvents(value, currentPath);
      } else {
        test(`${currentPath} is a valid event`, () => {
          expect(typeof value).toBe("string");
          expect(value.length).toBeGreaterThan(0);
        });
      }
    });
  }

  validateEvents(ev);
});
```

This automatically tests **every event**, no matter how many you add.

### Better version

I would make the validation a little stronger:

```js
import { ev } from "./event";

describe("event definitions", () => {
  function validateEvents(obj, path = "ev") {
    Object.entries(obj).forEach(([key, value]) => {
      const currentPath = `${path}.${key}`;

      if (value && typeof value === "object") {
        validateEvents(value, currentPath);
        return;
      }

      test(`${currentPath} is a valid event`, () => {
        expect(typeof value).toBe("string");
        expect(value.trim().length).toBeGreaterThan(0);
      });
    });
  }

  validateEvents(ev);
});
```

So if you later have:

```js
export const ev = {
  api: {
    query: {
      started: "api.query.started",
      completed: "api.query.completed",
      failed: "api.query.failed",
    },
  },

  ui: {
    filter: {
      created: "ui.filter.created",
      deleted: "ui.filter.deleted",
    },
  },

  authentication: {
    login: "authentication.login",
    logout: "authentication.logout",
  },
};
```

you don't need to change the test at all.

It will automatically discover and test all **leaf values** in `ev`.

### One additional test I recommend

Since event names should be unique, you can also test for duplicates:

```js
import { ev } from "./event";

describe("event definitions", () => {
  function collectEvents(obj, events = []) {
    Object.values(obj).forEach((value) => {
      if (value && typeof value === "object") {
        collectEvents(value, events);
      } else {
        events.push(value);
      }
    });

    return events;
  }

  test("all event names are unique", () => {
    const events = collectEvents(ev);

    expect(new Set(events).size).toBe(events.length);
  });

  function validateEvents(obj, path = "ev") {
    Object.entries(obj).forEach(([key, value]) => {
      const currentPath = `${path}.${key}`;

      if (value && typeof value === "object") {
        validateEvents(value, currentPath);
      } else {
        test(`${currentPath} is a valid event`, () => {
          expect(typeof value).toBe("string");
          expect(value.trim()).not.toBe("");
        });
      }
    });
  }

  validateEvents(ev);
});
```

This gives you a useful **event contract test** without having to manually maintain a test for every event.

Strengthen the event contract

* Reject arrays and null values
* Check event naming conventions


---

For example, if `event.js` is:

```js
export const ev = {
  api: {
    requestStarted: "api.request.started",
    requestCompleted: "api.request.completed",
    requestFailed: "api.request.failed",
  },
  query: {
    started: "query.started",
    completed: "query.completed",
    failed: "query.failed",
  },
};
```

Create:

```text
event.test.js
```

```js
import { ev } from "./event";

describe("event definitions", () => {
  test("exports ev", () => {
    expect(ev).toBeDefined();
    expect(typeof ev).toBe("object");
  });

  test("contains api events", () => {
    expect(ev.api).toBeDefined();
    expect(typeof ev.api).toBe("object");
  });

  test("contains expected API events", () => {
    expect(ev.api.requestStarted).toBe("api.request.started");
    expect(ev.api.requestCompleted).toBe("api.request.completed");
    expect(ev.api.requestFailed).toBe("api.request.failed");
  });

  test("event names are strings", () => {
    Object.values(ev.api).forEach((event) => {
      expect(typeof event).toBe("string");
      expect(event.length).toBeGreaterThan(0);
    });
  });
});
```

### If your actual file is currently only

```js
export const ev = {
  api: {}
};
```

then the test can simply be:

```js
import { ev } from "./event";

describe("event definitions", () => {
  test("exports ev", () => {
    expect(ev).toBeDefined();
    expect(typeof ev).toBe("object");
  });

  test("contains api", () => {
    expect(ev.api).toBeDefined();
    expect(typeof ev.api).toBe("object");
  });
});
```

As you add events to `event.js`, I would recommend testing the **event names explicitly**, because these names become part of your logging contract.



---

Below are complete Jest test files for both `LoggerService` and `loggingHelpers`.

I recommend these two files:

```text
src/services/LoggerService.test.ts
src/utils/loggingHelpers.test.ts
```

## 1. `LoggerService.test.ts`

```ts
import { LoggerService } from "./LoggerService";

describe("LoggerService", () => {
  let logger: LoggerService;

  beforeEach(() => {
    logger = new LoggerService({
      service: "test-service",
      application: "TEST",
      environment: "test",
      host: "test.local",
      schemaVersion: 1,
      consoleEnabled: true,
      minLevel: "debug",
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ============================================================
  // CONSTRUCTOR
  // ============================================================

  describe("constructor", () => {
    test("creates logger with supplied configuration", () => {
      expect(logger.getSessionId()).toBeDefined();
      expect(logger.getSessionId()).toContain("test-service");
    });

    test("uses default schema version when not provided", () => {
      const testLogger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
      });

      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      testLogger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.schema_version).toBe(1);
    });
  });

  // ============================================================
  // SESSION
  // ============================================================

  describe("session", () => {
    test("creates a session ID", () => {
      const sessionId = logger.getSessionId();

      expect(sessionId).toBeDefined();
      expect(typeof sessionId).toBe("string");
      expect(sessionId.length).toBeGreaterThan(0);
      expect(sessionId).toContain("test-service");
    });

    test("returns the same session ID until session is restarted", () => {
      const sessionId1 = logger.getSessionId();
      const sessionId2 = logger.getSessionId();

      expect(sessionId1).toBe(sessionId2);
    });

    test("startSession creates a new session ID", () => {
      const oldSessionId = logger.getSessionId();

      const newSessionId = logger.startSession();

      expect(newSessionId).toBeDefined();
      expect(newSessionId).not.toBe(oldSessionId);
      expect(logger.getSessionId()).toBe(newSessionId);
    });
  });

  // ============================================================
  // TRACE ID
  // ============================================================

  describe("trace ID", () => {
    test("creates a trace ID", () => {
      const traceId = logger.newTraceId();

      expect(traceId).toBeDefined();
      expect(typeof traceId).toBe("string");
      expect(traceId.length).toBeGreaterThan(0);
    });

    test("trace ID contains the session ID", () => {
      const traceId = logger.newTraceId();

      expect(traceId).toContain(logger.getSessionId());
    });

    test("creates unique trace IDs", () => {
      const traceId1 = logger.newTraceId();
      const traceId2 = logger.newTraceId();

      expect(traceId1).not.toBe(traceId2);
    });
  });

  // ============================================================
  // DEBUG
  // ============================================================

  describe("debug", () => {
    test("logs debug messages", () => {
      const consoleSpy = jest
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      logger.debug(
        "test.debug",
        "Debug message"
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("debug");
      expect(entry.event).toBe("test.debug");
      expect(entry.message).toBe("Debug message");
    });
  });

  // ============================================================
  // INFO
  // ============================================================

  describe("info", () => {
    test("logs info messages", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "query.completed",
        "Query completed"
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("info");
      expect(entry.event).toBe("query.completed");
      expect(entry.message).toBe("Query completed");
    });

    test("logs attributes", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "query.completed",
        "Query completed",
        undefined,
        {
          row_count: 100,
          duration_ms: 250,
        }
      );

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.attrs).toEqual({
        row_count: 100,
        duration_ms: 250,
      });
    });
  });

  // ============================================================
  // WARNING
  // ============================================================

  describe("warning", () => {
    test("logs warning messages", () => {
      const consoleSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      logger.warning(
        "query.slow",
        "Query is slow"
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("warning");
      expect(entry.event).toBe("query.slow");
      expect(entry.message).toBe("Query is slow");
    });
  });

  // ============================================================
  // ERROR
  // ============================================================

  describe("error", () => {
    test("logs error messages", () => {
      const consoleSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.error(
        "query.failed",
        "Query failed"
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("error");
      expect(entry.event).toBe("query.failed");
      expect(entry.message).toBe("Query failed");
    });
  });

  // ============================================================
  // TRACE ID IN LOG
  // ============================================================

  describe("trace ID in log entry", () => {
    test("includes trace_id when provided", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const traceId = logger.newTraceId();

      logger.info(
        "query.started",
        "Query started",
        traceId
      );

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.trace_id).toBe(traceId);
    });

    test("does not include trace_id when not provided", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("query.started");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry).not.toHaveProperty("trace_id");
    });
  });

  // ============================================================
  // MESSAGE
  // ============================================================

  describe("message", () => {
    test("includes message when provided", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "test.event",
        "Test message"
      );

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.message).toBe("Test message");
    });

    test("does not include message when undefined", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry).not.toHaveProperty("message");
    });
  });

  // ============================================================
  // STRUCTURED LOG FIELDS
  // ============================================================

  describe("structured log entry", () => {
    test("contains all required fields", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const traceId = logger.newTraceId();

      logger.info(
        "query.completed",
        "Query completed",
        traceId,
        {
          row_count: 50,
        }
      );

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry).toEqual(
        expect.objectContaining({
          level: "info",
          event: "query.completed",
          service: "test-service",
          application: "TEST",
          environment: "test",
          host: "test.local",
          session_id: logger.getSessionId(),
          trace_id: traceId,
          schema_version: 1,
          message: "Query completed",
          attrs: {
            row_count: 50,
          },
        })
      );

      expect(entry.ts).toBeDefined();
    });

    test("timestamp is a valid ISO timestamp", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.ts).toBeDefined();

      const date = new Date(entry.ts);

      expect(date.toString()).not.toBe("Invalid Date");
    });
  });

  // ============================================================
  // CONSOLE ENABLED
  // ============================================================

  describe("consoleEnabled", () => {
    test("writes to console when enabled", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      expect(consoleSpy).toHaveBeenCalledTimes(1);
    });

    test("does not write to console when disabled", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      testLogger.info("test.event");

      expect(consoleSpy).not.toHaveBeenCalled();
    });
  });

  // ============================================================
  // MINIMUM LOG LEVEL
  // ============================================================

  describe("minimum log level", () => {
    test("debug level allows all log levels", () => {
      const debugSpy = jest
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "debug",
      });

      testLogger.debug("debug.event");

      expect(debugSpy).toHaveBeenCalledTimes(1);
    });

    test("info level filters debug messages", () => {
      const debugSpy = jest
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "info",
      });

      testLogger.debug("debug.event");

      expect(debugSpy).not.toHaveBeenCalled();
    });

    test("info level allows info messages", () => {
      const infoSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "info",
      });

      testLogger.info("info.event");

      expect(infoSpy).toHaveBeenCalledTimes(1);
    });

    test("warning level filters info messages", () => {
      const infoSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "warning",
      });

      testLogger.info("info.event");

      expect(infoSpy).not.toHaveBeenCalled();
    });

    test("warning level allows warning messages", () => {
      const warningSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "warning",
      });

      testLogger.warning("warning.event");

      expect(warningSpy).toHaveBeenCalledTimes(1);
    });

    test("error level filters warning messages", () => {
      const warningSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "error",
      });

      testLogger.warning("warning.event");

      expect(warningSpy).not.toHaveBeenCalled();
    });

    test("error level allows error messages", () => {
      const errorSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test",
        application: "TEST",
        environment: "test",
        minLevel: "error",
      });

      testLogger.error("error.event");

      expect(errorSpy).toHaveBeenCalledTimes(1);
    });
  });

  // ============================================================
  // HOST
  // ============================================================

  describe("host", () => {
    test("uses configured host", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        host: "qa.example.com",
      });

      testLogger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.host).toBe("qa.example.com");
    });

    test("uses browser hostname when host is not provided", () => {
      Object.defineProperty(window, "location", {
        configurable: true,
        value: {
          hostname: "localhost",
        },
      });

      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const testLogger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
      });

      testLogger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.host).toBe("localhost");
    });
  });

  // ============================================================
  // ATTRIBUTES
  // ============================================================

  describe("attributes", () => {
    test("supports multiple attribute types", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "test.event",
        undefined,
        undefined,
        {
          string_value: "hello",
          number_value: 123,
          boolean_value: true,
          null_value: null,
          object_value: {
            id: 1,
          },
          array_value: [1, 2, 3],
        }
      );

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.attrs.string_value).toBe("hello");
      expect(entry.attrs.number_value).toBe(123);
      expect(entry.attrs.boolean_value).toBe(true);
      expect(entry.attrs.null_value).toBeNull();
      expect(entry.attrs.object_value).toEqual({
        id: 1,
      });
      expect(entry.attrs.array_value).toEqual([1, 2, 3]);
    });

    test("uses empty object when attributes are not provided", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.attrs).toEqual({});
    });
  });
});
```

## 2. `loggingHelpers.test.ts`

```ts
import {
  getLogInfo,
  getErrorInfo,
  getPerformanceInfo,
} from "./loggingHelpers";

describe("loggingHelpers", () => {
  // ============================================================
  // getLogInfo
  // ============================================================

  describe("getLogInfo", () => {
    test("returns component", () => {
      const result = getLogInfo("FilterPanel");

      expect(result).toEqual({
        component: "FilterPanel",
      });
    });

    test("returns component and operation", () => {
      const result = getLogInfo(
        "FilterPanel",
        "loadFilters"
      );

      expect(result).toEqual({
        component: "FilterPanel",
        operation: "loadFilters",
      });
    });

    test("includes additional attributes", () => {
      const result = getLogInfo(
        "FilterPanel",
        "loadFilters",
        {
          filter_count: 10,
          source: "api",
        }
      );

      expect(result).toEqual({
        component: "FilterPanel",
        operation: "loadFilters",
        filter_count: 10,
        source: "api",
      });
    });

    test("does not include operation when undefined", () => {
      const result = getLogInfo(
        "FilterPanel",
        undefined,
        {
          filter_count: 10,
        }
      );

      expect(result).toEqual({
        component: "FilterPanel",
        filter_count: 10,
      });
    });

    test("works with empty attributes", () => {
      const result = getLogInfo(
        "FilterPanel",
        "loadFilters",
        {}
      );

      expect(result).toEqual({
        component: "FilterPanel",
        operation: "loadFilters",
      });
    });
  });

  // ============================================================
  // getErrorInfo
  // ============================================================

  describe("getErrorInfo", () => {
    test("returns empty object for null", () => {
      expect(getErrorInfo(null)).toEqual({});
    });

    test("returns empty object for undefined", () => {
      expect(getErrorInfo(undefined)).toEqual({});
    });

    test("extracts information from Error", () => {
      const error = new Error("Something went wrong");

      const result = getErrorInfo(error);

      expect(result.error_name).toBe("Error");
      expect(result.error_message).toBe(
        "Something went wrong"
      );
      expect(result.error_stack).toBeDefined();
    });

    test("extracts information from TypeError", () => {
      const error = new TypeError("Invalid value");

      const result = getErrorInfo(error);

      expect(result.error_name).toBe("TypeError");
      expect(result.error_message).toBe("Invalid value");
      expect(result.error_stack).toBeDefined();
    });

    test("handles string errors", () => {
      const result = getErrorInfo("Something failed");

      expect(result).toEqual({
        error_message: "Something failed",
      });
    });

    test("handles object errors", () => {
      const error = {
        code: "ERR_001",
        reason: "Invalid request",
      };

      const result = getErrorInfo(error);

      expect(result).toEqual({
        error,
      });
    });

    test("handles numeric errors", () => {
      const result = getErrorInfo(123);

      expect(result).toEqual({
        error: 123,
      });
    });

    test("handles boolean errors", () => {
      const result = getErrorInfo(true);

      expect(result).toEqual({
        error: true,
      });
    });
  });

  // ============================================================
  // getPerformanceInfo
  // ============================================================

  describe("getPerformanceInfo", () => {
    test("returns duration", () => {
      const startTime = performance.now();

      const result = getPerformanceInfo(startTime);

      expect(result.duration_ms).toBeGreaterThanOrEqual(0);
    });

    test("returns duration as a number", () => {
      const startTime = performance.now();

      const result = getPerformanceInfo(startTime);

      expect(typeof result.duration_ms).toBe("number");
    });

    test("includes additional attributes", () => {
      const startTime = performance.now();

      const result = getPerformanceInfo(
        startTime,
        {
          operation: "loadFilters",
          item_count: 20,
        }
      );

      expect(result.operation).toBe("loadFilters");
      expect(result.item_count).toBe(20);
      expect(typeof result.duration_ms).toBe("number");
    });

    test("works with empty attributes", () => {
      const startTime = performance.now();

      const result = getPerformanceInfo(
        startTime,
        {}
      );

      expect(result.duration_ms).toBeGreaterThanOrEqual(0);
    });

    test("calculates elapsed time", () => {
      const startTime = performance.now() - 100;

      const result = getPerformanceInfo(startTime);

      expect(result.duration_ms).toBeGreaterThanOrEqual(100);
    });
  });
});
```

### One important change to `loggingHelpers.ts`

Your current code has:

```ts
if (!error) {
  return {};
}
```

That means `false`, `0`, and `""` are discarded. I recommend changing it to:

```ts
if (error === null || error === undefined) {
  return {};
}
```

So this test:

```ts
test("handles numeric errors", () => {
  const result = getErrorInfo(123);

  expect(result).toEqual({
    error: 123,
  });
});
```

and the boolean test correctly verify that non-null values are preserved.

Also make sure `LoggerService` is exported:

```ts
export class LoggerService {
```

while keeping:

```ts
export default logger;
```

These tests are designed around the **actual public behavior** of your logger rather than its private methods, which is the right approach for this service.

For the Jest tests

* Add Jest mocks for logging helpers




---



### 1. Install Jest

For a TypeScript/Vite project, a common setup is:

```bash
npm install --save-dev jest ts-jest @types/jest jest-environment-jsdom
```

If you use Babel or another TypeScript transform, the configuration can be different.

### 2. Export `LoggerService`

In `LoggerService.ts`, change:

```ts
class LoggerService {
```

to:

```ts
export class LoggerService {
```

Keep the default singleton at the bottom:

```ts
export default logger;
```

### 3. Jest Configuration

Create `jest.config.js`:

```js
module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  clearMocks: true,
  restoreMocks: true,
};
```

### 4. Package Scripts

In `package.json`:

```json
{
  "scripts": {
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  }
}
```

### 5. LoggerService Test

For example:

```ts
import { LoggerService } from "./LoggerService";

describe("LoggerService", () => {
  let logger: LoggerService;

  beforeEach(() => {
    logger = new LoggerService({
      service: "test-service",
      application: "TEST",
      environment: "test",
      host: "test.local",
      consoleEnabled: true,
      minLevel: "debug",
    });
  });

  describe("session", () => {
    test("creates a session ID", () => {
      const sessionId = logger.getSessionId();

      expect(sessionId).toBeDefined();
      expect(sessionId).toContain("test-service");
    });

    test("creates a new session when startSession is called", () => {
      const oldSessionId = logger.getSessionId();

      const newSessionId = logger.startSession();

      expect(newSessionId).toBeDefined();
      expect(newSessionId).not.toBe(oldSessionId);
      expect(logger.getSessionId()).toBe(newSessionId);
    });
  });

  describe("trace ID", () => {
    test("creates a trace ID", () => {
      const traceId = logger.newTraceId();

      expect(traceId).toBeDefined();
      expect(traceId).toContain(logger.getSessionId());
    });

    test("creates unique trace IDs", () => {
      const trace1 = logger.newTraceId();
      const trace2 = logger.newTraceId();

      expect(trace1).not.toBe(trace2);
    });
  });

  describe("logging", () => {
    test("logs an info message", () => {
      const consoleSpy = jest
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "query.completed",
        "Query completed",
        "trace-123",
        {
          row_count: 10,
        }
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("info");
      expect(entry.event).toBe("query.completed");
      expect(entry.message).toBe("Query completed");
      expect(entry.trace_id).toBe("trace-123");
      expect(entry.service).toBe("test-service");
      expect(entry.application).toBe("TEST");
      expect(entry.environment).toBe("test");
      expect(entry.host).toBe("test.local");
      expect(entry.schema_version).toBe(1);
      expect(entry.attrs.row_count).toBe(10);
    });

    test("logs errors using console.error", () => {
      const consoleSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.error(
        "query.failed",
        "Query failed"
      );

      expect(consoleSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(consoleSpy.mock.calls[0][0]);

      expect(entry.level).toBe("error");
      expect(entry.event).toBe("query.failed");
      expect(entry.message).toBe("Query failed");
    });
  });
});
```

### 6. Testing Minimum Log Level

This is particularly important for your logger:

```ts
test("does not log messages below the minimum level", () => {
  const consoleSpy = jest
    .spyOn(console, "debug")
    .mockImplementation(() => {});

  const logger = new LoggerService({
    service: "test-service",
    application: "TEST",
    environment: "test",
    consoleEnabled: true,
    minLevel: "info",
  });

  logger.debug("debug.event");

  expect(consoleSpy).not.toHaveBeenCalled();
});
```

And:

```ts
test("logs messages at or above the minimum level", () => {
  const consoleSpy = jest
    .spyOn(console, "info")
    .mockImplementation(() => {});

  const logger = new LoggerService({
    service: "test-service",
    application: "TEST",
    environment: "test",
    consoleEnabled: true,
    minLevel: "info",
  });

  logger.info("info.event");

  expect(consoleSpy).toHaveBeenCalledTimes(1);
});
```

### 7. Testing `consoleEnabled`

```ts
test("does not write to console when console logging is disabled", () => {
  const consoleSpy = jest
    .spyOn(console, "info")
    .mockImplementation(() => {});

  const logger = new LoggerService({
    service: "test-service",
    application: "TEST",
    environment: "test",
    consoleEnabled: false,
  });

  logger.info("test.event");

  expect(consoleSpy).not.toHaveBeenCalled();
});
```

### 8. Jest vs Vitest

The main changes from the earlier tests are:

| Vitest                 | Jest                     |
| ---------------------- | ------------------------ |
| `vi.spyOn()`           | `jest.spyOn()`           |
| `vi.fn()`              | `jest.fn()`              |
| `vi.mock()`            | `jest.mock()`            |
| `vi.restoreAllMocks()` | `jest.restoreAllMocks()` |
| `vi.clearAllMocks()`   | `jest.clearAllMocks()`   |
| `vitest`               | `jest`                   |
| `vitest run`           | `jest`                   |

So your LoggerService design does **not** need to change just because you are using Jest. The tests need to use Jest's APIs and Jest needs to be configured for your TypeScript/browser environment.

For the Jest setup

* Add Jest mocks for the logger helpers








---





Since your `.gitlab-ci.yml` is including a shared pipeline template and your unit tests are under `web/`, the important question is **where the shared template defines the test job**.

Your current file:

```yaml
include:
  - project: "some url pipeline-template"
    file:
      - "templates/container.yml"

variables:

.sub_project_template:
  parallel:
    matrix:
      - SUB_PROJECT_PATH: "web"
```

The `.sub_project_template` is a **hidden job/template configuration**. It doesn't actually run your unit tests by itself.

### If `web` is a Node/React project

If the shared `container.yml` already has a test job that uses `SUB_PROJECT_PATH`, you generally should **not create another job**. You need to configure the test command expected by that template.

For example, if the template expects something like:

```yaml
npm test
```

then your `web/package.json` should contain:

```json
{
  "scripts": {
    "test": "vitest"
  }
}
```

and your tests can be under:

```text
web/
├── package.json
├── src/
│   └── ...
└── tests/
    └── ...
```

or:

```text
web/
├── package.json
├── src/
│   └── LoggerService.test.ts
```

depending on your Vitest configuration.

### If you need to explicitly add a unit-test job

You could add something like:

```yaml
unit_test:
  parallel:
    matrix:
      - SUB_PROJECT_PATH: "web"
  script:
    - cd "$SUB_PROJECT_PATH"
    - npm ci
    - npm test
```

But **don't add this yet** if `templates/container.yml` already defines a test job. You may end up running the tests twice or conflicting with the shared template.

### What I need to see

Because your pipeline is coming from:

```yaml
include:
  - project: "some url pipeline-template"
    file:
      - "templates/container.yml"
```

the answer depends on what is inside **`templates/container.yml`**.

If you paste the contents of `templates/container.yml` (especially the jobs containing `test`, `unit`, `npm`, `script`, or `SUB_PROJECT_PATH`), I can tell you **exactly where to put your unit-test configuration** without changing the shared pipeline unnecessarily.

Find the right place for the unit test

* Show the shared template job








python3 -c "p='.gitlab-ci.yml'; d=open(p,'rb').read(); open(p,'ab').write(b'\n' if d and not d.endswith(b'\n') else b'')"



That's fine. You don't need Ruby or Python either.

If you're on macOS, use the built-in `sed`/`tail` commands.

### Fix `No newline at end of file`

The simplest method is:

```bash
echo >> .gitlab-ci.yml
```

Then check:

```bash
git diff --check
```

If `git diff --check` produces **no output**, the whitespace/newline problem is clean.

### If you want to see whether the file ends with a newline

Run:

```bash
tail -c 1 .gitlab-ci.yml | od -An -t x1
```

If it ends correctly, you should see:

```text
0a
```

`0a` is the newline character.




---



That warning is **not an error**. It means your `.gitlab-ci.yml` currently uses **Windows-style line endings (CRLF)**, and Git is configured to convert them to **Unix-style line endings (LF)**.

For a GitLab CI file, **LF is the preferred format**.

### Fix it permanently for this file

Run:

```bash
sed -i '' 's/\r$//' .gitlab-ci.yml
```

Then check:

```bash
git add .gitlab-ci.yml
```

You should no longer get the CRLF warning.

### Verify the line endings

Run:

```bash
file .gitlab-ci.yml
```

You want something like:

```text
.gitlab-ci.yml: ASCII text
```

or:

```text
.gitlab-ci.yml: UTF-8 Unicode text
```

and **not** something mentioning `CRLF`.

### Also configure Git on your Mac

I recommend:

```bash
git config --global core.autocrlf input
```

This tells Git:

* Keep files as **LF** in your repository
* If you receive CRLF files, convert them to LF when committing
* Don't unnecessarily convert LF files back to CRLF

Then:

```bash
git add .gitlab-ci.yml
git status
```

### One important point

Earlier you saw:

```text
0a
```

That is **correct**. `0a` means LF/newline.

The problem you're seeing now is that **some or all of the other lines contain `0d 0a`**, where:

* `0d` = CR (carriage return)
* `0a` = LF (newline)

So the goal is to convert:

```text
CRLF = 0d 0a
```

to:

```text
LF = 0a
```

The `sed` command above does exactly that.


### Important

If your GitLab pipeline is still failing after fixing this, **`No newline at end of file` is probably not the actual CI error**. It is usually just a Git diff warning.

If you paste the **exact GitLab error message** and your `.gitlab-ci.yml`, I can tell you exactly what needs to be fixed.

To check the actual CI problem

* Open GitLab CI Lint
















include:
  - project: 'some/project'
    file: '/some-template.yml'

stages:
  - test

vitest:
  stage: test
  image: node:20

  before_script:
    - npm ci

  script:
    - npm test





I would test these as **two separate unit-test files**:

* `LoggerService.test.ts` — test session IDs, trace IDs, log levels, filtering, log structure, console methods, browser host detection.
* `loggingHelpers.test.ts` — test the three pure helper functions independently.

One important point first: your `LoggerService.ts` currently exports only the **singleton**:

```ts
export default logger;
```

That makes it harder to test different configurations such as `minLevel: "error"` or `consoleEnabled: false`.

I recommend exporting the class as well.

### 1. Small change to `LoggerService.ts`

Change:

```ts
class LoggerService {
```

to:

```ts
export class LoggerService {
```

Keep this at the bottom:

```ts
const logger = new LoggerService({
  service: "sda-query-builder",
  application: "SDA",
  environment:
    import.meta.env?.MODE ??
    "development",
  schemaVersion: 1,
  consoleEnabled: true,
  minLevel: "debug",
});

export default logger;
```

This lets your application continue using:

```ts
import logger from "./LoggerService";
```

while tests can do:

```ts
import { LoggerService } from "./LoggerService";
```

---

# 2. Vitest setup

If you already have Vitest installed, you can skip the installation.

Otherwise:

```bash
npm install -D vitest jsdom
```

If this is a Vite React project, you probably already have most of this.

Your `package.json` can contain:

```json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  }
}
```

I recommend `jsdom` because `LoggerService` contains:

```ts
window.location.hostname
```

---

# 3. Vitest configuration

If you have `vite.config.ts`, add the test configuration there:

```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],

  test: {
    environment: "jsdom",
    globals: true,
    clearMocks: true,
  },
});
```

If you don't want to put testing configuration into your Vite config, create `vitest.config.ts` instead.

---

# 4. Test `LoggerService`

I would create:

```text
src/
├── services/
│   ├── LoggerService.ts
│   └── LoggerService.test.ts
│
└── utils/
    ├── loggingHelpers.ts
    └── loggingHelpers.test.ts
```

Adjust the paths to your actual project structure.

## `LoggerService.test.ts`

Here is a fairly complete test suite for your implementation:

```ts
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

import { LoggerService } from "./LoggerService";

describe("LoggerService", () => {
  beforeEach(() => {
    vi.restoreAllMocks();

    vi.spyOn(Date.prototype, "toISOString")
      .mockReturnValue("2026-09-23T12:00:00.000Z");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });


  // ==========================================================
  // CONSTRUCTOR
  // ==========================================================

  describe("constructor", () => {
    it("creates a logger with the supplied configuration", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        host: "test-host",
        schemaVersion: 2,
        consoleEnabled: false,
        minLevel: "info",
      });

      expect(logger.getSessionId()).toContain("test-service.");
    });


    it("uses the supplied host", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        host: "my-test-host",
        consoleEnabled: false,
      });

      const consoleSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      expect(consoleSpy).not.toHaveBeenCalled();
    });
  });


  // ==========================================================
  // SESSION
  // ==========================================================

  describe("session", () => {
    it("creates a session ID", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const sessionId = logger.getSessionId();

      expect(sessionId).toBeTruthy();
      expect(sessionId).toContain("test-service.");
    });


    it("returns the same session ID until a new session starts", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const session1 = logger.getSessionId();
      const session2 = logger.getSessionId();

      expect(session2).toBe(session1);
    });


    it("creates a new session when startSession is called", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const originalSession = logger.getSessionId();

      const newSession = logger.startSession();

      expect(newSession).toBe(logger.getSessionId());
      expect(newSession).not.toBe(originalSession);
    });
  });


  // ==========================================================
  // TRACE
  // ==========================================================

  describe("trace IDs", () => {
    it("generates a trace ID", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const traceId = logger.newTraceId();

      expect(traceId).toBeTruthy();
      expect(traceId).toContain(logger.getSessionId());
    });


    it("generates different trace IDs", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const trace1 = logger.newTraceId();
      const trace2 = logger.newTraceId();

      expect(trace1).not.toBe(trace2);
    });
  });


  // ==========================================================
  // DEBUG
  // ==========================================================

  describe("debug", () => {
    it("writes a debug log", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        host: "test-host",
        schemaVersion: 3,
        consoleEnabled: true,
        minLevel: "debug",
      });

      const debugSpy = vi
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      logger.debug(
        "filter.created",
        "Filter created"
      );

      expect(debugSpy).toHaveBeenCalledTimes(1);

      const output = debugSpy.mock.calls[0][0];

      const entry = JSON.parse(output);

      expect(entry.level).toBe("debug");
      expect(entry.event).toBe("filter.created");
      expect(entry.message).toBe("Filter created");

      expect(entry.service).toBe("test-service");
      expect(entry.application).toBe("TEST");
      expect(entry.environment).toBe("test");
      expect(entry.host).toBe("test-host");

      expect(entry.schema_version).toBe(3);
      expect(entry.session_id).toBe(logger.getSessionId());

      expect(entry.ts).toBe("2026-09-23T12:00:00.000Z");
      expect(entry.attrs).toEqual({});
    });
  });


  // ==========================================================
  // INFO
  // ==========================================================

  describe("info", () => {
    it("writes an info log", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "debug",
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "filter.created",
        "Filter created",
        "trace-123",
        {
          filter_count: 3,
        }
      );

      expect(infoSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry.level).toBe("info");
      expect(entry.event).toBe("filter.created");
      expect(entry.message).toBe("Filter created");
      expect(entry.trace_id).toBe("trace-123");

      expect(entry.attrs).toEqual({
        filter_count: 3,
      });
    });
  });


  // ==========================================================
  // WARNING
  // ==========================================================

  describe("warning", () => {
    it("writes a warning using console.warn", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "debug",
      });

      const warnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      logger.warning(
        "filter.options.failed",
        "Unable to load options"
      );

      expect(warnSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(
        warnSpy.mock.calls[0][0]
      );

      expect(entry.level).toBe("warning");
      expect(entry.event).toBe(
        "filter.options.failed"
      );
    });
  });


  // ==========================================================
  // ERROR
  // ==========================================================

  describe("error", () => {
    it("writes an error using console.error", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "debug",
      });

      const errorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.error(
        "cube_api.request.failed",
        "Cube API request failed",
        "trace-123",
        {
          status: 500,
          endpoint: "/cube/load",
        }
      );

      expect(errorSpy).toHaveBeenCalledTimes(1);

      const entry = JSON.parse(
        errorSpy.mock.calls[0][0]
      );

      expect(entry.level).toBe("error");
      expect(entry.event).toBe(
        "cube_api.request.failed"
      );

      expect(entry.message).toBe(
        "Cube API request failed"
      );

      expect(entry.trace_id).toBe("trace-123");

      expect(entry.attrs).toEqual({
        status: 500,
        endpoint: "/cube/load",
      });
    });
  });


  // ==========================================================
  // ATTRIBUTES
  // ==========================================================

  describe("attributes", () => {
    it("includes supplied attributes", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "test.event",
        undefined,
        undefined,
        {
          page: 1,
          count: 25,
          active: true,
          nested: {
            value: "hello",
          },
        }
      );

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry.attrs).toEqual({
        page: 1,
        count: 25,
        active: true,
        nested: {
          value: "hello",
        },
      });
    });
  });


  // ==========================================================
  // TRACE IN LOG ENTRY
  // ==========================================================

  describe("trace ID in log entry", () => {
    it("includes trace_id when supplied", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "operation.started",
        "Operation started",
        "trace-abc"
      );

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry.trace_id).toBe("trace-abc");
    });


    it("does not include trace_id when omitted", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info(
        "operation.started"
      );

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry).not.toHaveProperty(
        "trace_id"
      );
    });
  });


  // ==========================================================
  // MESSAGE
  // ==========================================================

  describe("message", () => {
    it("does not include message when omitted", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry).not.toHaveProperty(
        "message"
      );
    });
  });


  // ==========================================================
  // CONSOLE ENABLED
  // ==========================================================

  describe("console output", () => {
    it("does not write to console when disabled", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: false,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      expect(infoSpy).not.toHaveBeenCalled();
    });


    it("writes to console when enabled", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      expect(infoSpy).toHaveBeenCalledTimes(1);
    });
  });


  // ==========================================================
  // MINIMUM LOG LEVEL
  // ==========================================================

  describe("minimum log level", () => {
    it("logs everything when minLevel is debug", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "debug",
      });

      const debugSpy = vi
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const warnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const errorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.debug("debug.event");
      logger.info("info.event");
      logger.warning("warning.event");
      logger.error("error.event");

      expect(debugSpy).toHaveBeenCalledTimes(1);
      expect(infoSpy).toHaveBeenCalledTimes(1);
      expect(warnSpy).toHaveBeenCalledTimes(1);
      expect(errorSpy).toHaveBeenCalledTimes(1);
    });


    it("suppresses debug when minLevel is info", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "info",
      });

      const debugSpy = vi
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.debug("debug.event");
      logger.info("info.event");

      expect(debugSpy).not.toHaveBeenCalled();
      expect(infoSpy).toHaveBeenCalledTimes(1);
    });


    it("logs warning and error when minLevel is warning", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "warning",
      });

      const debugSpy = vi
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const warnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const errorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.debug("debug.event");
      logger.info("info.event");
      logger.warning("warning.event");
      logger.error("error.event");

      expect(debugSpy).not.toHaveBeenCalled();
      expect(infoSpy).not.toHaveBeenCalled();
      expect(warnSpy).toHaveBeenCalledTimes(1);
      expect(errorSpy).toHaveBeenCalledTimes(1);
    });


    it("logs only errors when minLevel is error", () => {
      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "test",
        consoleEnabled: true,
        minLevel: "error",
      });

      const debugSpy = vi
        .spyOn(console, "debug")
        .mockImplementation(() => {});

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      const warnSpy = vi
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const errorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      logger.debug("debug.event");
      logger.info("info.event");
      logger.warning("warning.event");
      logger.error("error.event");

      expect(debugSpy).not.toHaveBeenCalled();
      expect(infoSpy).not.toHaveBeenCalled();
      expect(warnSpy).not.toHaveBeenCalled();
      expect(errorSpy).toHaveBeenCalledTimes(1);
    });
  });


  // ==========================================================
  // BROWSER HOST
  // ==========================================================

  describe("browser host", () => {
    it("uses window.location.hostname when host is not supplied", () => {
      Object.defineProperty(window, "location", {
        configurable: true,
        value: {
          hostname: "qa.stemx365.org",
        },
      });

      const logger = new LoggerService({
        service: "test-service",
        application: "TEST",
        environment: "qa",
        consoleEnabled: true,
      });

      const infoSpy = vi
        .spyOn(console, "info")
        .mockImplementation(() => {});

      logger.info("test.event");

      const entry = JSON.parse(
        infoSpy.mock.calls[0][0]
      );

      expect(entry.host).toBe(
        "qa.stemx365.org"
      );
    });
  });
});
```

---

# 5. Test `loggingHelpers.ts`

This one is much simpler because your functions are pure functions.

Create:

```text
loggingHelpers.test.ts
```

with:

```ts
import {
  describe,
  it,
  expect,
  vi,
  beforeEach,
  afterEach,
} from "vitest";

import {
  getLogInfo,
  getErrorInfo,
  getPerformanceInfo,
} from "./loggingHelpers";


describe("loggingHelpers", () => {

  // ==========================================================
  // getLogInfo
  // ==========================================================

  describe("getLogInfo", () => {

    it("returns component", () => {
      expect(
        getLogInfo("FilterOptions")
      ).toEqual({
        component: "FilterOptions",
      });
    });


    it("includes operation when supplied", () => {
      expect(
        getLogInfo(
          "FilterOptions",
          "options.fetch"
        )
      ).toEqual({
        component: "FilterOptions",
        operation: "options.fetch",
      });
    });


    it("includes custom attributes", () => {
      expect(
        getLogInfo(
          "FilterOptions",
          "options.fetch",
          {
            page: 1,
            count: 25,
          }
        )
      ).toEqual({
        component: "FilterOptions",
        operation: "options.fetch",
        page: 1,
        count: 25,
      });
    });


    it("works without operation but with attributes", () => {
      expect(
        getLogInfo(
          "CubeAPI",
          undefined,
          {
            endpoint: "/cube/load",
          }
        )
      ).toEqual({
        component: "CubeAPI",
        endpoint: "/cube/load",
      });
    });


    it("does not add operation when undefined", () => {
      const result = getLogInfo(
        "FilterSerializer",
        undefined
      );

      expect(result).not.toHaveProperty(
        "operation"
      );
    });


    it("handles empty attributes", () => {
      expect(
        getLogInfo(
          "FilterOptions",
          "fetch",
          {}
        )
      ).toEqual({
        component: "FilterOptions",
        operation: "fetch",
      });
    });
  });


  // ==========================================================
  // getErrorInfo
  // ==========================================================

  describe("getErrorInfo", () => {

    it("returns empty object for null", () => {
      expect(
        getErrorInfo(null)
      ).toEqual({});
    });


    it("returns empty object for undefined", () => {
      expect(
        getErrorInfo(undefined)
      ).toEqual({});
    });


    it("handles Error objects", () => {
      const error = new Error(
        "Something went wrong"
      );

      const result = getErrorInfo(error);

      expect(result.error_name).toBe(
        "Error"
      );

      expect(result.error_message).toBe(
        "Something went wrong"
      );

      expect(result.error_stack).toBe(
        error.stack
      );
    });


    it("handles custom Error types", () => {
      const error = new TypeError(
        "Invalid value"
      );

      const result = getErrorInfo(error);

      expect(result.error_name).toBe(
        "TypeError"
      );

      expect(result.error_message).toBe(
        "Invalid value"
      );

      expect(result.error_stack).toBe(
        error.stack
      );
    });


    it("handles string errors", () => {
      expect(
        getErrorInfo("Request failed")
      ).toEqual({
        error_message: "Request failed",
      });
    });


    it("handles numeric errors", () => {
      expect(
        getErrorInfo(500)
      ).toEqual({
        error: 500,
      });
    });


    it("handles object errors", () => {
      const error = {
        code: "CUBE_ERROR",
        status: 500,
      };

      expect(
        getErrorInfo(error)
      ).toEqual({
        error,
      });
    });


    it("handles boolean errors", () => {
      expect(
        getErrorInfo(false)
      ).toEqual({});
    });
  });


  // ==========================================================
  // getPerformanceInfo
  // ==========================================================

  describe("getPerformanceInfo", () => {

    beforeEach(() => {
      vi.spyOn(performance, "now")
        .mockReturnValue(1500);
    });


    afterEach(() => {
      vi.restoreAllMocks();
    });


    it("calculates duration", () => {
      const result =
        getPerformanceInfo(1000);

      expect(result.duration_ms).toBe(
        500
      );
    });


    it("includes additional attributes", () => {
      const result =
        getPerformanceInfo(
          1000,
          {
            operation: "cube.load",
            endpoint: "/cube/load",
          }
        );

      expect(result).toEqual({
        duration_ms: 500,
        operation: "cube.load",
        endpoint: "/cube/load",
      });
    });


    it("handles zero start time", () => {
      const result =
        getPerformanceInfo(0);

      expect(result.duration_ms).toBe(
        1500
      );
    });


    it("handles empty attributes", () => {
      const result =
        getPerformanceInfo(
          1000,
          {}
        );

      expect(result).toEqual({
        duration_ms: 500,
      });
    });
  });
});
```

---

# 6. Run the tests

Run once:

```bash
npm test
```

Or:

```bash
npm run test:run
```

For development, I prefer:

```bash
npm test
```

because Vitest stays running and reruns tests whenever you change the code.

You should see something like:

```text
✓ src/services/LoggerService.test.ts
✓ src/utils/loggingHelpers.test.ts

Test Files  2 passed (2)
Tests       40 passed (40)
```

---

# 7. One important issue in your current implementation

There is a subtle problem in:

```ts
getErrorInfo(error: unknown)
```

You have:

```ts
if (!error) {
  return {};
}
```

This means these values:

```ts
false
0
""
null
undefined
```

are all treated as "no error".

For logging, I would actually change this to:

```ts
export function getErrorInfo(
  error: unknown
): LogAttributes {

  if (error === null || error === undefined) {
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
```

Then:

```ts
getErrorInfo(false)
```

produces:

```ts
{
  error: false
}
```

and:

```ts
getErrorInfo(0)
```

produces:

```ts
{
  error: 0
}
```

That's generally more correct for a logging utility because **any value passed as an error should not silently disappear**.

---

# 8. I would add one more important test

Because this is a structured logging system, I would test the **schema itself**.

For example:

```ts
it("creates a valid structured log entry", () => {
  const logger = new LoggerService({
    service: "sda-query-builder",
    application: "SDA",
    environment: "qa",
    host: "qa.stemx365.org",
    schemaVersion: 1,
    consoleEnabled: true,
    minLevel: "debug",
  });

  const infoSpy = vi
    .spyOn(console, "info")
    .mockImplementation(() => {});

  logger.info(
    "filter.options.fetch.started",
    "Fetching filter options",
    "trace-123",
    {
      filter_id: 42,
    }
  );

  const entry = JSON.parse(
    infoSpy.mock.calls[0][0]
  );

  expect(entry).toMatchObject({
    level: "info",
    event: "filter.options.fetch.started",
    service: "sda-query-builder",
    application: "SDA",
    environment: "qa",
    host: "qa.stemx365.org",
    session_id: expect.any(String),
    trace_id: "trace-123",
    schema_version: 1,
    message: "Fetching filter options",
    attrs: {
      filter_id: 42,
    },
  });

  expect(entry.ts).toEqual(
    expect.any(String)
  );

  expect(
    new Date(entry.ts).toString()
  ).not.toBe("Invalid Date");
});
```

This test is particularly valuable for your logger because the important thing isn't just **"console.info was called."**

The important thing is that the application emits the expected structured event:

```json
{
  "ts": "...",
  "level": "info",
  "event": "filter.options.fetch.started",
  "service": "sda-query-builder",
  "application": "SDA",
  "environment": "qa",
  "host": "qa.stemx365.org",
  "session_id": "...",
  "trace_id": "...",
  "schema_version": 1,
  "message": "Fetching filter options",
  "attrs": {
    "filter_id": 42
  }
}
```

That is the contract I'd want protected by your Vitest suite, especially if you later add a backend/log collector or Fluent Bit.

### Recommended test coverage

For these two files, I'd aim for:

| Area           | Test                            |
| -------------- | ------------------------------- |
| Session        | generated                       |
| Session        | remains stable                  |
| Session        | `startSession()` creates new ID |
| Trace          | generated                       |
| Trace          | unique                          |
| Debug          | emitted                         |
| Info           | emitted                         |
| Warning        | `console.warn`                  |
| Error          | `console.error`                 |
| Attributes     | preserved                       |
| Message        | optional                        |
| Trace ID       | optional                        |
| Console        | enabled/disabled                |
| Filtering      | debug                           |
| Filtering      | info                            |
| Filtering      | warning                         |
| Filtering      | error                           |
| Host           | browser hostname                |
| Schema         | complete structured entry       |
| `getLogInfo`   | component                       |
| `getLogInfo`   | operation                       |
| `getLogInfo`   | attributes                      |
| `getErrorInfo` | `Error`                         |
| `getErrorInfo` | `TypeError`                     |
| `getErrorInfo` | string                          |
| `getErrorInfo` | object                          |
| `getErrorInfo` | null/undefined                  |
| Performance    | duration                        |
| Performance    | attributes                      |

That gives you a solid unit-test foundation without testing private implementation details.

Tighten the logger tests

* Add schema validation






================README===================


# LoggerService Usage Guide

## 1. Overview

`LoggerService` provides structured logging for the SDA Query Builder application.

Instead of using:

```ts
console.log("Query started");
```

use:

```ts
logger.info("query.started", "Query started");
```

The logger produces structured JSON that can later be collected by systems such as Fluent Bit and centralized logging platforms.

---

## 2. Import the Logger

Use the shared logger instance:

```ts
import logger from "./LoggerService";
```

Do **not** create a new `LoggerService` inside every component.

---

## 3. Log Levels

The logger supports four levels:

| Level     | Purpose                                        |
| --------- | ---------------------------------------------- |
| `debug`   | Detailed information useful during development |
| `info`    | Normal application activity                    |
| `warning` | Unexpected but recoverable conditions          |
| `error`   | Failed operations or errors                    |

Examples:

```ts
logger.debug("query.parameters", "Query parameters prepared");

logger.info("query.started", "Query started");

logger.warning("query.slow", "Query is taking longer than expected");

logger.error("query.failed", "Query failed");
```

---

## 4. Event Names

The `event` should be a stable, machine-readable name.

Use:

```text
query.started
query.completed
query.failed

filter.created
filter.updated
filter.deleted

cube_api.request.started
cube_api.request.completed
cube_api.request.failed
```

Avoid:

```ts
logger.info("Something happened");
```

The event should describe **what happened**, while `message` provides a human-readable description.

---

## 5. Additional Attributes

Application-specific information should be placed in `attrs`.

```ts
logger.info(
  "filter.created",
  "Filter created",
  undefined,
  {
    filter_id: 25,
    filter_name: "Revenue",
    filter_count: 3,
  }
);
```

This produces structured information that can later be searched or analyzed.

---

## 6. Trace IDs

Use a trace ID when several log messages belong to the same operation.

```ts
const traceId = logger.newTraceId();

logger.info(
  "query.started",
  "Query started",
  traceId
);

logger.info(
  "query.completed",
  "Query completed",
  traceId
);
```

All logs for the operation will contain the same `trace_id`.

This is especially useful for asynchronous operations such as API requests.

---

## 7. API Request Example

A typical API operation should look like:

```ts
const traceId = logger.newTraceId();

logger.info(
  "cube_api.request.started",
  "Starting Cube API request",
  traceId,
  {
    endpoint: "/cube/load",
  }
);

try {
  const result = await loadCube();

  logger.info(
    "cube_api.request.completed",
    "Cube API request completed",
    traceId,
    {
      status: 200,
    }
  );

  return result;
} catch (error) {
  logger.error(
    "cube_api.request.failed",
    "Cube API request failed",
    traceId,
    getErrorInfo(error)
  );

  throw error;
}
```

---

## 8. Error Logging

Use `getErrorInfo()` from `loggingHelpers.ts`.

```ts
import { getErrorInfo } from "./loggingHelpers";
```

Example:

```ts
try {
  await loadData();
} catch (error) {
  logger.error(
    "data.load.failed",
    "Failed to load data",
    traceId,
    getErrorInfo(error)
  );
}
```

For an `Error`, the logger can capture:

```json
{
  "error_name": "TypeError",
  "error_message": "Invalid value",
  "error_stack": "..."
}
```

---

## 9. Component and Operation Information

Use `getLogInfo()` when logging from a specific component.

```ts
import { getLogInfo } from "./loggingHelpers";

logger.info(
  "filter.load.started",
  "Loading filters",
  traceId,
  getLogInfo("FilterPanel", "loadFilters")
);
```

You can also add application-specific attributes:

```ts
logger.info(
  "filter.load.completed",
  "Filters loaded",
  traceId,
  getLogInfo(
    "FilterPanel",
    "loadFilters",
    {
      filter_count: 10,
    }
  )
);
```

---

## 10. Performance Logging

Use `getPerformanceInfo()` to measure operation duration.

```ts
import { getPerformanceInfo } from "./loggingHelpers";

const startTime = performance.now();

await loadFilters();

logger.info(
  "filter.load.completed",
  "Filters loaded",
  traceId,
  getPerformanceInfo(startTime, {
    filter_count: 10,
  })
);
```

The resulting attributes include:

```json
{
  "duration_ms": 125.42,
  "filter_count": 10
}
```

---

## 11. Recommended Async Pattern

For most important asynchronous operations, use this pattern:

```ts
const traceId = logger.newTraceId();
const startTime = performance.now();

logger.info(
  "operation.started",
  "Operation started",
  traceId
);

try {
  const result = await performOperation();

  logger.info(
    "operation.completed",
    "Operation completed",
    traceId,
    getPerformanceInfo(startTime)
  );

  return result;
} catch (error) {
  logger.error(
    "operation.failed",
    "Operation failed",
    traceId,
    {
      ...getPerformanceInfo(startTime),
      ...getErrorInfo(error),
    }
  );

  throw error;
}
```

This provides:

* Start event
* Completion or failure event
* Trace ID
* Execution time
* Error information

---

## 12. Session ID vs Trace ID

The logger automatically creates a `session_id`.

### Session ID

Identifies the application session.

```text
session_id
```

It remains the same until:

```ts
logger.startSession();
```

is called.

### Trace ID

Identifies one particular operation.

```ts
const traceId = logger.newTraceId();
```

A session can contain many trace IDs.

```text
Session
 ├── Trace: query execution
 ├── Trace: filter loading
 ├── Trace: Cube API request
 └── Trace: export operation
```

---

## 13. Structured Log Format

A typical log entry looks like:

```json
{
  "ts": "2026-09-23T12:00:00.000Z",
  "level": "info",
  "event": "query.completed",
  "service": "sda-query-builder",
  "application": "SDA",
  "environment": "qa",
  "host": "qa.stemx365.org",
  "session_id": "sda-query-builder.20260923...",
  "trace_id": "sda-query-builder....",
  "schema_version": 1,
  "message": "Query completed",
  "attrs": {
    "duration_ms": 125,
    "row_count": 250
  }
}
```

The fixed fields provide consistency across the application. The `attrs` object contains operation-specific information.

---

## 14. What Should Be Logged

Good examples:

```ts
logger.info("user.login.completed");

logger.info("query.started");

logger.info("query.completed", undefined, traceId, {
  row_count: 250,
});

logger.warning("query.slow", "Query exceeded expected duration");

logger.error("query.failed", "Query execution failed", traceId, {
  ...getErrorInfo(error),
});
```

---

## 15. What Should Not Be Logged

Do not log sensitive information:

```ts
// Do NOT do this
logger.info("user.login", "User logged in", undefined, {
  password: password,
  token: accessToken,
  api_key: apiKey,
});
```

Instead log safe information:

```ts
logger.info("user.login.completed", "User login completed", undefined, {
  authentication_method: "password",
});
```

Never place passwords, authentication tokens, API keys, or secrets into log attributes.

---

## 16. Do Not Use Direct Console Logging

Application code should generally use:

```ts
logger.info(...)
logger.warning(...)
logger.error(...)
logger.debug(...)
```

instead of:

```ts
console.log(...)
console.info(...)
console.warn(...)
console.error(...)
```

This keeps logging consistent and makes it possible to change the logging destination later without changing application code.

---

## 17. Environment and Log Levels

The logger supports a minimum log level.

For example:

```ts
minLevel: "debug"
```

allows all logs.

```ts
minLevel: "info"
```

ignores debug logs.

A typical configuration can be:

| Environment | Minimum Level |
| ----------- | ------------- |
| Development | `debug`       |
| QA          | `debug`       |
| Staging     | `info`        |
| Production  | `info`        |

The exact production configuration can be changed later without changing application logging calls.

---

## 18. Logging Architecture

The current logger writes structured JSON to the browser console.

The intended architecture is:

```text
SDA Query Builder
        |
        v
  LoggerService
        |
        v
 Structured JSON
        |
        v
 Log Collector
        |
        v
    Fluent Bit
        |
        v
 Central Logging System
```

Because the application already produces structured JSON, the logging backend can be added later without redesigning how application code creates logs.

---

## 19. Main Rules

When using `LoggerService`:

1. Use the shared logger instance.
2. Use stable event names.
3. Use `message` for human-readable descriptions.
4. Use `attrs` for additional structured information.
5. Use a `trace_id` for multi-step operations.
6. Use `getErrorInfo()` for errors.
7. Use `getPerformanceInfo()` for timing.
8. Do not log secrets or credentials.
9. Avoid direct `console.log()` calls in application code.
10. Keep the log structure consistent so it can be processed by future centralized logging systems.

