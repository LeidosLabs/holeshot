package com.leidoslabs.holeshot.imaging.nitf;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.codice.imaging.nitf.core.image.ImageMode;
import org.codice.imaging.nitf.core.image.ImageSegment;

import it.geosolutions.imageio.plugins.jp2k.JP2KKakaduImageReader;
import it.geosolutions.imageio.plugins.jp2k.JP2KKakaduImageReaderSpi;

/**
 * TileReader for UncompressedNITF
 */
public class NITFJP2KCompressedTileReader extends NITFTileReader {
	private static final int BYTE_MASK = 0xFF;
	private static final int START_OF_IMAGE = (short)0xFFD8;
	private BufferedImage image;

	public NITFJP2KCompressedTileReader(final ImageSegment imageSegment) throws IOException {
		super(imageSegment);
		ImageReader reader = new JP2KKakaduImageReaderSpi().createReaderInstance();
		
		ImageInputStream imageInputStream = getImageSegment().getData();
		imageInputStream.seek(0);
		skipToMarker(imageInputStream, START_OF_IMAGE);
		reader.setInput(imageInputStream);
		this.image = reader.read(0);
		reader.dispose();
	}


	@Override
	protected Raster readRaster(int tileX, int tileY) throws IOException {
		int width = (this.tileXToX(tileX + 1) - this.tileXToX(tileX));
		int height = (this.tileYToY(tileY + 1) - this.tileYToY(tileY));
		int x = this.tileXToX(tileX), y = this.tileYToY(tileY);
		width = x + width >= image.getWidth()? image.getWidth() - x: width;
		height = y + height >= image.getHeight()? image.getHeight() - y: height;
		Rectangle rect = new Rectangle(x, y, width, height);
		return image.getData(rect);
	}

	private void skipToMarker(final ImageInputStream imageInputStream, final int markerCode) throws IOException {
		imageInputStream.mark();
		byte fillByte = (byte) ((markerCode >> Byte.SIZE) & BYTE_MASK);
		byte markerByte = (byte) (markerCode & BYTE_MASK);

		int i = 0;
		byte a = imageInputStream.readByte();

		while (a == fillByte) {
			i++;
			a = imageInputStream.readByte();
		}

		imageInputStream.reset();

		if (a == markerByte) {
			imageInputStream.skipBytes(i - 1);
		}
	}
}
