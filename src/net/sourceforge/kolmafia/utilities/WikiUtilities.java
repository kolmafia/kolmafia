package net.sourceforge.kolmafia.utilities;

import java.util.Map.Entry;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiUtilities {

  public enum WikiType {
    ANY_TYPE,
    ITEM_TYPE,
    EFFECT_TYPE,
    SKILL_TYPE,
    MONSTER_TYPE
  }

  private WikiUtilities() {}

  public static String getWikiLocation(String name, WikiType type, boolean dataPage) {
    boolean checkOtherTables = true;

    if (type != WikiType.ANY_TYPE) {
      ModifierType modType =
          switch (type) {
            case ITEM_TYPE -> ModifierType.ITEM;
            case EFFECT_TYPE -> ModifierType.EFFECT;
            case SKILL_TYPE -> ModifierType.SKILL;
            default -> ModifierType.NONE;
          };

      Modifiers mods = Modifiers.getModifiers(modType, name);
      if (mods != null) {
        String wikiname = mods.getString("Wiki Name");
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

    return "https://kol.coldfront.net/thekolwiki/index.php/" + name;
  }

  private static String disambiguateTypes(WikiType type, String name) {
    boolean inItemTable = ItemDatabase.containsExactly(name);
    boolean inEffectTable = EffectDatabase.containsExactly(name);
    boolean inSkillTable = SkillDatabase.contains(name);
    boolean inMonsterTable = MonsterDatabase.contains(name);
    switch (type) {
      case ITEM_TYPE -> {
        if (inEffectTable || inSkillTable || inMonsterTable) {
          return name + " (item)";
        }
        return name;
      }
      case EFFECT_TYPE -> {
        if (name.equals("Souped Up")) {
          // also an adventure
          return name + " (effect)";
        }
        if (inItemTable || inSkillTable || inMonsterTable) {
          return name + " (effect)";
        }
        return name;
      }
      case SKILL_TYPE -> {
        if (inItemTable || inEffectTable || inMonsterTable) {
          return name + " (skill)";
        }
        return name;
      }
      case MONSTER_TYPE -> {
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
    WikiType type = WikiType.ANY_TYPE;

    if (item instanceof Boost) {
      item = ((Boost) item).getItem();
    } else if (item instanceof Entry) {
      item = ((Entry<?, ?>) item).getValue();
    }

    if (item instanceof MonsterData) {
      name = ((MonsterData) item).getWikiName();
      type = WikiType.MONSTER_TYPE;
    } else if (item instanceof AdventureResult result) {
      name = result.getDataName();

      type =
          result.isItem()
              ? WikiType.ITEM_TYPE
              : result.isStatusEffect() ? WikiType.EFFECT_TYPE : WikiType.ANY_TYPE;
    } else if (item instanceof UseSkillRequest) {
      name = ((UseSkillRequest) item).getSkillName();
      type = WikiType.SKILL_TYPE;
    } else if (item instanceof Concoction) {
      name = ((Concoction) item).getName();
      type = WikiType.ITEM_TYPE;
    } else if (item instanceof QueuedConcoction) {
      name = ((QueuedConcoction) item).getName();
      type = WikiType.ITEM_TYPE;
    } else if (item instanceof CreateItemRequest) {
      name = ((CreateItemRequest) item).getName();
      type = WikiType.ITEM_TYPE;
    } else if (item instanceof PurchaseRequest) {
      name = ((PurchaseRequest) item).getItem().getDataName();
      type = WikiType.ITEM_TYPE;
    } else if (item instanceof SoldItem) {
      name = ((SoldItem) item).getItemName();
      type = WikiType.ITEM_TYPE;
    } else if (item instanceof String) {
      name = (String) item;
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
