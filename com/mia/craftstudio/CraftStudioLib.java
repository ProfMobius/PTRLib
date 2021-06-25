package com.mia.craftstudio;

import java.awt.image.BufferedImage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mia.craftstudio.utils.Timer;

public enum CraftStudioLib {
	INSTANCE;

	// BEGIN **** CraftStudio Constants ****
	//Major version should always be compatible, minor just bugfixes
	public static final String CSLibVersion = "1.0.1"; //major.minor.build

	// currently targeted allowable version constants
	public static final byte version_model = 5;
	public static final byte version_animation = 3;
	public static final byte version_datapackage = 9;

	// allowable types that we can load
	public static final byte type_model = 0;
	public static final byte type_animation = 6;
	// END **** CraftStudio Constants ****

	private Timer timer = new Timer();
	private Logger log = LogManager.getLogger("CraftStudioLib");
	private boolean DEBUG =  Boolean.parseBoolean(System.getProperty("craftstudiolib.debug","false"));
	private BufferedImage defaultTexture; // Default texture in case there is some trouble

	public static Timer getTimer() {
		return INSTANCE.timer;
	}

	public static Logger getLog() {
		return INSTANCE.log;
	}

	public static void debug(final Object o) {
		if (INSTANCE.DEBUG) {
			INSTANCE.log.error(o.toString());
		}
	}

	public static BufferedImage getDefaultTexture() {
		if (INSTANCE.defaultTexture == null) {
			INSTANCE.defaultTexture = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < 512; x++) {
				for (int y = 0; y < 512; y++) {
					INSTANCE.defaultTexture.setRGB(x, y, (x % 2 + y % 2) * 255);
				}
			}
		}
		return INSTANCE.defaultTexture;
	}
}
