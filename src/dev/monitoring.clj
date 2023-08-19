(ns dev.monitoring 
  (:require [malli.dev :as dev]
            [malli.core :as m] 
            [malli.instrument :as mi]))

(defn instrument [] (comment dev/start!))
(defn unstrument [] (dev/stop!))
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn restrument [] (unstrument) (instrument))

(defmacro =>
  [& body]
  `(m/=> ~@body))

;; (trace/trace-ns malli.core)
(defn check 
  ([] (prn (mi/check)))
  ([options] (prn (mi/check options))))