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



===========================================================================


Absolutely. I spent some time looking through the three files. They actually form a fairly clean architecture. Think of them as three layers:

```
                    React UI
                        │
        ┌───────────────┴───────────────┐
        │                               │
 CardEditorPage                 QueryBuilderPage
        │                               │
        └───────────────┬───────────────┘
                        │
               DashboardStorage
                        │
                 localStorage
```

The editors don't store anything themselves. They ask `DashboardStorage` to persist dashboards, while queries are stored through `QueryRegistry`. `DashboardStorage` is responsible only for dashboards. 

---

# 1. DashboardStorage

This is the easiest component to understand.

It is essentially a **repository/service layer**.

It owns three in-memory collections:

```text
Map<String, Dashboard> dashboards

Map<String, updatedAt>

Set<String> lockedIds
```

These are initialized when the application starts.

```
loadFromLocalStorage()

        │

localStorage
      │
      ▼

Map<dashboardId, Dashboard>

Map<dashboardId, updatedAt>
```



---

## Save

When someone saves a dashboard:

```
DashboardStorage.save(dashboard)

        │

update Map

update updatedAt

persist()
```

`persist()` writes only **user dashboards** to localStorage.

Preset dashboards are skipped.

```
dashboard map
     │
     ├── preset dashboard ❌
     ├── preset dashboard ❌
     ├── user dashboard ✅
     └── user dashboard ✅

            ↓

localStorage
```

That's why you see

```ts
.filter((d) => !this.lockedIds.has(d.id))
```

inside `persist()`. 

---

## Locked dashboards

This is a nice design.

There are two kinds of dashboards.

```
Preset dashboards
-----------------
NASA Dashboard
Sales Dashboard
Executive Dashboard

User dashboards
---------------
My Dashboard
Test Dashboard
Customer Dashboard
```

Preset dashboards are loaded from your bundle.

The user can view them but cannot modify them.

```
lockedIds

NASA

Sales

Executive
```

Whenever

```
save()

delete()

rename()
```

is called, the first thing checked is

```
lockedIds.has(id)
```

If true

↓

throw LockedEntityError



---

## Duplicate

Duplicating is simple.

```
Existing Dashboard

Dashboard A

    Card1
    Card2
    Card3
```

↓

```
duplicate()

```

↓

```
Dashboard B

new id

new name

every card copied

every card gets a NEW id
```

Notice

```ts
card.id-${Date.now()}
```

to avoid duplicate card IDs. 

---

## Metadata

Notice that `list()` does **not** return dashboards.

Instead it returns

```
id

name

description

updatedAt

locked

cardCount
```

This is much smaller.

The UI can show a dashboard list without loading every chart.



---

# 2. QueryBuilderPage

This page creates or edits **queries**.

Think of it like Power BI's query editor or Metabase's question editor.

```
SQL Builder

↓

Saved Query

↓

Used by many charts
```

The important idea is:

**One query can feed many charts.**

```
Revenue Query

        │

   ┌────┴────┐

 Bar Chart

 Line Chart

 Table

 KPI Card
```

That's why the query editor and chart editor are separate.

---

## State

The page keeps lots of UI state.

```
question

visualization

validation

saving

dirty

dialogs
```

Most of these are ordinary React state. 

---

## Loading

When editing

```
questionId exists

↓

registry.get(questionId)

↓

populate form
```

When creating

```
empty query

↓

blank form
```

Or

```
drill-down

↓

consumePendingDrillResult()

↓

prefill form
```



---

## Validation

Before saving it checks

```
Name?

ID?

Cube?

Dimension or Measure?

Duplicate ID?
```

If anything fails

↓

Save button disabled

↓

validation messages displayed



---

## Cube Cache

One clever feature is

```
cubeCache
```

Suppose the user does

```
Cube A

Customer

Revenue

Filters...
```

then changes to

```
Cube B
```

Instead of destroying the first configuration

it remembers it.

```
Cube A

↓

cache

↓

Cube B

↓

cache

↓

switch back

↓

restore previous selections
```

Very user friendly.



