package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneResultPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.request.concoction.MayamRequest;
import net.sourceforge.kolmafia.session.BeachManager;
import net.sourceforge.kolmafia.session.BeachManager.BeachHead;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.textui.command.AlliedRadioCommand;
import net.sourceforge.kolmafia.textui.command.LoathingIdolCommand;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * Applies game-specific availability rules to one effect-source command.
 *
 * <p>The visible command-family switch replaces a long precedence-sensitive conditional chain in
 * {@link Maximizer}. Each named handler changes only source-specific state on {@link Plan}: it may
 * keep the source, disable execution while preserving an explanatory recommendation, or skip the
 * source entirely. Shared acquisition, affordability, and display behavior belongs to {@link
 * EffectSourcePlanFinalizer}.
 *
 * <p>This is intentionally not a generic rules engine. A switch keeps command precedence and the
 * complete supported vocabulary reviewable in one place, while named methods isolate the many
 * unrelated Kingdom of Loathing mechanics.
 */
final class EffectSourceDispatcher {
  /** Stable request and character context shared by all source handlers. */
  record Context(
      String name,
      AdventureResult effect,
      int effectId,
      boolean includeAll,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      LimitMode limitMode,
      boolean haveVipKey) {}

  /**
   * Mutable result of source-specific dispatch.
   *
   * <p>{@link #skip} means the recommendation is omitted. An empty {@link #command} means it
   * remains visible but cannot currently execute.
   */
  static final class Plan {
    String command;
    String text;
    AdventureResult item;
    long price;
    long mpCost;
    long usesRemaining;
    int adventureCost;
    int fullnessCost;
    int inebrietyCost;
    int spleenCost;
    int soulsauceCost;
    int thunderCost;
    int rainCost;
    int lightningCost;
    int fuelCost;
    int hpCost;
    int duration;
    int itemsRemaining;
    int itemsCreatable;
    boolean skip;

    private Plan(String command, String text) {
      this.command = command;
      this.text = text;
    }

    Plan skip() {
      this.skip = true;
      return this;
    }

    void disable() {
      this.command = "";
    }

    void disable(String text) {
      this.text = text;
      this.command = "";
    }
  }

  private EffectSourceDispatcher() {}

