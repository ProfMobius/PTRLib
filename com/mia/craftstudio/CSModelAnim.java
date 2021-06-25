package com.mia.craftstudio;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mia.craftstudio.libgdx.Quaternion;
import com.mia.craftstudio.libgdx.Vector3;

public class CSModelAnim {
	private final byte asset_type;  // Must be 6
	private final short format_version;  //  curent 3
	private final short animation_duration;
	private final boolean hold_last_keyframe;
	private final short node_count;
	private final HashMap<String, ModelNodeAnimation> nodes = new HashMap<String, ModelNodeAnimation>();

	private HashSet<String> affectedNodes = null;

	public CSModelAnim(final ByteBuffer buffer) throws CSExceptions.TypeMismatchException, CSExceptions.UnsupportedVersionException {
		this.asset_type = buffer.get();
		if (this.asset_type != CraftStudioLib.type_animation) throw new CSExceptions.TypeMismatchException(CraftStudioLib.type_animation, this.asset_type);

		this.format_version = buffer.getShort();
		if (this.format_version != CraftStudioLib.version_animation) throw new CSExceptions.UnsupportedVersionException(this.format_version);

		this.animation_duration	= buffer.getShort();
		this.hold_last_keyframe	= buffer.get() != 0 ? true: false;

		this.node_count = buffer.getShort();
		for (int i = 0; i < this.node_count; i++) {
			final ModelNodeAnimation node = new ModelNodeAnimation(buffer);
			this.nodes.put(node.getName(), node);
		}
	}

	public short getNode_count() {
		return node_count;
	}

	public HashMap<String, ModelNodeAnimation> getNodes() {
		return nodes;
	}
	public ModelNodeAnimation getNodeAffectingModel(final String modelName) {
		return nodes.get(modelName);
	}

	public Set<String> getModelNamesAffected() {
		return nodes.keySet();
	}

	public short getAnimationDuration() {
		return animation_duration;
	}

	public boolean isHoldingLastKeyframe() {
		return hold_last_keyframe;
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder(String.format("-=-=-=-=-\nCSModelAnimation: Duration[%d] HoldLastKeyframe[%b] NodeCount[%d] :\n", animation_duration, hold_last_keyframe, node_count));
		for (final Map.Entry<String, ModelNodeAnimation> entry : nodes.entrySet()) {
			ret.append(entry.getValue() + "\n");
		}
		return ret.toString();
	}

	public static class ModelNodeAnimation {
		private final String name;
		private final short position_keyframe_count;
		private final ArrayList<ModelNodeKeyFrame> position_keyframes = new ArrayList<ModelNodeKeyFrame>();
		private final short orientation_keyframe_count;
		private final ArrayList<ModelNodeKeyFrame> orientation_keyframes = new ArrayList<ModelNodeKeyFrame>();
		private final short block_size_keyframe_count;
		private final ArrayList<ModelNodeKeyFrame> block_size_keyframes = new ArrayList<ModelNodeKeyFrame>();
		private final short pivot_offset_keyframe_count;
		private final ArrayList<ModelNodeKeyFrame> pivot_offset_keyframes = new ArrayList<ModelNodeKeyFrame>();
		private final short scale_keyframe_count;
		private final ArrayList<ModelNodeKeyFrame> scale_keyframes = new ArrayList<ModelNodeKeyFrame>();

		public String getName() {
			return name;
		}

		public ArrayList<ModelNodeKeyFrame> getOrientationKeyframes() {
			return orientation_keyframes;
		}

