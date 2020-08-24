(ns miracle.clj.save
  (:require [clojure.pprint]))

(def ^:dynamic *max-saves* "The maximum number of saves per id." 100)

(defn inspect-map [map-to-print & {:keys [desired-level safe-count]
                                   :or {desired-level 4 safe-count 10}}]
  (binding [*print-level* desired-level *print-length* safe-count]
    (clojure.pprint/pprint map-to-print)))

(defn gensym? [s]       
  (re-find #"__\d+" s))

(defonce saves (atom {}))

(defn clear-saves! [] (reset! saves {}))

(defn new*
  "Creates a new save at `id`, with `bindings` containing the local bindings from where `save` was called."
  ([ref id]
   (new* id {}))
  ([ref id bindings]
   (swap! ref update id
          (fn [saves]
            (-> (conj saves bindings)
                (#(if (< (count %) *max-saves*)
                    (into '() (reverse (take *max-saves* %)))
                    %)))))))

(defn save-fn
  ([ref bindings id]
   (let [bindings `(into {} (remove #(gensym? (name (key %))) ~bindings))]
     `(new* ~ref ~id ~bindings))))

(defmacro save
  "Used to save all local bindings, takes an identifier as a parameter.
  The identifier is used with `ld` in order to load the local bindings where `save` was called."
  [& args]
  (apply save-fn
         'miracle.clj.save/saves
         (into {} (for [k (keys &env)]
                    [`'~k k]))
         args))

(defn ld
  "Loads the local bindings that have been saved using `save` with `id` as parameter."
  ([id] (ld id 0))
  ([id pos]
   (let [locals (nth (get @saves id) pos)]
     (when locals
       (println "Defining:")
       (inspect-map locals))
     (doseq [[sym val] locals]
       (try
         (eval `(def ~(symbol sym) '~val))
         (catch Exception e (prn sym val) (throw e)))))))

(defn print-saves
  [id]
  (let [locals (take 10 (get @saves id))]
    (doseq [i (reverse (range (count locals)))]
      (println "Entry no." i)
      (inspect-map (first (drop i locals)))
      (prn))))


;; ============== Saving function input and output ====================


(defonce f-saves (atom {}))

(defn which-args
  "Takes a list of arg-names, e.g. ([x] [x y] [x y {:keys [a b]}])
  and a number.
  Returns the arg-names that matches n-args."
  [arg-names-list n-args]
  (first
   (filter 
    some?
    (for [arg-names arg-names-list]
      (if (and (some #{'&} arg-names)
               (>= n-args (count (take-while #(not= % '&) arg-names))))
        arg-names
        (when (= n-args (count arg-names))
          arg-names))))))

(defmacro get-env
  "Returns the locally defined variables and their values."
  []
  (into {} (for [k (keys &env)]
             [`'~k k])))  

(defn destructure-bindings
  [arg-names values]
  `(let [~(which-args arg-names (count values)) '~values]
     (get-env)))

(defn gen-spec-args
  [arg-lists args]
  (let [arg-list (which-args arg-lists (count args))
        [before-and _] (split-with #(not= % '&) arg-list)
        nof-before-and (count before-and)]
    (concat
     (->> (map-indexed
           #(if (coll? %2) (keyword (str "arg-" %1)) %2)
           before-and)
          (#(map vector % args)))
     (when (< nof-before-and (count args))
       [['rest (drop (count before-and) args)]]))))

(defn save-wrapper
  "f-var should be a var or symbol holding a IFn, and f the actual function.
  Returns a function that will save all args and return values when being called.
  The data will be stored in `f-saves`."
  [f-var f args]
  (let [arg-lists (some-> (meta f-var) :arglists)
        ret (apply f args)
        
        args-to-save
        (into {}
              (if arg-lists
                (->> (eval (destructure-bindings arg-lists args))
                     (remove #(gensym? (name (key %)))))
                (->> (range (count args))                           ; Generate default arg-names
                     (map #(keyword (str "arg-" (str %))))
                     (#(map vector % args)))))
        
        spec-args  ;; argument specifically for generating specs
        (if arg-lists
          (gen-spec-args arg-lists args)
          (->> (range (count args))                                ; Generate default arg-names
               (map #(keyword (str "arg-" (str %))))
               (#(map vector % args))))]
    (new* f-saves
          f-var
          {:args args-to-save
           :spec-args spec-args
           :ret ret})
    ret))

(defn save-var*
  "Applies `save-wrapper` to a var, which means that whenever that var is called,
  it will save all args and return values of that function.
  Check `save-wrapper` for more information."
  ([ns s]
   (save-var* (ns-resolve ns s)))
  ([v]
   (let [^clojure.lang.Var v (if (var? v) v (resolve v))
         ns (.ns v)
         s  (.sym v)]
     (if (and (ifn? @v) (-> v meta :macro not) (-> v meta ::saved not))
       (let [f @v
             vname (symbol (str ns "/" s))]
         (doto v
           (alter-var-root #(fn [& args] (save-wrapper v % args)))
           (alter-meta! assoc ::saved f)))))))

(defn unsave-var*
  "The opposite of `save-var*`, restores the var to its original state."
  ([ns s]
   (unsave-var* (ns-resolve ns s)))
  ([v]
   (let [^clojure.lang.Var v (if (var? v) v (resolve v))
         ns (.ns v)
         s  (.sym v)
         f  ((meta v) ::saved)]
     (when f
       (doto v
         (alter-var-root (constantly ((meta v) ::saved)))
         (alter-meta! dissoc ::saved))))))

(defn save-ns*
  "Applies `save-var*` to all function vars in a namespace."
  [ns]
  (let [ns (the-ns ns)]
    (when-not ('#{clojure.core miracle.clj.save} (.getName ns))
      (let [ns-fns (->> ns ns-interns vals (filter (comp fn? var-get)))]
        (doseq [f ns-fns]
          (save-var* f))))))

(defn unsave-ns*
  "Applies `unsave-var*` to all function vars in a namespace."
  [ns]
  (let [ns-fns (->> ns the-ns ns-interns vals)]
    (doseq [f ns-fns]
      (unsave-var* f))))





(comment
  ;; Example usage of basic `save`
  (defn add [x y] (save :a) (+ x y))
  (add 5 10)
  (ld :a)
  x ;; 5
  y ;; 10
  
  (add 20 30)
  (add 7 13)
  
  (print-saves :a))



(comment
  ;; Example usage of `save-var*`
  (do
    (defn yo ([x] x) ([x {:keys [a b]}] (+ x a b)) ([x y & rest] (apply + x y rest)))
    (def wat (partial yo 5))  
    (save-var* 'yo)  
    (save-var* 'wat)  
    (reset! f-saves {})
    (yo 5)
    (wat 5 10)
    (yo 5 {:a 20 :b 30})
    (yo 5 30 40)
    (yo 5 30 40 50)
    @f-saves)

  (unsave-var* 'yo))
