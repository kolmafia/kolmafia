package internal.helpers;

import static org.mockito.Mockito.mockStatic;

import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.net.http.HttpClient;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DateTimeManager;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.ChoiceControl;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.mockito.Mockito;

public class Player {

  /**
   * Ensures that the character stats are sufficient to equip an item
   *
   * @param itemName The item of interest
   * @return Restores the stat to the old value
   */
  public static Cleanups withStatsRequiredForEquipment(final String itemName) {
    int itemId = ItemDatabase.getItemId(itemName, 1, false);
    return withStatsRequiredForEquipment(itemId);
  }

  /**
   * Ensures that the character stats are sufficient to equip an item
   *
   * @param item The item of interest
   * @return Restores the stat to the old value
   */
  public static Cleanups withStatsRequiredForEquipment(AdventureResult item) {
    return withStatsRequiredForEquipment(item.getItemId());
  }

  /**
   * Ensures that the character stats are sufficient to equip an item
   *
   * @param itemId The item of interest
   * @return Restores the stat to the old value
   */
  public static Cleanups withStatsRequiredForEquipment(final int itemId) {
    String requirement = EquipmentDatabase.getEquipRequirement(itemId);
    EquipmentRequirement req = new EquipmentRequirement(requirement);

    return req.isMuscle()
        ? withMuscleAtLeast(req.getAmount())
        : req.isMysticality()
            ? withMysticalityAtLeast(req.getAmount())
            : req.isMoxie() ? withMoxieAtLeast(req.getAmount()) : new Cleanups();
  }

  /**
   * Equip the given slot with the given item
   *
   * @param slot Slot to equip
   * @param itemName Item to equip to slot
   * @return Restores item previously equipped to slot
   */
  public static Cleanups withEquipped(final int slot, final String itemName) {
    return withEquipped(slot, AdventureResult.tallyItem(itemName));
  }

  /**
   * Equip the given slot with the given item
   *
   * @param slot Slot to equip
   * @param itemId Item to equip to slot
   * @return Restores item previously equipped to slot
   */
  public static Cleanups withEquipped(final int slot, final int itemId) {
    return withEquipped(slot, ItemPool.get(itemId));
  }

  /**
   * Unequips the given slot
   *
   * @param slot Slot to unequip
   * @return Restores item previously equipped to slot
   */
  public static Cleanups withUnequipped(final int slot) {
    return withEquipped(slot, (String) null);
  }

  /**
   * Equip the given slot with the given item
   *
   * @param slot Slot to equip
   * @param item Item to equip to slot
   * @return Restores item previously equipped to slot
   */
  public static Cleanups withEquipped(final int slot, final AdventureResult item) {
    var cleanups = new Cleanups();
    // Do this first so that Equipment lists and outfits will update appropriately
    cleanups.add(withStatsRequiredForEquipment(item));

    var old = EquipmentManager.getEquipment(slot);
    EquipmentManager.setEquipment(slot, item.getItemId() == -1 ? EquipmentRequest.UNEQUIP : item);
    EquipmentManager.updateNormalOutfits();
    KoLCharacter.recalculateAdjustments();
    // may have access to a new item = may have access to a new concoction
    ConcoctionDatabase.refreshConcoctions();
    cleanups.add(
        new Cleanups(
            () -> {
              EquipmentManager.setEquipment(slot, old);
              EquipmentManager.updateNormalOutfits();
              KoLCharacter.recalculateAdjustments();
              ConcoctionDatabase.refreshConcoctions();
            }));
    return cleanups;
  }

  /**
   * Puts the given item into the player's inventory
   *
   * @param itemName Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItem(final String itemName) {
    return withItem(itemName, 1);
  }

  /**
   * Puts an amount of the given item into the player's inventory
   *
   * @param itemName Item to give
   * @param count Quantity of item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItem(final String itemName, final int count) {
    int itemId = ItemDatabase.getItemId(itemName, count, false);
    return withItem(ItemPool.get(itemId, count));
  }

  /**
   * Puts the given item into the player's inventory
   *
   * @param itemId Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItem(final int itemId) {
    return withItem(itemId, 1);
  }

  /**
   * Puts an amount of the given item into the player's inventory
   *
   * @param itemId Item to give
   * @param count Quantity of item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItem(final int itemId, final int count) {
    return withItem(ItemPool.get(itemId, count));
  }

  /**
   * Puts the given item into the player's inventory
   *
   * @param item Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItem(final AdventureResult item) {
    return addToList(item, KoLConstants.inventory);
  }

  /**
   * Puts the given item into the player's closet
   *
   * @param itemName Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInCloset(final String itemName) {
    return withItemInCloset(itemName, 1);
  }

  /**
   * Puts an amount of the given item into the player's closet
   *
   * @param itemName Item to give
   * @param count Quantity to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInCloset(final String itemName, final int count) {
    int itemId = ItemDatabase.getItemId(itemName, count, false);
    AdventureResult item = ItemPool.get(itemId, count);
    return addToList(item, KoLConstants.closet);
  }

  /**
   * Puts the given item into the player's storage
   *
   * @param itemName Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStorage(final String itemName) {
    return withItemInStorage(itemName, 1);
  }

  /**
   * Puts an amount of the given item into the player's storage
   *
   * @param itemName Item to give
   * @param count Quantity to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStorage(final String itemName, final int count) {
    int itemId = ItemDatabase.getItemId(itemName, count, false);
    return withItemInStorage(itemId, count);
  }

  /**
   * Puts the given item into the player's storage
   *
   * @param itemId Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStorage(final int itemId) {
    return withItemInStorage(itemId, 1);
  }

  /**
   * Puts an amount of the given item into the player's storage
   *
   * @param itemId Item to give
   * @param count Quantity to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStorage(final int itemId, final int count) {
    AdventureResult item = ItemPool.get(itemId, count);
    return addToList(item, KoLConstants.storage);
  }

  /**
   * Puts the given item into the player's clan stash
   *
   * @param itemName Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStash(final String itemName) {
    return withItemInStash(itemName, 1);
  }

  /**
   * Puts an amount of the given item into the player's clan stash
   *
   * @param itemName Item to give
   * @param count Quantity to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withItemInStash(final String itemName, final int count) {
    int itemId = ItemDatabase.getItemId(itemName, count, false);
    AdventureResult item = ItemPool.get(itemId, count);
    return addToList(item, ClanManager.getStash());
  }

  private static Cleanups addToList(final AdventureResult item, final List<AdventureResult> list) {
    var old = item.getCount(list);
    AdventureResult.addResultToList(list, item);
    EquipmentManager.updateEquipmentLists();
    // may have access to a new item = may have access to a new concoction
    ConcoctionDatabase.refreshConcoctions();

    return new Cleanups(
        () -> {
          AdventureResult.removeResultFromList(list, item);
          if (old != 0) AdventureResult.addResultToList(list, item.getInstance(old));
          EquipmentManager.updateEquipmentLists();
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  /**
   * Puts the given item into the player's clan lounge
   *
   * @param itemId Item to give
   * @return Removes the item from the lounge
   */
  public static Cleanups withClanLoungeItem(final int itemId) {
    ClanLoungeRequest.setClanLoungeItem(itemId, 1);
    return new Cleanups(() -> ClanLoungeRequest.setClanLoungeItem(itemId, 0));
  }

