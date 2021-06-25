package com.mia.craftstudio.minecraft.forge;

import com.mia.craftstudio.minecraft.AnimationManager;
import com.mia.craftstudio.minecraft.client.CSClientEventHandler;

public class ProxyClient extends ProxyCommon {

	@Override
	public void preInit() {
		// We load into memory some of the enums/singletons we are going to use later
		CSClientEventHandler.register();
		AnimationManager.INSTANCE.ordinal();
	};
}