---

## Saving

```
Save

↓

buildCompleteQuestion()

↓

QueryRegistry.register()

↓

navigate back
```

"Save As"

```
copy name

generate new id

register()

open copied query
```



---

# 3. CardEditorPage

This is the most complex file.

It edits **chart cards** inside dashboards.

A dashboard contains cards.

A chart card references a query.

```
Dashboard

    Card

        Query

            Cube
```

So the hierarchy is

```
Dashboard

    ├── Card A
    ├── Card B
    └── Card C

Card

    references

Query
```

---

## Initialization

When the editor opens

```
Edit existing card?

        yes

↓

load dashboard

↓

find card

↓

populate state
```

Otherwise

```
new card

↓

blank state

↓

optional query preselected
```

This entire initialization happens inside the main `useEffect`. 

---

## Editor state

The page keeps track of

```
card title

description

query

chart type

axis mapping

chart options

filters

table columns

preview

saving

tabs

dirty state
```

Nearly everything about a chart is represented as React state. 

---

## Dirty checking

Instead of comparing every field individually,

it snapshots the initial state:

```
JSON.stringify(...)

↓

initialState
```

Later

```
currentState

↓

JSON.stringify

↓

compare strings
```

If different

↓

Unsaved changes

↓

Navigation guard enabled

This is a simple and effective approach for a state object composed of plain data. 

---

## Query change

When a new query is selected

```
new query

↓

load visualization defaults

↓

reset axis mapping

↓

clear filters

↓

clear table columns

↓

new preview
```



---

## Chart type cache

This is probably the smartest feature in the file.

Imagine

```
Bar Chart

X = Month

Y = Revenue
```

Then

```
switch

↓

Table
```

Then

```
switch

↓

Line
```

Then

```
switch

↓

Bar
```

Without a cache,

all settings would be lost.

Instead it stores

```
Bar

↓

config

↓

Map

↓

Line

↓

config

↓

Table

↓

config
```

Every chart type remembers its own configuration.



---

## Visualization builder

Eventually everything is converted into one object.

```
chart type

axis mapping

chart options

table columns

↓

visualization
```

That object is what gets stored in the dashboard.



---

## Saving

Saving works like this:

```
User clicks Save

        │

selectedQueryId?

        │

Load Dashboard

        │

Build ChartCardData

        │

Existing card?

      /     \

Update      Create

      \     /

DashboardStorage.save()

        │

Navigate back
```



---

# Overall architecture

Putting the three files together gives this flow:

```text
               QueryBuilderPage
                      │
               Creates Query
                      │
                      ▼
               QueryRegistry
                      │
                      ▼
             CardEditorPage
                      │
        Selects a saved Query
                      │
        Builds Visualization
                      │
                      ▼
           DashboardStorage.save()
                      │
                      ▼
              Dashboard
             (contains Cards)
                      │
                      ▼
               DashboardView
```

I think the overall design is solid because it cleanly separates responsibilities:

* **`DashboardStorage`** is a persistence layer that knows how dashboards are stored and protected.
* **`QueryBuilderPage`** is responsible for creating and editing reusable queries.
* **`CardEditorPage`** is responsible for configuring how a query is visualized and embedding that visualization into a dashboard.

The main weakness isn't the architecture itself but the size of `CardEditorPage`. At nearly 1,900 lines, it combines state management, business logic, data transformation, and UI rendering in one component. Splitting pieces like visualization building, reconciliation logic, chart-type caching, and save operations into separate hooks or utility modules would make the code easier to understand, test, and maintain without changing the overall architecture.



==========================================USE==============================

Yes. This is exactly what I would do if I were the lead architect on this project. The goal is **not** to change the behavior. The goal is to make the code smaller, easier to test, and easier to maintain.

## Current architecture

```
CardEditorPage (≈1858 lines)

├── UI Rendering
├── useState (30+)
├── useEffect (many)
├── Validation
├── Dirty Checking
├── Query Loading
├── Dashboard Loading
├── Chart Type Cache
├── Visualization Builder
├── Axis Reconciliation
├── Table Reconciliation
├── Save Logic
├── Delete Logic
├── Navigation
├── Dialog Management
└── Preview
```

