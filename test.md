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

