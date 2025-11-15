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
        if (event.phase == TickEvent.Phase.END) return;

        GiveCountWorldData.instance.tick();

        if (++tickCounter < 20) return;
        tickCounter = 0;

        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || server.worldServers == null || server.worldServers.length == 0) return;

            WorldServer overworld = server.worldServers[0];
            GiveCountWorldData data = GiveCountWorldData.get(overworld);

            if (!data.enabled) return;
            int mode = data.mode;

            Scoreboard sb = overworld.getScoreboard();

            for (int i = 0; i < 3; i++) {
                String objName = "giveCount" + (i + 1);
                ScoreObjective obj = sb.getObjective(objName);
                if (obj == null) {
                    obj = sb.addScoreObjective(objName, IScoreObjectiveCriteria.field_96641_b);
                }
                cached[i] = obj;
                sb.func_96530_a(i, obj);
            }

            ScoreObjective objective = cached[mode - 1];

            String titleKey = "Scoreboard_GiveCount_0" + (mode - 1);
            objective.setDisplayName(StatCollector.translateToLocal(titleKey));

            NBTTagCompound playerData = data.playerData;

            for (Object key : playerData.func_150296_c()) {
                if (!(key instanceof String playerName)) continue;

                NBTTagCompound playerTag = playerData.getCompoundTag(playerName);
                Score score = sb.func_96529_a(playerName, objective);

                if (mode == 1) {
                    int uses = playerTag.getInteger("uses");
                    score.setScorePoints(uses);

                } else if (mode == 2) {
                    long lastTime = playerTag.getLong("last_time");
                    long currentTime = System.currentTimeMillis();
                    long diff = (currentTime - lastTime) / 1000;
                    if (diff < 0) diff = 0;
                    score.setScorePoints((int) diff);

                } else if (mode == 3) {
                    NBTTagList items = playerTag.getTagList("items", 10);
                    int totalCount = 0;
                    for (int i = 0; i < items.tagCount(); i++) {
                        totalCount += items.getCompoundTagAt(i)
                            .getInteger("count");
                    }
                    score.setScorePoints(totalCount);
                }
            }

        } catch (Exception e) {
            System.err.print("Give Count Scoreboard Error, Check Stack Trace");
            e.printStackTrace();
        }
    }
}