It has become a "God Component."

---

# Target Architecture

```
CardEditorPage (300-500 lines)

├── Layout
├── Toolbar
├── Preview
├── Editor
└── Hooks

analytics/
    hooks/
        useCardEditor.ts
        useVisualization.ts
        useChartCache.ts
        useCardSave.ts
        useCardValidation.ts

    utils/
        visualizationBuilder.ts
        reconciliation.ts
        cardFactory.ts
        chartCache.ts
        dirtyCheck.ts
```

Notice the React component becomes almost entirely UI.

---

# 1. Extract Visualization Builder

Currently the page probably contains code similar to

```ts
const visualization = {
    type,
    xAxis,
    yAxis,
    legend,
    colors,
    stacking,
    labels,
    tableColumns,
    ...
};
```

Move all of that into

```
analytics/utils/visualizationBuilder.ts
```

```ts
export interface VisualizationInput {
    chartType: ChartType;
    selectedDimensions: string[];
    selectedMeasures: string[];
    chartOptions: ChartOptions;
    tableColumns: TableColumn[];
}

export function buildVisualization(
    input: VisualizationInput
): VisualizationConfig {

    ...

    return visualization;
}
```

Now your page becomes

```ts
const visualization = buildVisualization({
    chartType,
    selectedDimensions,
    selectedMeasures,
    chartOptions,
    tableColumns
});
```

Testing becomes

```ts
describe("buildVisualization", ()=>{

    it("builds bar chart")

    it("builds line chart")

    it("builds pie chart")

    it("builds stacked chart")

})
```

No React involved.

---

# 2. Extract Reconciliation

I noticed lots of logic like

```
selected dimension disappears

↓

remove from axis

↓

remove from table

↓

remove from filters

↓

remove from legend
```

This is business logic.

Move it to

```
analytics/utils/reconciliation.ts
```

```ts
export function reconcileVisualization(
    query,
    visualization
){

}
```

Now your component simply says

```ts
const newVisualization =
    reconcileVisualization(
        query,
        visualization
    );
```

Unit tests

```ts
describe("reconcileVisualization"){

    removes deleted measure

    removes deleted dimension

    updates table columns

    preserves existing options

}
```

---

# 3. Extract Chart Cache

Right now the component probably has

```ts
const chartCache = useRef(new Map());
```

This deserves its own hook.

```
hooks/useChartCache.ts
```

```ts
export function useChartCache(){

    const cache = useRef(new Map());

    function save(type,config){

    }

    function load(type){

    }

    return {
        save,
        load
    }

}
```

Then

```ts
const chartCache =
    useChartCache();
```

instead of

200 lines inside CardEditor.

Testing

```ts
save()

load()

overwrite()

clear()
```

---

# 4. Extract Save Logic

Currently save probably does

```
validate

↓

build card

↓

load dashboard

↓

replace/add

↓

DashboardStorage.save()

↓

navigate
```

Move into

```
hooks/useCardSave.ts
```

```ts
export function useCardSave(){

    async function save(){

    }

    async function saveAs(){

    }

    async function deleteCard(){

    }

    return {
        save,
        saveAs,
        deleteCard
    }

}
```

Now CardEditorPage becomes

```ts
const {
    save,
    saveAs
} = useCardSave(...);

<Button onClick={save}/>
```

instead of 200 lines of save logic.

---

# 5. Extract Dirty Checking

Instead of

```ts
const dirty =
JSON.stringify(a)!=JSON.stringify(b);
```

inside CardEditor

create

```
utils/dirtyCheck.ts
```

```ts
export function isDirty(
    original,
    current
){

}
```

Then

```ts
const dirty =
isDirty(original,current);
```

Very easy to test.

---

# 6. Extract Validation

Current

```
if title missing

if query missing

if axis invalid

if duplicate

if ...

...
```

Move

```
utils/cardValidation.ts
```

