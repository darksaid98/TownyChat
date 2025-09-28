package com.palmergames.bukkit.TownyChat.listener;

import com.palmergames.bukkit.towny.object.Resident;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;

@Deprecated(forRemoval = true)
public class InternalTownyChatEvent {
    private final AsyncChatEvent event;
    private final Resident resident;

    public InternalTownyChatEvent(AsyncChatEvent event, Resident resident) {
        this.event = event;
        this.resident = resident;
    }

    /**
     * Get the resident associated with the chat event's talking player.
     *
     * @return resident associated with the chat event's talking player
     */
    public Resident getResident() {
        return resident;
    }

    /**
     * @return the AsyncChatEvent
     */
    public AsyncChatEvent getEvent() {
        return event;
    }

    /**
     * Convenience method for getting the chat event's format
     *
     * @return the chat event's format
     */
    @Deprecated(forRemoval = true)
    public String getFormat() {
        return "";
    }

    /**
     * Convenience method for setting the chat event's format.
     *
     * @param format the chat event's format
     */
    @Deprecated(forRemoval = true)
    public void setFormat(String format) {
    }

    /**
     * Convenience method for getting the chat event's message
     *
     * @return the chat event's message
     */
    public String getMessage() {
        return "";
    }

    /**
     * Get the event message
     * @return event message
     */
    public Component message() {
        return event.message();
    }

    /**
     * Set the event message
     * @param message event message
     */
    public void message(Component message) {
        event.message(message);
    }
}
