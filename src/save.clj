(ns save)

(def ^:dynamic *max-saves* "The maximum number of saves per id." 100)

(defn gensym? [s]       
  (re-find #"__\d+" s))

;; Contains the saved local bindings of various vars and identifiers.
(defonce saves (atom {}))

(defn clear-saves! [] (reset! saves {}))

(defn new*
  "Creates a new save at `id`, with `bindings` containing the local bindings from where `save` was called."
  ([id]
   (new* id {}))
  ([id bindings]
   (swap! saves update id
          (fn [saves]
            (-> (conj saves bindings)
                (#(if (< (count %) *max-saves*)
                    (into '() (reverse (take *max-saves* %)))
                    %)))))))

(defn save-fn
  ([bindings id]
   (let [bindings `(into {} (remove #(gensym? (name (key %))) ~bindings))]
     `(do (new* ~id ~bindings)
          (println (first (get @saves ~id)))))))

(defmacro get-env
  []
  (into {} (for [k (keys &env)]
             [`'~k k])))

(defmacro save
  "Used to save all local bindings, takes an identifier as a parameter.
  The identifier is used with `ld` in order to load the local bindings where `save` was called."
  [& args]
  (apply save-fn
         (into {} (for [k (keys &env)]
                    [`'~k k]))
         args))

(defn ld
  "Loads the local bindings that have been saved using `save` with `id` as parameter."
  [id]
  (let [locals (first (get @saves id))]
    (when locals (println "Defining" (keys locals)))
    (doseq [[sym val] locals]
      (try
        (eval `(def ~(symbol sym) '~val))
        (catch Exception e (prn sym val) (throw e))))))

(defn inspect-map [map-to-print & {:keys [desired-level safe-count]
                                   :or {desired-level 4 safe-count 10}}]
  (binding [*print-level* desired-level *print-length* safe-count]
    (clojure.pprint/pprint map-to-print)))

(defn print-saves
  [id]
  (let [locals (take 10 (get @saves id))]
    (doseq [i (reverse (range (count locals)))]
      (println "Entry no." i)
      (inspect-map (first (drop i locals)))
      (prn))))

(comment
  ;; Example usage
  (defn add [x y] (save :a) (+ x y))
  (add 5 10)
  (ld :a)
  x ;; 5
  y ;; 10
  
  (add 20 30)
  (add 7 13)
  
  (print-saves :a))
