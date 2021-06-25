package com.mia.craftstudio.minecraft.forge;

import com.mia.craftstudio.minecraft.AnimationManager;

public class ProxyCommon {

	public void preInit() {
		// We load into memory some of the enums/singletons we are going to use later
		AnimationManager.INSTANCE.ordinal();
	};
}
