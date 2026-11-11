package net.sourceforge.kolmafia.session;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.jsoup.Jsoup;

public class LegendaryAcorndionManager {
  public enum AcornZoneEffect {
    PASSIVE_DMG("Monsters take 10-20 damage each round in this zone"),
    WEAKEN("Weaken monsters in this zone by 30%"),
    STAT_GAIN("+15% to all Stat Gains from Fights in this zone"),
    PASSIVE_DMG_PERCENT("Monsters take 10-20% of their HP in damage each round in this zone"),
    DAMAGE_REDUCTION("Damage Taken from Monsters -40% in this zone"),
    NONCOMBAT_5("Monsters will be less attracted to you in this zone"),
    STAT_BUFF("All Attributes +100% during fights in this zone"),
    SEA_FRUIT("Occasionally find sea fruit after fights in this zone"),
    BANISH("This monster will no longer appear in this zone"),
    MHP_DEBUFF("Monsters start each fight with 25% less HP in this zone"),
    FRUIT("Occasionally find fruit after fights in this zone"),
    NONCOMBAT_10("Monsters will be much less attracted to you in this zone"),
    PASSIVE_DMG_SPOOKY(
        "Monsters take 10-20 <font color=\"gray\">Spooky</font> damage each round in this zone"),
    ENTANGLE("Monsters will sometimes be too tangled up to attack you in this zone"),
    SNIFF("This monster will occur four times more often in this zone"),
    BRIDGE_PARTS("Occasionally find building materials after fights in this zone"),
    PASSIVE_DMG_SLEAZE(
        "Monsters take 10-20 <font color=\"blueviolet\">Sleaze</font> damage each round in this zone"),
    RESTORE("Restores 20-30% HP and 10-20% MP after fights in this zone"),
    SEA_COWS("Sea cows will appear in this zone"),
    INITIATIVE("+50% Combat Initiative in this zone"),
    ITEM_DROPS("+30% Item Drops from Monsters in this zone"),
    STUN("Monsters are stunned for the first 3 rounds of each fight in this zone"),
    BETTER_DIVER("Underwater pressure -30% in this zone");

    public final String fightText;
    public final String descText;

    AcornZoneEffect(String fightText) {
      this.fightText = fightText;
      this.descText =
          fightText.replaceFirst(" in this zone", "").replaceFirst("This monster", "").trim();
    }

    public static EnumSet<AcornZoneEffect> forSkill(int skillId) {
      return switch (skillId) {
        case SkillPool.DISSONANT_RIFF, SkillPool.IRRITATING_RIFF ->
            EnumSet.of(PASSIVE_DMG, WEAKEN, STAT_GAIN, PASSIVE_DMG_PERCENT);
        case SkillPool.CADENZA, SkillPool.BADENZA ->
            EnumSet.of(DAMAGE_REDUCTION, NONCOMBAT_5, STAT_BUFF, SEA_FRUIT);
        case SkillPool.ACCORDION_BASH, SkillPool.ACCORDION_SHOVE ->
            EnumSet.of(BANISH, MHP_DEBUFF, FRUIT, NONCOMBAT_10);
        case SkillPool.CONCERTO_DE_LOS_MUERTOS, SkillPool.CHIASSO_DE_LOS_MUERTOS ->
            EnumSet.of(PASSIVE_DMG_SPOOKY, ENTANGLE, SNIFF, BANISH);
        case SkillPool.BAWDY_REFRAIN, SkillPool.JUVENILE_REFRAIN ->
            EnumSet.of(BRIDGE_PARTS, PASSIVE_DMG_SLEAZE, RESTORE, SEA_COWS);
        case SkillPool.CONE_OF_ZYDECO, SkillPool.LINE_OF_ZYDECO ->
            EnumSet.of(INITIATIVE, ITEM_DROPS, STUN, BETTER_DIVER);
        default -> EnumSet.noneOf(AcornZoneEffect.class);
      };
    }
  }

  public static void plantAcorn(KoLAdventure location, AcornZoneEffect effect) {
    String locationName = location.getAdventureName();
    Map<String, String> prevAcorns = Preferences.getStringMap("_legendaryAcornZonesToday");
    if (prevAcorns.containsKey(locationName)) {
      int prevCount = Integer.parseInt(prevAcorns.get(locationName));
      prevAcorns.put(locationName, Integer.toString(prevCount + 1));
    } else {
      prevAcorns.put(locationName, "1");
    }
    Preferences.setStringMap("_legendaryAcornZonesToday", prevAcorns);
  }

  public static void parseLegendaryAcorndionZoneEffects(final String desc) {
    if (!desc.contains("Current Zone Effects")) {
      Preferences.setString("legendaryAcornsPlanted", "");
      return;
    }

    Map<String, String> acornZones = new HashMap<>();
    var parsed = Jsoup.parse(desc);
    var locations = parsed.select("li:has(ul)");
    for (var location : locations) {
      String locationName = location.select("b").text();

      StringBuilder sb = new StringBuilder();
      var locEffects = location.select("font[color=blue]");
      for (var locEffect : locEffects) {
        String effHtml = locEffect.html();
        for (AcornZoneEffect acornZoneEffect : AcornZoneEffect.values()) {
          if (effHtml.contains(acornZoneEffect.descText)) {
            sb.append(acornZoneEffect.name()).append(',');
            break;
          }
        }
      }
      sb.setLength(sb.length() - 1);
      acornZones.put(locationName, sb.toString());
    }
    Preferences.setStringMap("legendaryAcornsPlanted", acornZones);
  }
}
