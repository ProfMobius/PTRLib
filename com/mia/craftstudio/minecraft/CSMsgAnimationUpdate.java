package com.mia.craftstudio.minecraft;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CSMsgAnimationUpdate implements IMessage {
	private IAnimatedTile propsTile;
	private AnimationState animationInfo;

	public CSMsgAnimationUpdate() {}
	public CSMsgAnimationUpdate(final IAnimatedTile tile, final AnimationState animationInfo) {
		this.propsTile = tile;
		this.animationInfo = animationInfo;
	}

	@Override
	public void fromBytes(final ByteBuf buf) {
		// Side is CLIENT, all animation updates come from server

		AnimationManager.addAnimationState(new BlockDimensionalPosition(buf), new AnimationState(buf));
	}

	@Override
	public void toBytes(final ByteBuf buf) {
		// Side is SERVER, sends all animation updates so they are synchronized between viewers

		propsTile.getBlockPosDim().write(buf);
		animationInfo.write(buf);
	}

	public static class Handler implements IMessageHandler<CSMsgAnimationUpdate, IMessage> {
		@Override
		public IMessage onMessage(final CSMsgAnimationUpdate message, final MessageContext ctx) {
			return null;
		}
	}
}
