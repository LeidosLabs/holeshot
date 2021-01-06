/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leidoslabs.holeshot.elt.tileserver;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

import org.image.common.cache.CacheableUtil;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leidoslabs.holeshot.credentials.HoleshotCredentials;
import com.leidoslabs.holeshot.elt.tileserver.awt.AWTImageTools;
import com.leidoslabs.holeshot.elt.utils.GeometryUtils;
import com.leidoslabs.holeshot.elt.utils.ImageLoadUtils;
import com.leidoslabs.holeshot.imaging.coord.ImageScale;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModelFactory;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClient;
import com.leidoslabs.holeshot.tileserver.v1.TileServerClientBuilder;

/**
 * Tileserver Image representation, also handles fetching tile image
 */
public class TileserverImage implements ImageTileFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(TileserverImage.class);

	private final CameraModel cameraModel;
	private final TilePyramidDescriptor tilePyramidDescriptor;
	private final TileserverUrlBuilder tileserverUrlBuilder;
	private final TileServerClient tileServerClient;
	private final TileRef topTile;
	private final String collectionId;
	private final String timestamp;
//	private final double maxImageDimension;

	/**
	 * Builds a TileServerClient, and a builds TilePyramidDescriptor and CameraModel from metadata
	 * and comptues some other useful info about the image
	 * @param imageMetadataURL metadata URL
	 * @throws IOException
	 */
	public TileserverImage(URL imageMetadataURL)
			throws IOException {
		HoleshotCredentials credentials = HoleshotCredentials.getApplicationDefaults();

		this.tileserverUrlBuilder = new TileserverUrlBuilder(imageMetadataURL);

		this.tileServerClient = TileServerClientBuilder
				.forEndpoint(imageMetadataURL.toString().replaceFirst("^(.*/tileserver)/.*$", "$1"))
				.withCredentials(credentials).withDefaultMaxPerRoute(Integer.MAX_VALUE)
				.withMaxTotal(Integer.MAX_VALUE).build();

		this.tilePyramidDescriptor = TileserverTileCache.getInstance()
				.getTilePyramidDescriptor(tileServerClient, getCollectionID(), getTimestamp());
		this.collectionId = null;
		this.timestamp = null;

		if (tilePyramidDescriptor == null) {
			throw new FileNotFoundException(String.format("Can't find metadata for %s", imageMetadataURL.toString()));
		}

	    cameraModel = RPCCameraModelFactory.buildRPCCameraFromMetadata(getMetadata());

		final int maxRset = getTilePyramidDescriptor().getMaxRLevel();
		this.topTile = new TileRef(this, maxRset, 0, 0);
//		maxImageDimension = Math.max(tilePyramidDescriptor.getTileWidth(),tilePyramidDescriptor.getTileHeight()) * Math.pow(2.0, maxRset);
	}

	   /**
     * Build the camera model when you already have the metadata and don't need a client
     * @param tilePyramidDescriptor The metadata descriptor
     */
	public TileserverImage(TilePyramidDescriptor tilePyramidDescriptor) {

		this.tileserverUrlBuilder = null;
	    this.tileServerClient = null;
	    this.tilePyramidDescriptor = tilePyramidDescriptor;
	    this.collectionId = tilePyramidDescriptor.getName().split(":")[0];
	    this.timestamp = tilePyramidDescriptor.getName().split(":")[1];

	    cameraModel = RPCCameraModelFactory.buildRPCCameraFromMetadata(getMetadata());

        final int maxRset = getTilePyramidDescriptor().getMaxRLevel();
        this.topTile = new TileRef(this, maxRset, 0, 0);
//        maxImageDimension = Math.max(tilePyramidDescriptor.getTileWidth(),tilePyramidDescriptor.getTileHeight()) * Math.pow(2.0, maxRset);
    }

	public double getGSD(Coordinate latLon, double rset) {
		final double r0GSD = cameraModel.getGSD(GeometryUtils.toPoint2D(latLon));
		final double rsetGSD = Math.pow(2.0, rset) * r0GSD;
		return rsetGSD;
	}


	public String getImageBaseURL() {
		return tileserverUrlBuilder.getImageBaseURL();
	}
	public String getImageMetadataURL() {
		return tileserverUrlBuilder.getImageMetadataURL();
	}

	public String getCollectionID() {
	    if(this.collectionId == null) {
            return tileserverUrlBuilder.getCollectionID();
        }
	    return this.collectionId;
	}

	public String getTimestamp() {
	    if(this.timestamp == null) {
            return tileserverUrlBuilder.getTimestamp();
        }
	    return this.timestamp;
	}

	public CameraModel getCameraModel() {
		return cameraModel;
	}

	public TilePyramidDescriptor getTilePyramidDescriptor() {
		return tilePyramidDescriptor;
	}

	public String getName() {
		return String.format("TileserverImage %s/%s", getCollectionID(), getTimestamp());
	}

	/**
	 * @param src
	 * @return normalized point between [0, 1] based off maxImageDimension
	 */