```ts
export interface ValidationResult{

    valid:boolean;

    errors:string[];

}

export function validateCard(){

}
```

Tests

```
missing title

missing query

duplicate name

invalid axis

invalid chart

```

---

# 7. Extract Card Factory

Currently save constructs

```ts
const card = {

    id,

    title,

    visualization,

    query,

    layout,

    ...

}
```

Move into

```
utils/cardFactory.ts
```

```ts
export function createCard(
    editorState
){

}
```

Testing becomes trivial.

---

# 8. Extract Dashboard Operations

Instead of

```
load dashboard

find card

replace

save

navigate
```

create

```
services/CardRepository.ts
```

```ts
updateCard()

addCard()

deleteCard()

duplicateCard()
```

This wraps `DashboardStorage` and keeps persistence concerns out of the UI.

---

# 9. Create a Main Hook

The component itself should not own 40 pieces of state.

Create

```
useCardEditor.ts
```

```
CardEditorPage

        │

useCardEditor()

        │

returns

{
    card,

    dirty,

    save,

    validation,

    visualization,

    chartCache,

    handlers
}
```

Now the page becomes

```tsx
export function CardEditorPage(){

    const editor = useCardEditor();

    return (

        <>
            <Toolbar
                save={editor.save}
                validation={editor.validation}
            />

            <CardForm
                card={editor.card}
            />

            <Preview
                visualization={editor.visualization}
            />
        </>

    );

}
```

This is almost entirely declarative.

---

# Result

Approximate file sizes after refactoring:

| File                    |     Current |        Target |
| ----------------------- | ----------: | ------------: |
| CardEditorPage.tsx      | ~1858 lines | 350–500 lines |
| useCardEditor.ts        |         New | 250–350 lines |
| useCardSave.ts          |         New | 120–180 lines |
| useChartCache.ts        |         New |  80–120 lines |
| visualizationBuilder.ts |         New | 200–300 lines |
| reconciliation.ts       |         New | 200–300 lines |
| cardValidation.ts       |         New | 100–150 lines |
| dirtyCheck.ts           |         New |   30–50 lines |
| cardFactory.ts          |         New |  80–120 lines |
| CardRepository.ts       |         New | 100–150 lines |

## Benefits

* **Much easier unit testing:** Each utility and hook can be tested independently without rendering React components.
* **Improved readability:** `CardEditorPage` focuses on composing the UI instead of implementing business logic.
* **Better reusability:** Logic like visualization building or reconciliation can be shared with future editors or import/export features.
* **Safer maintenance:** Changes to visualization rules or save behavior are isolated to a single module.
* **Future extensibility:** Supporting new chart types or storage backends requires localized changes instead of modifying a monolithic component.

I would apply the same refactoring pattern to `QueryBuilderPage` as well. Together, these changes would make the analytics application resemble the architecture used by mature projects like Metabase, Apache Superset, and Grafana, where React components primarily orchestrate UI while business logic resides in hooks, services, and pure utility modules.





=======================RETROFACT QUERY BUILDER ==============================

I think `QueryBuilderPage` is actually **better designed than `CardEditorPage`**, but it still mixes **five different responsibilities** into one file. The same refactoring pattern applies, but with fewer modules.

Current file:

```text
QueryBuilderPage (577 lines)

├── Query Loading
├── Form State
├── Validation
├── Cube Cache
├── Save Logic
├── Navigation
├── Delete
├── Preview
├── Dialogs
└── UI
```

Target:

```text
QueryBuilderPage (200-300 lines)

├── Toolbar
├── Form
├── Preview
└── Hooks

analytics/
    hooks/
        useQueryEditor.ts
        useQuerySave.ts
        useCubeCache.ts

    utils/
        queryValidation.ts
        queryFactory.ts
        cubeSelection.ts
        queryDirtyCheck.ts

    services/
        QueryRepository.ts
```

---

# 1. Extract Query Validation

Current

```ts
validation = useMemo(() => {

    ...

    if (!question.name)

    if (!question.id)

    if (!cube)

    if (!dimension)

    ...

})
```

Move into

