(ns amaze.maze (:require [amaze.config :refer [directions get-maze-state maze-algorithms]]))

(def maze-cells "Map holding the generated maze cells with direction of opening." (atom {}))

(def unvisited-maze-cells "Unvisited cells set" (atom #{}))

(defn opposite-direction [dir]
  (when dir
    (ffirst (filter (fn [[_ [x y]]] (= [(* -1 x) (* -1 y)] (dir directions))) directions))))

(defn neighbor-cell "Find the coordinates of a neighboring cell."
  [[from-x from-y] to-direction]
  (when to-direction
    (let [[dx dy] (to-direction directions)]
      [(+ from-x dx) (+ from-y dy)])))

(defn in-range? "Check if a cell is in the maze."
  [from-cell to-direction]
  (when-let [[to-x to-y] (neighbor-cell from-cell to-direction)]
    (and (< -1 to-x (get-maze-state :width))
         (< -1 to-y (get-maze-state :height)))))

(defn generate-dfs-maze "Generate a maze using the Depth-First Search algorith."
  [[from-cell & rest-from-cells :as from-cells] threshold]
  (let [to-direction (first (filter #(and (in-range? from-cell %)
                                          (not (get @maze-cells (neighbor-cell from-cell %))))
                                    (shuffle [:north :south :east :west])))]
    (if-let [to-cell (neighbor-cell from-cell to-direction)]
      (do
        (swap! maze-cells assoc to-cell (opposite-direction to-direction))
        (swap! unvisited-maze-cells disj to-cell)
        (when (< threshold (/ (count @unvisited-maze-cells) (* (get-maze-state :width) (get-maze-state :height))))
          (recur (cons to-cell from-cells) threshold)))
    (when-not (empty? rest-from-cells)
      (recur rest-from-cells threshold)))))

(defn generate-aldous-broder-maze "Generate a maze using the Aldous-Broder algorithm."
  [from-cell threshold]
  (let [to-direction (first (filter #(in-range? from-cell %) (shuffle [:north :south :east :west])))
        to-cell (neighbor-cell from-cell to-direction)
        from-direction (opposite-direction to-direction)]
    (when-not (get @maze-cells to-cell)
      (swap! maze-cells assoc to-cell from-direction)
      (swap! unvisited-maze-cells disj to-cell))
    (when (< threshold (/ (count @unvisited-maze-cells) (* (get-maze-state :width) (get-maze-state :height))))
      (recur to-cell threshold))))

(defn walk-maze [from-cell visited-cells]
  (let [to-direction (get visited-cells from-cell)
        to-cell (neighbor-cell from-cell to-direction)]
    (when (get visited-cells to-cell)
      (swap! maze-cells assoc from-cell to-direction)
      (swap! unvisited-maze-cells disj from-cell)
      (recur to-cell (dissoc visited-cells from-cell)))))

(defn visit-maze-cells [from-cell start-cell visited-cells]
  (let [to-direction (first (filter #(in-range? from-cell %) (shuffle [:north :south :east :west])))
        to-cell (neighbor-cell from-cell to-direction)]
    (if (get @unvisited-maze-cells from-cell)
      (recur to-cell start-cell (assoc visited-cells from-cell to-direction))
      (do
        (walk-maze start-cell (assoc visited-cells from-cell to-direction))
        (when-not (empty? @unvisited-maze-cells)
          (let [new-start-cell (rand-nth (into [] @unvisited-maze-cells))]
            (recur new-start-cell new-start-cell {})))))))

(defn generate-wilson-maze "Generate a maze using the Wilson algorithm."
  [stop-cell threshold]
  (let [start-cell (rand-nth (into [] @unvisited-maze-cells))
        to-direction (first (filter #(in-range? stop-cell %) (shuffle [:north :south :east :west])))]
    (swap! maze-cells assoc stop-cell to-direction)
    (swap! unvisited-maze-cells disj stop-cell)
    (visit-maze-cells start-cell start-cell {})
    (when (= threshold 0.0)
      (swap! maze-cells assoc stop-cell :west))))

(defn init-maze []
  (reset! maze-cells {})
  (reset! unvisited-maze-cells (into #{} (for [width (range (get-maze-state :width))
                                               height (range (get-maze-state :height))]
                                           [width height]))))

(defn generate-maze []
  (init-maze)
  (time (case (get-maze-state :algorithm)
          1 (generate-dfs-maze [[-1 (rand-int (get-maze-state :height))]] 0)
          2 (generate-aldous-broder-maze [-1 (rand-int (get-maze-state :height))] 0)
          3 (generate-wilson-maze [0 (rand-int (get-maze-state :height))] 0)
          4 (do
              (generate-aldous-broder-maze [(rand-int (get-maze-state :width))(rand-int (get-maze-state :height))] 0.70)
              (generate-wilson-maze [(rand-int (get-maze-state :width)) (rand-int (get-maze-state :height))] 0.30)))))
