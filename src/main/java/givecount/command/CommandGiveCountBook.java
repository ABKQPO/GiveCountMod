package givecount.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.registry.GameData;
import givecount.GiveCountWorldData;

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

        if (args.length >= 1 && !args[0].isEmpty()) {
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

        WorldServer world = (targetPlayer != null) ? (WorldServer) targetPlayer.getEntityWorld()
            : MinecraftServer.getServer().worldServers[0];

        GiveCountWorldData data = GiveCountWorldData.get(world);
        NBTTagCompound playerData = data.playerData;

        if (!playerData.hasKey(targetName)) {
            sender.addChatMessage(new ChatComponentText("No data found for player: " + targetName));
            return;
        }

        NBTTagCompound targetTag = playerData.getCompoundTag(targetName);
        int totalUses = targetTag.hasKey("uses") ? targetTag.getInteger("uses") : 0;
        long lastTime = targetTag.hasKey("last_time") ? targetTag.getLong("last_time") : 0L;
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastTime));

        NBTTagList itemsList = targetTag.getTagList("items", 10);
        int totalItems = itemsList.tagCount();

        ItemStack book = new ItemStack(Items.paper);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();

        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_00") + ": " + targetName));
        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_01") + ": " + totalUses));
        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_02")));
        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("Info_GiveCountBook_03") + ": " + timeStamp));

        System.out.println("[GiveCount] GiveCountBook for " + targetName);
        System.out.println("[GiveCount] Total Uses: " + totalUses + ", Last Time: " + timeStamp);

        int start = (page - 1) * 250;
        int end = Math.min(start + 250, totalItems);

        for (int i = start; i < end; i++) {
            NBTTagCompound itemTag = itemsList.getCompoundTagAt(i);
            String regWithMeta = itemTag.getString("id");
            int count = itemTag.getInteger("count");
            long last = itemTag.getLong("last_time");
            String itemTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(last));

            String[] parts = regWithMeta.split(":");
            if (parts.length < 2) continue;

            int meta = 0;
            try {
                meta = Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}

            String regName = String.join(":", parts[0], parts[1]);

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
        }

        if (totalItems > end) {
            lore.appendTag(new NBTTagString("§r§a" + StatCollector.translateToLocal("Info_GiveCountBook_04")));
        }

        // 显示页码
        int totalPages = (totalItems + 249) / 250;
        lore.appendTag(new NBTTagString("§r§7(Page " + page + " / " + totalPages + ")"));

        NBTTagCompound display = new NBTTagCompound();
        display.setTag("Lore", lore);
        tag.setTag("display", display);
        book.setTagCompound(tag);
        book.setStackDisplayName(StatCollector.translateToLocal("NameGiveCountBook") + targetName + " - " + page);

        if (sender instanceof EntityPlayerMP player) {
            player.inventory.addItemStackToInventory(book);
            sender.addChatMessage(new ChatComponentText("Gave Give Count Paper to " + player.getCommandSenderName()));
        } else {
            sender.addChatMessage(new ChatComponentText("Cannot give paper to console."));
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
            for (EntityPlayerMP obj : (List<EntityPlayerMP>) MinecraftServer.getServer()
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
