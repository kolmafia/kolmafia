package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class KGBRequest extends GenericRequest {
  public KGBRequest() {
    super("place.php");
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    if (action == null) {
      return;
    }
    KGBRequest.countClicks(responseText);

    if (action.startsWith("kgb_button")) {
      KGBRequest.updateEnchantments(responseText);
    } else if (action.equals("kgb_dispenser")) {
      if (responseText.contains("You acquire an item")) {
        Preferences.increment("_kgbDispenserUses");
      } else if (responseText.contains("out of juice")) {
        Preferences.setInteger("_kgbDispenserUses", 3);
      }
    } else if (action.equals("kgb_drawer1")) {
      Preferences.setBoolean("_kgbRightDrawerUsed", true);
    } else if (action.equals("kgb_drawer2")) {
      Preferences.setBoolean("_kgbLeftDrawerUsed", true);
    } else if (action.equals("kgb_daily")) {
      Preferences.setBoolean("_kgbOpened", true);
    } else if (action.startsWith("kgb_handle")) {
      if (responseText.contains("The case emanates warmth.")) {
        Preferences.setBoolean("_kgbFlywheelCharged", true);
      }
    }
  }

  public static final void countClicks(String responseText) {
    int startIndex = responseText.indexOf("<br>Click");
    if (startIndex == -1) {
      return;
    }
    startIndex += 4;
    int endIndex = responseText.indexOf("<br>", startIndex);
    String text = responseText.substring(startIndex, endIndex).toLowerCase();
    int index = text.indexOf("click");
    int count = 0;
    while (index != -1) {
      count++;
      index = text.indexOf("click", index + 5);
    }
    Preferences.increment("_kgbClicksUsed", count);
  }

  // <s>Monsters will be less attracted to you</s><br><br><b>+5 PvP Fights per day</b>
  private static final Pattern ENCHANT_PATTERN =
      Pattern.compile("<s>(.*?)</s><br><br><b>(.*?)</b>");

  private static final HashMap<String, ModifierList> modMap = new HashMap<String, ModifierList>();

  static {
    ModifierList list1 = new ModifierList();
    list1.addModifier("Weapon Damage Percent", "25");
    modMap.put("Weapon Damage +25%", list1);

    ModifierList list2 = new ModifierList();
    list2.addModifier("Spell Damage Percent", "50");
    modMap.put("Spell Damage +50%", list2);

    ModifierList list3 = new ModifierList();
    list3.addModifier("Hot Damage", "5");
    list3.addModifier("Cold Damage", "5");
    list3.addModifier("Spooky Damage", "5");
    list3.addModifier("Stench Damage", "5");
    list3.addModifier("Sleaze Damage", "5");
    modMap.put("+5 Prismatic Damage", list3);

    ModifierList list4 = new ModifierList();
    list4.addModifier("Critical Hit Percent", "10");
    modMap.put("+10% chance of Critical Hit", list4);

    ModifierList list5 = new ModifierList();
    list5.addModifier("PvP Fights", "5");
    modMap.put("+5 PvP Fights per day", list5);

    ModifierList list6 = new ModifierList();
    list6.addModifier("Combat Rate", "-5");
    modMap.put("Monsters will be less attracted to you", list6);

    ModifierList list7 = new ModifierList();
    list7.addModifier("Combat Rate", "5");
    modMap.put("Monsters will be more attracted to you", list7);

    ModifierList list8 = new ModifierList();
    list8.addModifier("Monster Level", "25");
    modMap.put("+25 to Monster Level", list8);

    ModifierList list9 = new ModifierList();
    list9.addModifier("Mana Cost", "-3");
    modMap.put("-3 MP to use Skills", list9);

    ModifierList list10 = new ModifierList();
    list10.addModifier("HP Regen Min", "5");
    list10.addModifier("HP Regen Max", "10");
    list10.addModifier("MP Regen Min", "5");
    list10.addModifier("MP Regen Max", "10");
    modMap.put("Regenerate 5-10 HP & MP per Adventure", list10);

    ModifierList list11 = new ModifierList();
    list11.addModifier("Adventures", "5");
    modMap.put("+5 Adventures per day", list11);

    ModifierList list12 = new ModifierList();
    list12.addModifier("Initiative", "25");
    modMap.put("+25% Combat Initiative", list12);

    ModifierList list13 = new ModifierList();
    list13.addModifier("Damage Absorption", "100");
    modMap.put("Damage Absorption +100", list13);

    ModifierList list14 = new ModifierList();
    list14.addModifier("Hot Resistance", "5");
    modMap.put("Superhuman Hot Resistance (+5)", list14);

    ModifierList list15 = new ModifierList();
    list15.addModifier("Cold Resistance", "5");
    modMap.put("Superhuman Cold Resistance (+5)", list15);

    ModifierList list16 = new ModifierList();
    list16.addModifier("Spooky Resistance", "5");
    modMap.put("Superhuman Spooky Resistance (+5)", list16);

    ModifierList list17 = new ModifierList();
    list17.addModifier("Stench Resistance", "5");
    modMap.put("Superhuman Stench Resistance (+5)", list17);

    ModifierList list18 = new ModifierList();
    list18.addModifier("Sleaze Resistance", "5");
    modMap.put("Superhuman Sleaze Resistance (+5)", list18);
  }

  private static void updateEnchantments(final String responseText) {
    // A symphony of mechanical buzzing and whirring ensues, and your case seems to be... different
    // somehow.
    if (!responseText.contains("symphony of mechanical")) {
      return;
    }
    Matcher matcher = KGBRequest.ENCHANT_PATTERN.matcher(responseText);
    if (matcher.find()) {
      String oldEnchantment = matcher.group(1);
      String newEnchantment = matcher.group(2);
      ModifierList modList = Modifiers.getModifierList("Item", ItemPool.KREMLIN_BRIEFCASE);
      ModifierList oldModList = modMap.get(oldEnchantment);
      for (Modifier modifier : oldModList) {
        modList.removeModifier(modifier.getName());
      }
      ModifierList newModList = modMap.get(newEnchantment);
      for (Modifier modifier : newModList) {
        modList.addModifier(modifier);
      }
      Modifiers.overrideModifier("Item:[" + ItemPool.KREMLIN_BRIEFCASE + "]", modList.toString());
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
    }
  }
}
