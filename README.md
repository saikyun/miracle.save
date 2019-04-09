# miracle.save
Tiny clojure lib for saving/loading local bindings, useful for debugging.

Example of usage:
```
(defn add [x y] (save :a) (+ x y))
(add 5 10)
(ld :a)
x ;;=> 5
y ;;=> 10

(add 20 30)
(add 7 13)
 
(print-saves :a)
```

Try it out with:

```edn
{:deps
 {github-saikyun/miracle.save
  {:git/url "https://github.com/saikyun/miracle.save"
   :sha     "e49fa76e6a5c34f936729b94c800727559d06da9"}}}
```
