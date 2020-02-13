photogrammetry
=============================

Pure Java library implementing the basic camera models and measurement algorithms 
necessary to calculate position and mensurate distances in non-orthorectified geospatial imagery.

This library does not provide any capabilities to parse geospatial image formats. See
https://github.com/codice/imaging-nitf or http://www.gdal.org/ for libraries capable of
parsing metadata needed by these classes.

## Building

```
mvn clean install
```

This will compile imaging-photogrammetry and run all of the tests.

## Maven

```xml
<dependency>
  <groupId>com.leidoslabs.abs.imaging</groupId>
  <artifactId>photogrammetry</artifactId>
  <version>1.0.0</version>
</dependency>
```
### External Dependencies
This project depends on the Java Topology Suite (JTS) for geometric primitives and Apache Commons 
Math for numerical routines. See https://github.com/locationtech/jts and 
http://commons.apache.org/proper/commons-math/ for more information on those projects.

## Using the CameraModel

The CameraModel abstraction is the main entry point for conversion between the 2D image plane 
(x, y) and the 3D World Geodetic System (WGS84) (lon, lat, elevation). These conversions allow
you to make simple real world measurements of locations identified in the image.


```java
CameraModel camera = RPCCameraModelFactory.buildRPCCameraFromMetadata(flatMetadataMap);

Point2D locationOnImage = camera.worldToImage(
    new Coordinate(56.3050482, 27.1580990, camera.getDefaultElevation());
    
Coordinate wgs84LocationOfULCorner = camera.imageToWorld(new Point2D.Double(0,0));

double gsd = camera.getGSD(new Point2D.Double(0,0));
double distance = camera.getDistanceInMeters(new Point2D.Double(0,0), new Point2D.Double(100,100));
```

## Using the LocalCartesianCoordinateSystem

The LocalCartesianCoordinateSystem allows you to convert between the WGS84 coordinate system 
(degrees latitude and longitude elevation in meters) to a more familar 3D coordinate system with
perpendicular axis in meters (x,y,z). 

```java
LocalCartesianCoordinateSystem lccs = new LocalCartesianCoordinateSystem(
        new Coordinate(-0.130491,51.510397,0)
    );

// WGS84 location of a point 5000 meters north and 200 meters east of the origin.
Coordinate wgs84Coordinate = lccs.localToGlobal(new Coordinate(200,5000,0));
```