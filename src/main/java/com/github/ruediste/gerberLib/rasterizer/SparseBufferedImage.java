package com.github.ruediste.gerberLib.rasterizer;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import com.github.ruediste.gerberLib.read.Polarity;

public class SparseBufferedImage {

	public static class SparseDataBuffer extends DataBuffer {
		private int tileSize;
		private int width;
		Map<Integer, byte[]> tiles = new HashMap<>();
		private int tilesPerLine;

		public SparseDataBuffer(int width, int height, int tileSize) {
			super(DataBuffer.TYPE_INT, width, height);
			this.width = width;
			this.tileSize = tileSize;
			this.tilesPerLine = (width / tileSize + 1);
		}

		byte[] getTile(int tileX, int tileY) {
			int key = tilesPerLine * tileY + tileX;
			return tiles.computeIfAbsent(key, x -> {
				byte[] result = new byte[tileSize * tileSize];
				return result;
			});
		}

		@Override
		public int getElem(int bank, int i) {
			int y = i / width;
			int x = i % width;

			int tileX = x / tileSize;
			int tileY = y / tileSize;

			return (getTile(tileX, tileY)[(y % tileSize) * tileSize + x % tileSize]) & 0xff;
		}

		@Override
		public void setElem(int bank, int i, int val) {
			int y = i / width;
			int x = i % width;

			int tileX = x / tileSize;
			int tileY = y / tileSize;

			getTile(tileX, tileY)[(y % tileSize) * tileSize + x % tileSize] = (byte) val;

		}

	}

	public BufferedImage image;
	public SparseDataBuffer buffer;
	private WritableRaster raster;

	public SparseBufferedImage(int width, int height, int tileSize) {

		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ComponentColorModel colorModel = new ComponentColorModel(cs, new int[] { 8 }, false, true, Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE);
		PixelInterleavedSampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height,
				1, width, new int[] { 0 });

		buffer = new SparseDataBuffer(width, height, tileSize);
		raster = new WritableRaster(sampleModel, buffer, new Point(0, 0)) {
		};
		this.image = new BufferedImage(colorModel, raster, false, null);

	}

	public void multiplyTo(BufferedImage target, Polarity polarity) {
		WritableRaster targetRaster = target.getRaster();

		buffer.tiles.forEach((tileIndex, tile) -> {
			int tileX = tileIndex % buffer.tilesPerLine;
			int tileY = tileIndex / buffer.tilesPerLine;

			int baseX = tileX * buffer.tileSize;
			int baseY = tileY * buffer.tileSize;

			byte valueToWrite = polarity == Polarity.DARK ? 0 : (byte) 255;

			byte[] elements = new byte[targetRaster.getNumDataElements()];
			for (int y = 0; y < buffer.tileSize; y++)
				for (int x = 0; x < buffer.tileSize; x++) {
					int value = tile[y * buffer.tileSize + x] & 0xff;
					if (value == 0)
						continue;

					targetRaster.getDataElements(baseX + x, baseY + y, elements);
					for (int i = 0; i < elements.length; i++) {
						elements[i] = valueToWrite;
					}
					targetRaster.setDataElements(baseX + x, baseY + y, elements);
				}
		});
	}

}
