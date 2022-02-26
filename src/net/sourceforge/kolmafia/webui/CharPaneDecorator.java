package net.sourceforge.kolmafia.webui;

import static net.sourceforge.kolmafia.KoLConstants.HUMAN_READABLE_FORMAT;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.maximizer.Evaluator;
import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CharPaneDecorator {
  private static final Pattern EFFECT_PATTERN =
      Pattern.compile("onClick='eff\\(.*?(\\d+)(?:</a>)?\\)");
  private static final Pattern FONT_TAG_PATTERN =
      Pattern.compile("^((?:<font .*?>)?)(.*?)((?:</font>)?)$", Pattern.DOTALL);

  private static final ArrayList<String> recentLocations = new ArrayList<String>();

  private static final String[][] BIRDFORM_STRINGS = {
    {
      "birdformStench",
      "<span title=\"stinkbug->Statue Treatment->buzzard->+meat\"><font color=green>",
      "/5</font></span>, "
    },
    {
      "birdformSleaze",
      "<span title=\"louse->The Bird->great tit->+ML\"><font color=purple>",
      "/5</font></span>, "
    },
    {
      "birdformCold",
      "<span title=\"ice ant->Antarctic Flap->penguin->regen\"><font color=blue>",
      "/5</font></span>, "
    },
    {
      "birdformHot",
      "<span title=\"fire ant->Rise from Ashes->phoenix->+stats\"><font color=red>",
      "/5</font></span>, "
    },
    {
      "birdformSpooky",
      "<span title=\"beetle->Feast on Carrion->raven->+items\"><font color=gray>",
      "/5</font></span>, "
    },
    {"birdformRoc", "<span title=\"Talon Slash/Wing Buffet->roc->adventures\">", "/15</span>, "},
  };

  private CharPaneDecorator() {}

  public static final void decorate(final StringBuffer buffer) {
    // If you are playing Spelunky or Batfellow, the charpane is
    // entirely different.

    String limitmode = KoLCharacter.getLimitmode();
    if (limitmode != Limitmode.SPELUNKY && limitmode != Limitmode.BATMAN) {
      // We are interested in the following sections of the CharPane:
      //
      // Status (HP/MP)
      // Last Adventure
      // Familiar or Minstrel
      // Effects
      // Intrinsics
      //
      // Two interface settings affect the appearance and/or order of
      // these sections:
      //
      // CharPaneRequest.compactCharacterPane
      // CharPaneRequest.familiarBelowEffects

      // In The Source, we may encounter Agents that don't follow normal turn counter rules, so
      // create a temporary one for decoration
      if (KoLCharacter.inTheSource()) {
        int interval = Preferences.getInteger("sourceInterval");
        if (interval != 0) {
          TurnCounter.startCounting(
              Math.max(interval / 200 - 2, 0), "Source Agent loc=*", "ss_overclocked.gif");
        }
      }

      // Decorate the various sections
      CharPaneDecorator.decorateStatus(buffer);
      CharPaneDecorator.decorateLastAdventure(buffer);
      CharPaneDecorator.decorateFamiliar(buffer);
      CharPaneDecorator.decorateEffects(buffer);
      CharPaneDecorator.decorateIntrinsics(buffer);

      // Destroy temporary counter in The Source
      if (KoLCharacter.inTheSource()) {
        TurnCounter.stopCounting("Source Agent");
      }

      // Update the safety text every time we load the charpane
      StringUtilities.singleStringReplace(
          buffer, "onload='startup();'", "onload='startup();updateSafetyText();'");
    }

    // Add a "refresh" link at the end
    StringUtilities.singleStringReplace(
        buffer,
        "</body>",
        "<center><font size=1>[<a href=\"charpane.php\">refresh</a>]</font></center></body>");
  }

  private static void decorateStatus(final StringBuffer buffer) {
    if (!Preferences.getBoolean("relayAddsRestoreLinks")) {
      return;
    }

    if (buffer.indexOf("Astral Spirit") != -1) { // No restoration needed in Valhalla!
      return;
    }

    // Replace HP information with a restore HP link, if necessary
    // Replace MP information with a restore MP link, if necessary

    float current1 = KoLCharacter.getCurrentHP();
    float maximum1 = KoLCharacter.getMaximumHP();
    // float target1 = Preferences.getFloat( "hpAutoRecoveryTarget" );
    // float threshold1 = maximum1; // * target1
    float dangerous1 = maximum1 * Preferences.getFloat("hpAutoRecovery");

    float current2 = KoLCharacter.getCurrentMP();
    float maximum2 = KoLCharacter.getMaximumMP();
    // target2 = Preferences.getFloat( "mpAutoRecoveryTarget" );
    // threshold2 = maximum2; // * target2
    float dangerous2 = maximum2 * Preferences.getFloat("mpAutoRecovery");

    // replace maximum with threshold if above code is changed
    CharPaneDecorator.addRestoreLinks(
        buffer, "HP", current1, maximum1, dangerous1, "MP", current2, maximum2, dangerous2);
  }

  // Normal:
  //
  //     <td align=center><img src="http://images.kingdomofloathing.com/itemimages/hp.gif"
  // class=hand onclick='doc("hp");' title="Hit Points" alt="Hit Points"><br><span
  // class=black>55&nbsp;/&nbsp;55</span></td>
  //     <td align=center><img src="http://images.kingdomofloathing.com/itemimages/mp.gif"
  // class=hand onclick='doc("mp");' title="Mana Points" alt="Mana Points"><br><span
  // class=black>76&nbsp;/&nbsp;76</span></td>
  //
  // Slim HP:
  //
  //     <td><img src=http://images.kingdomofloathing.com/itemimages/hp.gif title="Hit Points"
  // alt="Hit Points" onclick='doc("hp");' width=20 height=20></td><td valign=center><span
  // class=black>38&nbsp;/&nbsp;58</span>&nbsp;&nbsp;</td>
  //     <td><img src=http://images.kingdomofloathing.com/itemimages/mp.gif title="Mana Points"
  // alt="Mana Points" onclick='doc("mp");' width=20 height=20></td><td valign=center><span
  // class=black>70&nbsp;/&nbsp;122</span></td>
  //
  // Compact:
  //
  //   <tr><td align=right>HP:</td><td align=left><b><font color=black>792/792</font></b></td></tr>
  //   <tr><td align=right>MP:</td><td align=left><b>1398/1628</b></td></tr>

  private static final Pattern POINTS_PATTERN =
      Pattern.compile("(doc\\(['\"](hp|mp).*?<span\\s+class=['\"]?(black|red)['\"]?>)(\\d+)");
  private static final Pattern COMPACT_POINTS_PATTERN =
      Pattern.compile(
          "((HP|MP):</td><td[^>]*><b>(?:<font\\s+color=['\"]?(black|red)['\"]?>)?)(\\d+)");

  private static void addRestoreLinks(
      final StringBuffer buffer,
      final String stat1,
      final float current1,
      final float threshold1,
      final float dangerous1,
      final String stat2,
      final float current2,
      final float threshold2,
      final float dangerous2) {
    // If we don't need restoration, do nothing
    if (current1 >= threshold1 && current2 >= threshold2) {
      return;
    }

    Matcher matcher =
        CharPaneRequest.compactCharacterPane
            ? COMPACT_POINTS_PATTERN.matcher(buffer)
            : POINTS_PATTERN.matcher(buffer);
    while (matcher.find()) {
      String found = matcher.group(2).toUpperCase();
      String stat;
      String color;

      if (found.equals(stat1)) {
        if (current1 >= threshold1) {
          continue;
        }
        stat = stat1;
        color =
            current1 > dangerous1 ? matcher.group(3) == null ? "black" : matcher.group(3) : "red";
      } else if (found.equals(stat2)) {
        if (current2 >= threshold2) {
          continue;
        }
        stat = stat2;
        color =
            current2 > dangerous2 ? matcher.group(3) == null ? "black" : matcher.group(3) : "red";
      } else {
        continue;
      }

      String rep =
          matcher.group(1)
              + "<a style=\"color:"
              + color
              + "\" title=\"Restore your "
              + stat
              + "\" href=\"/KoLmafia/sideCommand?cmd=restore+"
              + stat
              + "&pwd="
              + GenericRequest.passwordHash
              + "\">"
              + matcher.group(4)
              + "</a>";
      StringUtilities.singleStringReplace(buffer, matcher.group(0), rep);
    }
  }

  private static final Pattern LASTADV_PATTERN =
      Pattern.compile(">Last Adventure.*?<font[^>]*>(.*?)<br></font>.*?</table>");
  private static final Pattern COMPACT_LASTADV_PATTERN =
      Pattern.compile(
          "<td align=right>(<a onclick=[^<]+ title=\"Last Adventure: ([^\"]+)\" target=mainpane href=\"([^\"]+)\">.*?</a>:)</td>");

  private static void decorateLastAdventure(final StringBuffer buffer) {
    int nLinks = Preferences.getInteger("recentLocations");

    if (nLinks <= 1) {
      return;
    }

    Pattern pattern =
        CharPaneRequest.compactCharacterPane ? COMPACT_LASTADV_PATTERN : LASTADV_PATTERN;
    Matcher matcher = pattern.matcher(buffer);

    if (!matcher.find()) {
      return;
    }

    // group(1) is the link itself, end() is the insertion point for the recent list

    String link;

    if (CharPaneRequest.compactCharacterPane) {
      link =
          "<a onclick='if (top.mainpane.focus) top.mainpane.focus();' target=mainpane href=\""
              + matcher.group(3)
              + "\">"
              + matcher.group(2)
              + "</a>";
    } else {
      link = matcher.group(1);
    }

    if (CharPaneDecorator.recentLocations.size() == 0) {
      CharPaneDecorator.recentLocations.add(link);
      return;
    }

    if (!CharPaneDecorator.recentLocations.get(0).equals(link)) {
      CharPaneDecorator.recentLocations.remove(link);
      CharPaneDecorator.recentLocations.add(0, link);

      while (CharPaneDecorator.recentLocations.size() > nLinks) {
        CharPaneDecorator.recentLocations.remove(nLinks);
      }
    }

    if (CharPaneDecorator.recentLocations.size() <= 1) {
      return;
    }

    if (CharPaneRequest.compactCharacterPane) {
      StringBuilder linkBuffer = new StringBuilder();

      linkBuffer.append("<td>");

      linkBuffer.append(
          "<span onmouseover=\"document.getElementById('lastadvmenu').style.display = 'inline';\" onmouseout=\"document.getElementById('lastadvmenu').style.display = 'none';\">");

      linkBuffer.append("<div style=\"text-align: right\">");

      linkBuffer.append(matcher.group(1));

      linkBuffer.append("</div>");

      linkBuffer.append("<span id=\"lastadvmenu\"");
      linkBuffer.append(
          " style=\"position: absolute; padding: 5px 5px 5px 5px; background: #f5f5f5; display: none\">");

      linkBuffer.append("<font size=1>");

      for (int i = 0; i < CharPaneDecorator.recentLocations.size(); ++i) {
        if (i > 0) {
          linkBuffer.append("<br/>");
        }

        linkBuffer.append("<nobr>");
        linkBuffer.append(CharPaneDecorator.recentLocations.get(i));
        linkBuffer.append("</nobr>");
      }

      linkBuffer.append("</font>");

      linkBuffer.append("</span>");
      linkBuffer.append("</span>");
      linkBuffer.append("</td>");

      buffer.delete(matcher.start(), matcher.end());
      buffer.insert(matcher.start(), linkBuffer.toString());
    } else {
      StringBuilder linkBuffer = new StringBuilder();

      linkBuffer.append("<font size=1>");

      for (int i = 1; i < CharPaneDecorator.recentLocations.size(); ++i) {
        if (i > 1) {
          linkBuffer.append("<br/>");
        }

        linkBuffer.append("<nobr>");
        linkBuffer.append(CharPaneDecorator.recentLocations.get(i));
        linkBuffer.append("</nobr>");
      }

      linkBuffer.append("</font>");
      buffer.insert(matcher.end(), linkBuffer.toString());
    }
  }

  private static void decorateFamiliar(final StringBuffer buffer) {
    StringBuffer annotations = CharPaneDecorator.getFamiliarAnnotation();
    if (annotations == null || annotations.length() == 0) {
      return;
    }

    if (CharPaneRequest.compactCharacterPane) {
      int pos = buffer.indexOf("<a target=mainpane href=\"familiar.php\"");
      if (pos == -1) return;
      annotations.append("<br>");
      buffer.insert(pos, annotations);
    } else {
      int pos = buffer.indexOf("<b>Familiar:</b>");
      if (pos == -1) return;
      annotations.insert(0, "<br>(");
      annotations.append(")");
      buffer.insert(pos + 16, annotations);
    }
  }

  public static final StringBuffer getFamiliarAnnotation() {
    FamiliarData familiar = KoLCharacter.getEffectiveFamiliar();
    if (familiar == null) {
      return null;
    }

    StringBuffer buffer = new StringBuffer();
    if (familiar.hasFights()) {
      buffer.append(familiar.fightsToday());
      if (familiar.fightDailyCap() != -1) {
        buffer.append("/");
        buffer.append(familiar.fightDailyCap());
        buffer.append(" combats");
      }
    }

    switch (familiar.getId()) {
      case FamiliarPool.BANDER:
        if (!KoLCharacter.inBigcore()) {
          buffer.append(Preferences.getString("_banderRunaways"));
          buffer.append("/");
          buffer.append(familiar.getModifiedWeight() / 5);
        }
        return buffer;

      case FamiliarPool.BOOTS:
        if (!KoLCharacter.inBigcore()) {
          buffer.append(Preferences.getString("_banderRunaways"));
          buffer.append("/");
          buffer.append(familiar.getModifiedWeight() / 5);
          buffer.append(" runs");
          buffer.append("<br>");
        }
        buffer.append(Preferences.getString("_bootStomps"));
        buffer.append("/7 ");
        buffer.append(" stomp");
        if (Preferences.getInteger("_bootStomps") != 1) {
          buffer.append("s");
        }
        if (Preferences.getBoolean("bootsCharged")) {
          buffer.append("!");
        }
        return buffer;

      case FamiliarPool.GIBBERER:
        buffer.append(Preferences.getString("_gibbererAdv"));
        buffer.append(" adv");
        buffer.append("<br>");
        buffer.append(Preferences.getString("_gibbererCharge"));
        buffer.append("/15 charges");
        return buffer;

      case FamiliarPool.HARE:
        buffer.append(Preferences.getString("_hareAdv"));
        buffer.append(" adv");
        buffer.append("<br>");
        buffer.append(Preferences.getString("_hareCharge"));
        buffer.append("/");
        AdventureResult dormouse = ItemPool.get(ItemPool.MINIATURE_DORMOUSE, 1);
        buffer.append(
            EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).equals(dormouse)
                ? "12"
                : "15");
        buffer.append(" charges");
        return buffer;

      case FamiliarPool.SLIMELING:
        {
          buffer.append("~");
          buffer.append(Preferences.getFloat("slimelingFullness"));
          buffer.append(" full");

          int due = Preferences.getInteger("slimelingStacksDue");
          int got = Preferences.getInteger("slimelingStacksDropped");
          if (due > got) {
            // N stacks drop in N * (N + 1)/2 combats according to
            // <http://ben.bloomroad.com/kol/tower_monsters.html>
            // free runaways do not count
            // each stack drops on the turn it's expected to with
            // no variance
            // int expectedTurns = ( got + 1 ) * ( got // + 2 ) / 2;

            buffer.append("; ");
            buffer.append(got);
            buffer.append("/");
            buffer.append(due);
            buffer.append(" stacks");
          }
          return buffer;
        }

      case FamiliarPool.ARTISTIC_GOTH_KID:
        if (KoLCharacter.getHippyStoneBroken()) {
          buffer.append("<br>");
          buffer.append(Preferences.getString("_gothKidCharge"));
          buffer.append("/");
          buffer.append(CharPaneDecorator.gothKidChargesNeeded());
          buffer.append(" charges");
        }
        return buffer;

      case FamiliarPool.GRINDER:
        buffer.append(Preferences.getString("_pieDrops"));
        buffer.append(" pie");
        if (Preferences.getInteger("_pieDrops") != 1) {
          buffer.append("s");
        }
        buffer.append("<br>");
        buffer.append(Preferences.getString("_piePartsCount"));
        buffer.append("/");
        int drops = Preferences.getInteger("_pieDrops");
        int need;
        if (drops < 1) {
          need = 5;
        } else {
          drops -= 1;
          need = 5 + (10 + drops) * (drops + 1) / 2;
          need = Math.min(need, 50);
          if (familiar.getItem().getItemId() == ItemPool.MICROWAVE_STOGIE) {
            need -= 5;
          }
        }
        buffer.append(need);
        buffer.append(" part");
        if (Preferences.getInteger("_piePartsCount") != 1) {
          buffer.append("s");
        }
        return buffer;

      case FamiliarPool.HAPPY_MEDIUM:
        buffer.append(Preferences.getString("_mediumSiphons"));
        buffer.append(" siphon");

        if (Preferences.getInteger("_mediumSiphons") != 1) {
          buffer.append("s");
        }
        return buffer;

      case FamiliarPool.JACK_IN_THE_BOX:
        buffer.append(Preferences.getString("_jitbCharge"));
        buffer.append("/2 charges");
        return buffer;

      case FamiliarPool.ANGRY_JUNG_MAN:
        String jDrops = Preferences.getString("_jungDrops");
        buffer.append(jDrops);
        buffer.append("/1");
        if (jDrops.equals("0")) {
          buffer.append(" next ");
          int charges = Preferences.getInteger("jungCharge");
          if (charges - 30 >= 0) buffer.append("now");
          else buffer.append("@ " + (30 - charges));
        }
        return buffer;

      case FamiliarPool.STEAM_CHEERLEADER:
        double steamLevel = Preferences.getInteger("_cheerleaderSteam") / 2.0;
        buffer.append(steamLevel);
        buffer.append("% steam");
        return buffer;

      case FamiliarPool.NANORHINO:
        int nanorhinoCharge = Preferences.getInteger("_nanorhinoCharge");
        buffer.append(nanorhinoCharge);
        buffer.append("% charge");
        return buffer;

      case FamiliarPool.CUBELING:
        int cubelingProgress = Preferences.getInteger("cubelingProgress");
        int cubelingDrops = Math.min(Math.max(cubelingProgress - 3, 0) / 3, 3);
        buffer.append(cubelingDrops);
        buffer.append("/3 drops");
        return buffer;

      case FamiliarPool.GRIMSTONE_GOLEM:
        String gDrops = Preferences.getString("_grimstoneMaskDrops");
        buffer.append(gDrops);
        buffer.append("/1");
        if (gDrops.equals("0")) {
          buffer.append(" next ");
          int charges = Preferences.getInteger("grimstoneCharge");
          if (charges - 50 >= 0) buffer.append("now");
          else buffer.append("@ " + (50 - charges));
        }
        return buffer;

      case FamiliarPool.CRIMBO_SHRUB:
        if (KoLCharacter.getHippyStoneBroken()
            && Preferences.getString("shrubGarland").equals("PvP")) {
          buffer.append(Preferences.getString("_shrubCharge"));
          buffer.append("/20 charges");
          return buffer;
        }
        return null;

      case FamiliarPool.ROCKIN_ROBIN:
        buffer.append(Preferences.getString("rockinRobinProgress"));
        buffer.append("/30 charges");
        return buffer;
      case FamiliarPool.SPACE_JELLYFISH:
        buffer.append(Preferences.getString("_spaceJellyfishDrops"));
        buffer.append(" jelly harvested");
        return buffer;
      case FamiliarPool.CANDLE:
        buffer.append(Preferences.getString("optimisticCandleProgress"));
        buffer.append("/30 charges");
        return buffer;
      case FamiliarPool.XO_SKELETON:
        buffer.append(Preferences.getString("xoSkeleltonXProgress"));
        buffer.append("/9 X, ");
        buffer.append(Preferences.getString("xoSkeleltonOProgress"));
        buffer.append("/9 O");
        return buffer;
      case FamiliarPool.GARBAGE_FIRE:
        buffer.append(Preferences.getString("garbageFireProgress"));
        buffer.append("/30 charges");
        return buffer;
      case FamiliarPool.CAT_BURGLAR:
        {
          // Calculate total charges available
          int charge = Preferences.getInteger("_catBurglarCharge");
          int minChargeCost = 10;
          int totalHeists = 0;
          while (charge >= minChargeCost) {
            totalHeists++;
            charge -= minChargeCost;
            minChargeCost *= 2;
          }
          int heistsComplete = Preferences.getInteger("_catBurglarHeistsComplete");
          int bankHeists = Preferences.getInteger("catBurglarBankHeists");
          if (totalHeists + bankHeists > heistsComplete) {
            buffer.append(charge);
            buffer.append("/");
            buffer.append(minChargeCost);
            buffer.append(" to heist<br>");
            buffer.append(heistsComplete);
            buffer.append("/");
            buffer.append(totalHeists + bankHeists);
            buffer.append(" heists");
          } else {
            buffer.append(charge);
            buffer.append("/");
            buffer.append(minChargeCost);
            buffer.append(" to heist");
          }
          return buffer;
        }
      case FamiliarPool.POCKET_PROFESSOR:
        {
          buffer.append(Preferences.getString("_pocketProfessorLectures"));
          buffer.append("/");
          int lectures = (int) Math.ceil(Math.sqrt(familiar.getModifiedWeight()));
          AdventureResult chip = ItemPool.get(ItemPool.POCKET_PROFESSOR_MEMORY_CHIP, 1);
          if (EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).equals(chip)) {
            lectures += 2;
          }
          buffer.append(lectures);
          buffer.append(" lectures");
          return buffer;
        }
      case FamiliarPool.RED_SNAPPER:
        {
          String phylum = Preferences.getString("redSnapperPhylum");
          int progress = Preferences.getInteger("redSnapperProgress");
          buffer.append(progress);
          buffer.append("/11 ");
          buffer.append(phylum.equals("") ? "(none)" : phylum);
          return buffer;
        }

      case FamiliarPool.MELODRAMEDARY:
        {
          int spit = Preferences.getInteger("camelSpit");
          buffer.append(spit).append("% charged");

          if (spit < 100) {
            double spitPerTurn = 10 / 3.0;
            AdventureResult helmet = ItemPool.get(ItemPool.DROMEDARY_DRINKING_HELMENT, 1);
            boolean wearingHelmet =
                EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).equals(helmet);

            if (wearingHelmet) {
              spitPerTurn += 1;
            }

            double turnsRemaining = Math.max((100 - spit) / spitPerTurn, 1.0);
            boolean estimate = wearingHelmet && turnsRemaining > 1;

            buffer
                .append("<br>(")
                .append(estimate ? "~" : "")
                .append(HUMAN_READABLE_FORMAT.format(turnsRemaining))
                .append(" combat")
                .append(turnsRemaining > 1 ? "s" : "")
                .append(")");
          }

          return buffer;
        }
    }

    if (familiar.hasDrop()) {
      if (buffer.length() != 0) {
        buffer.append("<br>");
      }

      buffer.append(familiar.dropsToday());
      if (familiar.dropDailyCap() != -1) {
        buffer.append("/");
        buffer.append(familiar.dropDailyCap());
        buffer.append(" ");
        buffer.append(familiar.dropName());
      }
      return buffer;
    }

    return buffer.length() > 0 ? buffer : null;
  }

  private static int gothKidChargesNeeded() {
    int fights = Preferences.getInteger("_gothKidFights");
    AdventureResult mannequin = ItemPool.get(ItemPool.LITTLE_MANNEQUIN, 1);
    if (EquipmentManager.getEquipment(EquipmentManager.FAMILIAR).equals(mannequin)) {
      return (int) (Math.pow(fights, 2) - fights + 10) / 2;
    } else {
      int[] goth = {7, 7, 8, 11, 15, 20, 27, 35, 44, 55, 67, 80, 95, 111, 128};
      if (fights > 14) {
        fights = 14;
      }
      return goth[fights];
    }
  }

  protected static StringBuffer decorateEffects(final StringBuffer buffer) {
    String effectText = CharPaneDecorator.getEffectText(buffer);
    String moodText = CharPaneDecorator.getMoodText();
    int counters = TurnCounter.count();

    // If there are no effects on the charpane, no active mood, and
    // no active counters, nothing to do.
    if (effectText == null && moodText == null && counters == 0) {
      return buffer;
    }

    // Otherwise, make a buffer to manipulate effect text in
    StringBuffer effects =
        new StringBuffer(effectText != null ? effectText : CharPaneDecorator.dummyEffectTable());

    if (moodText != null) {
      CharPaneDecorator.addMoodText(effects, moodText);
    }

    // Add links to effects
    if (effectText != null && Preferences.getBoolean("relayAddsUpArrowLinks")) {
      CharPaneDecorator.addUpArrowLinks(effects);
    }

    // Interpolate counters with effects
    if (counters > 0) {
      CharPaneDecorator.addCounters(effects);
    }

    // Colorize certain effects
    if (effectText != null) {
      StringUtilities.singleStringReplace(
          effects,
          "<font size=2>Everything Looks Yellow",
          "<font size=2 color=olive>Everything Looks Yellow");
      StringUtilities.singleStringReplace(
          effects,
          "<font size=2>Everything Looks Red",
          "<font size=2 color=red>Everything Looks Red");
      StringUtilities.singleStringReplace(
          effects,
          "<font size=2>Everything Looks Blue",
          "<font size=2 color=blue>Everything Looks Blue");
    }

    if (effectText != null) {
      // Replace existing effects table with what we generated
      StringUtilities.singleStringReplace(buffer, effectText, effects.toString());
    } else {
      // Figure out where to insert it
      int index = CharPaneDecorator.chooseEffectTableIndex(buffer);
      buffer.insert(index, effects.toString());
    }

    return buffer;
  }

  private static int getIntrinsicIndex(final StringBuffer buffer) {
    if (CharPaneRequest.compactCharacterPane) {
      int index = buffer.indexOf("&infin;");
      return index < 0 ? -1 : buffer.lastIndexOf("<hr width=50%>", index);
    }

    return buffer.indexOf("<center><b><font size=2>Intrinsics:");
  }

  private static int getFamiliarIndex(final StringBuffer buffer) {
    int index =
        KoLCharacter.inAxecore()
            ? buffer.indexOf("otherimages/clancy")
            : buffer.indexOf("familiar.php");

    if (index < 0) {
      return buffer.lastIndexOf("</center>");
    }

    if (CharPaneRequest.compactCharacterPane) {
      return buffer.lastIndexOf("<hr width=50%>", index);
    }

    return buffer.lastIndexOf(KoLCharacter.inAxecore() ? "<center" : "<table", index);
  }

  private static String getEffectText(final StringBuffer buffer) {
    int startIndex;

    if (CharPaneRequest.compactCharacterPane) {
      int effectIndex = buffer.indexOf("eff(");
      if (effectIndex == -1) {
        return null;
      }

      int intrinsics = CharPaneDecorator.getIntrinsicIndex(buffer);
      if (intrinsics != -1 && effectIndex > intrinsics) {
        return null;
      }

      startIndex = buffer.lastIndexOf("<hr width=50%>", effectIndex);
    } else {
      int effectIndex = buffer.indexOf("<center><p><b><font size=2>Effects:</font></b>");
      if (effectIndex == -1) {
        return null;
      }

      startIndex = effectIndex;
    }

    int endIndex = buffer.indexOf("</table>", startIndex);
    if (endIndex == -1) {
      return null;
    }
    endIndex += 8;

    return buffer.substring(startIndex, endIndex);
  }

  private static String getIntrinsicsText(final StringBuffer buffer) {
    int startIndex = CharPaneDecorator.getIntrinsicIndex(buffer);
    if (startIndex == -1) {
      return null;
    }

    int endIndex = buffer.indexOf("</table>", startIndex);
    if (endIndex == -1) {
      return null;
    }
    endIndex += 8;

    return buffer.substring(startIndex, endIndex);
  }

  private static int chooseEffectTableIndex(final StringBuffer buffer) {
    // There are no effects currently on the charpane. Choose where
    // to place the table we constructed.

    // If there are Intrinsics, place effects before them
    int intrinsics = CharPaneDecorator.getIntrinsicIndex(buffer);
    if (intrinsics != -1) {
      return intrinsics;
    }

    // If familiar goes below effects, place above familiar
    if (CharPaneRequest.familiarBelowEffects) {
      int familiar = CharPaneDecorator.getFamiliarIndex(buffer);
      if (familiar != -1) {
        return familiar;
      }
    }

    // Otherwise, place at the end.
    return buffer.lastIndexOf("</center>");
  }

  private static String getMoodText() {
    String fontColor = null;
    String moodText = null;

    if (MoodManager.willExecute(0)) {
      fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
      moodText = "mood " + Preferences.getString("currentMood");
    } else if (ManaBurnManager.getNextBurnCast() != null) {
      fontColor = FightRequest.getCurrentRound() == 0 ? "black" : "gray";
      moodText = "burn extra mp";
    } else if (!MoodManager.getTriggers().isEmpty()) {
      fontColor = "gray";
      moodText = "mood " + Preferences.getString("currentMood");
    } else {
      for (int i = 0; i < KoLConstants.activeEffects.size(); ++i) {
        AdventureResult currentEffect = KoLConstants.activeEffects.get(i);
        if (currentEffect.getCount() != Integer.MAX_VALUE
            && !MoodManager.getDefaultAction("lose_effect", currentEffect.getName()).equals("")) {
          fontColor = "black";
          moodText = "save as mood";
          break;
        }
      }
    }

    if (moodText == null) {
      return null;
    }

    StringBuilder buffer = new StringBuilder();

    buffer.append("<font size=2 color=");
    buffer.append(fontColor);

    buffer.append(">[<a title=\"I'm feeling moody\" href=\"/KoLmafia/sideCommand?cmd=");

    if (moodText.startsWith("mood")) {
      buffer.append("mood+execute");
    } else {
      buffer.append(StringUtilities.getURLEncode(moodText));
    }

    buffer.append("&pwd=");
    buffer.append(GenericRequest.passwordHash);
    buffer.append("\" style=\"color:");
    buffer.append(fontColor);
    buffer.append("\">");

    buffer.append(moodText);
    buffer.append("</a>]</font>");

    return buffer.toString();
  }

  private static String dummyEffectTable() {
    StringBuilder effects = new StringBuilder();

    if (CharPaneRequest.compactCharacterPane) {
      effects.append("<hr width=50%>");
    } else {
      effects.append("<center><p><b><font size=2>Effects:</font></b>");
    }
    effects.append("<table></table>");

    return effects.toString();
  }

  private static void addMoodText(final StringBuffer buffer, final String moodText) {
    if (CharPaneRequest.compactCharacterPane) {
      String test = "<hr width=50%>";
      int index = buffer.indexOf(test) + test.length();
      buffer.insert(index, moodText);
      index += moodText.length();
      buffer.insert(index, "<br><br>");
    } else {
      String test = "</b>";
      int index = buffer.indexOf(test) + test.length();
      buffer.insert(index, "<br>");
      index += 4;
      buffer.insert(index, moodText);
      index += moodText.length();
    }
  }

  private static void addUpArrowLinks(final StringBuffer buffer) {
    String text = buffer.toString();
    buffer.setLength(0);

    int startingIndex = 0;
    int lastAppendIndex = 0;

    // Insert any effects which are in your maintenance list which
    // have already run out.

    List<AdventureResult> missingEffects = MoodManager.getMissingEffects();

    // If the player has at least one effect, then go ahead and add
    // all of their missing effects.

    startingIndex = text.indexOf("<tr>", lastAppendIndex);
    if (startingIndex != -1 && !missingEffects.isEmpty() && !KoLConstants.activeEffects.isEmpty()) {
      buffer.append(text, lastAppendIndex, startingIndex);
      lastAppendIndex = startingIndex;

      AdventureResult currentEffect;

      for (int i = 0; i < missingEffects.size(); ++i) {
        currentEffect = missingEffects.get(i);

        String effectName = currentEffect.getName();
        int effectId = currentEffect.getEffectId();
        String escapedEffectName = StringUtilities.getEntityEncode(effectName);

        if (Evaluator.checkEffectConstraints(effectId)) {
          // Don't include effects that you cannot cast
          continue;
        }

        String descriptionId = EffectDatabase.getDescriptionId(effectId);

        buffer.append("<tr>");

        if (!CharPaneRequest.compactCharacterPane
            || !Preferences.getBoolean("relayTextualizesEffects")) {
          // The extra <td></td> fixes alignment with KoL up-arrows enabled
          // and doesn't break anything with them disabled
          buffer.append("<td></td><td><img src=\"");
          buffer.append(EffectDatabase.getImage(effectId));
          buffer.append("\" class=hand alt=\"");
          buffer.append(escapedEffectName);
          buffer.append("\" title=\"");
          buffer.append(escapedEffectName);
          buffer.append("\" onClick='eff(\"");
          buffer.append(descriptionId);
          buffer.append("\");'></td>");
        }

        if (!CharPaneRequest.compactCharacterPane
            || Preferences.getBoolean("relayTextualizesEffects")) {
          buffer.append("<td><font size=2>");
          buffer.append(escapedEffectName);
        } else {
          buffer.append("<td><font size=2>");
        }

        buffer.append(" (0)</font>&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=");

        buffer.append(
            StringUtilities.getURLEncode(MoodManager.getDefaultAction("lose_effect", effectName)));

        buffer.append("&pwd=");
        buffer.append(GenericRequest.passwordHash);
        buffer.append("\" title=\"Increase rounds of ");
        buffer.append(escapedEffectName);
        buffer.append("\"><img src=\"/images/redup.gif\" border=0></a></td></tr>");
      }
    }

    // Replace all of the shrug off links associated with this response text.
    CharPaneDecorator.addEffectLinks(buffer, text, startingIndex, lastAppendIndex);
  }

  private static void addShrugIntrinsicLinks(final StringBuffer buffer) {
    String text = buffer.toString();
    buffer.setLength(0);

    // Replace all of the shrug off links associated with this response text.
    CharPaneDecorator.addEffectLinks(buffer, text, text.indexOf("<tr>"), 0);
  }

  private static void addEffectLinks(
      final StringBuffer buffer, final String text, int startingIndex, int lastAppendIndex) {
    while (startingIndex != -1) {
      startingIndex = text.indexOf("onClick='eff", lastAppendIndex + 1);
      if (startingIndex == -1) {
        break;
      }

      startingIndex = text.lastIndexOf("<", startingIndex);
      AdventureResult effect = CharPaneRequest.extractEffect(text, startingIndex);

      if (effect == null) {
        int nextAppendIndex = text.indexOf(">", startingIndex) + 1;
        buffer.append(text, lastAppendIndex, nextAppendIndex);
        lastAppendIndex = nextAppendIndex;
        continue;
      }

      String effectName = effect.getName();
      String escapedEffectName = StringUtilities.getEntityEncode(effectName);

      int nextAppendIndex = text.indexOf("(", startingIndex) + 1;
      buffer.append(text, lastAppendIndex, nextAppendIndex);
      lastAppendIndex = nextAppendIndex;

      if (CharPaneRequest.compactCharacterPane) {
        if (Preferences.getBoolean("relayTextualizesEffects")) {
          nextAppendIndex = text.indexOf("></td>", startingIndex);
          buffer.append(text, lastAppendIndex, nextAppendIndex);
          lastAppendIndex = nextAppendIndex + 6;

          int deleteIndex = buffer.lastIndexOf("<img");
          buffer.delete(deleteIndex, buffer.length());

          buffer.append("<td align=right><nobr><font size=2>");
          buffer.append(escapedEffectName);
          buffer.append("</font></nobr></td>");
        }

        nextAppendIndex = text.indexOf("<td>(", startingIndex) + 4;
      } else {
        nextAppendIndex = text.lastIndexOf("(", text.indexOf("</font", startingIndex));
      }

      buffer.append(text, lastAppendIndex, nextAppendIndex);
      buffer.append("<span style='white-space:nowrap;'>(");
      lastAppendIndex = nextAppendIndex + 1;

      String upkeepAction = MoodManager.getDefaultAction("lose_effect", effectName);

      if (upkeepAction.startsWith("adventure") || upkeepAction.endsWith("astral mushroom")) {
        upkeepAction = "";
      }

      String imageAction = null;

      if (upkeepAction.endsWith("absinthe")) {
        imageAction = "Go to Worm Wood";
        upkeepAction = "place.php?whichplace=wormwood";
      }

      boolean isShruggable = UneffectRequest.isShruggable(effectName);
      boolean isRemovable = UneffectRequest.isRemovable(effectName);
      boolean needsCocoa = UneffectRequest.needsCocoa(effectName);
      boolean isTimer = effectName.startsWith("Timer ");
      boolean isCowrruption = effectName.equals("Cowrruption");
      boolean isCowpuncher = KoLCharacter.getAscensionClass() == AscensionClass.COWPUNCHER;
      int duration = effect.getCount();
      boolean isIntrinsic = duration == Integer.MAX_VALUE;

      // Add a removal link to the duration for buffs which
      // can be removed.  This is either when the buff can be
      // shrugged or the buff has a default removal method.

      String removeAction =
          needsCocoa
              ? "use 1 hot Dreadsylvanian cocoa"
              : (isIntrinsic || (isCowrruption && isCowpuncher))
                  ? UneffectRequest.getUneffectSkill(effectName)
                  : !isRemovable ? "" : MoodManager.getDefaultAction("gain_effect", effectName);

      if (effectName.equalsIgnoreCase("On the Trail")) {
        String monsterName = Preferences.getString("olfactedMonster");
        monsterName = StringUtilities.getEntityEncode(monsterName);
        monsterName = StringUtilities.singleStringReplace(monsterName, "&lt;i&gt;", "<i>");
        monsterName = StringUtilities.singleStringReplace(monsterName, "&lt;/i&gt;", "</i>");
        buffer.append(monsterName);
        buffer.append(", ");
      } else if (effectName.equalsIgnoreCase("Shape of...Mole!")) {
        int level = Preferences.getInteger("moleTunnelLevel");
        buffer.append(level >= 0 ? "+" : "");
        buffer.append(level);
        buffer.append(", ");
      } else if (effectName.equalsIgnoreCase("Form of...Bird!")) {
        for (int i = 0; i < BIRDFORM_STRINGS.length; ++i) {
          int count = Preferences.getInteger(BIRDFORM_STRINGS[i][0]);
          if (count != 0) {
            buffer.append(BIRDFORM_STRINGS[i][1]);
            buffer.append(count);
            buffer.append(BIRDFORM_STRINGS[i][2]);
          }
        }
      }

      if (!removeAction.equals("")) {
        buffer.append("<a href=\"/KoLmafia/sideCommand?cmd=");
        buffer.append(StringUtilities.getURLEncode(removeAction));
        buffer.append("&pwd=");
        buffer.append(GenericRequest.passwordHash);
        buffer.append("\" title=\"");

        if (isTimer) {
          buffer.append("Cancel ");
        } else if (isShruggable) {
          buffer.append("Shrug off the ");
        } else if (removeAction.startsWith("uneffect")) {
          buffer.append("Use a remedy to remove the ");
        } else {
          buffer.append(Character.toUpperCase(removeAction.charAt(0)));
          buffer.append(removeAction.substring(1));
          buffer.append(" to remove the ");
        }

        buffer.append(escapedEffectName);

        if (!isTimer) {
          buffer.append(" effect");
        }

        buffer.append("\"");

        if (effectName.indexOf("Poisoned") != -1 || effectName.equals("Beaten Up")) {
          buffer.append(" style=\"color:red\"");
        }

        buffer.append(">");
      }

      nextAppendIndex = text.indexOf(")", lastAppendIndex);
      buffer.append(text, lastAppendIndex, nextAppendIndex);
      lastAppendIndex = nextAppendIndex;

      if (isShruggable || !removeAction.equals("")) {
        buffer.append("</a>");
      }

      buffer.append(")");
      lastAppendIndex++;

      if (isIntrinsic) {
        buffer.append("</span>");
        continue;
      }

      // Add the up-arrow icon for buffs which can be maintained, based
      // on information known to the mood maintenance module.

      if (imageAction != null) {
        buffer.append("&nbsp;<a href=\"");
        buffer.append(upkeepAction);
        buffer.append("\" target=\"mainpane\" title=\"");
        buffer.append(imageAction);
        buffer.append("\"><img src=\"/images/browser.gif\" width=14 height=14 border=0></a>");
      } else if (!upkeepAction.equals("")
          && MoodManager.currentlyExecutable(effect, upkeepAction)) {
        buffer.append("&nbsp;<a href=\"/KoLmafia/sideCommand?cmd=");
        buffer.append(StringUtilities.getURLEncode(upkeepAction));
        buffer.append("&pwd=");
        buffer.append(GenericRequest.passwordHash);
        buffer.append("\" title=\"Increase rounds of ");
        buffer.append(escapedEffectName);
        buffer.append("\"><img src=\"/images/");

        if (duration <= 5) {
          buffer.append("red");
        }

        buffer.append("up.gif\" border=0></a>");
      }

      buffer.append("</span>");
    }

    buffer.append(text.substring(lastAppendIndex));
  }

  private static void addCounters(final StringBuffer buffer) {
    TurnCounter[] counters = TurnCounter.getCounters();
    if (counters.length == 0) {
      return;
    }

    int index = 0;
    TurnCounter current = counters[index++];
    while (current.getTurnsRemaining() < 0) {
      // Skip expired informational counters that are still
      // pending delivery to a counterScript.
      if (index == counters.length) return;
      current = counters[index++];
    }
    String text = buffer.toString();
    buffer.setLength(0);
    int lastPos = 0;
    int insPos;
    boolean compact = CharPaneRequest.compactCharacterPane;
    Matcher m = CharPaneDecorator.EFFECT_PATTERN.matcher(text);
    while (m.find()) {
      int duration = StringUtilities.parseInt(m.group(1));
      if (duration >= current.getTurnsRemaining()) {
        insPos = text.lastIndexOf("<tr>", m.start(0));
        buffer.append(text, lastPos, insPos);
        lastPos = insPos;
        do {
          CharPaneDecorator.addOneCounter(buffer, current, compact);
          if (index == counters.length) {
            buffer.append(text.substring(lastPos));
            return;
          }
          current = counters[index++];
        } while (duration >= current.getTurnsRemaining());
      }
    }

    // If we've gotten this far, there are counters that are higher
    // than any effect duration.  Insert them at the very end, but
    // before any intrinsics.

    insPos = text.lastIndexOf("&infin;");
    if (insPos != -1) {
      insPos = text.lastIndexOf("</table>", insPos);
    } else {
      insPos = text.lastIndexOf("</table>");
    }
    if (insPos == -1) { // something is very wrong
      insPos = text.length();
    }
    buffer.append(text, lastPos, insPos);
    lastPos = insPos;
    while (true) {
      CharPaneDecorator.addOneCounter(buffer, current, compact);
      if (index == counters.length) {
        buffer.append(text.substring(lastPos));
        return;
      }
      current = counters[index++];
    }
  }

  private static void addOneCounter(StringBuffer buffer, TurnCounter current, boolean compact) {
    String url = current.imageURL();

    if (CharPaneRequest.compactCharacterPane) {
      Matcher m = CharPaneDecorator.FONT_TAG_PATTERN.matcher(current.getLabel());
      m.find(); // this cannot fail, group 2 matches anything
      // Extra <td></td> fixes layout when KoL buff arrows are enabled and doesn't
      // break anything when they are disabled
      buffer.append("<tr><td></td><td>");
      if (url != null) {
        buffer.append("<a href=\"");
        buffer.append(url);
        buffer.append("\" target=\"mainpane\">");
      }
      buffer.append("<img src=\"");
      buffer.append(KoLmafia.imageServerPath());
      buffer.append("itemimages/");
      buffer.append(current.getImage());
      buffer.append("\" title=\"");
      buffer.append(m.group(2));
      buffer.append("\">");
      if (url != null) {
        buffer.append("</a>");
      }
      buffer.append("</td><td>");
      buffer.append(m.group(1));
      buffer.append("(<a href=\"/KoLmafia/sideCommand?cmd=counters+deletehash+");
      buffer.append(System.identityHashCode(current));
      buffer.append("&pwd=");
      buffer.append(GenericRequest.passwordHash);
      buffer.append("\">");
      buffer.append(current.getTurnsRemaining());
      buffer.append("</a>)");
      buffer.append(m.group(3));
      buffer.append("</td></tr>");
    } else { // !compact
      // Extra <td></td> fixes layout when KoL buff arrows are enabled and doesn't
      // break anything when they are disabled
      buffer.append("<tr><td></td><td>");
      if (url != null) {
        buffer.append("<a href=\"");
        buffer.append(url);
        buffer.append("\" target=\"mainpane\">");
      }
      buffer.append("<img src=\"");
      buffer.append(KoLmafia.imageServerPath());
      buffer.append("itemimages/");
      buffer.append(current.getImage());
      buffer.append("\">");
      if (url != null) {
        buffer.append("</a>");
      }
      buffer.append("</td><td valign=center><font size=2>");
      buffer.append(current.getLabel());
      buffer.append(" (<a href=\"/KoLmafia/sideCommand?cmd=counters+deletehash+");
      buffer.append(System.identityHashCode(current));
      buffer.append("&pwd=");
      buffer.append(GenericRequest.passwordHash);
      buffer.append("\">");
      buffer.append(current.getTurnsRemaining());
      buffer.append("</a>)</td></tr>");
    }
  }

  protected static StringBuffer decorateIntrinsics(final StringBuffer buffer) {
    String intrinsicsText = CharPaneDecorator.getIntrinsicsText(buffer);

    // If there are no intrinsics on the charpane, nothing to do.
    if (intrinsicsText == null || !Preferences.getBoolean("relayAddsUpArrowLinks")) {
      return buffer;
    }

    // Otherwise, make a buffer to manipulate intrinsic text in
    StringBuffer intrinsics = new StringBuffer(intrinsicsText);

    // Add links to intrinsics
    CharPaneDecorator.addShrugIntrinsicLinks(intrinsics);

    // Replace existing effects table with what we generated
    StringUtilities.singleStringReplace(buffer, intrinsicsText, intrinsics.toString());

    return buffer;
  }

  public static final void updateFromPreferences() {
    CharPaneDecorator.recentLocations.clear();
  }
}
