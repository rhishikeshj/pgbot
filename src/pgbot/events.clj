(ns pgbot.events)

(defn register
  "Takes a list of event keywords and a list of functions and assigns
   the functions to be called for each event. It returns the connection
   with an updated events map.

   (register [:incoming :outgoing] [#(println \"messages\")])"
  [connection events functions]
  (assoc connection :events
         (reduce (fn [event-map [e f]]
                   (assoc event-map e
                          (conj (event-map e #{}) f)))
                 (connection :events)
                 (for [e events f functions] [e f]))))

(defn trigger
  "Triggers the specified event, calling as a side effect each assigned
   function. The connection map and the data is passed to each
   function."
  [connection event & data]
  (doseq [f (event (connection :events))]
    (apply f connection (flatten data))))
