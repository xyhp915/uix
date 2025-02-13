(ns uix.compiler.hoist
  (:import (com.google.javascript.jscomp CodePrinter$Builder CompilerInput CompilerOptions CompilerPass
                                         CustomPassExecutionTime JSChunk NodeTraversal NodeTraversal$Callback ShadowAccess)
           (com.google.javascript.rhino IR Node Token)))

(def prim-token
  #{Token/STRINGLIT Token/NUMBER Token/TRUE Token/FALSE Token/NULL})

(def object-tokens
  #{Token/ARRAYLIT Token/OBJECTLIT Token/STRING_KEY})

(defn- node-literal? [^Node n]
  (let [token (.getToken n)]
    (cond
      (contains? prim-token token) true
      (contains? object-tokens token) (or (not (.hasChildren n))
                                          (loop [n (.getFirstChild n)]
                                            (if (node-literal? n)
                                              (if-let [next (.getNext n)]
                                                (recur next)
                                                true)
                                              false)))
      (and (.isName n) (.startsWith (.getString n) "cljs$cst$uix_hoisted_")) true
      :else false)))

(defn- print-node [^Node n]
  (.build (CodePrinter$Builder. n)))

(defn create-object-ast [kvs]
  (let [obj-lit (Node. Token/OBJECTLIT)]
    (doseq [[k v] kvs
            :let [k (IR/stringKey k)]]
      (.addChildToFront k v)
      (.addChildToFront obj-lit k))
    obj-lit))

(defn create-vnode [^Node n]
  (let [tag (.getChildAtIndex n 1)
        attrs-children (.getChildAtIndex n 2)
        attrs (some-> (.getFirstChild attrs-children) .cloneTree)
        child (some-> (.getSecondChild attrs-children) .cloneTree)
        children (some-> (.getChildAtIndex n 3) .cloneTree)

        symbol-for-call (doto (Node. Token/CALL)
                          (.addChildToFront (IR/getprop (IR/name "Symbol") "for"))
                          ;; should be "react.element" for React <19
                          (.addChildToBack (IR/string "react.transitional.element")))
        key-prop (loop [n (.getFirstChild attrs)]
                   (if (and (.isStringKey n)
                            (= "key" (.getString n)))
                     (do
                       (.detach n)
                       (.getFirstChild n))
                     (when-let [n (.getNext n)]
                       (recur n))))]
    (.detach tag)
    (when key-prop
      (.detach key-prop))
    (when (.hasParent n) (.detach n))
    (when children
      (when child
        (.addChildrenToFront children child))
      (when (.hasChildren children)
        (let [k (IR/stringKey "children")]
          (.addChildToFront k children)
          (.addChildToFront attrs k))))
    (create-object-ast {"$$typeof" symbol-for-call
                        "type" tag
                        "props" attrs
                        "ref" (IR/nullNode)
                        "key" (or key-prop (IR/nullNode))
                        "_owner" (IR/nullNode)})))

(defrecord ConstantRef [var-name ^Node node used-id])

(defn replace-uix-constants [^com.google.javascript.jscomp.Compiler cc]
  (let [constants (atom {})
        this (proxy [CompilerPass NodeTraversal$Callback] []
               (process [externs node]
                 (assert (empty? @constants) "can only run once")
                 (doseq [^CompilerInput input (ShadowAccess/getInputsInOrder cc)
                         :when (.contains (.getName input) ".clj")]
                   (NodeTraversal/traverse cc (.getAstRoot input cc) this))
                 (doseq [^ConstantRef ref (vals @constants)
                         :let [^JSChunk target-module (if (== 1 (count @(:used-id ref)))
                                                        (first @(:used-id ref))
                                                        (-> (ShadowAccess/getChunkGraph cc)
                                                            (.getDeepestCommonDependencyInclusive @(:used-id ref))))
                               target (reduce
                                        (fn [target ^CompilerInput input]
                                          (if (.startsWith (.getName input) "shadow/cljs/constants/")
                                            (reduced (.getScriptNode cc (.getName input)))
                                            target))
                                        (.getScriptNode cc "cljs/core.cljs")
                                        (.getInputs target-module))
                               var-node (IR/var (IR/name (:var-name ref)) (create-vnode (:node ref)))]]
                   (.addChildToBack target var-node)
                   (ShadowAccess/reportChangeToEnclosingScope cc target)))

               (shouldTraverse [t n parent]
                 true)
               (visit [^NodeTraversal t ^Node n parent]
                 (when (and (.isCall n)
                            (= "uix.compiler.aot._GT_el" (.getQualifiedName (.getFirstChild n))))
                   (let [tag (.getChildAtIndex n 1)
                         attrs-children (.getChildAtIndex n 2)
                         children (.getChildAtIndex n 3)]
                     (when (and (.isString tag)
                                (node-literal? attrs-children)
                                (node-literal? children))
                       (let [hash (hash (print-node n))
                             ref (or (@constants hash)
                                     (let [var-name (str "cljs$cst$uix_hoisted_" (count @constants))
                                           ref (ConstantRef. var-name n (atom #{}))]
                                       (swap! constants assoc hash ref)
                                       ref))]
                         (swap! (:used-id ref) conj (.getChunk t))
                         (.replaceWith n (IR/name (:var-name ref)))
                         (ShadowAccess/reportChangeToEnclosingScope cc parent)))))))]
    this))

(defn hoisting-pass [cc ^CompilerOptions closure-opts state]
  (.addCustomPass closure-opts CustomPassExecutionTime/BEFORE_CHECKS
    (replace-uix-constants cc)))

(defonce __add-uix-hoisting-pass
  (alter-var-root #'shadow.build.closure/setup
    (fn [f]
      (fn [state]
        (let [state (f state)
              cc (:shadow.build.closure/compiler state)
              ^CompilerOptions closure-opts (:shadow.build.closure/compiler-options state)]
          (hoisting-pass cc closure-opts state)
          state)))))
