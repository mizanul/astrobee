Your `App.jsx` is a good candidate for **React Testing Library + Vitest** (or Jest). Since `AppContent` is entirely driven by the `useHashRoute()` hook, the easiest approach is to **mock the hook** and verify that the correct page is rendered.

## 1. Install testing packages

If using **Vite**:

```bash
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

If using **Create React App**, Jest is already included, so you only need:

```bash
npm install -D @testing-library/react @testing-library/jest-dom
```

---

## 2. Configure Vitest

`vitest.config.js`

```javascript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
});
```

`src/test/setup.js`

```javascript
import '@testing-library/jest-dom';
```

---

## 3. Mock all child components

Your goal is to test **routing**, not the implementation of DashboardView, CardEditorPage, etc.

```javascript
// App.test.jsx

import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import App from './App';

vi.mock('./analytics/navigation/Router', () => ({
  useHashRoute: vi.fn(),
}));

vi.mock('./analytics/layout/DashboardView', () => ({
  DashboardView: () => <div>Dashboard View</div>,
}));

vi.mock('./analytics/pages/CardEditorPage', () => ({
  CardEditorPage: (props) => (
    <div>
      Card Editor
      <span data-testid="card-id">{props.cardId}</span>
    </div>
  ),
}));

vi.mock('./analytics/pages/QueryBuilderPage', () => ({
  QueryBuilderPage: (props) => (
    <div>
      Query Builder
      <span data-testid="question-id">{props.questionId}</span>
    </div>
  ),
}));

vi.mock('./analytics/layout/AppLayout', () => ({
  AppLayout: ({ children }) => <div>{children}</div>,
}));

vi.mock('./components/theme-provider', () => ({
  ThemeProvider: ({ children }) => <>{children}</>,
}));

import { useHashRoute } from './analytics/navigation/Router';
```

---

## 4. Test Dashboard route

```javascript
test('renders DashboardView by default', () => {
  useHashRoute.mockReturnValue({
    type: 'dashboard',
  });

  render(<App />);

  expect(screen.getByText('Dashboard View')).toBeInTheDocument();
});
```

---

## 5. Test Card Editor route

```javascript
test('renders CardEditorPage', () => {
  useHashRoute.mockReturnValue({
    type: 'card-editor',
    cardId: '123',
    dashboardId: '1',
    returnTo: 'dashboard',
    fromQuestionId: '55',
  });

  render(<App />);

  expect(screen.getByText('Card Editor')).toBeInTheDocument();
  expect(screen.getByTestId('card-id')).toHaveTextContent('123');
});
```

---

## 6. Test Query Builder route

```javascript
test('renders QueryBuilderPage', () => {
  useHashRoute.mockReturnValue({
    type: 'query-builder',
    questionId: '999',
    returnTo: 'dashboard',
    returnToCard: 'abc',
  });

  render(<App />);

  expect(screen.getByText('Query Builder')).toBeInTheDocument();
  expect(screen.getByTestId('question-id')).toHaveTextContent('999');
});
```

---

# What these tests verify

| Route           | Expected Component |
| --------------- | ------------------ |
| `dashboard`     | DashboardView      |
| `card-editor`   | CardEditorPage     |
| `query-builder` | QueryBuilderPage   |

These are **unit tests** because you're testing only the logic inside `AppContent` and `App`, while mocking all dependencies.

---

## Optional improvement

To make testing even cleaner, export `AppContent` separately:

```javascript
export function AppContent() {
    ...
}

