package givecount.command;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cpw.mods.fml.common.registry.GameData;

public class CommandGiveCountBook extends CommandBase {

    @Override
    public String getCommandName() {
        return "givecountbook";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/givecountbook [playerName] [page]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String targetName;
        int page = 1;
        EntityPlayerMP targetPlayer = null;

        if (args.length >= 1 && args[0].length() > 0) {
            targetName = args[0];
        } else if (sender instanceof EntityPlayerMP) {
            targetPlayer = (EntityPlayerMP) sender;
            targetName = sender.getCommandSenderName();
        } else {
            sender.addChatMessage(new ChatComponentText("Console must specify a player name."));
            return;
        }

        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        World world = (targetPlayer != null) ? targetPlayer.getEntityWorld()
            : MinecraftServer.getServer().worldServers[0];
        File gtDir = new File(
            world.getSaveHandler()
                .getWorldDirectory(),
            "GiveCount");
        File usesFile = new File(gtDir, "player_give_count.xml");
        File itemsFile = new File(gtDir, "player_give_item.xml");

        if (!usesFile.exists() || !itemsFile.exists()) {
            sender.addChatMessage(new ChatComponentText("Player data not found."));
            return;
        }

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
            Document docUses = db.parse(usesFile);
            Element rootUses = docUses.getDocumentElement();
            int totalUses = 0;
            String timeStamp = "";
            for (int i = 0; i < rootUses.getElementsByTagName("player")
                .getLength(); i++) {
                Element el = (Element) rootUses.getElementsByTagName("player")
                    .item(i);
                if (el.getAttribute("name")
                    .equals(targetName)) {
                    totalUses = Integer.parseInt(el.getAttribute("uses"));
                    long lastTime = Long.parseLong(el.getAttribute("last_time"));
                    timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastTime));
                    break;
                }
            }

            Document docItems = db.parse(itemsFile);
            Element rootItems = docItems.getDocumentElement();
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
                sender.addChatMessage(new ChatComponentText("No item data for player: " + targetName));
                return;
            }

            ItemStack book = new ItemStack(Items.book);
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagList lore = new NBTTagList();

            lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_00") + targetName));
            lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_01") + totalUses));
            lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_02")));
            lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_03") + timeStamp));

            System.out.println("[GTNL] GiveCountBook for: " + targetName);
            System.out.println("[GTNL] Total Uses: " + totalUses + ", Last Time: " + timeStamp);

            int start = (page - 1) * 250;
            int end = start + 250;
            int totalItems = targetEl.getElementsByTagName("item")
                .getLength();

            for (int i = start; i < Math.min(end, totalItems); i++) {
                Element itemEl = (Element) targetEl.getElementsByTagName("item")
                    .item(i);
                String regWithMeta = itemEl.getAttribute("name");
                int count = Integer.parseInt(itemEl.getAttribute("count"));
                long last = Long.parseLong(itemEl.getAttribute("last_time"));
                String itemTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(last));

                String[] parts = regWithMeta.split(":");
                if (parts.length < 2) continue;

                String regName = parts[0] + ":" + parts[1];
                int meta = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;

                Item item = GameData.getItemRegistry()
                    .getObject(regName);
                if (item == null) continue;

                ItemStack stack = new ItemStack(item, 1, meta);
                String unlocName = stack.getUnlocalizedName();
                if (unlocName == null) continue;

                String localized = StatCollector.translateToLocal(unlocName + ".name");
                String line = localized + " : " + regName + ":" + meta + " : " + count + " : " + itemTime;
                line = line.replaceAll("§[0-9a-fk-or]", "");
                line = "§r§e" + line;
                lore.appendTag(new NBTTagString(line));

                System.out.println("[GTNL] " + line);
            }

            if (totalItems > end) {
                String moreLine = "§r§a" + StatCollector.translateToLocal("Info_GiveCountBook_04");
                lore.appendTag(new NBTTagString(moreLine));
            }

            NBTTagCompound display = new NBTTagCompound();
            display.setTag("Lore", lore);
            tag.setTag("display", display);
            book.setTagCompound(tag);
            book.setStackDisplayName(StatCollector.translateToLocal("NameGiveCountBook") + targetName + " - " + page);

            if (sender instanceof EntityPlayerMP) {
                ((EntityPlayerMP) sender).inventory.addItemStackToInventory(book);
                sender.addChatMessage(
                    new ChatComponentText("Give " + sender.getCommandSenderName() + " Give Count Paper"));
            } else {
                sender.addChatMessage(new ChatComponentText("Cannot give paper to console."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sender.addChatMessage(new ChatComponentText("Error reading Player data."));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (EntityPlayerMP obj : MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList) {
                names.add(obj.getCommandSenderName());
            }
            return names;
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public int compareTo(Object o) {
        return this.getCommandName()
            .compareTo(((ICommand) o).getCommandName());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 1;
    }
}