  /**
   * Restores Clan Furniture after cleanup
   *
   * @return Resets to previous state
   */
  public static Cleanups withClanFurniture() {
    var old = ClanManager.getClanRumpus();

    return new Cleanups(
        () -> {
          var current = new ArrayList<>(ClanManager.getClanRumpus());
          for (var item : current) {
            if (!old.contains(item)) ClanManager.removeFromRumpus(item);
          }
        });
  }

  /**
   * Adds the given set of furniture to the player's Clan Rumpus Room
   *
   * @param furniture Furniture items to install
   * @return Resets to previous value
   */
  public static Cleanups withClanFurniture(final String... furniture) {
    var cleanups = new Cleanups();
    var rumpus = ClanManager.getClanRumpus();

    for (var f : furniture) {
      var old = rumpus.contains(f);
      ClanManager.addToRumpus(f);

      cleanups.add(
          () -> {
            if (!old) {
              ClanManager.removeFromRumpus(f);
            }
          });
    }

    return cleanups;
  }

  /**
   * Sets the player's meat to the given quantity
   *
   * @param meat Amount of meat to have
   * @return Restores the meat to the previous amount
   */
  public static Cleanups withMeat(final long meat) {
    var old = KoLCharacter.getAvailableMeat();
    KoLCharacter.setAvailableMeat(meat);
    ConcoctionDatabase.refreshConcoctions();
    return new Cleanups(
        () -> {
          KoLCharacter.setAvailableMeat(old);
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  /**
   * Sets the player's closeted meat to the given quantity
   *
   * @param meat Amount of meat to have in closet
   * @return Restores the meat to the previous amount
   */
  public static Cleanups withMeatInCloset(final long meat) {
    var old = KoLCharacter.getClosetMeat();
    KoLCharacter.setClosetMeat(meat);
    return new Cleanups(() -> KoLCharacter.setClosetMeat(old));
  }

  /**
   * Puts item in player's inventory and ensures player meets requirements to equip
   *
   * @param itemName Name of the item
   * @return Removes item from player's inventory and resets stats
   */
  public static Cleanups withEquippableItem(final String itemName) {
    return withEquippableItem(itemName, 1);
  }

  /**
   * Puts number of items in player's inventory and ensures player meets requirements to equip
   *
   * @param itemName Name of the item
   * @param count Number of items
   * @return Removes item from player's inventory and resets stats
   */
  public static Cleanups withEquippableItem(final String itemName, final int count) {
    int itemId = ItemDatabase.getItemId(itemName, count, false);
    return withEquippableItem(ItemPool.get(itemId, count));
  }

  /**
   * Puts item in player's inventory and ensures player meets requirements to equip
   *
   * @param itemId Item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withEquippableItem(final int itemId) {
    return withEquippableItem(itemId, 1);
  }

  /**
   * Puts number of items in player's inventory and ensures player meets requirements to equip
   *
   * @param itemId Item to give
   * @param count Quantity of item to give
   * @return Restores the number of this item to the old value
   */
  public static Cleanups withEquippableItem(final int itemId, final int count) {
    return withEquippableItem(ItemPool.get(itemId, count));
  }

  /**
   * Puts item in player's inventory and ensures player meets requirements to equip
   *
   * @param item Item to add
   * @return Removes item from player's inventory and resets stats
   */
  public static Cleanups withEquippableItem(final AdventureResult item) {
    var cleanups = new Cleanups();
    // Do this first so that Equipment lists and outfits will update appropriately
    cleanups.add(withStatsRequiredForEquipment(item));
    cleanups.add(withItem(item));
    return cleanups;
  }

  /**
   * Adds familiar to player's terrarium, but does not take it out
   *
   * @param familiarId Familiar to add
   * @return Removes familiar from the terrarium
   */
  public static Cleanups withFamiliarInTerrarium(final int familiarId) {
    var familiar = FamiliarData.registerFamiliar(familiarId, 0);
    KoLCharacter.addFamiliar(familiar);
    return new Cleanups(() -> KoLCharacter.removeFamiliar(familiar));
  }

  /**
   * Takes familiar as player's current familiar
   *
   * @param familiarId Familiar to take
   * @return Reset current familiar
   */
  public static Cleanups withFamiliar(final int familiarId) {
    return withFamiliar(familiarId, 0);
  }

  /**
   * Takes familiar as player's current familiar
   *
   * @param familiarId Familiar to take
   * @param experience Experience for familiar to have
   * @return Reset current familiar
   */
  public static Cleanups withFamiliar(final int familiarId, final int experience) {
    return withFamiliar(familiarId, experience, null);
  }

  /**
   * Takes familiar as player's current familiar
   *
   * @param familiarId Familiar to take
   * @param name Name for familiar to have
   * @return Reset current familiar
   */
  public static Cleanups withFamiliar(final int familiarId, final String name) {
    return withFamiliar(familiarId, 1, name);
  }

  /**
   * Takes familiar as player's current familiar
   *
   * @param familiarId Familiar to take
   * @param experience Experience for familiar to have
   * @param name Name for familiar to have
   * @return Reset current familiar
   */
  public static Cleanups withFamiliar(
      final int familiarId, final int experience, final String name) {
    var old = KoLCharacter.getFamiliar();
    var fam = FamiliarData.registerFamiliar(familiarId, experience);
    if (name != null) fam.setName(name);
    KoLCharacter.setFamiliar(fam);
    return new Cleanups(
        () -> {
          KoLCharacter.setFamiliar(old);
          KoLCharacter.removeFamiliar(fam);
        });
  }

  /**
   * Puts familiar in the Buddy Bjorn
   *
   * @param familiarId Familiar to bjorn
   * @return Removes familiar from buddy bjorn
   */
  public static Cleanups withBjorned(final int familiarId) {
    return withBjorned(new FamiliarData(familiarId));
  }

  /**
   * Puts familiar in the Buddy Bjorn
   *
   * @param familiar Familiar to bjorn
   * @return Removes familiar from buddy bjorn
   */
  public static Cleanups withBjorned(final FamiliarData familiar) {
    var old = KoLCharacter.getBjorned();
    KoLCharacter.setBjorned(familiar);
    return new Cleanups(() -> KoLCharacter.setBjorned(old));
  }

  /**
   * Puts familiar in the Crown of Thrones
   *
   * @param familiarId Familiar to enthrone
   * @return Removes familiar from Crown of Thrones
   */
  public static Cleanups withEnthroned(final int familiarId) {
    return withEnthroned(new FamiliarData(familiarId));
  }

  /**
   * Puts familiar in the Crown of Thrones
   *
   * @param familiar Familiar to enthrone
   * @return Removes familiar from Crown of Thrones
   */
  public static Cleanups withEnthroned(final FamiliarData familiar) {
    var old = KoLCharacter.getEnthroned();
    KoLCharacter.setEnthroned(familiar);
    return new Cleanups(() -> KoLCharacter.setEnthroned(old));
  }

  /**
   * Gives player a number of turns of the given effect
   *
   * @param effectId Effect to add
   * @param turns Turns of effect to give
   * @return Removes effect
   */
  public static Cleanups withEffect(final int effectId, final int turns) {
    var effect = EffectPool.get(effectId, turns);
    KoLConstants.activeEffects.add(effect);
    return new Cleanups(() -> KoLConstants.activeEffects.remove(effect));
  }

  /**
   * Gives player a number of turns of the given effect
   *
   * @param effectName Effect to add
   * @param turns Turns of effect to give
   * @return Removes effect
   */
  public static Cleanups withEffect(final String effectName, final int turns) {
    return withEffect(EffectDatabase.getEffectId(effectName), turns);
  }

  /**
   * Gives player one turn of the given effect
   *
   * @param effectName Effect to add
   * @return Removes effect
   */
  public static Cleanups withEffect(final String effectName) {
    return withEffect(effectName, 1);
  }

  /**
   * Gives player one turn of the given effect
   *
   * @param effectId Effect to add
   * @return Removes effect
   */
  public static Cleanups withEffect(final int effectId) {
    return withEffect(effectId, 1);
  }

  /**
   * Gives player (effectively) infinite turns of an effect
   *
   * @param effectName Effect to add intrinsicly
   * @return Removes effect
   */
  public static Cleanups withIntrinsicEffect(final String effectName) {
    return withEffect(effectName, Integer.MAX_VALUE);
  }

  /**
   * Gives player a skill
   *
   * @param skillName Skill to add
   * @return Removes the skill
   */
  public static Cleanups withSkill(final String skillName) {
    KoLCharacter.addAvailableSkill(skillName);
    return new Cleanups(() -> KoLCharacter.removeAvailableSkill(skillName));
  }

  /**
   * Gives player a skill
   *
   * @param skillId Skill to add
   * @return Removes the skill
   */
  public static Cleanups withSkill(final int skillId) {
    KoLCharacter.addAvailableSkill(skillId);
    return new Cleanups(() -> KoLCharacter.removeAvailableSkill(skillId));
  }

  /**
   * Ensures player does not have a skill
   *
   * @param skillId Skill to ensure is removed
   * @return Removes the skill if it was gained
   */
  public static Cleanups withoutSkill(final int skillId) {
    KoLCharacter.removeAvailableSkill(skillId);
    return new Cleanups(() -> KoLCharacter.removeAvailableSkill(skillId));
  }

  /**
   * Sets player's stats to given values. Substats are set to stat squared
   *
   * @param muscle Muscle mainstat
   * @param mysticality Mysticality mainstat
   * @param moxie Moxie mainstat
   * @return Resets stats to zero
   */
  public static Cleanups withStats(final int muscle, final int mysticality, final int moxie) {
    KoLCharacter.setStatPoints(
        muscle,
        (long) muscle * muscle,
        mysticality,
        (long) mysticality * mysticality,
        moxie,
        (long) moxie * moxie);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(
        () -> {
          KoLCharacter.setStatPoints(0, 0, 0, 0, 0, 0);
          KoLCharacter.recalculateAdjustments();
        });
  }

  /**
   * Sets player's muscle to given value. Substats are set to stat squared
   *
   * @param muscle Desired muscle
   * @return Resets muscle to zero
   */
  public static Cleanups withMuscle(final int muscle) {
    return withMuscle(muscle, muscle);
  }

  /**
   * Sets player's muscle to given value. Substats are set to stat squared
   *
   * @param muscle Desired muscle
   * @param buffedMuscle Buffed muscle value
   * @return Resets muscle to zero
   */
  public static Cleanups withMuscle(final int muscle, final int buffedMuscle) {
    KoLCharacter.setMuscle(buffedMuscle, (long) muscle * muscle);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> KoLCharacter.setMuscle(0, 0));
  }

  /**
   * Sets player's muscle to given value unless the current value is higher. Substats are set to
   * stat squared
   *
   * @param muscle Desired minimum muscle
   * @return Resets muscle to zero
   */
  public static Cleanups withMuscleAtLeast(final int muscle) {
    return withMuscle(Math.max(muscle, KoLCharacter.getBaseMuscle()));
  }

  /**
   * Sets player's mysticality to given value. Substats are set to stat squared
   *
   * @param mysticality Desired mysticality
   * @return Resets mysticality to zero
   */
  public static Cleanups withMysticality(final int mysticality) {
    return withMysticality(mysticality, mysticality);
  }

  /**
   * Sets player's mysticality to given value. Substats are set to stat squared
   *
   * @param mysticality Desired mysticality
   * @param buffedMysticality Buffed mysticality value
   * @return Resets mysticality to zero
   */
  public static Cleanups withMysticality(final int mysticality, final int buffedMysticality) {
    KoLCharacter.setMysticality(buffedMysticality, (long) mysticality * mysticality);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> KoLCharacter.setMysticality(0, 0));
  }

  /**
   * Sets player's mysticality to given value unless the current value is higher. Substats are set
   * to stat squared
   *
   * @param mysticality Desired minimum mysticality
   * @return Resets mysticality to zero
   */
  public static Cleanups withMysticalityAtLeast(final int mysticality) {
    return withMysticality(Math.max(mysticality, KoLCharacter.getBaseMysticality()));
  }

  /**
   * Sets player's moxie to given value. Substats are set to stat squared
   *
   * @param moxie Desired moxie
   * @return Resets moxie to zero
   */
  public static Cleanups withMoxie(final int moxie) {
    return withMoxie(moxie, moxie);
  }

  /**
   * Sets player's moxie to given value. Substats are set to stat squared
   *
   * @param moxie Desired moxie
   * @param buffedMoxie Buffed moxie value
   * @return Resets moxie to zero
   */
  public static Cleanups withMoxie(final int moxie, final int buffedMoxie) {
    KoLCharacter.setMoxie(buffedMoxie, (long) moxie * moxie);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> KoLCharacter.setMoxie(0, 0));
  }

