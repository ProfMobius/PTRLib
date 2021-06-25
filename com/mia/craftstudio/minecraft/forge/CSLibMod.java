package com.mia.craftstudio.minecraft.forge;

import com.mia.craftstudio.CSProject;
import com.mia.craftstudio.CraftStudioLib;
import com.mia.craftstudio.IPackReaderCallback;
import com.mia.craftstudio.api.ICSProject;
import com.mia.craftstudio.minecraft.AnimationState;
import com.mia.craftstudio.minecraft.BlockDimensionalPosition;
import com.mia.craftstudio.minecraft.CSMsgAnimationUpdate;
import com.mia.craftstudio.minecraft.IAnimatedTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Locale;

@Mod(modid = CSLibMod.modid, name = CSLibMod.modid, version = "1.0.5", clientSideOnly = true)
public class CSLibMod {
    @Mod.Instance(CSLibMod.modid)
    public static CSLibMod instance;

    @SidedProxy(clientSide = "com.mia.craftstudio.minecraft.forge.ProxyClient", serverSide = "com.mia.craftstudio.minecraft.forge.ProxyCommon")
    public static ProxyCommon proxy;

    public static final String modid = "ptrmodellib";
    public static Logger log = LogManager.getLogger(CSLibMod.modid);

    public static boolean displayOutline = false;
    private static Configuration config = null;

    private SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent ev) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel("CraftStudioLib");
        network.registerMessage(CSMsgAnimationUpdate.Handler.class, CSMsgAnimationUpdate.class, 0, Side.CLIENT);

        config = new Configuration(ev.getSuggestedConfigurationFile());

        try {
            config.load();
            displayOutline = config.getBoolean("displayOutline", Configuration.CATEGORY_GENERAL, false, "Should we render the targeting outline ? Set false for better performances.");
        } catch (final Exception e) {
            FMLLog.severe("PTRModelLib has a problem loading it's configuration");
            FMLLog.severe(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }

        proxy.preInit();
    }

    public static void startAnimation(final TileEntity tile, final ICSProject project, final Integer animationID) {
        if (tile instanceof IAnimatedTile) {
            final BlockDimensionalPosition tilePosition = ((IAnimatedTile) tile).getBlockPosDim();
            final AnimationState anim = new AnimationState(project, animationID, System.currentTimeMillis(), 5, false);
            //TODO add to server side TE, keep track of active TE's ?

            instance.network.sendToAllAround(new CSMsgAnimationUpdate((IAnimatedTile) tile, anim), new NetworkRegistry.TargetPoint(tilePosition.getDim(), tilePosition.getX(), tilePosition.getY(), tilePosition.getZ(), 128));
        }
    }

    /**
     * Helper method to obtain a CSProject for the current active mod, it  should only be used during FML load
     * events(pre/int/post, etc..). The project name should match the directory name in
     * assets/modid/models/[dirname]/*.cspack(s go here)
     *
     * @param name
     */
    public static ICSProject getCSProject(final String name) {
        final ModContainer mod = Loader.instance().activeModContainer();
        return new CSProject(name, mod.getModId(), String.format("/assets/%s/models/%s", mod.getModId().toLowerCase(Locale.US), name), mod.getSource());
    }

    public static ICSProject getCSProjectAndLoad(final String name, final InputStream overrideStream, final IPackReaderCallback callback) {
        CraftStudioLib.getTimer().start("Full project");
        final ICSProject project = getCSProject(name).addAllPacks(callback).loadPacks(overrideStream);
        CraftStudioLib.getTimer().stop("Full project");

        CraftStudioLib.debug(String.format("Project loaded in %s ms", CraftStudioLib.getTimer().get("Full project")));

        return project;
    }
}
