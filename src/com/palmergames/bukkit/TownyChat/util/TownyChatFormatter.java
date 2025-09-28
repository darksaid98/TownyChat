package com.palmergames.bukkit.TownyChat.util;

import com.github.milkdrinkers.colorparser.ColorParser;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.listener.InternalTownyChatEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.StringMgmt;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TownyChatFormatter {
    public static @NotNull TagResolver parser(InternalTownyChatEvent e) {
        final Player p = e.getEvent().getPlayer();
        final Resident res = e.getResident();

        return TagResolver.resolver(
            TagResolver.resolver("worldname", Tag.inserting(ColorParser.of(getWorldTag(e)).parseLegacy().build())),
            TagResolver.resolver("town", Tag.inserting(ColorParser.of(getTownName(res)).parseLegacy().build())),
            TagResolver.resolver("townformatted", Tag.inserting(ColorParser.of(formatTownTag(res, false, true)).parseLegacy().build())),
            TagResolver.resolver("towntag", Tag.inserting(ColorParser.of(formatTownTag(res, false, false)).parseLegacy().build())),
            TagResolver.resolver("towntagoverride", Tag.inserting(ColorParser.of(formatTownTag(res, true, false)).parseLegacy().build())),
            TagResolver.resolver("nation", Tag.inserting(ColorParser.of(getNationName(res)).parseLegacy().build())),
            TagResolver.resolver("nationformatted", Tag.inserting(ColorParser.of(formatNationTag(res, false, true)).parseLegacy().build())),
            TagResolver.resolver("nationtag", Tag.inserting(ColorParser.of(formatNationTag(res, false, false)).parseLegacy().build())),
            TagResolver.resolver("nationtagoverride", Tag.inserting(ColorParser.of(formatNationTag(res, true, false)).parseLegacy().build())),
            TagResolver.resolver("townytag", Tag.inserting(ColorParser.of(formatTownyTag(res, false, true)).parseLegacy().build())),
            TagResolver.resolver("townyformatted", Tag.inserting(ColorParser.of(formatTownyTag(res, false, false)).parseLegacy().build())),
            TagResolver.resolver("townytagoverride", Tag.inserting(ColorParser.of(formatTownyTag(res, true, false)).parseLegacy().build())),
            TagResolver.resolver("title", Tag.inserting(ColorParser.of(getPrefix(res, true)).parseLegacy().build())),
            TagResolver.resolver("surname", Tag.inserting(ColorParser.of(getSuffix(res, false)).parseLegacy().build())),
            TagResolver.resolver("townynameprefix", Tag.inserting(ColorParser.of(getNamePrefix(res)).parseLegacy().build())),
            TagResolver.resolver("townynamepostfix", Tag.inserting(ColorParser.of(getNamePostfix(res)).parseLegacy().build())),
            TagResolver.resolver("townyprefix", Tag.inserting(ColorParser.of(getPrefix(res, false)).parseLegacy().build())),
            TagResolver.resolver("townypostfix", Tag.inserting(ColorParser.of(getSuffix(res, false)).parseLegacy().build())),
            TagResolver.resolver("townycolor", Tag.inserting(ColorParser.of(getTownyColour(res)).parseLegacy().build())),
            TagResolver.resolver("group", Tag.inserting(ColorParser.of(getVaultGroup(p)).parseLegacy().build())),
            TagResolver.resolver("permprefix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "prefix")).parseLegacy().build())),
            TagResolver.resolver("permsuffix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "suffix")).parseLegacy().build())),
            TagResolver.resolver("permuserprefix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "userprefix")).parseLegacy().build())),
            TagResolver.resolver("permusersuffix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "usersuffix")).parseLegacy().build())),
            TagResolver.resolver("permgroupprefix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "groupprefix")).parseLegacy().build())),
            TagResolver.resolver("permgroupsuffix", Tag.inserting(ColorParser.of(getVaultPrefixSuffix(res, "groupsuffix")).parseLegacy().build())),
            TagResolver.resolver("playername", Tag.inserting(ColorParser.of(p.getName()).parseLegacy().build())),
            TagResolver.resolver("modplayername", Tag.inserting(ColorParser.of(Adventure.plainText().serialize(p.displayName())).parseLegacy().build())),
            TagResolver.resolver("primaryresidentrank", Tag.inserting(ColorParser.of(getPrimaryRankPrefix(res)).parseLegacy().build()))
        );
    }


    public static String getWorldTag(InternalTownyChatEvent event) {
        return String.format(ChatSettings.getWorldTag(), event.getEvent().getPlayer().getWorld().getName());
    }

    public static String getTownName(Resident resident) {
        return resident.getTownOrNull() != null ? resident.getTownOrNull().getName() : "";
    }

    public static String getNationName(Resident resident) {
        return resident.getNationOrNull() != null ? resident.getNationOrNull().getName() : "";
    }

    /**
     * @param resident the resident
     * @param override use full names if no tag is present
     * @param full     Only use full names (no tags).
     * @return string containing the correctly formatted nation/town data
     */
    public static String formatTownyTag(Resident resident, Boolean override, Boolean full) {
        Town town = TownyAPI.getInstance().getResidentTownOrNull(resident);

        if (town == null)
            return "";

        String townTag = getTag(town);
        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(resident);
        String nationTag = nation != null ? getTag(nation) : "";
        String nTag = "", tTag = "";

        // Force use of full names only
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
        else if (nation != null && (override || full))
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

        return "";
    }

    public static String formatTownTag(Resident resident, Boolean override, Boolean full) {
        Town town = TownyAPI.getInstance().getResidentTownOrNull(resident);

        if (town == null)
            return "";

        if (full)
            return String.format(ChatSettings.getTownTag(), getName(town));
        else if (town.hasTag())
            return String.format(ChatSettings.getTownTag(), getTag(town));
        else if (override)
            return String.format(ChatSettings.getTownTag(), getName(town));

        return "";
    }

    public static String formatNationTag(Resident res, Boolean override, Boolean full) {
        Nation nation = TownyAPI.getInstance().getResidentNationOrNull(res);

        if (nation == null)
            return "";

        if (full)
            return String.format(ChatSettings.getNationTag(), getName(nation));
        else if (nation.hasTag())
            return String.format(ChatSettings.getNationTag(), getTag(nation));
        else if (override)
            return String.format(ChatSettings.getNationTag(), getName(nation));

        return "";
    }

    public static String getNamePrefix(Resident res) {
        if (res == null)
            return "";

        if (res.isKing())
            return TownySettings.getKingPrefix(res);
        else if (res.isMayor())
            return TownySettings.getMayorPrefix(res);

        return "";
    }

    public static String getNamePostfix(Resident res) {
        if (res == null)
            return "";

        if (res.isKing())
            return TownySettings.getKingPostfix(res);
        else if (res.isMayor())
            return TownySettings.getMayorPostfix(res);

        return "";
    }

    public static String getPrimaryRankPrefix(Resident res) {
        if (res == null)
            return "";

        String rank = res.getPrimaryRankPrefix();
        return rank.isEmpty() ? "" : rank + " ";
    }

    private static String getName(Government gov) {
        return StringMgmt.remUnderscore(gov.getName());
    }

    private static String getTag(Government gov) {
        return gov.getTag();
    }

    public static String getPrefix(Resident res, boolean isBlank) {
        return res.hasTitle() ? res.getTitle() + " " : isBlank ? "" : getNamePrefix(res);
    }

    public static String getSuffix(Resident res, boolean isBlank) {
        return res.hasSurname() ? " " + res.getSurname() : isBlank ? "" : getNamePostfix(res);
    }

    public static String getTownyColour(Resident res) {
        return !res.hasTown()
            ? ChatSettings.getNomadColour()
            : res.isMayor() ? (res.isKing() ? ChatSettings.getKingColour() : ChatSettings.getMayorColour()) : ChatSettings.getResidentColour();
    }

    public static String getVaultGroup(Player p) {
        return TownyUniverse.getInstance().getPermissionSource().getPlayerGroup(p);
    }

    public static String getVaultPrefixSuffix(Resident res, String slug) {
        return TownyUniverse.getInstance().getPermissionSource().getPrefixSuffix(res, slug);
    }
}
