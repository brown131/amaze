# AMAZE!

This is a simple maze generation program using a depth-first search algorithm. The name of this project is an homage to an old BASIC program I came across that got me interested in writing maze generating program.

This particular implementation was done as a study in using [ClojureScript](https://clojurescript.org/) and in particular clojure.core.logic, which is a Clojure/ClojureScript implementation of the [miniKanren](http://minikanren.org/) logic programming DSL originally written in Scheme.

## How to use

The height, width, thickness of the walls, and the breadth of the hallways in pixels can be set before pressing the "Generate" button. The maze can be displayed stand-alone on a separate page for printing by pressing the "Print" button. The "Clear" button clears the maze canvas.

## Development

Run ```lein watch``` to start the shadow-clj server.

![https://clojurescript.org/index](https://clojurescript.org/images/cljs-logo-60b.png)
