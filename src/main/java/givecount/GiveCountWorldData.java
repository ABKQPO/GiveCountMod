package givecount;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class GiveCountWorldData extends WorldSavedData {

    public static final String DATA_NAME = "GiveCountWorldData";
    public static GiveCountWorldData instance = new GiveCountWorldData();

    public boolean enabled = false;
    public int mode = 0;
    public NBTTagCompound playerData = new NBTTagCompound();

    public GiveCountWorldData() {
        super(DATA_NAME);
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
        enabled = nbt.getBoolean("enabled");
        mode = nbt.getInteger("mode");
        playerData = nbt.getCompoundTag("playerData");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("enabled", enabled);
        nbt.setInteger("mode", mode);
        nbt.setTag("playerData", playerData);
    }
}
