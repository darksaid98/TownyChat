package com.palmergames.bukkit.TownyChat.Command;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ElgarL
 */
public class TownyChatCommand extends BaseCommand implements CommandExecutor {

    private static final List<String> townychat_help = new ArrayList<String>();

    static {

        townychat_help.add(ChatTools.formatTitle("/townychat"));
        //TODO: Add lang strings for description!
        townychat_help.add(ChatTools.formatCommand("", "/townychat", "reload", ""));
    }

    private Chat plugin;

    public TownyChatCommand(Chat instance) {
        this.plugin = instance;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("townychat")) {
            parseTownyChatCommand(sender, args);
            return true;
        }
        return false;
    }

    private void parseTownyChatCommand(CommandSender sender, String[] split) {
        if (split.length == 0 || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("?")) {
            for (String line : townychat_help) {
                TownyMessaging.sendMessage(sender, line);
            }
        } else if (split[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            TownyMessaging.sendMessage(sender, Translatable.of("tc_settings_reloaded"));
        } else {
            TownyMessaging.sendErrorMsg(sender, Translatable.of("tc_err_unrecognized_command_format"));
        }

    }

}
