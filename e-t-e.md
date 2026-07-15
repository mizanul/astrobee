Below is an architecture overview suitable for a software engineering report or thesis. It describes the complete end-to-end workflow for a user creating a **stacked bar chart**, including authentication, authorization, query generation, visualization, and audit logging.

---

# End-to-End Architecture Overview: Creating a Stacked Bar Chart

## Introduction

Creating a stacked bar chart is a representative workflow that demonstrates the interaction between the presentation layer, application layer, authentication and authorization services, database, and visualization engine. This workflow illustrates how the system securely processes user requests while enforcing access control, executing analytical queries, generating visualizations, and maintaining an audit trail.

The architecture follows a layered design consisting of a React frontend, a Node.js REST API backend, Microsoft SQL Server as the data repository, and an audit logging subsystem.

---

# System Architecture

```text
+--------------------------------------------------------------+
|                     React Frontend                           |
|--------------------------------------------------------------|
| Login | Query Builder | Dashboard | Chart Viewer             |
+---------------------------+----------------------------------+
                            |
                      HTTPS + JWT
                            |
+---------------------------v----------------------------------+
|                  Node.js / Express API                       |
|--------------------------------------------------------------|
| Authentication Middleware                                   |
| Authorization Middleware                                    |
| Query Validation Service                                    |
| SQL Query Builder                                            |
| Data Processing Service                                      |
| Visualization Metadata Service                               |
| Audit Logging Service                                        |
+---------------------------+----------------------------------+
                            |
                -----------------------------
                |                           |
      Microsoft SQL Server           Audit Database
                |
          Analytical Data
```

---

# End-to-End Workflow

## Step 1: User Authentication

The user begins by logging into the application through the React frontend.

Authentication workflow:

1. User enters username and password.
2. Credentials are transmitted securely over HTTPS.
3. Node.js validates credentials.
4. User information is retrieved from SQL Server.
5. Password hash is verified.
6. A JSON Web Token (JWT) is generated.
7. JWT is returned to the React application.
8. React stores the token securely for subsequent API requests.

At this stage, the user has been authenticated but has not yet been authorized to access specific datasets.

---

## Step 2: Authorization

When the user opens the Query Builder, the frontend sends the JWT with every API request.

The backend performs authorization by:

* Validating the JWT signature.
* Verifying token expiration.
* Identifying the authenticated user.
* Retrieving assigned roles.
* Determining dataset permissions.

Example roles include:

* Administrator
* Analyst
* Manager
* Viewer

Authorization determines:

* Which datasets can be accessed.
* Which columns are visible.
* Which analytical functions are permitted.
* Whether exporting results is allowed.

Unauthorized requests are rejected before any SQL query is generated.

---

## Step 3: Query Construction

The authenticated user creates a stacked bar chart using the visual query builder.

The user selects:

* Dataset
* Category (X-axis)
* Numerical measure
* Stack grouping
* Filters
* Sorting
* Aggregation (SUM, COUNT, AVG, etc.)

For example:

Dataset:

```
Sales
```

Category:

```
Region
```

Stack:

```
Product Category
```

Measure:

```
Sales Amount
```

Aggregation:

```
SUM
```

No SQL is written manually by the user.

---

## Step 4: Request Submission

React serializes the query definition into JSON and sends it to the backend.

Example request:

```json
{
    "dataset":"Sales",
    "chartType":"StackedBar",
    "xAxis":"Region",
    "stack":"Category",
    "measure":"SalesAmount",
    "aggregation":"SUM",
    "filters":{
        "Year":2025
    }
}
```

The request also includes:

* JWT
* Session identifier
* Request ID

---

## Step 5: Query Validation

The backend validates:

* Dataset exists.
* Requested fields exist.
* User has permission.
* Aggregation is valid.
* Filters are valid.
* SQL injection is impossible.

Only validated metadata is used to construct SQL.

---

## Step 6: SQL Query Generation

The Query Builder converts the metadata into a parameterized SQL statement.

Example:

