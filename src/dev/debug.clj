(ns dev.debug 
  (:require [taoensso.timbre :as timbre]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro spy "Wrapper around timbre/spy" [& rest]
  `(timbre/spy ~@rest))