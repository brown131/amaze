# AMAZE!

This is a simple maze generation program that I used to play with maze generation algotithms. The name of this project is an homage to an old BASIC program I came across that got me interested in writing maze generating program.

There are two branches on this project. The first branch "core-logic" was done as a study in using [ClojureScript](https://clojurescript.org/) and in particular clojure.core.logic, which is a Clojure/ClojureScript implementation of the [miniKanren](http://minikanren.org/) logic programming DSL originally written in Scheme.

For the second branch "algorithms" I implemented a few different maze generaton algorithms:
- Depth-First Search algorithm - Fastest, but creates tunnels with no exits.
- Aldous-Broder algorithm - Quick at first, slow at end
- Wilson algorithm - Slow at first, quick at end
- Aldous-Broder/Wilson Hybrid algorithm - Uses Aldous-Broder until the Wilson algorithm becomes faster.
- AB/DFS/W Hybrid algorithm - Played with a mixture of the three. I can't say it's particularly better or worse in any way.

## How to use

The height, width, thickness of the walls, and the breadth of the hallways in pixels can be set before pressing the "Generate" button. The algorithm to use when generating the mase can also be selected. 

The maze can be displayed on a separate page for printing by pressing the "Print" button. 

The "Play" button displays a red ball at the entrance to the maze. The ball can then be moved through the maze using:
* The "W" or "I" key to move the ball up;
* The "A" or "J" key to move the ball left;
* The "S" or "K" key to move the ball down;
* The "D" or "L" key to move the ball right.

## Development

Run ```lein watch``` to start the shadow-cljs server.

Run ```lein release``` to build a release version.

![https://clojurescript.org/index](https://clojurescript.org/images/cljs-logo-60b.png)
