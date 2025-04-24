package givecount.command;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import givecount.GiveCount;

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
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
            return;
        }

        boolean enable = args[0].equalsIgnoreCase("enable");
        boolean disable = args[0].equalsIgnoreCase("disable");

        if (!enable && !disable) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
            return;
        }

        int mode = 0; // 默认 disable 时为 0
        if (enable) {
            if (args.length != 2) {
                sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
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
                    sender.addChatMessage(new ChatComponentText("Valid modes: count, lasttime, moreitem"));
                    return;
            }
        }

        GiveCount.scoreboardEnabled = enable;

        String msgKey = enable ? "Scoreboard_Enable_Success" : "Scoreboard_Disable_Success";
        String localized = StatCollector.translateToLocal(msgKey);
        sender.addChatMessage(new ChatComponentText(localized));

        try {
            World world = sender.getEntityWorld();
            File worldDir = world.getSaveHandler()
                .getWorldDirectory();
            File gtDir = new File(worldDir, "GiveCount");
            if (!gtDir.exists()) gtDir.mkdirs();

            File jsonFile = new File(gtDir, "scoreboard.json");

            String json = "{\n" + "  \"enabled\": " + enable + ",\n" + "  \"mode\": " + mode + "\n" + "}";

            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(json);
            }
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("Failed to save scoreboard setting."));
            e.printStackTrace();
        }
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
