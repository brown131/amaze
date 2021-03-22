(ns amaze.core (:require [clojure.core.logic :as cl :refer [fresh is run-db]]
                         [clojure.core.logic.pldb :refer [db db-rel db-fact empty-db]]
                         [reagent.core :as reagent]
                         [reagent.dom :as rdom]
                         [re-com.core :refer [at box button gap h-box input-text label title v-box]]
                         [monet.canvas :as canvas]
                         [re-frame.core :as re-frame]))

(enable-console-print!)

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

;; HTML5 drawing canvas.
(def monet-canvas (atom nil))

;; State of the maze UI controls.
(def maze-state {:width (reagent/atom "20")
                 :height (reagent/atom "20")
                 :thickness (reagent/atom "10")
                 :breadth (reagent/atom "15")})

;; Facts database holding the generated maze cells.
(def maze-cells (atom empty-db))

;; Define and set the arity of the db relations:
(db-rel direction ^:index d p)
(db-rel maze ^:index p d)

;; Maze directions with coordinate deltas.
(def directions (db [direction :north [0 -1]]
                    [direction :south [0 1]]
                    [direction :east [1 0]]
                    [direction :west [-1 0]]))

(defn get-maze-state [kw] (js/parseInt @(kw maze-state)))

(defn calc-canvas-size "Calculate the size in pixels of the maze background."
  []
  (let [width (get-maze-state :width)
        height (get-maze-state :height)
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        canvas-width (+ (* width (+ thickness breadth)) thickness)
        canvas-height (+ (* height (+ thickness breadth)) thickness)]
    [canvas-width canvas-height]))

(defn render-cell "Display a cell at the specified coordinates."
  [x y dir]
  (let [thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        [dx dy] (first (run-db 1 directions [p] (direction dir p)))
        px (- (+ (* x (+ breadth thickness)) (* (Math/min dx 0) thickness)) breadth)
        py (- (+ (* y (+ breadth thickness)) (* (Math/min dy 0) thickness)) breadth)
        cw (+ breadth (* thickness (Math/abs dx)))
        ch (+ breadth (* thickness (Math/abs dy)))]
    (canvas/add-entity @monet-canvas [px py]
                       (canvas/entity {:x px :y py :w cw :h ch} nil
                                      (fn [ctx val] (-> ctx
                                                        (canvas/fill-style :white)
                                                        (canvas/fill-rect val)))))))

(defn render-canvas []
  (let [[canvas-width canvas-height] (calc-canvas-size)]
    [box :width (str canvas-width "px") :height (str canvas-height "px")
     :child [:canvas {:id "canvas" :width (str canvas-width) :height (str canvas-height)
                      :style {:background-color :black}}]]))

(defn clear-canvas []
  (canvas/clear! @monet-canvas)
  (reset! maze-cells empty-db))

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
    (and (<= 1 to-x (get-maze-state :width))
         (<= 1 to-y (get-maze-state :height))
         (empty? (cl/run-db 1 @maze-cells [d] (maze [to-x to-y] d))))))

(defn generate-maze [from-x from-y]         
  (let [exits (filterv #(unvisited from-x from-y %) [:north :south :east :west])]
    (when-not (empty? exits)
      (let [to-direction (get exits (rand-int (count exits)))
            [to-x to-y] (neighbor-cell from-x from-y to-direction)
            from-direction (opposite-direction to-direction)]
        (render-cell to-x to-y from-direction)
        (swap! maze-cells #(db-fact % maze [to-x to-y] from-direction))
        (generate-maze to-x to-y)
        (recur from-x from-y)))))

(defn render-exit []
  (let [x (inc (get-maze-state :width))
        y (inc (rand-int (get-maze-state :height)))
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        px (* (dec x) (+ breadth thickness))
        py (+ (* (dec y) (+ breadth thickness)) thickness)]
    (swap! maze-cells #(db-fact % maze [x y] :east))
    (canvas/add-entity @monet-canvas [px py]
                       (canvas/entity {:x px :y py :w thickness :h breadth} nil
                                      (fn [ctx val] (-> ctx
                                                        (canvas/fill-style :white)
                                                        (canvas/fill-rect val)))))))
    ;;(render-cell x y :east)))
    
(defn render-maze []
  (clear-canvas)
  (generate-maze 0 (inc (rand-int (get-maze-state :height))))
  (render-exit)
  (println (sort (cl/run-db* @maze-cells [x y d] (maze [x y] d)))))

(defn print-maze []
  (let [image-url (.toDataURL (:canvas @monet-canvas) "image/jpeg")
        win (.open js/window image-url)]
    (.write (. win -document) (str "<br><img src=\"" image-url "\"/>"))))

(defn main-panel []
  [v-box :src (at) :class "container" :children
   [[box :align :center :child [title :src (at) :label "AMAZE!" :level :level1
                                :style {:font-family ["Courier New", "monospace"]
                                        :font-weight :bold :color :orange}]]
    [h-box :children
     [[v-box :size "1" :children
       [[label :label "Maze Width"]
        [input-text :width "90px" :model (:width maze-state)
         :on-change #(reset! (:width maze-state) %)]]]
      [v-box :size "1" :children
       [[label :label "Maze Height"]
        [input-text :width "90px" :model (:height maze-state)
         :on-change #(reset! (:height maze-state) %)]]]
      [v-box :size "1" :children
       [[label :label "Wall Thickness"]
        [input-text :width "90px" :model (:thickness maze-state)
         :on-change #(reset! (:thickness maze-state) %)]]]
      [v-box :size "7" :children
       [[label :label "Hall Breadth"]
        [input-text :width "90px" :model (:breadth maze-state)
         :on-change #(reset! (:breadth maze-state) %)]]]]]
    [:p]
    [h-box :children
     [[box :child [button :label "Generate" :on-click render-maze :class "btn-primary"]]
      [gap :size "20px"]
      [box :child [button :label "Print" :on-click print-maze :class "btn-info"]]
      [gap :size "20px"]
      [box :child [button :label "Clear" :on-click clear-canvas :class "btn-info"]]]]
    [:p]
    [render-canvas]]])

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-panel] root-el)
    (reset! monet-canvas (canvas/init (.getElementById js/document "canvas") "2d"))))

(defn init []
  (dev-setup)
  (mount-root))
