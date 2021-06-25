package com.mia.craftstudio.minecraft;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;

/**
 * Interface for TileEntities that can provide their own nodes on request.
 * Used for variable rendering TEs
 */
public interface INodeProvider {
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getParam(String key);
	
	/**
	 * 
	 * @return Returns a set of directions that will be used to cache the current status of the TE. 
	 * The directions might be the surrounding TEs of the same time or whatever, as long as one set correspond to one rendering status.
	 */
	public String getStatusHash();
	
	/**
	 * 
	 * @param inNodes Full list of nodes for the model
	 * @return Filtered nodes based on the surrounding status
	 */
	@SideOnly(Side.CLIENT)
	public Set<CraftStudioRendererVBO> getNodes(Set<CraftStudioRendererVBO> inNodes);
}