```
analytics/utils/queryValidation.ts
```

```ts
export interface QueryValidationResult {

    isValid: boolean;

    errors: string[];

}

export function validateQuery(

    question,

    editing,

    currentId

): QueryValidationResult {

}
```

Then

```ts
const validation = validateQuery(
    question,
    isEditing,
    questionId
);
```

Now testing is simple.

```ts
describe("validateQuery"){

    missing name

    duplicate id

    invalid id

    missing cube

    no measures

    valid query

}
```

---

# 2. Extract Cube Cache

This is already nicely isolated.

Current

```ts
cubeCache.current.set(...)

cubeCache.current.get(...)
```

Move to

```
hooks/useCubeCache.ts
```

```ts
export interface CubeSelection {

    dimensions

    measures

    filters

    visualization

}
```

```ts
export function useCubeCache(){

    const cache = useRef(new Map());

    function save(

        cube,

        selection

    ){

    }

    function load(cube){

    }

    function clear(){

    }

    return {

        save,

        load,

        clear

    }

}
```

Now

```ts
const cubeCache = useCubeCache();
```

instead of

150 lines of caching code.

Testing

```ts
save()

restore()

overwrite()

clear()

multiple cubes
```

---

# 3. Extract Query Factory

Currently

```ts
buildCompleteQuestion()
```

constructs

```ts
{

id,

name,

description,

query,

filters,

visualization

}
```

Move to

```
utils/queryFactory.ts
```

```ts
export function buildQuery(

    question,

    visualization

): Query

```

Now

```ts
const query = buildQuery(
    question,
    visualization
);
```

Unit tests

```ts
builds complete query

preserves filters

preserves visualization

default arrays

```

---

# 4. Extract Save Logic

Current flow

```text
validate

↓

build query

↓

QueryRegistry.register()

↓

navigate
```

Move

```
hooks/useQuerySave.ts
```

```ts
export function useQuerySave(){

    save()

    saveAs()

    saveAndNewChart()

    saveAsAndNewChart()

}
```

Component becomes

```tsx
const {

    save,

    saveAs,

    saveAndNewChart

}
= useQuerySave(...);
```

instead of

100+ lines.

---

# 5. Extract Navigation

Current

```ts
handleCancel()

handleDelete()

navigateAfterSave()

forceNavigate()
```

Move

```
hooks/useQueryNavigation.ts
```

```ts
return {

    cancel,

    afterSave,

    deleteFinished

}
```

Now the component doesn't know routing details.

---

# 6. Extract Dirty Check

Current

```ts
JSON.stringify(question)

!=

JSON.stringify(original)
```

Move

```
utils/queryDirtyCheck.ts
```

```ts
export function isQueryDirty(

    original,

    current

)
```

Tests

```text
new query

edited query

same query

deleted field

added filter
```

---

# 7. Extract Query Repository

Instead of calling

```ts
getQueryRegistry()
```

everywhere

wrap it.

```
services/QueryRepository.ts
```

```ts
load(id)

save(query)

delete(id)

duplicate(id)

exists(id)

```

Later if you replace QueryRegistry with an API,

nothing changes in the UI.

---

# 8. Main Hook

The component owns

```text
question

saving

visualization

dialogs

validation

dirty

cube cache

navigation

loading
```

Move into

```
hooks/useQueryEditor.ts
```

```ts
const editor =
useQueryEditor();
```

returns

```ts
{

question,

validation,

saving,

visualization,

dirty,

save,

saveAs,

delete,

handlers

}
```

---

# 9. Split Toolbar

Current toolbar

```text
Save

Save As

Delete

Cancel

Dropdown

Validation

Unsaved

```

Make

```
components/

QueryEditorToolbar.tsx
```

```tsx
<QueryEditorToolbar

    validation={validation}

    dirty={dirty}

    saving={saving}

    onSave={save}

    onDelete={delete}

/>
```

Around 150 lines disappear.

---

# 10. Split Dialogs

Current

```text
Delete Dialog

Add Dashboard Dialog
```

Move

