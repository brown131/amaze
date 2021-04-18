(ns amaze.maze (:require [amaze.config :refer [directions get-maze-state]]))

; Maze algorithms: 
;; :dfs = Depth-First Search algorithm - fastest, but creates tunnels with no exits.
;; :aldous-broder = Aldous-Broder algorithm - quick at first, slow at end
;; :wilson = Wilson algorithm - slow at first, quick at end
;; :hybrid = Aldous-Broder/Wilson Hybrid algorith - Use Aldous-Broder until the Wilson alogorithm becomes faster.
(def maze-algorithm :hybrid)

(def maze-cells "Map holding the generated maze cells with direction of opening." (atom {}))

(def unvisited-maze-cells "Unvisited cells set" (atom #{}))

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
  [from-cell threshold]
  (let [to-direction (rand-nth (filter #(in-range? from-cell %) [:north :south :east :west]))
        to-cell (neighbor-cell from-cell to-direction)
        from-direction (opposite-direction to-direction)]
    (when-not (get @maze-cells to-cell)
      (swap! maze-cells assoc to-cell from-direction)
      (swap! unvisited-maze-cells disj to-cell))
    (when (>= threshold (/ (count @unvisited-maze-cells) (* (get-maze-state :width) (get-maze-state :height))) )
      (recur to-cell threshold))))

(defn walk-maze [from-cell visited-cells]
  (let [to-direction (get visited-cells from-cell)
        to-cell (neighbor-cell from-cell to-direction)]
    (when (get visited-cells to-cell)
      (swap! maze-cells assoc from-cell to-direction)
      (swap! unvisited-maze-cells disj from-cell)
      (recur to-cell (dissoc visited-cells from-cell)))))

(defn visit-maze-cells [from-cell start-cell visited-cells]
  (let [to-direction (rand-nth (filter #(in-range? from-cell %) [:north :south :east :west]))
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
        to-direction (rand-nth (filter #(in-range? stop-cell %) [:north :south :east :west]))]
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
  (time (case maze-algorithm
    :dfs (generate-dfs-maze [-1 (rand-int (get-maze-state :height))])
    :aldous-broder (generate-aldous-broder-maze [-1 (rand-int (get-maze-state :height))] 0)
    :wilson (generate-wilson-maze [0 (rand-int (get-maze-state :height))] 0)
    :hybrid (do
              (generate-aldous-broder-maze [-1 (rand-int (get-maze-state :height))] 0.70)
              (generate-wilson-maze [(rand-int (get-maze-state :width)) (rand-int (get-maze-state :height))] 0.30)))))
