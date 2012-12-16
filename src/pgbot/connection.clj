(ns pgbot.connection
  "Create and handle IRC connections. The IRC connection map is the base
  object of our program."
  (:require (clojure [string :as string])
            (clojure.java [io :as io]))
  (:import java.net.Socket))

(defn create
  "Take several parameters and return a map containing information about
   the irc connection."
  [& {:keys [host port nick]}]
  (let [socket (Socket. host (Integer. port))]
    {:socket socket
     :in (io/reader socket)
     :out (io/writer socket)
     :nick nick}))

(defn send-message [connection & strings]
  (binding [*out* (:out connection)]
    (println (string/join " " strings))))

(defn register [connection nick & [password]]
  (send-message connection "USER" nick "i *" nick)
  (send-message connection "NICK" nick)
  (when password
    (send-message connection "PRIVMSG nickserv :identify" nick password)))

(defn join-channel
  "Join a specified channel."
  [connection channel]
  (send-message connection "JOIN" channel))

(defn connect [& {:keys [host port nick password channel]}]
  (future
    (let [connection (create :host host :port port :nick nick)]
      (register connection nick password)
      (join-channel connection channel)
      (while true
        (let [next-line (binding [*in* (:in connection)] read-line)]
          (println next-line))))))
