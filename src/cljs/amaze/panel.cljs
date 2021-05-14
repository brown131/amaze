(ns amaze.panel
  (:require [re-com.core :refer [at box button gap h-box input-text label single-dropdown title v-box]]
            [amaze.canvas :refer [calc-canvas-size clear-canvas draw-rect draw-circle print-maze]]
            [amaze.config :refer [directions maze-algorithms db get-db-value]]
            [amaze.maze :refer [maze-cells init-maze generate-maze]]))

(enable-console-print!)

(defn render-canvas []
  (let [[canvas-width canvas-height] (calc-canvas-size)]
    [box :width (str canvas-width "px") :height (str canvas-height "px")
     :child [:canvas {:id "canvas" :width (str canvas-width) :height (str canvas-height)
                      :style {:background-color :black}}]]))

(defn render-cell "Display a cell at the specified coordinates.  
                   An opening is rendered in the specified direction."
  [x y dir]
  (let [thickness (get-db-value :thickness)
        breadth (get-db-value :breadth)
        [dx dy] (dir directions)
        px (+ (* x breadth) (* thickness (+ x 1 (Math/min dx 0))))
        py (+ (* y breadth) (* thickness (+ y 1 (Math/min dy 0))))
        cw (+ breadth (* thickness (Math/abs dx)))
        ch (+ breadth (* thickness (Math/abs dy)))]
    (draw-rect px py cw ch :white)))

(defn render-ball
  [color]
  (let [[x y] @(:ball-position db)
        thickness (get-db-value :thickness)
        breadth (get-db-value :breadth)
        px (+ (* x (+ breadth thickness)) thickness 1)
        py (+ (* y (+ breadth thickness)) thickness 1)
        r (dec (/ breadth 2))]
    (draw-circle px py r color)))

(defn play-maze [] (render-ball :red))

(defn render-exit []
  (let [x (get-db-value :width)
        y (rand-int (get-db-value :height))
        thickness (get-db-value :thickness)
        breadth (get-db-value :breadth)
        px (* x (+ breadth thickness))
        py (+ (* y (+ breadth thickness)) thickness)]
    (draw-rect px py thickness breadth :white)))

(defn render-maze []
  (clear-canvas)
  (init-maze)
  (generate-maze)
  (run! (fn [[[x y] d]] (render-cell x y d)) @maze-cells)
  (render-exit))

(defn main-panel []
  [v-box :src (at) :class "container" :children
   [[box :align :center :child [title :src (at) :label "AMAZE!" :level :level1
                                :style {:font-family ["Courier New", "monospace"]
                                        :font-weight :bold :color :orange}]]
    [h-box :children
     [[v-box :size "1" :children
       [[label :label "Maze Width"]
        [input-text :width "90px" :model (:width db)
         :on-change #(reset! (:width db) %)]]]
      [v-box :size "1" :children
       [[label :label "Maze Height"]
        [input-text :width "90px" :model (:height db)
         :on-change #(reset! (:height db) %)]]]
      [v-box :size "1" :children
       [[label :label "Wall Thickness"]
        [input-text :width "90px" :model (:thickness db)
         :on-change #(reset! (:thickness db) %)]]]
      [v-box :size "1" :children
       [[label :label "Hall Breadth"]
        [input-text :width "90px" :model (:breadth db)
         :on-change #(reset! (:breadth db) %)]]]
      [v-box :size "5" :children
       [[label :label "Algorithm"]
        [single-dropdown :choices maze-algorithms :model (:algorithm db)
         :width "250px" :max-height "300px" :on-change  #(reset! (:algorithm db) %)]]]]]
    [:p]
    [h-box :children
     [[box :child [button :label "Generate" :on-click render-maze :class "btn-primary"]]
      [gap :size "20px"]
      [box :child [button :label "Print" :on-click print-maze :class "btn-info"]]
      [gap :size "20px"]
      [box :child [button :label "Play" :on-click play-maze :class "btn-info"]]]]
    [:p]
    [render-canvas]]])
