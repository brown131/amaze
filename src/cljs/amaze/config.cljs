(ns amaze.config (:require [reagent.core :as reagent]))

(def debug? ^boolean goog.DEBUG)

(def directions "Maze directions with coordinate deltas.  Origin is upper left."
  {:north [0 -1] :south [0 1] :east [1 0] :west [-1 0] :none [0 0]})

(def maze-algorithms "Maze generation algorithms"
  [{:id 1 :label "Depth-First Search"}
   {:id 2 :label "Aldous-Broder"}
   {:id 3 :label "Wilson"}
   {:id 4 :label "Aldous-Broder/Wilson Hybrid"}
   {:id 5 :label "AB/DFS/W Hybrid"}])

(def db "State of the maze UI controls."
  {:width (reagent/atom "20")
   :height (reagent/atom "20")
   :thickness (reagent/atom "10")
   :breadth (reagent/atom "15")
   :algorithm (reagent/atom 4)
   :size (reagent/atom 400)
   :ball-position (reagent/atom [0 0])})

(defn get-db-value [kw] (js/parseInt @(kw db)))