//	public Point2D normalize(Point2D src) {
//		return new Point2D.Double(normalize(src.getX()), normalize(src.getY()));
//	}
	/**
	 * @param src
	 * @return maps points from [0,1] -> [0, maxImageDimension]
	 */
//	public Point2D deNormalize(Point2D src) {
//		return new Point2D.Double(deNormalize(src.getX()), deNormalize(src.getY()));
//	}
	
	/**
	 * @param src
	 * @return normalized point between [0, 1] based off maxImageDimension
	 */
//	public double normalize(double src) {
//		return src/maxImageDimension;
//
//	}
	
	/**
	 * @param src
	 * @return maps points from [0,1] -> [0, maxImageDimension]
	 */
//	public double deNormalize(double src) {
//		return src*maxImageDimension;
//	}
	
	public TileRef getTopTile() {
		return topTile;
	}
	
	public CoreImage getTileserverTile(TileRef tileRef) throws IOException {
		return getTileserverTile(tileRef, false);
	}

	/**
	 * Retrieves tile data for each band and merges
	 */
	public CoreImage getTileserverTile(TileRef tileRef, boolean minPriority) throws IOException {
		final int numBands = getNumBands();

		CoreImage[] bands = IntStream.range(0, Math.min(3, numBands)).parallel()
				.mapToObj(i -> getTileserverTileForBand(tileRef.getForBand(i), minPriority)).toArray(CoreImage[]::new);

		CoreImage tileserverTile = null;
		if (Arrays.stream(bands).noneMatch(b->b == null)) {
			final CoreImage alpha = new CoreImage(AWTImageTools.makeAlphaChannelFor(bands[0].getBufferedImage(), 1.0f));
			if (numBands > 0 && numBands < 3) {
				tileserverTile = mergeBands(new CoreImage[] { bands[0], bands[0], bands[0], alpha});
			} else {
				// TODO: Need to figure out a band selection strategy that isn't just a hardcoding.  (e.g. Is this in the original
				// image metadata? Do I need a user panel for it?)
				tileserverTile = mergeBands(new CoreImage[] { bands[2], bands[1], bands[0], alpha});
			}
		}

		return tileserverTile;
	}

	/**
	 * @param bands
	 * @return Image with channels merged
	 */
	protected CoreImage mergeBands(CoreImage[] bands) {
		BufferedImage[] bandImages = Arrays.stream(bands).map(b->b.getBufferedImage()).toArray(BufferedImage[]::new);
		return new CoreImage(AWTImageTools.mergeChannels(bandImages, true, false));
	}

	/**
	 * Gets tile by invoking TileserverTileCache.getTileServerTileForBand, which 
	 * either gets the tile from cache or by using tileserverclient
	 * @param tileRef
	 * @return
	 */
	protected CoreImage getTileserverTileForBand(TileRef tileRef, boolean minPriority) {
		CoreImage result = null;
		try {
			result = TileserverTileCache.getInstance().getTileserverTileForBand(this, tileRef, false, minPriority);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return result;
	}

	public int getMaxPixelValue() {
		return (int)Math.pow(2.0, this.getBitsPerPixel());
	}

	protected Map<String, Object> getMetadata() {
		return (Map<String, Object>) tilePyramidDescriptor.getMetadata();
	}

	public int getNumBands() {
		return Integer.parseInt(String.valueOf(getMetadata().get("NBANDS")));
	}
	public int getBitsPerPixel() {
		return Integer.parseInt(String.valueOf(getMetadata().get("NBPP")));
	}
	public int getTileWidth() {
		return tilePyramidDescriptor.getTileWidth();
	}

	public int getTileHeight() {
		return tilePyramidDescriptor.getTileHeight();
	}

	public int getR0ImageWidth() {
		return tilePyramidDescriptor.getWidth();
	}

	public int getR0ImageHeight() {
		return tilePyramidDescriptor.getHeight();
	}

	public int getMaxRLevel() {
		return tilePyramidDescriptor.getMaxRLevel();
	}

	public TileServerClient getTileServerClient() {
		return tileServerClient;
	}
	
	/**
	 * @param offsetX
	 * @param offsetY
	 * @param width
	 * @param height
	 * @return 3D ImageSpace polygon
	 */
	protected Polygon getRectImageSpacePolygon(double offsetX, double offsetY, double width,
			double height) {
		Polygon polygon = GeometryUtils.GEOMETRY_FACTORY
				.createPolygon(new Coordinate[] {new Coordinate(offsetX, offsetY + height - 1.0, 0.0),
						new Coordinate(offsetX + width - 1.0, offsetY + height - 1.0, 0.0),
						new Coordinate(offsetX + width - 1.0, offsetY, 0.0), new Coordinate(offsetX, offsetY, 0.0),
						new Coordinate(offsetX, offsetY + height - 1.0, 0.0)});
		return polygon;
	}
	/**
	 * @param rset
	 * @return image space polyon
	 */
//	protected Polygon getImagePolygon(int rset) {
//		ImageScale rsetScale = ImageScale.forRset(rset);
//		Point2D normalizedPoint = normalize(new Point2D.Double(getR0ImageWidth(), getR0ImageHeight()));
//		Point2D scaledDownPoint = rsetScale.scaleDownToRset(normalizedPoint);
//		return getRectImageSpacePolygon(0, 0, scaledDownPoint.getX(), scaledDownPoint.getY());
//	}
	
	public long getSizeInBytes() {
		return CacheableUtil.getDefault().getSizeInBytesForObjects(cameraModel, tilePyramidDescriptor,
				tileserverUrlBuilder, tileServerClient);
	}


	@Override
	public boolean equals(Object obj) {
		boolean result = (this == obj);

		if (!result && TileserverImage.class.isInstance(obj)) {
			TileserverImage image = (TileserverImage) obj;
			result = toString().equals(image.toString());
		}
		return result;
	}

	@Override
	public String toString() {
		return tileserverUrlBuilder.getImageMetadataURL();
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Convert from an array of bytes to an array of shorts
	 * @param byteArray
	 */
	public static  short[] byteArrayToShortArray(byte[] byteArray) {

		if(byteArray.length % 2 != 0) {
			throw new IllegalArgumentException("Length of byteArray is not evenly divisible by 2, "
					+ "which is the number of bytes in one short.");
		}
		short shortArray[] = new short[byteArray.length / 2];
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		ShortBuffer shortBuff = byteBuf.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
		shortBuff.get(shortArray);
		return shortArray;
	}


	/**
	 * Decode image from input stream
	 */
	@Override
	public CoreImage imageDecode(InputStream tileInputStream) throws IOException {
		// Having intermittent problems with thread safety on ImageIO.read(), trying a lock
		//      BufferedImage image =  ImageIO.read(tileInputStream);
		BufferedImage image = ImageLoadUtils.getInstance().loadImage(tileInputStream);
		return new CoreImage(image);
	}

	public Rectangle getRsetImageRectangle(int rset) {
		Point2D rsetDimension = ImageScale.forRset(rset).scaleDownToRset(new Point2D.Double(getR0ImageWidth(), getR0ImageHeight()));
		return new Rectangle(0,0,(int)Math.floor(rsetDimension.getX() + 1E-5), (int)Math.floor(rsetDimension.getY()+1E-5));
	}
	
	public Rectangle getR0ImageRectangle() {
		return new Rectangle(0,0,getR0ImageWidth(), getR0ImageHeight());
	}

	public Polygon getR0ImageBounds() {
		final Rectangle r0Image = getR0ImageRectangle();
		final Polygon r0ImageBounds = GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(
						new double[][] {
							{r0Image.getMinX(), r0Image.getMinY()},
							{r0Image.getMaxX()-1, r0Image.getMinY()},
							{r0Image.getMaxX()-1, r0Image.getMaxY()-1},
							{r0Image.getMinX(), r0Image.getMaxY()-1},
							{r0Image.getMinX(), r0Image.getMinY()}
						}).map(c->new Coordinate(c[0], c[1], 0.0))
				.toArray(Coordinate[]::new));
		return r0ImageBounds;
	}
	
	public Polygon getGeodeticBounds() {
		final CameraModel camera = getCameraModel();
		final Polygon geodeticFootprint = GeometryUtils.GEOMETRY_FACTORY.createPolygon(
				Arrays.stream(getR0ImageBounds().getCoordinates())
				.map(c->camera.imageToWorld(GeometryUtils.toPoint2D(c)))
				.toArray(Coordinate[]::new));
		return geodeticFootprint;

	}
	public double getAngleOffNorth() {
		final Polygon imageGeoBounds = getGeodeticBounds();
		final Coordinate[] imageGeoBoundsCoords = imageGeoBounds.getCoordinates();

		Vector2dc[] imageGeoBoundsVecs = Arrays.stream(imageGeoBoundsCoords).map(c->GeometryUtils.toVector2d(c)).toArray(Vector2dc[]::new);
		final Vector2dc imageLLGeoVec = imageGeoBoundsVecs[0];
		final Vector2dc imageULGeoVec = imageGeoBoundsVecs[3];
		final Vector2dc northVec = new Vector2d(0.0, 90.0);
		final double angleOffNorth = northVec.angle(imageLLGeoVec.sub(imageULGeoVec, new Vector2d()));
		return angleOffNorth;
	}

}
