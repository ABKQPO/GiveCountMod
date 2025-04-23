package main.java.givecount;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.minecraft.block.Block;
import net.minecraft.command.CommandGive;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;

public class GiveCommandMonitor {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        if (!(event.command instanceof CommandGive)) return;
        if (!(event.sender instanceof EntityPlayerMP senderPlayer)) return;

        String[] args = event.parameters;
        if (args.length < 2) return;

        String executor = senderPlayer.getCommandSenderName();
        String targetName = args[0];
        String rawItem = args[1];
        int count = 1;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }
        int meta = 0;
        if (args.length >= 4) {
            try {
                meta = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {}
        }

        String displayName = resolveDisplayName(rawItem, meta);
        if (count > 64) return;
        if (displayName == null) return;

        World world = senderPlayer.getEntityWorld();
        File worldDir = world.getSaveHandler()
            .getWorldDirectory();
        File gtDir = new File(worldDir, "GiveCount");
        if (!gtDir.exists()) gtDir.mkdirs();

        File usesFile = new File(gtDir, "player_give_count.xml");
        File itemsFile = new File(gtDir, "player_give_item.xml");

        try {
            long now = System.currentTimeMillis();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document docUses;
            Element rootUses;
            if (usesFile.exists()) {
                docUses = db.parse(usesFile);
                rootUses = docUses.getDocumentElement();
            } else {
                docUses = db.newDocument();
                rootUses = docUses.createElement("players");
                docUses.appendChild(rootUses);
            }
            Element playerEl = null;
            for (int i = 0; i < rootUses.getElementsByTagName("player")
                .getLength(); i++) {
                Element el = (Element) rootUses.getElementsByTagName("player")
                    .item(i);
                if (el.getAttribute("name")
                    .equals(executor)) {
                    playerEl = el;
                    break;
                }
            }
            if (playerEl == null) {
                playerEl = docUses.createElement("player");
                playerEl.setAttribute("name", executor);
                playerEl.setAttribute("uses", "0");
                rootUses.appendChild(playerEl);
            }
            int prevUses = Integer.parseInt(playerEl.getAttribute("uses"));
            playerEl.setAttribute("uses", String.valueOf(prevUses + 1));
            playerEl.setAttribute("last_time", String.valueOf(now));

            Transformer t = TransformerFactory.newInstance()
                .newTransformer();
            t.transform(new DOMSource(docUses), new StreamResult(usesFile));

            Document docItems;
            Element rootItems;
            if (itemsFile.exists()) {
                docItems = db.parse(itemsFile);
                rootItems = docItems.getDocumentElement();
            } else {
                docItems = db.newDocument();
                rootItems = docItems.createElement("targets");
                docItems.appendChild(rootItems);
            }
            Element targetEl = null;
            for (int i = 0; i < rootItems.getElementsByTagName("target")
                .getLength(); i++) {
                Element el = (Element) rootItems.getElementsByTagName("target")
                    .item(i);
                if (el.getAttribute("name")
                    .equals(targetName)) {
                    targetEl = el;
                    break;
                }
            }
            if (targetEl == null) {
                targetEl = docItems.createElement("target");
                targetEl.setAttribute("name", targetName);
                rootItems.appendChild(targetEl);
            }
            Element itemEl = null;
            for (int i = 0; i < targetEl.getElementsByTagName("item")
                .getLength(); i++) {
                Element el = (Element) targetEl.getElementsByTagName("item")
                    .item(i);
                if (el.getAttribute("name")
                    .equals(displayName)) {
                    itemEl = el;
                    break;
                }
            }
            if (itemEl == null) {
                itemEl = docItems.createElement("item");
                itemEl.setAttribute("name", displayName);
                itemEl.setAttribute("count", "0");
                targetEl.appendChild(itemEl);
            }
            int prevCount = Integer.parseInt(itemEl.getAttribute("count"));
            itemEl.setAttribute("count", String.valueOf(prevCount + count));
            itemEl.setAttribute("last_time", String.valueOf(now));

            t.transform(new DOMSource(docItems), new StreamResult(itemsFile));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 利用数字 ID + meta 构建 ItemStack
     */
    private String resolveDisplayName(String raw, int meta) {
        ItemStack stack = null;
        if (raw.matches("\\d+")) {
            int id = Integer.parseInt(raw);
            Item item = Item.getItemById(id);
            if (item != null) {
                stack = new ItemStack(item, 1, meta);
            } else {
                Block block = Block.getBlockById(id);
                if (block != null) {
                    stack = new ItemStack(block, 1, meta);
                }
            }
        } else {
            Item item = GameData.getItemRegistry()
                .getObject(raw);
            if (item != null) {
                stack = new ItemStack(item, 1, meta);
            }
        }

        if (stack != null) {
            String regName = GameData.getItemRegistry()
                .getNameForObject(stack.getItem());
            if (regName == null) return null;
            return regName + ":" + meta;
        }

        return null;
    }
}
