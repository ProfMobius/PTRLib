package com.mia.craftstudio;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CSPack {
    private final Map<Integer, ProjectEntry> entries = new HashMap<Integer, ProjectEntry>();
    private final String name;
    private byte version;
    private static final Logger LOGGER = LogManager.getLogger();

    transient CSProject project = null;

    public CSPack(final String name, final IPackReaderCallback callback) {
        this.name = name;
        this.callback = callback;
    }

    public String getName() {
        return name;
    }

    public byte getVersion() {
        return version;
    }

    public BiMap<Integer, ProjectEntry> getEntries() {
        return ImmutableBiMap.copyOf(entries);
    }

    private final IPackReaderCallback callback;

    // Package access below

    public static class ProjectEntry {
        private final boolean folder;
        private final short id;
        private final short parent_id;
        private final String name;
        private final byte type;
        private final boolean locked;

        public ProjectEntry(final ByteBuffer buffer) {
            this.folder = buffer.get() != 0 ? true : false;
            this.id = buffer.getShort();
            this.parent_id = buffer.getShort();

            final byte[] tmpName = new byte[buffer.get()];
            buffer.get(tmpName);
            this.name = new String(tmpName);

            this.type = buffer.get();
            this.locked = buffer.get() != 0 ? true : false;

            if (!this.folder) {
                /*
                 * so, asset entries have the following extra data compared to folder:
				 * IsTrashed, 1 byte - Would always be 0 for exported assets
				 * NextRevisionId, 2 bytes
				 * Revisions count, 2 bytes
				 * and if revisions count > 0 then there's a list of revision id (2 bytes) and revision description (string)
				 * [4:41:36 PM] �lis�e: I guess it's possible the "next revision id" and "revisions count" are always 0 when exporting to an asset pack
				 * [4:41:52 PM] �lis�e: so basically it's just 5 useless bytes ^_^
				 */
                buffer.position(buffer.position() + 5);
            }
        }

        public boolean isFolder() {
            return folder;
        }

        public short getID() {
            return id;
        }

        public short getParentID() {
            return parent_id;
        }

        public String getName() {
            return name;
        }

        public byte getType() {
            return type;
        }

        public boolean isLocked() {
            return locked;
        }

        @Override
        public String toString() {
            return String.format("ProjectEntry[%s] : Folder[%b], ID[%d], Parent[%d], Type[%d], Locked[%b]", name, folder, id, parent_id, type, locked);
        }
    }

    void readPackFile(final InputStream overrideStream) throws CSExceptions.NoCSProjectException {
        if (project == null) throw new CSExceptions.NoCSProjectException();

        try {
            entries.clear();

            final Locale localeOriginal = Locale.getDefault();
            LOGGER.info(String.format("Original locale was %s, switching to Locale.US", localeOriginal));
            Locale.setDefault(Locale.US);
            LOGGER.info(String.format("Locale is now %s", Locale.getDefault()));


            final Map<Integer, JsonElement> descriptors = new HashMap<Integer, JsonElement>();
            //move asset loc to argument, remove from MC and allow for general use
            final String path = String.format("%s/%s", project.getProjectAssetPath(), this.name);
            final ZipInputStream zis = new ZipInputStream(CSPack.class.getResourceAsStream(path));

            // We read from the override stream if it exists. Otherwise, we just read the file provided with the pack
            final InputStream jsonStream = overrideStream != null ? overrideStream : CSPack.class.getResourceAsStream(String.format("%s/%s", project.getProjectAssetPath(), this.name.replace(".cspack", ".json")));

            if (jsonStream != null) {
                final byte[] buffer = new byte[4096];
                final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                while (true) {
                    final int len = jsonStream.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    outstream.write(buffer, 0, len);
                }
                final String jsonString = outstream.toString();

                final JsonElement rootElement;
                try {
                    final JsonParser jsonParser = new JsonParser();
                    final JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
                    jsonReader.setLenient(true);
                    rootElement = jsonParser.parse(jsonReader);
                } catch (final Throwable e) {
                    LOGGER.error(outstream.toString());
                    throw new RuntimeException(e);
                }

                // only expecting the json data in an array format
                if (rootElement.isJsonArray()) {
                    final JsonArray jsonList = rootElement.getAsJsonArray();
                    for (final JsonElement element : jsonList) {
                        if (element.isJsonObject()) {
                            //project.addDescriptor(element.getAsJsonObject().get("craftstudioAssetID").getAsInt(), element);
                            descriptors.put(element.getAsJsonObject().get("craftstudioAssetID").getAsInt(), element);
                        }
                    }
                }
            }

            Locale.setDefault(localeOriginal);
            LOGGER.info(String.format("Locale was restored to %s", localeOriginal));

            final ReadableByteChannel zipByteChan = Channels.newChannel(zis);
            ZipEntry ze;

            if (callback != null) {
                callback.setCount(descriptors.size());
            }

            while ((ze = zis.getNextEntry()) != null) {
                final ByteBuffer bbuf = ByteBuffer.allocate((int) ze.getSize());
                zipByteChan.read(bbuf);
                bbuf.order(ByteOrder.LITTLE_ENDIAN).rewind(); //reorder and reset read position

                if (ze.getName().indexOf('/') > 0) {
                    final String[] nameParts = ze.getName().split("[/.]");

                    final Integer id = Integer.valueOf(nameParts[nameParts.length - 2]);
                    final String type = nameParts[nameParts.length - 1];

                    if ("csmodel".equals(type)) {
                        try {
                            final CSModel model = new CSModel(bbuf);
                            project.addModel(id, model);

                            if (callback != null) {
                                callback.modelLoaded(model, descriptors.get(id));
                            }

                        } catch (final CSExceptions e) {
                            CraftStudioLib.getLog().info(String.format("Skipping Asset %s: %s", id, e.getMessage()));
                        }
                    }

                    if ("csmodelanim".equals(type)) {
                        try {
                            project.addAnimation(id, new CSModelAnim(bbuf));
                        } catch (final CSExceptions e) {
                            CraftStudioLib.getLog().info(String.format("Skipping Asset %s: %s", id, e.getMessage()));
                        }
                    }

                    if (descriptors.containsKey(id)) {
                        project.addDescriptor(id, descriptors.get(id));
                    }
                } else if ("DataPackage.dat".equals(ze.getName())) {
                    try {
                        readDataPackage(bbuf);
                    } catch (final CSExceptions e) {
                        CraftStudioLib.getLog().info(String.format("Error while reading CSPack: %s", e.getMessage()));
                    }
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void readDataPackage(final ByteBuffer buffer) throws CSExceptions.UnsupportedVersionException {
        this.version = buffer.get();
        short entry_count = buffer.getShort();

        while (entry_count-- > 0) {
            final ProjectEntry entry = new ProjectEntry(buffer);
            CraftStudioLib.debug(entry);
            this.entries.put((int) entry.getID(), entry);
        }
    }

    CSPack cleanup() {
        entries.clear();
        this.project = null;
        return this;
    }
}
