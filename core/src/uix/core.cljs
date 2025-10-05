(ns uix.core
  "Public API"
  (:refer-clojure :exclude [use])
  (:require-macros [uix.core])
  (:require [goog.object :as gobj]
            [react]
            [uix.hooks.alpha :as hooks]
            [uix.compiler.aot]
            [uix.lib :refer [doseq-loop map->js]]
            [cljs-bean.core :as bean]))

(def props-assert-fn (atom (fn [& args] true)))

(def ^:dynamic *current-component*)

;; React's top-level API

(def ^:private built-in-static-method-names
  [:childContextTypes :contextTypes :contextType
   :getDerivedStateFromProps :getDerivedStateFromError])

(defn create-class
  "Creates class based React component"
  [{:keys [constructor getInitialState render
           ;; lifecycle methods
           componentDidMount componentDidUpdate componentDidCatch
           shouldComponentUpdate getSnapshotBeforeUpdate componentWillUnmount
           ;; static methods
           childContextTypes contextTypes contextType
           getDerivedStateFromProps getDerivedStateFromError
           ;; class properties
           defaultProps displayName]
    :as fields}]
  (let [methods (map->js (apply dissoc fields :displayName :getInitialState :constructor :render
                                built-in-static-method-names))
        static-methods (map->js (select-keys fields built-in-static-method-names))
        ctor (fn [props]
               (this-as this
                        (.apply react/Component this (js-arguments))
                        (when constructor
                          (constructor this props))
                        (when getInitialState
                          (set! (.-state this) (getInitialState this)))
                        this))]
    (gobj/extend (.-prototype ctor) (.-prototype react/Component) methods)
    (when render (set! (.-render ^js (.-prototype ctor)) render))
    (gobj/extend ctor react/Component static-methods)
    (when displayName
      (set! (.-displayName ctor) displayName)
      (set! (.-cljs$lang$ctorStr ctor) displayName)
      (set! (.-cljs$lang$ctorPrWriter ctor)
            (fn [this writer opt]
              (-write writer displayName))))
    (set! (.-cljs$lang$type ctor) true)
    (set! (.. ctor -prototype -constructor) ctor)
    (set! (.-uix-component? ctor) true)
    ctor))

(defn create-ref
  "Creates React's ref type object."
  []
  (react/createRef))

(defn glue-args [^js props]
  (cond-> (.-argv props)
    (.-children props) (assoc :children (.-children props))))

(defn- memo-compare-args [a b]
  (= (glue-args a) (glue-args b)))

(defn memo
  "Takes component `f` and optional comparator function `should-update?`
  that takes previous and next props of the component.
  Returns memoized `f`.

  When `should-update?` is not provided uses default comparator
  that compares props with clojure.core/="
  ([f]
   (memo f memo-compare-args))
  ([^js f should-update?]
   (let [fm (react/memo f should-update?)]
     (when (.-uix-component? f)
       (set! (.-uix-component? fm) true))
     fm)))

(defn use-state
  "Takes initial value or a function that computes it and returns a stateful value,
  and a function to update it.

  See: https://react.dev/reference/react/useState"
  [value]
  (hooks/use-state value))

(defn use-reducer
  "An alternative to `use-state`. Accepts a reducer of type (state, action) => new-state,
  and returns the current state paired with a dispatch method.

  See: https://react.dev/reference/react/useReducer"
  ([f value]
   (hooks/use-reducer f value))
  ([f value init-state]
   (hooks/use-reducer f value init-state)))

(defn use-ref
  "Takes optional initial value and returns React's ref hook wrapped in atom-like type."
  ([]
   (use-ref nil))
  ([value]
   (let [ref (hooks/use-ref nil)]
     (when (nil? (.-current ref))
       (set! (.-current ref)
             (specify! #js {:current value}
                       IDeref
                       (-deref [this]
                               (.-current this))

                       IReset
                       (-reset! [this v]
                                (set! (.-current ^js this) v))

                       ISwap
                       (-swap!
                        ([this f]
                         (set! (.-current ^js this) (f (.-current ^js this))))
                        ([this f a]
                         (set! (.-current ^js this) (f (.-current ^js this) a)))
                        ([this f a b]
                         (set! (.-current ^js this) (f (.-current ^js this) a b)))
                        ([this f a b xs]
                         (set! (.-current ^js this) (apply f (.-current ^js this) a b xs)))))))
     (.-current ref))))

