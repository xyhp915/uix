# UIx Devlog

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
