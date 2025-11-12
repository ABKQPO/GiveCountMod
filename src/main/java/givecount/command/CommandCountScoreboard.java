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
        return "/countscoreboard <enable count|lasttime|mostitem | disable>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 2) {
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
            if (args.length != 2) {
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

        World world = sender.getEntityWorld();
        if (!(world instanceof WorldServer worldServer)) {
            return;
        }

        GiveCountWorldData data = GiveCountWorldData.get(worldServer);
        data.enabled = enable;
        data.mode = mode;
        data.markDirty();

        String msgKey = enable ? "Scoreboard_Enable_Success" : "Scoreboard_Disable_Success";
        sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal(msgKey)));

        System.out.println("[GiveCount] Scoreboard " + (enable ? "enabled" : "disabled") + ", mode=" + mode);
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enable")) {
            return Arrays.asList("count", "lasttime", "mostitem");
        }
        return null;
    }
}
