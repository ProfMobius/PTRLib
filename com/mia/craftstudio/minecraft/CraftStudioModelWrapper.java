package com.mia.craftstudio.minecraft;

import java.awt.Color;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.mia.craftstudio.CSModel.ModelNode;
import com.mia.craftstudio.CSModel.ModelNode.Attrb;
import com.mia.craftstudio.libgdx.Matrix4;
import com.mia.craftstudio.libgdx.Quaternion;
import com.mia.craftstudio.libgdx.Vector3;

public class CraftStudioModelWrapper { // split block stuff out to generic MC stuff away from what is *needed* to get the model to work?
	protected final ModelMetadata metadata;

	protected final Vector3[][] extend  = new Vector3[16][2];
	protected final int[][] extendBlock = new int[16][6];

	protected final Vector3[][] extendPlacement  = new Vector3[16][2];
	protected final int[][] extendPlacementBlock = new int[16][6];

	public Color colorPrimary;
	public Color colorSecondary;
	public Color colorAccentPrimary;
	public Color colorAccentSecondary;

	public static class NodeHashCache extends HashMap<ModelNode, NodeWrapper> {
		final CraftStudioModelWrapper modelWrapper;

		public NodeHashCache(final CraftStudioModelWrapper modelWrapper) {
			this.modelWrapper = modelWrapper;
		}

		@Override
		public NodeWrapper get(final Object key) {
			NodeWrapper ret = super.get(key);
			if (ret == null) {
				ret = new NodeWrapper((ModelNode)key, this.modelWrapper);
				this.put((ModelNode)key, ret);
			}
			return ret;
		}
	}

	public final NodeHashCache nodeCache = new NodeHashCache(this);

	public static class NodeWrapper {
		protected final ModelNode node;
		protected final CraftStudioModelWrapper modelWrapper;
		protected final boolean flat;

		protected Vector3[][] vertices = new Vector3[16][8];			// Absolute position of the vertices once all transforms have been applied
		protected Vector3[][] extend   = new Vector3[16][2];			// Minimum and maximum position of the vertices, useful to compute bounding boxes.

		public NodeWrapper(final ModelNode node, final CraftStudioModelWrapper modelWrapper) {
			this.node = node;
			this.modelWrapper = modelWrapper;
			final short[] size = node.getSize();
			this.flat = size[0] == 0 || size[1] == 0 || size[2] == 0;
		}

		public void computeVertices(final Matrix4 matrix, final int orient) { // per above added note, make this abstract and place into MC specific extension
			// WARNING : This computation is totally dependent on how MC do things. If we ever reuse this code anywhere,
			// we need to modify the rotations and translation for the new engine, otherwise everything here is basically BS

			final Matrix4 pushed = matrix.cpy();

			// First transformation is a translation to the center of the block
			final float[] scaledSize = this.node.getSize(1f/16f);

			// We move into position
			final Matrix4 position = new Matrix4();
			position.setTranslation(this.node.getPosition()[0], -this.node.getPosition()[1], -this.node.getPosition()[2]);
			pushed.mul(position);

			// We apply the Quaternion
			final Quaternion quat = this.node.getOrientation();
			quat.y *= -1;
			quat.z *= -1;
			pushed.mul(new Matrix4(quat));

			// The offset now
			final Matrix4 offset = new Matrix4();
			offset.setTranslation(this.node.getOffset()[0], this.node.getOffset()[1] * -1, this.node.getOffset()[2] * -1);
			pushed.mul(offset);

			// We apply the scaling. We instanciate a new matrix because scaling is specific to one node only.
			final Matrix4 mWorldScaling = pushed.cpy();
			final Matrix4 scaling = new Matrix4();
			scaling.setToScaling(Math.abs(this.node.getScale()[0]), Math.abs(this.node.getScale()[1]), Math.abs(this.node.getScale()[2]));	// We correct the vertices in case of negative scaling
			//scaling.setToScaling(this.node.getScale()[0], this.node.getScale()[1], this.node.getScale()[2]);	// We correct the vertices in case of negative scaling
			mWorldScaling.mul(scaling);

			// We can now compute the position of the various vertices
			this.vertices[orient][0] =  new Vector3(-scaledSize[0]/2f, -scaledSize[1]/2f, -scaledSize[2]/2f).mul(mWorldScaling); // -1, -1, -1
			this.vertices[orient][1] =  new Vector3(-scaledSize[0]/2f, -scaledSize[1]/2f,  scaledSize[2]/2f).mul(mWorldScaling); // -1, -1,  1
			this.vertices[orient][2] =  new Vector3(-scaledSize[0]/2f,  scaledSize[1]/2f, -scaledSize[2]/2f).mul(mWorldScaling); // -1,  1, -1
			this.vertices[orient][3] =  new Vector3(-scaledSize[0]/2f,  scaledSize[1]/2f,  scaledSize[2]/2f).mul(mWorldScaling); // -1,  1,  1
			this.vertices[orient][4] =  new Vector3( scaledSize[0]/2f, -scaledSize[1]/2f, -scaledSize[2]/2f).mul(mWorldScaling); //  1, -1, -1
			this.vertices[orient][5] =  new Vector3( scaledSize[0]/2f, -scaledSize[1]/2f,  scaledSize[2]/2f).mul(mWorldScaling); //  1, -1,  1
			this.vertices[orient][6] =  new Vector3( scaledSize[0]/2f,  scaledSize[1]/2f, -scaledSize[2]/2f).mul(mWorldScaling); //  1,  1, -1
			this.vertices[orient][7] =  new Vector3( scaledSize[0]/2f,  scaledSize[1]/2f,  scaledSize[2]/2f).mul(mWorldScaling); //  1,  1,  1

			this.extend[orient] = ProjectionHelper.getExtend(vertices[orient]);

			//XXX : This was an attempt at compressing models into one block. But it fails in too many ways
//			Vector3 vertexOffset = new Vector3();
//			if (this.extend[orient][0].x < 0.0f) {
//				vertexOffset.x = (float) Math.abs(Math.floor(this.extend[orient][0].x));
//			}
//
//			if (this.extend[orient][0].y < 0.0f) {
//				vertexOffset.y = (float) Math.abs(Math.floor(this.extend[orient][0].y));
//			}
//
//			if (this.extend[orient][0].z < 0.0f) {
//				vertexOffset.z = (float) Math.abs(Math.floor(this.extend[orient][0].z));
//			}
//
//			for (int i = 0; i < 8; i++) {
//				this.vertices[orient][i] = new Vector3((this.vertices[orient][i].x + vertexOffset.x) % 1.0f, (this.vertices[orient][i].y + vertexOffset.y) % 1.0f, (this.vertices[orient][i].z + vertexOffset.z) % 1.0f);
//			}
//
//			this.extend[orient] = ProjectionHelper.getExtend(vertices[orient]);

			for (final ModelNode child : this.node.getChildren()) {
				this.modelWrapper.nodeCache.get(child).computeVertices(pushed, orient);
			}
		}

