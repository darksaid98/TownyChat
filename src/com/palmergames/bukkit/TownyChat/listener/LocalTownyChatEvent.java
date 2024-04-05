package com.palmergames.bukkit.TownyChat.listener;

import com.palmergames.bukkit.towny.object.Resident;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class LocalTownyChatEvent {
    private AsyncChatEvent event;
    private Resident resident;

    public LocalTownyChatEvent(AsyncChatEvent event, Resident resident) {
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
    @Deprecated
    public String getFormat() {
        return "";
    }

    /**
     * Convenience method for setting the chat event's format.
     *
     * @param format
     */
    @Deprecated
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

    public @NotNull Component message() {
        return event.message();
    }
}
