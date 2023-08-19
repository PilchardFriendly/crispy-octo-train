(ns gol.generator 
  (:require [dev.meta :as dev]))

(defn generator
  "returns a fn that 
   when called with no arg concurrently increments init by delta.
   When called with one arg, resets the state to the argument."
  [init delta]
  (let [state (atom init)]
    (fn
      ([] (let [current @state]
            (swap! state delta)
            current))
      ([new-init] (reset! state new-init)))))

(dev/annotate {:dev.test/testable-id 'gol.generator-test})