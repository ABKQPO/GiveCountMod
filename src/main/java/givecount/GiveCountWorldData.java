package givecount;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class GiveCountWorldData extends WorldSavedData {

    private static final String DATA_NAME = "giveCountWorldData";
    public static GiveCountWorldData instance = new GiveCountWorldData();

    public boolean enabled = false;
    public int mode = 0;
    public int displaySlot = 1;
    public NBTTagCompound playerData = new NBTTagCompound();

    public int tickCounter;

    public GiveCountWorldData() {
        super(DATA_NAME);
        this.markDirty();
    }

    public GiveCountWorldData(String name) {
        super(name);
    }

    public static GiveCountWorldData get(World world) {
        MapStorage storage = world.mapStorage;
        GiveCountWorldData data = (GiveCountWorldData) storage.loadData(GiveCountWorldData.class, DATA_NAME);
        if (data == null) {
            data = new GiveCountWorldData();
            storage.setData(DATA_NAME, data);
            data.markDirty();
        }
        instance = data;
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagCompound data = nbt.getCompoundTag("giveCountData");
        enabled = data.getBoolean("enabled");
        mode = data.getInteger("mode");
        displaySlot = data.getInteger("displaySlot");
        playerData = data.getCompoundTag("playerData");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound data = new NBTTagCompound();

        data.setBoolean("enabled", enabled);
        data.setInteger("mode", mode);
        data.setInteger("displaySlot", displaySlot);
        data.setTag("playerData", playerData);

        nbt.setTag("giveCountData", data);
    }

    public void tick() {
        ++this.tickCounter;

        if (this.tickCounter % 400 == 0) {
            this.markDirty();
        }
    }
}
