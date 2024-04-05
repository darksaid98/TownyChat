package com.palmergames.bukkit.TownyChat;

import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.listener.LocalTownyChatEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.StringMgmt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.entity.Player;

public class TownyChatFormatter {
    static TagResolver parser(LocalTownyChatEvent event) {
        return TagResolver.resolver(
            TagResolver.resolver("worldname", Tag.inserting(Component.text(getWorldTag(event)))),
            TagResolver.resolver("town", Tag.inserting(Component.text(getTownName(event.getResident())))),
            TagResolver.resolver("townformatted", Tag.inserting(Component.text(formatTownTag(event.getResident(), false, true)))),
            TagResolver.resolver("towntag", Tag.inserting(Component.text(formatTownTag(event.getResident(), false, false)))),
            TagResolver.resolver("towntagoverride", Tag.inserting(Component.text(formatTownTag(event.getResident(), true, false)))),
            TagResolver.resolver("nation", Tag.inserting(Component.text(getNationName(event.getResident())))),
            TagResolver.resolver("nationformatted", Tag.inserting(Component.text(formatNationTag(event.getResident(), false, true)))),
            TagResolver.resolver("nationtag", Tag.inserting(Component.text(formatNationTag(event.getResident(), false, false)))),
            TagResolver.resolver("nationtagoverride", Tag.inserting(Component.text(formatNationTag(event.getResident(), true, false)))),
            TagResolver.resolver("townytag", Tag.inserting(Component.text(formatTownyTag(event.getResident(), false, true)))),
            TagResolver.resolver("townyformatted", Tag.inserting(Component.text(formatTownyTag(event.getResident(), false, false)))),
            TagResolver.resolver("townytagoverride", Tag.inserting(Component.text(formatTownyTag(event.getResident(), true, false)))),
            TagResolver.resolver("title", Tag.inserting(Component.text(getPrefix(event.getResident(), true)))),
            TagResolver.resolver("surname", Tag.inserting(Component.text(getSuffix(event.getResident(), false)))),
            TagResolver.resolver("townynameprefix", Tag.inserting(Component.text(getNamePrefix(event.getResident())))),
            TagResolver.resolver("townynamepostfix", Tag.inserting(Component.text(getNamePostfix(event.getResident())))),
            TagResolver.resolver("townyprefix", Tag.inserting(Component.text(getPrefix(event.getResident(), false)))),
            TagResolver.resolver("townypostfix", Tag.inserting(Component.text(getSuffix(event.getResident(), false)))),
            TagResolver.resolver("townycolor", Tag.inserting(Component.text(getTownyColour(event.getResident())))),
            TagResolver.resolver("group", Tag.inserting(Component.text(getVaultGroup(event.getEvent().getPlayer())))),
            TagResolver.resolver("permprefix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "prefix")))),
            TagResolver.resolver("permsuffix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "suffix")))),
            TagResolver.resolver("permuserprefix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "userprefix")))),
            TagResolver.resolver("permusersuffix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "usersuffix")))),
            TagResolver.resolver("permgroupprefix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "groupprefix")))),
            TagResolver.resolver("permgroupsuffix", Tag.inserting(Component.text(getVaultPrefixSuffix(event.getResident(), "groupsuffix")))),
            TagResolver.resolver("playername", Tag.inserting(Component.text(event.getEvent().getPlayer().getName()))),
            TagResolver.resolver("primaryresidentrank", Tag.inserting(Component.text(getPrimaryRankPrefix(event.getResident()))))
        );
    }

    public static Component parseMessage(String message, LocalTownyChatEvent event) {
        final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.builder()
                .resolver(parser(event))
                .resolver(StandardTags.defaults())
                .build())
            .build();

        return miniMessage.deserialize(message);
    }

    public static TagResolver getChatFormat(LocalTownyChatEvent event) {
        return TagResolver.builder()
            .resolver(parser(event))
            .resolver(StandardTags.defaults())
            .build();
    }

    public static String getWorldTag(LocalTownyChatEvent event) {
        return String.format(ChatSettings.getWorldTag(), event.getEvent().getPlayer().getWorld().getName());
    }

    public static String getTownName(Resident resident) {
        return resident.hasTown() ? resident.getTownOrNull().getName() : "";
    }

    public static String getNationName(Resident resident) {
        return resident.hasNation() ? resident.getNationOrNull().getName() : "";
    }

    /**
     * @param resident
     * @param override use full names if no tag is present
     * @param full     Only use full names (no tags).
     * @return string containing the correctly formatted nation/town data
     */
    public static String formatTownyTag(Resident resident, Boolean override, Boolean full) {
        if (resident.hasTown()) {
            Town town = TownyAPI.getInstance().getResidentTownOrNull(resident);
            String townTag = getTag(town);
            Nation nation = null;
            String nationTag = null;
            if (resident.hasNation()) {
                nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
                nationTag = getTag(nation);
            }

            String nTag = "", tTag = "";

            //Force use of full names only
            if (full) {
                nationTag = "";
                townTag = "";
            }
            // Load town tags/names
            if (townTag != null && !townTag.isEmpty())
                tTag = townTag;
            else if (override || full)
                tTag = getName(town);

            // Load the nation tags/names
            if ((nationTag != null) && !nationTag.isEmpty())
                nTag = nationTag;
            else if (resident.hasNation() && (override || full))
                nTag = getName(nation);

            // Output depending on what tags are present
            if ((!tTag.isEmpty()) && (!nTag.isEmpty())) {
                if (ChatSettings.getBothTag().contains("%t") || ChatSettings.getBothTag().contains("%n")) {
                    // Then it contains %s & %s
                    // Small suttle change so that an issue is solved, it is documented in the config.
                    // But only after addition of this. (v0.50)
                    return ChatSettings.getBothTag().replace("%t", tTag).replace("%n", nTag);
                } else {
                    return String.format(ChatSettings.getBothTag(), nTag, tTag);
                }
            }

            if (!nTag.isEmpty()) {
                return String.format(ChatSettings.getNationTag(), nTag);
            }

            if (!tTag.isEmpty()) {
                return String.format(ChatSettings.getTownTag(), tTag);
            }

        }
        return "";
    }

    public static String formatTownTag(Resident resident, Boolean override, Boolean full) {
        if (resident.hasTown()) {
            Town town = TownyAPI.getInstance().getResidentTownOrNull(resident);
            if (full)
                return String.format(ChatSettings.getTownTag(), getName(town));
            else if (town.hasTag())
                return String.format(ChatSettings.getTownTag(), getTag(town));
            else if (override)
                return String.format(ChatSettings.getTownTag(), getName(town));

        }
        return "";
    }

    public static String formatNationTag(Resident resident, Boolean override, Boolean full) {
        if (resident.hasNation()) {
            Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
            if (full)
                return String.format(ChatSettings.getNationTag(), getName(nation));
            else if (nation.hasTag())
                return String.format(ChatSettings.getNationTag(), getTag(nation));
            else if (override)
                return String.format(ChatSettings.getNationTag(), getName(nation));
        }
        return "";
    }

    public static String getNamePrefix(Resident resident) {

        if (resident == null)
            return "";
        if (resident.isKing())
            return TownySettings.getKingPrefix(resident);
        else if (resident.isMayor())
            return TownySettings.getMayorPrefix(resident);
        return "";
    }

    public static String getNamePostfix(Resident resident) {

        if (resident == null)
            return "";
        if (resident.isKing())
            return TownySettings.getKingPostfix(resident);
        else if (resident.isMayor())
            return TownySettings.getMayorPostfix(resident);
        return "";
    }

    public static String getPrimaryRankPrefix(Resident resident) {
        if (resident == null)
            return "";
        String rank = resident.getPrimaryRankPrefix();
        return rank.isEmpty() ? "" : rank + " ";
    }

    private static String getName(Government gov) {
        return StringMgmt.remUnderscore(gov.getName());
    }

    private static String getTag(Government gov) {
        return gov.getTag();
    }

    public static String getPrefix(Resident resident, boolean blank) {
        return resident.hasTitle() ? resident.getTitle() + " " : blank ? "" : getNamePrefix(resident);
    }

    public static String getSuffix(Resident resident, boolean blank) {
        return resident.hasSurname() ? " " + resident.getSurname() : blank ? "" : getNamePostfix(resident);
    }

    public static String getTownyColour(Resident resident) {
        return !resident.hasTown()
            ? ChatSettings.getNomadColour()
            : resident.isMayor() ? (resident.isKing() ? ChatSettings.getKingColour() : ChatSettings.getMayorColour()) : ChatSettings.getResidentColour();
    }

    public static String getVaultGroup(Player player) {
        return TownyUniverse.getInstance().getPermissionSource().getPlayerGroup(player);
    }

    public static String getVaultPrefixSuffix(Resident resident, String slug) {
        return TownyUniverse.getInstance().getPermissionSource().getPrefixSuffix(resident, slug);
    }
}
