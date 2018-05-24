# The Grapple Notebook REPL

Grapple is a notebook-style REPL for exploring, understanding, and presenting data. It is inspired by the [Gorilla REPL](https://github.com/JonyEpsilon/gorilla-repl) and [Observables](http://observablehq.com/).

Here's an example, a contour plot:

![Contour plot](https://raw.githubusercontent.com/exupero/grapple/master/screenshots/contour-plot.png)

## Usage

To run the Grapple server,

```
lein figwheel
```

You can also start it from a Clojure REPL. First run,

```
lein do clean, cljsbuild once prod
```

Then start a REPL and run,

```
(require 'grapple.server)
(grapple.server/start-server {})
```

## Built-In Components

Besides Clojure primitives, there are currently four built-in components that can be rendered:

- `grapple.table/matrix`, a basic grid of values.
- `grapple.table/table`, a table of values based on a list of maps; headers are the keys of the first map in the sequence.
- `grapple.plot/scatter`, a basic Vega 3 scatter plot.
- `grapple.plot/contour`, a Vega 3 contour plot.
