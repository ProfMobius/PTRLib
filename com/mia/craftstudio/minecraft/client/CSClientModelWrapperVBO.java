package com.mia.craftstudio.minecraft.client;

import com.mia.craftstudio.libgdx.Vector3;
import com.mia.craftstudio.minecraft.CraftStudioModelWrapper;
import com.mia.craftstudio.minecraft.CraftStudioRendererVBO;
import com.mia.craftstudio.minecraft.ModelMetadata;
import com.mia.craftstudio.minecraft.forge.CSLibMod;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;

public class CSClientModelWrapperVBO extends CraftStudioModelWrapper {
    public Set<CraftStudioRendererVBO> topRenderers = new HashSet<CraftStudioRendererVBO>();

    protected RenderingObject roDefault;

    // borrowed from AbstractTexture
    protected int glTextureId = -1;
    protected boolean isPrecomputed = false;

    protected class RenderingObject {
        private final int[][] vbos = new int[16][];
        private final int[][] elems = new int[16][];
        private final boolean faceCulling;

        public RenderingObject(final int[] vbos, final int[] sizes, final boolean faceCulling) {
            this.vbos[0] = vbos;
            this.elems[0] = sizes;
            this.faceCulling = faceCulling;
        }

        public RenderingObject(final int[] vbos, final int[] sizes, final int meta, final boolean faceCulling) {
            this.vbos[meta] = vbos;
            this.elems[meta] = sizes;
            this.faceCulling = faceCulling;
        }

        public int[] getVBOs() {
            return this.vbos[0];
        }

        public int getVBO1st() {
            return this.vbos[0][0];
        }

        public int getVBO2nd() {
            return this.vbos[0][1];
        }

        public int getVBOOut() {
            return this.vbos[0][2];
        }

        public int[] getSizes() {
            return this.elems[0];
        }

        public int getSize1st() {
            return this.elems[0][0];
        }

        public int getSize2nd() {
            return this.elems[0][1];
        }

        public int getSizeOut() {
            return this.elems[0][0] + this.elems[0][1];
        }

        public int[] getVBOs(final int meta) {
            return this.vbos[meta];
        }

        public int getVBO1st(final int meta) {
            return this.vbos[meta][0];
        }

        public int getVBO2nd(final int meta) {
            return this.vbos[meta][1];
        }

        public int getVBOOut(final int meta) {
            return this.vbos[meta][2];
        }

        public int[] getSizes(final int meta) {
            return this.elems[meta];
        }

        public int getSize1st(final int meta) {
            return this.elems[meta][0];
        }

        public int getSize2nd(final int meta) {
            return this.elems[meta][1];
        }

        public int getSizeOut(final int meta) {
            return this.elems[meta][0] + this.elems[meta][1];
        }

        public boolean getFaceCulling() {
            return faceCulling;
        }
    }

    public CSClientModelWrapperVBO(final ModelMetadata metadata) {
        super(metadata);
    }

    // Override the texture ID from an external source if we want to render a specific model with an alternative texture
    public void setGlTextureId(final int textureId) {
        this.glTextureId = textureId;
    }

    public int getGlTextureId() {
        if (this.glTextureId == -1) {
            this.glTextureId = TextureUtil.glGenTextures();
        }

        return this.glTextureId;
    }

    public void deleteGlTexture() {
        if (this.glTextureId != -1) {
            TextureUtil.deleteTexture(this.glTextureId);
            this.glTextureId = -1;
        }
    }

