package givecount;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ScoreboardHandler {

    public static int tickCounter = 0;

    public ScoreObjective[] cached = new ScoreObjective[3];

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        GiveCountWorldData.instance.tick();

        if (++tickCounter < 20) return;
        tickCounter = 0;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers.length == 0) return;

        WorldServer overworld = server.worldServers[0];
        GiveCountWorldData data = GiveCountWorldData.get(overworld);

        if (!data.enabled) return;

        Scoreboard sb = overworld.getScoreboard();
        int mode = data.mode;

        for (int i = 0; i < 3; i++) {
            String objName = "giveCount" + (i + 1);
            ScoreObjective obj = sb.getObjective(objName);
            if (obj == null) {
                obj = sb.addScoreObjective(objName, IScoreObjectiveCriteria.field_96641_b);
            }
            cached[i] = obj;
        }

        ScoreObjective objective = cached[mode - 1];

        sb.func_96530_a(data.displaySlot, objective);

        String titleKey = "Scoreboard_GiveCount_0" + (mode - 1);
        objective.setDisplayName(StatCollector.translateToLocal(titleKey));

        NBTTagCompound playerData = data.playerData;

        for (Object key : playerData.func_150296_c()) {
            if (!(key instanceof String playerName)) continue;

            NBTTagCompound playerTag = playerData.getCompoundTag(playerName);
            Score score = sb.func_96529_a(playerName, objective);

            if (mode == 1) {
                score.setScorePoints(playerTag.getInteger("uses"));

            } else if (mode == 2) {
                long lastTime = playerTag.getLong("last_time");
                long diff = (System.currentTimeMillis() - lastTime) / 1000;
                if (diff < 0) diff = 0;
                score.setScorePoints((int) diff);

            } else if (mode == 3) {
                NBTTagList items = playerTag.getTagList("items", 10);
                int total = 0;
                for (int i = 0; i < items.tagCount(); i++) {
                    total += items.getCompoundTagAt(i)
                        .getInteger("count");
                }
                score.setScorePoints(total);
            }
        }
    }
}
