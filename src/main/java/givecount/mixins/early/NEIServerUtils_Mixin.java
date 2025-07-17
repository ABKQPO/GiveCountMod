package givecount.mixins.early;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import codechicken.nei.NEIServerUtils;
import cpw.mods.fml.common.registry.GameData;

@SuppressWarnings("UnusedMixin")
@Mixin(value = NEIServerUtils.class, remap = false)
public abstract class NEIServerUtils_Mixin {

    @Inject(
        method = "givePlayerItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Container;detectAndSendChanges()V"),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void afterGivePlayerItem(EntityPlayerMP player, ItemStack stack, boolean infinite, boolean doGive,
        CallbackInfo ci, int given) {
        if (!doGive || stack == null || stack.getItem() == null || given <= 0) return;
        gcm$recordGiveAction(player, stack, given);
    }

    @Unique
    private static void gcm$recordGiveAction(EntityPlayerMP player, ItemStack stack, int given) {
        String executor = player.getCommandSenderName();
        String displayName = gcm$resolveDisplayName(stack);
        if (displayName == null) return;

        World world = player.getEntityWorld();
        File worldDir = world.getSaveHandler()
            .getWorldDirectory();
        File gtDir = new File(worldDir, "GiveCount");
        if (!gtDir.exists()) gtDir.mkdirs();

        File usesFile = new File(gtDir, "player_give_count.xml");
        File itemsFile = new File(gtDir, "player_give_item.xml");

        long currentTime = System.currentTimeMillis();

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

            // --- Handle player_give_count.xml ---
            Document docUses = usesFile.exists() ? db.parse(usesFile) : db.newDocument();
            Element rootUses = docUses.getDocumentElement();
            if (rootUses == null) {
                rootUses = docUses.createElement("players");
                docUses.appendChild(rootUses);
            }

            Element playerEl = null;
            NodeList players = rootUses.getElementsByTagName("player");
            for (int i = 0; i < players.getLength(); i++) {
                Element el = (Element) players.item(i);
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
            playerEl.setAttribute("last_time", String.valueOf(currentTime));

            Transformer t = TransformerFactory.newInstance()
                .newTransformer();
            t.transform(new DOMSource(docUses), new StreamResult(usesFile));

            Document docItems = itemsFile.exists() ? db.parse(itemsFile) : db.newDocument();
            Element rootItems = docItems.getDocumentElement();
            if (rootItems == null) {
                rootItems = docItems.createElement("targets");
                docItems.appendChild(rootItems);
            }

            Element targetEl = null;
            NodeList targets = rootItems.getElementsByTagName("target");
            for (int i = 0; i < targets.getLength(); i++) {
                Element el = (Element) targets.item(i);
                if (el.getAttribute("name")
                    .equals(executor)) {
                    targetEl = el;
                    break;
                }
            }
            if (targetEl == null) {
                targetEl = docItems.createElement("target");
                targetEl.setAttribute("name", executor);
                rootItems.appendChild(targetEl);
            }

            Element itemEl = null;
            NodeList items = targetEl.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element el = (Element) items.item(i);
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
            itemEl.setAttribute("count", String.valueOf(prevCount + given));
            itemEl.setAttribute("last_time", String.valueOf(currentTime));

            t.transform(new DOMSource(docItems), new StreamResult(itemsFile));

        } catch (Exception e) {
            e.printStackTrace();
        }
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
