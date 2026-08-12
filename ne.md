
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
// VALUE CHANGE
// ============================================================

/**
 * Creates attributes for a value change.
 *
 * Useful for any part of the application.
 */
export function getValueChangeInfo(
  component: string,
  oldValue: unknown,
  newValue: unknown,
  attrs?: LogAttributes
): LogAttributes {
  return getLogInfo(
    component,
    "value.changed",
    {
      old_value: oldValue,
      new_value: newValue,

      ...(attrs ?? {}),
    }
  );
}


// ============================================================
// FIELD CHANGE
// ============================================================

/**
 * Creates attributes for a field change.
 */
export function getFieldChangeInfo(
  component: string,
  oldField: unknown,
  newField: unknown,
  attrs?: LogAttributes
): LogAttributes {
  return getLogInfo(
    component,
    "field.changed",
    {
      old_field: oldField,
      new_field: newField,

      ...(attrs ?? {}),
    }
  );
}


// ============================================================
// OPERATOR CHANGE
// ============================================================

/**
 * Creates attributes for an operator change.
 */
export function getOperatorChangeInfo(
  component: string,
  oldOperator: unknown,
  newOperator: unknown,
  attrs?: LogAttributes
): LogAttributes {
  return getLogInfo(
    component,
    "operator.changed",
    {
      old_operator: oldOperator,
      new_operator: newOperator,

      ...(attrs ?? {}),
    }
  );
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