(defn create-context
  "Creates React Context with an optional default value"
  ([]
   (react/createContext))
  ([default-value]
   (react/createContext default-value)))

(defn use-context
  "Takes React context and returns its current value"
  [context]
  (hooks/use-context context))

(defn use-debug
  ([v]
   (hooks/use-debug v))
  ([v fmt]
   (hooks/use-debug v fmt)))

(defn use-deferred-value
  "Accepts a value and returns a new copy of the value that will defer to more urgent updates.
  If the current render is the result of an urgent update, like user input,
  React will return the previous value and then render the new value after the urgent render has completed.

  See: https://react.dev/reference/react/useDeferredValue"
  ([v]
   (hooks/use-deferred-value v))
  ([v initial]
   (hooks/use-deferred-value v initial)))

(defn use-transition
  "Returns a stateful value for the pending state of the transition, and a function to start it.

  See: https://react.dev/reference/react/useTransition"
  []
  (hooks/use-transition))

(defn start-transition
  "Marks updates in `f` as transitions
  See: https://react.dev/reference/react/startTransition"
  [f]
  (react/startTransition f))

(defn use-id
  "Returns unique ID that is stable across the server and client, while avoiding hydration mismatches.

  See: https://react.dev/reference/react/useId"
  []
  (hooks/use-id))

(def use-effect-event
  "EXPERIMENTAL: Creates a stable event handler from a function, allowing it to be used in use-effect
   without adding the function as a dependency.
  See: https://react.dev/learn/separating-events-from-effects"
  (if (exists? react/useEffectEvent)
    react/useEffectEvent
    (fn [f]
      (let [ref (use-ref nil)]
        (reset! ref f)
        (uix.core/use-callback (fn [& args] (apply @ref args)) [])))))

(defn use-sync-external-store
  "For reading and subscribing from external data sources in a way that’s compatible
  with concurrent rendering features like selective hydration and time slicing.

  subscribe: function to register a callback that is called whenever the store changes
  get-snapshot: function that returns the current value of the store
  get-server-snapshot: function that returns the snapshot used during server rendering

  See: https://react.dev/reference/react/useSyncExternalStore"
  ([subscribe get-snapshot]
   (hooks/use-sync-external-store subscribe get-snapshot))
  ([subscribe get-snapshot get-server-snapshot]
   (hooks/use-sync-external-store subscribe get-snapshot get-server-snapshot)))

(defn use-optimistic
  "Lets you show a different state while an async action is underway

  state: current state value
  update-fn: (current-state, optimistic-value) => optimistic-state

  Returns a tuple of [optimistic-state, add-optimistic!]
  optimistic-state: resulting optimistic state
  add-optimistic!: takes optimistic-value and calls update-fn

  See: https://react.dev/reference/react/useOptimistic"
  [state update-fn]
  (hooks/use-optimistic state update-fn))

(defn use-action-state
  "Allows you to update state based on the result of a form action

  f: the function to be called when the form is submitted or button pressed
  state: the value you want the state to be initially
  permalink: a string containing the unique page URL that this form modifies

  Returns a triplet of [state, form-action, pending?]
  state: current form state
  form-action: a new action that you can pass as the `action` prop to your form component or `form-action` prop to any button component within the form
  pending?: a boolean indicating whether the form is currently pending

  See: https://react.dev/reference/react/useActionState"
  ([f state]
   (hooks/use-action-state f state))
  ([f state permalink]
   (hooks/use-action-state f state permalink)))

(defn use
  "Lets you read the value of a resource like a Promise or context.

  See: https://react.dev/reference/react/use"
  [resource]
  (hooks/use resource))

