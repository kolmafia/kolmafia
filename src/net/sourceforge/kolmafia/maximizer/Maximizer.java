package net.sourceforge.kolmafia.maximizer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.filterType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneResultPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.BeachManager;
import net.sourceforge.kolmafia.session.BeachManager.BeachHead;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import net.sourceforge.kolmafia.utilities.IntOrString;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Maximizer {
  private static boolean firstTime = true;

  public static final LockableListModel<Boost> boosts = new LockableListModel<>();
  public static Evaluator eval;

  public static String[] maximizationCategories = {
    "_hoboPower",
    "_brimstone",
    "_cloathing",
    "_slimeHate",
    "_stickers",
    "_folderholder",
    "_cardsleeve",
    "_smithsness",
  };

  static MaximizerSpeculation best;
  static int bestChecked;
  static long bestUpdate;

  private Maximizer() {}

  public static boolean maximize(
      String maximizerString, int maxPrice, PriceLevel priceLevel, boolean isSpeculationOnly) {
    MaximizerFrame.expressionSelect.setSelectedItem(maximizerString);
    EquipScope equipScope =
        isSpeculationOnly ? EquipScope.SPECULATE_INVENTORY : EquipScope.EQUIP_NOW;

    // iECOC has to be turned off before actually maximizing as
    // it would cause all item lookups during the process to just
    // print the item name and return null.

    KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

    Maximizer.maximize(equipScope, maxPrice, priceLevel, false, EnumSet.allOf(filterType.class));

    if (!KoLmafia.permitsContinue()) {
      return false;
    }

    Modifiers mods = Maximizer.best.calculate();
    ModifierDatabase.overrideModifier(ModifierType.GENERATED, "_spec", mods);

    return !Maximizer.best.failed;
  }

  public static void maximize(
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean includeAll,
      Set<filterType> filter) {
    KoLmafia.forceContinue();
    String maxMe = (String) MaximizerFrame.expressionSelect.getSelectedItem();
    RequestLogger.printLine("Maximizer: " + maxMe);
    RequestLogger.updateSessionLog("Maximizer: " + maxMe);
    KoLConstants.maximizerMList.addItem(maxMe);
    Maximizer.eval = new Evaluator(maxMe);
    int filterCount = filter.size();
    var limitMode = KoLCharacter.getLimitMode();

    // parsing error
    if (!KoLmafia.permitsContinue() || filterCount == 0) {
      return;
    }

    // ensure current modifiers are up-to-date
    KoLCharacter.recalculateAdjustments();
    double current =
        Maximizer.eval.getScore(
            KoLCharacter.getCurrentModifiers(), EquipmentManager.currentEquipment());

    if (maxPrice <= 0) {
      maxPrice = Preferences.getInteger("autoBuyPriceLimit");
    }

    KoLmafia.updateDisplay(
        Maximizer.firstTime ? "Maximizing (1st time may take a while)..." : "Maximizing...");
    Maximizer.firstTime = false;

    Maximizer.boosts.clear();
    if (filter.contains(KoLConstants.filterType.EQUIP)) {
      Maximizer.best = new MaximizerSpeculation();
      Maximizer.best.getScore();
      // In case the current outfit scores better than any tried combination,
      // due to some newly-added constraint (such as +melee):
      Maximizer.best.failed = true;
      Maximizer.bestChecked = 0;
      Maximizer.bestUpdate = System.currentTimeMillis() + 5000;
      try {
        Maximizer.eval.enumerateEquipment(equipScope, maxPrice, priceLevel);
      } catch (MaximizerExceededException e) {
        Maximizer.boosts.add(
            new Boost(
                "", "(maximum achieved, no further combinations checked)", Slot.NONE, null, 0.0));
      } catch (MaximizerLimitException e) {
        Maximizer.boosts.add(
            new Boost(
                "",
                "<font color=red>(hit combination limit, optimality not guaranteed)</font>",
                Slot.NONE,
                null,
                0.0));
      } catch (MaximizerInterruptedException e) {
        KoLmafia.forceContinue();
        Maximizer.boosts.add(
            new Boost(
                "",
                "<font color=red>(interrupted, optimality not guaranteed)</font>",
                Slot.NONE,
                null,
                0.0));
      }
      MaximizerSpeculation.showProgress();

      EnumSet<Slot> alreadyDone = EnumSet.noneOf(Slot.class);

      for (Slot slot : SlotSet.ACCESSORY_SLOTS) {
        if (Maximizer.best.equipment.get(slot).getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE
            && EquipmentManager.getEquipment(slot).getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE) {
          equipScope = Maximizer.emitSlot(slot, equipScope, maxPrice, priceLevel, current);
          alreadyDone.add(slot);
        }
      }

      for (var slot : SlotSet.ALL_SLOTS) {
        if (!alreadyDone.contains(slot)) {
          equipScope = Maximizer.emitSlot(slot, equipScope, maxPrice, priceLevel, current);
        }
      }
    }

    current =
        Maximizer.eval.getScore(
            KoLCharacter.getCurrentModifiers(), EquipmentManager.currentEquipment());

    // Show only equipment
    if (filter.contains(filterType.EQUIP) && filterCount == 1) {
      return;
    }

    // Include skills from absorbing items in Noobcore
    if (KoLCharacter.inNoobcore()) {
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.SKILL)) {
        if (!entry.getKey().isInt()) continue;
        int skillId = entry.getKey().getIntValue();
        if (skillId < 23001 || skillId > 23125) {
          continue;
        }
        if (KoLCharacter.hasSkill(skillId)) {
          continue;
        }
        int absorbsLeft = KoLCharacter.getAbsorbsLimit() - KoLCharacter.getAbsorbs();
        if (absorbsLeft < 1) {
          continue;
        }
        MaximizerSpeculation spec = new MaximizerSpeculation();
        String mods = entry.getValue();
        spec.setCustom(mods);
        double delta = spec.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        int[] itemList = ItemDatabase.getItemListByNoobSkillId(skillId);
        if (itemList == null) {
          continue;
        }
        // Iterate over items to see if we have access to them
        int count = 0;
        for (int itemId : itemList) {
          var makeable = getAbsorbable(itemId, equipScope, maxPrice, priceLevel);
          if (!makeable.canMake) continue;
          String cmd = makeable.cmd;
          String text = makeable.txt;
          text = text + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
          text = text + " [" + absorbsLeft + " absorbs remaining]";
          if (count > 0) {
            text = "  or " + text;
          }
          Maximizer.boosts.add(new Boost(cmd, text, ItemPool.get(itemId), delta));
          count++;
        }
      }

      // Include enchantments from absorbing equipment in Noobcore
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.ITEM)) {
        if (!entry.getKey().isInt()) continue;
        int itemId = entry.getKey().getIntValue();
        int absorbsLeft = KoLCharacter.getAbsorbsLimit() - KoLCharacter.getAbsorbs();
        if (absorbsLeft < 1) {
          continue;
        }
        // Cannot abosrb undiscardable items
        if (!ItemDatabase.isDiscardable(itemId)) {
          continue;
        }
        // Can only absorb tradeable and gift items
        if (!ItemDatabase.isTradeable(itemId) && !ItemDatabase.isGiftItem(itemId)) {
          continue;
        }
        // Can only get it from Equipment (not including familiar equipment)
        if (!ItemDatabase.isEquipment(itemId) || ItemDatabase.isFamiliarEquipment(itemId)) {
          continue;
        }
        MaximizerSpeculation spec = new MaximizerSpeculation();
        Modifiers itemMods = ModifierDatabase.getItemModifiers(itemId);
        if (itemMods == null) {
          continue;
        }
        // Only take numeric modifiers, and not Surgeonosity, from Items in Noobcore
        StringBuilder mods = new StringBuilder();
        for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
          if (itemMods.getDouble(mod) != 0.0) {
            if (mods.length() > 0) {
              mods.append(", ");
            }
            mods.append(mod.getName() + ": " + itemMods.getDouble(mod));
          }
        }
        if (mods.length() == 0) {
          continue;
        }
        spec.setCustom(mods.toString());
        double delta = spec.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        var makeable = getAbsorbable(itemId, equipScope, maxPrice, priceLevel);
        if (!makeable.canMake) continue;
        String cmd = makeable.cmd;
        String text = makeable.txt;
        CheckedItem checkedItem = makeable.checkedItem;
        text = text + "lasts til end of day, ";
        text = text + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        text = text + " [" + absorbsLeft + " absorbs remaining";
        if (checkedItem.inventory > 0) {
          text = text + ", " + checkedItem.inventory + " in inventory";
        }
        if (checkedItem.initial - checkedItem.inventory > 0) {
          text = text + ", " + (checkedItem.initial - checkedItem.inventory) + " obtainable";
        }
        if (checkedItem.creatable > 0) {
          text = text + ", " + checkedItem.creatable + " createable";
        }
        if (checkedItem.npcBuyable > 0) {
          text = text + ", " + checkedItem.npcBuyable + " NPC buyable";
        }
        if (checkedItem.pullable > 0) {
          text = text + ", " + checkedItem.pullable + " pullable";
        }
        text = text + "]";
        Maximizer.boosts.add(new Boost(cmd, text, ItemPool.get(itemId), delta));
      }
    }

    if (filter.contains(KoLConstants.filterType.OTHER)) {
      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.HORSERY)) {
        if (!entry.getKey().isString()) continue;
        String name = entry.getKey().getStringValue();
        // Must be available in your current path
        if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Horsery contract")) {
          continue;
        }
        String cmd, text;
        int price = 0;
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.setHorsery(name);
        double delta = spec.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        text = "horsery " + name;
        cmd = "horsery " + name;
        if (!Preferences.getBoolean("horseryAvailable")) {
          cmd = "";
          if (includeAll) {
            text = "(get a horsery and ride a " + name + ")";
          } else continue;
        }
        text += " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        if (Preferences.getString("_horsery").length() > 0) {
          price = 500;
        }
        if (KoLCharacter.getAvailableMeat() < price) {
          cmd = "";
        }
        if (Preferences.getBoolean("verboseMaximizer")) {
          text += " [" + price + " meat]";
        }
        Maximizer.boosts.add(new Boost(cmd, text, name, delta));
      }

      for (Map.Entry<IntOrString, String> entry :
          ModifierDatabase.getAllModifiersOfType(ModifierType.BOOM_BOX)) {
        if (!entry.getKey().isString()) continue;
        String name = entry.getKey().getStringValue();
        String cmd, text;
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.setBoomBox(name);
        double delta = spec.getScore() - current;
        if (delta <= 0.0) {
          continue;
        }
        text = "boombox " + name.toLowerCase();
        cmd = "boombox " + name.toLowerCase();
        if (!InventoryManager.hasItem(ItemPool.BOOMBOX)) {
          cmd = "";
          if (includeAll) {
            text = "(get a SongBoom&trade; BoomBox and play " + name + ")";
          } else continue;
        }
        int usesRemaining = Preferences.getInteger("_boomBoxSongsLeft");
        text += " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        if (Preferences.getBoolean("verboseMaximizer")) {
          if (usesRemaining == 1) {
            text += " [1 use remaining]";
          } else {
            text += " [" + usesRemaining + " uses remaining]";
          }
        }
        if (usesRemaining < 1) {
          cmd = "";
        }
        Maximizer.boosts.add(new Boost(cmd, text, (AdventureResult) null, delta));
      }
    }

    for (Map.Entry<IntOrString, String> entry :
        ModifierDatabase.getAllModifiersOfType(ModifierType.EFFECT)) {
      if (!entry.getKey().isInt()) continue;
      int effectId = entry.getKey().getIntValue();
      if (effectId == -1) {
        continue;
      }

      double delta;
      boolean isSpecial = false;
      MaximizerSpeculation spec = new MaximizerSpeculation();
      AdventureResult effect = EffectPool.get(effectId);
      String name = effect.getName();
      boolean hasEffect = KoLConstants.activeEffects.contains(effect);
      Iterator<String> sources;

      if (!hasEffect) {
        spec.addEffect(effect);
        delta = spec.getScore() - current;
        if ((spec.getModifiers().getRawBitmap(BitmapModifier.MUTEX_VIOLATIONS)
                & ~KoLCharacter.currentRawBitmapModifier(BitmapModifier.MUTEX_VIOLATIONS))
            != 0) { // This effect creates a mutex problem that the player
          // didn't already have.  In the future, perhaps suggest
          // uneffecting the conflicting effect, but for now just skip.
          continue;
        }
        switch (Maximizer.eval.checkConstraints(ModifierDatabase.getEffectModifiers(effectId))) {
          case VIOLATES:
            continue;
          case IRRELEVANT:
            if (delta <= 0.0) continue;
            break;
          case MEETS:
            isSpecial = true;
        }
        if (Evaluator.cannotGainEffect(effectId)) {
          continue;
        }
        sources = EffectDatabase.getAllActions(effectId);
        if (!sources.hasNext()) {
          if (includeAll) {
            sources = Collections.singletonList("(no known source of " + name + ")").iterator();
          } else continue;
        }
      } else {
        spec.removeEffect(effect);
        delta = spec.getScore() - current;
        switch (Maximizer.eval.checkConstraints(ModifierDatabase.getEffectModifiers(effectId))) {
          case MEETS:
            continue;
          case IRRELEVANT:
            if (delta <= 0.0) continue;
            break;
          case VIOLATES:
            isSpecial = true;
        }
        String cmd = MoodManager.getDefaultAction("gain_effect", name);
        if (cmd.length() == 0) {
          if (includeAll) {
            cmd = "(find some way to remove " + name + ")";
          } else continue;
        }
        sources = Collections.singletonList(cmd).iterator();
      }

      boolean haveVipKey = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean orFlag = false;
      while (sources.hasNext()) {
        if (!KoLmafia.permitsContinue()) {
          return;
        }

        String cmd, text;

        int price = 0;
        int advCost = 0;
        long mpCost = 0;
        int fullCost = 0;
        int drunkCost = 0;
        int spleenCost = 0;
        int soulsauceCost = 0;
        int thunderCost = 0;
        int rainCost = 0;
        int lightningCost = 0;
        int fuelCost = 0;
        int hpCost = 0;
        int duration = 0;
        long usesRemaining = 0;
        int itemsRemaining = 0;
        int itemsCreatable = 0;

        cmd = text = sources.next();
        AdventureResult item = null;

        // Check filters

        String basecommand = cmd.trim().contains(" ") ? cmd.split(" ")[0] : cmd;

        switch (basecommand) {
          case "cast":
            if (!filter.contains(KoLConstants.filterType.CAST)) continue;
            break;
          case "synthesize":
          case "chew":
            if (!filter.contains(KoLConstants.filterType.SPLEEN)) continue;
            break;
          case "drink":
            if (!filter.contains(KoLConstants.filterType.BOOZE)) continue;
            break;
          case "eat":
            if (!filter.contains(KoLConstants.filterType.FOOD)) continue;
            break;
          case "use":
            if (!filter.contains(KoLConstants.filterType.USABLE)) continue;
            break;
          default:
            if (!filter.contains(KoLConstants.filterType.OTHER)) continue;
        }

        if (cmd.startsWith("#")) { // usage note, no command
          if (includeAll) {
            if (cmd.contains("BM") && !KoLCharacter.inBadMoon()) {
              continue; // no use displaying this in non-BM
            }
            text = (orFlag ? "(...or get " : "(get ") + name + " via " + cmd.substring(1) + ")";
            orFlag = false;
            cmd = "";
          } else continue;
        }

        if (hasEffect && !cmd.toLowerCase().contains(name.toLowerCase())) {
          text = text + " (to remove " + name + ")";
        }

        if (cmd.startsWith("(")) { // preformatted note
          cmd = "";
          orFlag = false;
        } else if (cmd.startsWith("use ")
            || cmd.startsWith("chew ")
            || cmd.startsWith("drink ")
            || cmd.startsWith("eat ")) {
          // Hardcoded exception for "Trivia Master", which has a non-standard use command.
          if (cmd.contains("use 1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?")
              && !MoodManager.canMasterTrivia()) {
            continue;
          }

          // Can get Box of Sunshine in hardcore/ronin, but can't use it
          if (!KoLCharacter.canInteract() && cmd.startsWith("use 1 box of sunshine")) {
            continue;
          }

          String iName = cmd.substring(cmd.indexOf(" ") + 3).trim();
          if (cmd.startsWith("use ")) {
            item = ItemFinder.getFirstMatchingItem(iName, false, Match.USE);
          } else if (cmd.startsWith("chew ")) {
            item = ItemFinder.getFirstMatchingItem(iName, false, Match.SPLEEN);
          } else if (cmd.startsWith("drink ")) {
            item = ItemFinder.getFirstMatchingItem(iName, false, Match.BOOZE);
          } else if (cmd.startsWith("eat ")) {
            item = ItemFinder.getFirstMatchingItem(iName, false, Match.FOOD);
          }

          if (item != null) {
            int itemId = item.getItemId();

            // Certain items with side-effects have enchantments only in TCRS,
            // Perhaps we should let the user accept the item - and suffer the
            // side-effect. Perhaps we should simply exclude the item.
            if (Maximizer.excludedTCRSItem(itemId)) {
              continue;
            }

            // Cannot use/eat/drink items without G's in them in G-Lover except from restaurants
            if (KoLCharacter.inGLover()
                && !KoLCharacter.hasGs(iName)
                && !KoLConstants.restaurantItems.contains(iName)
                && !KoLConstants.microbreweryItems.contains(iName)
                && !KoLConstants.cafeItems.contains(iName)) {
              continue;
            }

            if (itemId == ItemPool.VAMPIRE_VINTNER_WINE) {
              // 1950 Vampire Vintner wine is a quest item.
              // If you don't have it, you must adventure to get one.
              if (!InventoryManager.hasItem(itemId)) {
                continue;
              }
              // 1950 Vampire Vintner wine can provide any of seven
              // effects.  Only consider using one if the wine you
              // currently have provides this effect.
              if (!Preferences.getString("vintnerWineEffect").equals(name)) {
                continue;
              }
              duration = 12;
            }

            // Resolve bang potions and slime vials
            if (itemId == -1) {
              item = item.resolveBangPotion();
              itemId = item.getItemId();
            }
            if (itemId == -1) {
              continue;
            }

            Modifiers effMod = ModifierDatabase.getItemModifiers(item.getItemId());
            if (effMod != null) {
              duration = (int) effMod.getDouble(DoubleModifier.EFFECT_DURATION);
            }
          }
          // Hot Dogs don't have items
          if (item == null && ClanLoungeRequest.isHotDog(iName)) {
            if (KoLCharacter.inBadMoon()) {
              continue;
            } else if (!StandardRequest.isAllowed(
                RestrictedItemType.CLAN_ITEMS, "Clan Hot Dog Stand")) {
              continue;
            }
            // Jarlsberg and Zombie characters can't eat hot dogs
            else if (KoLCharacter.isJarlsberg() || KoLCharacter.isZombieMaster()) {
              continue;
            } else if (limitMode.limitClan()) {
              continue;
            } else if (!haveVipKey) {
              if (includeAll) {
                text = "( get access to the VIP lounge )";
                cmd = "";
              } else continue;
            }
            // Fullness available?
            fullCost = ClanLoungeRequest.hotdogNameToFullness(iName);
            if (fullCost > 0
                && KoLCharacter.getFullness() + fullCost > KoLCharacter.getFullnessLimit()) {
              continue;
            }
            // Is it Fancy and has one been used?
            if (ClanLoungeRequest.isFancyHotDog(iName)
                && Preferences.getBoolean("_fancyHotDogEaten")) {
              continue;
            } else {
              Modifiers effMod = ModifierDatabase.getModifiers(ModifierType.ITEM, iName);
              if (effMod != null) {
                duration = (int) effMod.getDouble(DoubleModifier.EFFECT_DURATION);
              }
              usesRemaining = 1;
            }
          } else if (item == null && !cmd.contains(",")) {
            if (includeAll) {
              text = "(identify & " + cmd + ")";
              cmd = "";
            } else continue;
          } else if (item != null) {
            int itemId = item.getItemId();
            usesRemaining = UseItemRequest.maximumUses(itemId);
            if (usesRemaining <= 0) {
              continue;
            }
          }
        } else if (cmd.startsWith("gong ")) {
          item = ItemPool.get(ItemPool.GONG, 1);
          advCost = 3;
          duration = 20;
        } else if (cmd.startsWith("cast ")) {
          String skillName = UneffectRequest.effectToSkill(name);
          if (!StandardRequest.isAllowed(RestrictedItemType.SKILLS, skillName)) {
            continue;
          }

          int skillId = SkillDatabase.getSkillId(skillName);
          UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);

          if (skill != null) {
            usesRemaining = skill.getMaximumCast();
          }

          if (!KoLCharacter.hasSkill(skillId) || usesRemaining == 0) {
            if (includeAll) {
              boolean isBuff = SkillDatabase.isBuff(skillId);
              text = "(learn to " + cmd + (isBuff ? ", or get it from a buffbot)" : ")");
              cmd = "";
            } else continue;
          }

          mpCost = SkillDatabase.getMPConsumptionById(skillId);
          advCost = SkillDatabase.getAdventureCost(skillId);
          soulsauceCost = SkillDatabase.getSoulsauceCost(skillId);
          thunderCost = SkillDatabase.getThunderCost(skillId);
          rainCost = SkillDatabase.getRainCost(skillId);
          lightningCost = SkillDatabase.getLightningCost(skillId);
          hpCost = SkillDatabase.getHPCost(skillId);
          duration = SkillDatabase.getEffectDuration(skillId);
        } else if (cmd.startsWith("synthesize ")) {
          // Not available in G-Lover
          if (KoLCharacter.inGLover()) {
            continue;
          }
          // Must be available in your current path
          if (!StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Sweet Synthesis")) {
            continue;
          }
          // You must know the skill
          if (!KoLCharacter.hasSkill(SkillPool.SWEET_SYNTHESIS)) {
            if (includeAll) {
              text = "(learn the Sweet Synthesis skill)";
              cmd = "";
            } else continue;
          }
          // You must have a spleen available
          usesRemaining = KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse();
          if (usesRemaining < 1) {
            cmd = "";
          }
          // You must have (or be able to get) a suitable pair of candies
          if (CandyDatabase.synthesisPair(effectId) == CandyDatabase.NO_PAIR) {
            cmd = "";
          }
          duration = 30;
          spleenCost = 1;
        } else if (cmd.startsWith("pillkeeper")) {
          // Must be available in your current path
          if (!StandardRequest.isAllowed(
              RestrictedItemType.ITEMS, "Eight Days a Week Pill Keeper")) {
            continue;
          }
          // You must have the pill keeper
          if (!InventoryManager.hasItem(ItemPool.PILL_KEEPER)) {
            if (includeAll) {
              text = "(get an Eight Days a Week Pill Keeper)";
              cmd = "";
            } else continue;
          }
          // If the free daily use has been spent, you must have 3 spleen available
          if (Preferences.getBoolean("_freePillKeeperUsed")) {
            usesRemaining = KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse();
            if (usesRemaining < 3) {
              cmd = "";
            }
            spleenCost = 3;
          }
          duration = 30;
        } else if (cmd.startsWith("cargo effect ")) {
          // Must be available in your current path
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Cargo Cultist Shorts")) {
            continue;
          }
          // You must have the cargo shorts
          if (!InventoryManager.hasItem(ItemPool.CARGO_CULTIST_SHORTS)) {
            if (includeAll) {
              text = "(acquire a pair of Cargo Cultist Shorts for " + name + ")";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_cargoPocketEmptied")) {
            cmd = "";
          } else {
            // Find an unpicked pocket with the effect
            Set<OneResultPocket> pockets = PocketDatabase.effectPockets.get(name);
            List<Pocket> sorted = PocketDatabase.sortResults(name, pockets);
            Pocket pocket = PocketDatabase.firstUnpickedPocket(sorted);
            if (pocket == null) {
              // You have used all the pockets with this effect this ascension
              cmd = "";
            } else {
              // It's available
              duration = ((OneResultPocket) pocket).getCount(name);
            }
          }
          usesRemaining = Preferences.getBoolean("_cargoPocketEmptied") ? 0 : 1;
        } else if (cmd.startsWith("friars ")) {
          int lfc = Preferences.getInteger("lastFriarCeremonyAscension");
          int ka = Preferences.getInteger("knownAscensions");
          if (lfc < ka || limitMode.limitZone("Friars")) {
            continue;
          } else if (Preferences.getBoolean("friarsBlessingReceived")) {
            cmd = "";
          }
          duration = 20;
          usesRemaining = Preferences.getBoolean("friarsBlessingReceived") ? 0 : 1;
        } else if (cmd.startsWith("hatter ")) {
          boolean haveEffect =
              KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE));
          boolean havePotion = InventoryManager.hasItem(ItemPool.DRINK_ME_POTION);
          if (!havePotion && !haveEffect) {
            continue;
          } else if (!RabbitHoleManager.hatLengthAvailable(
              StringUtilities.parseInt(cmd.substring(7)))) {
            continue;
          } else if (limitMode.limitZone("Rabbit Hole")) {
            continue;
          } else if (Preferences.getBoolean("_madTeaParty")) {
            cmd = "";
          }
          duration = 30;
          usesRemaining = Preferences.getBoolean("_madTeaParty") ? 0 : 1;
        } else if (cmd.startsWith("mom ")) {
          if (!QuestDatabase.isQuestFinished(Quest.SEA_MONKEES)) {
            continue;
          } else if (limitMode.limitZone("The Sea")) {
            continue;
          } else if (Preferences.getBoolean("_momFoodReceived")) {
            cmd = "";
          }
          duration = 50;
          usesRemaining = Preferences.getBoolean("_momFoodReceived") ? 0 : 1;
        } else if (cmd.startsWith("summon ")) {
          if (!QuestDatabase.isQuestFinished(Quest.MANOR)) {
            continue;
          }
          int onHand = InventoryManager.getAccessibleCount(ItemPool.EVIL_SCROLL);
          int candles = InventoryManager.getAccessibleCount(ItemPool.BLACK_CANDLE);
          int creatable = CreateItemRequest.getInstance(ItemPool.EVIL_SCROLL).getQuantityPossible();

          if (!KoLCharacter.canInteract() && ((onHand + creatable) < 1 || candles < 3)) {
            continue;
          } else if (limitMode.limitZone("Manor0")) {
            continue;
          } else if (Preferences.getBoolean("demonSummoned")) {
            cmd = "";
          } else {
            try {
              int num = Integer.parseInt(cmd.split(" ")[1]);
              if (Preferences.getString("demonName" + num).equals("")) {
                cmd = "";
              }
            } catch (Exception e) {
            }
          }
          // Existential Torment is 20 turns, but won't appear here as the effects are unknown
          duration = 30;
          usesRemaining = Preferences.getBoolean("demonSummoned") ? 0 : 1;
        } else if (cmd.startsWith("concert ")) {
          String side = Preferences.getString("sidequestArenaCompleted");
          boolean available = false;

          if (side.equals("none")) {
            continue;
          } else if (limitMode.limitZone("Island") || limitMode.limitZone("IsleWar")) {
            continue;
          } else if (side.equals("fratboy")) {
            available =
                cmd.contains("Elvish")
                    || cmd.contains("Winklered")
                    || cmd.contains("White-boy Angst");
          } else if (side.equals("hippy")) {
            available = cmd.contains("Moon") || cmd.contains("Dilated") || cmd.contains("Optimist");
          }

          if (!available) {
            continue;
          } else if (Preferences.getBoolean("concertVisited")) {
            cmd = "";
          }
          duration = 20;
          usesRemaining = Preferences.getBoolean("concertVisited") ? 0 : 1;
        } else if (cmd.startsWith("telescope ")) {
          if (limitMode.limitCampground()) {
            continue;
          } else if (Preferences.getInteger("telescopeUpgrades") == 0) {
            if (includeAll) {
              text = "( get a telescope )";
              cmd = "";
            } else continue;
          } else if (KoLCharacter.inBadMoon() || KoLCharacter.inNuclearAutumn()) {
            continue;
          } else if (Preferences.getBoolean("telescopeLookedHigh")) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = Preferences.getBoolean("telescopeLookedHigh") ? 0 : 1;
        } else if (cmd.startsWith("ballpit")) {
          if (!KoLCharacter.canInteract()) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (Preferences.getBoolean("_ballpit")) {
            cmd = "";
          }
          duration = 20;
          usesRemaining = Preferences.getBoolean("_ballpit") ? 0 : 1;
        } else if (cmd.startsWith("jukebox")) {
          if (!KoLCharacter.canInteract()) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (Preferences.getBoolean("_jukebox")) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = Preferences.getBoolean("_jukebox") ? 0 : 1;
        } else if (cmd.startsWith("pool ")) {
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "Pool Table")) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (!haveVipKey) {
            if (includeAll) {
              text = "( get access to the VIP lounge )";
              cmd = "";
            } else continue;
          } else if (Preferences.getInteger("_poolGames") >= 3) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = 3 - Preferences.getInteger("_poolGames");
        } else if (cmd.startsWith("shower ")) {
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "April Shower")) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (!haveVipKey) {
            if (includeAll) {
              text = "( get access to the VIP lounge )";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_aprilShower")) {
            cmd = "";
          }
          duration = 50;
          usesRemaining = Preferences.getBoolean("_aprilShower") ? 0 : 1;
        } else if (cmd.startsWith("swim ")) {
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(
              RestrictedItemType.CLAN_ITEMS, "Clan Swimming Pool")) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (!haveVipKey) {
            if (includeAll) {
              text = "( get access to the VIP lounge )";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_olympicSwimmingPool")) {
            cmd = "";
          }
          duration = 50;
          usesRemaining = Preferences.getBoolean("_olympicSwimmingPool") ? 0 : 1;
        } else if (cmd.startsWith("fortune ")) {
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(
              RestrictedItemType.CLAN_ITEMS, "Clan Love Tester")) {
            continue;
          } else if (limitMode.limitClan()) {
            continue;
          } else if (!haveVipKey) {
            if (includeAll) {
              text = "( get access to the VIP lounge )";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_clanFortuneBuffUsed")) {
            cmd = "";
          }
          duration = 100;
          usesRemaining = Preferences.getBoolean("_clanFortuneBuffUsed") ? 0 : 1;
        } else if (cmd.startsWith("mayosoak")) {
          AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "portable Mayo Clinic")) {
            continue;
          } else if (limitMode.limitCampground()) {
            continue;
          } else if (workshed == null || workshed.getItemId() != ItemPool.MAYO_CLINIC) {
            if (includeAll) {
              text = "( install portable Mayo Clinic )";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_mayoTankSoaked")) {
            cmd = "";
          }
          duration = 20;
          usesRemaining = Preferences.getBoolean("_mayoTankSoaked") ? 0 : 1;
        } else if (cmd.startsWith("barrelprayer")) {
          if (KoLCharacter.inBadMoon()) {
            continue;
          } else if (!StandardRequest.isAllowed(
              RestrictedItemType.ITEMS, "shrine to the Barrel god")) {
            continue;
          } else if (limitMode.limitZone("Dungeon Full of Dungeons")) {
            continue;
          } else if (!Preferences.getBoolean("barrelShrineUnlocked")) {
            if (includeAll) {
              text = "( install shrine to the Barrel god )";
              cmd = "";
            } else continue;
          } else if (Preferences.getBoolean("_barrelPrayer")) {
            cmd = "";
          }
          duration = 50;
          usesRemaining = Preferences.getBoolean("_barrelPrayer") ? 0 : 1;
        } else if (cmd.startsWith("styx ")) {
          if (!KoLCharacter.inBadMoon()) {
            continue;
          } else if (limitMode.limitZone("BadMoon")) {
            continue;
          } else if (Preferences.getBoolean("styxPixieVisited")) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = Preferences.getBoolean("styxPixieVisited") ? 0 : 1;
        } else if (cmd.startsWith("skate ")) {
          String status = Preferences.getString("skateParkStatus");
          int buff = SkateParkRequest.placeToBuff(cmd.substring(6));
          var data = SkateParkRequest.buffToData(buff);
          String buffPref = data.setting();
          String buffStatus = data.state();

          if (!status.equals(buffStatus)) {
            continue;
          } else if (limitMode.limitZone("The Sea")) {
            continue;
          } else if (Preferences.getBoolean(buffPref)) {
            cmd = "";
          }
          duration = 30;
          usesRemaining = Preferences.getBoolean(buffPref) ? 0 : 1;
        } else if (cmd.startsWith("gap ")) {
          AdventureResult pants = EquipmentManager.getEquipment(Slot.PANTS);
          if (InventoryManager.getAccessibleCount(ItemPool.GREAT_PANTS) == 0
              && (!KoLCharacter.inLegacyOfLoathing()
                  || InventoryManager.getAccessibleCount(ItemPool.REPLICA_GREAT_PANTS) == 0)) {
            if (includeAll) {
              text = "(acquire and equip Greatest American Pants for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getInteger("_gapBuffs") >= 5) {
            cmd = "";
          } else if (pants == null || (pants.getItemId() != ItemPool.GREAT_PANTS)) {
            text = "(equip Greatest American Pants for " + name + ")";
            cmd = "";
          }
          duration =
              switch (name) {
                case "Super Skill" -> 5;
                case "Super Structure", "Super Accuracy" -> 10;
                case "Super Vision", "Super Speed" -> 20;
                default -> duration;
              };
          usesRemaining = 5 - Preferences.getInteger("_gapBuffs");
        } else if (cmd.startsWith("spacegate")) {
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Spacegate access badge")) {
            continue;
          }
          if (KoLCharacter.isKingdomOfExploathing()) {
            continue;
          }
          boolean available =
              Preferences.getBoolean("spacegateAlways")
                  || Preferences.getBoolean("_spacegateToday");
          String number = cmd.substring(cmd.length() - 1);
          String setting = "spacegateVaccine" + number;
          boolean vaccineAvailable = Preferences.getBoolean(setting);
          if (!available || !vaccineAvailable) {
            if (includeAll) {
              text = "(unlock Spacegate and vaccine " + number + " for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getBoolean("_spacegateVaccine")) {
            cmd = "";
          }
          duration = 30;
          usesRemaining = Preferences.getBoolean("_spacegateVaccine") ? 0 : 1;
        } else if (cmd.startsWith("beach head ")) {
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Beach Comb")) {
            continue;
          }
          boolean available =
              (InventoryManager.getAccessibleCount(ItemPool.BEACH_COMB) > 0)
                  || (InventoryManager.getAccessibleCount(ItemPool.DRIFTWOOD_BEACH_COMB) > 0);
          BeachHead head = BeachManager.effectToBeachHead.get(name);
          Set<Integer> visited = BeachManager.getBeachHeadPreference("_beachHeadsUsed");
          boolean headAvailable = head != null && !visited.contains(head.id);
          if (!available) {
            if (includeAll) {
              text = "(acquire a Beach Comb or a driftwood beach comb for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (!headAvailable) {
            cmd = "";
          }
          duration = 50;
          usesRemaining = headAvailable ? 1 : 0;
        } else if (cmd.startsWith("daycare")) {
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Boxing Day care package")) {
            continue;
          }
          boolean available =
              Preferences.getBoolean("daycareOpen") || Preferences.getBoolean("_daycareToday");
          if (!available) {
            if (includeAll) {
              text = "(unlock Boxing Daycare and visit spa for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getBoolean("_daycareSpa")) {
            cmd = "";
          }
          duration = 100;
          usesRemaining = Preferences.getBoolean("_daycareSpa") ? 0 : 1;
        } else if (cmd.startsWith("play")) {
          if (InventoryManager.getAccessibleCount(ItemPool.DECK_OF_EVERY_CARD) == 0
              && (!KoLCharacter.inLegacyOfLoathing()
                  || InventoryManager.getAccessibleCount(ItemPool.REPLICA_DECK_OF_EVERY_CARD)
                      == 0)) {
            if (includeAll) {
              text = "(acquire Deck of Every Card for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getInteger("_deckCardsDrawn") > 10) {
            cmd = "";
          }
          duration = 20;
          usesRemaining = (15 - Preferences.getInteger("_deckCardsDrawn")) / 5;
        } else if (cmd.startsWith("grim")) {
          var fam = KoLCharacter.ownedFamiliar(FamiliarPool.GRIM_BROTHER);
          if (fam.isEmpty()) {
            if (limitMode.limitFamiliars()) {
              continue;
            } else if (includeAll) {
              text = "(get a Grim Brother familiar for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getBoolean("_grimBuff")) {
            cmd = "";
          }
          duration = 30;
          usesRemaining = Preferences.getBoolean("_grimBuff") ? 0 : 1;
        } else if (cmd.equals("witchess")) {
          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Witchess Set")) {
            continue;
          }
          if (!KoLConstants.campground.contains(ItemPool.get(ItemPool.WITCHESS_SET, 1))) {
            if (includeAll) {
              text = "(install Witchess Set for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getBoolean("_witchessBuff")) {
            cmd = "";
          } else if (Preferences.getInteger("puzzleChampBonus") != 20) {
            text = "(manually get " + name + ")";
            cmd = "";
          }
          duration = 25;
          usesRemaining = Preferences.getBoolean("_witchessBuff") ? 0 : 1;
        } else if (cmd.equals("crossstreams")) {
          if (InventoryManager.getAccessibleCount(ItemPool.PROTON_ACCELERATOR) == 0) {
            if (includeAll) {
              text = "(acquire protonic accelerator pack and crossstreams for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (Preferences.getBoolean("_streamsCrossed")) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = Preferences.getBoolean("_streamsCrossed") ? 0 : 1;
        } else if (cmd.startsWith("terminal enhance")) {
          int limit = 1;
          String chips = Preferences.getString("sourceTerminalChips");
          String files = Preferences.getString("sourceTerminalEnhanceKnown");
          if (chips.contains("CRAM")) limit++;
          if (chips.contains("SCRAM")) limit++;
          boolean haveTerminal =
              KoLConstants.campground.contains(ItemPool.get(ItemPool.SOURCE_TERMINAL, 1))
                  || KoLConstants.falloutShelter.contains(
                      ItemPool.get(ItemPool.SOURCE_TERMINAL, 1));
          if (!haveTerminal) {
            if (includeAll) {
              text = "(install Source Terminal for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else if (cmd.contains(name) && !files.contains(name)) {
            if (includeAll) {
              text = "(install Source terminal file: " + name + " for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else {
            if (Preferences.getInteger("_sourceTerminalEnhanceUses") >= limit) {
              cmd = "";
            }
          }
          duration =
              25
                  + (chips.contains("INGRAM") ? 25 : 0)
                  + 5 * Preferences.getInteger("sourceTerminalPram");
          usesRemaining = limit - Preferences.getInteger("_sourceTerminalEnhanceUses");
        } else if (cmd.startsWith("asdonmartin drive")) {
          boolean haveAsdonMartin = false;
          AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
          if (workshed != null) {
            haveAsdonMartin = workshed.getItemId() == ItemPool.ASDON_MARTIN;
          }
          if (!haveAsdonMartin) {
            if (includeAll) {
              text = "(install Asdon Martin for " + name + ")";
              cmd = "";
            } else {
              continue;
            }
          } else {
            if (CampgroundRequest.getFuel() < 37) {
              cmd = "";
            }
          }
          duration = 30;
          usesRemaining = CampgroundRequest.getFuel() / 37;
          fuelCost = 37;
        } else if (cmd.startsWith("campground vault3")) {
          if (!KoLCharacter.inNuclearAutumn()) {
            continue;
          }
          if (Preferences.getInteger("falloutShelterLevel") < 3) {
            continue;
          } else if (limitMode.limitCampground()) {
            continue;
          } else if (Preferences.getBoolean("_falloutShelterSpaUsed")) {
            cmd = "";
          }
          duration = 100;
          usesRemaining = Preferences.getBoolean("_falloutShelterSpaUsed") ? 0 : 1;
        } else if (cmd.startsWith("skeleton ")) {
          item = ItemPool.get(ItemPool.SKELETON, 1);
          duration = 30;
        } else if (cmd.startsWith("monorail ")) {
          if (Preferences.getBoolean("_lyleFavored")) {
            cmd = "";
          }
          duration = 10;
          usesRemaining = Preferences.getBoolean("_lyleFavored") ? 0 : 1;
        } else if (cmd.startsWith("toggle")) {
          if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.INTENSELY_INTERESTED))
              && !KoLConstants.activeEffects.contains(
                  EffectPool.get(EffectPool.SUPERFICIALLY_INTERESTED))) {
            continue;
          }
        }

        if (item != null) {
          String iname = item.getName();

          if (KoLCharacter.inBeecore() && KoLCharacter.hasBeeosity(iname)) {
            continue;
          }

          if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, iname)) {
            continue;
          }

          fullCost = ConsumablesDatabase.getFullness(iname);
          if (fullCost != 0
              && KoLCharacter.getFullness() + fullCost > KoLCharacter.getFullnessLimit()) {
            cmd = "";
          }
          drunkCost = ConsumablesDatabase.getInebriety(iname);
          if (drunkCost != 0
              && KoLCharacter.getInebriety() + drunkCost > KoLCharacter.getInebrietyLimit()) {
            cmd = "";
          }
          spleenCost = ConsumablesDatabase.getSpleenHit(iname);
          if (spleenCost != 0 && !cmd.contains("chew")) {
            RequestLogger.printLine(
                "(Note: extender for " + name + " is a spleen item that doesn't use 'chew')");
          }
          if (spleenCost != 0
              && KoLCharacter.getSpleenUse() + spleenCost > KoLCharacter.getSpleenLimit()) {
            cmd = "";
          }
          if (!ConsumablesDatabase.meetsLevelRequirement(iname)) {
            if (includeAll) {
              text = "level up & " + text;
              cmd = "";
            } else continue;
          }

          if (cmd.length() > 0) {
            int itemId = item.getItemId();
            // Outside Ronin/Hardcore, always show all purchasable
            EquipScope showScope = equipScope;
            if (KoLCharacter.canInteract()) {
              showScope = EquipScope.SPECULATE_ANY;
            }
            CheckedItem checkedItem = new CheckedItem(itemId, showScope, maxPrice, priceLevel);
            if (checkedItem.inventory > 0) {
            } else if (checkedItem.initial > 0) {
              String method =
                  InventoryManager.simRetrieveItem(item, equipScope == EquipScope.EQUIP_NOW, false);
              if (!method.equals("have")) {
                text = method + " & " + text;
              }
              if (method.equals("uncloset")) {
                cmd = "closet take 1 \u00B6" + itemId + ";" + cmd;
              }
              // Should be only hitting this after Ronin I think
              else if (method.equals("pull")) {
                cmd = "pull 1 \u00B6" + itemId + ";" + cmd;
              }
            } else if (checkedItem.creatable > 0) {
              text = "make & " + text;
              cmd = "make \u00B6" + itemId + ";" + cmd;
              price = ConcoctionPool.get(item).price;
              advCost = ConcoctionPool.get(item).getAdventuresNeeded(1);
            } else if (checkedItem.npcBuyable > 0) {
              text = "buy & " + text;
              cmd = "buy 1 \u00B6" + itemId + ";" + cmd;
              price = ConcoctionPool.get(item).price;
            } else if (checkedItem.pullable > 0) {
              text = "pull & " + text;
              cmd = "pull \u00B6" + itemId + ";" + cmd;
            } else if (checkedItem.mallBuyable > 0) {
              text = "acquire & " + text;
              if (priceLevel != PriceLevel.DONT_CHECK) {
                if (MallPriceDatabase.getPrice(itemId) > maxPrice * 2) {
                  continue;
                }

                // Depending on preference, either get historical mall price or look it up
                if (Preferences.getBoolean("maximizerCurrentMallPrices")) {
                  price = MallPriceManager.getMallPrice(itemId);
                } else {
                  price = MallPriceManager.getMallPrice(itemId, 7.0f);
                }
              }
            } else if (checkedItem.pullBuyable > 0) {
              text = "buy & pull & " + text;
              cmd = "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + cmd;
              if (priceLevel != PriceLevel.DONT_CHECK) {
                if (MallPriceDatabase.getPrice(itemId) > maxPrice * 2) {
                  continue;
                }

                // Depending on preference, either get historical mall price or look it up
                if (Preferences.getBoolean("maximizerCurrentMallPrices")) {
                  price = MallPriceManager.getMallPrice(itemId);
                } else {
                  price = MallPriceManager.getMallPrice(itemId, 7.0f);
                }
              }
            } else {
              continue;
            }

            if (price > maxPrice || price == -1) continue;
            if (priceLevel == PriceLevel.ALL
                && (checkedItem.initial > 0
                    || checkedItem.creatable > 0
                    || checkedItem.pullable > 0
                    || checkedItem.npcBuyable > 0)) {
              // Only check mall prices on tradeable items.
              if (ItemDatabase.isTradeable(itemId) && !ClanLoungeRequest.isSpeakeasyDrink(iname)) {
                if (MallPriceDatabase.getPrice(itemId) > maxPrice * 2) {
                  continue;
                }

                // Depending on preference, either get historical mall price or look it up
                if (Preferences.getBoolean("maximizerCurrentMallPrices")) {
                  price = MallPriceManager.getMallPrice(itemId);
                } else {
                  price = MallPriceManager.getMallPrice(itemId, 7.0f);
                }
              }
            }
          } else if (item.getCount(KoLConstants.inventory) == 0) {
            continue;
          }
          itemsRemaining = item.getCount(KoLConstants.inventory);
        }

        text = text + " (";
        if (advCost > 0) {
          if (Preferences.getBoolean("maximizerNoAdventures")) {
            continue;
          }
          text += advCost + " adv, ";
          if (advCost > KoLCharacter.getAdventuresLeft()) {
            cmd = "";
          }
        }
        if (fullCost != 0) {
          text += fullCost + " full, ";
          if (KoLCharacter.getFullness() + fullCost > KoLCharacter.getFullnessLimit()) {
            cmd = "";
          }
        }
        if (drunkCost != 0) {
          text += drunkCost + " drunk, ";
          if (KoLCharacter.getInebriety() + drunkCost > KoLCharacter.getInebrietyLimit()) {
            cmd = "";
          }
        }
        if (spleenCost != 0) {
          text += spleenCost + " spleen, ";
          if (KoLCharacter.getSpleenUse() + spleenCost > KoLCharacter.getSpleenLimit()) {
            cmd = "";
          }
        }
        if (mpCost > 0) {
          text += mpCost + " mp, ";
          // Don't ever grey out as we can recover MP
        }
        if (soulsauceCost > 0) {
          text += soulsauceCost + " soulsauce, ";
          if (soulsauceCost > KoLCharacter.getSoulsauce()) {
            cmd = "";
          }
        }
        if (thunderCost > 0) {
          text += thunderCost + " dB of thunder, ";
          if (thunderCost > KoLCharacter.getThunder()) {
            cmd = "";
          }
        } else if (rainCost > 0) {
          text += rainCost + " drops of rain, ";
          if (rainCost > KoLCharacter.getRain()) {
            cmd = "";
          }
        } else if (lightningCost > 0) {
          text += lightningCost + " bolts of lightning, ";
          if (lightningCost > KoLCharacter.getLightning()) {
            cmd = "";
          }
        }
        if (hpCost > 0) {
          text += hpCost + " hp, ";
          if (hpCost > KoLCharacter.getCurrentHP()) {
            cmd = "";
          }
        }
        if (fuelCost > 0) {
          text += fuelCost + " fuel, ";
        }
        if (price > 0) {
          text += KoLConstants.COMMA_FORMAT.format(price) + " meat, ";
          if (cmd.startsWith("buy using storage")) {
            if (price > KoLCharacter.getStorageMeat()) {
              cmd = "";
            }
          } else if (cmd.startsWith("acquire") || cmd.startsWith("make") || cmd.startsWith("buy")) {
            if (price > KoLCharacter.getAvailableMeat()) {
              cmd = "";
            }
          }
        }
        text += KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
        if (Preferences.getBoolean("verboseMaximizer")) {
          boolean show =
              duration > 0
                  || (usesRemaining > 0 && usesRemaining < Integer.MAX_VALUE)
                  || itemsRemaining + itemsCreatable > 0;
          int count = 0;
          if (show) {
            text += " [";
          }
          if (duration > 0) {
            if (duration == 999) {
              text += "intrinsic";
            } else if (duration == 1) {
              text += "1 adv duration";
            } else {
              text += duration + " advs duration";
            }
            count++;
          }
          if (usesRemaining > 0 && usesRemaining < Integer.MAX_VALUE) {
            if (count > 0) {
              text += ", ";
            }
            if (usesRemaining == 1) {
              text += "1 use remaining";
              count++;
            } else {
              text += usesRemaining + " uses remaining";
              count++;
            }
          }
          if (itemsRemaining > 0) {
            if (count > 0) {
              text += ", ";
            }
            text += itemsRemaining + " in inventory";
            count++;
          }
          if (itemsCreatable > 0) {
            if (count > 0) {
              text += ", ";
            }
            text += itemsCreatable + " creatable";
            count++;
          }
          if (show) {
            text += "]";
          }
        }
        if (orFlag) {
          text = "...or " + text;
        }
        Maximizer.boosts.add(new Boost(cmd, text, effect, hasEffect, item, delta, isSpecial));
        orFlag = true;
      }
    }

    if (Maximizer.boosts.size() == 0) {
      Maximizer.boosts.add(new Boost("", "(nothing useful found)", Slot.HAT, null, 0.0));
    }

    Maximizer.boosts.sort();
  }

  private static EquipScope emitSlot(
      Slot slot, EquipScope equipScope, int maxPrice, PriceLevel priceLevel, double current) {
    if (slot == Slot.FAMILIAR) { // Insert any familiar switch at this point
      FamiliarData fam = Maximizer.best.getFamiliar();
      if (!fam.equals(KoLCharacter.getFamiliar())) {
        MaximizerSpeculation spec = new MaximizerSpeculation();
        spec.setFamiliar(fam);
        double delta = spec.getScore() - current;
        String cmd, text;
        cmd = "familiar " + fam.getRace();
        text = cmd + " (" + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";

        Boost boost = new Boost(cmd, text, fam, delta);
        if (equipScope == EquipScope.EQUIP_NOW) { // called from CLI
          boost.execute(true);
          if (!KoLmafia.permitsContinue()) equipScope = EquipScope.SPECULATE_INVENTORY;
        } else {
          Maximizer.boosts.add(boost);
        }
      }
    }

    String slotname = slot.name;
    AdventureResult item = Maximizer.best.equipment.get(slot);
    int itemId = -1;
    FamiliarData enthroned = Maximizer.best.getEnthroned();
    FamiliarData bjorned = Maximizer.best.getBjorned();
    var modeables = Maximizer.best.getModeables();
    AdventureResult curr = EquipmentManager.getEquipment(slot);
    FamiliarData currEnthroned = KoLCharacter.getEnthroned();
    FamiliarData currBjorned = KoLCharacter.getBjorned();

    if (item == null || item.getItemId() == 0) {
      item = EquipmentRequest.UNEQUIP;
    } else {
      itemId = item.getItemId();
    }

    boolean changeEnthroned = itemId == ItemPool.HATSEAT && enthroned != currEnthroned;
    boolean changeBjorned = itemId == ItemPool.BUDDY_BJORN && bjorned != currBjorned;

    var modeable = Modeable.find(itemId);
    var changeModeable =
        modeable != null && !Objects.equals(modeables.get(modeable), modeable.getState());

    if (curr.equals(item)
        && !changeEnthroned
        && !changeBjorned
        && !changeModeable
        && !(itemId == ItemPool.BROKEN_CHAMPAGNE
            && Preferences.getInteger("garbageChampagneCharge") == 0
            && !Preferences.getBoolean("_garbageItemChanged"))
        && !(itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
            && Preferences.getInteger("garbageShirtCharge") == 0
            && !Preferences.getBoolean("_garbageItemChanged"))) {
      if (!SlotSet.SLOTS.contains(slot)
          || curr.equals(EquipmentRequest.UNEQUIP)
          || equipScope == EquipScope.EQUIP_NOW) {
        return equipScope;
      }
      Maximizer.boosts.add(
          new Boost("", "keep " + slotname + ": " + item.getName(), Slot.NONE, item, 0.0));
      return equipScope;
    }
    MaximizerSpeculation spec = new MaximizerSpeculation();
    spec.equip(slot, item);
    if (itemId == ItemPool.HATSEAT) {
      spec.setEnthroned(enthroned);
    } else if (itemId == ItemPool.BUDDY_BJORN) {
      spec.setBjorned(bjorned);
    } else if (modeable != null) {
      spec.setModeable(modeable, modeables.get(modeable));
    }

    double delta = spec.getScore() - current;

    String cmd, text;
    if (item.equals(EquipmentRequest.UNEQUIP)) {
      item = curr;
      cmd = "unequip " + slotname;
      text = cmd + " (" + curr.getName() + ", " + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
    } else {
      if (changeEnthroned) {
        cmd = "enthrone " + enthroned.getRace();
        text = cmd;
      } else if (changeBjorned) {
        cmd = "bjornify " + bjorned.getRace();
        text = cmd;
      } else if (changeModeable) {
        text = modeable.getCommand() + " " + modeables.get(modeable);
        cmd = text;
        if (modeable.mustEquipAfterChange())
          cmd += "; equip " + slotname + " \u00B6" + item.getItemId();
      } else {
        cmd = "equip " + slotname + " \u00B6" + item.getItemId();
        text = "equip " + slotname + " " + item.getName();
      }
      text = text + " (";

      CheckedItem checkedItem = new CheckedItem(itemId, equipScope, maxPrice, priceLevel);

      int price = 0;

      // How many have been needed so far to make this maximization set?
      // We need 1 + that number to equip this item, not just 1
      int count = 0;

      // If we're running from command line then execute them straight away,
      // so we have to count how much we've used in 'earlier' items
      // TODO: confirm this still works
      if (equipScope == EquipScope.EQUIP_NOW) {
        for (var piece : SlotSet.ALL_SLOTS) {
          if (piece.ordinal() >= slot.ordinal()) break;
          AdventureResult equipped = EquipmentManager.getEquipment(piece);
          if (equipped != null && item.getItemId() == equipped.getItemId()) {
            count++;
          }
        }
      } else {
        // Otherwise we iterate through the maximization set so far
        for (Boost boost : Maximizer.boosts) {
          if (item.equals(boost.getItem())) {
            count++;
          }
        }
      }

      // We might want to fold for a new Garbage item, even if we already have it, to reset it
      if ((itemId == ItemPool.BROKEN_CHAMPAGNE
              && Preferences.getInteger("garbageChampagneCharge") == 0)
          || (itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
                  && Preferences.getInteger("garbageShirtCharge") == 0)
              && !Preferences.getBoolean("_garbageItemChanged")) {
        if (checkedItem.initial > count) {
          text = "fold & " + text;
          cmd = "fold \u00B6" + item.getItemId() + ";" + cmd;
        }
        if (curr.equals(item)) {
          text = "unequip & " + text;
          cmd = "unequip " + slotname + ";" + cmd;
        }
      }

      // The "initial" quantity comes from InventoryManager.getAccessibleCount.
      // It can include inventory, closet, and storage.  However, anything that
      // is included should also be supported by retrieveItem(), so we don't need
      // to take any special action here.  Displaying the method that will be used
      // would still be useful, though.
      if (curr.equals(item)) {
      } else if (checkedItem.initial > count) {
        // This may look odd, but we need an item, not a checked item
        // The count of a checked item includes creatable, buyable, pullable etc.
        String method =
            InventoryManager.simRetrieveItem(
                ItemPool.get(item.getItemId(), count + 1),
                equipScope == EquipScope.EQUIP_NOW,
                false);
        if (!method.equals("have")) {
          text = method + " & " + text;
        }
        cmd =
            switch (method) {
              case "uncloset" -> "closet take 1 \u00B6" + item.getItemId() + ";" + cmd;
              case "unstash" -> "stash take 1 \u00B6" + item.getItemId() + ";" + cmd;
                // Should be only hitting this after Ronin I think
              case "pull" -> "pull 1 \u00B6" + item.getItemId() + ";" + cmd;
              default -> cmd;
            };
      } else if (checkedItem.creatable + checkedItem.initial > count) {
        text = "make & " + text;
        cmd = "make \u00B6" + item.getItemId() + ";" + cmd;
        price = ConcoctionPool.get(item).price;
      } else if (checkedItem.npcBuyable + checkedItem.initial > count) {
        text = "buy & " + text;
        cmd = "buy 1 \u00B6" + item.getItemId() + ";" + cmd;
        price = ConcoctionPool.get(item).price;
      } else if (checkedItem.foldable + checkedItem.initial > count) {
        // We assume that there is only one available fold item type of the right group.
        // Not always right, but will do for now.
        String method =
            InventoryManager.simRetrieveItem(ItemPool.get(checkedItem.foldItemId, count + 1));
        if (method.equals("have") || method.equals("remove")) {
          text = "fold & " + text;
          cmd = "fold \u00B6" + item.getItemId() + ";" + cmd;
        } else {
          text = method + " & fold & " + text;
          cmd =
              "acquire 1 \u00B6"
                  + checkedItem.foldItemId
                  + ";fold \u00B6"
                  + item.getItemId()
                  + ";"
                  + cmd;
        }
      } else if (checkedItem.pullable + checkedItem.initial > count) {
        text = "pull & " + text;
        cmd = "pull \u00B6" + item.getItemId() + ";" + cmd;
      } else if (checkedItem.pullfoldable + checkedItem.initial > count) {
        // We assume that there is only one available fold item type of the right group.
        // Not always right, but will do for now.
        text = "pull & fold & " + text;
        cmd =
            "pull 1 \u00B6"
                + checkedItem.foldItemId
                + ";fold \u00B6"
                + item.getItemId()
                + ";"
                + cmd;
      } else if (checkedItem.pullBuyable + checkedItem.initial > count) {
        text = "buy & pull & " + text;
        cmd = "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + cmd;
        if (priceLevel != PriceLevel.DONT_CHECK) {
          price = MallPriceManager.getMallPrice(itemId);
        }
      } else { // Mall buyable
        text = "acquire & " + text;
        if (priceLevel != PriceLevel.DONT_CHECK) {
          price = MallPriceManager.getMallPrice(itemId);
        }
      }

      if (price > 0) {
        text = text + KoLConstants.COMMA_FORMAT.format(price) + " meat, ";
      }
      text = text + KoLConstants.MODIFIER_FORMAT.format(delta) + ")";
    }

    Boost boost = new Boost(cmd, text, slot, item, delta, enthroned, bjorned, modeables);
    if (equipScope == EquipScope.EQUIP_NOW) { // called from CLI
      boost.execute(true);
      if (!KoLmafia.permitsContinue()) {
        equipScope = EquipScope.SPECULATE_INVENTORY;
        Maximizer.boosts.add(boost);
      }
    } else {
      Maximizer.boosts.add(boost);
    }
    return equipScope;
  }

  private static boolean excludedTCRSItem(int itemId) {
    return switch (itemId) {
      case ItemPool.DIETING_PILL ->
      // Doubles adventures and stats from next food.  Also
      // doubles fullness - which can be a surprise.
      true;
      default -> false;
    };
  }

  private static class Makeable {
    final String cmd;
    final String txt;
    final boolean canMake;
    final CheckedItem checkedItem;

    private Makeable(String cmd, String txt, boolean canMake, CheckedItem checkedItem) {
      this.cmd = cmd;
      this.txt = txt;
      this.canMake = canMake;
      this.checkedItem = checkedItem;
    }
  }

  private static Makeable getAbsorbable(
      int itemId, EquipScope equipScope, int maxPrice, PriceLevel priceLevel) {
    // Check if we have access to item
    CheckedItem checkedItem = new CheckedItem(itemId, equipScope, maxPrice, priceLevel);
    // We won't include unavailable items, as this just gets far too large
    String cmd, text;
    int price = 0;
    boolean canMake = true;
    AdventureResult item = ItemPool.get(itemId);
    cmd = "absorb \u00B6" + itemId;
    text = "absorb " + item.getName() + " (";
    if (checkedItem.inventory > 0) {
    } else if (checkedItem.initial > 0) {
      String method =
          InventoryManager.simRetrieveItem(item, equipScope == EquipScope.EQUIP_NOW, false);
      if (!method.equals("have")) {
        text = method + " & " + text;
      }
      if (method.equals("uncloset")) {
        cmd = "closet take 1 \u00B6" + itemId + ";" + cmd;
      }
      // Should be only hitting this after Ronin I think
      else if (method.equals("pull")) {
        cmd = "pull 1 \u00B6" + itemId + ";" + cmd;
      }
    } else if (checkedItem.creatable > 0) {
      text = "make & " + text;
      cmd = "make \u00B6" + itemId + ";" + cmd;
      price = ConcoctionPool.get(item).price;
    } else if (checkedItem.npcBuyable > 0) {
      text = "buy & " + text;
      cmd = "buy 1 \u00B6" + itemId + ";" + cmd;
      price = ConcoctionPool.get(item).price;
    } else if (checkedItem.pullable > 0) {
      text = "pull & " + text;
      cmd = "pull \u00B6" + itemId + ";" + cmd;
    } else if (checkedItem.mallBuyable > 0) {
      text = "acquire & " + text;
      if (priceLevel != PriceLevel.DONT_CHECK) {
        price = MallPriceManager.getMallPrice(itemId);
      }
    } else if (checkedItem.pullBuyable > 0) {
      text = "buy & pull & " + text;
      cmd = "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + cmd;
      if (priceLevel != PriceLevel.DONT_CHECK) {
        price = MallPriceManager.getMallPrice(itemId);
      }
    } else {
      canMake = false;
    }
    if (price > 0) {
      text = text + KoLConstants.COMMA_FORMAT.format(price) + " meat, ";
    }
    return new Makeable(cmd, text, canMake, checkedItem);
  }
}
