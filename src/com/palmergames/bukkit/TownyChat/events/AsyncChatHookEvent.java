package com.palmergames.bukkit.TownyChat.events;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
public class AsyncChatHookEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    protected AsyncChatEvent event;
    protected boolean changed;
    protected Channel channel;
    protected Set<Player> recipients;
    protected Audience audience;

    public AsyncChatHookEvent(AsyncChatEvent event, Channel channel, boolean async, Set<Player> recipients) {
        super(async);
        this.event = event;
        this.changed = false;
        this.channel = channel;
        this.recipients = recipients;
        this.audience = Audience.audience(getRecipients().stream().map(player -> Chat.getTownyChat().adventure().player(player)).toArray(Audience[]::new));
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Channel getChannel() {
        return channel;
    }

    /*
     * Returns true if the hooked event was changed
     */
    public boolean isChanged() {
        return changed;
    }

    /*
     * Informs Chat if the event was changed or not
     */
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public AsyncChatEvent getAsyncPlayerChatEvent() {
        return event;
    }

    public String getFormat() {
        return "";
    }

    public void setFormat(String format) {
    }

    public String getMessage() {
        return "";
    }

    public void setMessage(String message) {
    }

    public void setMessage(Component message) {
        changed = true;
        event.message(message);
    }

    public Component message() {
        return event.message();
    }

    public Set<Player> getRecipients() {
        return this.recipients;
    }

    public void setRecipients(Set<Player> recipients) {
        changed = true;
        this.recipients.clear();
        this.recipients.addAll(recipients);
        setAudience(Audience.audience(this.recipients.stream().map(player -> Chat.getTownyChat().adventure().player(player)).toArray(Audience[]::new))); // Update audience as well
    }

    public Audience getAudience() {
        return this.audience;
    }

    public void setAudience(Audience audience) {
        changed = true;
        this.audience = audience;
    }

    public boolean isCancelled() {
        return event.isCancelled();
    }

    public void setCancelled(boolean cancel) {
        changed = (cancel);
        event.setCancelled(cancel);
    }

    public Player getPlayer() {
        return event.getPlayer();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
