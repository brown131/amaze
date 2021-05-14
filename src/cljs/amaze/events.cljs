(ns amaze.events (:require [re-frame.core :as re-frame]
                           [re-pressed.core :as rp]
                           [goog.events.KeyCodes :as kc]
                           [amaze.config :refer [db directions]]
                           [amaze.panel :refer [render-ball]]))

(enable-console-print!)

(re-frame/reg-event-db ::initialize-db
  (fn [_ _] db))

(re-frame/reg-event-db ::set-ball-position
  (fn [db [_ value]]
    (println "value" value "pos" @(:ball-position db))
    (let [[dx dy] (value directions)
          [x y] @(:ball-position db)]
      (render-ball :white)
      (reset! (:ball-position db) [(+ x dx) (+ y dy)])
      (render-ball :red))
    db))

(defn dispatch-keydown-rules []
  (re-frame/dispatch
   [::rp/set-keydown-rules
    {:event-keys [[[::set-ball-position :west] [{:keyCode kc/A}]]
                  [[::set-ball-position :east] [{:keyCode kc/S}]]
                  [[::set-ball-position :north] [{:keyCode kc/W}]]
                  [[::set-ball-position :south] [{:keyCode kc/Z}]]]
     :clear-keys [[{:keyCode kc/ESC}]]}]))
