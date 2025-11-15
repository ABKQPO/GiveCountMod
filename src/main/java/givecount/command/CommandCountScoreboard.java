package givecount.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import givecount.GiveCountWorldData;

public class CommandCountScoreboard extends CommandBase {

    @Override
    public String getCommandName() {
        return "countscoreboard";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/countscoreboard <enable|disable> <count|lasttime|mostitem> [sidebar|list|below]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        if (args.length < 1 || args.length > 3) {
            sendUsage(sender);
            return;
        }

        boolean enable = args[0].equalsIgnoreCase("enable");
        boolean disable = args[0].equalsIgnoreCase("disable");

        if (!enable && !disable) {
            sendUsage(sender);
            return;
        }

        int mode = 0;

        if (enable) {
            if (args.length < 2) {
                sendUsage(sender);
                return;
            }

            switch (args[1].toLowerCase()) {
                case "count":
                    mode = 1;
                    break;
                case "lasttime":
                    mode = 2;
                    break;
                case "mostitem":
                    mode = 3;
                    break;
                default:
                    sender.addChatMessage(new ChatComponentText("Invalid mode: " + args[1]));
                    sender.addChatMessage(new ChatComponentText("Valid modes: count, lasttime, mostitem"));
                    return;
            }
        }

        int displaySlot = 1;
        if (args.length == 3) {
            switch (args[2].toLowerCase()) {
                case "list":
                    displaySlot = 0;
                    break;
                case "sidebar":
                    displaySlot = 1;
                    break;
                case "below":
                    displaySlot = 2;
                    break;
                default:
                    sender.addChatMessage(new ChatComponentText("Invalid display position: " + args[2]));
                    sender.addChatMessage(new ChatComponentText("Valid: sidebar, list, below"));
                    return;
            }
        }

        World world = sender.getEntityWorld();
        if (!(world instanceof WorldServer worldServer)) {
            return;
        }

        GiveCountWorldData data = GiveCountWorldData.get(worldServer);

        data.enabled = enable;
        data.mode = mode;
        data.displaySlot = displaySlot;
        data.markDirty();

        String msgKey = enable ? "Scoreboard_Enable_Success" : "Scoreboard_Disable_Success";
        sender.addChatMessage(
            new ChatComponentText(StatCollector.translateToLocal(msgKey) + " (slot=" + displaySlot + ")"));

        System.out.println(
            "[GiveCount] Scoreboard " + (enable ? "enabled" : "disabled") + ", mode=" + mode + ", slot=" + displaySlot);
    }

    private void sendUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return super.canCommandSenderUseCommand(sender);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enable", "disable");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("enable")) {
            return Arrays.asList("count", "lasttime", "mostitem");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("enable")) {
            return Arrays.asList("sidebar", "list", "below");
        }
        return null;
    }
}
