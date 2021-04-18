(ns amaze.config (:require [reagent.core :as reagent]))

(def debug? ^boolean goog.DEBUG)

(def directions "Maze directions with coordinate deltas.  Origin is upper left."
  {:north [0 -1] :south [0 1] :east [1 0] :west [-1 0]})

;; 
(def maze-state "State of the maze UI controls."
  {:width (reagent/atom "20")
   :height (reagent/atom "20")
   :thickness (reagent/atom "10")
   :breadth (reagent/atom "15")})

(defn get-maze-state [kw] (js/parseInt @(kw maze-state)))
