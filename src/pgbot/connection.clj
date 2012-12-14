(ns pgbot.connection
  "Create and handle IRC connections. The IRC connection map is the base
  object of our program."
  (:require (clojure [string :as string])
            (clojure.java [io :as io]))
  (:import java.net.Socket))

(defn create
  "Given a host and a port, generate an IRC connection map, containing
   pairs for the socket, the reader, and the writer."
  [host port]
  (let [socket (Socket. host (Integer. port))]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)}))

(defn send-message [connection & strings]
  (let [message (string/join " " strings)]
    (binding [*out* (:out connection)]
      (println message))))

(defn register [connection nickname & [password]]
  (send-message connection "NICK" nickname)
  (send-message connection "USER" nickname " i * " nickname)
  (when password
    (send-message connection "PRIVMSG nickserv :identify" nickname password)))

(defn join-channel
  "Join a specified channel."
  [connection channel]
  (send-message connection "JOIN" channel))

(defn connect [& {:keys [host port nick nick-password channel]}]
  (let [connection (create host port)]
    (future
      (register connection nick nick-password)
      (join-channel connection channel)
      (loop [lines (line-seq (connection :in))]
        (if-let [line (first lines)]
          (do
            (when (re-find #"PING" line)
              (send-message connection "PONG"))
            (println line)
            (recur (rest lines)))
          (.close (connection :socket)))))))
