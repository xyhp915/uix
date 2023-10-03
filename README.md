<img src="logo.png" width="125" />

_Idiomatic ClojureScript interface to modern React.js_

> "Oh god, I just started learning reagent. Don’t tell me I’ll have to switch" /r/clojure

- API compatibility: React v18.2.0
- UIx v1 is in [roman01la/uix](https://github.com/roman01la/uix) repo
- Discuss at #uix on [Clojurians Slack](http://clojurians.net)
- [A slide deck explaining UIx and migration path from Reagent](https://pitch.com/public/821ed924-6fe6-4ce7-9d75-a63f1ee3c61f)
- Try it out live in [ClojureScript Studio](https://www.clojurescript.studio/)

[![CircleCI](https://circleci.com/gh/pitch-io/uix.svg?style=svg)](https://circleci.com/gh/pitch-io/uix)
[![Clojars Project](https://img.shields.io/clojars/v/com.pitch/uix.core.svg)](https://clojars.org/com.pitch/uix.core)
[![Clojars Project](https://img.shields.io/clojars/v/com.pitch/uix.dom.svg)](https://clojars.org/com.pitch/uix.dom)

## Installation

```
yarn add react@18.2.0 react-dom@18.2.0
```

```clj
{:deps {com.pitch/uix.core {:mvn/version "0.10.0"}
        com.pitch/uix.dom {:mvn/version "0.10.0"}}}
```

### How to start a new project with UIx

- Run `npx create-uix-app my-app` to scaffold a new project
- Clone starter template manually from [pitch-io/uix-starter](https://github.com/pitch-io/uix-starter)

## Usage

```clj
(ns my.app
  (:require [uix.core :refer [defui $]]
            [uix.dom]))

(defui button [{:keys [on-click children]}]
  ($ :button.btn {:on-click on-click}
    children))

(defui app []
  (let [[state set-state!] (uix.core/use-state 0)]
    ($ :<>
      ($ button {:on-click #(set-state! dec)} "-")
      ($ :span state)
      ($ button {:on-click #(set-state! inc)} "+"))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))

(uix.dom/render-root ($ app) root)
```

## Docs

- [What is UIx?](https://pitch-io.github.io/uix/docs/what-is-uix.html)
- [Components](https://pitch-io.github.io/uix/docs/components.html)
- [Elements](https://pitch-io.github.io/uix/docs/elements.html)
- [Hooks](https://pitch-io.github.io/uix/docs/hooks.html)
- [State](https://pitch-io.github.io/uix/docs/state.html)
- [Effects](https://pitch-io.github.io/uix/docs/effects.html)
- [Interop with React](https://pitch-io.github.io/uix/docs/interop-with-react.html)
- [Interop with Reagent](https://pitch-io.github.io/uix/docs/interop-with-reagent.html)
- [Code-splitting and React.lazy](https://pitch-io.github.io/uix/docs/code-splitting.html)
- [Migrating from Reagent](https://pitch-io.github.io/uix/docs/migrating-from-reagent.html)
- [Server-side rendering](https://pitch-io.github.io/uix/docs/server-side-rendering.html)
- [Hot reloading](https://pitch-io.github.io/uix/docs/hot-reloading.html)
- [React DevTools](https://pitch-io.github.io/uix/docs/react-devtools.html)
- [Code linting](https://pitch-io.github.io/uix/docs/code-linting.html)
- [Utilities](https://pitch-io.github.io/uix/docs/utilities.html)
- [Examples](/core/dev/uix/examples.cljs)
- [Internals](https://pitch-io.github.io/uix/docs/internals.html)
- Other render targets
  - [React Native](/docs/react-native.md)
  - [React Three Fiber](/docs/react-three-fiber.md)

## Testing

```
scripts/test
```

_Note: to ensure you're using the right Node.js version, you can use [nvm](https://github.com/nvm-sh/nvm) and run `nvm use`
once in the directory. Otherwise the Node.js version you use is in the `.nvmrc` file. See nvm repo for more documentation._

## Publishing

1. Update version in core/release.edn, dom/release.edn and in README.md
2. Update docs if needed
3. Update CHANGELOG.md
4. Publish both `core` and `dom` packages to Clojars

```
cd core && CLOJARS_PASSWORD={YOUR_CLOJARS_TOKEN} clj -A:release --skip-tag
cd dom && CLOJARS_PASSWORD={YOUR_CLOJARS_TOKEN} clj -A:release --skip-tag
```

## Who's using UIx2?

- [Pitch](https://pitch.com/)
- [Cognician](https://info.cognician.com/)
- [Multiply](https://multiply.co/)
- [ClojureScript Studio](https://www.clojurescript.studio/)

## Thanks to

- [UIx v1](https://github.com/roman01la/uix) for initial set of ideas and learnings
- [Helix](https://github.com/lilactown/helix) for even more ideas
- [Pitch](https://github.com/pitch-io) for sponsoring and dogfooding the work
