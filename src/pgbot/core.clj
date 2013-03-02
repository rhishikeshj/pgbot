(ns pgbot.core
  "A simple IRC bot."
  (:require overtone.at-at))

(def ^:private plugins
  #{'pgbot.plugin.help})

(doseq [p plugins]
  (require `~p))

(def ^:private events
  "Returns an agent containing a map of event keywords to sets of action
   functions."
  {:incoming #{'log
               'ping-pong}
   :outgoing #{'log
               'send-message}})

(defn trigger-event
  "Triggers the specified event, passing in the connection map and data
   to the event's action functions."
  [connection event & data]
  (doseq [f (events event)]
    (apply f connection data)))

(defn- log
  "Log a string to a preferred output."
  [_ s]
  (spit "/tmp/pgbot.log" (str (java.util.Date.) " : " s "\n") :append true))

(defn- create-connection
  "Opens a connection to a server. Returns a map containing information
   about the connection."
  [host port nick channel]
  (let [socket (java.net.Socket. host (Integer. port))]
    {:socket socket
     :in (clojure.java.io/reader socket)
     :out (clojure.java.io/writer socket)
     :nick nick
     :channel channel}))

(defn- parse-message
  "Takes a message string and returns a map of the message properties."
  [message]
  (let [[_ prefix type destination content]
        (re-matches #"^(?:[:](\S+) )?(\S+)(?: (?!:)(.+?))?(?: [:](.+))?$"
                    message)]
    {:prefix prefix
     :type type
     :destination destination
     :content content}))

(defn- send-message
  "Sends a message through a connection's writer. This takes multiple
   string arguments and will join them with spaces in-between, or, if
   message is sequential, it prints out each string in the list."
  [connection message & messages]
  (binding [*out* (connection :out)]
    (if (sequential? message)
      (doseq [m message]
        (println m))
      (let [m (clojure.string/join " " (cons message messages))]
        (println m)))))

(defn- register-connection
  "Sends a 'handshake' message to register the connection."
  [connection]
  (let [nick (connection :nick)]
    (trigger-event connection
                   :outgoing
                   (str "NICK " nick)
                   (str "USER " nick " i * " nick))))

(defn- read-line-from-connection
  "Reads a single line from the connection. Returns nil if the socket is
   closed."
  [connection]
  (try (.readLine (connection :in))
    (catch java.net.SocketException _ nil)))

(defn- ping-pong
  "Triggers an outgoing event with a PONG string if the incoming message
   is a PING."
  [connection line]
  (when-let [[_ server] (re-matches #"^PING :(.*)" line)]
    (trigger-event connection :outgoing (str "PONG :" server))))

(def ^:private thread-pool
  "Returns the app's thread pool for interval-based code execution."
  (overtone.at-at/mk-pool))

(defn connect
  "Entry point for operating the bot. This creates a connection, does
   the dance to ensure that it stays open, and begins listening for
   messages in a new thread. It returns the connection map."
  [host port nick channel]
  (let [connection (create-connection host port nick channel)]
    (future
      (register-connection connection)
      (send-message connection "JOIN" channel)
      (overtone.at-at/every
        10000
        #(doseq [p plugins]
           (when-let [message ((ns-resolve p 'run) connection)]
             (send-message connection message)))
        thread-pool
        :initial-delay 30000)
      (loop [line (read-line-from-connection connection)]
        (when line
          (trigger-event connection :incoming line)
          (recur (read-line-from-connection connection)))))
    connection))
