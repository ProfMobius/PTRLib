package com.mia.craftstudio.minecraft.client;

import com.mia.craftstudio.libgdx.Vector3;
import com.mia.craftstudio.minecraft.CraftStudioRendererVBO;
import com.mia.craftstudio.minecraft.INodeProvider;
import com.mia.craftstudio.minecraft.ModelMetadata;
import com.mia.craftstudio.minecraft.forge.CSLibMod;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.regex.Pattern;

public class CSClientModelWrapperVariableVBO extends CSClientModelWrapperVBO {
    private Set<CraftStudioRendererVBO> staticNodes1st;
    private Set<CraftStudioRendererVBO> staticNodes2nd;

    private List<Pattern> staticFilters = new ArrayList<Pattern>();
    private List<Pattern> inventoryFilters = new ArrayList<Pattern>();

    private Map<String, RenderingObject> roMain = new HashMap<String, RenderingObject>();

    public CSClientModelWrapperVariableVBO(final ModelMetadata metadata) {
        super(metadata);

        for (final String s : metadata.tileParams.get("VarRend_STATIC").split(";")) {
            final String pattern = s.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            this.staticFilters.add(Pattern.compile(pattern));
        }

        for (final String s : metadata.tileParams.get("VarRend_INVENTORY").split(";")) {
            final String pattern = s.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            this.inventoryFilters.add(Pattern.compile(pattern));
        }
    }

    private Set<CraftStudioRendererVBO> computeNodesSet(final Set<CraftStudioRendererVBO> nodes, final List<Pattern> filter) {
        final Set<CraftStudioRendererVBO> outSet = new HashSet<CraftStudioRendererVBO>();
        for (final CraftStudioRendererVBO node : nodes) {
            for (final Pattern p : filter) {
                final String fullName = node.getNode().getFullName();
                if (p.matcher(fullName).matches()) {
                    outSet.add(node);
                }
            }
        }
        return outSet;
    }

    Set<CraftStudioRendererVBO> renderers1st = new HashSet<CraftStudioRendererVBO>();
    Set<CraftStudioRendererVBO> renderers2nd = new HashSet<CraftStudioRendererVBO>();

    @Override
    protected void precompute() {
        // We collect the renderers into 2 lists to render the 1st and 2nd passes and generate the VBOs
        for (final CraftStudioRendererVBO renderer : this.topRenderers) {
            renderer.collectRenderers(renderers1st, renderers2nd);
        }

        staticNodes1st = this.computeNodesSet(renderers1st, staticFilters);
        staticNodes2nd = this.computeNodesSet(renderers2nd, staticFilters);

        final Set<CraftStudioRendererVBO> inventoryNodes1st = this.computeNodesSet(renderers1st, inventoryFilters);
        final Set<CraftStudioRendererVBO> inventoryNodes2nd = this.computeNodesSet(renderers2nd, inventoryFilters);
        inventoryNodes1st.addAll(staticNodes1st);
        inventoryNodes2nd.addAll(staticNodes2nd);

        roDefault = new RenderingObject(this.generateVBOs(inventoryNodes1st, inventoryNodes2nd), new int[]{inventoryNodes1st.size(), inventoryNodes2nd.size()}, metadata.faceCulling);
        isPrecomputed = true;
    }

    @Override
    public void render(final Object instanceRenderState, final float timeSinceLastRender, final int pass, final boolean renderOutline, final float rotationAngle, final Vector3 rotationVector, final Vector3 preRotataionTranslation, final Vector3 postRotataionTranslation) {
        if (!isPrecomputed) {
            precompute();
        }

        GL11.glPushMatrix();

        if (!Vector3.Zero.equals(preRotataionTranslation))
            GL11.glTranslatef(preRotataionTranslation.x, preRotataionTranslation.y, preRotataionTranslation.z);
        if (!Vector3.Zero.equals(rotationVector))
            GL11.glRotatef(rotationAngle, rotationVector.x, rotationVector.y, rotationVector.z);
        if (!Vector3.Zero.equals(postRotataionTranslation))
            GL11.glTranslatef(postRotataionTranslation.x, postRotataionTranslation.y, postRotataionTranslation.z);

        if (instanceRenderState instanceof INodeProvider) {
            final INodeProvider tile = (INodeProvider) instanceRenderState;

            final String hash = tile.getStatusHash();
            if (hash == null) {
                return;
            }

            if (!roMain.containsKey(hash)) {
                final Set<CraftStudioRendererVBO> nodes1st = tile.getNodes(renderers1st); // TODO see about removing these to precompute?
                final Set<CraftStudioRendererVBO> nodes2nd = tile.getNodes(renderers2nd);
                nodes1st.addAll(staticNodes1st);
                nodes2nd.addAll(staticNodes2nd);

                roMain.put(hash, new RenderingObject(this.generateVBOs(nodes1st, nodes2nd), new int[]{nodes1st.size(), nodes2nd.size()}, metadata.faceCulling));
            }

            this.renderWithVBO(roMain.get(hash), instanceRenderState, timeSinceLastRender, pass);

            // Rendering of the block outline
            if (renderOutline && CSLibMod.displayOutline) {
                this.renderOutline(roMain.get(hash), instanceRenderState, timeSinceLastRender);
            }

        } else {
            this.renderWithVBO(roDefault, instanceRenderState, timeSinceLastRender, pass);
        }

        GL11.glPopMatrix();
    }
}
