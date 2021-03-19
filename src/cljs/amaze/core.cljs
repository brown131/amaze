(ns amaze.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [monet.canvas :as canvas]
   [amaze.events :as events]
   [amaze.views :as views]
   [amaze.config :as config]))

(enable-console-print!)

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)
    (reset! config/monet-canvas (canvas/init (.getElementById js/document "canvas") "2d"))))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