(defn use-atom
  "Takes an atom, subscribes UI component to changes in the atom
  and returns its current value"
  [ref]
  (let [subscribe (hooks/use-batched-subscribe ref)
        get-snapshot (hooks/use-callback #(-deref ref) #js [ref])]
    (use-sync-external-store subscribe get-snapshot)))

(defn as-react
  "Interop with React components. Takes a function that returns UIx component
  and returns same component wrapped into interop layer."
  [f]
  #(f (bean/bean %)))

(defn- lazy-shadow-reloadable
  "Special case for traditional hot-reloading via shadow-cljs,
  when UI tree is rendered from the root after evert hot-reload"
  [f loadable]
  (let [lazy-component (react/lazy #(.then (f) (fn [_] #js {:default (fn [props]
                                                                       (uix.core/$ @loadable (glue-args props)))})))]
    (set! (.-uix-component? lazy-component) true)
    lazy-component))

(defn lazy
  "Like React.lazy, but supposed to be used with UIx components"
  ([f]
   (let [lazy-component (react/lazy #(.then (f) (fn [component] #js {:default component})))]
     (set! (.-uix-component? lazy-component) true)
     lazy-component))
  ([f loadable]
   (lazy-shadow-reloadable f loadable)))

(defn create-error-boundary
  "Creates React's error boundary component

  display-name       — the name of the component to be displayed in stack trace
  derive-error-state — maps error object to component's state that is used in render-fn
  did-catch          — 2 arg function for side-effects, logging etc.
  receives the exception and additional component info as args
  render-fn          — takes state value returned from error->state and a vector
  of arguments passed into error boundary"
  [{:keys [display-name derive-error-state did-catch]
    :or   {display-name (str (gensym "uix.error-boundary"))}}
   render-fn]
  (let [constructor  (fn [^js/React.Component this _]
                       (set! (.-state this) #js {:argv nil}))
        derive-state (fn [error]
                       #js {:argv (derive-error-state error)})
        render       (fn []
                       (this-as ^react/Component this
                                (let [props     (.-props this)
                                      state     (.-state this)
                                      set-state (fn [new-value]
                                                  (.setState this #js {:argv new-value}))]
                                  (render-fn [(.-argv state) set-state]
                                             (glue-args props)))))
        class        (create-class {:constructor              constructor
                                    :displayName              display-name
                                    :getDerivedStateFromError derive-state
                                    :componentDidCatch        did-catch
                                    :render                   render})]
    (set! (.-uix-component? class) true)
    class))

(defn forward-ref
  "Like React's `forwardRef`, but should be used only for UIx components
  when passing them into React components that inject a ref"
  [component]
  (let [ref-comp
        (react/forwardRef
         (fn [props ref]
           (let [argv (cond-> (.-argv props)
                        (.-children props) (assoc :children (.-children props))
                        :always (assoc :ref ref))
                 argv (merge argv
                             (-> (bean/bean props)
                                 (dissoc [:argv :children])))]
             (uix.core/$ component argv))))]
    (set! (.-uix-component? ref-comp) true)
    ref-comp))

(defn clone-element [^js element props & children]
  (let [type (.-type element)
        okey (.-key element)
        oref (.-ref element)
        update-children #(when %
                           (map (fn [^js el]
                                  (when (some-> el .-_store)
                                    (set! (.. el -_store -validated) true))
                                  el)
                                %))
        children (update-children children)
        props (update props :children update-children)]
    (if (or (string? type) (not (.-uix-component? type)))
      (let [oprops (.-props element)
            nel (uix.core/$ type (cond-> props (seq children) (assoc :children (into-array children))))
            nprops (js/Object.assign #js {}  oprops #js {:key okey :ref oref} (.-props nel))]
        (uix.core/$ type nprops))
      (let [oprops (.. element -props -argv)]
        (uix.core/$ type
          (cond-> oprops
                  okey (assoc :key okey)
                  :always (merge props)
                  (seq children) (assoc :children children)))))))

(def suspense react/Suspense)
(def activity react/Activity)
(def strict-mode react/StrictMode)
(def profiler react/Profiler)

;; SSR helpers
(def client? (exists? js/document)) ;; cljs can run in a browser or Node.js
(def server? (not client?))

(defn ^{:jsdoc ["@nosideeffects"]} set-display-name [f name]
  (set! (.-displayName f) name)
  (js/Object.defineProperty f "name" #js {:value name}))