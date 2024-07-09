package com.palmergames.bukkit.TownyChat.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Adventure {
    private static final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static PlainTextComponentSerializer plainText() {
        return plainText;
    }

    public static MiniMessage miniMessage() {
        return miniMessage;
    }
}
