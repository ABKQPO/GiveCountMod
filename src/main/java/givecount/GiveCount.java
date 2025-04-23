package givecount;

import static givecount.Tags.MODID;
import static givecount.Tags.MODNAME;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = MODID,
    version = Tags.VERSION,
    name = MODNAME,
    dependencies = "required-after:NotEnoughItems;",
    acceptedMinecraftVersions = "1.7.10")

public class GiveCount {

    @Mod.Instance
    public static GiveCount instance;
    public static final String MODID = "givecount";
    public static final String MODNAME = "GiveCount";
    public static final String VERSION = Tags.VERSION;
    public static final String Arthor = "HFstudio";
    public static final String RESOURCE_ROOT_ID = "givecount";

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new main.java.givecount.CommandGiveCountBook());
    }

    @Mod.EventHandler
    public void midGame(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new main.java.givecount.GiveCommandMonitor());
    }
}
