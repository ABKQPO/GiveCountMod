package givecount;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.world.WorldEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ScoreboardHandler {

    private ScoreObjective objective;
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {

        if (++tickCounter < 20) return;
        tickCounter = 0;

        try {
            File gtDir = new File(
                MinecraftServer.getServer()
                    .getEntityWorld()
                    .getSaveHandler()
                    .getWorldDirectory(),
                "GiveCount");
            File configFile = new File(gtDir, "scoreboard.json");
            if (!configFile.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonBuilder.append(line);
            reader.close();

            String json = jsonBuilder.toString();
            if (!json.contains("\"enabled\": true")) return;

            int mode = 0;
            if (json.contains("\"mode\": 1")) mode = 1;
            else if (json.contains("\"mode\": 2")) mode = 2;
            else if (json.contains("\"mode\": 3")) mode = 3;

            File usesFile = new File(gtDir, "player_give_count.xml");
            File itemsFile = new File(gtDir, "player_give_item.xml");
            if (!usesFile.exists() || (mode == 3 && !itemsFile.exists())) return;

            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
            Document docUses = db.parse(usesFile);
            Element rootUses = docUses.getDocumentElement();

            Document docItems = null;
            if (mode == 3) {
                docItems = db.parse(itemsFile);
            }

            for (int dim = 0; dim < MinecraftServer.getServer().worldServers.length; dim++) {
                Scoreboard sb = MinecraftServer.getServer().worldServers[dim].getScoreboard();

                if (objective == null) {
                    objective = sb.getObjective("GiveCount");
                    if (objective == null) {
                        objective = sb.addScoreObjective("GiveCount", new ScoreDummyCriteria("GiveCount"));
                        sb.func_96530_a(1, objective);
                    }
                }

                String titleKey = "Scoreboard_GiveCount_0" + (mode - 1);
                objective.setDisplayName(StatCollector.translateToLocal(titleKey));

                for (int i = 0; i < rootUses.getElementsByTagName("player")
                    .getLength(); i++) {
                    Element el = (Element) rootUses.getElementsByTagName("player")
                        .item(i);
                    String playerName = el.getAttribute("name");
                    Score score = sb.func_96529_a(playerName, objective);

                    if (mode == 1) {
                        int uses = Integer.parseInt(el.getAttribute("uses"));
                        score.setScorePoints(uses);
                    } else if (mode == 2) {
                        long lastTime = Long.parseLong(el.getAttribute("last_time"));
                        long currentTime = System.currentTimeMillis();
                        long timeDifferenceInSeconds = (currentTime - lastTime) / 1000;
                        try {
                            score.setScorePoints((int) timeDifferenceInSeconds);
                        } catch (Exception ignored) {
                            score.setScorePoints(0);
                        }
                    } else if (mode == 3 && docItems != null) {
                        Element rootItems = docItems.getDocumentElement();
                        NodeList targets = rootItems.getElementsByTagName("target");
                        Element targetEl = null;
                        for (int j = 0; j < targets.getLength(); j++) {
                            Element tel = (Element) targets.item(j);
                            if (tel.getAttribute("name")
                                .equals(playerName)) {
                                targetEl = tel;
                                break;
                            }
                        }
                        if (targetEl != null) {
                            NodeList items = targetEl.getElementsByTagName("item");
                            int totalCount = 0;
                            for (int j = 0; j < items.getLength(); j++) {
                                Element itemEl = (Element) items.item(j);
                                int count = Integer.parseInt(itemEl.getAttribute("count"));
                                totalCount += count;
                            }
                            score.setScorePoints(totalCount);
                        } else {
                            score.setScorePoints(0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!GiveCount.scoreboardEnabled && objective != null) {
            Scoreboard sb = event.world.getScoreboard();
            sb.func_96519_k(objective);
            objective = null;
        }
    }
}
