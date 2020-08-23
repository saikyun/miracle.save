(ns miracle.save.cljs)

(defmacro save
  "Used to save all local bindings, takes an identifier as a parameter.
  The identifier is used with `ld` in order to load the local bindings where `save` was called."
  [key]
  `(do (swap! miracle.save.cljs/saves
              update
              ~key
              #(into []
                     (take-last 
                      *max-saves*
                      (conj %
                            [(gen-id)
                             (into {} (list ~@(let [ks (keys (:locals &env))]
                                                (for [k ks]
                                                  `['~k ~k]))))]))))
       :ok))

(defmacro save-do
  "Used to save all local bindings, takes an identifier as a parameter.
  The identifier is used with `ld` in order to load the local bindings where `save` was called."
  [key & body]
  `(let [~'_ (swap! miracle.save.cljs/saves
                    update
                    ~key
                    #(into []
                           (take-last 
                            *max-saves*
                            (conj %
                                  [(gen-id)
                                   (into {} (list ~@(let [ks (keys (:locals &env))]
                                                      (for [k ks]
                                                        `['~k ~k]))))]))))]
     ~@body))
