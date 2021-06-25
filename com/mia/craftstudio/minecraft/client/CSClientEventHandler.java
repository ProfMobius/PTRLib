package com.mia.craftstudio.minecraft.client;

import com.mia.craftstudio.minecraft.AnimationManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public enum CSClientEventHandler {
	INSTANCE;

	public static void register() {
		FMLCommonHandler.instance().bus().register(INSTANCE);
	}

	@SubscribeEvent
	public void onRenderTick(final TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			AnimationManager.INSTANCE.update(event.renderTickTime);
		}
	}
}
