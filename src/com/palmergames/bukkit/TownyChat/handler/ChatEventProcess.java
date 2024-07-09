package com.palmergames.bukkit.TownyChat.handler;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.channels.ChannelTypes;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.util.Adventure;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.WeakHashMap;

public class ChatEventProcess {
    public static void processChatEvent(AsyncChatEvent e, WeakHashMap<Player, String> directedChat) {
        Player p = e.getPlayer();

        // Save the message
        Component messageContent = e.originalMessage();
        String messageContentString = Adventure.plainText().serialize(messageContent);

        // Check if the player is muted by essentials
        if (isEssentialsMuted(p)) {
            e.setCancelled(true);
            return;
        }

        final boolean userForcedGlobal = ChatSettings.isExclamationPoint() && messageContentString.startsWith("!");

        // If the player is using /g or /n to send their message
        final boolean isDirectedChat = directedChat.containsKey(p);

        // Get the user channel or select the first global channel
        Channel channel = isDirectedChat ? Chat.getTownyChat().getChannelsHandler().getChannel(directedChat.get(p)) : Chat.getTownyChat().getPlayerChannel(p);
        if (channel == null || !channel.hasSpeakPermission(p)) {
            channel = Chat.getTownyChat().getChannelsHandler().getActiveChannel(p, ChannelTypes.GLOBAL, userForcedGlobal);
        }

        // No valid channel
        if (channel == null) {
            e.setCancelled(true);
            if (isDirectedChat)
                directedChat.remove(p);
            return;
        }

        // If muted
        if (isMutedOrSpam(e, channel, p)) {
            e.setCancelled(true);
            if (isDirectedChat)
                directedChat.remove(p);
            return;
        }

        // Post to chat
        channel.chatProcess(e);

        // Remove player from directed chat if using it
        if (isDirectedChat && (!Chat.usingEssentialsDiscord || e.isCancelled())) {
            directedChat.remove(p);
        }
    }

    /**
     * Check if the player is channel-muted or channel-spamming and cancel the
     * {@link AsyncChatEvent} if this is the case.
     *
     * @param e   {@link AsyncChatEvent} which has fired.
     * @param channel {@link Channel} being spoken in to.
     * @param p  {@link Player} speaking.
     * @return true if the chat is muted or spammed.
     */
    private static boolean isMutedOrSpam(AsyncChatEvent e, Channel channel, Player p) {
        if (channel.isMuted(p.getName())) {
            TownyMessaging.sendErrorMsg(p, Translatable.of("tc_err_you_are_currently_muted_in_channel", channel.getName()));
            return true;
        }
        return channel.isSpam(p);
    }

    /**
     * Is this player Muted via Essentials?
     *
     * @param p {@link Player} speaking.
     * @return true if muted by Essentials.
     */
    private static boolean isEssentialsMuted(Player p) {
        // Check if essentials has this player muted.
        if (Chat.getTownyChat().isEssentialsMuted(p)) {
            TownyMessaging.sendErrorMsg(p, Translatable.of("tc_err_unable_to_talk_essentials_mute"));
            return true;
        }
        return false;
    }
}
