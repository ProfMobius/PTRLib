package com.mia.craftstudio.minecraft;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mia.craftstudio.CSModel;

// This is recommended to be used in conjunction with a gson loader of this data that pulls from the loaded json of the cspacks
public class ModelMetadata implements Comparable<ModelMetadata> {
	public String craftstudioAssetName;
	public String craftstudioPack;
	public int craftstudioAssetID;
	public float scale = 1.0f;
	public String textureOverride;
	public boolean faceCulling = true;

	public Map<String, String> tileParams = new LinkedHashMap<String, String>();

	public transient CSModel csmodel;
	// The wrapper will holds various MC specific data, isolating it from the model itself
	public transient CraftStudioModelWrapper wrapper;

	/**
	 *  This is called at the end of the wrapper constructor, in case there is any metadata
	 *   that need to be constructed after the wrapper(such as using extends computations)
	 */
	public void wrapperCallback() {}

	@Override
	public int compareTo(final ModelMetadata o) {
		if (this.craftstudioAssetID < o.craftstudioAssetID) {
			return -1;
		} else if (this.craftstudioAssetID < o.craftstudioAssetID) {
			return 1;
		}
		return 0;
	}
}
