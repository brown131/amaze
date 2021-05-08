(ns amaze.core (:require [reagent.dom :as rdom]
                         [re-com.core :refer [at box button gap h-box input-text label single-dropdown title v-box]]
                         [monet.canvas :as canvas]
                         [re-frame.core :as re-frame]
                         [amaze.config :refer [debug? directions maze-algorithms maze-state get-maze-state]]
                         [amaze.maze :refer [maze-cells init-maze generate-maze]]))

(enable-console-print!)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

;; HTML5 drawing canvas.
(def monet-canvas (atom nil))

(defn calc-canvas-size "Calculate the size in pixels of the maze background."
  []
  (let [width (get-maze-state :width)
        height (get-maze-state :height)
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        canvas-width (+ (* width (+ thickness breadth)) thickness)
        canvas-height (+ (* height (+ thickness breadth)) thickness)]
    [canvas-width canvas-height]))

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

(defn render-exit []
  (let [x (get-maze-state :width)
        y (rand-int (get-maze-state :height))
        thickness (get-maze-state :thickness)
        breadth (get-maze-state :breadth)
        px (* x (+ breadth thickness))
        py (+ (* y (+ breadth thickness)) thickness)]
    (canvas/add-entity @monet-canvas [px py]
                       (canvas/entity {:x px :y py :w thickness :h breadth} nil
                                      (fn [ctx val] (-> ctx
                                                        (canvas/fill-style :white)
                                                        (canvas/fill-rect val)))))))

(defn clear-canvas [] (canvas/clear! @monet-canvas))

(defn render-maze []
  (clear-canvas)
  (init-maze)
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
      [v-box :size "1" :children
       [[label :label "Hall Breadth"]
        [input-text :width "90px" :model (:breadth maze-state)
         :on-change #(reset! (:breadth maze-state) %)]]]
      [v-box :size "5" :children
       [[label :label "Algorithm"]
        [single-dropdown :choices maze-algorithms :model (:algorithm maze-state)
         :width "250px" :max-height "300px" :on-change  #(reset! (:algorithm maze-state) %)]]]]]
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
