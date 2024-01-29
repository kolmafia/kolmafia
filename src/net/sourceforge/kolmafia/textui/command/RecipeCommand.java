package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RecipeCommand extends AbstractCommand {
  public RecipeCommand() {
    this.usage = " <item> [, <item>]... - get ingredients or recipe for items.";
  }

  @Override
  public void run(final String cmd, final String params) {
    String[] concoctions = params.split("\\s*,\\s*");

    if (concoctions.length == 0) {
      return;
    }

    StringBuilder buffer = new StringBuilder();

    for (int i = 0; i < concoctions.length; ++i) {
      AdventureResult item = ItemFinder.getFirstMatchingItem(concoctions[i]);
      if (item == null) {
        continue;
      }

      int itemId = item.getItemId();
      String name = item.getName();
      String startNumber = "";
      if (concoctions.length > 1) {
        startNumber = (i + 1) + ". ";
      }

      if (ConcoctionDatabase.getMixingMethod(itemId) == CraftingType.NOCREATE) {
        RequestLogger.printHtml(startNumber + "This item cannot be created: <b>" + name + "</b>");
        continue;
      }

      buffer.setLength(0);
      if (concoctions.length > 1) {
        buffer.append(startNumber);
      }

      if (cmd.equals("ingredients")) {
        RecipeCommand.getIngredients(item, buffer);
      } else if (cmd.equals("recipe")) {
        RecipeCommand.getRecipe(item, buffer, 0);
      }

      RequestLogger.printLine(buffer.toString());
    }
  }

  private static void getIngredients(final AdventureResult ar, final StringBuilder sb) {
    sb.append("<b>");
    sb.append(ar.getInstance(ConcoctionDatabase.getYield(ar.getItemId())).toString());
    sb.append("</b>: ");

    List<AdventureResult> ingredients =
        RecipeCommand.getFlattenedIngredients(ar, new ArrayList<>(), false);
    Collections.sort(ingredients);

    Iterator<AdventureResult> it = ingredients.iterator();
    boolean first = true;
    while (it.hasNext()) {
      AdventureResult ingredient = it.next();
      int need = ingredient.getCount();
      int have = InventoryManager.getAccessibleCount(ingredient);
      int missing = need - have;

      if (!first) {
        sb.append(", ");
      }

      first = false;

      if (missing < 1) {
        sb.append(ingredient);
        continue;
      }

      sb.append("<i>");
      sb.append(ingredient.getName());
      sb.append(" (");
      sb.append(have);
      sb.append("/");
      sb.append(need);
      sb.append(")</i>");
    }
  }

  private static List<AdventureResult> getFlattenedIngredients(
      AdventureResult ar, List<AdventureResult> list, boolean deep) {
    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(ar.getItemId());
    for (AdventureResult ingredient : ingredients) {
      if (ConcoctionDatabase.getMixingMethod(ingredient.getItemId()) != CraftingType.NOCREATE) {
        int have = InventoryManager.getAccessibleCount(ingredient);
        if (!RecipeCommand.isRecursing(ar, ingredient) && (deep || have == 0)) {
          RecipeCommand.getFlattenedIngredients(ingredient, list, deep);
          continue;
        }
      }
      AdventureResult.addResultToList(list, ingredient);
    }

    return list;
  }

  private static boolean isRecursing(final AdventureResult parent, final AdventureResult child) {
    if (parent.equals(child)) {
      // should never actually happen, but eh
      return true;
    }

    if (ConcoctionDatabase.getMixingMethod(parent.getItemId()) == CraftingType.ROLLING_PIN) {
      return true;
    }

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(child.getItemId());
    for (AdventureResult ingredient : ingredients) {
      if (ingredient.equals(parent)) {
        return true;
      }
    }

    return false;
  }

  private static void getRecipe(final AdventureResult ar, final StringBuilder sb, final int depth) {
    if (depth > 0) {
      sb.append("<br>");
      sb.append("\u00a0\u00a0\u00a0".repeat(depth));
    }

    int itemId = ar.getItemId();

    sb.append("<b>");
    sb.append(ar.getInstance(ConcoctionDatabase.getYield(ar.getItemId())).toString());
    sb.append("</b>");

    CraftingType mixingMethod = ConcoctionDatabase.getMixingMethod(itemId);
    EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements(itemId);
    if (mixingMethod != CraftingType.NOCREATE) {
      sb.append(": <i>[");
      sb.append(ConcoctionDatabase.mixingMethodDescription(mixingMethod, requirements));
      sb.append("]</i> ");

      AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);
      for (int i = 0; i < ingredients.length; ++i) {
        AdventureResult ingredient = ingredients[i];
        if (i > 0) {
          sb.append(" + ");
        }
        sb.append(ingredient.toString());
      }

      for (AdventureResult ingredient : ingredients) {
        if (RecipeCommand.isRecursing(ar, ingredient)) {
          continue;
        }
        RecipeCommand.getRecipe(ingredient, sb, depth + 1);
      }
    }
  }
}
