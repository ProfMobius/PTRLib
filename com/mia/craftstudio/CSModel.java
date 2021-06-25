package com.mia.craftstudio;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.mia.craftstudio.CSModel.ModelNode.Attrb;
import com.mia.craftstudio.libgdx.Quaternion;
import com.mia.craftstudio.utils.ImageIOCS;

public class CSModel {
	private final byte  asset_type;
	private final short format_version;
	private final short next_unused_id;
	private final short node_count;
	private final Map<Short, ModelNode> nodes = new LinkedHashMap<Short, ModelNode>();
	private final Set<ModelNode> topNodes     = new HashSet<ModelNode>();
	private final int     texture_size;
	private final byte[]  texture_data;
	private final short   animation_count;
	private final short[] animation_asset_id;
	private final static String sep = ".";	
	private BufferedImage texture;
	private boolean hasTransparency   = false;
	private ModelNode root;
 
	
	// This is needed because animations refernces nodes by name, and move all same nodes at the same time.
	private final HashMap<String, ArrayList<Short>> asset_ids_for_name = new HashMap<String, ArrayList<Short>>();

	// CS normally only runs one animation at a time, multiple at once should work, but only so long as they do not touch the same pieces
	// This is a map of animations that move the same pieces, key is your anim id, return is list of anim ids that conflict
	private final HashMap<Short, ArrayList<Short>> animation_conflict_blacklist = new HashMap<Short, ArrayList<Short>>();

	public static class Point {
		public final int x, y;

		public Point(final ByteBuffer buffer) {
			this.x = buffer.getInt();
			this.y = buffer.getInt();
		}