		public Vector3[] getVertices(final int orient) {
			return this.vertices[orient];
		}

		public Vector3[] getExtend(final int orient) {
			return this.extend[orient];
		}

		public boolean isFlat() {
			return flat;
		}
	}


	// todo add extends/bounds stuff to this class

	public CraftStudioModelWrapper(final ModelMetadata metadata) {
		metadata.wrapper = this;
		this.metadata = metadata;

		for (int orient = 0; orient < 16; orient++) {
			this.computeExtend(orient);
			this.computeExtendBlock(orient);
		}

		metadata.wrapperCallback();
	}

	private void computeExtend(final int orient) {
		for (final ModelNode node : this.metadata.csmodel.getTopNodes()) {
			final Matrix4 matrix = new Matrix4();

			// We move the computation by 0.5,0.0,0.5 to simulate the "put in the middle of the cube" thing for the TE renderer
			final Matrix4 centerBlock = new Matrix4();
			centerBlock.translate(0.5f, 0.0f, 0.5f);
			matrix.mul(centerBlock);

			final Matrix4 scaleModel = new Matrix4();
			scaleModel.setToScaling(this.metadata.scale, this.metadata.scale, this.metadata.scale);
			matrix.mul(scaleModel);
			
			// We revert up and down to get the proper axis
			final Matrix4 invertAxe = new Matrix4();
			invertAxe.rotate(new Vector3(0,0,1), 180f);
			matrix.mul(invertAxe);

			final Matrix4 rotation = new Matrix4();
			rotation.rotate(new Vector3(0,1,0), orient * 360.0F / 16.0F);
			matrix.mul(rotation);

			this.nodeCache.get(node).computeVertices(matrix, orient);
		}

		this.extend[orient][0] = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.extend[orient][1] = new Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

		this.extendPlacement[orient][0] = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.extendPlacement[orient][1] = new Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

		// If the model doesn't show up in inventory, the targeting box is wrong or the extends are NaN,
		// it means that one of the nodes has a NaN value somewhere and need to be fixed on the model level.

		//for (ModelNode node : this.topNodes) {
		for (final ModelNode node : this.metadata.csmodel.getNodes()) {
			if (node.isNull()) continue;
			
			//XXX: Here, we are computing the whole extend based on full vertices. Doing it like that means that passable nodes will still prevent placement.
			// If we want passable nodes to not prevent placement, we have to compute another extend especially for placement by dropping passable nodes from the
			// extend computation. We shouldn't drop those from the main extend as it would influence too many thing from the engine.
			
			//ModelNode node = (ModelNode)(this.nodes.values().toArray()[0]);
			for (int i = 0; i < 8; i++) {
				this.extend[orient][0].x = Math.min(this.nodeCache.get(node).getVertices(orient)[i].x, this.extend[orient][0].x);
				this.extend[orient][1].x = Math.max(this.nodeCache.get(node).getVertices(orient)[i].x, this.extend[orient][1].x);

				this.extend[orient][0].y = Math.min(this.nodeCache.get(node).getVertices(orient)[i].y, this.extend[orient][0].y);
				this.extend[orient][1].y = Math.max(this.nodeCache.get(node).getVertices(orient)[i].y, this.extend[orient][1].y);

				this.extend[orient][0].z = Math.min(this.nodeCache.get(node).getVertices(orient)[i].z, this.extend[orient][0].z);
				this.extend[orient][1].z = Math.max(this.nodeCache.get(node).getVertices(orient)[i].z, this.extend[orient][1].z);
			}

			if (!node.hasAttribute(Attrb.PASSABLE) && !node.hasAttribute(Attrb.PASSABLEPROPAGATES)) {
				for (int i = 0; i < 8; i++) {
					this.extendPlacement[orient][0].x = Math.min(this.nodeCache.get(node).getVertices(orient)[i].x, this.extendPlacement[orient][0].x);
					this.extendPlacement[orient][1].x = Math.max(this.nodeCache.get(node).getVertices(orient)[i].x, this.extendPlacement[orient][1].x);

					this.extendPlacement[orient][0].y = Math.min(this.nodeCache.get(node).getVertices(orient)[i].y, this.extendPlacement[orient][0].y);
					this.extendPlacement[orient][1].y = Math.max(this.nodeCache.get(node).getVertices(orient)[i].y, this.extendPlacement[orient][1].y);

					this.extendPlacement[orient][0].z = Math.min(this.nodeCache.get(node).getVertices(orient)[i].z, this.extendPlacement[orient][0].z);
					this.extendPlacement[orient][1].z = Math.max(this.nodeCache.get(node).getVertices(orient)[i].z, this.extendPlacement[orient][1].z);
				}
			}
		}
	}

