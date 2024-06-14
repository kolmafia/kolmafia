package net.sourceforge.kolmafia.webui;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.textui.command.EudoraCommand.Correspondent;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ValhallaDecorator {
  private ValhallaDecorator() {}

  public static final void decorateGashJump(final String location, final StringBuffer buffer) {
    // ascend.php
    // ascend.php?alttext=communityservice

    if (buffer.indexOf("<p>Are you") > -1) {
      buffer.delete(buffer.indexOf("<p>Are you"), buffer.indexOf("<p><center>"));
    }

    StringUtilities.singleStringReplace(buffer, "<p>Please", " Please");

    StringBuffer predictions = new StringBuffer();

    predictions
        .append("</center></td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>")
        .append(
            "<td><div style=\"padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>");
    HolidayDatabase.addPredictionHTML(
        predictions, DateTimeManager.getRolloverDateTime(), HolidayDatabase.getPhaseStep(), false);
    predictions
        .append("</font></div></td></tr><tr><td colspan=3><br>")
        .append(KoLConstants.LINE_BREAK)
        .append(KoLConstants.LINE_BREAK);

    StringUtilities.singleStringReplace(buffer, "</center><p>", predictions.toString());

    // We remove the confirmation checkboxes and automatically submit those controls as "on"
    String oldButtons =
        "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)";
    String newButtons =
        "<input type=submit class=button value=\"Ascend\"><input type=hidden name=confirm value=on><input type=hidden name=confirm2 value=on>";

    StringBuffer reminders = new StringBuffer();
    reminders
        .append("<br><table>")
        .append("<tr><td>")
        .append(newButtons)
        .append("</td></tr>")
        .append("</table>")
        .append("<br><table cellspacing=10 cellpadding=10><tr>");

    ArrayList<String> skillList = new ArrayList<>();
    ArrayList<UseSkillRequest> unpermedSkills = new ArrayList<>();
    for (int i = 0; i < KoLConstants.availableSkills.size(); ++i) {
      UseSkillRequest skill = KoLConstants.availableSkills.get(i);
      skillList.add(String.valueOf(skill.getSkillId()));
      if (!KoLConstants.permedSkills.contains(skill)) {
        unpermedSkills.add(skill);
      }
    }

    reminders.append(
        "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Haven't Yet Permed</th></tr><tr><td align=center><font size=\"-1\">");
    ValhallaDecorator.listPermableSkills(reminders, unpermedSkills);
    reminders.append("</font></td></tr></table></td>");

    StringBuffer buySkills = new StringBuffer();
    buySkills.append(
        "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Didn't Buy</th></tr><tr><td align=center><font size=\"-1\">");
    int count =
        ValhallaDecorator.listPermanentSkills(
            buySkills, skillList, SkillDatabase.classSkillsBase());
    buySkills.append("</font></td></tr></table></td>");
    // Don't show not purchasable skill list if nothing to show
    if (count != 0) {
      reminders.append(buySkills);
    }

    reminders.append(
        "<td bgcolor=\"#eeeeff\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Common Stuff You Didn't Do</th></tr><tr><td align=center><font size=\"-1\">");
    ValhallaDecorator.listCommonTasks(reminders);
    reminders.append("</font></td></tr></table></td>").append("</tr></table><br><br>");

    StringUtilities.singleStringReplace(buffer, oldButtons, reminders.toString());

    return;
  }

  private static void listPermableSkills(
      final StringBuffer buffer, final ArrayList<UseSkillRequest> unpermedSkills) {
    for (UseSkillRequest skill : unpermedSkills) {
      int skillId = skill.getSkillId();

      if (!SkillDatabase.isPermable(skillId)) {
        continue;
      }

      String skillName = skill.getSkillName();

      buffer
          .append("<nobr>")
          .append("<a onClick=\"skill('")
          .append(skillId)
          .append("');\">")
          .append(skillName)
          .append("</a>")
          .append("</nobr><br>");
    }
  }

  private static int listPermanentSkills(
      final StringBuffer buffer, final ArrayList<String> skillList, final int startingPoint) {
    int count = 0;
    for (int i = 0; i < 100; ++i) {
      int skillId = startingPoint + i;

      String skillName = SkillDatabase.getSkillName(skillId);
      if (skillName == null) {
        continue;
      }

      if (!SkillDatabase.isPermable(skillId)) {
        continue;
      }

      // You can ascend in Goocore without breaking Ronin
      if (!StandardRequest.isNotRestricted(RestrictedItemType.SKILLS, skillName)) {
        continue;
      }

      boolean alreadyPermed = skillList.contains(String.valueOf(skillId));

      buffer.append("<nobr>");
      if (alreadyPermed) {
        buffer.append("<font color=darkgray><s>");
      }

      buffer
          .append("<a onClick=\"skill('")
          .append(skillId)
          .append("');\">")
          .append(skillName)
          .append("</a>");

      if (alreadyPermed) {
        buffer.append("</s></font>");
      }

      buffer.append("</nobr><br>");
      count++;
    }
    return count;
  }

  private static void listCommonTasks(final StringBuffer buffer) {
    RelayRequest.redirectedCommandURL = "/ascend.php";

    boolean hasGift = false;
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.RUBBER_EMO_ROE, "Veracity");
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.RUBBER_WWTNSD_BRACELET, "Veracity");
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.STUFFED_COCOABO, "holatuwol");
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.D10, "bmaher");
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.SPADE_NECKLACE, "Darzil");
    hasGift |= ValhallaDecorator.developerGift(buffer, ItemPool.TOAST, "gausie");

    int count = InventoryManager.getCount(ItemPool.INSTANT_KARMA);
    if (count > 0) {
      int banked = Preferences.getInteger("bankedKarma");
      buffer
          .append(
              "<nobr><a href=\"javascript:if(confirm('Are you sure you want to discard your Instant Karma?')) {singleUse('inventory.php?which=1&action=discard&pwd=")
          .append(GenericRequest.passwordHash)
          .append("&whichitem=")
          .append(ItemPool.INSTANT_KARMA)
          .append(
              "&ajax=1');discardKarma();}void(0);\">discard karma</a> (have <span id='haveKarma'>")
          .append(count)
          .append("</span>, banked <span id='bankedKarma'>")
          .append(banked)
          .append("</span>)</nobr><br>");
    }

    if (KoLCharacter.getZapper() != null) {
      buffer
          .append("<nobr><a href=\"wand.php?whichwand=")
          .append(KoLCharacter.getZapper().getItemId())
          .append("\">blow up your zap wand</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.DEAD_MIMIC)) {
      buffer
          .append("<nobr><a href=\"javascript:singleUse('inv_use.php?&pwd=")
          .append(GenericRequest.passwordHash)
          .append("&which=3&whichitem=")
          .append(ItemPool.DEAD_MIMIC)
          .append("&ajax=1')\">use your dead mimic</a></nobr><br>");
    }

    ValhallaDecorator.checkForKeyLime(buffer, ItemPool.BORIS_KEY, "Boris&#39;s");
    ValhallaDecorator.checkForKeyLime(buffer, ItemPool.JARLSBERG_KEY, "Jarlsberg&#39;s");
    ValhallaDecorator.checkForKeyLime(buffer, ItemPool.SNEAKY_PETE_KEY, "Sneaky Pete&#39;");
    ValhallaDecorator.checkForKeyLime(buffer, ItemPool.DIGITAL_KEY, "digital");
    ValhallaDecorator.checkForKeyLime(buffer, ItemPool.STAR_KEY, "star");

    if (InventoryManager.hasItem(ItemPool.BUBBLIN_STONE)) {
      buffer
          .append(
              "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+1+aerated+diving+helmet&pwd=")
          .append(GenericRequest.passwordHash)
          .append("\">make an aerated diving helmet</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.CITADEL_SATCHEL)) {
      buffer.append(
          "<nobr><a href=\"guild.php?place=paco\">complete white citadel quest by turning in White Citadel Satisfaction Satchel</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.THICK_PADDED_ENVELOPE)) {
      buffer.append(
          "<nobr><a href=\"guild.php?place=paco\">complete dwarvish delivery quest by turning in thick padded envelope</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.DWARVISH_PUNCHCARD)) {
      buffer.append(
          "<nobr><a href=\"dwarfcontraption.php\">acquire dwarvish war outfit piece</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.EARTHENWARE_MUFFIN_TIN)
        && Preferences.getString("muffinOnOrder").equals("none")) {
      buffer.append(
          "<nobr><a href=\"place.php?whichplace=monorail&action=monorail_downtown\">order a muffin at the Breakfast Counter</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.BLUEBERRY_MUFFIN)
        || InventoryManager.hasItem(ItemPool.BRAN_MUFFIN)
        || InventoryManager.hasItem(ItemPool.CHOCOLATE_CHIP_MUFFIN)) {
      buffer.append(
          "<nobr><a href=\"inventory.php?which=1\">eat your muffin and request a new one</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.RAT_WHISKER)
        && QuestDatabase.isQuestFinished(Quest.ARTIST)) {
      buffer
          .append(
              "<nobr><a href=\"place.php?whichplace=town_wrong&action=townwrong_artist_quest&subaction=whiskers\">")
          .append("trade in rat whiskers for meat</a></nobr><br>");
    }

    if (InventoryManager.hasItem(ItemPool.MILK_CAP)) {
      buffer.append(
          "<nobr><a href=\"shop.php?whichshop=olivers\">spend milk cap(s)</a></nobr><br>");
    }

    GenericRequest trophyCheck = new GenericRequest("trophy.php");
    trophyCheck.run();
    if (!trophyCheck.responseText.contains("You're not currently entitled to any trophies")) {
      buffer.append("<nobr><a href=\"trophy.php\">buy trophies you're eligible for</a></nobr><br>");
    }
    int ip = Preferences.getInteger("lastGoofballBuy");
    if (KoLCharacter.getAscensions() > ip) {
      buffer.append("<nobr><a href=\"tavern.php?place=susguy\">get free goofballs?</a></nobr><br>");
    }

    if (KoLCharacter.getAttacksLeft() > 0) {
      buffer.append(
          "<nobr><a href=\"peevpee.php?place=fight\">Use remaining PVP fights</a></nobr><br>");
    }

    ValhallaDecorator.checkCatHeists(buffer);

    ValhallaDecorator.check2002MrStoreCredit(buffer);

    ValhallaDecorator.switchSeeds(buffer);

    ValhallaDecorator.switchCorrespondent(buffer);

    ValhallaDecorator.switchFolderHolder(buffer);

    ValhallaDecorator.checkIceHouse(buffer);

    ValhallaDecorator.switchChateau(buffer);

    ValhallaDecorator.switchCowboyBoots(buffer);

    ValhallaDecorator.collectUntradablePulls(buffer);
  }

  private static void checkForKeyLime(StringBuffer buffer, int itemId, String keyType) {
    if (!InventoryManager.hasItem(itemId)) {
      return;
    }

    buffer
        .append("<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+")
        .append(InventoryManager.getAccessibleCount(itemId))
        .append("+")
        .append(StringUtilities.getURLEncode(keyType))
        .append("+key+lime&pwd=")
        .append(GenericRequest.passwordHash)
        .append("\">make a ")
        .append(keyType)
        .append(" key lime</a></nobr><br />");
  }

  private static boolean developerGift(
      final StringBuffer buffer, final int itemId, final String developer) {
    // We exclude the stash here because the user may hit clan karma limits, which will cause mafia
    // to attempt to acquire the difference from the mall.
    int giftCount = InventoryManager.getAccessibleCount(itemId, false);
    if (giftCount <= 0) {
      return false;
    }

    String itemName = StringUtilities.getURLEncode(ItemDatabase.getItemName(itemId));
    String plural = ItemDatabase.getPluralName(itemId);

    buffer
        .append("<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=acquire+")
        .append(giftCount)
        .append("+")
        .append(itemName)
        .append(";csend+")
        .append(giftCount)
        .append("+")
        .append(itemName)
        .append("+to+")
        .append(developer)
        .append("&pwd=")
        .append(GenericRequest.passwordHash)
        .append("\">send your ")
        .append(plural)
        .append(" to ")
        .append(developer)
        .append("</a></nobr><br>");
    return true;
  }

  private static void gardenOption(
      final StringBuffer buffer, String seedsEncoded, String seedsShort) {
    buffer
        .append("<option style=\"background-color: #eeeeff\" ")
        .append("value=\"/KoLmafia/redirectedCommand?cmd=acquire+")
        .append(seedsEncoded)
        .append(";")
        .append("+use+")
        .append(seedsEncoded)
        .append("&pwd=")
        .append(GenericRequest.passwordHash)
        .append("\">")
        .append(seedsShort)
        .append("</option>");
  }

  private static void switchSeeds(final StringBuffer buffer) {
    boolean havePumpkin = InventoryManager.hasItem(ItemPool.PUMPKIN_SEEDS);
    boolean havePeppermint = InventoryManager.hasItem(ItemPool.PEPPERMINT_PACKET);
    boolean haveSkeleton = InventoryManager.hasItem(ItemPool.DRAGON_TEETH);
    boolean haveBeer = InventoryManager.hasItem(ItemPool.BEER_SEEDS);
    boolean haveWinter = InventoryManager.hasItem(ItemPool.WINTER_SEEDS);
    boolean haveThanksGarden = InventoryManager.hasItem(ItemPool.THANKSGARDEN_SEEDS);
    boolean haveGrass = InventoryManager.hasItem(ItemPool.TALL_GRASS_SEEDS);
    boolean haveMushroom = InventoryManager.hasItem(ItemPool.MUSHROOM_SPORES);
    boolean haveRock = InventoryManager.hasItem(ItemPool.ROCK_SEEDS);
    if (!havePumpkin
        && !havePeppermint
        && !haveSkeleton
        && !haveBeer
        && !haveWinter
        && !haveThanksGarden
        && !haveGrass
        && !haveMushroom
        && !haveRock) {
      return;
    }

    buffer.append("<nobr>Garden: ");

    buffer.append(
        "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"garden\" onchange=\"if (this.value) window.location.href=this.value\">");
    buffer.append("<option value=\"\" style=\"background-color: #eeeeff\">Plant one</option>");

    if (havePumpkin) {
      gardenOption(buffer, "packet+of+pumpkin+seeds", "pumpkin");
    }

    if (havePeppermint) {
      gardenOption(buffer, "Peppermint+Pip+Packet", "peppermint");
    }

    if (haveSkeleton) {
      gardenOption(buffer, "packet+of+dragon's+teeth", "skeleton");
    }

    if (haveBeer) {
      gardenOption(buffer, "packet+of+beer+seeds", "beer");
    }

    if (haveWinter) {
      gardenOption(buffer, "packet+of+winter+seeds", "winter");
    }

    if (haveThanksGarden) {
      gardenOption(buffer, "packet+of+thanksgarden+seeds", "thanksgarden");
    }

    if (haveGrass) {
      gardenOption(buffer, "packet+of+tall+grass+seeds", "grass");
    }

    if (haveMushroom) {
      gardenOption(buffer, "packet+of+mushroom+spores", "mushroom");
    }

    if (haveRock) {
      gardenOption(buffer, "packet+of+rock+seeds", "rock");
    }

    buffer.append("</select></form>");

    CropType cropType = CampgroundRequest.getCropType();
    if (cropType != null) {
      String cropString =
          switch (cropType) {
            case PUMPKIN -> "Pumpkin";
            case PEPPERMINT -> "Peppermint";
            case SKELETON -> "Skeleton";
            case BEER -> "Beer Garden";
            case WINTER -> "Winter Garden";
            case THANKSGARDEN -> "Thanksgarden";
            case GRASS -> "Grass";
            case MUSHROOM -> "Mushroom";
            case ROCK -> "Rock";
            default -> "Unknown";
          };
      buffer.append("</nobr><br><nobr>");
      buffer.append(" (currently ").append(cropString).append(")");
    }
    buffer.append("</nobr><br>");
  }

  private static final Pattern EUDORA_PATTERN =
      Pattern.compile("<option (selected='selected' )?value=\"(\\d)\">([\\w\\s-]*)");

  private static void switchCorrespondent(final StringBuffer buffer) {
    GenericRequest eudoraCheck = new GenericRequest("account.php?tab=correspondence");
    eudoraCheck.run();
    String response = eudoraCheck.responseText;
    if (!response.contains("Eudora")) {
      // No choices as tab does not exist
      return;
    }

    // have[Eudora] means that it can be switched to, which means
    // it is not currently active
    Matcher matcher = ValhallaDecorator.EUDORA_PATTERN.matcher(response);

    Correspondent activeCorrespondent = Correspondent.NONE;
    ArrayList<Correspondent> availableCorrespondents = new ArrayList<>();

    while (matcher.find()) {
      String name = matcher.group(3);
      Correspondent eudora = Correspondent.find(name);
      if (matcher.group(1) == null) {
        availableCorrespondents.add(eudora);
      } else {
        activeCorrespondent = eudora;
      }
    }

    if (availableCorrespondents.size() == 0) {
      // No choice to make
      return;
    }

    buffer
        .append("<nobr>Eudora: ")
        .append(
            "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"garden\" onchange=\"if (this.value) window.location.href=this.value\">")
        .append("<option value=\"\" style=\"background-color: #eeeeff\">Select one</option>");

    for (Correspondent correspondent : availableCorrespondents) {
      if (correspondent == Correspondent.NONE) {
        continue;
      }

      buffer
          .append("<option style=\"background-color: #eeeeff\" ")
          .append("value=\"/KoLmafia/redirectedCommand?cmd=eudora+")
          .append(correspondent.getSlug())
          .append("&pwd=")
          .append(GenericRequest.passwordHash)
          .append("\">")
          .append(correspondent.getItem())
          .append("</option>");
    }

    buffer
        .append("</select></form>")
        .append("</nobr><br><nobr>")
        .append("(currently ")
        .append(activeCorrespondent.getItem())
        .append(")")
        .append("</nobr><br>");
  }

  private static void switchFolderHolder(StringBuffer buffer) {
    StringBuilder folderHolderBuffer = new StringBuilder();

    if (InventoryManager.getCount(ItemPool.FOLDER_HOLDER)
            + InventoryManager.getEquippedCount(ItemPool.FOLDER_HOLDER)
        == 0) {
      return;
    }

    folderHolderBuffer.append("Folder Holder: ");
    for (var slot : SlotSet.FOLDER_SLOTS_AFTERCORE) {
      AdventureResult folder = EquipmentManager.getEquipment(slot);
      if (folder != null) {
        String name = folder.getName();
        String enchantments;
        if (name.startsWith("folder (")) {
          Modifiers mods = ModifierDatabase.getItemModifiers(folder.getItemId());
          name = name.substring(8, name.indexOf(")"));
          enchantments = mods != null ? mods.getString(StringModifier.MODIFIERS) : "none";
        } else {
          name = "(empty)";
          enchantments = "none";
        }
        folderHolderBuffer
            .append("<nobr><a href=\"inventory.php?action=useholder\" title=\"Change from ")
            .append(enchantments)
            .append("\">")
            .append(name)
            .append("</a></nobr> ");
      }
    }
    folderHolderBuffer.append("<br>");
    buffer.append(folderHolderBuffer);
  }

  private static void checkIceHouse(StringBuffer buffer) {
    StringBuilder iceHouseBuffer = new StringBuilder();

    String monster = BanishManager.getFirstBanished(BanishManager.Banisher.ICE_HOUSE);

    iceHouseBuffer.append(
        "<nobr>Ice House: <a href=\"museum.php?action=icehouse\" title=\"Check ice house monster\">");
    if (monster != null) {
      iceHouseBuffer.append(monster).append(" (currently)");
    } else {
      iceHouseBuffer.append("(none currently)");
    }
    iceHouseBuffer.append("</a></nobr><br />");
    buffer.append(iceHouseBuffer);
  }

  private static void checkCatHeists(StringBuffer buffer) {
    StringBuilder catHeistBuffer = new StringBuilder();

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
      int heistsRemaining = totalHeists + bankHeists - heistsComplete;
      FamiliarData familiar = KoLCharacter.getFamiliar();
      if (familiar.getId() != FamiliarPool.CAT_BURGLAR) {
        catHeistBuffer
            .append("<nobr><a href=\"familiar.php?action=newfam&newfam=267&pwd=")
            .append(GenericRequest.passwordHash);
      } else {
        catHeistBuffer.append("<nobr><a href=\"main.php?heist=1");
      }
      catHeistBuffer
          .append("\">Use remaining Cat Burglar heists (")
          .append(heistsRemaining)
          .append(")</a></nobr><br>");
      buffer.append(catHeistBuffer);
    }
  }

  private static void check2002MrStoreCredit(StringBuffer buffer) {
    StringBuilder storeCreditBuffer = new StringBuilder();

    int credits = Preferences.getInteger("availableMrStore2002Credits");

    if (credits > 0) {
      storeCreditBuffer
          .append(
              "<nobr><a href=\"shop.php?whichshop=mrstore2002\">Spend remaining 2002 Mr. Store Credits (")
          .append(credits)
          .append(")</a></nobr><br>");
      buffer.append(storeCreditBuffer);
    }
  }

  private static void switchChateau(StringBuffer buffer) {
    if (!Preferences.getBoolean("chateauAvailable")) {
      return;
    }

    StringBuilder chateauBuffer = new StringBuilder();

    chateauBuffer.append("Chateau: ");

    for (AdventureResult item : KoLConstants.chateau) {
      chateauBuffer.append(
          "<nobr><a href=\"shop.php?whichshop=chateau\" title=\"Change from giving ");
      switch (item.getItemId()) {
        case ItemPool.CHATEAU_MUSCLE -> chateauBuffer.append("muscle stats when resting");
        case ItemPool.CHATEAU_MYST -> chateauBuffer.append("mysticality stats when resting");
        case ItemPool.CHATEAU_MOXIE -> chateauBuffer.append("moxie stats when resting");
        case ItemPool.CHATEAU_FAN -> chateauBuffer.append("+5 free rests per day");
        case ItemPool.CHATEAU_CHANDELIER -> chateauBuffer.append("+3 PvP fights at rollover");
        case ItemPool.CHATEAU_SKYLIGHT -> chateauBuffer.append("+3 adventures at rollover");
        case ItemPool.CHATEAU_BANK -> chateauBuffer.append("1,000 meat per day");
        case ItemPool.CHATEAU_JUICE_BAR -> chateauBuffer.append("3 random potions per day");
        case ItemPool.CHATEAU_PENS -> chateauBuffer.append("3 fancy calligraphy pens per day");
        default -> chateauBuffer.append("unknown");
      }
      chateauBuffer.append("\">").append(item.getName()).append("</a></nobr> ");
    }

    String monster = Preferences.getString("chateauMonster");
    chateauBuffer
        .append("<br>Chateau monster: ")
        .append("<nobr><a href=\"place.php?whichplace=chateau\" title=\"Check painted monster\">");
    if (monster.equals("")) {
      chateauBuffer.append("(none currently)");
    } else {
      chateauBuffer.append(monster).append(" (currently)");
    }
    chateauBuffer.append("</a></nobr><br>");

    buffer.append(chateauBuffer);
  }

  private static void switchCowboyBoots(StringBuffer buffer) {
    if (InventoryManager.getCount(ItemPool.COWBOY_BOOTS)
            + InventoryManager.getEquippedCount(ItemPool.COWBOY_BOOTS)
        == 0) {
      return;
    }

    StringBuilder cowboyBootsBuffer = new StringBuilder();

    AdventureResult skin = EquipmentManager.getEquipment(Slot.BOOTSKIN);
    AdventureResult spurs = EquipmentManager.getEquipment(Slot.BOOTSPUR);

    cowboyBootsBuffer
        .append("<nobr>Cowboy Boot skin: ")
        .append(
            "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"cowboy_boots_skin\" onchange=\"if (this.value) window.location.href=this.value\">")
        .append("<option value=\"\" style=\"background-color: #eeeeff\">Apply one</option>");

    for (int i = ItemPool.MOUNTAIN_SKIN; i <= ItemPool.ROTTING_SKIN; i++) {
      if (InventoryManager.hasItem(i) && !(skin != null && skin.getItemId() == i)) {
        String name = ItemDatabase.getItemName(i);
        cowboyBootsBuffer
            .append("<option style=\"background-color: #eeeeff\" ")
            .append("title=\"")
            .append(ValhallaDecorator.tooltip(i))
            .append("\" value=\"/KoLmafia/redirectedCommand?cmd=acquire+")
            .append(name.replaceAll(" ", "+"))
            .append(";+use+")
            .append(name.replaceAll(" ", "+"))
            .append("&pwd=")
            .append(GenericRequest.passwordHash)
            .append("\">")
            .append(name)
            .append("</option>");
      }
    }
    cowboyBootsBuffer.append("</select></form>");

    if (skin != null && skin != EquipmentRequest.UNEQUIP) {
      cowboyBootsBuffer
          .append("</nobr><br><nobr>")
          .append("(currently <span title=\"")
          .append(ValhallaDecorator.tooltip(skin.getItemId()))
          .append("\">")
          .append(skin.getName())
          .append("</span>)");
    } else {
      cowboyBootsBuffer.append("(none currently)");
    }

    cowboyBootsBuffer
        .append("</a></nobr><br>")
        .append("<nobr>Cowboy Boot spurs: ")
        .append(
            "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"cowboy_boots_spurs\" onchange=\"if (this.value) window.location.href=this.value\">")
        .append("<option value=\"\" style=\"background-color: #eeeeff\">Apply one</option>");

    for (int i = ItemPool.QUICKSILVER_SPURS; i <= ItemPool.TICKSILVER_SPURS; i++) {
      if (InventoryManager.hasItem(i) && !(spurs != null && spurs.getItemId() == i)) {
        String name = ItemDatabase.getItemName(i);
        cowboyBootsBuffer
            .append("<option style=\"background-color: #eeeeff\" ")
            .append("title=\"")
            .append(ValhallaDecorator.tooltip(i))
            .append("\" value=\"/KoLmafia/redirectedCommand?cmd=acquire+")
            .append(name.replaceAll(" ", "+"))
            .append(";+use+")
            .append(name.replaceAll(" ", "+"))
            .append("&pwd=")
            .append(GenericRequest.passwordHash)
            .append("\">")
            .append(name)
            .append("</option>");
      }
    }
    cowboyBootsBuffer.append("</select></form>");

    if (spurs != null && spurs != EquipmentRequest.UNEQUIP) {
      cowboyBootsBuffer
          .append("</nobr><br><nobr>")
          .append("(currently <span title=\"")
          .append(ValhallaDecorator.tooltip(spurs.getItemId()))
          .append("\">")
          .append(spurs.getName())
          .append("</span>)");
    } else {
      cowboyBootsBuffer.append("(none currently)");
    }

    cowboyBootsBuffer.append("</a></nobr><br>");

    buffer.append(cowboyBootsBuffer);
  }

  private static String tooltip(int itemId) {
    return switch (itemId) {
      case ItemPool.MOUNTAIN_SKIN -> "+50% Moxie";
      case ItemPool.GRIZZLED_SKIN -> "+50% Muscle";
      case ItemPool.DIAMONDBACK_SKIN -> "+20 Monster Level";
      case ItemPool.COAL_SKIN -> "Cowboy Kick does Spooky Damage";
      case ItemPool.FRONTWINDER_SKIN -> "+50% Mysticality";
      case ItemPool.ROTTING_SKIN -> "Cowboy Kick does 15% delevel, plus damage from Cowrruption";
      case ItemPool.QUICKSILVER_SPURS -> "+30% Initiative";
      case ItemPool.THICKSILVER_SPURS -> "+2 All Elemental Resistance";
      case ItemPool.WICKSILVER_SPURS -> "Cowboy Kick does Hot Damage";
      case ItemPool.SLICKSILVER_SPURS -> "Cowboy Kick does Sleaze Damage";
      case ItemPool.SICKSILVER_SPURS -> "Cowboy Kick does Stench Damage";
      case ItemPool.NICKSILVER_SPURS -> "+20% Item Drop";
      case ItemPool.TICKSILVER_SPURS -> "+5 Adventures";
      default -> "";
    };
  }

  private static void collectUntradablePulls(StringBuffer buffer) {
    record PotentialPull(int itemId, int desiredAmount, String url) {
      PotentialPull(int itemId, int desiredAmount) {
        this(itemId, desiredAmount, null);
      }
    }
    var pulls =
        List.of(
            new PotentialPull(ItemPool.SHIP_TRIP_SCRIP, 4, "adventure.php?snarfblat=355"),
            new PotentialPull(ItemPool.BORIS_BREAD, 3),
            new PotentialPull(ItemPool.ROASTED_VEGETABLE_OF_JARLSBERG, 3),
            new PotentialPull(ItemPool.PETES_RICH_RICOTTA, 3),
            new PotentialPull(ItemPool.ROASTED_VEGETABLE_FOCACCIA, 3),
            new PotentialPull(ItemPool.PLAIN_CALZONE, 3),
            new PotentialPull(ItemPool.BAKED_VEGGIE_RICOTTA_CASSEROLE, 3),
            new PotentialPull(ItemPool.DEEP_DISH_OF_LEGEND, 1),
            new PotentialPull(ItemPool.CALZONE_OF_LEGEND, 1),
            new PotentialPull(ItemPool.PIZZA_OF_LEGEND, 1));

    boolean showPulls = false;

    var pullBuffer = new StringBuilder();
    pullBuffer
        .append("<br><b>Collect Untradable Pulls</b>")
        .append("<ul style=\"list-style-type:none;padding-inline:0;\">");

    for (var pull : pulls) {
      var countItem = InventoryManager.getCount(pull.itemId);
      if (countItem >= pull.desiredAmount) {
        continue;
      }
      showPulls = true;
      var itemName = ItemDatabase.getItemName(pull.itemId);
      var link =
          pull.url != null
              ? pull.url
              : "/KoLmafia/redirectedCommand?cmd=acquire+"
                  + pull.desiredAmount
                  + "+"
                  + StringUtilities.getURLEncode(itemName)
                  + "&pwd="
                  + GenericRequest.passwordHash;
      pullBuffer
          .append("<li>")
          .append("<a href=\"")
          .append(link)
          .append("\">")
          .append(itemName)
          .append(" (")
          .append(countItem)
          .append("/")
          .append(pull.desiredAmount)
          .append(")")
          .append("</li>");
    }

    pullBuffer.append("</ul>");
    if (showPulls) {
      buffer.append(pullBuffer);
    }
  }
}