export default function App() {
    ...
}
```

Then you can write focused unit tests for `AppContent` without needing to include `ThemeProvider` or `AppLayout`. Keep a single smoke test for `App` that simply verifies it renders without crashing, and put most of your routing tests against `AppContent`.

This approach scales well as your application grows because each routing decision can be tested independently from the surrounding layout and providers.



==========================================================


Looking at your codebase, I would **not** start by writing unit tests for `CardEditorPage.jsx` (1,858 lines) or `QueryBuilderPage.jsx` (577 lines). Those are orchestration components with dozens of hooks, effects, and dependencies.  

Instead, I would use the **testing pyramid**.

| Component          | Recommended Test                |
| ------------------ | ------------------------------- |
| `DashboardStorage` | **Unit tests** ✅                |
| `AppContent`       | **Unit tests** ✅                |
| `CardEditorPage`   | Integration tests               |
| `QueryBuilderPage` | Integration tests               |
| Navigation         | Integration tests               |
| Chart rendering    | Integration tests               |
| Complete workflow  | End-to-end (Playwright/Cypress) |

## 1. DashboardStorage should have extensive unit tests

This file is perfect for unit testing because it contains business logic without React. It manages loading, saving, locking, duplication, metadata, and localStorage persistence. 

I would write tests like:

```
DashboardStorage.test.ts
```

```javascript
describe("DashboardStorage", () => {

    beforeEach(() => {
        localStorage.clear();
        DashboardStorage.loadFromLocalStorage();
    });

    test("save dashboard", () => {
    });

    test("load dashboard", () => {
    });

    test("delete dashboard", () => {
    });

    test("rename dashboard", () => {
    });

    test("duplicate dashboard", () => {
    });

    test("locked dashboard cannot save", () => {
    });

    test("locked dashboard cannot delete", () => {
    });

    test("list metadata", () => {
    });

    test("clearAll removes only user dashboards", () => {
    });

});
```

This one file alone should probably have **20–30 unit tests**.

---

# 2. App.jsx should have 3 unit tests

Exactly as I suggested previously.

```
✓ dashboard route

✓ card-editor route

✓ query-builder route
```

---

# 3. CardEditorPage should be integration tested

`CardEditorPage` contains:

* 30+ useState
* many useEffect
* DashboardStorage
* QueryRegistry
* navigation
* chart preview
* dialogs
* dropdowns
* tabs
* filters
* chart options

It's essentially an entire application inside one component. 

Testing every internal function individually would require mocking dozens of dependencies and would provide little value.

Instead, test what the user does.

Example:

```
render(<CardEditorPage .../>)

Select query

Choose chart type

Click Save

expect(DashboardStorage.save).toHaveBeenCalled()
```

Another example:

```
render(...)

Click Cancel

expect(navigateToDashboard).toHaveBeenCalled()
```

Another:

```
Load existing card

Change title

Save

expect(storage.save).toHaveBeenCalledWith(...)
```

These are much more maintainable than testing every `useEffect`.

---

# 4. QueryBuilderPage should also be integration tested

The same applies here. It manages form state, validation, query registry, visualization settings, save/save-as flows, navigation, and dialogs. 

Typical tests:

```
✓ shows validation errors

✓ Save disabled until valid

✓ Save stores query

✓ Save As creates copy

✓ Delete removes query

✓ Cancel navigates home

✓ Auto ID generation

✓ Editing existing query loads values
```

---

# 5. Extract pure logic for unit testing

Several functions embedded inside these components would be much easier to test if moved into utility modules.

For example, from `CardEditorPage`:

```javascript
findCardsUsingQuery(...)
```

can become

```
utils/findCardsUsingQuery.ts
```

and then have simple unit tests.

Similarly:

```
buildVisualization()

generateCard()

validateAxis()

reconcileTableColumns()
```

can each be tested without rendering React.

---

## Recommended testing strategy for your analytics application

Given the size of your project, I'd aim for approximately:

* **DashboardStorage:** 25–35 unit tests
* **App routing:** 3–5 unit tests
* **Utility functions:** 20–30 unit tests (after extracting logic)
* **CardEditorPage:** 15–20 integration tests
* **QueryBuilderPage:** 10–15 integration tests
* **DashboardView:** 10–15 integration tests
* **End-to-end workflows:** 8–10 Playwright tests

This gives you strong coverage while keeping the tests maintainable. The main opportunity for easier testing is to extract business logic from `CardEditorPage`; at nearly 1,900 lines, it's doing enough work that splitting out pure functions would significantly simplify both testing and future maintenance.

