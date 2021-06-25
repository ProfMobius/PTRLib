package com.mia.craftstudio;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mia.craftstudio.CSExceptions.ResourcesNotFoundException;
import com.mia.craftstudio.api.ICSProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * CSProject's are designed to hold all the CSPack files that are exported from
 * <strong>the same CraftStudio server</strong>.  This is because the internal CS ID
 * numbers are consistent only within the same server.  Adding packs from different
 * CS servers may result in ID collisions and overwritten models or animations.
 */
public class CSProject implements ICSProject {
    private static HashMap<String, CSProject> projectsList = new HashMap<String, CSProject>();

    private final List<CSPack> packs = new ArrayList<CSPack>();
    private final List<String> jsons = new ArrayList<String>();
    private final Map<Integer, CSModel> models = new HashMap<Integer, CSModel>();
    private final Map<Integer, CSModelAnim> animations = new HashMap<Integer, CSModelAnim>();
    private final Map<Integer, JsonElement> descriptors = new HashMap<Integer, JsonElement>();
    private final Multimap<String, CSModel> modelsMM = HashMultimap.create();
    private final String projectName;
    private final String projectContainer;
    private final String projectID;
    private final String projectAssetPath;
    private final File jarFile;

    public CSProject(final String name, final String container, final String assetPath, final File jarFile) {
        this.projectID = String.format("%s:%s", container, name);
        if (projectsList.containsKey(this.projectID)) {
            throw new CSExceptions.DuplicateProjectException(this.projectID);
        } else {
            projectsList.put(this.projectID, this);
        }
        this.projectName = name;
        this.projectContainer = container;
        this.projectAssetPath = assetPath;
        this.jarFile = jarFile;
    }

    /**
     * Find a CSProject by unique project id lookup
     *
     * @param projectID String in format of "container:name" of the project to find in the master list
     * @return the CSProject or null if not found
     */
    public static ICSProject getProjectFromID(final String projectID) {
        return projectsList.get(projectID);
    }

    @Override
    public String getName() {
        return this.projectName;
    }

    @Override
    public String getContainerName() {
        return this.projectContainer;
    }

    @Override
    public String getProjectID() {
        return this.projectID;
    }

    @Override
    public File getJarFile() {
        return this.jarFile;
    }

    @Override
    public String getProjectAssetPath() {
        return this.projectAssetPath;
    }

    @Override
    public String toString() {
        if (this.jarFile != null)
            return String.format("CSProject[%s] located in File: %s", this.projectID, this.jarFile.toString());
        else
            return String.format("CSProject[%s]", this.projectID);
    }

    private Set<String> getResourceList(String path) throws ResourcesNotFoundException {
        final Set<String> filenames = new HashSet<String>();

        try {
            final URL directory = CSProject.class.getResource(path);

            if (directory != null && "file".equals(directory.getProtocol())) {
                // Real file, probably dev environment or unpacked resource, just load and return list
                filenames.addAll(Arrays.asList(new File(directory.toURI()).list()));
            } else if (this.jarFile != null && this.jarFile.isFile()) {
                // packaged assets in jarfile
                path = path.substring(1) + "/"; // change path format over to the syle used in zipfiles

                // TODO: might need to revist this for MC resource packs
                final ZipFile zipfile = new ZipFile(jarFile);

                final Enumeration<? extends ZipEntry> entryList = zipfile.entries();

                while (entryList.hasMoreElements()) {
                    final ZipEntry zipEntry = entryList.nextElement();
                    if (zipEntry.getName().equals(path)) {
                        continue; // skip the directory
                    }
                    if (zipEntry.getName().startsWith(path)) {
                        filenames.add(zipEntry.getName().substring(path.length()));
                    }
                }

                zipfile.close();
            } else
                throw new CSExceptions.ResourcesNotFoundException(this);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final CSExceptions.ResourcesNotFoundException e) {
            throw e;
        }

        return filenames;
    }

