(ns pilchardfriendly.prelude)
(defn flip [function]
  (fn
    ([] (function))
    ([x] (function x))
    ([x y] (function y x))
    ([x y z] (function z y x))
    ([a b c d] (function d c b a))
    ([a b c d & rest]
     (->> rest
          (concat [a b c d])
          reverse
          (apply function)))))