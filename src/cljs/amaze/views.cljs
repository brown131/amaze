(ns amaze.views
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as cl :refer [fresh is run run-db]]
            [clojure.core.logic.pldb :refer [db db-rel with-db db-fact with-dbs empty-db]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :refer [at box button gap h-box input-text label title v-box]]
            [monet.canvas :as canvas]
            [amaze.config :as config]
            [amaze.subs :as subs]))

(enable-console-print!)

(def maze-state {:width (reagent/atom "20")
                 :height (reagent/atom "20")
                 :thickness (reagent/atom "10")
                 :breadth (reagent/atom "15")})

(db-rel direction)
(db-rel maze)

(def directions (db [direction :north [0 -1]]
                    [direction :south [0 1]]
                    [direction :east [1 0]]
                    [direction :west [-1 0]]))

(def maze-cells (atom empty-db))

(defn render-cell "Display a cell at the specified coordinates."
  [x y dir]
  (let [thickness (js/parseInt @(:thickness maze-state))
        breadth (js/parseInt @(:breadth maze-state))
        [dx dy] (first (with-db directions (run 1 [q] (direction dir q))))
        px (- (+ (* x (+ breadth thickness)) (* (Math/min dx 0) thickness)) breadth)
        py (- (+ (* y (+ breadth thickness)) (* (Math/min dy 0) thickness)) breadth)
        cw (+ breadth (* thickness (Math/abs dx)))
        ch (+ breadth (* thickness (Math/abs dy)))]
    (canvas/add-entity @config/monet-canvas
                       :background (canvas/entity {:x px :y py :w cw :h ch} nil
                                                  (fn [ctx val] (-> ctx (canvas/fill-style :white) (canvas/fill-rect val)))))))

(defn calc-canvas-size "Calculate the size in pixels of the maze background."
  []
  (let [width (js/parseInt @(:width maze-state))
        height (js/parseInt @(:height maze-state))
        thickness (js/parseInt @(:thickness maze-state))
        breadth (js/parseInt @(:breadth maze-state))
        canvas-width (+ (* width (+ thickness breadth)) thickness)
        canvas-height (+ (* height (+ thickness breadth)) thickness)]
    [canvas-width canvas-height]))

(defn render-canvas []
  (let [[canvas-width canvas-height] (calc-canvas-size)]
    [box :width (str canvas-width "px") :height (str canvas-height "px")
     :child [:canvas {:id "canvas" :width (str canvas-width) :height (str canvas-height) :style {:background-color :black}}]]))

(defn clear-canvas []
  (let [[canvas-width canvas-height] (calc-canvas-size)]
    (canvas/add-entity @config/monet-canvas
                       :background (canvas/entity {:x 0 :y 0 :w canvas-width :h canvas-height} nil
                                                  (fn [ctx val] (-> ctx (canvas/fill-style :black) (canvas/fill-rect val)))))))

(defn opposite-direction [dir]
  (first (cl/run-db 1 directions [opposite-dir]
                    (fresh [x y dx dy]
                           (direction dir [x y])
                           (is dx x #(* % -1))
                           (is dy y #(* % -1))
                           (direction opposite-dir [dx dy])))))

(defn neighbor-cell "Find the coordinates of a neighboring cell."
  [from-x from-y to-direction]
  (first (cl/run-db 1 directions [to-x to-y]
                    (fresh [x y]
                           (direction to-direction [x y])
                           (is to-x x #(+ % from-x))
                           (is to-y y #(+ % from-y))))))

(defn unvisited "Check if a cell is not yet part of the maze."
  [from-x from-y to-direction]
  (let [[to-x to-y] (neighbor-cell from-x from-y to-direction)]
    (and (<= 1 to-x (js/parseInt @(:width maze-state)))
         (<= 1 to-y (js/parseInt @(:height maze-state)))
         (empty? (cl/run-db 1 @maze-cells [d] (maze to-x to-y d))))))

(defn generate-maze [from-x from-y]
  )

(defn render-maze []
  (render-canvas)
  (clear-canvas)
  (generate-maze 0 (inc (rand-int (:height maze-state)))))

(defn main-panel []
  [v-box :src (at) :class "container" :children
   [[box :align :center :child [title :src (at) :label "AMAZE!" :level :level1
                                :style {:font-family ["Courier New", "monospace"] :font-weight :bold :color :orange}]]
    [h-box :children
     [[v-box :size "1" :children
       [[label :label "Maze Width"]
        [input-text :width "90px" :model (:width maze-state) :on-change #(reset! (:width maze-state) %)]]]
      [v-box :size "1" :children
       [[label :label "Maze Height"]
        [input-text :width "90px" :model (:height maze-state) :on-change #(reset! (:height maze-state) %)]]]
      [v-box :size "1" :children
       [[label :label "Wall Thickness"]
        [input-text :width "90px" :model (:thickness maze-state) :on-change #(reset! (:thickness maze-state) %)]]]
      [v-box :size "7" :children
       [[label :label "Hall Breadth"]
        [input-text :width "90px" :model (:breadth maze-state) :on-change #(reset! (:breadth maze-state) %)]]]]]
    [:p]
    [h-box :children
     [[box :size "100px" :child [button :label "Generate" :on-click #(render-cell 1 1 :west) :class "btn-primary"]]
      [box :size "100px" :child [button :label "Clear" :on-click clear-canvas :class "btn-secondary"]]]]
    [:p]
    [render-canvas]]])
