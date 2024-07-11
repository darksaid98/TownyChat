package com.palmergames.bukkit.TownyChat.events;

import com.github.milkdrinkers.colorparser.ColorParser;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.util.Adventure;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/*
 * Allows other plugins to hook into a chat message being accepted into any of the channels
 *
 * In order to use this event, you will have to add the hooked: true flag to the channel you wish you hook in the channels.yml
 *
 * ex:
 *
 *Channels:
 *  general:
 *      commands: [g]
 *      type: GLOBAL
 *      hooked: true
 *      channeltag: '&f[g]'
 *      messagecolour: '&f'
 *      permission: 'towny.chat.general'
 *      range: '-1'
 */
public class AsyncChatHookEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private String legacyMessage;
    private boolean cancelled;

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private final AsyncChatEvent event;
    private final Channel channel;
    private final Set<Player> recipients;

    public AsyncChatHookEvent(AsyncChatEvent e, Channel channel, Set<Player> recipients) {
        super(e.isAsynchronous());
        this.event = e;
        this.cancelled = e.isCancelled();
        this.legacyMessage = Adventure.plainText().serialize(message());

        this.channel = channel;
        this.recipients = recipients;
    }

    public AsyncChatEvent getEvent() {
        return event;
    }

    @Deprecated
    public String getFormat() {
        return "undefined";
    }

    @Deprecated
    public void setFormat(String format) {
    }

    @Deprecated
    public String getMessage() {
        return legacyMessage;
    }

    @Deprecated
    public void setMessage(String message) {
        legacyMessage = message;
    }

    public Component message() {
        return event.message();
    }

    public void message(Component message) {
        event.message(message);
    }

    public Set<Player> getRecipients() {
        return this.recipients;
    }

    public void setRecipients(Set<Player> recipients) {
        this.recipients.clear();
        this.recipients.addAll(recipients);
    }

    public Player getPlayer() {
        return event.getPlayer();
    }

    public Channel getChannel() {
        return channel;
    }

    /*
     * Returns true if the hooked event was changed
     */
    @Deprecated(forRemoval = true)
    public boolean isChanged() {
        return true;
    }

    /*
     * Informs Chat if the event was changed or not
     */
    @Deprecated(forRemoval = true)
    public void setChanged(boolean changed) {

    }
}
