package com.palmergames.bukkit.TownyChat.listener;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.TownyChatFormatter;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.channels.channelTypes;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.util.Colors;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class TownyChatPlayerListener implements Listener {
    public WeakHashMap<Player, String> directedChat = new WeakHashMap<>();
    private Chat plugin;

    public TownyChatPlayerListener(Chat instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(final PlayerJoinEvent event) {

        plugin.getScheduler().runLater(event.getPlayer(), () -> loginPlayer(event.getPlayer()), 2L);

    }

    private void loginPlayer(Player player) {
        checkPlayerForOldMeta(player);

        refreshPlayerChannels(player);

        Channel channel = plugin.getChannelsHandler().getDefaultChannel();
        if (channel != null && channel.hasSpeakPermission(player)) {
            plugin.setPlayerChannel(player, channel);
            if (ChatSettings.getShowChannelMessageOnServerJoin())
                TownyMessaging.sendMessage(player, Translatable.of("tc_you_are_now_talking_in_channel", channel.getName()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        refreshPlayerChannels(event.getPlayer());
    }

    private void refreshPlayerChannels(Player player) {
        plugin.getChannelsHandler().getAllChannels().values().stream().forEach(channel -> channel.forgetPlayer(player));
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        final String messageContent = PlainTextComponentSerializer.plainText().serializeOr(event.message(), "");
        Component message = event.message();
        String messageString = message.toString();

        // TODO START HERE ROOOOOSE :))) This is where the logic "begins"
        // Check if the message contains colour codes we need to remove or parse.
        testColourCodes(event, player);

        // Check if essentials has this player muted.
        if (!isEssentialsMuted(player)) {

            boolean forceGlobal = ChatSettings.isExclamationPoint() && messageString.startsWith("!");

            /*
             * If this was directed chat send it via the relevant channel
             */
            if (directedChat.containsKey(player)) {
                Channel channel = plugin.getChannelsHandler().getChannel(directedChat.get(player));

                if (channel != null) {
                    if (isMutedOrSpam(event, channel, player)) {
                        directedChat.remove(player);
                        return;
                    }
                    channel.chatProcess(event);
                    if (!Chat.usingEssentialsDiscord || event.isCancelled()) {
                        directedChat.remove(player);
                    }
                    return;
                }
                directedChat.remove(player);
            }

            /*
             * Check the player for any channel modes.
             */
            Channel channel = plugin.getPlayerChannel(player);
            if (!forceGlobal && channel != null && channel.hasSpeakPermission(player)) {
                if (isMutedOrSpam(event, channel, player))
                    return;
                channel.chatProcess(event);
                return;
            }

            /*
             *  Find a global channel this player has permissions for.
             */
            channel = plugin.getChannelsHandler().getActiveChannel(player, channelTypes.GLOBAL, forceGlobal);
            if (channel != null) {
                if (isMutedOrSpam(event, channel, player))
                    return;
                if (forceGlobal)
                    event.message(Component.text(messageString.substring(1)));
                channel.chatProcess(event);
                return;
            }
        }

        /*
         * We found no channels available so modify the chat (if enabled) and exit.
         */
        if (ChatSettings.isModify_chat()) {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null)
                return;
            // Nuke the channeltag and message colouring, but apply the remaining format.
            String format = plugin.getPlayerChannel(player).getFormat(player);
            String newFormat = format;

            // format is left to store the original non-PAPI-parsed chat format.
            List<TagResolver> tagResolvers = new ArrayList<>();
            tagResolvers.add(
                TagResolver.resolver(
                    TagResolver.resolver("channeltag", Tag.inserting(Component.empty())),
                    TagResolver.resolver("msgcolour", Tag.inserting(Component.empty()))
                )
            );

            // Parse any PAPI placeholders.
            if (Chat.usingPlaceholderAPI)
                newFormat = PlaceholderAPI.setPlaceholders(player, format);

            // Attempt to apply the new format. //TODO potentially reimplement this
//			catchFormatConversionException(event, format, newFormat);

            // Fire the LocalTownyChatEvent.
            LocalTownyChatEvent chatEvent = new LocalTownyChatEvent(event, resident);

            // Format the chat line, replacing the TownyChat chat tags.
            tagResolvers.add(TownyChatFormatter.getChatFormat(chatEvent));
//            Component newMessage = MiniMessage.builder().
            Component newMessage = MiniMessage.builder()
                .tags(
                    TagResolver.resolver(
                        Stream.concat(
                            tagResolvers.stream(),
                            List.of(
                                StandardTags.hoverEvent(),
                                StandardTags.clickEvent(),
                                StandardTags.color(),
                                StandardTags.keybind(),
                                StandardTags.transition(),
                                StandardTags.insertion(),
                                StandardTags.font(),
                                StandardTags.decorations(),
                                StandardTags.gradient(),
                                StandardTags.rainbow(),
                                StandardTags.reset(),
                                StandardTags.newline()
                            ).stream()
                        ).toList()
                    )
                )
                .build()
                .deserialize(newFormat);

            /*event.renderer((player1, component, component1, audience) -> {
                final @NotNull Optional<UUID> recipientUUID = audience.get(Identity.UUID);
                final Audience recipientViewer = audience;

                Component renderedMessage = renderedMessage;
                for (final var renderer : this.renderers()) {
                    renderedMessage = renderer.render(this.sender, recipientViewer, renderedMessage, renderedMessage);
                }
                return renderedMessage;
            });*/
//			newFormat = TownyChatFormatter.getChatFormat(chatEvent);

            // Attempt to apply the new format.
//			catchFormatConversionException(event, format, newFormat);

            // Set the format based on the global channelformat, with channeltag and msgcolour removed.
//			event.setFormat(newFormat);

            // The lines below were added by dark
//			Component component = Chat.getTownyChat().getMiniMessage().deserialize(newFormat, tagResolvers.toArray(new TagResolver[0]));
// 			Audience.audience(event.getRecipients().stream().map(p -> Chat.getTownyChat().adventure().player(p)).toArray(Audience[]::new)).sendMessage(component);;

            // TODO unsure if this works lol
            Audience.audience(event.viewers()).sendMessage(newMessage);

//			event.setFormat(GsonComponentSerializer.gson().serialize(component));
        }
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

    /**
     * Is this player Muted via Essentials?
     *
     * @param player {@link Player} speaking.
     * @return true if muted by Essentials.
     */
    private boolean isEssentialsMuted(Player player) {
        // Check if essentials has this player muted.
        if (plugin.isEssentialsMuted(player)) {
            TownyMessaging.sendErrorMsg(player, Translatable.of("tc_err_unable_to_talk_essentials_mute"));
            return true;
        }
        return false;
    }

    /**
     * Check if the player is channel-muted or channel-spamming and cancel the
     * {@link AsyncChatEvent} if this is the case.
     *
     * @param event   {@link AsyncChatEvent} which has fired.
     * @param channel {@link Channel} being spoken in to.
     * @param player  {@link Player} speaking.
     * @return true if the chat is muted or spammed.
     */
    private boolean isMutedOrSpam(AsyncChatEvent event, Channel channel, Player player) {
        if (channel.isMuted(player.getName())) {
            TownyMessaging.sendErrorMsg(player, Translatable.of("tc_err_you_are_currently_muted_in_channel", channel.getName()));
            event.setCancelled(true);
            return true;
        }
        if (channel.isSpam(player)) {
            event.setCancelled(true);
            return true;
        }
        return false;
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

    private void catchFormatConversionException(AsyncPlayerChatEvent event, String format, String newFormat) {
        try {
            event.setFormat(newFormat);
        } catch (UnknownFormatConversionException e) {
            // This exception is usually thrown when a PAPI placeholder did not get parsed
            // and has left behind a % symbol followed by something that String#format
            // cannot handle.
            boolean percentSymbol = format.contains("%" + e.getConversion());
            String errmsg = "TownyChat tried to apply a chat format that is not allowed: '" +
                newFormat + "', because of the " + e.getConversion() + " symbol" +
                (percentSymbol ? ", found after a %. There is probably a PAPIPlaceholder that could not be parsed." : "." +
                    " You should attempt to correct this in your towny\\settings\\chatconfig.yml file and use /townychat reload.");
            Chat.getTownyChat().getLogger().severe(errmsg);

            if (percentSymbol)
                // Attempt to remove the unparsed placeholder and send this right back.
                catchFormatConversionException(event, format, purgePAPI(newFormat, "%" + e.getConversion()));
            else
                // Just let the chat go, this results in an error in the log, and TownyChat not being able to format chat.
                event.setFormat(format);
        }
    }

    private String purgePAPI(String format, String startOfPlaceholder) {
        return format.replaceAll(startOfPlaceholder + ".*%", "");
    }
}