    @Override
    public ICSProject addAllPacks(final IPackReaderCallback callback) {
        this.packs.clear();
        List<String> orderedPacks = null;

        orderedPacks = new ArrayList<String>(getResourceList(this.projectAssetPath));

        CraftStudioLib.debug(String.format("Looking up CSPacks for %s, unordered list", this));
        for (final String s : orderedPacks) {
            CraftStudioLib.debug(String.format("\t+ %s", s));
        }

        // Order such that nondated items are listed first, then dated items in oldest order
        Collections.sort(orderedPacks, new Comparator<String>() {
            private boolean isIntNumber(final String str) {
                for (final char c : str.toCharArray()) {
                    if (!Character.isDigit(c)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public int compare(final String name1, final String name2) {
                try {
                    final int sep1 = name1.indexOf('_');
                    final int sep2 = name2.indexOf('_');
                    final String date1 = name1.substring(0, sep1);
                    final String date2 = name2.substring(0, sep2);

                    if (isIntNumber(date1)) {
                        if (isIntNumber(date2)) {
                            final int result = Long.valueOf(date1).compareTo(Long.valueOf(date2));
                            if (result == 0)
                                return name1.substring(sep1).toLowerCase(Locale.US).compareTo(name2.substring(sep2).toLowerCase(Locale.US));
                            return result;
                        }
                        return 1;
                    } else if (isIntNumber(date2)) return -1;
                } catch (final Exception e) {
                    // IndexOutOfBoundsException, NumberFormatException
                    // If there's an error parsing the number value, then just skip this and return regular string compare below
                }

                return name1.toLowerCase(Locale.US).compareTo(name2.toLowerCase(Locale.US));
            }
        });

        CraftStudioLib.debug(String.format("Loading CSPacks for %s, ordered list: ", this));
        for (final String pack : orderedPacks) {
            CraftStudioLib.debug(String.format("\t+ %s", pack));
            if (pack.endsWith(".cspack")) {
                this.addPack(new CSPack(pack, callback));
            }

            if (pack.endsWith(".json")) {
                this.jsons.add(pack);
            }
        }

        return this;
    }


    @Override
    public ICSProject addPack(final CSPack pack) {
        this.packs.add(pack);
        pack.project = this;
        return this;
    }

    @Override
    public boolean removePack(final String name) {
        for (final CSPack pack : packs) {
            if (pack.getName().equals(name)) {
                packs.remove(pack.cleanup());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<CSPack> getPacks() {
        return this.packs;
    }

    /**
     * Clears all model and animation entries, then loads each added pack.
     *
     * @param overrideStream
     * @return this for chaining
     */
    @Override
    public ICSProject loadPacks(final InputStream overrideStream) {
        models.clear();
        modelsMM.clear();
        animations.clear();
        for (final CSPack pack : Arrays.copyOf(packs.toArray(new CSPack[0]), packs.size())) {
            try {
                pack.readPackFile(overrideStream);
            } catch (final CSExceptions.NoCSProjectException e) {
                // No pack should trigger this unless we wrote something wrong, or someone
                // reflected to add a pack instead of using addPack() above
                System.out.printf("Removing CSPack '%s' from CSProject '%s'. It was improperly added.", pack.getName(), this.projectName);
                packs.remove(pack.cleanup());
            }
        }

        for (final CSPack pack : packs) {
            final BiMap<Integer, CSPack.ProjectEntry> entries = pack.getEntries();
            for (final CSPack.ProjectEntry entry : entries.values()) {
                if (entry.getType() == CraftStudioLib.type_model) {
                    modelsMM.put(entry.getName(), this.models.get(entry.getID()));
                }
            }
        }

        CraftStudioLib.debug(String.format("%s", this));
        CraftStudioLib.debug(String.format("\t+ %d models", models.size()));
        CraftStudioLib.debug(String.format("\t+ %d animations", animations.size()));

        return this;
    }

    @Override
    public Integer findFirstIDfromName(final String name) {
        final List<Integer> ret = getIDsForName(name);
        return ret.size() > 0 ? ret.get(0) : -1;
    }

    @Override
    public List<Integer> getIDsForName(final String name) {
        final ArrayList<Integer> ret = new ArrayList<Integer>();
        for (final CSPack pack : packs) {
            final BiMap<Integer, CSPack.ProjectEntry> entries = pack.getEntries();

            for (final CSPack.ProjectEntry entry : entries.values()) {
                if (entry.getName().equals(name)) {
                    ret.add(entries.inverse().get(entry));
                }
            }
        }
        return ret;
    }

    @Override
    public String findNamefromID(final Integer id) {
        for (final CSPack pack : packs) {
            final BiMap<Integer, CSPack.ProjectEntry> entries = pack.getEntries();
            if (entries.containsKey(id))
                return entries.get(id).getName();
        }
        return null;
    }

    @Override
    public BiMap<Integer, CSModel> getModels() {
        return ImmutableBiMap.copyOf(this.models);
    }

    @Override
    public List<CSModel> getModels(final String name) {
        return this.modelsMM.get(name).size() > 0 ? ImmutableList.copyOf(this.modelsMM.get(name)) : new ArrayList<CSModel>();
    }

    @Override
    public CSModel getModel(final Integer id) {
        return this.models.get(id);
    }

    @Override
    public CSModel getModel(final String name) {
        return this.modelsMM.get(name).size() > 0 ? this.getModels(name).get(0) : null;
    }

    @Override
    public int getModelCount() {
        return this.models.size();
    }

    @Override
    public Map<Integer, CSModelAnim> getAnimations() {
        return ImmutableMap.copyOf(this.animations);
    }

    @Override
    public CSModelAnim getAnimation(final Integer id) {
        return this.animations.get(id);
    }

    @Override
    public int getAnimationCount() {
        return this.animations.size();
    }

    @Override
    public JsonElement getDescriptor(final Integer id) {
        return this.descriptors.get(id);
    }

    // Below should be called inside package only

    /**
     * Associates the passed in {@link CSModel} with the specified ID
     *
     * @param id    id number of the model in this project collection
     * @param model CSModel to add to this project collection
     * @return true if this overwrote a previously mapped model
     */
    boolean addModel(final Integer id, final CSModel model) {
        return this.models.put(id, model) != null;
    }

    /**
     * Associates the passed in {@link CSModelAnim} with the specified ID
     *
     * @param id   id number of the animation in this project collection
     * @param anim CSModelAnim to add to this project collection
     * @return true if this overwrote a previously mapped animation
     */
    boolean addAnimation(final Integer id, final CSModelAnim anim) {
        return this.animations.put(id, anim) != null;
    }

    /**
     * Associates the passed in {@link JsonElement} with the specified asset ID
     *
     * @param id   id number of the asset in this project collection
     * @param json root JsonElement of a json array with descriptors for this asset, it
     *             is left up to the project to determine how to use this.
     * @return true if this overwrote a previously mapped asset descriptor
     */
    boolean addDescriptor(final Integer id, final JsonElement json) {
        return this.descriptors.put(id, json) != null;
    }
}
