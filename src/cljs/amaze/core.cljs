(ns amaze.core (:require [reagent.dom :as rdom]
                         [re-frame.core :as re-frame]
                         [re-pressed.core :as rp]
                         [amaze.canvas :refer [reset-canvas]]
                         [amaze.config :refer [debug?]]
                         [amaze.panel :refer [main-panel render-maze]]
                         [amaze.play :as play]))

(enable-console-print!)

(defn dev-setup []
  (when debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-panel] root-el)
    (play/dispatch-keydown-rules)
    (reset-canvas)
    (render-maze)))

(defn init []
  (re-frame/dispatch-sync [::play/initialize-db])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (dev-setup)
  (mount-root))
