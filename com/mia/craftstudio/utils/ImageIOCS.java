package com.mia.craftstudio.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.sun.imageio.plugins.png.PNGImageReader;

import gnu.trove.map.TDoubleFloatMap;
import gnu.trove.map.hash.TDoubleFloatHashMap;

public class ImageIOCS {

	private static final IIORegistry theRegistry = IIORegistry.getDefaultInstance();	
	private static final ImageReader pngReader = new PNGImageReader(null);
	private static final int[] mipmapBuffer = new int[4];;

	public static BufferedImage read(final InputStream input) throws IOException {
		final ImageInputStream stream = new MemoryCacheImageInputStream(input);
		pngReader.setInput(stream, true, true);
		final BufferedImage bi = pngReader.read(0, pngReader.getDefaultReadParam());
		pngReader.dispose();
		stream.close();    	
		return bi;
	}

	public static int[][] generateMipmapData(final int mipmapLevels, final int textureWidth, final int[][] inImages) {
		final int[][] outImages = new int[mipmapLevels + 1][];
		outImages[0] = inImages[0];

		if (mipmapLevels > 0) {
			boolean isTransparent = false;
			int level;

			for (level = 0; level < inImages.length; ++level) {
				if (inImages[0][level] >> 24 == 0) {
					isTransparent = true;
					break;
				}
			}

			for (level = 1; level <= mipmapLevels; ++level) {
				if (inImages[level] != null) {
					outImages[level] = inImages[level];
				} else {
					final int[] previousLevelImage = outImages[level - 1];
					final int[] newCurrentLevelImage = new int[previousLevelImage.length >> 2];
					final int currentLevelTextureWidth = textureWidth >> level;
					final int currentLevelTextureHeight = newCurrentLevelImage.length / currentLevelTextureWidth;
					final int previousLevelTextureWidth = currentLevelTextureWidth << 1;

					for (int x = 0; x < currentLevelTextureWidth; ++x) {
						for (int y = 0; y < currentLevelTextureHeight; ++y) {
							final int i2 = 2 * (x + y * previousLevelTextureWidth);
							newCurrentLevelImage[x + y * currentLevelTextureWidth] = blendColors(previousLevelImage[i2 + 0], previousLevelImage[i2 + 1], previousLevelImage[i2 + 0 + previousLevelTextureWidth], previousLevelImage[i2 + 1 + previousLevelTextureWidth], isTransparent);
						}
					}

					outImages[level] = newCurrentLevelImage;
				}
			}
		}

		return outImages;
	}

	private static int blendColors(final int pixel1, final int pixel2, final int pixel3, final int pixel4, final boolean isTransparent) {
		if (!isTransparent) {
			return blendColorComponent(pixel1, pixel2, pixel3, pixel4, 24) << 24 | blendColorComponent(pixel1, pixel2, pixel3, pixel4, 16) << 16 | blendColorComponent(pixel1, pixel2, pixel3, pixel4, 8) << 8 | blendColorComponent(pixel1, pixel2, pixel3, pixel4, 0);
		} else {
			mipmapBuffer[0] = pixel1;
			mipmapBuffer[1] = pixel2;
			mipmapBuffer[2] = pixel3;
			mipmapBuffer[3] = pixel4;
			float alphaComponent = 0.0F;
			float redComponent   = 0.0F;
			float greenComponent = 0.0F;
			float blueComponent  = 0.0F;

			for (int pixel = 0; pixel < 4; ++pixel) {
				if (mipmapBuffer[pixel] >> 24 != 0) {
					alphaComponent += cachedPow22ComponentValue[mipmapBuffer[pixel] >> 24 & 255];
					redComponent   += cachedPow22ComponentValue[mipmapBuffer[pixel] >> 16 & 255];
					greenComponent += cachedPow22ComponentValue[mipmapBuffer[pixel] >>  8 & 255];
					blueComponent  += cachedPow22ComponentValue[mipmapBuffer[pixel] >>  0 & 255];                	
				}
			}

			int alphaValue = (int)(fastPow045(alphaComponent * 0.25F) * 255.0D);
			final int redValue   = (int)(fastPow045(redComponent   * 0.25F) * 255.0D);
			final int greenValue = (int)(fastPow045(greenComponent * 0.25F) * 255.0F);
			final int blueValue  = (int)(fastPow045(blueComponent  * 0.25F) * 255.0F);

			if (alphaValue < 96) {
				alphaValue = 0;
			}

			return alphaValue << 24 | redValue << 16 | greenValue << 8 | blueValue;
		}
	}

	private static int blendColorComponent(final int pixel1, final int pixel2, final int pixel3, final int pixel4, final int componentShift) {
		return (int)(fastPow045((cachedPow22ComponentValue[pixel1 >> componentShift & 255] + cachedPow22ComponentValue[pixel2 >> componentShift & 255] + cachedPow22ComponentValue[pixel3 >> componentShift & 255] + cachedPow22ComponentValue[pixel4 >> componentShift & 255]) * 0.25F) * 255.0F);
	}

	private static float fastPow045(final double val) {
		if (!cachedPow045.containsKey(val))
			cachedPow045.put(val, (float)FastMath.pow(val, 0.45454545454545453D)); // 5/11
		return cachedPow045.get(val);    	
	}

	// TODO: expiriment with using timed google collections here to empty map out after X time to save memory. need to make sure performance is still good though; reasoning: this data is only needed on resource pack loading, and rarely elsewise. a timer of a few minutes should work
	static float[] cachedPow22ComponentValue = new float[256];
	static TDoubleFloatMap cachedPow045 = new TDoubleFloatHashMap();

	static {
		for (int i = 0; i < 256; i++) {
			cachedPow22ComponentValue[i] = (float)(FastMath.pow((i / 255.0F), 2.2D));
		}
	}
}
