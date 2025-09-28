package com.palmergames.bukkit.TownyChat.util;

import com.palmergames.bukkit.TownyChat.Chat;
import org.bukkit.entity.Player;

public class EssentialsIntegration {

    public static boolean ignoredByEssentials(Player sender, Player player) {
        com.earth2me.essentials.User targetUser = Chat.getTownyChat().getEssentials().getUser(player);
        com.earth2me.essentials.User senderUser = Chat.getTownyChat().getEssentials().getUser(sender);
        return targetUser.isIgnoredPlayer(senderUser);
    }

    public static boolean isMuted(Player player) {
        return Chat.getTownyChat().getEssentials().getUser(player).isMuted();
    }
}