```
dialogs/

DeleteQueryDialog.tsx

AddToDashboardDialog.tsx
```

---

# Final Architecture

```text
QueryBuilderPage.tsx
│
├── QueryEditorToolbar
├── QueryBuilderForm
├── QueryBuilderPreview
├── DeleteDialog
└── useQueryEditor()

useQueryEditor
│
├── useCubeCache
├── useQuerySave
├── useQueryNavigation
├── validateQuery
├── buildQuery
├── isQueryDirty
└── QueryRepository
```

## Approximate sizes

| File                   | Current |  Target |
| ---------------------- | ------: | ------: |
| QueryBuilderPage.tsx   |     577 | 220–280 |
| useQueryEditor.ts      |     New | 180–250 |
| useQuerySave.ts        |     New | 100–150 |
| useCubeCache.ts        |     New |  80–120 |
| queryValidation.ts     |     New |     100 |
| queryFactory.ts        |     New |      80 |
| QueryRepository.ts     |     New |      80 |
| queryDirtyCheck.ts     |     New |      30 |
| QueryEditorToolbar.tsx |     New |     120 |
| DeleteQueryDialog.tsx  |     New |      60 |

## One architectural improvement

I would also introduce a shared **Editor framework** because `CardEditorPage` and `QueryBuilderPage` solve many of the same problems:

```text
hooks/
    useEditor.ts        // dirty tracking, save state, navigation guard
    useNavigation.ts
    useSaveState.ts

services/
    DashboardRepository.ts
    QueryRepository.ts

utils/
    dirtyCheck.ts
    validation.ts
```

Both editors would then share common infrastructure for:

* dirty-state detection
* save lifecycle (`idle → saving → saved → error`)
* navigation guarding
* confirmation dialogs
* validation patterns

This reduces duplicated code and gives both editors a consistent behavior while keeping their domain-specific logic (queries vs. cards) separate.


=====================QuerySave Hooks=========================


Yes. Below is how I would implement it. I also take the opportunity to make it **generic**, so it contains **no React UI**, only save operations. The hook simply exposes actions that `QueryBuilderPage` can call.

## Folder structure

```text
analytics/
    hooks/
        useQuerySave.ts

    services/
        QueryRepository.ts

    utils/
        queryFactory.ts
```

---

## useQuerySave.ts

```ts
import { useCallback, useState } from "react";

import { Query } from "../queries/types";
import { buildQuery } from "../utils/queryFactory";
import { generateIdFromName } from "../utils/generateId";
import { QueryRepository } from "../services/QueryRepository";

interface UseQuerySaveProps {

    question: Partial<Query>;

    visualization: any;

    returnTo?: string;

    navigateHome: () => void;

    navigateToQuery: (id: string) => void;

    navigateToCardEditor: (questionId: string) => void;

    buildValidation: () => {
        isValid: boolean;
        errors: string[];
    };
}

export function useQuerySave({

    question,

    visualization,

    returnTo,

    navigateHome,

    navigateToQuery,

    navigateToCardEditor,

    buildValidation

}: UseQuerySaveProps) {

    const [saving, setSaving] = useState(false);

    //------------------------------------------------
    // Save
    //------------------------------------------------

    const save = useCallback(async (): Promise<boolean> => {

        const validation = buildValidation();

        if (!validation.isValid)
            return false;

        setSaving(true);

        try {

            const query = buildQuery(
                question,
                visualization
            );

            QueryRepository.save(query);

            navigateHome();

            return true;

        }
        finally {

            setSaving(false);

        }

    }, [
        question,
        visualization,
        buildValidation,
        navigateHome
    ]);

    //------------------------------------------------
    // Save As
    //------------------------------------------------

    const saveAs = useCallback(async (): Promise<string | null> => {

        const validation = buildValidation();

        if (!validation.isValid)
            return null;

        setSaving(true);

        try {

            const query = buildQuery(
                question,
                visualization
            );

            const newName = `${query.name} (Copy)`;

            query.id = generateIdFromName(newName);

            query.name = newName;

            QueryRepository.save(query);

            navigateToQuery(query.id);

            return query.id;

        }
        finally {

            setSaving(false);

        }

    }, [
        question,
        visualization,
        buildValidation,
        navigateToQuery
    ]);

    //------------------------------------------------
    // Save + New Chart
    //------------------------------------------------

    const saveAndNewChart = useCallback(async () => {

        const validation = buildValidation();

        if (!validation.isValid)
            return false;

        setSaving(true);

        try {

            const query = buildQuery(
                question,
                visualization
            );

            QueryRepository.save(query);

            navigateToCardEditor(query.id);

            return true;

        }
        finally {

            setSaving(false);

        }

    }, [
        question,
        visualization,
        buildValidation,
        navigateToCardEditor
    ]);

    //------------------------------------------------
    // Save As + New Chart
    //------------------------------------------------

    const saveAsAndNewChart = useCallback(async () => {

        const validation = buildValidation();

        if (!validation.isValid)
            return false;

        setSaving(true);

        try {

            const query = buildQuery(
                question,
                visualization
            );

            query.name += " (Copy)";
            query.id = generateIdFromName(query.name);

            QueryRepository.save(query);

            navigateToCardEditor(query.id);

            return true;

        }
        finally {

            setSaving(false);

        }

    }, [
        question,
        visualization,
        buildValidation,
        navigateToCardEditor
    ]);

    //------------------------------------------------
    // Delete
    //------------------------------------------------

    const deleteQuery = useCallback((id: string) => {

        QueryRepository.delete(id);

        navigateHome();

    }, [
        navigateHome
    ]);

    //------------------------------------------------

    return {

        saving,

        save,

        saveAs,

        saveAndNewChart,

        saveAsAndNewChart,

        deleteQuery

    };

}
```

