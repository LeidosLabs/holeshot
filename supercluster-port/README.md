# supercluster port to Java

Port from Node.js to Java of the [original supercluster repository.](https://github.com/mapbox/supercluster) 

## Developing Supercluster

```
Insert install & test-running instructions here
```

## Methods from original supercluster
#### `load(points)`

Loads an array of [GeoJSON Feature](https://tools.ietf.org/html/rfc7946#section-3.2) objects. Each feature's `geometry` must be a [GeoJSON Point](https://tools.ietf.org/html/rfc7946#section-3.1.2). Once loaded, index is immutable.

#### `getClusters(bbox, zoom)`

For the given `bbox` array (`[westLng, southLat, eastLng, northLat]`) and integer `zoom`, returns an array of clusters and points as [GeoJSON Feature](https://tools.ietf.org/html/rfc7946#section-3.2) objects.

#### `getTile(z, x, y)`

For a given zoom and x/y coordinates, returns a [geojson-vt](https://github.com/mapbox/geojson-vt)-compatible JSON tile object with cluster/point features.

#### `getChildren(clusterId)`

Returns the children of a cluster (on the next zoom level) given its id (`cluster_id` value from feature properties).

#### `getLeaves(clusterId, limit = 10, offset = 0)`

Returns all the points of a cluster (given its `cluster_id`), with pagination support:
`limit` is the number of points to return (set to `Infinity` for all points),
and `offset` is the amount of points to skip (for pagination).

#### `getClusterExpansionZoom(clusterId)`

Returns the zoom on which the cluster expands into several children (useful for "click to zoom" feature) given the cluster's `cluster_id`.

## Options from original supercluster

| Option   | Default | Description                                                       |
|----------|---------|-------------------------------------------------------------------|
| minZoom  | 0       | Minimum zoom level at which clusters are generated.               |
| maxZoom  | 16      | Maximum zoom level at which clusters are generated.               |
| radius   | 40      | Cluster radius, in pixels.                                        |
| extent   | 512     | (Tiles) Tile extent. Radius is calculated relative to this value. |
| nodeSize | 64      | Size of the KD-tree leaf node. Affects performance.               |
| log      | false   | Whether timing info should be logged.                             |

### Property map/reduce options

In addition to the options above, supercluster supports property aggregation with the following three options:

- `map`: a function that returns cluster properties corresponding to a single point.
- `reduce`: a reduce function that merges properties of two clusters into one.

Example of setting up a `sum` cluster property that accumulates the sum of `myValue` property values:

```js
const index = new Supercluster({
    map: (props) => ({sum: props.myValue}),
    reduce: (accumulated, props) => { accumulated.sum += props.sum; }
});
```

Note that `reduce` must not mutate the second argument (`props`).