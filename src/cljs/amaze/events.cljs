(ns amaze.events (:require [re-frame.core :as re-frame]
                           [re-pressed.core :as rp]
                           [goog.events.KeyCodes :as kc]
                           [amaze.config :refer [db directions]]
                           [amaze.maze :refer [valid-move?]]
                           [amaze.panel :refer [render-ball]]))

(enable-console-print!)

(re-frame/reg-event-db ::initialize-db
  (fn [_ _] db))

;; TODO Add breadcrumbs.
;; TODO Win game!
(re-frame/reg-event-db ::set-ball-position
  (fn [db [_ dir]]
    (when (valid-move? @(:ball-position db) dir)
      (render-ball :white)
      (let [[dx dy] (dir directions)
            [x y] @(:ball-position db)]
        (reset! (:ball-position db) [(+ x dx) (+ y dy)])
        (render-ball :red))
      db)))

(defn dispatch-keydown-rules []
  (re-frame/dispatch
   [::rp/set-keydown-rules
    {:event-keys [[[::set-ball-position :west]  [{:keyCode kc/A}]] [[::set-ball-position :west]  [{:keyCode kc/J}]]
                  [[::set-ball-position :east]  [{:keyCode kc/D}]] [[::set-ball-position :east]  [{:keyCode kc/L}]]
                  [[::set-ball-position :north] [{:keyCode kc/W}]] [[::set-ball-position :north] [{:keyCode kc/I}]]
                  [[::set-ball-position :south] [{:keyCode kc/S}]] [[::set-ball-position :south] [{:keyCode kc/K}]]]
     :clear-keys [[{:keyCode kc/ESC}]]}]))
