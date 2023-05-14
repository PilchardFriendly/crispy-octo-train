(ns gol.debug 
  (:require [taoensso.timbre :as timbre]))

(defmacro spy "Wrapper around timbre/spy" [& rest]
  `(timbre/spy ~@rest))