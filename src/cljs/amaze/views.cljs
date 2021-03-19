(ns amaze.views (:require [re-frame.core :as re-frame]
                          [re-com.core :refer [at box button gap h-box input-text label title v-box]]
                          [monet.canvas :as canvas]
                          [amaze.config :as config]
                          [amaze.subs :as subs]))

(defn display-maze []
  (canvas/add-entity @config/monet-canvas
                     :background (canvas/entity {:x 50 :y 50 :w 60 :h 60} ; val
                                                nil                       ; update function
                                                (fn [ctx val]             ; draw function
                                                  (-> ctx (canvas/fill-style :white) (canvas/fill-rect val))))))

(def text (atom ""))

(defn title-name []
  (let [name (re-frame/subscribe [::subs/name])]
    [title :src (at) :label (str "Amaze! " @name) :level :level1]))

(defn main-panel []
  [v-box :src (at) :class "sans-serif bg-white pa4"
   :children [[box :child [title-name]]
              [h-box :children [[box :size "auto" :child [button :label "Generate" :on-click display-maze
                                                          :class "bw0 br2 bg-blue pv2 ph3 white fwl tc ttu tracked"]]
                                [gap :size "5px"]
                                [box :size "auto" :child [button :label "Generate" :on-click display-maze
                                                          :class "f6 link dim ph3 pv2 mb2 dib white bg-dark-blue"]]]]
              [v-box :children [[label :class "f6 b db mb2"
                                 :label [:div "Name" [:span.normal.black-60 "(optional)"]]]
                                [input-text :model @text :on-change identity :class "ba b--black-20 pa2 mb2 db.w-20"]
                                [:small#name-desc.f6.black-60.db.mb2 "Helper text for the form control."]]]
              [box :width "640px" :child [:canvas {:id "canvas" :width "640" :height "640" :style {:background-color "black"}}]]]])
