package net.sourceforge.kolmafia.textui.command;

import java.io.File;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.DateTimeManager;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase.SkillType;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.swingui.CalendarFrame;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShowDataCommand extends AbstractCommand {
  public ShowDataCommand() {
    this.usage = " [<param>] - list indicated type of data, possibly filtered by param.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    ShowDataCommand.show(cmd + " " + parameters);
  }

  public static void show(final String parameters) {
    ShowDataCommand.show(parameters, false);
  }

  /**
   * A special module used specifically for properly printing out data relevant to the current
   * session.
   */
  public static void show(String parameters, final boolean sessionPrint) {
    if (parameters.length() == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Print what?");
      return;
    }

    parameters = parameters.trim();
    int spaceIndex = parameters.indexOf(" ");

    String list = spaceIndex == -1 ? parameters : parameters.substring(0, spaceIndex).trim();
    String filter =
        spaceIndex == -1
            ? ""
            : StringUtilities.getCanonicalName(parameters.substring(spaceIndex).trim());

    PrintStream desiredOutputStream =
        sessionPrint ? RequestLogger.getSessionStream() : RequestLogger.INSTANCE;

    if (!filter.equals("")
        && (parameters.startsWith("summary")
            || parameters.startsWith("session")
            || parameters.equals("status")
            || parameters.startsWith("equip")
            || parameters.startsWith("encounters")
            || parameters.startsWith("locations"))) {
      desiredOutputStream =
          LogStream.openStream(new File(KoLConstants.ROOT_LOCATION, filter), false);
      filter = "";
    }

    ShowDataCommand.show(list, filter, desiredOutputStream);

    if (sessionPrint && RequestLogger.isDebugging()) {
      ShowDataCommand.show(list, filter, RequestLogger.getDebugStream());
    }

    if (!sessionPrint) {
      desiredOutputStream.close();
    }
  }

  /**
   * A special module used specifically for properly printing out data relevant to the current
   * session. This method is more specialized than its counterpart and is used when the data to be
   * printed is known, as well as the stream to print to. Usually called by its counterpart to
   * handle specific instances.
   */
  private static void show(
      final String desiredData, String filter, final PrintStream desiredStream) {
    desiredStream.println();

    if (desiredData.startsWith("moon")) {
      var today = DateTimeManager.getRolloverDateTime();

      desiredStream.println(
          today.format(DateTimeFormatter.ofPattern(CalendarFrame.LONG_FORMAT.toPattern()))
              + " - "
              + HolidayDatabase.getCalendarDayAsString(today));
      desiredStream.println();

      desiredStream.println("Ronald: " + HolidayDatabase.getRonaldPhaseAsString());
      desiredStream.println("Grimace: " + HolidayDatabase.getGrimacePhaseAsString());
      desiredStream.println("Mini-moon: " + HolidayDatabase.getHamburglarPositionAsString());
      desiredStream.println();

      String[] holidayPredictions = HolidayDatabase.getHolidayPredictions(today);
      for (String holidayPrediction : holidayPredictions) {
        desiredStream.println(holidayPrediction);
      }

      desiredStream.println();
      desiredStream.println(HolidayDatabase.getHoliday());
      desiredStream.println(HolidayDatabase.getMoonEffect());
      desiredStream.println();
      return;
    }

    if (desiredData.equals("session")) {
      desiredStream.println("Player: " + KoLCharacter.getUserName());
      desiredStream.println("Session Id: " + GenericRequest.sessionId);
      desiredStream.println("Password Hash: " + GenericRequest.passwordHash);
      desiredStream.println("Current Server: " + GenericRequest.KOL_HOST);
      desiredStream.println();
      return;
    }

    if (desiredData.equals("status")) {
      desiredStream.println("Name: " + KoLCharacter.getUserName());
      desiredStream.println("Class: " + KoLCharacter.getAscensionClassName());
      desiredStream.println();

      desiredStream.println("Lv: " + KoLCharacter.getLevel());
      desiredStream.println(
          "HP: "
              + KoLCharacter.getCurrentHP()
              + " / "
              + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getMaximumHP()));
      desiredStream.println(
          "MP: "
              + KoLCharacter.getCurrentMP()
              + " / "
              + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getMaximumMP()));

      desiredStream.println();

      desiredStream.println(
          "Mus: "
              + ShowDataCommand.getStatString(
                  KoLCharacter.getBaseMuscle(),
                  KoLCharacter.getAdjustedMuscle(),
                  KoLCharacter.getMuscleTNP()));
      desiredStream.println(
          "Mys: "
              + ShowDataCommand.getStatString(
                  KoLCharacter.getBaseMysticality(),
                  KoLCharacter.getAdjustedMysticality(),
                  KoLCharacter.getMysticalityTNP()));
      desiredStream.println(
          "Mox: "
              + ShowDataCommand.getStatString(
                  KoLCharacter.getBaseMoxie(),
                  KoLCharacter.getAdjustedMoxie(),
                  KoLCharacter.getMoxieTNP()));

      desiredStream.println();

      desiredStream.println("Advs: " + KoLCharacter.getAdventuresLeft());
      desiredStream.println(
          "Meat: " + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getAvailableMeat()));

      desiredStream.println();

      desiredStream.println(
          "Full: " + KoLCharacter.getFullness() + " / " + KoLCharacter.getStomachCapacity());
      desiredStream.println(
          "Drunk: " + KoLCharacter.getInebriety() + " / " + KoLCharacter.getLiverCapacity());
      desiredStream.println(
          "Spleen: " + KoLCharacter.getSpleenUse() + " / " + KoLCharacter.getSpleenLimit());

      desiredStream.println();
      return;
    }

    if (desiredData.equals("modifiers")) {
      desiredStream.println(
          "ML: " + KoLConstants.MODIFIER_FORMAT.format(KoLCharacter.getMonsterLevelAdjustment()));
      desiredStream.println(
          "Enc: "
              + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getCombatRateAdjustment())
              + "%");
      desiredStream.println(
          "Init: "
              + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getInitiativeAdjustment())
              + "%");

      desiredStream.println();

      desiredStream.println(
          "Exp: "
              + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
                  KoLCharacter.getExperienceAdjustment()));
      desiredStream.println(
          "Meat: "
              + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
                  KoLCharacter.getMeatDropPercentAdjustment())
              + "%");
      desiredStream.println(
          "Item: "
              + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
                  KoLCharacter.getItemDropPercentAdjustment())
              + "%");

      desiredStream.println();
      return;
    }

    if (desiredData.startsWith("equip")) {
      AdventureResult hat = EquipmentManager.getEquipment(Slot.HAT);
      desiredStream.println("Hat: " + hat);
      if (hat.getItemId() == ItemPool.HATSEAT) {
        desiredStream.println("Carrying: " + KoLCharacter.getEnthroned());
      }
      desiredStream.println("Weapon: " + EquipmentManager.getEquipment(Slot.WEAPON));

      if (EquipmentManager.getFakeHands() > 0) {
        desiredStream.println("Fake Hands: " + EquipmentManager.getFakeHands());
      }

      desiredStream.println("Off-hand: " + EquipmentManager.getEquipment(Slot.OFFHAND));
      desiredStream.println("Shirt: " + EquipmentManager.getEquipment(Slot.SHIRT));
      desiredStream.println("Pants: " + EquipmentManager.getEquipment(Slot.PANTS));

      AdventureResult container = EquipmentManager.getEquipment(Slot.CONTAINER);
      if (container != EquipmentRequest.UNEQUIP) {
        desiredStream.println("Back: " + container);
        if (container.getItemId() == ItemPool.BUDDY_BJORN) {
          desiredStream.println("Carrying: " + KoLCharacter.getBjorned());
        }
      }

      desiredStream.println();

      desiredStream.println("Acc. 1: " + EquipmentManager.getEquipment(Slot.ACCESSORY1));
      desiredStream.println("Acc. 2: " + EquipmentManager.getEquipment(Slot.ACCESSORY2));
      desiredStream.println("Acc. 3: " + EquipmentManager.getEquipment(Slot.ACCESSORY3));

      desiredStream.println();

      desiredStream.println("Pet: " + KoLCharacter.getFamiliar());
      desiredStream.println(
          "Item: "
              + EquipmentManager.getFamiliarItem()
              + (EquipmentManager.familiarItemLocked() ? " (locked)" : ""));
      AdventureResult st1 = EquipmentManager.getEquipment(Slot.STICKER1);
      AdventureResult st2 = EquipmentManager.getEquipment(Slot.STICKER2);
      AdventureResult st3 = EquipmentManager.getEquipment(Slot.STICKER3);
      if (st1 != EquipmentRequest.UNEQUIP
          || st2 != EquipmentRequest.UNEQUIP
          || st3 != EquipmentRequest.UNEQUIP) {
        desiredStream.println();
        desiredStream.println(
            "Sticker 1: "
                + ShowDataCommand.getStickerText(st1, EquipmentManager.getTurns(Slot.STICKER1)));
        desiredStream.println(
            "Sticker 2: "
                + ShowDataCommand.getStickerText(st2, EquipmentManager.getTurns(Slot.STICKER2)));
        desiredStream.println(
            "Sticker 3: "
                + ShowDataCommand.getStickerText(st3, EquipmentManager.getTurns(Slot.STICKER3)));
      }
      return;
    }

    if (desiredData.equals("encounters")) {
      desiredStream.println("Encounter Listing: ");

      desiredStream.println();
      RequestLogger.printList(KoLConstants.encounterList, desiredStream);
      return;
    }

    if (desiredData.equals("locations")) {
      desiredStream.println("Visited Locations: ");
      desiredStream.println();

      RequestLogger.printList(KoLConstants.adventureList, desiredStream);
      return;
    }

    if (desiredData.equals("counters")) {
      String counters = TurnCounter.getUnexpiredCounters();
      if (counters.equals("")) {
        desiredStream.println("No active counters.");
      } else {
        desiredStream.println("Unexpired counters:");
        desiredStream.println(counters);
      }
      desiredStream.println();
      return;
    }

    List<?> mainList =
        desiredData.equals("closet")
            ? KoLConstants.closet
            : desiredData.equals("summary")
                ? KoLConstants.tally
                : desiredData.equals("storage")
                    ? KoLConstants.storage
                    : desiredData.equals("display")
                        ? KoLConstants.collection
                        : desiredData.equals("outfits")
                            ? EquipmentManager.getOutfits()
                            : desiredData.equals("familiars")
                                ? KoLCharacter.usableFamiliars()
                                : KoLConstants.inventory;

    if (desiredData.equals("effects")) {
      mainList = KoLConstants.activeEffects;
      AdventureResult[] effects = new AdventureResult[mainList.size()];
      mainList.toArray(effects);

      int nBuffs = 0;

      for (int i = 0; i < effects.length; ++i) {
        String skillName = UneffectRequest.effectToSkill(effects[i].getName());
        if (SkillDatabase.contains(skillName)) {
          int skillId = SkillDatabase.getSkillId(skillName);
          if (SkillDatabase.isAccordionThiefSong(skillId)) {
            ++nBuffs;
          }
        }
      }

      desiredStream.println(nBuffs + " of " + UseSkillRequest.songLimit() + " AT buffs active.");
    }

    if (desiredData.startsWith("skills")) {
      List<UseSkillRequest> skillsList = new ArrayList<>(KoLConstants.availableSkills);
      filter = filter.toLowerCase();

      if (filter.startsWith("cast")) {
        List<UseSkillRequest> intersect = SkillDatabase.getCastableSkills();
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("pass")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.PASSIVE);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("self")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.SELF_ONLY);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("buff")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.BUFF);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("combat")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.COMBAT);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("song")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.SONG);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("expression")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.EXPRESSION);
        skillsList.retainAll(intersect);
        filter = "";
      }

      if (filter.startsWith("walk")) {
        List<UseSkillRequest> intersect = SkillDatabase.getSkillsByType(SkillType.WALK);
        skillsList.retainAll(intersect);
        filter = "";
      }

      mainList = skillsList;
    }

    if (filter.equals("")) {
      RequestLogger.printList(mainList, desiredStream);
      return;
    }

    String currentItem;
    List<Object> resultList = new ArrayList<>();

    Object[] items = new Object[mainList.size()];
    mainList.toArray(items);
    Matcher m = Pattern.compile("&lt;.*?&gt;").matcher("");

    for (int i = 0; i < items.length; ++i) {
      currentItem = StringUtilities.getCanonicalName(items[i].toString());
      if (currentItem.indexOf(filter) != -1
          || m.reset(currentItem).replaceAll("").indexOf(filter) != -1) {
        resultList.add(items[i]);
      }
    }

    RequestLogger.printList(resultList, desiredStream);
  }

  private static String getStatString(final int base, final int adjusted, final int tnp) {
    StringBuffer statString = new StringBuffer();
    statString.append(KoLConstants.COMMA_FORMAT.format(adjusted));

    if (base != adjusted) {
      statString.append(" (" + KoLConstants.COMMA_FORMAT.format(base) + ")");
    }

    statString.append(", tnp = ");
    statString.append(KoLConstants.COMMA_FORMAT.format(tnp));

    return statString.toString();
  }

  private static String getStickerText(AdventureResult item, final int turns) {
    if (!item.equals(EquipmentRequest.UNEQUIP)) {
      item = item.getInstance(turns);
    }
    return item.toString();
  }
}
