package com.palmergames.bukkit.TownyChat.listener;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.handler.ChatEventProcess;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.util.Colors;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.WeakHashMap;

public class TownyChatPlayerListener implements Listener {
    public WeakHashMap<Player, String> directedChat = new WeakHashMap<>();
    private final Chat plugin;

    public TownyChatPlayerListener(Chat instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        plugin.getScheduler().runLater(p, () -> {
            checkPlayerForOldMeta(p);

            refreshPlayerChannels(p);

            Channel channel = plugin.getChannelsHandler().getDefaultChannel();
            if (channel != null && channel.hasSpeakPermission(p)) {
                plugin.setPlayerChannel(p, channel);
                if (ChatSettings.getShowChannelMessageOnServerJoin())
                    TownyMessaging.sendMessage(p, Translatable.of("tc_you_are_now_talking_in_channel", channel.getName()));
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent e) {
        refreshPlayerChannels(e.getPlayer());
    }

    private void refreshPlayerChannels(Player p) {
        plugin.getChannelsHandler().getAllChannels().values().forEach(channel -> channel.forgetPlayer(p));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent e) {
        ChatEventProcess.processChatEvent(e, directedChat);
    }

    /**
     * Remove colour tags that a player doesn't not have permission for,
     * and colour messages if the player is allowed.
     *
     * @param event  {@link AsyncChatEvent} which has been fired.
     * @param player {@link Player} which has spoken.
     */
    private void testColourCodes(AsyncChatEvent event, Player player) {
        Component message = event.message();
        String messageString = message.toString();

        if ((messageString.contains("&L") || messageString.contains("&l"))
            && !player.hasPermission("townychat.chat.format.bold"))
            event.message(Component.text(messageString.replaceAll("&L", "").replaceAll("&l", "")));

        if ((messageString.contains("&O") || messageString.contains("&o"))
            && !player.hasPermission("townychat.chat.format.italic"))
            event.message(Component.text(messageString.replaceAll("&O", "").replaceAll("&o", "")));

        if ((messageString.contains("&K") || messageString.contains("&k"))
            && !player.hasPermission("townychat.chat.format.magic"))
            event.message(Component.text(messageString.replaceAll("&K", "").replaceAll("&k", "")));

        if ((messageString.contains("&N") || messageString.contains("&n"))
            && !player.hasPermission("townychat.chat.format.underlined"))
            event.message(Component.text(messageString.replaceAll("&N", "").replaceAll("&n", "")));

        if ((messageString.contains("&M") || messageString.contains("&m"))
            && !player.hasPermission("townychat.chat.format.strike"))
            event.message(Component.text(messageString.replaceAll("&M", "").replaceAll("&m", "")));

        if ((messageString.contains("&R") || messageString.contains("&r"))
            && !player.hasPermission("townychat.chat.format.reset"))
            event.message(Component.text(messageString.replaceAll("&R", "").replaceAll("&r", "")));

        if (player.hasPermission("townychat.chat.color"))
            event.message(Component.text(Colors.translateColorCodes(messageString)));
    }

    // From TownyChat 0.84-0.95 the symbol used to separate the channels in the meta
    // was not good for non-unicode-using mysql servers.
    private void checkPlayerForOldMeta(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident != null && playerHasTCMeta(resident))
            checkIfMetaContainsOldSplitter(resident);
    }

    private boolean playerHasTCMeta(Resident resident) {
        StringDataField icsdf = new StringDataField("townychat_ignoredChannels", "", "Ignored TownyChat Channels");
        StringDataField socsdf = new StringDataField("townychat_soundOffChannels", "", "TownyChat Channels with Sound Toggle Off");
        return MetaDataUtil.hasMeta(resident, icsdf) || MetaDataUtil.hasMeta(resident, socsdf);
    }

    private void checkIfMetaContainsOldSplitter(Resident resident) {
        StringDataField icsdf = new StringDataField("townychat_ignoredChannels", "", "Ignored TownyChat Channels");
        if (MetaDataUtil.hasMeta(resident, icsdf)) {
            String meta = MetaDataUtil.getString(resident, icsdf);
            if (meta.contains("\uFF0c ")) {
                meta = replaceSymbol(meta);
                MetaDataUtil.setString(resident, icsdf, meta, true);
            }
        }
        StringDataField socsdf = new StringDataField("townychat_soundOffChannels", "", "TownyChat Channels with Sound Toggle Off");
        if (MetaDataUtil.hasMeta(resident, socsdf)) {
            String meta = MetaDataUtil.getString(resident, socsdf);
            if (meta.contains("\uFF0c ")) {
                meta = replaceSymbol(meta);
                MetaDataUtil.setString(resident, socsdf, meta, true);
            }
        }
    }

    private String replaceSymbol(String meta) {
        char[] charray = meta.toCharArray();
        for (int i = 0; i < meta.length(); i++) {
            char n = meta.charAt(i);
            if (n == '\uFF0c')
                charray[i] = ',';
        }
        return new String(charray);
    }

}