```sql
SELECT
    Region,
    Category,
    SUM(SalesAmount) AS TotalSales
FROM Sales
WHERE Year = @Year
GROUP BY
    Region,
    Category
ORDER BY Region;
```

Parameterized queries eliminate SQL injection risks.

---

## Step 7: Query Execution

Node.js submits the SQL statement to Microsoft SQL Server.

SQL Server:

* Optimizes the query.
* Executes joins and aggregations.
* Returns the result set.

Example:

| Region | Category    | TotalSales |
| ------ | ----------- | ---------- |
| North  | Electronics | 45000      |
| North  | Furniture   | 23000      |
| South  | Electronics | 51000      |
| South  | Furniture   | 19000      |

---

## Step 8: Data Transformation

The backend converts relational data into a visualization-friendly format.

Example:

```json
[
  {
    "Region":"North",
    "Electronics":45000,
    "Furniture":23000
  },
  {
    "Region":"South",
    "Electronics":51000,
    "Furniture":19000
  }
]
```

Metadata describing the visualization is also included.

---

## Step 9: Chart Rendering

The React frontend receives:

* Processed data
* Chart metadata

The visualization component renders a stacked bar chart using the selected charting library.

The user can then:

* Zoom
* Apply filters
* Change colors
* Download
* Save dashboard

---

## Step 10: Audit Logging

After successful execution, the Audit Logging Service records the activity.

Example audit information includes:

| Field          | Value                       |
| -------------- | --------------------------- |
| User           | John Smith                  |
| Action         | Generated Stacked Bar Chart |
| Dataset        | Sales                       |
| Chart Type     | Stacked Bar                 |
| Filters        | Year = 2025                 |
| Execution Time | 215 ms                      |
| Rows Returned  | 128                         |
| Timestamp      | 2026-07-15 14:32 UTC        |
| IP Address     | 10.10.20.45                 |

This information supports governance, reproducibility, and security investigations.

---

# Security Considerations

The workflow incorporates multiple security mechanisms.

**Authentication**

* JWT-based authentication
* Secure password hashing
* HTTPS encryption

**Authorization**

* Role-Based Access Control (RBAC)
* Dataset-level permissions
* Column-level permissions
* Feature-level authorization

**Query Security**

* Parameterized SQL
* Query validation
* Input sanitization

**Audit**

* Complete activity logging
* Immutable audit records
* Timestamped events

---

# Sequence of Operations

```text
User
 │
 │ Login
 ▼
React Frontend
 │
 │ HTTPS
 ▼
Authentication API
 │
 │ Verify Credentials
 ▼
SQL Server
 │
 │ Return User
 ▼
Generate JWT
 │
 ▼
React Dashboard
 │
 │ Build Stacked Bar Chart
 ▼
Node.js API
 │
 │ Validate JWT
 │
 │ Check Permissions
 │
 │ Validate Query
 │
 │ Generate SQL
 ▼
SQL Server
 │
 │ Execute Query
 ▼
Return Results
 │
 ▼
Data Transformation
 │
 ▼
React Chart Component
 │
 ▼
Render Stacked Bar Chart
 │
 ▼
Audit Logging Service
 │
 ▼
Audit Database
```

---

# Benefits of the Architecture

This architecture provides several advantages:

* **Security:** Authentication, authorization, and parameterized SQL protect sensitive data and prevent unauthorized access.
* **Scalability:** The separation of the frontend, backend, database, and audit services supports independent scaling and maintenance.
* **Maintainability:** Query generation, visualization, authentication, and logging are implemented as distinct services with clear responsibilities.
* **Performance:** Server-side query optimization and data transformation reduce the amount of processing required by the client.
* **Traceability:** Comprehensive audit logging records all significant user actions, enabling compliance reporting, usage analysis, and forensic investigations.
* **Extensibility:** The same workflow can support additional visualization types, such as line charts, heat maps, scatter plots, and dashboards, without requiring significant architectural changes.

This end-to-end workflow demonstrates how a modern analytics platform securely transforms a user's high-level visualization request into a validated SQL query, retrieves the necessary data, renders an interactive stacked bar chart, and records the operation in an audit log for governance and accountability.
