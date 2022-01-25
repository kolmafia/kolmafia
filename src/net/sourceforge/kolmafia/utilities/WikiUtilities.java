package net.sourceforge.kolmafia.utilities;

import java.util.Map.Entry;
import net.sourceforge.kolmafia.AdventureResult;
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
  public static final int ANY_TYPE = 0;
  public static final int ITEM_TYPE = 1;
  public static final int EFFECT_TYPE = 2;
  public static final int SKILL_TYPE = 3;
  public static final int MONSTER_TYPE = 4;

  private WikiUtilities() {}

  public static final String getWikiLocation(String name, int type) {
    boolean checkOtherTables = true;

    if (type != ANY_TYPE) {
      String modType =
          type == ITEM_TYPE
              ? "Item"
              : type == EFFECT_TYPE ? "Effect" : type == SKILL_TYPE ? "Skill" : "None";

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
      boolean inItemTable = ItemDatabase.containsExactly(name);
      boolean inEffectTable = EffectDatabase.containsExactly(name);
      boolean inSkillTable = SkillDatabase.contains(name);
      boolean inMonsterTable = MonsterDatabase.contains(name);
      switch (type) {
        case ITEM_TYPE:
          if (name.equals("sweet tooth")
              || name.equals("water wings")
              || name.equals("knuckle sandwich")
              || name.equals("industrial strength starch")) {
            // If its not an effect or skill, no disambiguation needed
          } else if (name.equals("black pudding")) {
            name = name + " (food)";
          } else if (name.equals("ice porter")) {
            name = name + " (drink)";
          } else if (name.equals("Bulky Buddy Box")) {
            name = name + " (hatchling)";
          } else if (name.equals("The Sword in the Steak")) {
            // Also an adventure
            name = name + " (item)";
          } else if (inEffectTable || inSkillTable || inMonsterTable) {
            name = name + " (item)";
          }
          break;
        case EFFECT_TYPE:
          if (name.equals("Sweet Tooth")
              || name.equals("Water Wings")
              || name.equals("Industrial Strength Starch")) {
            // all items but the case is different
          } else if (inItemTable || inSkillTable || inMonsterTable) {
            name = name + " (effect)";
          }
          break;
        case SKILL_TYPE:
          if (inItemTable || inEffectTable || inMonsterTable) {
            name = name + " (skill)";
          }
          break;
        case MONSTER_TYPE:
          if (name.equals("ice porter")) {
            // Also a drink.
          } else if (name.equals("undead elbow macaroni")) {
            // Also (formerly) a pasta guardian
            name = name + " (monster)";
          } else if (inItemTable || inEffectTable || inSkillTable) {
            name = name + " (monster)";
          }
          break;
      }
    }

    name = StringUtilities.globalStringReplace(name, "#", "");
    name = StringUtilities.globalStringReplace(name, "<i>", "");
    name = StringUtilities.globalStringReplace(name, "</i>", "");
    name = StringUtilities.globalStringReplace(name, "<s>", "");
    name = StringUtilities.globalStringReplace(name, "</s>", "");
    name = StringUtilities.globalStringReplace(name, " ", "_");
    name = StringUtilities.globalStringReplace(name, "?", "%3F");

    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

    // Turn character entities into characters
    name = CharacterEntities.unescape(name);

    if (type == MONSTER_TYPE) {
      name = StringUtilities.getURLEncode(name);
      name = "Data:" + name;
    }

    return "https://kol.coldfront.net/thekolwiki/index.php/" + name;
  }

  public static final String getWikiLocation(Object item) {
    if (item == null) {
      return null;
    }

    String name = null;
    int type = WikiUtilities.ANY_TYPE;

    if (item instanceof Boost) {
      item = ((Boost) item).getItem();
    } else if (item instanceof Entry) {
      item = ((Entry<?, ?>) item).getValue();
    }

    if (item instanceof MonsterData) {
      name = ((MonsterData) item).getWikiName();
      type = WikiUtilities.MONSTER_TYPE;
    } else if (item instanceof AdventureResult) {
      AdventureResult result = (AdventureResult) item;
      name = result.getDataName();

      type =
          result.isItem()
              ? WikiUtilities.ITEM_TYPE
              : result.isStatusEffect() ? WikiUtilities.EFFECT_TYPE : WikiUtilities.ANY_TYPE;
    } else if (item instanceof UseSkillRequest) {
      name = ((UseSkillRequest) item).getSkillName();
      type = WikiUtilities.SKILL_TYPE;
    } else if (item instanceof Concoction) {
      name = ((Concoction) item).getName();
      type = WikiUtilities.ITEM_TYPE;
    } else if (item instanceof QueuedConcoction) {
      name = ((QueuedConcoction) item).getName();
      type = WikiUtilities.ITEM_TYPE;
    } else if (item instanceof CreateItemRequest) {
      name = ((CreateItemRequest) item).getName();
      type = WikiUtilities.ITEM_TYPE;
    } else if (item instanceof PurchaseRequest) {
      name = ((PurchaseRequest) item).getItem().getDataName();
      type = WikiUtilities.ITEM_TYPE;
    } else if (item instanceof SoldItem) {
      name = ((SoldItem) item).getItemName();
      type = WikiUtilities.ITEM_TYPE;
    } else if (item instanceof String) {
      name = (String) item;
    }

    if (name == null) {
      return null;
    }

    return WikiUtilities.getWikiLocation(name, type);
  }

  public static final void showWikiDescription(final Object item) {
    String location = WikiUtilities.getWikiLocation(item);

    if (location != null) {
      RelayLoader.openSystemBrowser(location);
    }
  }
}
