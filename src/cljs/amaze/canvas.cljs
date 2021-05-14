(ns amaze.canvas
  (:require [monet.canvas :as canvas]
            [amaze.config :refer [get-db-value]]))

;; HTML5 drawing canvas.
(def monet-canvas (atom nil))

(defn calc-canvas-size "Calculate the size in pixels of the maze background."
  []
  (let [width (get-db-value :width)
        height (get-db-value :height)
        thickness (get-db-value :thickness)
        breadth (get-db-value :breadth)
        canvas-width (+ (* width (+ thickness breadth)) thickness)
        canvas-height (+ (* height (+ thickness breadth)) thickness)]
    [canvas-width canvas-height]))

(defn reset-canvas [] (reset! monet-canvas (canvas/init (.getElementById js/document "canvas") "2d")))

(defn clear-canvas [] (canvas/clear! @monet-canvas))
  
(defn print-maze []
  (let [image-url (.toDataURL (:canvas @monet-canvas) "image/jpeg")
        win (.open js/window image-url)]
    (.write (. win -document) (str "<br><img src=\"" image-url "\"/>"))))

(defn draw-circle [x y r c]
  (canvas/add-entity @monet-canvas [:circle x y]
                     (canvas/entity {:x (+ x r) :y (+ y r) :r r} nil
                                    (fn [ctx val] (-> ctx
                                                      (canvas/fill-style c)
                                                      (canvas/circle val)
                                                      (canvas/fill))))))

(defn draw-rect [x y w h c]
  (canvas/add-entity @monet-canvas [:rect x y]
                     (canvas/entity {:x x :y y :w w :h h} nil
                                    (fn [ctx val] (-> ctx
                                                      (canvas/fill-style c)
                                                      (canvas/fill-rect val))))))
