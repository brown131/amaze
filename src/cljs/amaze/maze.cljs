(ns amaze.maze (:require [amaze.config :refer [directions get-db-value db]]))

(enable-console-print!)

(def maze-cells "Map holding the generated maze cells with direction of opening." (atom {}))

(def unvisited-maze-cells "Unvisited cells set" (atom #{}))

(defn opposite-direction [dir]
  (when dir
    (ffirst (filter (fn [[_ [x y]]] (= [(* -1 x) (* -1 y)] (dir directions))) directions))))

(defn neighbor-cell "Find the coordinates of a neighboring cell."
  [cell to-direction]
  (when to-direction
    (mapv + cell (to-direction directions))))

(defn in-range? "Check if a cell is in the maze."
  [from-cell to-direction]
  (when-let [[to-x to-y] (neighbor-cell from-cell to-direction)]
    (and (< -1 to-x (get-db-value :width))
         (< -1 to-y (get-db-value :height)))))

(defn valid-move? "Check if a neighboring cell is not seperated by a wall."
  [cell dir]
  (and (in-range? cell dir) 
       (or (= dir (get @maze-cells cell))
           (= dir (opposite-direction (get @maze-cells (neighbor-cell cell dir)))))))

(defn generate-dfs-maze "Generate a maze using the Depth-First Search algorithm."
  [[from-cell & rest-from-cells :as from-cells] threshold]
  (let [to-direction (first (filter #(and (in-range? from-cell %) (not (get @maze-cells (neighbor-cell from-cell %))))
                                    (shuffle [:north :south :east :west])))]
    (if-let [to-cell (neighbor-cell from-cell to-direction)]
      (do
        (swap! maze-cells assoc to-cell (opposite-direction to-direction))
        (when (< (/ (count @maze-cells) (get-db-value :size)) threshold)
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
    (when (< (/ (count @maze-cells) (get-db-value :size)) threshold)
      (recur to-cell threshold))))

(defn walk-maze [from-cell visited-cells]
  (let [to-direction (get visited-cells from-cell)
        to-cell (neighbor-cell from-cell to-direction)]
    (when (get visited-cells to-cell)
      (when-not (and (= (first from-cell) 0) (= (get @maze-cells from-cell) :west))
        (swap! maze-cells assoc from-cell to-direction))
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
  (let [start-cell (rand-nth (into [] @unvisited-maze-cells))]
    (when (empty? @maze-cells)
      (let [to-direction (first (filter #(in-range? stop-cell %) (shuffle [:north :south :east :west])))]
        (swap! maze-cells assoc stop-cell to-direction)
        (swap! unvisited-maze-cells disj stop-cell)))
    (visit-maze-cells start-cell start-cell {})
    (when (zero? (first stop-cell))
      (swap! maze-cells assoc stop-cell :west))))

(defn init-maze []
  (reset! (:entrance db) [0 (rand-int (get-db-value :height))])
  (reset! (:exit db) [(dec (get-db-value :width)) (rand-int (get-db-value :height))])
  (reset! (:ball-position db) @(:entrance db))
  (reset! (:size db) (* (get-db-value :width) (get-db-value :height)))
  (reset! maze-cells {})
  (reset! unvisited-maze-cells (into #{} (for [width (range (get-db-value :width))
                                               height (range (get-db-value :height))]
                                           [width height]))))

(defmulti generate-maze (fn [] @(:algorithm db)) :default 4)

(defmethod generate-maze 1 [] (generate-dfs-maze [(neighbor-cell @(:entrance db) :west)] 1))
(defmethod generate-maze 2 [] (generate-aldous-broder-maze (neighbor-cell @(:entrance db) :west) 1))
(defmethod generate-maze 3 [] (generate-wilson-maze @(:entrance db) 1))
(defmethod generate-maze 4 [] (do (generate-aldous-broder-maze (neighbor-cell @(:entrance db) :west) 0.30)
                                  (generate-wilson-maze @(:exit db) 0.70)))
(defmethod generate-maze 5 [] (do (generate-aldous-broder-maze (neighbor-cell @(:entrance db) :west) 0.10)
                                  (generate-dfs-maze [[(rand-int (get-db-value :width)) (rand-int (get-db-value :height))]] 0.80)
                                  (generate-wilson-maze @(:exit db) 1)))