    public void bindGlTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getGlTextureId());
    }

    public void addRenderer(final CraftStudioRendererVBO renderer) {
        this.topRenderers.add(renderer);
    }

    public void renderPlacement(final EntityPlayer player, final float timeSinceLastRender, final boolean canPlace, final int tx, final int ty, final int tz, final int playerOrientation) {
        final double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * timeSinceLastRender;
        final double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * timeSinceLastRender;
        final double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * timeSinceLastRender;

        GL11.glPushMatrix();
        GL11.glTranslated(tx - px + 0.5d, ty - py, tz - pz + 0.5d);
        GL11.glRotatef((playerOrientation * 22.5F), 0, -1, 0); // rotate as requested
        GL11.glTranslatef(-0.5f, 0f, -0.5f);

        this.startOutlineRendering(3.0f, 1.0f, canPlace ? 1.0f : 0.0f, canPlace ? 1.0f : 0.0f, 0.5f);
        this.renderOutlineVBO(roDefault, null, timeSinceLastRender);
        this.stopOutlineRendering();

        GL11.glPopMatrix();
    }

    protected void renderOutline(final RenderingObject obj, final Object instanceRenderState, final float timeSinceLastRender) {
        GL11.glPushMatrix();
        this.startOutlineRendering(1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        this.renderOutlineVBO(obj, instanceRenderState, timeSinceLastRender);
        this.stopOutlineRendering();
        GL11.glPopMatrix();
    }

    private enum GLBlendStateManager {
        INSTANCE;

        private boolean oldState, savingState = false;
        private int oldSrcRGB, oldDstRGB, oldSrcAlpha, oldDstAlpha;

        /**
         * Saves current GL Blend state and returns if blending is currently enabled
         */
        static boolean saveState() {
            if (INSTANCE.savingState) {
                throw new RuntimeException("Already saving blend state!");
            }
            INSTANCE.savingState = true;

            INSTANCE.oldState = GL11.glGetBoolean(GL11.GL_BLEND);
            INSTANCE.oldSrcRGB = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            INSTANCE.oldDstRGB = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            INSTANCE.oldSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            INSTANCE.oldDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

            return INSTANCE.oldState;
        }

        static void restoreState() {
            if (!INSTANCE.savingState) {
                throw new RuntimeException("Not saving blend state!");
            }
            INSTANCE.savingState = false;

            if (!INSTANCE.oldState) {
                GL11.glDisable(GL11.GL_BLEND);
            } else {
                GL14.glBlendFuncSeparate(INSTANCE.oldSrcRGB, INSTANCE.oldDstRGB, INSTANCE.oldSrcAlpha, INSTANCE.oldDstAlpha);
            }
        }
    }

    protected void activateTransparency() {
        // We activate blending if it is actually useful
        if (!GLBlendStateManager.saveState()) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    protected void deactivateTransparency() {
        // and we deactivate it if it was activated
        GLBlendStateManager.restoreState();
        //GL11.glDisable(GL11.GL_NORMALIZE);
    }

    protected void precompute() {
        final Set<CraftStudioRendererVBO> renderers1st = new HashSet<CraftStudioRendererVBO>();
        final Set<CraftStudioRendererVBO> renderers2nd = new HashSet<CraftStudioRendererVBO>();

        // We collect the renderers into 2 lists to render the 1st and 2nd passes and generate the VBOs
        for (final CraftStudioRendererVBO renderer : this.topRenderers) {
            renderer.collectRenderers(renderers1st, renderers2nd);
        }

        roDefault = new RenderingObject(this.generateVBOs(renderers1st, renderers2nd), new int[]{renderers1st.size(), renderers2nd.size()}, metadata.faceCulling);
        isPrecomputed = true;
    }

    public void render(final Object instanceRenderState, final float timeSinceLastRender, final int pass, final boolean renderOutline, final float rotationAngle, final Vector3 rotationVector, final Vector3 preRotataionTranslation, final Vector3 postRotataionTranslation) {
        //f is partialTickTime from the MC world renderer, probably useful to keep, can scrap xyz,
        //scale is also probably no longer useful as it's accounted for in the VBO generation based
        //off of the metadata.  the rotation should probably be changed to a vec4 to allow any arbitray
        //rotation to be passed in to the renderer, but this may also be best handled before calling the
        //vbo renderer. so probably remove

        if (!isPrecomputed) {
            this.precompute();
        }

        GL11.glPushMatrix();

        if (!Vector3.Zero.equals(preRotataionTranslation))
            GL11.glTranslatef(preRotataionTranslation.x, preRotataionTranslation.y, preRotataionTranslation.z);
        if (!Vector3.Zero.equals(rotationVector))
            GL11.glRotatef(rotationAngle, rotationVector.x, rotationVector.y, rotationVector.z);
        if (!Vector3.Zero.equals(postRotataionTranslation))
            GL11.glTranslatef(postRotataionTranslation.x, postRotataionTranslation.y, postRotataionTranslation.z);

        // Rendering of the block outline
        if (CSLibMod.displayOutline && renderOutline) {
            this.renderOutline(roDefault, instanceRenderState, timeSinceLastRender);
        }

        this.renderWithVBO(roDefault, instanceRenderState, timeSinceLastRender, pass);

        GL11.glPopMatrix();
    }

    public void render(final Object instanceRenderState, final float timeSinceLastRender, final int pass, final boolean renderOutline, final FloatBuffer transformMatrix) {
        if (!isPrecomputed) {
            this.precompute();
        }

        if (transformMatrix != null) {
            GL11.glPushMatrix();
            GL11.glMultMatrix(transformMatrix);
        }

        if (CSLibMod.displayOutline && renderOutline) {
            this.renderOutline(roDefault, instanceRenderState, timeSinceLastRender);
        }

        this.renderWithVBO(roDefault, instanceRenderState, timeSinceLastRender, pass);

        if (transformMatrix != null) {
            GL11.glPopMatrix();
        }
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
    protected static final int nVertices = 30; // for triangle strips, the 4 vertices per side plus a degenerate triangle, so 5 vertices * 6 side = 30
    protected static final int sizeStride = 3 + 3 + 2 + 3; // 3 Pos, 3 Norm, 2 UV, 3 Color
    protected static final int sizeFloat = 4; // size in bytes for some array calcs

    protected int[] generateVBOs(final Set<CraftStudioRendererVBO> nodes1st, final Set<CraftStudioRendererVBO> nodes2nd) {
        final FloatBuffer qBuffer1st = BufferUtils.createFloatBuffer(sizeStride * nVertices * nodes1st.size());
        final FloatBuffer qBuffer2nd = BufferUtils.createFloatBuffer(sizeStride * nVertices * nodes2nd.size());
        final FloatBuffer outlineBuffer = BufferUtils.createFloatBuffer(3 * 5 * 4 * (nodes1st.size() + nodes2nd.size()));

        for (final CraftStudioRendererVBO renderer : nodes1st) {
            renderer.generateVBOTriangleStripInterleaved(qBuffer1st, 0);
            renderer.generateVBOLineStrip(outlineBuffer, 0);
        }

        for (final CraftStudioRendererVBO renderer : nodes2nd) {
            renderer.generateVBOTriangleStripInterleaved(qBuffer2nd, 0);
            renderer.generateVBOLineStrip(outlineBuffer, 0);
        }

        qBuffer1st.flip();
        qBuffer2nd.flip();
        outlineBuffer.flip();

        final int VBO1st = GL15.glGenBuffers();
        final int VBO2nd = GL15.glGenBuffers();
        final int VBOOutline = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO1st);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, qBuffer1st, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO2nd);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, qBuffer2nd, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBOOutline);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, outlineBuffer, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return new int[]{VBO1st, VBO2nd, VBOOutline};
    }

    //TODO, move this in to generic CSLib stuff, abstracted out to leave MC specifics here, etc..
    // ALso look in to checking OGL context, and if using OGL 3+, use VAO, else use the VBO as below(1.5t o 3.0)
    // also look in to trying to clean up memory usage a little: keep less things around once the vao/vbo have been made
    //for animation, check context again, and if using OGL 4+ use sep shader program, else if 2+ just use regular shader(for
    // this, might need to "backup" current shader state and restoreafter we used our shader, to account for mods possibly
    //doing full screen shaders as an ongoing shader vs a post processed shader.)


    protected void renderWithVBO(final RenderingObject obj, final Object instanceRenderState, final float timeSinceLastRender, final int pass) {
        final int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        this.bindGlTexture();

        final boolean CULL_FACE = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (obj.faceCulling) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }

        final boolean LIGHT1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_LIGHT1);

        final boolean ALPHA_TEST = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        final int[] vbos = obj.getVBOs();
        final int[] sizes = obj.getSizes();

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

        // Dirty trick. The main TE renderer can return 0 or 1 for the pass. But since 1.8.9, we can't determine for the inventory.
        // If we are rendering from outside the main loop, we use 2 for the pass and render both the normal layer and the transparent layer after that
        if (pass == 0 || pass == 2) {
            drawArrays(vbos[0], sizes[0]);
        }

        if ((pass == 1 || pass == 2) && sizes[1] > 0) {
            activateTransparency();
            drawArrays(vbos[1], sizes[1]);
            deactivateTransparency();
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);

        if (LIGHT1) GL11.glEnable(GL11.GL_LIGHT1);
        if (!CULL_FACE) GL11.glDisable(GL11.GL_CULL_FACE);
        if (!ALPHA_TEST) GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
    }

    protected void drawArrays(final int vbo, final int size) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, sizeStride * sizeFloat, 0);
        GL11.glNormalPointer(GL11.GL_FLOAT, sizeStride * sizeFloat, 3 * sizeFloat);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, sizeStride * sizeFloat, (3 + 3) * sizeFloat);
        GL11.glColorPointer(3, GL11.GL_FLOAT, sizeStride * sizeFloat, (3 + 3 + 2) * sizeFloat);

        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, nVertices * size);
    }

    protected void renderOutlineVBO(final RenderingObject obj, final Object instanceRenderState, final float timeSinceLastRender) {
        if (obj == null) return;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, obj.getVBOOut());
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);

        for (int i = 0; i < obj.getSizeOut() * 4; i++) {
            GL11.glDrawArrays(GL11.GL_LINE_STRIP, i * 5, 5);

        }
    }

    @SideOnly(Side.CLIENT)
    protected void startOutlineRendering(final float width, final float r, final float g, final float b, final float a) {
        if (!GLBlendStateManager.saveState()) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glLineWidth(width);
        GL11.glColor4f(r, g, b, a);
    }

    @SideOnly(Side.CLIENT)
    protected void stopOutlineRendering() {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GLBlendStateManager.restoreState();
    }
}
