(ns miracle.cljs.save
  (:require [clojure.pprint]))

(defonce id (atom 0))

(defn gen-id [] (swap! id inc))

(def ^:dynamic *max-saves* "The maximum number of saves per id." 10)

(defn inspect-map
  [map-to-print &
   {:keys [desired-level safe-count], :or {desired-level 4, safe-count 10}}]
  (binding [*print-level* desired-level
            *print-length* safe-count]
    (clojure.pprint/pprint map-to-print)))

(defn gensym? [s] (re-find #"__\d+" s))

(defonce saves (atom {}))

(defn eval-in-context
  "Evals the form with the local bindings that have been saved using `save` with `id` as parameter."
  ([form id]
   (eval-in-context form
                    id
                    (-> @miracle.cljs.save/saves
                        (get id)
                        last
                        first)))
  ([form id pos]
   (let [locals (second (first (filter #(= (first %) pos)
                                       (-> @miracle.cljs.save/saves
                                           (get id)))))
         ks (keys locals)]
     `(let [~'all (second (first (filter #(= (first %) ~pos)
                                         (-> @miracle.cljs.save/saves
                                             (get ~id)))))
            ~@(apply concat (for [k ks] [k `(get ~'all '~k)]))]
        ~form))))

(defn clear-saves! [] (reset! saves {}))

(defn new*
  "Creates a new save at `id`, with `bindings` containing the local bindings from where `save` was called."
  ([ref id] (new* id {}))
  ([ref id bindings]
   (swap! ref update
          id
          (fn [saves]
            (-> (conj saves bindings)
                (#(if (< (count %) *max-saves*)
                    (into '() (reverse (take *max-saves* %)))
                    %)))))))

(defn save-fn
  ([ref bindings id]
   (let [bindings `(into {} (remove #(gensym? (name (key %))) ~bindings))]
     `(new* ~ref ~id ~bindings))))

(defn get-last
  [id]
  (let [res (-> @saves
                (get id)
                last)]
    [(first res) (with-out-str (inspect-map (second res)))]))

(defn get-last-nof
  [id nof]
  (for [res (reverse (take nof
                           (-> @saves
                               (get id))))]
    [(first res)
     (pr-str (subs (with-out-str (inspect-map (second res))) 0 250))]))

(defn print-saves
  [id]
  (let [locals (take 10 (get @saves id))]
    (doseq [i (reverse (range (count locals)))
            :let [[k v] (first (drop i locals))]]
      (println "Entry id." k)
      (inspect-map v)
      (prn))))
