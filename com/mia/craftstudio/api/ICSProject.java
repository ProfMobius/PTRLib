package com.mia.craftstudio.api;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.gson.JsonElement;
import com.mia.craftstudio.CSModel;
import com.mia.craftstudio.CSModelAnim;
import com.mia.craftstudio.CSPack;
import com.mia.craftstudio.IPackReaderCallback;

/**
 * CSProject's are designed to hold all the CSPack files that are exported from
 * <strong>the same CraftStudio server</strong>.  This is because the internal CS ID
 * numbers are consistent only within the same server.  Adding packs from different
 * CS servers may result in ID collisions and overwritten models or animations.
 */
public interface ICSProject {

	// **********************************
	//   CSProject methods
	// **********************************

	/**
	 * Get a descriptor for a given asset ID
	 */
	public JsonElement getDescriptor(Integer id);

	/**
	 * Get project(subdirectory) name
	 */
	public String getName();

	/**
	 * Get name of overall container(mod) holding this project associated with the current project 
	 */
	public String getContainerName();

	/**
	 * Get the jar file for the current project, if available
	 */
	public File getJarFile();

	/**
	 * Get the project UniqueID ("containername:projectname")
	 */
	public String getProjectID();

	/**
	 * Get the project asset path on disk 
	 */
	public String getProjectAssetPath();


	// **********************************
	//   CraftStudio cspack methods
	// **********************************

	/**
	 * Add all the cspack files in the current project directory. 
	 * @return the ICSProject with all cspack files at the default project path ordered and added to the project load list
	 * @param callback
	 */
	public ICSProject addAllPacks(IPackReaderCallback callback);

	/**
	 * Clears all model and animation entries, then loads each added pack in this project's load list.
	 * @return the ICSProject
	 * @param modelListOverride
	 */
	public ICSProject loadPacks(InputStream overrideStream);

	/**
	 * Adds the individual named pack to the project's load list
	 * @return the ICSProject
	 */
	public ICSProject addPack(CSPack pack);

	/**
	 * Removes the first matching named pack from this project's pack list
	 * @return true if the pack existed in the project and was cleaned up 
	 */
	public boolean removePack(String name);

	/**
	 * Get all the cspacks associated with this project
	 * @return the mutable internal list of the CSProject
	 */
	public List<CSPack> getPacks();


	// **********************************
	//   Asset ID utility methods
	// **********************************

	/**
	 * Scans the pack collection to find the ID for the passed in name
	 *
	 * @param name the name to search for
	 * @return Integer ID number of the asset or -1 if not found
	 */
	public Integer findFirstIDfromName(String name);

	/**
	 * Scans the pack collection to find the ID for the passed in name
	 *
	 * @param name the name to search for
	 * @return A ordered list of ID's found that match the asset name given. Order is CSProject pack order(last is newest)
	 */
	public List<Integer> getIDsForName(String name);

	/**
	 * Scans the pack collection to find the name for the passed in ID
	 *
	 * @param id the Integer ID to search for
	 * @return the name of the asset or null if not found
	 */
	public String findNamefromID(Integer id);


	// **********************************
	//   Model methods
	// **********************************

	/**
	 * Get a bimap with all the models. The bipmap is a copy of the internal storage and can't be modified manually
	 */
	public BiMap<Integer, CSModel> getModels();

	/**
	 * Get a list of all the models that correspond to a given asset name. If you are sure there is only one model per name, you can use the method ICSProject.getModel(String name) which returns the first model.
	 * If not model is matched, returns an empty list.
	 */
	public List<CSModel> getModels(String name);

	/**
	 * Get a model based on CraftStudio internal ID
	 */
	public CSModel getModel(Integer id);	

	/**
	 * Get the first model matching the given name. More secure to use if we are sure there is only one model with a given name. If no model is matched, returns null
	 */
	public CSModel getModel(String name);

	/**
	 * Returns the number of models in the project.
	 */
	public int getModelCount();


	// **********************************
	//   Animation methods
	// **********************************

	/**
	 * Get all the animations in this project
	 */
	public Map<Integer, CSModelAnim> getAnimations();

	/**
	 * Get an animation by ID
	 */
	public CSModelAnim getAnimation(Integer id);

	/**
	 * Returns the number of animations in the project. 
	 */
	public int getAnimationCount();

}
