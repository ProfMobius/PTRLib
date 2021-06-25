package com.mia.craftstudio.minecraft;

import com.mia.craftstudio.CSModel.ModelNode;
import com.mia.craftstudio.CSModel.ModelNode.Attrb;
import com.mia.craftstudio.CSModel.Point;
import com.mia.craftstudio.libgdx.Vector3;
import com.mia.craftstudio.minecraft.CraftStudioModelWrapper.NodeWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class CraftStudioRendererVBO {
    private Set<CraftStudioRendererVBO> subRenderers = new HashSet<CraftStudioRendererVBO>();
    private int[] size = new int[3];   //Given in subblock (1 block == 16x16x16 subblocks)
    private Point[] textureUV;
    private int[] texSize = new int[2];
    private byte[] UVTransform = new byte[6];
    private boolean isTransparent;
    private ModelNode node;
    private NodeWrapper wrappedNode;
    private boolean ao;

    private enum UVTrans {
        Rotation_0,
        Rotation_90,
        Rotation_180,
        Rotation_270,
        Mirror_H,
        Mirror_V;

        public static EnumSet<UVTrans> getUVTransform(final int value) {
            final EnumSet<UVTrans> ret = EnumSet.noneOf(UVTrans.class);

            if ((value & 1) == 1)
                ret.add(Rotation_90);

            if ((value & 2) == 2)
                ret.add(Rotation_180);

            if ((value & 4) == 4)
                ret.add(Rotation_270);

            if (ret.isEmpty())
                ret.add(Rotation_0);

            if ((value & 8) == 8)
                ret.add(Mirror_H);

            if ((value & 16) == 16)
                ret.add(Mirror_V);

            return ret;
        }

        public static int getRotationIndex(final int value) {
            final EnumSet<UVTrans> transforms = UVTrans.getUVTransform(value);

            if (transforms.contains(Rotation_0))
                return 0;

            if (transforms.contains(Rotation_90))
                return 1;

            if (transforms.contains(Rotation_180))
                return 2;

            if (transforms.contains(Rotation_270))
                return 3;

            return -1;
        }

        public static int getMirrorIndex(final int value) {
            final EnumSet<UVTrans> transforms = UVTrans.getUVTransform(value);

            if (transforms.contains(Mirror_H) && transforms.contains(Mirror_V))
                return 3;

            if (transforms.contains(Mirror_V))
                return 2;

            if (transforms.contains(Mirror_H))
                return 1;

            return 0;
        }

    }

    public CraftStudioRendererVBO(final NodeWrapper wrappedNode) {
        this.wrappedNode = wrappedNode;
        this.node = wrappedNode.node;
        this.isTransparent = this.node.hasAttribute(Attrb.TRANSPARENT);
        this.ao = this.node.hasAttribute(Attrb.AMBIENTOCCLUSION);

        for (int i = 0; i < 3; i++) {
            this.size[i] = this.node.getSize()[i];
        }

        this.texSize[0] = this.node.getTexture().getWidth();
        this.texSize[1] = this.node.getTexture().getHeight();
        this.textureUV = this.node.getQuads();
        this.UVTransform = this.node.getUVTransform();

        for (final ModelNode node_ : this.node.getChildren()) {
            this.subRenderers.add(new CraftStudioRendererVBO(wrappedNode.modelWrapper.nodeCache.get(node_)));
        }
    }

    public ModelNode getNode() {
        return this.node;
    }

    /* ======================================
     * 	 __      ______   ____
     * 	 \ \    / /  _ \ / __ \
     * 	  \ \  / /| |_) | |  | |
     * 	   \ \/ / |  _ <| |  | |
     * 	    \  /  | |_) | |__| |
     * 	     \/   |____/ \____/
     *
     * ======================================
     */
	
	/*	Our cube. The numbering doesn't make much sense and is based on the output of CS. Might be interresting to actually order the vertices properly, but this is not necessary
	 * 
      1---5
     /|  /|
    0---4 |
    | 3-|-7
    |/  |/ 
    2---6
    
    Front  Right  Back   Left
    0---4  4---5  5---1  1---0
    |   |  |   |  |   |  |   |
    |   |  |   |  |   |  |   |
    2---6  6---7  7---3  3---2
    
    Top    Bot
    1---5  2---6
    |   |  |   |
    |   |  |   |
    0---4  3---7    

    STANDARD CS UVs

   0,0---16,0-.----.----.
    |    |    |    |    |
    |    | T  | Bo |    |
    |    |    |    |    |    
   0,16--16,16.----.----.
    |    |    |    |    |
    | L  | F  | R  | Ba |
    |    |    |    |    |    
   0,32--.----.----.----.     

   0,0--1,0--2,0--3,0--4,0
    |    |    |    |    |
    |    | T  | Bo |    |
    |    |    |    |    |
   0,1--1,1--2,1--3,1--4,1
    |    |    |    |    |
    | L  | F  | R  | Ba |
    |    |    |    |    |
   0,2--1,2--2,2--3,2--4,2

    CS indexes for rot 0:
    0 [ 16 16 ]	Front
    1 [ 48 16 ]	Back
    2 [ 32 16 ]	Right
    3 [ 32  0 ]	Bottom
    4 [  0 16 ]	Left
    5 [ 16  0 ]	Top
		
	CS indexes for rot 1 (90°):
    0 [ 16 48 ] Front
    1 [ 16 80 ] Back
    2 [ 16 64 ] Right
    3 [ 32 64 ] Bottom
    4 [ 16 32 ] Left
	5 [ 32 48 ] Top
    
    From internet, a correct order for triangle strips would be 
    ABCDBFDHFEHGEAGCEFABCDGH
    with 
    
    ABCD BFDH FEHG EAGC EFAB CDGH
    
	ABCD DBHF FEHG GECA AEBF FC CDGH
    2604 4657 7351 1302 2367 70 0415
    
      G---H
     /|  /|
    C---D |
    | E-|-F
    |/  |/ 
    A---B
    
    The corresponding UV table :
        // Front face
        0,0, 1,0, 0,1, 1,1,
        // Right face
        0,1, 0,0, 1,1, 1,0,
        // Back face
        0,0, 1,0, 0,1, 1,1,
        // Left face
        0,1, 0,0, 1,1, 1,0,
        // Bottom face
        0,1, 0,0, 1,1, 1,0,
       
        1,0, 0,0,
       
        // Top face
        0,0, 1,0, 0,1, 1,1    
    
	 */

    static final int[][] vertexLineStripsIndexes = {
            {0, 1, 3, 2, 0},
            {4, 5, 7, 6, 4},
            {1, 5, 7, 3, 1},
            {0, 4, 6, 2, 0}
    };

    static final int[][] vertexQuadsIndexes = {
            {0, 2, 6, 4},    //Front
            {5, 7, 3, 1},    //Back
            {4, 6, 7, 5},    //Right
            {2, 3, 7, 6},    //Bottom
            {1, 3, 2, 0},    //Left
            {1, 0, 4, 5},    //Top
    };

    static final int[][] vertexTriangleStripIndexes = {
            {2, 2}, {2, 6, 0, 4}, {4, 6, 5, 7}, {7, 3, 5, 1}, {1, 3, 0, 2}, {2, 3, 6, 7}, {7, 0}, {0, 4, 1, 5}, {5, 5}
            //  Dege Front       Right     Back       Left       Bottom     Degen  Top        Dege
    };


    static final int[][] UVPairs = {{0, 0}, {0, 1}, {1, 1}, {1, 0}}; // 0 : TL, 1 : BL, 2 : BR, 3 : TR
    static final int[][] UVSetDegen = {{0, 0}, {0, 0}, {0, 0}, {0, 0}};
    static final int[][][] UVSet = {
            {{1, 2, 0, 3}, {0, 1, 3, 2}, {3, 0, 2, 1}, {2, 3, 1, 0}},    // Orders for 0, 90, 180, 270, first set of coordinates  (F, Ba, T)
            {{0, 1, 3, 2}, {3, 0, 2, 1}, {2, 3, 1, 0}, {1, 2, 0, 3}},    // Orders for 0, 90, 180, 270, second set of coordinates (R, L, Bo)

            {{2, 1, 3, 0}, {1, 0, 2, 3}, {0, 3, 1, 2}, {3, 2, 0, 1}},    // Orders for 0, 90, 180, 270, first set of coordinates  (F, Ba, T), H Rotation // VERIFIED
            {{3, 2, 0, 1}, {2, 1, 3, 0}, {1, 0, 2, 3}, {0, 3, 1, 2}},    // Orders for 0, 90, 180, 270, second set of coordinates (R, L, Bo), H Rotation	// VERIFIED

            {{0, 3, 1, 2}, {3, 2, 0, 1}, {2, 1, 3, 0}, {1, 0, 2, 3}},    // Orders for 0, 90, 180, 270, first set of coordinates  (F, Ba, T), V Rotation // VERIFIED
            {{1, 0, 2, 3}, {0, 3, 1, 2}, {3, 0, 2, 1}, {2, 1, 3, 0}},    // Orders for 0, 90, 180, 270, second set of coordinates (R, L, Bo), V Rotation // VERIFIED

            {{3, 0, 2, 1}, {2, 3, 1, 0}, {1, 2, 0, 3}, {0, 1, 3, 2}},    // Orders for 0, 90, 180, 270, first set of coordinates  (F, Ba, T)
            {{2, 3, 1, 0}, {1, 2, 0, 3}, {0, 1, 3, 2}, {3, 0, 2, 1}}    // Orders for 0, 90, 180, 270, second set of coordinates (R, L, Bo)

    };

    static final int[][][][] vertexUVsCoordsTriangleStrip = {
            {UVSetDegen, UVSetDegen, UVSetDegen, UVSetDegen},  // Degen
            {UVSet[0], UVSet[2], UVSet[4], UVSet[6]},    // Front
            {UVSet[1], UVSet[3], UVSet[5], UVSet[7]},    // Right
            {UVSet[0], UVSet[2], UVSet[4], UVSet[6]},    // Back
            {UVSet[1], UVSet[3], UVSet[5], UVSet[7]},    // Left
            {UVSet[1], UVSet[3], UVSet[5], UVSet[7]},    // Bottom
            {UVSetDegen, UVSetDegen, UVSetDegen, UVSetDegen},  // Degen
            {UVSet[0], UVSet[2], UVSet[4], UVSet[6]},    // Top
            {UVSetDegen, UVSetDegen, UVSetDegen, UVSetDegen}   // Degen
    };

    static final int[] convertionTable = {0, 0, 2, 1, 4, 3, 5, 5, 5};    // Convert between the ordering of UV coordinates (F,Ba,R,Bo,L,T) and the triangle strip ordering (De,F,R,Ba,L,Bo,De,T,De)

    static final int[][][] rotationTransformTable = {
            {
                    //   X Coords adjustment
                    //	 -  H  V  B
                    {0, 1, 0, 1},    //0°
                    {1, 1, 0, 0},    //90°
                    {1, 0, 1, 0},    //180°
                    {0, 0, 1, 1}    //270°
            },
            {
                    //   Y Coords adjustment
                    //	 -  H  V  B
                    {0, 0, 1, 1},    //0°
                    {0, 1, 0, 1},    //90°
                    {1, 1, 0, 0},    //180°
                    {1, 0, 1, 0}    //270°
            }
    };

    @SideOnly(Side.CLIENT)
    private float[][] generateUVs() {
        final float[][] outArray = new float[6][4];
        final Point[] texUV = new Point[6];

        final int[][][] texOffsetsX = new int[][][]{
                {{0, size[0]}, {0, size[0]}, {0, size[2]}, {0, size[0]}, {0, size[2]}, {0, size[0]}},
                {{size[0], 0}, {size[0], 0}, {size[2], 0}, {size[0], 0}, {size[2], 0}, {size[0], 0}}
        };
        final int[][][] texOffsetsY = new int[][][]{
                {{0, size[1]}, {0, size[1]}, {0, size[1]}, {0, size[2]}, {0, size[1]}, {0, size[2]}},
                {{size[1], 0}, {size[1], 0}, {size[1], 0}, {size[2], 0}, {size[1], 0}, {size[2], 0}}
        };

        final int indexRot = UVTrans.getRotationIndex(this.UVTransform[0]);
        final int indexMir = UVTrans.getMirrorIndex(this.UVTransform[0]);

        final float[] scale = node.getScale();
        final int[] convertTable = new int[]{0, 1, 2, 3, 4, 5};
        int texInvertedX = 0;
        int texInvertedY = 0;
        if (scale[0] < 0) {
            texInvertedX = 1;
            convertTable[2] = 4;
            convertTable[4] = 2;
        }
        if (scale[1] < 0) {
            texInvertedY = 1;
            convertTable[3] = 5;
            convertTable[5] = 3;
        }
        if (scale[2] < 0) {
            convertTable[0] = 1;
            convertTable[1] = 0;
        }

        for (int i = 0; i < 6; i++) {
            final int index = convertTable[i];
            texUV[i] = new Point(textureUV[index].x - texOffsetsX[texInvertedX][index][1] * rotationTransformTable[0][indexRot][indexMir], textureUV[index].y - texOffsetsY[texInvertedY][index][1] * rotationTransformTable[1][indexRot][indexMir]);
        }

        for (int i = 0; i < 6; i++) {
            final float u1 = (float) (texUV[i].x + texOffsetsX[texInvertedX][i][0]) / (float) (this.texSize[0]);
            final float u2 = (float) (texUV[i].x + texOffsetsX[texInvertedX][i][1]) / (float) (this.texSize[0]);
            final float v1 = (float) (texUV[i].y + texOffsetsY[texInvertedY][i][0]) / (float) (this.texSize[1]);
            final float v2 = (float) (texUV[i].y + texOffsetsY[texInvertedY][i][1]) / (float) (this.texSize[1]);

            //outArray[i][0] = Math.min(u1, u2); //umin
            //outArray[i][1] = Math.max(u1, u2); //umax
            //outArray[i][2] = Math.min(v1, v2); //vmin
            //outArray[i][3] = Math.max(v1, v2); //vmax

            outArray[i][0] = u1; //umin
            outArray[i][1] = u2; //umax
            outArray[i][2] = v1; //vmin
            outArray[i][3] = v2; //vmax
        }

        return outArray;
    }

    @SideOnly(Side.CLIENT)
    public void generateVBOTriangleStripInterleaved(final FloatBuffer buffer, final int rot) {
        float r = 1f, g = 1f, b = 1f;

        if (this.node.hasAttribute(Attrb.COLORPRIMARY) && this.wrappedNode.modelWrapper.colorPrimary != null) {
            r = this.wrappedNode.modelWrapper.colorPrimary.getRed() / 255f;
            g = this.wrappedNode.modelWrapper.colorPrimary.getGreen() / 255f;
            b = this.wrappedNode.modelWrapper.colorPrimary.getBlue() / 255f;
        } else if (this.node.hasAttribute(Attrb.COLORSECONDARY) && this.wrappedNode.modelWrapper.colorSecondary != null) {
            r = this.wrappedNode.modelWrapper.colorSecondary.getRed() / 255f;
            g = this.wrappedNode.modelWrapper.colorSecondary.getGreen() / 255f;
            b = this.wrappedNode.modelWrapper.colorSecondary.getBlue() / 255f;
        } else if (this.node.hasAttribute(Attrb.COLORACCENTPRIMARY) && this.wrappedNode.modelWrapper.colorAccentPrimary != null) {
            r = this.wrappedNode.modelWrapper.colorAccentPrimary.getRed() / 255f;
            g = this.wrappedNode.modelWrapper.colorAccentPrimary.getGreen() / 255f;
            b = this.wrappedNode.modelWrapper.colorAccentPrimary.getBlue() / 255f;
        } else if (this.node.hasAttribute(Attrb.COLORACCENTSECONDARY) && this.wrappedNode.modelWrapper.colorAccentSecondary != null) {
            r = this.wrappedNode.modelWrapper.colorAccentSecondary.getRed() / 255f;
            g = this.wrappedNode.modelWrapper.colorAccentSecondary.getGreen() / 255f;
            b = this.wrappedNode.modelWrapper.colorAccentSecondary.getBlue() / 255f;
        }

        this.generateVBOTriangleStripInterleaved(buffer, rot, r, g, b);
    }

    @SideOnly(Side.CLIENT)
    public void generateVBOTriangleStripInterleaved(final FloatBuffer buffer, final int rot, final float r, final float g, final float b) {
        final Vector3[] vertices = this.wrappedNode.getVertices(rot);
        final float[][] UVs = this.generateUVs();
        final EnumSet<UVTrans> transforms = UVTrans.getUVTransform(this.UVTransform[0]);
        final int indexRot = UVTrans.getRotationIndex(this.UVTransform[0]);
        final int indexMir = UVTrans.getMirrorIndex(this.UVTransform[0]);

        for (int i = 0; i < vertexTriangleStripIndexes.length; i++) {
            final int[] vertexIndexes = vertexTriangleStripIndexes[i];

            // Normals computation
            final Vector3 v1;
            final Vector3 v2;
            if (vertexIndexes.length > 2) {
                v1 = new Vector3(vertices[vertexIndexes[1]].x, vertices[vertexIndexes[1]].y, vertices[vertexIndexes[1]].z)
                        .sub(new Vector3(vertices[vertexIndexes[0]].x, vertices[vertexIndexes[0]].y, vertices[vertexIndexes[0]].z));
                v2 = new Vector3(vertices[vertexIndexes[2]].x, vertices[vertexIndexes[2]].y, vertices[vertexIndexes[2]].z)
                        .sub(new Vector3(vertices[vertexIndexes[0]].x, vertices[vertexIndexes[0]].y, vertices[vertexIndexes[0]].z));
                v1.crs(v2).nor();
            } else {
                v1 = new Vector3(1.0f, 1.0f, 1.0f).nor();
            }

            final int[] verticesUVs = vertexUVsCoordsTriangleStrip[i][indexMir][indexRot];

            for (int j = 0; j < vertexIndexes.length; j++) {
                final Vector3 vertex = vertices[vertexIndexes[j]];
                final int vertexUV = verticesUVs[j];

                buffer.put(vertex.x).put(vertex.y).put(vertex.z);
                buffer.put(v1.x).put(v1.y).put(v1.z);
                buffer.put(UVPairs[vertexUV][0] == 0 ? UVs[convertionTable[i]][0] : UVs[convertionTable[i]][1])
                        .put(UVPairs[vertexUV][1] == 0 ? UVs[convertionTable[i]][2] : UVs[convertionTable[i]][3]);
                buffer.put(r).put(g).put(b);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void generateVBOLineStrip(final FloatBuffer buffer, final int rot) {
        final boolean isFlat = this.wrappedNode.isFlat();

        final Vector3[] vertices = this.wrappedNode.getVertices(rot);

        for (int i = 0; i < vertexLineStripsIndexes.length; i++) {
            final int[] vertexIndexes = vertexLineStripsIndexes[i];

            for (int j = 0; j < vertexIndexes.length; j++) {
                final Vector3 vertex = vertices[vertexIndexes[j]];

                if (!isFlat) {
                    buffer.put(vertex.x).put(vertex.y).put(vertex.z);
                } else {
                    buffer.put(vertices[0].x).put(vertices[0].y).put(vertices[0].z);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void collectRenderers(final Set<CraftStudioRendererVBO> renderers1st, final Set<CraftStudioRendererVBO> renderers2nd) {
        if (!this.isTransparent) {
            renderers1st.add(this);
        } else {
            renderers2nd.add(this);
        }

        for (final CraftStudioRendererVBO subRenderer : this.subRenderers) {
            subRenderer.collectRenderers(renderers1st, renderers2nd);
        }
    }

}
