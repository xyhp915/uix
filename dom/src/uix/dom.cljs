(ns uix.dom
  "Public API"
  (:require ["react-dom/client" :as rdom]))

;; react-dom top-level API

(defn create-root
  "Create a React root for the supplied container and return the root.

  See: https://reactjs.org/docs/react-dom-client.html#createroot"
  ([node]
   (rdom/createRoot node))
  ([node {:keys [on-recoverable-error identifier-prefix] :as options}]
   (rdom/createRoot node #js {:onRecoverableError on-recoverable-error
                              :identifierPrefix identifier-prefix})))

(defn hydrate-root
  "Same as `create-root`, but is used to hydrate a container whose HTML contents were rendered by ReactDOMServer.

  See: https://reactjs.org/docs/react-dom-client.html#hydrateroot"
  ([container element]
   (rdom/hydrateRoot container element))
  ([container element {:keys [on-recoverable-error identifier-prefix] :as options}]
   (rdom/hydrateRoot container element #js {:onRecoverableError on-recoverable-error
                                            :identifierPrefix identifier-prefix})))

(defn render-root
  "Renders React root into the DOM node."
  [element root]
  (.render root element))

(defn unmount-root
  "Remove a mounted React root from the DOM and clean up its event handlers and state."
  [root]
  (.unmount root))

(defn flush-sync
  "Force React to flush any updates inside the provided callback synchronously.
  This ensures that the DOM is updated immediately.

  See: https://reactjs.org/docs/react-dom.html#flushsync"
  [callback]
  (rdom/flushSync callback))

(defn batched-updates [f]
  (rdom/unstable_batchedUpdates f))

(defn find-dom-node
  "If this component has been mounted into the DOM, this returns the corresponding native browser DOM element.

  See: https://reactjs.org/docs/react-dom.html#finddomnode"
  [component]
  (rdom/findDOMNode component))

(defn create-portal
  "Creates a portal. Portals provide a way to render children into a DOM node
  that exists outside the hierarchy of the DOM component.

  See: https://reactjs.org/docs/react-dom.html#createportal"
  [child node]
  (rdom/createPortal child node))
