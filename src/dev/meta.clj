(ns dev.meta
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [dev.debug :refer [spy]]))

(defonce ^:private -in-repl (atom [false]))
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn set-repl! [v]
  (swap!  -in-repl (constantly v)))
(defn in-repl? [] (deref  -in-repl))

(defonce ^:private -hooks (atom {}))

(defn register-hook 
  ([hook] (register-hook *ns* hook))
  ([n hook] (swap! -hooks #(assoc %1 n hook))))

(defn dev-meta
  ([]
   (dev-meta *ns* {}))
  ([arg]
   (cond
     (map? arg) (dev-meta *ns* arg)
     (symbol? arg) (dev-meta arg {})
     :else (dev-meta *ns* {})))
  ([ns & args]
   (when (in-repl?)
     (spy args)
     (spy @-hooks)
     (fmap #(apply %1 (into [ns] args)) @-hooks))))

(def annotate dev-meta)


