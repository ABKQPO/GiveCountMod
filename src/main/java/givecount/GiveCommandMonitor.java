package givecount;

import net.minecraft.block.Block;
import net.minecraft.command.CommandGive;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.CommandEvent;

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
        if (count > 64 || displayName == null) return;

        World world = senderPlayer.getEntityWorld();
        if (!(world instanceof WorldServer worldServer)) return;

        GiveCountWorldData data = GiveCountWorldData.get(worldServer);

        long now = System.currentTimeMillis();

        NBTTagCompound playerData = data.playerData;
        NBTTagCompound executorTag = playerData.getCompoundTag(executor);
        if (executorTag == null) executorTag = new NBTTagCompound();

        int uses = executorTag.getInteger("uses");
        executorTag.setInteger("uses", uses + 1);
        executorTag.setLong("last_time", now);

        if (!executorTag.hasKey("items")) {
            executorTag.setTag("items", new NBTTagList());
        }
        playerData.setTag(executor, executorTag);

        NBTTagCompound targetTag = playerData.getCompoundTag(targetName);
        if (targetTag == null) targetTag = new NBTTagCompound();

        NBTTagList itemsList = targetTag.getTagList("items", 10);
        boolean found = false;

        for (int i = 0; i < itemsList.tagCount(); i++) {
            NBTTagCompound itemTag = itemsList.getCompoundTagAt(i);
            if (itemTag.getString("id")
                .equals(displayName)) {
                int prevCount = itemTag.getInteger("count");
                itemTag.setInteger("count", prevCount + count);
                itemTag.setLong("last_time", now);
                found = true;
                break;
            }
        }

        if (!found) {
            NBTTagCompound newItem = new NBTTagCompound();
            newItem.setString("id", displayName);
            newItem.setInteger("count", count);
            newItem.setLong("last_time", now);
            itemsList.appendTag(newItem);
        }

        targetTag.setTag("items", itemsList);
        playerData.setTag(targetName, targetTag);

        data.playerData = playerData;
        data.markDirty();
    }

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
