package net.sourceforge.kolmafia.utilities;

import java.util.Map.Entry;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.webui.RelayLoader;

@SuppressWarnings("incomplete-switch")
public class WikiUtilities {

  public enum WikiType {
    ANY,
    ITEM,
    EFFECT,
    SKILL,
    MONSTER
  }

  private WikiUtilities() {}

  public static String getWikiLocation(String name, WikiType type, boolean dataPage) {
    boolean checkOtherTables = true;

    if (type != WikiType.ANY) {
      ModifierType modType =
          switch (type) {
            case ITEM -> ModifierType.ITEM;
            case EFFECT -> ModifierType.EFFECT;
            case SKILL -> ModifierType.SKILL;
            default -> ModifierType.NONE;
          };

      Modifiers mods = ModifierDatabase.getModifiers(modType, name);
      if (mods != null) {
        String wikiname = mods.getString(StringModifier.WIKI_NAME);
        if (wikiname != null && wikiname.length() > 0) {
          name = wikiname;
          checkOtherTables = false;
        }
      }
    }

    if (checkOtherTables) {
      name = disambiguateTypes(type, name);
    }

    name = StringUtilities.globalStringReplace(name, "#", "");
    name = StringUtilities.globalStringReplace(name, "<i>", "");
    name = StringUtilities.globalStringReplace(name, "</i>", "");
    name = StringUtilities.globalStringReplace(name, "<s>", "");
    name = StringUtilities.globalStringReplace(name, "</s>", "");
    name = StringUtilities.globalStringReplace(name, " ", "_");

    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

    // Turn character entities into characters
    name = CharacterEntities.unescape(name);

    name = StringUtilities.getURLEncode(name);
    name = StringUtilities.globalStringReplace(name, "%2F", "/");

    if (dataPage) {
      name = "Data:" + name;
    }

    return "https://wiki.kingdomofloathing.com/" + name;
  }

  private static String disambiguateTypes(WikiType type, String name) {
    boolean inItemTable = ItemDatabase.containsExactly(name);
    boolean inEffectTable = EffectDatabase.containsExactly(name);
    boolean inSkillTable = SkillDatabase.contains(name);
    boolean inMonsterTable = MonsterDatabase.contains(name);
    switch (type) {
      case ITEM -> {
        if (inEffectTable || inSkillTable || inMonsterTable) {
          return name + " (item)";
        }
        return name;
      }
      case EFFECT -> {
        if (name.equals("Souped Up")) {
          // also an adventure
          return name + " (effect)";
        }
        if (inItemTable || inSkillTable || inMonsterTable) {
          return name + " (effect)";
        }
        return name;
      }
      case SKILL -> {
        if (inItemTable || inEffectTable || inMonsterTable) {
          return name + " (skill)";
        }
        return name;
      }
      case MONSTER -> {
        switch (name) {
          case "ice porter":
          case "licorice snake":
          case "Porkpocket":
          case "Tin of Submardines":
            // Also an item.
            return name;
          case "Frosty":
            // Also an effect.
            return name;
          case "rage flame":
            // Also a skill.
            return name;
          case "undead elbow macaroni":
            // Also (formerly) a pasta guardian
            return name + " (monster)";
        }
        if (inItemTable || inEffectTable || inSkillTable) {
          return name + " (monster)";
        }
        return name;
      }
    }
    return name;
  }

  public static String getWikiLocation(Object item) {
    return getWikiLocation(item, false);
  }

  public static String getWikiLocation(Object item, boolean dataPage) {
    if (item == null) {
      return null;
    }

    String name = null;
    WikiType type = WikiType.ANY;

    if (item instanceof Boost b) {
      item = b.getItem();
    } else if (item instanceof Entry<?, ?> e) {
      item = e.getValue();
    }

    if (item instanceof MonsterData md) {
      name = md.getWikiName();
      type = WikiType.MONSTER;
    } else if (item instanceof AdventureResult result) {
      name = result.getDataName();

      type =
          result.isItem()
              ? WikiType.ITEM
              : result.isStatusEffect() ? WikiType.EFFECT : WikiType.ANY;
    } else if (item instanceof ShopRow sr) {
      name = sr.getItem().getDataName();
      type = WikiType.ITEM;
    } else if (item instanceof UseSkillRequest usr) {
      name = usr.getSkillName();
      type = WikiType.SKILL;
    } else if (item instanceof Concoction c) {
      name = c.getName();
      type = WikiType.ITEM;
    } else if (item instanceof QueuedConcoction qc) {
      name = qc.getName();
      type = WikiType.ITEM;
    } else if (item instanceof CreateItemRequest cir) {
      name = cir.getName();
      type = WikiType.ITEM;
    } else if (item instanceof PurchaseRequest pr) {
      name = pr.getItem().getDataName();
      type = WikiType.ITEM;
    } else if (item instanceof SoldItem si) {
      name = si.getItemName();
      type = WikiType.ITEM;
    } else if (item instanceof String s) {
      name = s;
    }

    if (name == null) {
      return null;
    }

    return WikiUtilities.getWikiLocation(name, type, dataPage);
  }

  public static void showWikiDescription(final Object item) {
    String location = WikiUtilities.getWikiLocation(item);

    if (location != null) {
      RelayLoader.openSystemBrowser(location);
    }
  }
}