		public Point(final int x, final int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static class ModelNode {
		private final short  id;
		private final short  parent_node_id;
		private final String name;
		private final float[] position    = new float[3];	//Relative position (to the parent) of the node in world coordinates (axis aligned to the world grid)
		private final float[] offset      = new float[3];	//Offset of the node post rotation (with local axis)
		private final float[] scale       = new float[3];	//Scaling of the node (never used so far ?)
		private final Quaternion orientation;				//Rotation quaternion (WXYZ)
		private final short[] size        = new short[3];	//Size of the node
		private final byte    unwrap;
		private final Point[] quads       = new Point[6];
		private final byte[]  uvtransform = new byte[6];

		private final Set<ModelNode> children = new HashSet<ModelNode>();
		private ModelNode parent  = null;
		private final CSModel csmodel;
		private EnumSet<Attrb> attributes = EnumSet.noneOf(Attrb.class);
		private boolean attributesParsed = false;
		private String fullName;
		
		public static enum Attrb{
			PASSABLE("p"),
			PASSABLEPROPAGATES("P", true),
			TRANSPARENT("2"),
			IGNOREBELOWYPLANE("y"),
			COLORPRIMARY("C"),
			COLORSECONDARY("c"),
			COLORACCENTPRIMARY("A"),
			COLORACCENTSECONDARY("a"),
			AMBIENTOCCLUSION("o"),
			;

			private final String controlChar;
			private final boolean propagates;

			private Attrb(final String controlChar) {
				this.controlChar = controlChar;
				this.propagates = false;
			}

			private Attrb(final String controlChar, final boolean propagates) {
				this.controlChar = controlChar;
				this.propagates = propagates;
			}

			public boolean hasAttribute(final String s) {
				return s.contains(this.controlChar);
			}

			public final boolean propagates() {
				return this.propagates;
			}
		}

		public ModelNode(final CSModel model, final ByteBuffer buffer) {
			this.csmodel        = model;
			this.id             = buffer.getShort();
			this.parent_node_id = buffer.getShort();

			final byte[] tmpName = new byte[buffer.get()];
			buffer.get(tmpName);
			this.name = new String(tmpName);
			this.fullName = this.getCleanName();

			for (int i = 0; i < 3; i++) {
				position[i] = buffer.getFloat();
			}

			for (int i = 0; i < 3; i++) {
				offset[i] = buffer.getFloat();
			}

			for (int i = 0; i < 3; i++) {
				scale[i] = buffer.getFloat();
			}

			orientation = new Quaternion(buffer);

			for (int i = 0; i < 3; i++) {
				size[i] = buffer.getShort();
			}

			// 0 : Collapsed
			// 1 : Full
			// 2 : Custom
			this.unwrap = buffer.get();

			for (int i = 0; i < 6; i++) {
				quads[i] = new Point(buffer);
			}

			for (int i = 0; i < 6; i++) {
				uvtransform[i] = buffer.get();
			}
		}

		private void parseAttributes() {
			if (this.attributesParsed) {
				return;
			}

			final String s = this.name.contains("$") ? this.name.split("\\$")[1] : "";

			for (final Attrb a : Attrb.values()) {
				boolean addAttrib = false;

				if (a.hasAttribute(s)) {
					addAttrib = true;
				} else if (a.propagates() && this.parent_node_id != -1) {
					final ModelNode parentNode = this.csmodel.nodes.get(this.parent_node_id);
					parentNode.parseAttributes(); // ensure the values are parsed before checking
					addAttrib = parentNode.hasAttribute(a);
				}

				if (addAttrib) {
					this.attributes.add(a);
				}
			}

			this.attributesParsed = true;
		}

		public boolean hasAttribute(final Attrb a) {
			return this.attributes.contains(a);
		}

		public Point getTextureOffset() {
			int x = Integer.MAX_VALUE;
			int y = Integer.MAX_VALUE;

			for (int i = 0; i < 6; i++) {
				x = Math.min(x, this.quads[i].x);
				y = Math.min(y, this.quads[i].y);
			}

			return new Point(x,y);
		}

		public String getName() {
			return this.name;
		}
		
		public String getCleanName(){
			return this.name.split("\\$")[0];
		}
		
		public String getFullName(){
			return this.fullName;
		}

		public void genFullName(){
			this.fullName = this.getCleanName();
			ModelNode currNode = this;
			
			while (true){
				if (currNode.getParent() != null){
					currNode = currNode.getParent();
					this.fullName = currNode.getCleanName() + sep + this.fullName;
				} else {
					break;
				}
			}			
		}
		
		public short[] getSize() {
			return this.size;
		}

		public float[] getSize(final float scale) {
			return new float[]{this.size[0] * scale, this.size[1] * scale, this.size[2] * scale};
		}

		public float[] getPosition() {
			return this.position;
		}

		public Quaternion getOrientation() {
			return this.orientation.cpy();
		}

		public short getParentNodeID() {
			return this.parent_node_id;
		}

		public short getNodeID() {
			return this.id;
		}

		public float[] getOffset() {
			return this.offset;
		}

		public float[] getScale() {
			return this.scale;
		}

		public Set<ModelNode> getChildren() {
			return this.children;
		}

		public ModelNode getParent() {
			return this.parent;
		}

		public void addChild(final ModelNode child) {
			this.children.add(child);
		}

		public void setParent(final ModelNode parent) {
			this.parent   = parent;
		}

		public CSModel getModel(){
			return this.csmodel;
		}

		public BufferedImage getTexture(){
			return this.csmodel.getTexture();
		}

		public Point[] getQuads(){
			return this.quads;
		}

		public boolean isNull(){
			return this.size[0] == 0 && this.size[1] == 0 && this.size[2] == 0;
		}

		// 0 : 0°
		// 1 : 90°
		// 2 : 180°
		// 4 : 270°
		public byte[] getUVTransform() {
			return this.uvtransform;
		}

		public boolean checkTransparency(final Raster alphaChannel){
			if (alphaChannel == null) return false;

			final int[][] values = new int[6][];
			try{
				values[0] = alphaChannel.getPixels(this.quads[0].x, this.quads[0].y, this.size[0], this.size[1], (int[])null);
				values[1] = alphaChannel.getPixels(this.quads[1].x, this.quads[1].y, this.size[0], this.size[1], (int[])null);
				values[2] = alphaChannel.getPixels(this.quads[2].x, this.quads[2].y, this.size[0], this.size[1], (int[])null);
				values[3] = alphaChannel.getPixels(this.quads[3].x, this.quads[3].y, this.size[0], this.size[2], (int[])null);
				values[4] = alphaChannel.getPixels(this.quads[4].x, this.quads[4].y, this.size[2], this.size[1], (int[])null);
				values[5] = alphaChannel.getPixels(this.quads[5].x, this.quads[5].y, this.size[0], this.size[2], (int[])null);

				for (final int[] pixels : values) {
					for (final int i : pixels)
						if (i != 0 && i != 255){
							this.attributes.add(Attrb.TRANSPARENT);
							return true;
						}
				}

			} catch (final ArrayIndexOutOfBoundsException e){
				//System.out.printf("%s : %d %d %d %d | %d %d\n", this.name, this.quads[0].x, this.quads[0].y, this.size[0], this.size[1], alphaChannel.getWidth(), alphaChannel.getHeight() );
			}

			return false;
		}

		@Override
		public String toString() {
			return String.format("CSMODEL %s | Position : [%.2f %.2f %.2f] | Size : [%d %d %d]", this.name,
					this.position[0], this.position[1], this.position[2],
					this.size[0], this.size[1], this.size[2]);
		}
	}

	public CSModel(final ByteBuffer buffer) throws CSExceptions.TypeMismatchException, CSExceptions.UnsupportedVersionException, IOException {
		this.asset_type = buffer.get();
		if (this.asset_type != CraftStudioLib.type_model) throw new CSExceptions.TypeMismatchException(CraftStudioLib.type_model, this.asset_type);

		this.format_version = buffer.getShort();
		if (this.format_version != CraftStudioLib.version_model) throw new CSExceptions.UnsupportedVersionException(this.format_version);

		this.next_unused_id = buffer.getShort();
		this.node_count     = buffer.getShort();

		for (int i = 0; i < this.node_count; i++) {
			final ModelNode node = new ModelNode(this, buffer);
			this.nodes.put(node.getNodeID(), node);
		}

		// Moved to here instead of in each node so we can check the node hierarchy for propagating attributes
		for (final ModelNode node : this.nodes.values()) {
			node.parseAttributes();
		}

		buffer.get((this.texture_data = new byte[(this.texture_size = buffer.getInt())]));

		this.animation_count = buffer.getShort();
		this.animation_asset_id = new short[this.animation_count];
		for (int i = 0; i < this.animation_count; i++) {
			this.animation_asset_id[i] = buffer.getShort();
		}

		//We reconstruct the hierarchy and determine if this model has transparencies
		for (final ModelNode node : this.nodes.values()) {
			if (node.getParentNodeID() == -1) {
				if (this.root == null && node.name.startsWith("RootNode")) {
					this.root = node;
				}
				this.topNodes.add(node);
			} else {
				final ModelNode parent = this.nodes.get(node.getParentNodeID());
				parent.addChild(node);
				node.setParent(parent);
			}
		}
		
		//Now that the hierarchy is proper, we generate full names for each node (in the form of Foo.Bar.Node01, with $ params cleaned up)
		for (final ModelNode node : this.nodes.values()) {
			node.genFullName();
		}

		// We generate the texture from the data
		//this.texture = ImageIO.read(new ByteArrayInputStream(this.texture_data));
		this.texture = ImageIOCS.read(new ByteArrayInputStream(this.texture_data));
		
		// We got a crash here at some point with an NPE. It might be due to a broken texture ?
		// https://www.irccloud.com/pastebin/nf5FlHNd/crash-2015-07-22_18.22.38-server.txt
		if (this.texture == null)
			this.texture = CraftStudioLib.getDefaultTexture();

		final Raster alphaChannel = this.texture.getAlphaRaster();
		for (final ModelNode node : this.nodes.values()) {
			node.checkTransparency(alphaChannel);
			this.hasTransparency = this.hasTransparency || node.hasAttribute(Attrb.TRANSPARENT);
		}
		
		for (final ModelNode node : this.nodes.values()) {
			if (!this.asset_ids_for_name.containsKey(node.getName())) {
				this.asset_ids_for_name.put(node.getName(), new ArrayList<Short>());
			}

			this.asset_ids_for_name.get(node.getName()).add(node.getNodeID());
		}

		// init animation blacklists to empty lists
		if (this.animation_count > 0) {
			for (final short animID : animation_asset_id) {
				this.animation_conflict_blacklist.put(animID, new ArrayList<Short>());
			}
		}
	}

	public BufferedImage getTexture() {
		return this.texture;
	}
	
	public byte[] getRawTexture() {
		return this.texture_data;
	}

	public Collection<ModelNode> getNodes() {
		return this.nodes.values();
	}

	public Collection<ModelNode> getTopNodes() {
		return this.topNodes;
	}

	public short getAnimationCount() {
		return this.animation_count;
	}

	public short[] getAnimationAssetIDS() {
		return this.animation_asset_id;
	}

	public boolean hasTransparency() {
		return this.hasTransparency;
	}

	public ModelNode getRootNode() {
		return this.root;
	}

	public void setupAnimationBlacklists(final CSProject project) {
		if (this.animation_count < 2)
			return; // Only need to parse for blocking anims if there is more than one anim for the model

		for (short test_anim = 0; test_anim < this.animation_count; test_anim++) {
			for (short target_anim = 0; target_anim < this.animation_count; target_anim++) {
				final Set<String> dupes = project.getAnimation((int)test_anim).getModelNamesAffected();
				dupes.retainAll(project.getAnimation((int)target_anim).getModelNamesAffected());
				if(!dupes.isEmpty()) {
					this.animation_conflict_blacklist.get(test_anim).add(target_anim);
					this.animation_conflict_blacklist.get(target_anim).add(test_anim);
				}
			}
		}
	}
}
