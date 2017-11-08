# The Grapple Notebook REPL

Grapple is a notebook-style REPL for exploring, understanding, and presenting data. It is inspired by the [Gorilla REPL](https://github.com/JonyEpsilon/gorilla-repl), but with a client built on ClojureScript that aims to be easier to customize. Grapple's graph generation and composition idioms are similar to Gorilla's, and most of the same keyboard shortcuts have been implemented.

Here's an example, a contour plot:

![Contour plot](https://raw.githubusercontent.com/exupero/grapple/master/screenshots/contour-plot.png)

_This project is still alpha. Contributions welcome._

## Usage

To run the Grapple server,

```
lein fighweel
```

If you want to run without Figwheel,

```
lein cljsbuild once app
lein run
```

The client is built on [Reagent](https://holmsand.github.io/reagent/) and [re-frame](https://github.com/Day8/re-frame). If you're unfamiliar with re-frame, I recommend Eric Normand's [excellent guide](https://purelyfunctional.tv/guide/re-frame-building-blocks/).

## Built-In Components

Besides Clojure primitives, there are currently four built-in components that can be rendered:

- `grapple.table/matrix`, a basic grid of values.
- `grapple.table/table`, a table of values based on a list of maps; headers are the keys of the first map in the sequence.
- `grapple.plot/scatter`, a basic Vega 3 scatter plot.
- `grapple.plot/contour`, a Vega 3 contour plot; optionally takes a map of options as the second argument, with the following keys:
    - `:show-points?`, setting to `true` shows the points underlying the contour plot.
    - `:levels`, the number of levels in the contour plot; defaults to 5.

## Creating Custom Components

Custom components can be defined from the Clojure side by instantiating `grapple.render/->Renderable`. It expects an object with any of the following keys:

- `:data`, a data payload that will be passed to the update function.
- `:dom`, a Hiccup-like DOM structure that will be used by Reagent to render the component's DOM tree.
- `:scripts`, a list of URLs to JavaScript files that need to be loaded before the component can fully render.
- `:update`, a string of JavaScript code that will be run to initialize and update the component. The following variables are available:
    - `data`, the value of `:data` converted to native JavaScript values via `clj->js`
    - `node`, the component's DOM node

I'm open to improvements on this scheme.

## To Do

- [ ] Configure to be usable as a library from another project
- [ ] Better system for creating custom components
- [ ] Add a menu of available commands and keyboard shortcuts
- [ ] Create a Leiningen plugin
- [ ] Better serialization format for saved notebooks
- [ ] Visually indicate difference between Clojure code blocks and Markdown blocks
- [ ] Indicate that server connection has been broken
- [ ] Connect to an existing nrepl
- [ ] Implement autocomplete
- [ ] Implement documentation lookup
