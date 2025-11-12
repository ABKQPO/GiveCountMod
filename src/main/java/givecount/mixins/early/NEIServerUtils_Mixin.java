package givecount.mixins.early;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import codechicken.nei.NEIServerUtils;
import cpw.mods.fml.common.registry.GameData;
import givecount.GiveCountWorldData;

@Mixin(value = NEIServerUtils.class, remap = false)
public abstract class NEIServerUtils_Mixin {

    @Inject(
        method = "givePlayerItem",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/NEIServerUtils;sendNotice(Lnet/minecraft/command/ICommandSender;Lnet/minecraft/util/IChatComponent;Ljava/lang/String;)V",
            shift = At.Shift.BEFORE))
    private static void afterGivePlayerItem(EntityPlayerMP player, ItemStack stack, boolean infinite, boolean doGive,
        CallbackInfo ci, @Local(name = "given") int given) {
        try {
            gcm$recordGiveAction(player, stack, given);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private static void gcm$recordGiveAction(EntityPlayerMP player, ItemStack stack, int given) {
        if (stack == null || given <= 0) return;

        String executor = player.getCommandSenderName();
        String displayName = gcm$resolveDisplayName(stack);
        if (displayName == null) return;

        World world = player.getEntityWorld();
        if (!(world instanceof WorldServer ws)) return;

        GiveCountWorldData data = GiveCountWorldData.get(ws);
        long now = System.currentTimeMillis();

        NBTTagCompound playerData = data.playerData;

        NBTTagCompound executorTag = playerData.getCompoundTag(executor);
        int uses = executorTag.getInteger("uses");
        executorTag.setInteger("uses", uses + 1);
        executorTag.setLong("last_time", now);

        if (!executorTag.hasKey("items")) {
            executorTag.setTag("items", new NBTTagList());
        }
        playerData.setTag(executor, executorTag);

        NBTTagList items = executorTag.getTagList("items", 10);
        boolean found = false;
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(i);
            if (itemTag.getString("id")
                .equals(displayName)) {
                int prev = itemTag.getInteger("count");
                itemTag.setInteger("count", prev + given);
                itemTag.setLong("last_time", now);
                found = true;
                break;
            }
        }
        if (!found) {
            NBTTagCompound newItem = new NBTTagCompound();
            newItem.setString("id", displayName);
            newItem.setInteger("count", given);
            newItem.setLong("last_time", now);
            items.appendTag(newItem);
        }
        executorTag.setTag("items", items);
        playerData.setTag(executor, executorTag);

        data.playerData = playerData;
        data.markDirty();
    }

    @Unique
    private static String gcm$resolveDisplayName(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        String regName = GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
        if (regName == null) return null;
        return regName + ":" + stack.getItemDamage();
    }
}