  /**
   * Sets player's moxie to given value unless the current value is higher. Substats are set to stat
   * squared
   *
   * @param moxie Desired minimum moxie
   * @return Resets moxie to zero
   */
  public static Cleanups withMoxieAtLeast(final int moxie) {
    return withMoxie(Math.max(moxie, KoLCharacter.getBaseMoxie()));
  }

  /**
   * Sets the player's gender to the given value.
   *
   * @param gender Required gender
   * @return Resets gender to unknown
   */
  public static Cleanups withGender(final int gender) {
    KoLCharacter.setGender(gender);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> KoLCharacter.setGender(0));
  }

  /**
   * Sets the player's level to the given value. This is done by setting all stats to the minimum
   * required for that level.
   *
   * @param level Required level
   * @return Resets level to zero
   */
  public static Cleanups withLevel(final int level) {
    int substats = (int) Math.pow(level, 2) - level * 2 + 5;
    return withStats(substats, substats, substats);
  }

  /**
   * Sets the player's current, maximum and base HP
   *
   * @param current Desired current HP
   * @param maximum Desired buffed maximum HP
   * @param base Desired base maximum HP
   * @return Resets these values to zero
   */
  public static Cleanups withHP(final long current, final long maximum, final long base) {
    KoLCharacter.setHP(current, maximum, base);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(
        () -> {
          KoLCharacter.setHP(0, 0, 0);
          KoLCharacter.recalculateAdjustments();
        });
  }

  /**
   * Sets the player's current, maximum and base MP
   *
   * @param current Desired current MP
   * @param maximum Desired buffed maximum MP
   * @param base Desired base maximum MP
   * @return Resets these values to zero
   */
  public static Cleanups withMP(final long current, final long maximum, final long base) {
    KoLCharacter.setMP(current, maximum, base);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(
        () -> {
          KoLCharacter.setMP(0, 0, 0);
          KoLCharacter.recalculateAdjustments();
        });
  }

  /**
   * Sets King Liberated
   *
   * @param level Required level
   * @return Resets level to zero
   */
  public static Cleanups withKingLiberated() {
    var cleanups = new Cleanups(withProperty("lastKingLiberation"), withProperty("kingLiberated"));
    KoLCharacter.setKingLiberated(true);
    return cleanups;
  }

  /**
   * Sets the player's remaining adventures
   *
   * @param adventures Desired adventures remaining
   * @return Resets remaining adventures to previous value
   */
  public static Cleanups withAdventuresLeft(final int adventures) {
    var old = KoLCharacter.getAdventuresLeft();
    KoLCharacter.setAdventuresLeft(adventures);
    return new Cleanups(() -> KoLCharacter.setAdventuresLeft(old));
  }

  /**
   * Sets the player's ascensions
   *
   * @param ascensions Desired ascensions
   * @return Resets ascensions to previous value
   */
  public static Cleanups withAscensions(final int ascensions) {
    var old = KoLCharacter.getAscensions();
    KoLCharacter.setAscensions(ascensions);
    return new Cleanups(() -> KoLCharacter.setAscensions(old));
  }

  /**
   * Sets the player's hippy stone to broken
   *
   * @return Set's the player's hippy stone to unbroken
   */
  public static Cleanups withHippyStoneBroken() {
    KoLCharacter.setHippyStoneBroken(true);
    return new Cleanups(() -> KoLCharacter.setHippyStoneBroken(false));
  }

  /**
   * Sets the player's fullness
   *
   * @param fullness Desired fullness
   * @return Resets fullness to previous value
   */
  public static Cleanups withFullness(final int fullness) {
    var old = KoLCharacter.getFullness();
    KoLCharacter.setFullness(fullness);
    return new Cleanups(() -> KoLCharacter.setFullness(old));
  }

  /**
   * Sets the player's inebriety
   *
   * @param inebriety Desired inebriety
   * @return Resets inebriety to previous value
   */
  public static Cleanups withInebriety(final int inebriety) {
    var old = KoLCharacter.getInebriety();
    KoLCharacter.setInebriety(inebriety);
    return new Cleanups(() -> KoLCharacter.setInebriety(old));
  }

  /**
   * Sets the player's class
   *
   * @param ascensionClass Desired class
   * @return Resets the class to the previous value
   */
  public static Cleanups withClass(final AscensionClass ascensionClass) {
    var old = KoLCharacter.getAscensionClass();
    KoLCharacter.setAscensionClass(ascensionClass);
    return new Cleanups(() -> KoLCharacter.setAscensionClass(old));
  }

  /**
   * Sets the player's sign
   *
   * @param sign Desired sign
   * @return Resets the sign to the previous value
   */
  public static Cleanups withSign(final String sign) {
    var old = KoLCharacter.getSign();
    KoLCharacter.setSign(sign);
    return new Cleanups(() -> KoLCharacter.setSign(old));
  }

  /**
   * Sets the player's sign
   *
   * @param sign Desired sign
   * @return Resets the sign to the previous value
   */
  public static Cleanups withSign(final ZodiacSign sign) {
    var old = KoLCharacter.getSign();
    KoLCharacter.setSign(sign);
    ConcoctionDatabase.refreshConcoctions();
    return new Cleanups(
        () -> {
          KoLCharacter.setSign(old);
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  /**
   * Sets the player's path
   *
   * @param path Desired path
   * @return Resets the path to the previous value
   */
  public static Cleanups withPath(final Path path) {
    var old = KoLCharacter.getPath();
    KoLCharacter.setPath(path);
    return new Cleanups(() -> KoLCharacter.setPath(old));
  }

  /**
   * Sets the player's location for modifier calculations
   *
   * @param location Desired location
   * @return Resets the location to the previous value
   */
  public static Cleanups withLocation(final String location) {
    var old = AdventureDatabase.getAdventure(Modifiers.currentLocation);
    Modifiers.setLocation(AdventureDatabase.getAdventure(location));
    return new Cleanups(() -> Modifiers.setLocation(old));
  }

  /**
   * Sets the player's limit mode
   *
   * @param limitMode Desired limit mode
   * @return Resets the limit mode to the previous value
   */
  public static Cleanups withLimitMode(final LimitMode limitMode) {
    var old = KoLCharacter.getLimitMode();
    KoLCharacter.setLimitMode(limitMode);
    return new Cleanups(() -> KoLCharacter.setLimitMode(old));
  }

  /**
   * Set's the player's fight state such that KoLmafia believes they are in an anapest mode
   *
   * @return Resets the location to the false
   */
  public static Cleanups withAnapest() {
    FightRequest.anapest = true;
    return new Cleanups(() -> FightRequest.anapest = false);
  }

  /**
   * Set next monster
   *
   * @param monster Monster to set
   * @return Restores to previous value
   */
  public static Cleanups withNextMonster(final MonsterData monster) {
    var previousMonster = MonsterStatusTracker.getLastMonster();
    MonsterStatusTracker.setNextMonster(monster);
    return new Cleanups(() -> MonsterStatusTracker.setNextMonster(previousMonster));
  }

  /**
   * Set next monster
   *
   * @param monsterName Monster to set
   * @return Restores to previous value
   */
  public static Cleanups withNextMonster(final String monsterName) {
    return withNextMonster(MonsterDatabase.findMonster(monsterName));
  }

  /**
   * Set the day used by HolidayDatabase
   *
   * @param year Year to set
   * @param month Month to set
   * @param day Day to set
   * @return Restores to using the real day
   */
  public static Cleanups withDay(final int year, final Month month, final int day) {
    return withDay(year, month, day, 12, 0);
  }

  /**
   * Set the day used by HolidayDatabase
   *
   * @param year Year to set
   * @param month Month to set
   * @param day Day to set
   * @param hour Hour to set
   * @param minute Minute to set
   * @return Restores to using the real day
   */
  public static Cleanups withDay(
      final int year, final Month month, final int day, final int hour, final int minute) {
    var mocked = mockStatic(DateTimeManager.class, Mockito.RETURNS_DEFAULTS);

    mocked
        .when(DateTimeManager::getArizonaDateTime)
        .thenReturn(
            ZonedDateTime.of(
                year, month.getValue(), day, hour, minute, 0, 0, DateTimeManager.ARIZONA));
    mocked
        .when(DateTimeManager::getRolloverDateTime)
        .thenReturn(
            ZonedDateTime.of(
                year, month.getValue(), day, hour, minute, 0, 0, DateTimeManager.ROLLOVER));

    HolidayDatabase.guessPhaseStep();

    return new Cleanups(mocked::close);
  }

  /**
   * Sets the player's noobcore absorbs
   *
   * @param absorbs Number of absorbs to use
   * @return Restores to the old value
   */
  public static Cleanups withUsedAbsorbs(final int absorbs) {
    var old = KoLCharacter.getAbsorbs();
    KoLCharacter.setAbsorbs(absorbs);
    return new Cleanups(() -> KoLCharacter.setAbsorbs(old));
  }

  /**
   * Sets the player to be in hardcore
   *
   * @return restores hardcore
   */
  public static Cleanups withHardcore() {
    return withHardcore(true);
  }

  /**
   * Sets the player's hardcore status
   *
   * @param hardcore Hardcore or not
   * @return Restores previous value
   */
  public static Cleanups withHardcore(final boolean hardcore) {
    var wasHardcore = KoLCharacter.isHardcore();
    KoLCharacter.setHardcore(hardcore);
    return new Cleanups(() -> KoLCharacter.setHardcore(wasHardcore));
  }

  /**
   * Sets the player's campground as having an item installed in it
   *
   * @param itemId Item to add
   * @return Removes the item
   */
  public static Cleanups withCampgroundItem(final int itemId) {
    CampgroundRequest.setCampgroundItem(itemId, 1);
    return new Cleanups(() -> CampgroundRequest.removeCampgroundItem(ItemPool.get(itemId, 1)));
  }

  /**
   * Sets the player's campground as having an item installed in it
   *
   * @param itemId Item to add
   * @param count associated count
   * @return Removes the item
   */
  public static Cleanups withCampgroundItem(final int itemId, final int count) {
    CampgroundRequest.setCampgroundItem(itemId, count);
    return new Cleanups(() -> CampgroundRequest.removeCampgroundItem(ItemPool.get(itemId, 1)));
  }

  /**
   * Clears the Campground and clears it again when done. This prevents leakage if the test adds
   * items to the campground.
   *
   * @return Empties the Campground
   */
  public static Cleanups withEmptyCampground() {
    CampgroundRequest.reset();
    return new Cleanups(() -> CampgroundRequest.reset());
  }

  /**
   * Sets the player's workshed
   *
   * @param itemId Item to set
   * @return Removes workshed item
   */
  public static Cleanups withWorkshedItem(final int itemId) {
    CampgroundRequest.setCurrentWorkshedItem(itemId);
    return new Cleanups(CampgroundRequest::resetCurrentWorkshedItem);
  }

  /**
   * Sets the user as having a range installed
   *
   * @return Removes the range
   */
  public static Cleanups withRange() {
    KoLCharacter.setRange(true);
    ConcoctionDatabase.refreshConcoctions();
    return new Cleanups(
        () -> {
          KoLCharacter.setRange(false);
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  /**
   * Sets the user as having a cocktail kit installed
   *
   * @return Removes the cocktail kit
   */
  public static Cleanups withCocktailKit() {
    KoLCharacter.setCocktailKit(true);
    ConcoctionDatabase.refreshConcoctions();
    return new Cleanups(
        () -> {
          KoLCharacter.setCocktailKit(false);
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  /**
   * Does nothing, but ensures the given property is reverted as part of cleanup
   *
   * @param key Key of property
   * @return Restores the previous value of the property
   */
  public static Cleanups withProperty(final String key) {
    var current = Preferences.getString(key);
    return withProperty(key, current);
  }

  /**
   * Sets a property for the user
   *
   * @param key Key of property
   * @param value Value to set
   * @return Restores the previous value of the property
   */
  public static Cleanups withProperty(final String key, final int value) {
    var oldValue = Preferences.getInteger(key);
    Preferences.setInteger(key, value);
    return new Cleanups(() -> Preferences.setInteger(key, oldValue));
  }

  /**
   * Sets a property for the user
   *
   * @param key Key of property
   * @param value Value to set
   * @return Restores the previous value of the property
   */
  public static Cleanups withProperty(final String key, final String value) {
    var oldValue = Preferences.getString(key);
    Preferences.setString(key, value);
    return new Cleanups(() -> Preferences.setString(key, oldValue));
  }

  /**
   * Sets a property for the user
   *
   * @param key Key of property
   * @param value Value to set
   * @return Restores the previous value of the property
   */
  public static Cleanups withProperty(final String key, final boolean value) {
    var oldValue = Preferences.getBoolean(key);
    Preferences.setBoolean(key, value);
    return new Cleanups(() -> Preferences.setBoolean(key, oldValue));
  }

  /**
   * Does nothing, but ensures the given quest is reverted as part of cleanup
   *
   * @param quest Quest to set
   * @return Restores previous value
   */
  public static Cleanups withQuestProgress(final QuestDatabase.Quest quest) {
    var current = QuestDatabase.getQuest(quest);
    return new Cleanups(() -> QuestDatabase.setQuest(quest, current));
  }

  /**
   * Sets progress for a given quest
   *
   * @param quest Quest to set
   * @param value Value for quest property
   * @return Restores previous value
   */
  public static Cleanups withQuestProgress(final QuestDatabase.Quest quest, final String value) {
    var oldValue = QuestDatabase.getQuest(quest);
    QuestDatabase.setQuest(quest, value);
    return new Cleanups(() -> QuestDatabase.setQuest(quest, oldValue));
  }

  /**
   * Sets progress for a given quest
   *
   * @param quest Quest to set
   * @param step Step to set
   * @return Restores previous value
   */
  public static Cleanups withQuestProgress(final QuestDatabase.Quest quest, final int step) {
    return withQuestProgress(quest, "step" + step);
  }

  /**
   * Sets supplied HttpClient.Builder to be used by GenericRequest
   *
   * @param builder The builder to use
   * @return restores previous builder
   */
  public static Cleanups withHttpClientBuilder(HttpClient.Builder builder) {
    var old = HttpUtilities.getClientBuilder();
    HttpUtilities.setClientBuilder(() -> builder);
    GenericRequest.resetClient();
    GenericRequest.sessionId = "TEST"; // we fake the client, so "run" the requests

    return new Cleanups(
        () -> {
          GenericRequest.sessionId = null;
          HttpUtilities.setClientBuilder(() -> old);
          GenericRequest.resetClient();
        });
  }

  /**
   * Sets supplied passwordHash to be used by GenericRequest
   *
   * @param passwordHash The passwordHash to use
   * @return restores previous passwordHash
   */
  public static Cleanups withPasswordHash(String passwordHash) {
    var old = GenericRequest.passwordHash;
    GenericRequest.setPasswordHash(passwordHash);
    return new Cleanups(
        () -> {
          GenericRequest.setPasswordHash(old);
        });
  }

  /**
   * Sets next response to a GenericRequest Note that this uses its own FakeHttpClientBuilder so
   * getRequests() will not work on one set separately
   *
   * @param code Status code to fake
   * @param response Response text to fake
   * @return Cleans up so this response is not given again
   */
  public static Cleanups withNextResponse(final int code, final String response) {
    return withNextResponse(code, new HashMap<>(), response);
  }

  /**
   * Sets next response to a GenericRequest Note that this uses its own FakeHttpClientBuilder so
   * getRequests() will not work on one set separately
   *
   * @param code Status code to fake
   * @param headers Response headers to fake
   * @param response Response text to fake
   * @return Cleans up so this response is not given again
   */
  public static Cleanups withNextResponse(
      final int code, final Map<String, List<String>> headers, final String response) {
    if (code > 0) {
      return withNextResponse(new FakeHttpResponse<>(code, headers, response));
    }

    return withNextResponse();
  }

  /**
   * Sets next response to a GenericRequest Note that this uses its own FakeHttpClientBuilder so
   * getRequests() will not work on one set separately
   *
   * @param fakeHttpResponses Responses to fake
   * @return Cleans up so this response is not given again
   */
  @SafeVarargs
  public static Cleanups withNextResponse(final FakeHttpResponse<String>... fakeHttpResponses) {
    var old = HttpUtilities.getClientBuilder();
    var builder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> builder);
    GenericRequest.resetClient();
    GenericRequest.sessionId = "TEST"; // we fake the client, so "run" the requests

    for (FakeHttpResponse<String> fakeHttpResponse : fakeHttpResponses) {
      builder.client.addResponse(fakeHttpResponse);
    }

    return new Cleanups(
        () -> {
          GenericRequest.sessionId = null;
          HttpUtilities.setClientBuilder(() -> old);
          GenericRequest.resetClient();
        });
  }

  /**
   * Runs postChoice1 with a given choice and decision and response
   *
   * @param choice Choice to set
   * @param decision Decision to set
   * @param responseText Response to fake
   * @return Restores last choice and last decision
   */
  public static Cleanups withPostChoice1(
      final int choice, final int decision, final String responseText) {
    ChoiceManager.lastChoice = choice;
    ChoiceManager.lastDecision = decision;
    var req = new GenericRequest("choice.php?choice=" + choice + "&option=" + decision);
    req.responseText = responseText;
    ChoiceControl.postChoice1("choice.php?choice=" + choice + "&option=" + decision, req);

    return new Cleanups(
        () -> {
          ChoiceManager.lastChoice = 0;
          ChoiceManager.lastDecision = 0;
        });
  }

  /**
   * Runs postChoice1 with a given choice and decision and response
   *
   * @param choice Choice to set
   * @param decision Decision to set
   * @return Restores last choice and last decision
   */
  public static Cleanups withPostChoice1(final int choice, final int decision) {
    return withPostChoice1(choice, decision, "");
  }

  /**
   * Runs postChoice2 with a given choice and decision and response
   *
   * @param choice Choice to set
   * @param decision Decision to set
   * @param responseText Response to fake
   * @return Restores last choice and last decision
   */
  public static Cleanups withPostChoice2(
      final int choice, final int decision, final String responseText) {
    ChoiceManager.lastChoice = choice;
    ChoiceManager.lastDecision = decision;
    var req = new GenericRequest("choice.php?choice=" + choice + "&option=" + decision);
    req.responseText = responseText;
    ChoiceControl.postChoice2("choice.php?choice=" + choice + "&option=" + decision, req);

    return new Cleanups(
        () -> {
          ChoiceManager.lastChoice = 0;
          ChoiceManager.lastDecision = 0;
        });
  }

  /**
   * Sets the last choice and last decision values
   *
   * @param choice Choice number
   * @param decision Decision number
   * @return Restores previous value
   */
  public static Cleanups withPostChoice2(final int choice, final int decision) {
    return withPostChoice2(choice, decision, "");
  }

  /**
   * Simulates a choice (postChoice1, processResults and then postChoice2)
   *
   * <p>{@code @todo} Still needs some more choice handling (visitChoice, postChoice0)
   *
   * @param choice Choice number
   * @param decision Decision number
   * @return Restores state for choice handling
   */
  public static Cleanups withChoice(
      final int choice, final int decision, final String responseText) {
    var cleanups = new Cleanups();
    cleanups.add(withPostChoice1(choice, decision, responseText));
    ResultProcessor.processResults(false, responseText);
    cleanups.add(withPostChoice2(choice, decision, responseText));
    return cleanups;
  }

  /**
   * Sets the last location
   *
   * @param lastLocationName Last location name to set
   * @return Restores previous value
   */
  public static Cleanups withLastLocation(final String lastLocationName) {
    var location = AdventureDatabase.getAdventure(lastLocationName);
    return withLastLocation(location);
  }

  public static Cleanups withLastLocation(final KoLAdventure lastLocation) {
    var old = KoLAdventure.lastVisitedLocation;
    var clearProperties =
        new Cleanups(
            withProperty("lastAdventure"),
            withProperty("hiddenApartmentProgress"),
            withProperty("hiddenHospitalProgress"),
            withProperty("hiddenOfficeProgress"),
            withProperty("hiddenBowlingAlleyProgress"));

    if (lastLocation == null) {
      KoLAdventure.setLastAdventure((String) null);
    } else {
      KoLAdventure.setLastAdventure(lastLocation);
    }

    var cleanups = new Cleanups(() -> KoLAdventure.setLastAdventure(old));
    cleanups.add(clearProperties);
    return cleanups;
  }

  /**
   * Acts like the player is currently in a fight
   *
   * @return Restores previous value
   */
  public static Cleanups withFight() {
    return withFight(1);
  }

  /**
   * Acts like the player is currently on the given round of a fight
   *
   * @return Restores previous value
   */
  public static Cleanups withFight(final int round) {
    var old = FightRequest.currentRound;
    FightRequest.currentRound = round;
    return new Cleanups(
        () -> {
          FightRequest.resetInstance();
          FightRequest.preFight(false);
          FightRequest.clearInstanceData();
        });
  }

  /**
   * Acts like the player is currently in a multi-fight
   *
   * @return Restores previous value
   */
  public static Cleanups withMultiFight() {
    var old = FightRequest.inMultiFight;
    FightRequest.inMultiFight = true;
    return new Cleanups(() -> FightRequest.inMultiFight = old);
  }

  /**
   * Acts like the player is currently handling a choice
   *
   * @return Restores previous value
   */
  public static Cleanups withHandlingChoice() {
    var old = ChoiceManager.handlingChoice;
    ChoiceManager.handlingChoice = true;
    return new Cleanups(() -> ChoiceManager.handlingChoice = old);
  }

  /**
   * Sets the player's "currently handling a choice" flag
   *
   * @param handlingChoice Whether player should appear to be currently handling a choice
   * @return Restores previous value
   */
  public static Cleanups withHandlingChoice(final boolean handlingChoice) {
    var old = ChoiceManager.handlingChoice;
    ChoiceManager.handlingChoice = handlingChoice;
    return new Cleanups(() -> ChoiceManager.handlingChoice = old);
  }

  /**
   * Sets the player's "currently handling a choice" flag
   *
   * @param whichChoice which choice is currently being handled
   * @return Restores previous value
   */
  public static Cleanups withHandlingChoice(final int whichChoice) {
    ChoiceManager.handlingChoice = true;
    ChoiceManager.lastChoice = whichChoice;
    return new Cleanups(
        () -> {
          ChoiceManager.handlingChoice = false;
          ChoiceManager.lastChoice = 0;
        });
  }

  /**
   * Sets the current "item monster" (the item source of the currently-being-fought monster)
   *
   * @param itemMonster Item source for monster
   * @return Restores to previous value
   */
  public static Cleanups withItemMonster(final String itemMonster) {
    var old = GenericRequest.itemMonster;
    GenericRequest.itemMonster = itemMonster;
    return new Cleanups(() -> GenericRequest.itemMonster = old);
  }

  /**
   * Sets the player's ronin status
   *
   * @param inRonin Whether the player is in Ronin
   * @return Restores to previous value
   */
  public static Cleanups withRonin(final boolean inRonin) {
    var old = KoLCharacter.inRonin();
    KoLCharacter.setRonin(inRonin);
    return new Cleanups(() -> KoLCharacter.setRonin(old));
  }

  /**
   * Sets the player's "can interact" status (i.e. whether they are under ronin-style restrictions)
   *
   * @param canInteract Whether the player can interact
   * @return Restores to previous value
   */
  public static Cleanups withInteractivity(final boolean canInteract) {
    var old = CharPaneRequest.canInteract();
    CharPaneRequest.setCanInteract(canInteract);
    return new Cleanups(() -> CharPaneRequest.setCanInteract(old));
  }

  /**
   * Sets the player's standard restriction status
   *
   * @param restricted Whether the player is standard restricted
   * @return Restores to previous value
   */
  public static Cleanups withRestricted(final boolean restricted) {
    var old = KoLCharacter.getRestricted();
    KoLCharacter.setRestricted(restricted);
    return new Cleanups(() -> KoLCharacter.setRestricted(old));
  }

  /**
   * Sets whether a particular item / skill is allowed in Standard
   *
   * @param type The type of key
   * @param key The restricted item / skill
   * @return Restores to previous value
   */
  public static Cleanups withNotAllowedInStandard(final RestrictedItemType type, final String key) {
    var lcKey = key.toLowerCase();
    var map = StandardRequest.getRestrictionMap();
    map.computeIfAbsent(type, k -> new HashSet<>()).add(lcKey);

    return new Cleanups(
        () -> {
          var val = map.get(type);
          if (val != null) val.remove(lcKey);
        });
  }

  /**
   * Sets the basement level
   *
   * @param level Level to set
   * @return Restores to the previous value
   */
  public static Cleanups withBasementLevel(final int level) {
    var old = BasementRequest.getBasementLevel();
    BasementRequest.setBasementLevel(level);
    return new Cleanups(() -> BasementRequest.setBasementLevel(old));
  }

  /**
   * Sets the basement level to zero This is useful if the tested function is going to set the level
   * and we just want to make sure it gets reset
   *
   * @return Restores the previous value
   */
  public static Cleanups withBasementLevel() {
    return withBasementLevel(0);
  }

  /**
   * Sets the continuation state (i.e. whether the CLI is in green "good", red "bad" mode etc)
   *
   * @param continuationState State to set
   * @return Restores previous continuation state
   */
  public static Cleanups withContinuationState(final MafiaState continuationState) {
    var old = StaticEntity.getContinuationState();
    StaticEntity.setContinuationState(continuationState);
    return new Cleanups(() -> StaticEntity.setContinuationState(old));
  }

  /**
   * Sets the continuation state to continue (green "good" mode)
   *
   * @return Restores previous state
   */
  public static Cleanups withContinuationState() {
    return withContinuationState(MafiaState.CONTINUE);
  }

  /**
   * Sets a counter
   *
   * @param turns how many turns
   * @param label what to call it
   * @param image what image to show in charpane
   * @return stops the counter
   */
  public static Cleanups withCounter(int turns, String label, String image) {
    var cleanups = new Cleanups(withProperty("relayCounters"));
    TurnCounter.startCounting(turns, label, image);
    cleanups.add(() -> TurnCounter.stopCounting(label));
    return cleanups;
  }

  /**
   * Sets the player's number of turns played
   *
   * @param turnsPlayed Turns to have played
   * @return Restores the old version
   */
  public static Cleanups withTurnsPlayed(final int turnsPlayed) {
    var old = KoLCharacter.getTurnsPlayed();
    KoLCharacter.setTurnsPlayed(turnsPlayed);
    return new Cleanups(() -> KoLCharacter.setTurnsPlayed(old));
  }

  /**
   * Sets the player's limit mode
   *
   * @param limitMode Desired limit mode
   * @return Resets limit mode to previous value
   */
  public static Cleanups withLimitMode(final String limitMode) {
    var old = KoLCharacter.getLimitMode();
    KoLCharacter.setLimitMode(limitMode);
    return new Cleanups(() -> KoLCharacter.setLimitMode(old));
  }
}
