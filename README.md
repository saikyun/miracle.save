[![Clojars Project](https://img.shields.io/clojars/v/miracle-save.svg)](https://clojars.org/miracle-save)
[![cljdoc badge](https://cljdoc.org/badge/miracle-save/miracle-save)](https://cljdoc.org/d/miracle-save/miracle-save/0.0.1/doc/readme)

# miracle.save
Tiny clojure lib for saving/loading local bindings, useful for debugging.

# `save` and `ld`
Use `save` to store the bindings at the place `save` was called. Use `ld` to load those bindings (i.e. `def` them).

## Example usage
```clojure
(defn add [x y] (save :a) (+ x y))
(add 5 10)
(ld :a)
x ;; 5
y ;; 10
  
(add 20 30)
(add 7 13)
  
(print-saves :a)
```

# `save-var*`

Use `save-var*` to automatically capture arguments and return values of 
a function or `save-ns*` to do so for all functions in a namespace.
Access the values via `print-saves` or `@f-saves`.

## Example usage
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
   :sha     "3d54fe9904fd324b82acb2692d46d227505f3891"}}}
```

## Similar libraries

* [scope-capture](https://github.com/vvvvalvalval/scope-capture) - capture the arguments passed to a wrapped expression and recreate them as global `def`s or a `let`. Also breakpoint/continue functionality.


## License

MIT License

Copyright (c) 2019 Jona Ekenberg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
