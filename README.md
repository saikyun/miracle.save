# miracle.save
Tiny clojure lib for saving/loading local bindings, useful for debugging.

Example of usage:
```clojure
(use 'miracle.save)

(defn add [x y] (save :a) (+ x y))
(add 5 10)
(ld :a)
x ;;=> 5
y ;;=> 10

(add 20 30)
(add 7 13)
 
(print-saves :a)
;; Entry no. 2
;; {x 5, y 10}
;;
;; Entry no. 1
;; {x 20, y 30}
;;
;; Entry no. 0
;; {x 7, y 13}
```

Example of usage on functions:
```clojure
(use 'miracle.save)

(defn yo ([x] x) ([x {:keys [a b]}] (+ x a b)) ([x y & rest] (apply + x y rest)))

(save-var* 'yo)

(yo 5)
(yo 5 {:a 20 :b 30})
(yo 5 30 40)
(yo 5 30 40 50)
@f-saves
;; =>
;; {#'user/yo
;;  ({:args {x 5, y 30, rest (40 50)},
;;    :spec-args ([x 5] [y 30] [rest (40 50)]),
;;    :ret 125}
;;   {:args {x 5, y 30, rest (40)},
;;    :spec-args ([x 5] [y 30] [rest (40)]),
;;    :ret 75}
;;   {:args {x 5, a 20, b 30},
;;    :spec-args ([x 5] [:arg-1 {:a 20, :b 30}]),
;;    :ret 55}
;;   {:args {x 5}, :spec-args ([x 5]), :ret 5})}
```

Try it out with:

```edn
{:deps
 {github-saikyun/miracle.save
  {:git/url "https://github.com/saikyun/miracle.save"
   :sha     "e49fa76e6a5c34f936729b94c800727559d06da9"}}}
```
