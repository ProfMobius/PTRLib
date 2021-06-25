package com.mia.craftstudio.minecraft;

import io.netty.buffer.ByteBuf;

import com.mia.craftstudio.CSModelAnim;
import com.mia.craftstudio.CSProject;
import com.mia.craftstudio.api.ICSProject;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class AnimationState {
	public int animationID;
	public long startTime; //sys milliseconds
	public int repeatCount; // 0 to run once, -1 to look forever, otherwise only repeats X times
	public boolean holdsLastKeyframe; // if this is set, this Animation is not removed from the active animation list when it's done

	public float frameTime = 0;
	public int lastFullFrameRendered = 0;
	public boolean running = true; // logic shortcutter for rendering, set to false when last keyframe is held and anim is not looping

	public final CSModelAnim animation;

	public final ICSProject project; // Project this animation is associated with, mainly used for network comms


	public AnimationState(final ICSProject project, final int id, final long time, final int count, final boolean holdFrame) {
		this.animationID = id;
		this.startTime = time;
		this.repeatCount = count;
		this.holdsLastKeyframe = holdFrame;
		this.project = project;
		this.animation = project.getAnimation(id);
	}

	public AnimationState(final ByteBuf buf) {
		this.animationID = buf.readInt();
		this.startTime = buf.readLong();
		this.repeatCount = buf.readInt();
		this.holdsLastKeyframe = buf.readBoolean();
		this.project = CSProject.getProjectFromID(ByteBufUtils.readUTF8String(buf));
		this.animation = this.project.getAnimation(this.animationID);
	}

	public void write(final ByteBuf buf) {
		buf.writeInt(animationID);
		buf.writeLong(startTime);
		buf.writeInt(repeatCount);
		buf.writeBoolean(holdsLastKeyframe);
		ByteBufUtils.writeUTF8String(buf, this.project.getProjectID());
	}
}
