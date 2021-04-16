(ns amaze.core (:require [reagent.core :as reagent]
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
(def maze-cells (atom {}))

;; Unsvisited cells list.
(def unvisited-maze-cells (atom #{}))

;; Maze directions with coordinate deltas.  Origin is upper left.
(def directions {:north [0 -1] :south [0 1] :east [1 0] :west [-1 0]})

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

(defn opposite-direction [dir]
  (ffirst (filter (fn [[_ [x y]]] (= [(* -1 x) (* -1 y)] (dir directions))) directions)))

(defn neighbor-cell "Find the coordinates of a neighboring cell."
  [[from-x from-y] to-direction]
  (let [[dx dy] (to-direction directions)]
    [(+ from-x dx) (+ from-y dy)]))

(defn unvisited? "Check if a cell is not yet part of the maze."
  [from-cell to-direction]
  (let [[to-x to-y] (neighbor-cell from-cell to-direction)]
    (and (< -1 to-x (get-maze-state :width))
         (< -1 to-y (get-maze-state :height))
         (not (get @maze-cells [to-x to-y])))))

(defn in-range? "Check if a cell is in the maze."
  [from-cell to-direction]
  (let [[to-x to-y] (neighbor-cell from-cell to-direction)]
    (and (< -1 to-x (get-maze-state :width))
         (< -1 to-y (get-maze-state :height)))))

(defn generate-dfs-maze "Generate a maze using the Depth-First Search algorith."
  [from-cell]
  (let [exits (filterv #(unvisited? from-cell %) [:north :south :east :west])]
    (when-not (empty? exits)
      (let [to-direction (rand-nth exits)
            to-cell (neighbor-cell from-cell to-direction)
            from-direction (opposite-direction to-direction)]
        (swap! maze-cells assoc to-cell from-direction)
        (swap! unvisited-maze-cells disj to-cell)
        (generate-dfs-maze to-cell)
        (recur from-cell)))))

(defn generate-aldous-broder-maze "Generate a maze using the Aldous-Broder algorithm."
  [from-cell]
  (let [to-direction (rand-nth (filter #(in-range? from-cell %) [:north :south :east :west]))
        to-cell (neighbor-cell from-cell to-direction)
        from-direction (opposite-direction to-direction)]
    (when-not (get @maze-cells to-cell)
      (swap! maze-cells assoc to-cell from-direction)
      (swap! unvisited-maze-cells disj to-cell))
    (when-not (empty? @unvisited-maze-cells)
      (recur to-cell))))

(defn walk-maze [from-cell visited-cells]
  (let [to-direction (get visited-cells from-cell)
        to-cell (neighbor-cell from-cell to-direction)]
    (when (get visited-cells to-cell)
      (swap! maze-cells assoc from-cell to-direction)
      (swap! unvisited-maze-cells disj from-cell)
      (recur to-cell (dissoc visited-cells from-cell)))))

(defn generate-wilson-maze "Generate a maze using the Wilson algorithm."
  [from-cell start-cell visited-cells]
  (let [to-direction (rand-nth (filter #(in-range? from-cell %) [:north :south :east :west]))
        to-cell (neighbor-cell from-cell to-direction)]
    (if (get @unvisited-maze-cells from-cell)
      (recur to-cell start-cell (assoc visited-cells from-cell to-direction))
      (do
        (walk-maze start-cell (assoc visited-cells from-cell to-direction))
        (when-not (empty? @unvisited-maze-cells)
          (let [new-start-cell (rand-nth (into [] @unvisited-maze-cells))]
            (recur new-start-cell new-start-cell {})))))))

(defn generate-maze []
  (let [start-cell (rand-nth (into [] @unvisited-maze-cells))
        stop-cell [0 (rand-int (get-maze-state :height))]]
    (swap! unvisited-maze-cells disj stop-cell)
    (generate-wilson-maze start-cell start-cell {})
    (swap! maze-cells assoc stop-cell :west))

  #_(generate-aldous-broder-maze [-1 (rand-int (get-maze-state :height))])

  #_(generate-dfs-maze [-1 (rand-int (get-maze-state :height))]))

(defn render-cell "Display a cell at the specified coordinates.  
                   An opening is rendered in the specified direction."
  [x y dir]
  (let [thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        [dx dy] (dir directions)
        px (+ (* x breadth) (* thickness (+ x 1 (Math/min dx 0))))
        py (+ (* y breadth) (* thickness (+ y 1 (Math/min dy 0))))
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
  (reset! maze-cells {})
  (reset! unvisited-maze-cells (into #{} (for [width (range (get-maze-state :width))
                                               height (range (get-maze-state :height))]
                                           [width height]))))

(defn render-wall [x y side]
  (let [[dx dy] (directions side)
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        width (if (zero? dx) breadth thickness)
        height (if (zero? dy) breadth thickness)
        px (+ (* x (+ breadth thickness)) (* dy width))
        py (+ (* y (+ breadth thickness)) (* dx height))]))

(defn render-exit []
  (let [x (get-maze-state :width)
        y (rand-int (get-maze-state :height))
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        px (* x (+ breadth thickness))
        py (+ (* y (+ breadth thickness)) thickness)]
    (swap! maze-cells assoc [x y] :east)
    (swap! unvisited-maze-cells disj [x y])
    (canvas/add-entity @monet-canvas [px py]
                       (canvas/entity {:x px :y py :w thickness :h breadth} nil
                                      (fn [ctx val] (-> ctx
                                                        (canvas/fill-style :white)
                                                        (canvas/fill-rect val)))))))

(defn render-maze []
  (clear-canvas)
  (generate-maze)
  (run! (fn [[[x y] d]] (render-cell x y d)) @maze-cells)
  (render-exit))

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
