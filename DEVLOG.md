# UIx Devlog

## December, 2024

### React 19

Since 1.3.0 UIx is compatible with React 19, the change mainly wraps and exposes new public APIs in `react` and `react-dom` packages.

### Reagent interop

Updated `uix.re-frame/use-reaction` hook to supprt Reagent's `Cursor` and `Track` types, [669d58](https://github.com/pitch-io/uix/commit/669d588fc9b14b07ae3720130c794bf8b91d3a27).

### Unused components removal

Now Google Closure will remove unused UIx components, [8462d7](https://github.com/pitch-io/uix/commit/8462d79e45ea550af79dad2385e15de2f540af2b).

This one is interesting. It turned out that a single side effecting operation prevented unused components to be removed, namely this line `(js/Object.defineProperty component "name" #js {:value name})`. The code adds readbale name to component's function, which is then used in stack traces and React DevTools.

Instead of removing this line I've dug into Google Closure and found that there's JSDoc annotation that tells the compiler to treat a piece of code as if it doesn't perform side effects: `@nosideeffects`. A string of JSDoc can be added to a function in ClojureScript using `:jsdoc` meta: `^{:jsdoc ["@nosideeffects"]}`.

## November, 2024

Hey yo! November was quite productive, let's have a look.

### `use-effect-event` hook

_v1.3.0_

This is a missing piece from React, actually they do have it under unstable flag, but it makes life so much easier in certain cases that I've decided to add it to UIx now. The hook was previously named `useEvent`, now it's `useEffectEvent` which sounds more appropriate given the context where it should be used. We adopted userland implementation of the hook at Pitch and it worked great. I highly recommend to [read the docs on `useEffectEvent`](https://react.dev/learn/separating-events-from-effects) to understand what it is doing and when you should use it.

### Renderable React Context

_v1.3.0_

To render React Context in UIx you have to use JS interop to render the `Provider` component:

```clojure
(def ctx (uix/create-context))

($ (.-Provider ctx) {:value color-theme}
  ...)
```

This is not great, I thought it would be nice to treat the context itself as a renderable element. Turned out this is [exactly what React v19 is gonna do](https://react.dev/blog/2024/04/25/react-19#context-as-a-provider), so I went ahead and implemented renderable context in UIx, which means you can render context regardless of what React version you are using.

```clojure
(def ctx (uix/create-context))

($ ctx {:value color-theme}
  ...)
```

### Fixed props conversion in React Context

_v1.3.0_

Normally, UIx shallowly converts Clojure's map of props into JS object when `$` is used to create React element from a third-party React component written in JavaScript.

React's Context provider is a JavaScript/React component, which triggers the same conversion logic in UIx. While this works fine because props conversion is shallow:

```clojure
($ ctx {:value {:bg "#000" :text "#fafafa"}}
  ...)
```

When passing Clojure primitive that dont exist in JS, such as keywords or symbols, the value will be transformed into a string:

```clojure
($ ctx {:value :hello}
  ...)

(uix/use-context ctx) ;; -> "hello"
```

This is fixed now and will be included in UIx v1.3.0

### Readable stack traces

_v1.2.0_

Clojure's identifier syntax is not valid JS syntax, which is why names like `uix.core/create-context` are compiled to `uix$core$create_context`. Those compiled names will pop out everywhere in JavaScript runtime: compiled source, stack traces, debugger window and React component stack traces. The last one is printed to the console when an exception is thrown during render phase.

To make your life a little bit easier, UIx components will be printed with their original names now. You can find an example in the PR [#171](https://github.com/pitch-io/uix/pull/171).

### Compiler: forms rewriter

_v1.2.0_

UIx compiler includes code rewriter now. It's not a user facing change, just something that the compiler is doing internally to speedup rendering, when possible.

For now the compiler only includes `for` rewriter that spits out `map` for a simplest form of `for`:

```clojure
;; source
(defui component []
  (for [x (range 10)]
    ($ :div {:key x} x)))

;; after rewriter
(defui component []
  (map (fn [x]
         ($ :div {:key x} x))
       (range 10)))
```

I've started with `for` rewriter specifically, because I use it alsmost exclusively for rendering lists, but at the same time `for` macro spits out a huge amount of code that potentially has a negative impact on bundle size, startup and runtime performance.

### Props validation at compile time

_v1.2.0_

In React it's common to use [`PropTypes`](https://legacy.reactjs.org/docs/typechecking-with-proptypes.html) for runtime validation or [TypeScript](https://www.typescriptlang.org/docs/handbook/jsx.html#function-component) for static type checking. Clojure being dynamic language, doesn't give us anything like static type checking, but it doesn't mean that at least some props validation is not possible.

In this experiment you can opt into props validation at compile time to further improve developer experience and make sure that no code is shipped with missing props.

To enable this feature you have to use clojure.spec (this might change in the future to allow pluggable spec libraries) and switch from `:pre` to `:props` assertion in UIx components.

```clojure
(s/def :prop/on-click fn?)
(s/def ::button (s/keys :req-un [:prop/on-click]))

(defui button
  [{:keys [children on-click] :as props}]
  {:props [::button]}
  ($ :button {:on-click on-click}
    children))
```

Given such usage of the `button` component UIx will emit compiler warning telling you that required key `:on-click` is missing from props map passed into the element.

```clojure
($ button {} "press me")
```

Why is this useful? Similar to other UIx linters, props linter enables validation during local development, in tests and on CI. Which reduces probability of shipping buggy UI.

#### Limitations

Of course compile time props checking is limited by dynamic nature of the language. There are some requirements to the code that enables this linter:

- There's no validation for values in props map, only for existence of specified keys (this might change in the future, since it's possible to have partial checking for contents of props map)
- Only required keys (`:req` and `:req-un`) are checked
- The check kicks-in only for props written as map literal

### Preparing for React 19

Since it sounds like [v19 is gonna land soonish](https://bsky.app/profile/react.dev/post/3lawuio3hac2u), I picked up the work on adding new APIs, which includes:

- `use-action-state` hook, allows you to update state based on the result of a form action ([read React docs](https://react.dev/reference/react/useActionState))
- `use-optimistic` hook to update UI optimistically ([read React docs](https://react.dev/reference/react/useOptimistic))
- `React.use` function, reads the value of a resource like a Promise or context ([read React docs](https://react.dev/reference/react/use))
- Resource preloading APIs: `prefetch-dns`, `preconnect`, `preload`, `preload-module`, `preinit` and `preinit-module` ([read React docs](https://react.dev/reference/react-dom#resource-preloading-apis))

Check out the PR ([#144](https://github.com/pitch-io/uix/pull/144)) and make sure to read about [all updates coming to React in v19](https://react.dev/blog/2024/04/25/react-19).

### Experiments

These two are likely additions to future version of UIx.

#### rest and spread syntax for props map [#169](https://github.com/pitch-io/uix/pull/169)

_v1.3.0_

One thing that is sometimes useful in React/JavaScript, but doesn't exist in Clojure is object _spread_ and _rest_ syntax for Clojure maps (see [object spread in JS](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Spread_syntax)). It's often used for props transferring, to extract a subset of props and pass the rest to underlying components.

```javascript
function Button({ style, ...props }) {
  return (
    <div style={style}>
      <MaterialButton {...props} />
    </div>
  );
}
```

In Clojure you'd have to `dissoc` keys manually, which is verbose and can be frustrating for folks coming from JavaScript.

```clojure
(defui button [{:keys [style] :as props}]
  ($ :div {:style style}
    ($ MaterialButton
      (merge {:theme "light"}
             (dissoc props :style)))))
```

For this specific reason I'm exploring syntatic sugar in `defui` and `$` macros to support the pattern. [Helix supports props spreading with a similar syntax](https://github.com/lilactown/helix/blob/master/docs/creating-elements.md#dynamic-props).

```clojure
(defui button [{:keys [style] props :&}]
  ($ :div {:style style}
    ($ MaterialButton {:theme "light" :& props})))
```

##### Props rest syntax

When destructing props in `uix.core/defui` or `uix.core/fn` all keys that are not mentioned in destructing form will be stored in a map assigned to `:&` keyword. The syntax is composable with all other means of destructuring maps in Clojure, with one exception: `:&` exists only at top level, it won't work for nested maps.

##### Props spread syntax

To spread or splice a map into props use `:&` key. This also works only at top level of the map literal and only a single spread is allowed: `{:width 100 :& props1 :& props 2}` is not valid Clojure map because of duplicate keys.

#### Streaming SSR with Suspense on JVM [#187](https://github.com/pitch-io/uix/pull/187)

UIx can already serialise components to HTML string on JVM, either for static website rendering or server-side rendering with client hydration. Both buffered and streamed rendering is supported.

The advantage of rendering on JVM, over JS runtimes, is that data fetching in UI components on server can be done in one pass, while rendering, due to blocking IO. While simple, the problem with this approach is that it creates a waterfall of blocking IO, which essentially blocks delivery of HTML string to a client until entire UI tree is serialised.

This problem is now solved in React with [Suspense](https://react.dev/reference/react-dom/server/renderToPipeableStream#streaming-more-content-as-it-loads) and a new rendering API in `react-dom/server` package — [`renderToPipeableStream`](https://react.dev/reference/react-dom/server/renderToPipeableStream). Streaming SSR with Suspense sends entire HTML document to the client immediately, excluding "suspended" subtrees of UI, and then streams chunks of HTML rendered within Suspense boundaries, when their data dependencies are resolved. When arriving on the client, JS runtime inserts those chunks of HTML into specified locations in the document.

This is essentially the behaviour I replicated in JVM renderer in UIx. Current implemementation is almost there. The IO in components remains blocking from user perspective, but under the hood UIx executes "suspended" UI concurrently, in core.async `go` blocks.

Checkout the PR ([#187](https://github.com/pitch-io/uix/pull/187))

## October, 2024

Over the last couple of years using UIx, I've collected a list of things I'd like to improve further in the library, and now this October when I left my full-time job, I was finally able to pick up the work.

There are two major annoyances that I wanted to fix for a long time.

### React.memo

Functional components in React can be optimised with memoisation using `React.memo` API, however this requires writing more code which feels cumbersome:

```clojure
(defui component [props] ...)

(def component-memo (uix/memo component))
```

Also now I have to come up with a name for memoised component, since it's a seprate var. I never was in a situation where I wanted both normal and memoised components, so it was obvious to me that the problem should be fixed at the level of `defui` macro.

In UIx v.1.2.0 you can tag `defui` with `:memo` meta, this will emit React component already wrapped with `React.memo`.

```clojure
(defui ^:memo component [props] ...)
```

### Clojure's equality check in hooks deps

Memoising components with `React.memo` is easy because it takes comparator function as the second argument, which means we can use `clojure.core/=` to compare props. Unfortunately this is not an option with React Hooks. In React hooks with dependencies array, like `useEffect`, `useCallback` and others use `Object.is` under the hood, which is the same as `===` in JS, and there's no way to redefine that. This means that supplying non `identical?` Clojure values to deps array, like immutable map or a vector, will re-run the hook. I observed that in most cases deps values are primitive types, which means that the problem is not relevant, but still sometimes a hook might depend on immutable compound value and there's no way to avoid it.

I'm glad that this problem is finally fixed and since 1.2.0 all React hooks in UIx will compare deps with `clojure.core/=`.

### swap-like `set-state`

Not a major improvement, but I really like the signature of `clojure.core/swap!` function. `set-state` returned from `useState` hook take updater function as well, but doesn't support supplying additional arguments. Now UIx wraps state updater to support that.

```clojure
(let [[state set-state] (uix/use-state {:n 0})]
  ($ :button {:on-click #(set-state update :n inc)}
    "+"))
```

### Minor improvements

For convenience `use-subscribe` hook was moved from [docs](https://github.com/pitch-io/uix/blob/master/docs/interop-with-reagent.md#syncing-with-ratoms-and-re-frame) into `uix.re-frame` namespace. If you are using UIx with re-frame, you don't have to keep track of updates in docs anymore.

For [hot-reloading setup with react-refresh](https://github.com/pitch-io/uix/blob/master/docs/hot-reloading.md) I have preload namespace that I carry between projects. Now there's `uix.preload` that you can put in `:preloads` config to enable hot-reloading with react-refresh.

React API exposes [`cloneElement` function](https://react.dev/reference/react/cloneElement) which is mainly used in UI libraries to decorate elements with additional props. Now there's `uix/clone-element` function for UIx components.

Fixed two issues in JVM server renderer: [#151](https://github.com/pitch-io/uix/issues/151), [#152](https://github.com/pitch-io/uix/issues/152)

### Other

#### CSS-in-CLJS

Based on my experience from [previous attempts](https://github.com/clj-commons/cljss) at creating CSS-in-CLJS library that I can be happy with, I put together [uix.css](https://github.com/roman01la/uix.css)

The library is similar to UIx in how it is making use of compile time optimisations. Dynamic values are inserted via CSS Variables API at runtime. Checkout [documenetation](https://github.com/roman01la/uix.css) to learn more about the library.

```clojure
(ns my.app
  (:require [uix.core :as uix :refer [defui $]]
            [uix.css :refer [css]]
            [uix.css.adapter.uix]))

(defn button []
  ($ :button {:style (css {:font-size "14px"
                           :background "#151e2c"})}))
```

#### Open sourcing ClojureScript Studio

Some time ago I built ClojureScript Studio, coding environment similar to CodeSandbox. Now it is open sourced, and I think it can serve as a reference for folks interested in learning how UIx looks in mid-sized projects. And you can also learn how to build in-browser dev environment that runs on bootstrapped ClojureScript.

Checkout the project on [GitHub](https://github.com/roman01la/clojurescript-studio).