---

# I would improve it one step further

I don't like passing six navigation functions into a save hook. Saving shouldn't know anything about routing.

Instead, I'd split it into **three layers**:

```
QueryBuilderPage
        │
        ▼
useQueryEditor()
        │
        ├──────────────┐
        ▼              ▼
useQuerySave()   useQueryNavigation()
        │              │
        └──────┬───────┘
               ▼
        QueryRepository
```

### `useQuerySave()`

Its only job is:

```ts
save()

saveAs()

delete()

duplicate()
```

It returns the saved query ID:

```ts
const id = await save();
```

It **does not navigate**.

### `useQueryNavigation()`

```ts
afterSave(id)

cancel()

afterDelete()

openCardEditor(id)
```

Now `QueryBuilderPage` becomes:

```ts
const save = async () => {

    const id = await querySave.save();

    if (id) {
        navigation.afterSave(id);
    }

};
```

This separation has several advantages:

* **`useQuerySave`** becomes pure business logic and is straightforward to unit test.
* **`useQueryNavigation`** encapsulates all routing decisions.
* The save hook can be reused later in a background autosave, REST API, or CLI tool without pulling in navigation concerns.
* If your routing changes (for example, moving from hash-based routing to React Router), only the navigation hook changes while the save logic remains untouched.

This is the architecture I'd recommend for a production analytics platform because it keeps persistence, navigation, and UI concerns cleanly separated.



========================================QueryRepository=====================================

I like this direction. One thing I would change from my previous proposal is to make **`QueryRepository` a class**, similar to your existing `DashboardStorage`. That keeps the API consistent across your application.

---

# 1. QueryRepository.ts

```typescript
import { getQueryRegistry } from "../queries/QueryRegistry";
import { Query } from "../queries/types";

export class QueryRepository {

    static get(id: string): Query | undefined {
        return getQueryRegistry().get(id);
    }

    static exists(id: string): boolean {
        return this.get(id) !== undefined;
    }

    static save(query: Query): void {
        getQueryRegistry().register(query, {
            overwrite: true,
            userCreated: true
        });
    }

    static saveNew(query: Query): void {
        getQueryRegistry().register(query, {
            overwrite: false,
            userCreated: true
        });
    }

    static delete(id: string): void {
        getQueryRegistry().unregister(id);
    }

    static duplicate(id: string): Query | null {

        const duplicated =
            getQueryRegistry().duplicate(id);

        return duplicated ?? null;

    }

    static list(): Query[] {

        return getQueryRegistry().list();

    }

}
```