		@Override
		public String toString() {
			final StringBuilder ret = new StringBuilder(String.format("ModelNodeAnimation for ModelNode[%s]:\n", name));

			if (position_keyframe_count > 0) {
				ret.append(String.format("\tPosition Keyframe count: %s\n", position_keyframe_count));
				for (final ModelNodeKeyFrame keyframe : position_keyframes) {
					ret.append("\t\t" + keyframe + "\n");
				}
			}
			if (orientation_keyframe_count > 0) {
				ret.append(String.format("\tOrientation Keyframe count: %s\n", orientation_keyframe_count));
				for (final ModelNodeKeyFrame keyframe : orientation_keyframes) {
					ret.append("\t\t" + keyframe + "\n");
				}
			}
			if (block_size_keyframe_count > 0) {
				ret.append(String.format("\tBlock Size Keyframe count: %s\n", block_size_keyframe_count));
				for (final ModelNodeKeyFrame keyframe : block_size_keyframes) {
					ret.append("\t\t" + keyframe + "\n");
				}
			}
			if (pivot_offset_keyframe_count > 0) {
				ret.append(String.format("\tPivot Offset Keyframe count: %s\n", pivot_offset_keyframe_count));
				for (final ModelNodeKeyFrame keyframe : pivot_offset_keyframes) {
					ret.append("\t\t" + keyframe + "\n");
				}
			}
			if (scale_keyframe_count > 0) {
				ret.append(String.format("\tScale Keyframe count: %s\n", scale_keyframe_count));
				for (final ModelNodeKeyFrame keyframe : scale_keyframes) {
					ret.append("\t\t" + keyframe + "\n");
				}
			}

			return ret.toString();
		}

		public ModelNodeAnimation(final ByteBuffer buffer) {
			final byte[] tmpName = new byte[buffer.get()];
			buffer.get(tmpName);
			this.name = new String(tmpName);

			this.position_keyframe_count = buffer.getShort();
			for (int i = 0; i < this.position_keyframe_count; i++) {
				this.position_keyframes.add(new ModelNodeKeyFrame(KeyFrameType.Position, buffer));
			}

			this.orientation_keyframe_count = buffer.getShort();
			for (int i = 0; i < this.orientation_keyframe_count; i++) {
				this.orientation_keyframes.add(new ModelNodeKeyFrame(KeyFrameType.Orientation, buffer));
			}

			this.block_size_keyframe_count = buffer.getShort();
			for (int i = 0; i < this.block_size_keyframe_count; i++) {
				this.block_size_keyframes.add(new ModelNodeKeyFrame(KeyFrameType.BlockSize, buffer));
			}

			this.pivot_offset_keyframe_count = buffer.getShort();
			for (int i = 0; i < this.pivot_offset_keyframe_count; i++) {
				this.pivot_offset_keyframes.add(new ModelNodeKeyFrame(KeyFrameType.PivotOffset, buffer));
			}

			this.scale_keyframe_count = buffer.getShort();
			for (int i = 0; i < this.scale_keyframe_count; i++) {
				this.scale_keyframes.add(new ModelNodeKeyFrame(KeyFrameType.Scale, buffer));
			}
		}
	}

	public static class ModelNodeKeyFrame {
		private final KeyFrameType type;

		private final short key_time_index;
		private final byte interpolation_mode; // currently ignored
		private final Vector3 vector;
		private final Quaternion quaternion;

		public final FloatBuffer quatbuffer;

		public ModelNodeKeyFrame(final KeyFrameType frameType, final ByteBuffer buffer) {
			this.type = frameType;
			this.key_time_index = buffer.getShort();
			this.interpolation_mode = buffer.get();

			switch (frameType) {
				case Orientation:
					vector = null;
					quaternion = new Quaternion(buffer);
					final float[] quatmatrix = new float[16];
					quaternion.y *= -1;
					quaternion.z *= -1;
					quaternion.toMatrix(quatmatrix);
					this.quatbuffer = FloatBuffer.allocate(16);
					this.quatbuffer.put(quatmatrix);
					this.quatbuffer.flip();
					break;
				case BlockSize:
					vector = new Vector3(buffer.getInt(), buffer.getInt(), buffer.getInt());
					quaternion = null;
					quatbuffer = null;
					break;
				default:
					vector = new Vector3(buffer);
					quaternion = null;
					quatbuffer = null;
					break;
			}
		}

		public byte getInterpolationMode() { return interpolation_mode; }
		public short getKeyTimeIndex() { return key_time_index; }
		public KeyFrameType getType() { return type; }
		public Vector3 getVector() { return vector; }
		public Quaternion getQuaternion() { return quaternion.cpy(); }

		@Override
		public String toString() {
			return String.format("Keyframe: Type[%s] Index[%s] Mode[%s] Vector[%s] Quaternion[%s]", type.name(), key_time_index, interpolation_mode, vector, quaternion);
		}
	}

	public static enum KeyFrameType {
		Position, // Vector XYZ
		Orientation, // Quarternion WXYZ
		BlockSize, // Vector XYZ
		PivotOffset, // Vector XYZ
		Scale; // Vector XYZ
	}
}
