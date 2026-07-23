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