---

# 2. queryFactory.ts

This file should **only build a valid Query object**.

```typescript
import {
    Query,
    VisualizationConfig
} from "../queries/types";

export function buildQuery(

    question: Partial<Query>,

    visualization: VisualizationConfig

): Query {

    return {

        id: question.id!,

        name: question.name!,

        description: question.description,

        category: question.category,

        tags: question.tags ?? [],

        query: {

            ...question.query!,

            cube: question.query?.cube ?? "",

            dimensions:
                question.query?.dimensions ?? [],

            measures:
                question.query?.measures ?? [],

            filters:
                question.query?.filters,

            timeDimensions:
                question.query?.timeDimensions,

            order:
                question.query?.order,

            limit:
                question.query?.limit

        },

        filters: question.filters ?? [],

        visualization

    };

}
```

Notice:

* No validation
* No saving
* No navigation

One responsibility only.

---

# 3. useQueryNavigation.ts

This hook wraps every navigation action.

```typescript
import { useCallback } from "react";

import {

    forceNavigateTo,

    navigateHome,

    navigateToDashboard,

    navigateToEditQuery

} from "../navigation/Router";

interface Props {

    returnTo?: string;

}

export function useQueryNavigation({

    returnTo

}: Props) {

    //---------------------------------------

    const cancel = useCallback(() => {

        if (returnTo) {

            navigateToDashboard(returnTo);

        }
        else {

            navigateHome();

        }

    }, [returnTo]);

    //---------------------------------------

    const afterSave = useCallback(() => {

        if (returnTo) {

            forceNavigateTo({

                type: "dashboard",

                dashboardId: returnTo

            });

        }
        else {

            forceNavigateTo({

                type: "home"

            });

        }

    }, [returnTo]);

    //---------------------------------------

    const openQuery = useCallback(

        (questionId: string) => {

            navigateToEditQuery(

                questionId,

                returnTo

            );

        },

        [returnTo]

    );

    //---------------------------------------

    const openCardEditor = useCallback(

        (questionId: string) => {

            forceNavigateTo({

                type: "card-editor",

                fromQuestionId: questionId,

                returnTo

            });

        },

        [returnTo]

    );

    //---------------------------------------

    const afterDelete = useCallback(() => {

        if (returnTo) {

            forceNavigateTo({

                type: "dashboard",

                dashboardId: returnTo

            });

        }
        else {

            forceNavigateTo({

                type: "home"

            });

        }

    }, [returnTo]);

    //---------------------------------------

    return {

        cancel,

        afterSave,

        afterDelete,

        openQuery,

        openCardEditor

    };

}
```

---

# Resulting QueryBuilderPage

The page becomes very small.

```typescript
const navigation =
    useQueryNavigation({
        returnTo
    });

const save =
    useQuerySave({

        question,

        visualization,

        validation

    });
```

Then

```typescript
const handleSave = async () => {

    const id = await save.save();

    if (id) {

        navigation.afterSave();

    }

};
```

And

```typescript
const handleSaveAs = async () => {

    const id = await save.saveAs();

    if (id) {

        navigation.openQuery(id);

    }

};
```

---

## Final architecture

At this point, your QueryBuilder feature follows a clean layered architecture:

```text
UI Layer
---------
QueryBuilderPage
QueryBuilderToolbar
QueryBuilderForm
QueryBuilderPreview

        │

Hooks
------
useQueryEditor
useQuerySave
useQueryNavigation
useCubeCache

        │

Business Logic
--------------
queryFactory
queryValidation
queryDirtyCheck

        │

Persistence
-----------
QueryRepository

        │

Data Source
-----------
QueryRegistry
```

This structure makes each layer responsible for a single concern: UI components render, hooks orchestrate state and workflows, utilities implement pure business rules, and the repository encapsulates persistence. It's also highly testable because each layer can be exercised independently.