  static Plan dispatch(String command, String text, Context context) {
    Plan plan = new Plan(command, text);
    int separator = command.indexOf(' ');
    String family = command.substring(0, separator == -1 ? command.length() : separator);
    return switch (family) {
      case "use", "chew", "drink", "eat" ->
          command.startsWith(family + " ") ? consumable(plan, context, family) : plan;
      case "gong" -> command.startsWith("gong ") ? gong(plan) : plan;
      case "cast" -> command.startsWith("cast ") ? cast(plan, context) : plan;
      case "synthesize" -> command.startsWith("synthesize ") ? synthesize(plan, context) : plan;
      case "pillkeeper" -> pillKeeper(plan, context);
      case "cargo" -> command.startsWith("cargo effect ") ? cargo(plan, context) : plan;
      case "friars" -> command.startsWith("friars ") ? friars(plan, context) : plan;
      case "hatter" -> command.startsWith("hatter ") ? hatter(plan, context) : plan;
      case "mom" -> command.startsWith("mom ") ? mom(plan, context) : plan;
      case "summon" -> command.startsWith("summon ") ? summon(plan, context) : plan;
      case "concert" -> command.startsWith("concert ") ? concert(plan, context) : plan;
      case "telescope" -> command.startsWith("telescope ") ? telescope(plan, context) : plan;
      case "skeleton" -> command.startsWith("skeleton ") ? skeleton(plan) : plan;
      case "monorail" -> command.startsWith("monorail ") ? monorail(plan) : plan;
      case "toggle" -> toggle(plan);
      case "loathingidol" -> command.startsWith("loathingidol ") ? loathingIdol(plan) : plan;
      case "aprilband" -> command.startsWith("aprilband ") ? aprilBand(plan) : plan;
      case "mayam" -> command.startsWith("mayam ") ? mayam(plan) : plan;
      case "spacegate" -> spacegate(plan, context);
      case "beach" -> command.startsWith("beach head ") ? beachHead(plan, context) : plan;
      case "daycare" -> daycare(plan, context);
      case "play" -> deck(plan, context);
      case "grim" -> grim(plan, context);
      case "witchess" -> command.equals("witchess") ? witchess(plan, context) : plan;
      case "crossstreams" -> command.equals("crossstreams") ? crossstreams(plan, context) : plan;
      case "ballpit" -> clanDaily(plan, context, "_ballpit", 20);
      case "jukebox" -> clanDaily(plan, context, "_jukebox", 10);
      case "pool" ->
          command.startsWith("pool ")
              ? clanFacility(
                  plan, context, "Pool Table", Preferences.getInteger("_poolGames"), 3, 10)
              : plan;
      case "shower" ->
          command.startsWith("shower ")
              ? clanFacility(
                  plan,
                  context,
                  "April Shower",
                  Preferences.getBoolean("_aprilShower") ? 1 : 0,
                  1,
                  50)
              : plan;
      case "swim" ->
          command.startsWith("swim ")
              ? clanFacility(
                  plan,
                  context,
                  "Clan Swimming Pool",
                  Preferences.getBoolean("_olympicSwimmingPool") ? 1 : 0,
                  1,
                  50)
              : plan;
      case "fortune" ->
          command.startsWith("fortune ")
              ? clanFacility(
                  plan,
                  context,
                  "Clan Love Tester",
                  Preferences.getBoolean("_clanFortuneBuffUsed") ? 1 : 0,
                  1,
                  100)
              : plan;
      case "mayosoak" -> mayoSoak(plan, context);
      case "barrelprayer" -> barrelPrayer(plan, context);
      case "styx" -> command.startsWith("styx ") ? styx(plan, context) : plan;
      case "skate" -> command.startsWith("skate ") ? skate(plan, context) : plan;
      case "gap" -> command.startsWith("gap ") ? gap(plan, context) : plan;
      case "terminal" ->
          command.startsWith("terminal enhance") ? sourceTerminal(plan, context) : plan;
      case "asdonmartin" ->
          command.startsWith("asdonmartin drive") ? asdonMartin(plan, context) : plan;
      case "campground" ->
          command.startsWith("campground vault3") ? falloutShelter(plan, context) : plan;
      case "photobooth" ->
          command.startsWith("photobooth effect ")
              ? clanFacility(
                  plan, context, "Photo Booth", Preferences.getInteger("_photoBoothEffects"), 3, 50)
              : plan;
      case "campaway" -> command.equals("campaway cloud") ? campaway(plan) : plan;
      case "alliedradio" ->
          command.startsWith("alliedradio effect ") ? alliedRadio(plan, context) : plan;
      case "monkeypaw" -> command.startsWith("monkeypaw effect ") ? monkeyPaw(plan, context) : plan;
      case "genie" -> command.startsWith("genie effect ") ? genie(plan) : plan;
      default -> plan;
    };
  }

