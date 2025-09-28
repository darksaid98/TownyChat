package com.palmergames.bukkit.TownyChat.channels;

import com.github.milkdrinkers.colorparser.ColorParser;
import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;
import com.palmergames.bukkit.TownyChat.util.Adventure;
import com.palmergames.bukkit.TownyChat.util.TownyChatFormatter;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.events.PlayerJoinChatChannelEvent;
import com.palmergames.bukkit.TownyChat.listener.InternalTownyChatEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.Colors;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.dynmap.DynmapAPI;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class StandardChannel extends Channel {
    private final Chat plugin;

    public StandardChannel(Chat instance, String name) {
        super(name);
        this.plugin = instance;
    }

    @Deprecated
    public void chatProcess(AsyncPlayerChatEvent e) {
        IllegalCallerException illegalCallerException = new IllegalCallerException("A plugin is using the deprecated method Channel#chatProcess(AsyncPlayerChatEvent) which is no longer supported. Please use Channel#chatProcess(AsyncChatEvent)!");
        Chat.getTownyChat().getComponentLogger().warn(
            Component.text(illegalCallerException + Arrays.toString(illegalCallerException.getStackTrace()))
        );
    }

    public void chatProcess(AsyncChatEvent e) {
        ChannelTypes channelType = this.getType();
        Player p = e.getPlayer();
        Resident res = TownyAPI.getInstance().getResident(p);
        boolean notifyjoin = false;

        if (res == null) {
            e.setCancelled(true);
            return;
        }

        Town town = TownyAPI.getInstance().getResidentTownOrNull(res);
        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(res);

        // If the channel would require a town/nation which is null, cancel and fail early.
        if (
            town == null && channelType.equals(ChannelTypes.TOWN) ||
            nation == null && (channelType.equals(ChannelTypes.NATION) || channelType.equals(ChannelTypes.ALLIANCE))
        ) {
            e.setCancelled(true);
            return;
        }

        // If player sends a message in a channel they have left, rejoin the channel
        if (isAbsent(p.getName())) {
            join(p);
            notifyjoin = true;
            Bukkit.getPluginManager().callEvent(new PlayerJoinChatChannelEvent(p, this));
        }

        final String channelMessageString = Chat.usingPlaceholderAPI ?
            PlaceholderAPI.setPlaceholders(p, getFormat(p)) :
            getFormat(p);

        final InternalTownyChatEvent chatEvent = new InternalTownyChatEvent(e, res);
        final Component finalMessage = MiniMessage.builder()
            .tags(
                TagResolver.builder()
                    .resolvers(
                        TownyChatFormatter.parser(chatEvent),
                        TagResolver.resolver(
                            "channeltag", Tag.inserting(ColorParser.of(this.getChannelTag()).parseLegacy().build())
                        ),
                        TagResolver.resolver(
                            "msgcolour", Tag.inserting(ColorParser.of(this.getMessageColour()).parseLegacy().build())
                        ),
                        StandardTags.defaults(),
                        TagResolver.resolver( // TODO Parse color codes, the player has permissions to use
                            "msg", Tag.inserting(e.message())
                        )
                    )
                    .build()
            )
            .build().deserialize(channelMessageString);

        // Get recipients
        final Set<Player> recipients = getRecipients(p, town, nation, channelType);

        // Try sending an alone message if the player is alone in a channel.
        trySendingAloneMessage(p, recipients);

        // Calculate recipients and set recipients
        e.message(finalMessage);
        e.viewers().clear();
        e.viewers().add(Audience.audience(recipients));

        // If the server has marked this Channel as hooked, fire the AsyncChatHookEvent.
        // If the event is cancelled, cancel the chat entirely.
        // Fires its own sendSpyMessage().
        if (isHooked()) {
            AsyncChatHookEvent hookEvent = new AsyncChatHookEvent(e, this, recipients);
            Bukkit.getServer().getPluginManager().callEvent(hookEvent);
            if (hookEvent.isCancelled()) {
                e.setCancelled(true);
                return;
            }

            e.viewers().clear();
            e.viewers().add(Audience.audience(hookEvent.getRecipients()));

            /*
             * Send spy message before another plugin changes any of the recipients, so we
             * know which people can see it.
             */
            sendSpyMessage(e, channelType, recipients);
        }

        // Add console as listener and set custom rendered for message
        e.viewers().add(Bukkit.getServer().getConsoleSender());
        e.renderer(
            (source, sourceDisplayName, message, viewer) -> e.message()
        );

        /*
         * Send spy message before another plugin changes any of the recipients, so we
         * know which people can see it.
         */
        sendSpyMessage(e, channelType, recipients);

        // Play the channel sound, if used.
        tryPlayChannelSound(recipients);

        if (notifyjoin)
            TownyMessaging.sendMessage(p, "You join " + Colors.translateColorCodes(getMessageColour()) + getName());

        /*
         * Perform any last channel specific functions like logging this chat and
         * relaying to Dynmap.
         */
        switch (channelType) {
            case TOWN:
            case NATION:
            case ALLIANCE:
            case DEFAULT:
                break;
            case PRIVATE:
            case GLOBAL:
                tryPostToDynmap(p, e.message());
                break;
        }
    }

    private Set<Player> getRecipients(Player player, Town town, Nation nation, ChannelTypes channelType) {
        return switch (channelType) {
            case TOWN -> new HashSet<>(findRecipients(player, TownyAPI.getInstance().getOnlinePlayers(town)));
            case NATION -> new HashSet<>(findRecipients(player, TownyAPI.getInstance().getOnlinePlayers(nation)));
            case ALLIANCE -> new HashSet<>(findRecipients(player, TownyAPI.getInstance().getOnlinePlayersAlliance(nation)));
            case DEFAULT, GLOBAL, PRIVATE -> new HashSet<>(findRecipients(player, new ArrayList<>(Bukkit.getOnlinePlayers())));
        };
    }

    /**
     * Compile a list of valid recipients for this message.
     *
     * @param sender
     * @param playerList
     * @return Set containing a list of players for this message.
     */
    private Set<Player> findRecipients(Player sender, List<Player> playerList) {
        // Refresh the potential channels a player can see, if they are not currently in the channel.
        playerList.forEach(p -> refreshPlayer(this, p));
        return playerList.stream()
            .filter(p -> testDistance(sender, p, getRange())) // Within range.
            .filter(p -> !plugin.isIgnoredByEssentials(sender, p)) // Check essentials ignore.
            .filter(p -> !isAbsent(p.getName())) // Check if player is purposefully absent.
            .collect(Collectors.toSet());
    }

    private void refreshPlayer(Channel channel, Player player) {
        if (!channel.isPresent(player.getName()))
            channel.forgetPlayer(player);
    }

    /**
     * Check the distance between players and return a result based upon the range setting
     * -1 = no limit
     * 0 = same world
     * any positive value = distance in blocks
     *
     * @param player1
     * @param player2
     * @param range
     * @return true if in range
     */
    private boolean testDistance(Player player1, Player player2, double range) {

        // unlimited range (all worlds)
        if (range == -1)
            return true;

        // Same world only
        if (range == 0)
            return player1.getWorld().equals(player2.getWorld());

        // Range check (same world)
        return player1.getWorld().equals(player2.getWorld()) &&
            player1.getLocation().distance(player2.getLocation()) < range;
    }

    private void trySendingAloneMessage(Player sender, Set<Player> recipients) {
        if (ChatSettings.isUsingAloneMessage() &&
            recipients.stream().filter(sender::canSee).count() < 2) // sender will usually be a recipient of their own message.
            TownyMessaging.sendMessage(sender, ChatSettings.getUsingAloneMessageString());
    }

    /**
     * Sends messages to spies who have not already seen the message naturally.
     *
     * @param event - Chat Event.
     * @param type  - Channel Type
     */
    private void sendSpyMessage(AsyncChatEvent event, ChannelTypes type, Set<Player> recipients) {
        Set<Player> spies = getSpies();
        String format = formatSpyMessage(type, event.getPlayer());
        if (format == null) return;

        String message = Adventure.plainText().serialize(event.message());
        // Remove spies who've already seen the message naturally.
        spies.stream()
            .filter(spy -> !recipients.contains(spy))
            .forEach(spy -> TownyMessaging.sendMessage(spy, format + message));
    }

    /**
     * @return A Set of online players who are spying.
     */
    private Set<Player> getSpies() {
        // Compile the list of recipients with spy perms
        return plugin.getServer().getOnlinePlayers().stream()
            .filter(p -> plugin.getTowny().hasPlayerMode(p, "spy"))
            .collect(Collectors.toSet());
    }

    /**
     * Formats look of message for spies
     *
     * @param type   - Channel Type.
     * @param player - Player who chatted.
     * @return format - Message format.
     */
    @Nullable
    private String formatSpyMessage(ChannelTypes type, Player player) {
        Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
        if (resident == null)
            return null;
        String channelPrefix = Colors.translateColorCodes(getChannelTag() != null ? getChannelTag() : getName()) + " ";
        if (isGovernmentChannel()) // Town, Nation, Alliance channels get an extra [Name] added after the channelPrefix.
            channelPrefix = getGovtChannelSpyingPrefix(resident, type, channelPrefix);
        return ChatColor.GOLD + "[SPY] " + ChatColor.WHITE + channelPrefix + resident.getName() + ": ";
    }

    private String getGovtChannelSpyingPrefix(Resident resident, ChannelTypes type, String channelPrefix) {
        String slug = type.equals(ChannelTypes.TOWN)
            ? TownyAPI.getInstance().getResidentTownOrNull(resident).getName()    // Town chat.
            : TownyAPI.getInstance().getResidentNationOrNull(resident).getName(); // Nation or Alliance chat.
        return channelPrefix + "[" + slug + "] ";
    }

    /**
     * Try to send a channel sound, if enabled.
     *
     * @param recipients Set of Players that will receive the message and potentially the sound.
     */
    private void tryPlayChannelSound(Set<Player> recipients) {
        if (getChannelSound() == null)
            return;
        for (Player recipient : recipients) {
            if (!isSoundMuted(recipient)) {
                try {
                    recipient.playSound(recipient, Sound.valueOf(getChannelSound()), 1.0f, 1.0f);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Channel " + this.getName() + " has an invalid sound configured.");
                    setChannelSound(null);
                    break;
                }
            }
        }
    }

    /**
     * Try to send a message to dynmap's web chat.
     *
     * @param player  Player which has spoken.
     * @param message Message being spoken.
     */
    private void tryPostToDynmap(Player player, Component message) {
        if (super.getRange() > 0)
            return;
        DynmapAPI dynMap = plugin.getDynmap();
        if (dynMap != null)
            dynMap.postPlayerMessageToWeb(player, Adventure.plainText().serialize(message));
    }
}
