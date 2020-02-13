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
import java.awt.geom.Rectangle2D;
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

import org.apache.commons.lang3.NotImplementedException;
import org.image.common.cache.CacheableUtil;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
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

	private static final String IMAGE_CRS_WKT = "IMAGE_CRS_WKT";
	private static final String RASTER_TO_MODEL_WKT = "RASTER_TO_MODEL_WKT";
	private static int READ_RETRIES = 3;
	private CameraModel cameraModel;
	private TilePyramidDescriptor tilePyramidDescriptor;
	private TileserverUrlBuilder tileserverUrlBuilder;
	private GeometryFactory geometryFactory;
	private GeometryUtils geometryUtils;
	private final TileServerClient tileServerClient;
	private TileRef topTile;

	private final double maxImageDimension;
	private final Vector2d openGLOffset;

	
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

		if (tilePyramidDescriptor == null) {
			throw new FileNotFoundException(String.format("Can't find metadata for %s", imageMetadataURL.toString()));
		}
		this.geometryFactory = new GeometryFactory();
		this.geometryUtils = new GeometryUtils(this.geometryFactory);

		buildCameraFromMetadata();

		final int maxRset = getTilePyramidDescriptor().getMaxRLevel();
		this.topTile = new TileRef(this, maxRset, 0, 0);
		//    maxImageDimension = Math.max(getR0ImageWidth(), getR0ImageHeight());
		maxImageDimension = Math.max(tilePyramidDescriptor.getTileWidth(),tilePyramidDescriptor.getTileHeight()) * Math.pow(2.0, maxRset);

		Rectangle fullTopTile = topTile.getFullR0RectInImageSpace();
		openGLOffset = new Vector2d(1.0 - (double)getR0ImageWidth() / fullTopTile.getWidth(),
				(double)getR0ImageHeight()/fullTopTile.getHeight() - 1.0);

		LOGGER.debug("openGLOffset == " + openGLOffset.toString());
	}
	

	public Vector2d getOpenGLOffset() {
		return openGLOffset;
	}

	public String getImageMetadataURL() {
		return tileserverUrlBuilder.getImageMetadataURL();
	}

	public String getCollectionID() {
		return tileserverUrlBuilder.getCollectionID();
	}

	public String getTimestamp() {
		return tileserverUrlBuilder.getTimestamp();
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
	public Point2D normalize(Point2D src) {
		return new Point2D.Double(normalize(src.getX()), normalize(src.getY()));
	}
	/**
	 * @param src
	 * @return maps points from [0,1] -> [0, maxImageDimension]
	 */
	public Point2D deNormalize(Point2D src) {
		return new Point2D.Double(deNormalize(src.getX()), deNormalize(src.getY()));
	}
	
	/**
	 * @param src
	 * @return normalized point between [0, 1] based off maxImageDimension
	 */
	public double normalize(double src) {
		return src/maxImageDimension;

	}
	
	/**
	 * @param src
	 * @return maps points from [0,1] -> [0, maxImageDimension]
	 */
	public double deNormalize(double src) {
		return src*maxImageDimension;
	}
	
	
	/**
	 * @param imageCoord
	 * @return imagespace -> [-1, 1]
	 */
	public Vector2d imageToOpenGL(Vector2d imageCoord) {
		Vector2d result = new Vector2d(normalize(imageCoord.x) * 2.0 - 1.0, (1.0 - normalize(imageCoord.y)) * 2.0 - 1.0).add(openGLOffset);
		return result;
	}
	/**
	 * @param imageCoord
	 * @return imagespace -> [-1, 1]
	 */
	public Coordinate imageToOpenGL(Coordinate imageCoord) {
		return GeometryUtils.toCoordinate(imageToOpenGL(GeometryUtils.toVector2d(imageCoord)));
	}
	/**
	 * @param openGLCoord
	 * @return [-1, 1] -> imagespace
	 */
	public Vector2d openGLToImage(Vector2d openGLCoord) {
		Vector2d adjustedCoord = openGLCoord.sub(openGLOffset);
		return new Vector2d(deNormalize((adjustedCoord.x + 1.0)/ 2.0), deNormalize(1.0-(adjustedCoord.y + 1.0)/2.0));
	}
	
	/**
	 * @param openGLCoord
	 * @return [-1, 1] imagespace
	 */
	public Coordinate openGLToImage(Coordinate openGLCoord) {
		return GeometryUtils.toCoordinate(openGLToImage(GeometryUtils.toVector2d(openGLCoord)));
	}

	/**
	 * @param imageRect
	 * @return imagespace -> [-1, 1]
	 */
	public Rectangle2D.Double imageToOpenGL(Rectangle imageRect) {
		Vector2d llImage = new Vector2d(imageRect.getMinX(), imageRect.getMinY());
		Vector2d llOpenGL = imageToOpenGL(llImage);

		Vector2d urImage = new Vector2d(imageRect.getMaxX(), imageRect.getMaxY());
		Vector2d urOpenGL = imageToOpenGL(urImage);

		return new Rectangle2D.Double(llOpenGL.x, llOpenGL.y, urOpenGL.x() - llOpenGL.x(), urOpenGL.y() - llOpenGL.y());
	}

	/**
	 * @param imageSpacePoly
	 * @return imagespace -> [-1, 1]
	 */
	public Polygon imageToOpenGL(Polygon imageSpacePoly) {
		return geometryFactory.createPolygon(Arrays.stream(imageSpacePoly.getCoordinates()).map(this::imageToOpenGL).toArray(Coordinate[]::new));
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
		Polygon polygon = geometryFactory
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
	protected Polygon getImagePolygon(int rset) {
		ImageScale rsetScale = ImageScale.forRset(rset);
		Point2D normalizedPoint = normalize(new Point2D.Double(getR0ImageWidth(), getR0ImageHeight()));
		Point2D scaledDownPoint = rsetScale.scaleDownToRset(normalizedPoint);
		return getRectImageSpacePolygon(0, 0, scaledDownPoint.getX(), scaledDownPoint.getY());
	}

	public GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}


	public TileRef getTopTile() {
		return topTile;
	}

	
	/**
	 * Retrieves tile data for each band and merges
	 */
	public CoreImage getTileserverTile(TileRef tileRef) throws IOException {
		final int numBands = getNumBands();

		CoreImage[] bands = IntStream.range(0, Math.min(3, numBands)).parallel()
				.mapToObj(i -> getTileserverTileForBand(tileRef.getForBand(i))).toArray(CoreImage[]::new);

		CoreImage tileserverTile = null;
		if (Arrays.stream(bands).noneMatch(b->b == null)) {
			final CoreImage alpha = new CoreImage(AWTImageTools.makeAlphaChannelFor(bands[0].getBufferedImage(), 1.0f));
			if (numBands > 0 && numBands < 3) {
				tileserverTile = mergeBands(new CoreImage[] { bands[0], bands[0], bands[0], alpha});
			} else {
				tileserverTile = mergeBands(new CoreImage[] { bands[0], bands[1], bands[2], alpha});
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
	protected CoreImage getTileserverTileForBand(TileRef tileRef) {
		CoreImage result = null;
		try {
			result = TileserverTileCache.getInstance().getTileserverTileForBand(this, tileRef);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return result;
	}

	protected int getRset(int level) {
		return getMaxRLevel() - level;
	}

	/**
	 * Get worldwind tile key
	 * @param row
	 * @param column
	 * @param tileLevel
	 * @return
	 */
	private String getWorldwindTileKey(int row, int column, int tileLevel) {
		return String.format("%s/%s/%d/%d/%d", getCollectionID(), getTimestamp(), tileLevel, row,
				column);
	}

	
	protected CoreImage buildWorldwindTile(int row, int column, int tileWidth,
			int tileHeight, int tileLevel, Envelope tileBBox) throws IOException {
		throw new NotImplementedException("buildWorldwindTile not implemented at this time");
	}

	private void buildCameraFromMetadata() throws IOException {
		//      try {
		final Map<String, Object> meta = getMetadata();
		cameraModel = RPCCameraModelFactory.buildRPCCameraFromMetadata(meta);
		//         if (cameraModel == null) {
		//            final String imageCRSWKT = (String)meta.get(IMAGE_CRS_WKT);
		//            final String rasterToModelWKT = (String)meta.get(RASTER_TO_MODEL_WKT);
		//
		//            if (imageCRSWKT != null && rasterToModelWKT != null) {
		//               final CoordinateReferenceSystem imageCRS = CRS.parseWKT(imageCRSWKT);
		//               final MathTransform rasterToModel = new DefaultMathTransformFactory().createFromWKT(rasterToModelWKT);
		//
		//               cameraModel = new MapProjectionCamera(rasterToModel, imageCRS, DefaultGeographicCRS.WGS84);
		//
		//               LOGGER.debug("referencePoint == " + cameraModel.getReferencePoint().toString());
		//            }
		//         }
		//      } catch (FactoryException e) {
		//         throw new IOException(e);
		//      } catch (NoninvertibleTransformException e) {
		//         // TODO Auto-generated catch block
		//         e.printStackTrace();
		//      }
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

	public long getSizeInBytes() {
		return CacheableUtil.getDefault().getSizeInBytesForObjects(cameraModel, tilePyramidDescriptor,
				tileserverUrlBuilder, geometryFactory, tileServerClient);
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

	public Rectangle getR0ImageRectangle() {
		return new Rectangle(0,0,getR0ImageWidth(), getR0ImageHeight());
	}

}