  private static Plan consumable(Plan plan, Context context, String family) {
    if (plan.command.contains(
            "use 1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?")
        && !MoodManager.canMasterTrivia()) {
      return plan.skip();
    }
    if (!KoLCharacter.canInteract() && plan.command.startsWith("use 1 box of sunshine")) {
      return plan.skip();
    }

    String itemName = plan.command.substring(plan.command.indexOf(' ') + 3).trim();
    Match match =
        switch (family) {
          case "use" -> Match.USE;
          case "chew" -> Match.SPLEEN;
          case "drink" -> Match.BOOZE;
          case "eat" -> Match.FOOD;
          default -> throw new IllegalStateException("Unsupported consumption command");
        };
    plan.item = ItemFinder.getFirstMatchingItem(itemName, false, match);

    if (plan.item != null) {
      int itemId = plan.item.getItemId();
      if (itemId == ItemPool.DIETING_PILL) return plan.skip();
      if (KoLCharacter.inGLover()
          && !KoLCharacter.hasGs(itemName)
          && !KoLConstants.restaurantItems.contains(itemName)
          && !KoLConstants.microbreweryItems.contains(itemName)
          && !KoLConstants.cafeItems.contains(itemName)) {
        return plan.skip();
      }
      if (itemId == ItemPool.VAMPIRE_VINTNER_WINE) {
        if (!InventoryManager.hasItem(itemId)
            || !Preferences.getString("vintnerWineEffect").equals(context.name())) {
          return plan.skip();
        }
        plan.duration = 12;
      }
      if (itemId == -1) {
        plan.item = plan.item.resolveBangPotion();
        itemId = plan.item.getItemId();
      }
      if (itemId == -1) return plan.skip();

      Modifiers itemModifiers = ModifierDatabase.getItemModifiers(itemId);
      if (itemModifiers != null) {
        var effects = itemModifiers.getStrings(StringModifier.EFFECT);
        int effectIndex = effects.indexOf(context.effect().getName());
        if (effectIndex != -1) {
          var durations = itemModifiers.getDoubles(DoubleModifier.EFFECT_DURATION);
          plan.duration =
              effectIndex >= durations.size() ? 0 : durations.get(effectIndex).intValue();
        }
      }
    }

    if (plan.item == null && ClanLoungeRequest.isHotDog(itemName)) {
      if (KoLCharacter.inBadMoon()
          || !StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, "Clan Hot Dog Stand")
          || KoLCharacter.isJarlsberg()
          || KoLCharacter.isZombieMaster()
          || context.limitMode().limitClan()) {
        return plan.skip();
      }
      if (!context.haveVipKey()) {
        if (!context.includeAll()) return plan.skip();
        plan.disable("( get access to the VIP lounge )");
      }
      plan.fullnessCost = ClanLoungeRequest.hotdogNameToFullness(itemName);
      if (plan.fullnessCost > 0
          && KoLCharacter.getFullness() + plan.fullnessCost > KoLCharacter.getStomachCapacity()) {
        return plan.skip();
      }
      if (ClanLoungeRequest.isFancyHotDog(itemName)
          && Preferences.getBoolean("_fancyHotDogEaten")) {
        return plan.skip();
      }
      Modifiers itemModifiers = ModifierDatabase.getModifiers(ModifierType.ITEM, itemName);
      if (itemModifiers != null) {
        plan.duration = (int) itemModifiers.getDouble(DoubleModifier.EFFECT_DURATION);
      }
      plan.usesRemaining = 1;
    } else if (plan.item == null && !plan.command.contains(",")) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(identify & " + plan.command + ")");
    } else if (plan.item != null) {
      plan.usesRemaining = UseItemRequest.maximumUses(plan.item.getItemId());
      if (plan.usesRemaining <= 0) return plan.skip();
    }
    return plan;
  }

  private static Plan gong(Plan plan) {
    plan.item = ItemPool.get(ItemPool.GONG, 1);
    plan.adventureCost = 3;
    plan.duration = 20;
    return plan;
  }

  private static Plan cast(Plan plan, Context context) {
    String skillName = UneffectRequest.effectToSkill(context.name());
    if (!StandardRequest.isAllowed(RestrictedItemType.SKILLS, skillName)) return plan.skip();

    int skillId = SkillDatabase.getSkillId(skillName);
    UseSkillRequest skill = UseSkillRequest.getUnmodifiedInstance(skillId);
    if (skill != null) plan.usesRemaining = skill.getMaximumCast();
    if (!KoLCharacter.hasSkill(skillId) || plan.usesRemaining == 0) {
      if (!context.includeAll()) return plan.skip();
      boolean isBuff = SkillDatabase.isBuff(skillId);
      plan.disable("(learn to " + plan.command + (isBuff ? ", or get it from a buffbot)" : ")"));
    }
    if (plan.command.contains(" ^ ")) {
      int requiredItem = UseSkillRequest.requiredItemForSkillEffect(skillId, context.effectId());
      if (requiredItem != -1 && !InventoryManager.equippedOrInInventory(requiredItem)) {
        return plan.skip();
      }
    }
    plan.mpCost = SkillDatabase.getMPConsumptionById(skillId);
    plan.adventureCost = SkillDatabase.getAdventureCost(skillId);
    plan.soulsauceCost = SkillDatabase.getSoulsauceCost(skillId);
    plan.thunderCost = SkillDatabase.getThunderCost(skillId);
    plan.rainCost = SkillDatabase.getRainCost(skillId);
    plan.lightningCost = SkillDatabase.getLightningCost(skillId);
    plan.hpCost = SkillDatabase.getHPCost(skillId);
    plan.duration = SkillDatabase.getEffectDuration(skillId);
    return plan;
  }

  private static Plan synthesize(Plan plan, Context context) {
    if (KoLCharacter.inGLover()
        || !StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Sweet Synthesis")) {
      return plan.skip();
    }
    if (!KoLCharacter.hasSkill(SkillPool.SWEET_SYNTHESIS)) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(learn the Sweet Synthesis skill)");
    }
    plan.usesRemaining = KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse();
    if (plan.usesRemaining < 1
        || CandyDatabase.synthesisPair(context.effectId()) == CandyDatabase.NO_PAIR) {
      plan.disable();
    }
    plan.duration = 30;
    plan.spleenCost = 1;
    return plan;
  }

  private static Plan pillKeeper(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Eight Days a Week Pill Keeper")) {
      return plan.skip();
    }
    if (!InventoryManager.hasItem(ItemPool.PILL_KEEPER)) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(get an Eight Days a Week Pill Keeper)");
    }
    if (Preferences.getBoolean("_freePillKeeperUsed")) {
      plan.usesRemaining = KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse();
      if (plan.usesRemaining < 3) plan.disable();
      plan.spleenCost = 3;
    }
    plan.duration = 30;
    return plan;
  }

  private static Plan cargo(Plan plan, Context context) {
    if (!KoLCharacter.inLegacyOfLoathing()
        && !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Cargo Cultist Shorts")) {
      return plan.skip();
    }
    if (!InventoryManager.hasItem(ItemPool.CARGO_CULTIST_SHORTS)
        && !(KoLCharacter.inLegacyOfLoathing()
            && InventoryManager.hasItem(ItemPool.REPLICA_CARGO_CULTIST_SHORTS))) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(acquire a pair of Cargo Cultist Shorts for " + context.name() + ")");
    } else if (Preferences.getBoolean("_cargoPocketEmptied")) {
      plan.disable();
    } else {
      Set<OneResultPocket> pockets = PocketDatabase.effectPockets.get(context.name());
      List<Pocket> sorted = PocketDatabase.sortResults(context.name(), pockets);
      Pocket pocket = PocketDatabase.firstUnpickedPocket(sorted);
      if (pocket == null) {
        plan.disable();
      } else {
        plan.duration = ((OneResultPocket) pocket).getCount(context.name());
      }
    }
    plan.usesRemaining = Preferences.getBoolean("_cargoPocketEmptied") ? 0 : 1;
    return plan;
  }

  private static Plan friars(Plan plan, Context context) {
    if (Preferences.getInteger("lastFriarCeremonyAscension")
            < Preferences.getInteger("knownAscensions")
        || context.limitMode().limitZone("Friars")) {
      return plan.skip();
    }
    if (Preferences.getBoolean("friarsBlessingReceived")) plan.disable();
    plan.duration = 20;
    plan.usesRemaining = Preferences.getBoolean("friarsBlessingReceived") ? 0 : 1;
    return plan;
  }

  private static Plan hatter(Plan plan, Context context) {
    boolean haveEffect =
        KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE));
    boolean havePotion = InventoryManager.hasItem(ItemPool.DRINK_ME_POTION);
    if ((!havePotion && !haveEffect)
        || !RabbitHoleManager.hatLengthAvailable(
            StringUtilities.parseInt(plan.command.substring(7)))
        || context.limitMode().limitZone("Rabbit Hole")) {
      return plan.skip();
    }
    if (Preferences.getBoolean("_madTeaParty")) plan.disable();
    plan.duration = 30;
    plan.usesRemaining = Preferences.getBoolean("_madTeaParty") ? 0 : 1;
    return plan;
  }

  private static Plan mom(Plan plan, Context context) {
    if (!QuestDatabase.isQuestFinished(Quest.SEA_MONKEES)
        || context.limitMode().limitZone("The Sea")) {
      return plan.skip();
    }
    if (Preferences.getBoolean("_momFoodReceived")) plan.disable();
    plan.duration = 50;
    plan.usesRemaining = Preferences.getBoolean("_momFoodReceived") ? 0 : 1;
    return plan;
  }

  private static Plan summon(Plan plan, Context context) {
    if (!QuestDatabase.isQuestFinished(Quest.MANOR)) return plan.skip();
    int onHand = InventoryManager.getAccessibleCount(ItemPool.EVIL_SCROLL);
    int candles = InventoryManager.getAccessibleCount(ItemPool.BLACK_CANDLE);
    int creatable = CreateItemRequest.getInstance(ItemPool.EVIL_SCROLL).getQuantityPossible();
    if ((!KoLCharacter.canInteract() && (onHand + creatable < 1 || candles < 3))
        || context.limitMode().limitZone("Manor0")) {
      return plan.skip();
    }
    if (Preferences.getBoolean("demonSummoned")) {
      plan.disable();
    } else {
      try {
        int number = Integer.parseInt(plan.command.split(" ")[1]);
        if (Preferences.getString("demonName" + number).isEmpty()) plan.disable();
      } catch (Exception ignored) {
      }
    }
    plan.duration = 30;
    plan.usesRemaining = Preferences.getBoolean("demonSummoned") ? 0 : 1;
    return plan;
  }

  private static Plan concert(Plan plan, Context context) {
    String side = Preferences.getString("sidequestArenaCompleted");
    if (side.equals("none")
        || context.limitMode().limitZone("Island")
        || context.limitMode().limitZone("IsleWar")) {
      return plan.skip();
    }
    boolean available =
        switch (side) {
          case "fratboy" ->
              plan.command.contains("Elvish")
                  || plan.command.contains("Winklered")
                  || plan.command.contains("White-boy Angst");
          case "hippy" ->
              plan.command.contains("Moon")
                  || plan.command.contains("Dilated")
                  || plan.command.contains("Optimist");
          default -> false;
        };
    if (!available) return plan.skip();
    if (Preferences.getBoolean("concertVisited")) plan.disable();
    plan.duration = 20;
    plan.usesRemaining = Preferences.getBoolean("concertVisited") ? 0 : 1;
    return plan;
  }

  private static Plan telescope(Plan plan, Context context) {
    if (!CampgroundRequest.haveCampground()) return plan.skip();
    if (Preferences.getInteger("telescopeUpgrades") == 0) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("( get a telescope )");
    } else if (KoLCharacter.inBadMoon()) {
      return plan.skip();
    } else if (Preferences.getBoolean("telescopeLookedHigh")) {
      plan.disable();
    }
    plan.duration = 10;
    plan.usesRemaining = Preferences.getBoolean("telescopeLookedHigh") ? 0 : 1;
    return plan;
  }

  private static Plan skeleton(Plan plan) {
    plan.item = ItemPool.get(ItemPool.SKELETON, 1);
    plan.duration = 30;
    return plan;
  }

  private static Plan monorail(Plan plan) {
    if (Preferences.getBoolean("_lyleFavored")) plan.command = "";
    plan.duration = 10;
    plan.usesRemaining = Preferences.getBoolean("_lyleFavored") ? 0 : 1;
    return plan;
  }

  private static Plan toggle(Plan plan) {
    return !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.INTENSELY_INTERESTED))
            && !KoLConstants.activeEffects.contains(
                EffectPool.get(EffectPool.SUPERFICIALLY_INTERESTED))
        ? plan.skip()
        : plan;
  }

  private static Plan loathingIdol(Plan plan) {
    int microphone = LoathingIdolCommand.getUsableMicrophone();
    plan.item = ItemPool.get(microphone == -1 ? ItemPool.LOATHING_IDOL_MICROPHONE : microphone, 1);
    plan.duration = 30;
    return plan;
  }

  private static Plan aprilBand(Plan plan) {
    plan.item = ItemPool.get(ItemPool.APRILING_BAND_HELMET, 1);
    return plan;
  }

  private static Plan mayam(Plan plan) {
    plan.item = ItemPool.get(ItemPool.MAYAM_CALENDAR, 1);
    if (plan.command.startsWith("mayam resonance ")
        && !MayamRequest.availableResonances().contains(plan.command.substring(16))) {
      plan.disable();
    }
    return plan;
  }

  private static Plan spacegate(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Spacegate access badge")
        || KoLCharacter.isKingdomOfExploathing()) {
      return plan.skip();
    }
    boolean available =
        Preferences.getBoolean("spacegateAlways") || Preferences.getBoolean("_spacegateToday");
    String number = plan.command.substring(plan.command.length() - 1);
    boolean vaccineAvailable = Preferences.getBoolean("spacegateVaccine" + number);
    if (!available || !vaccineAvailable) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(unlock Spacegate and vaccine " + number + " for " + context.name() + ")");
    } else if (Preferences.getBoolean("_spacegateVaccine")) {
      plan.disable();
    }
    plan.duration = 30;
    plan.usesRemaining = Preferences.getBoolean("_spacegateVaccine") ? 0 : 1;
    return plan;
  }

  private static Plan beachHead(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Beach Comb")) return plan.skip();
    boolean available =
        InventoryManager.getAccessibleCount(ItemPool.BEACH_COMB) > 0
            || InventoryManager.getAccessibleCount(ItemPool.DRIFTWOOD_BEACH_COMB) > 0;
    BeachHead head = BeachManager.effectToBeachHead.get(context.name());
    Set<Integer> visited = BeachManager.getBeachHeadPreference("_beachHeadsUsed");
    boolean headAvailable = head != null && !visited.contains(head.id);
    if (!available) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(acquire a Beach Comb or a driftwood beach comb for " + context.name() + ")");
    } else if (!headAvailable) {
      plan.disable();
    }
    plan.duration = 50;
    plan.usesRemaining = headAvailable ? 1 : 0;
    return plan;
  }

  private static Plan daycare(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Boxing Day care package")) {
      return plan.skip();
    }
    boolean available =
        Preferences.getBoolean("daycareOpen") || Preferences.getBoolean("_daycareToday");
    if (!available) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(unlock Boxing Daycare and visit spa for " + context.name() + ")");
    } else if (Preferences.getBoolean("_daycareSpa")) {
      plan.disable();
    }
    plan.duration = 100;
    plan.usesRemaining = Preferences.getBoolean("_daycareSpa") ? 0 : 1;
    return plan;
  }

  private static Plan deck(Plan plan, Context context) {
    if (InventoryManager.getAccessibleCount(ItemPool.DECK_OF_EVERY_CARD) == 0
        && (!KoLCharacter.inLegacyOfLoathing()
            || InventoryManager.getAccessibleCount(ItemPool.REPLICA_DECK_OF_EVERY_CARD) == 0)) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(acquire Deck of Every Card for " + context.name() + ")");
    } else if (Preferences.getInteger("_deckCardsDrawn") > 10) {
      plan.disable();
    }
    plan.duration = 20;
    plan.usesRemaining = (15 - Preferences.getInteger("_deckCardsDrawn")) / 5;
    return plan;
  }

  private static Plan grim(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, "Grim Brother")) {
      return plan.skip();
    }
    var familiar = KoLCharacter.ownedFamiliar(FamiliarPool.GRIM_BROTHER);
    if (familiar.isEmpty()) {
      if (context.limitMode().limitFamiliars() || !context.includeAll()) return plan.skip();
      plan.disable("(get a Grim Brother familiar for " + context.name() + ")");
    } else if (Preferences.getBoolean("_grimBuff")) {
      plan.disable();
    }
    plan.duration = 30;
    plan.usesRemaining = Preferences.getBoolean("_grimBuff") ? 0 : 1;
    return plan;
  }

  private static Plan witchess(Plan plan, Context context) {
    if (!(KoLCharacter.inLegacyOfLoathing()
            && Preferences.getBoolean("replicaWitchessSetAvailable"))
        && !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Witchess Set")) {
      return plan.skip();
    }
    if (!KoLConstants.campground.contains(ItemPool.get(ItemPool.WITCHESS_SET, 1))) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(install Witchess Set for " + context.name() + ")");
    } else if (Preferences.getBoolean("_witchessBuff")) {
      plan.disable();
    } else if (Preferences.getInteger("puzzleChampBonus") != 20) {
      plan.disable("(manually get " + context.name() + ")");
    }
    plan.duration = 25;
    plan.usesRemaining = Preferences.getBoolean("_witchessBuff") ? 0 : 1;
    return plan;
  }

  private static Plan crossstreams(Plan plan, Context context) {
    if (InventoryManager.getAccessibleCount(ItemPool.PROTON_ACCELERATOR) == 0) {
      if (!context.includeAll()) return plan.skip();
      plan.disable(
          "(acquire protonic accelerator pack and crossstreams for " + context.name() + ")");
    } else if (Preferences.getBoolean("_streamsCrossed")) {
      plan.disable();
    }
    plan.duration = 10;
    plan.usesRemaining = Preferences.getBoolean("_streamsCrossed") ? 0 : 1;
    return plan;
  }

  private static Plan clanDaily(Plan plan, Context context, String preference, int duration) {
    if (!KoLCharacter.canInteract() || context.limitMode().limitClan()) return plan.skip();
    if (Preferences.getBoolean(preference)) plan.disable();
    plan.duration = duration;
    plan.usesRemaining = Preferences.getBoolean(preference) ? 0 : 1;
    return plan;
  }

  private static Plan clanFacility(
      Plan plan, Context context, String facility, int uses, int limit, int duration) {
    if (KoLCharacter.inBadMoon()
        || !StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, facility)
        || context.limitMode().limitClan()) {
      return plan.skip();
    }
    if (!context.haveVipKey()) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("( get access to the VIP lounge )");
    } else if (uses >= limit) {
      plan.disable();
    }
    plan.duration = duration;
    plan.usesRemaining = limit - uses;
    return plan;
  }

  private static Plan mayoSoak(Plan plan, Context context) {
    AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
    if (KoLCharacter.inBadMoon()
        || !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "portable Mayo Clinic")
        || context.limitMode().limitCampground()) {
      return plan.skip();
    }
    if (workshed == null || workshed.getItemId() != ItemPool.MAYO_CLINIC) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("( install portable Mayo Clinic )");
    } else if (Preferences.getBoolean("_mayoTankSoaked")) {
      plan.disable();
    }
    plan.duration = 20;
    plan.usesRemaining = Preferences.getBoolean("_mayoTankSoaked") ? 0 : 1;
    return plan;
  }

  private static Plan barrelPrayer(Plan plan, Context context) {
    if (KoLCharacter.inBadMoon()
        || !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "shrine to the Barrel god")
        || context.limitMode().limitZone("Dungeon Full of Dungeons")) {
      return plan.skip();
    }
    if (!Preferences.getBoolean("barrelShrineUnlocked")) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("( install shrine to the Barrel god )");
    } else if (Preferences.getBoolean("_barrelPrayer")) {
      plan.disable();
    }
    plan.duration = 50;
    plan.usesRemaining = Preferences.getBoolean("_barrelPrayer") ? 0 : 1;
    return plan;
  }

  private static Plan styx(Plan plan, Context context) {
    if (!KoLCharacter.inBadMoon() || context.limitMode().limitZone("BadMoon")) {
      return plan.skip();
    }
    if (Preferences.getBoolean("styxPixieVisited")) plan.disable();
    plan.duration = 10;
    plan.usesRemaining = Preferences.getBoolean("styxPixieVisited") ? 0 : 1;
    return plan;
  }

  private static Plan skate(Plan plan, Context context) {
    int buff = SkateParkRequest.placeToBuff(plan.command.substring(6));
    var data = SkateParkRequest.buffToData(buff);
    String preference = data.setting();
    if (!Preferences.getString("skateParkStatus").equals(data.state())
        || context.limitMode().limitZone("The Sea")) {
      return plan.skip();
    }
    if (Preferences.getBoolean(preference)) plan.disable();
    plan.duration = 30;
    plan.usesRemaining = Preferences.getBoolean(preference) ? 0 : 1;
    return plan;
  }

  private static Plan gap(Plan plan, Context context) {
    AdventureResult pants = EquipmentManager.getEquipment(Slot.PANTS);
    if (InventoryManager.getAccessibleCount(ItemPool.GREAT_PANTS) == 0
        && (!KoLCharacter.inLegacyOfLoathing()
            || InventoryManager.getAccessibleCount(ItemPool.REPLICA_GREAT_PANTS) == 0)) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(acquire and equip Greatest American Pants for " + context.name() + ")");
    } else if (Preferences.getInteger("_gapBuffs") >= 5) {
      plan.disable();
    } else if (pants == null
        || (pants.getItemId() != ItemPool.GREAT_PANTS
            && pants.getItemId() != ItemPool.REPLICA_GREAT_PANTS)) {
      plan.disable("(equip Greatest American Pants for " + context.name() + ")");
    }
    plan.duration =
        switch (context.name()) {
          case "Super Skill" -> 5;
          case "Super Structure", "Super Accuracy" -> 10;
          case "Super Vision", "Super Speed" -> 20;
          default -> plan.duration;
        };
    plan.usesRemaining = 5 - Preferences.getInteger("_gapBuffs");
    return plan;
  }

  private static Plan sourceTerminal(Plan plan, Context context) {
    int limit = 1;
    String chips = Preferences.getString("sourceTerminalChips");
    String files = Preferences.getString("sourceTerminalEnhanceKnown");
    if (chips.contains("CRAM")) limit++;
    if (chips.contains("SCRAM")) limit++;
    boolean haveTerminal =
        KoLConstants.campground.contains(ItemPool.get(ItemPool.SOURCE_TERMINAL, 1))
            || KoLConstants.falloutShelter.contains(ItemPool.get(ItemPool.SOURCE_TERMINAL, 1));
    if (!haveTerminal) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(install Source Terminal for " + context.name() + ")");
    } else if (plan.command.contains(context.name()) && !files.contains(context.name())) {
      if (!context.includeAll()) return plan.skip();
      plan.disable(
          "(install Source terminal file: " + context.name() + " for " + context.name() + ")");
    } else if (Preferences.getInteger("_sourceTerminalEnhanceUses") >= limit) {
      plan.disable();
    }
    plan.duration =
        25 + (chips.contains("INGRAM") ? 25 : 0) + 5 * Preferences.getInteger("sourceTerminalPram");
    plan.usesRemaining = limit - Preferences.getInteger("_sourceTerminalEnhanceUses");
    return plan;
  }

  private static Plan asdonMartin(Plan plan, Context context) {
    AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
    boolean available = workshed != null && workshed.getItemId() == ItemPool.ASDON_MARTIN;
    if (!available) {
      if (!context.includeAll()) return plan.skip();
      plan.disable("(install Asdon Martin for " + context.name() + ")");
    } else if (CampgroundRequest.getFuel() < 37) {
      plan.disable();
    }
    plan.duration = 30;
    plan.usesRemaining = CampgroundRequest.getFuel() / 37;
    plan.fuelCost = 37;
    return plan;
  }

  private static Plan falloutShelter(Plan plan, Context context) {
    if (!KoLCharacter.inNuclearAutumn()
        || Preferences.getInteger("falloutShelterLevel") < 3
        || context.limitMode().limitCampground()) {
      return plan.skip();
    }
    if (Preferences.getBoolean("_falloutShelterSpaUsed")) plan.disable();
    plan.duration = 100;
    plan.usesRemaining = Preferences.getBoolean("_falloutShelterSpaUsed") ? 0 : 1;
    return plan;
  }

  private static Plan campaway(Plan plan) {
    if (!CampAwayRequest.campAwayTentAvailable()) return plan.skip();
    int used = Preferences.getInteger("_campAwayCloudBuffs");
    if (used > 0) plan.command = "";
    plan.duration = 100;
    plan.usesRemaining = 1 - used;
    return plan;
  }

  private static Plan alliedRadio(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Allied Radio Backpack")) {
      return plan.skip();
    }
    if (!(InventoryManager.equippedOrInInventory(ItemPool.ALLIED_RADIO_BACKPACK)
        && Preferences.getInteger("_alliedRadioDropsUsed") < 3)) {
      plan.item = ItemPool.get(ItemPool.HANDHELD_ALLIED_RADIO, 1);
    }
    if (context.effectId() == EffectPool.WILDSUN_BOON) {
      if (Preferences.getBoolean("_alliedRadioWildsunBoon")) plan.command = "";
      plan.duration = 100;
    } else if (context.effectId() == EffectPool.ELLIPSOIDTINED) {
      plan.duration = 30;
    } else if (context.effectId() == EffectPool.MATERIEL_INTEL) {
      if (Preferences.getBoolean("_alliedRadioMaterielIntel")) plan.command = "";
      plan.duration = 10;
    }
    plan.usesRemaining = AlliedRadioCommand.usesRemaining();
    return plan;
  }

  private static Plan monkeyPaw(Plan plan, Context context) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "cursed monkey's paw")) {
      return plan.skip();
    }
    if (!InventoryManager.equippedOrInInventory(ItemPool.CURSED_MONKEY_PAW)) {
      if (!context.includeAll()) return plan.skip();
      plan.text = "( acquire a cursed monkey's paw )";
      plan.command = "";
    }
    int used = Preferences.getInteger("_monkeyPawWishesUsed");
    if (used >= 5) plan.command = "";
    plan.duration = 30;
    plan.usesRemaining = 5 - used;
    return plan;
  }

  private static Plan genie(Plan plan) {
    if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "pocket wish")) {
      return plan.skip();
    }
    if (InventoryManager.getCount(ItemPool.GENIE_BOTTLE) > 0
        && Preferences.getInteger("_genieWishesUsed") < 3) {
      plan.item = ItemPool.get(ItemPool.GENIE_BOTTLE);
    } else if (KoLCharacter.inLegacyOfLoathing()
        && InventoryManager.getCount(ItemPool.REPLICA_GENIE_BOTTLE) > 0
        && Preferences.getInteger("_genieWishesUsed") < 3) {
      plan.item = ItemPool.get(ItemPool.REPLICA_GENIE_BOTTLE, 1);
    } else {
      plan.item = ItemPool.get(ItemPool.POCKET_WISH, 1);
    }
    plan.duration = 20;
    plan.usesRemaining =
        (InventoryManager.getCount(ItemPool.GENIE_BOTTLE) > 0
                    || InventoryManager.getCount(ItemPool.REPLICA_GENIE_BOTTLE) > 0
                ? 3 - Preferences.getInteger("_genieWishesUsed")
                : 0)
            + InventoryManager.getCount(ItemPool.POCKET_WISH);
    return plan;
  }
}