	private void computeExtendBlock(final int orient) {
		final float[] trunc = new float[] {
				((int)(extend[orient][0].x * 1000f)) / 1000f,
				((int)(extend[orient][0].y * 1000f)) / 1000f,
				((int)(extend[orient][0].z * 1000f)) / 1000f,
				((int)(extend[orient][1].x * 1000f)) / 1000f,
				((int)(extend[orient][1].y * 1000f)) / 1000f,
				((int)(extend[orient][1].z * 1000f)) / 1000f
		};

		this.extendBlock[orient][0] = (int)(Math.floor(trunc[0]));
		this.extendBlock[orient][1] = (int)(Math.floor(trunc[1]));
		this.extendBlock[orient][2] = (int)(Math.floor(trunc[2]));
		this.extendBlock[orient][3] = (int)(Math.ceil(trunc[3] - 1));
		this.extendBlock[orient][4] = (int)(Math.ceil(trunc[4] - 1));
		this.extendBlock[orient][5] = (int)(Math.ceil(trunc[5] - 1));


		final float[] truncPlacement = new float[] {
				((int)(extendPlacement[orient][0].x * 1000f)) / 1000f,
				((int)(extendPlacement[orient][0].y * 1000f)) / 1000f,
				((int)(extendPlacement[orient][0].z * 1000f)) / 1000f,
				((int)(extendPlacement[orient][1].x * 1000f)) / 1000f,
				((int)(extendPlacement[orient][1].y * 1000f)) / 1000f,
				((int)(extendPlacement[orient][1].z * 1000f)) / 1000f
		};

		this.extendPlacementBlock[orient][0] = (int)(Math.floor(truncPlacement[0]));
		this.extendPlacementBlock[orient][1] = (int)(Math.floor(truncPlacement[1]));
		this.extendPlacementBlock[orient][2] = (int)(Math.floor(truncPlacement[2]));
		this.extendPlacementBlock[orient][3] = (int)(Math.ceil(truncPlacement[3] - 1));
		this.extendPlacementBlock[orient][4] = (int)(Math.ceil(truncPlacement[4] - 1));
		this.extendPlacementBlock[orient][5] = (int)(Math.ceil(truncPlacement[5] - 1));

	}

	public Vector3[] getExtend(final int orient) {
		return this.extend[orient];
	}

	public int[] getExtendBlock(final int orient) {
		return this.extendBlock[orient];
	}

	public int[] getExtendPlacementBlock(final int orient) {
		return this.extendPlacementBlock[orient];
	}

	public ModelMetadata getMetadata(){
		return this.metadata;
	}

	public boolean canPlace(final World world, final int x, final int y, final int z, final EntityPlayer player, final int orient) {
		for (int lx = this.extendPlacementBlock[orient][0]; lx <= this.extendPlacementBlock[orient][3]; lx++) {
			for (int ly = (this.metadata.csmodel.getRootNode() != null && this.metadata.csmodel.getRootNode().hasAttribute(Attrb.IGNOREBELOWYPLANE) ? 0 : this.extendPlacementBlock[orient][1]);
				 ly <= this.extendPlacementBlock[orient][4]; ly++){

				for (int lz = this.extendPlacementBlock[orient][2]; lz <= this.extendPlacementBlock[orient][5]; lz++) {
					final IBlockState blockState = world.getBlockState(new BlockPos(lx + x, ly + y, lz + z));
					final Block targetBlock = blockState.getBlock();
					if (!((blockState.getMaterial() == Material.AIR)
							|| (targetBlock == Blocks.TALLGRASS)
							|| (targetBlock == Blocks.SNOW_LAYER)
						))
						return false;
				}
			}
		}
		return true;
	}
}
