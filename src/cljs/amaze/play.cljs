(ns amaze.play(:require [re-frame.core :as re-frame]
                        [re-pressed.core :as rp]
                        [goog.events.KeyCodes :as kc]
                        [amaze.canvas :refer [draw-circle]]
                        [amaze.config :refer [db directions get-db-value]]
                        [amaze.maze :refer [valid-move?]]))

(enable-console-print!)

(def bread-crumbs (atom #{}))

(defn render-ball
  [color r]
  (let [[x y] @(:ball-position db)
        thickness (get-db-value :thickness)
        breadth (get-db-value :breadth)
        px (+ (* x (+ breadth thickness)) thickness 1)
        py (+ (* y (+ breadth thickness)) thickness 1)]
    (draw-circle px py r color)))

(defn play-maze []
  (let [breadth (get-db-value :breadth)
        r (dec (/ breadth 2))]
    (reset! bread-crumbs #{})
    (render-ball :red r)))

(re-frame/reg-event-db ::initialize-db
  (fn [_ _] db))

(re-frame/reg-event-db ::set-ball-position
  (fn [db [_ dir]]
    (when (valid-move? @(:ball-position db) dir)
      (let [cell (mapv + @(:ball-position db) (dir directions))
            r (dec (/ (get-db-value :breadth) 2))]
        (render-ball ::white r)
        (cond (contains? @bread-crumbs @(:ball-position db)) (swap! bread-crumbs disj @(:ball-position db))
              (contains? @bread-crumbs cell) (swap! bread-crumbs disj cell)
              :else (do (render-ball ::grey (dec r))
                        (swap! bread-crumbs conj @(:ball-position db))))
        (reset! (:ball-position db) cell)
        (if (= cell @(:exit db))
          (render-ball ::green r)
          (render-ball ::red r)))
      db)))

(defn dispatch-keydown-rules []
  (re-frame/dispatch
   [::rp/set-keydown-rules
    {:event-keys [[[::set-ball-position :west]  [{:keyCode kc/A}]] [[::set-ball-position :west]  [{:keyCode kc/J}]]
                  [[::set-ball-position :east]  [{:keyCode kc/D}]] [[::set-ball-position :east]  [{:keyCode kc/L}]]
                  [[::set-ball-position :north] [{:keyCode kc/W}]] [[::set-ball-position :north] [{:keyCode kc/I}]]
                  [[::set-ball-position :south] [{:keyCode kc/S}]] [[::set-ball-position :south] [{:keyCode kc/K}]]]
     :clear-keys [[{:keyCode kc/ESC}]]}]))
