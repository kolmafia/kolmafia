package net.sourceforge.kolmafia;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarData implements Comparable<FamiliarData> {
  public static final FamiliarData NO_FAMILIAR = new FamiliarData(-1);

  // <center>Current Familiar:<br><img onClick='fam(265)'
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/pokefam8675309.gif"
  // width=30 height=30 border=0><br><b>Bodolph</b><br>9-pound Mu (86 experience, 3,184
  // kills)<table><tr><td valign=center>Equipment:</td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/ast_sweater.gif"
  // class=hand onClick='descitem(648386354)'></td><td valign=center>astral pet sweater <font
  // size=1><a
  // href='inv_equip.php?pwd=930f51c25a0adce026fb5b3a9d34172b&action=unequip&type=familiarequip&terrarium=1'>[unequip]</a></font></td><td><a href='familiar.php?action=lockequip&pwd=930f51c25a0adce026fb5b3a9d34172b'><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/openpadlock.gif" class=hand title='This Familiar Equipment is Unlocked'></a></td><td valign=top><font size=-1><b><a class=nounder href='javascript:doc("famequiplock");'>?</a></b></font></td></tr></table><p><form name=rename action=familiar.php method=post><input type=hidden name=action value="rename"><input type=hidden name=pwd value='930f51c25a0adce026fb5b3a9d34172b'>Change your Familiar's Name:<br><input class=text type=text size=40 maxlength=40 name=newname value="Bodolph"> <input class=button type=submit value="Rename"></form>

  // <center>Current Familiar:<br><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/righthandbody.png"
  // width=30 height=30 border=0><br><b>Manny Frashins</b><br>1-pound Left-Hand Man (0 experience, 0
  // kills)<p><form name=rename action=familiar.php method=post><input type=hidden name=action
  // value="rename"><input type=hidden name=pwd value='d6bcda5f6ff05d0c71b6ccd1702dbacd'>Change your
  // Familiar's Name:<br><input class=text type=text size=40 maxlength=40 name=newname value="Manny
  // Frashins"> <input class=button type=submit value="Rename"></form>

  // <center>Current Familiar:<br><div style="position: relative; height: 50px; width: 30px"><img
  // onClick='fam(278)' style="position: absolute; left: -2px; bottom: 25px;"
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/uvcompass.gif" width=30
  // height=30 border=0 ><img style="position: absolute; left: 0; bottom: 0;"
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/righthandbody.png"
  // width=30 height=30 border=0></div><br><b>Manny Frashins</b><br>1-pound Left-Hand Man (0
  // experience, 0 kills)<table><tr><td valign=center>Equipment:</td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/uvcompass.gif" class=hand
  // onClick='descitem(323401194)'></td><td valign=center>UV-resistant compass <font size=1><a
  // href='inv_equip.php?pwd=e7baebb793602dd11b8d966f095336c4&action=unequip&type=familiarequip&terrarium=1'>[unequip]</a></font></td></tr></table><p><form name=rename action=familiar.php method=post><input type=hidden name=action value="rename"><input type=hidden name=pwd value='e7baebb793602dd11b8d966f095336c4'>Change your Familiar's Name:<br><input class=text type=text size=40 maxlength=40 name=newname value="Manny Frashins"> <input class=button type=submit value="Rename"></form>

  // <center>Current Familiar:<br><span style="display: block; position: relative; height: 30px;
  // width: 55px" onClick='fam(279)'><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/camelfam_left.gif"
  // width=20 height=30 border=0><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/camelfam_right.gif"
  // width=25 height=30 border=0></span><br><b>Gogarth</b><br>1-pound Melodramedary (0 experience, 0
  // kills)<table><tr><td valign=center>Equipment:</td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/hookah.gif" class=hand
  // onClick='descitem(279445768)'></td><td valign=center>ittah bittah hookah <font size=1><a
  // href='inv_equip.php?pwd=276c8f0c21e1b2a96a410d3a6ebd34d0&action=unequip&type=familiarequip&terrarium=1'>[unequip]</a></font></td><td><a href='familiar.php?action=lockequip&pwd=276c8f0c21e1b2a96a410d3a6ebd34d0'><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/openpadlock.gif" class=hand title='This Familiar Equipment is Unlocked'></a></td><td valign=top><font size=-1><b><a class=nounder href='javascript:doc("famequiplock");'>?</a></b></font></td></tr></table><p><form name=rename action=familiar.php method=post><input type=hidden name=action value="rename"><input type=hidden name=pwd value='276c8f0c21e1b2a96a410d3a6ebd34d0'>Change your Familiar's Name:<br><input class=text type=text size=40 maxlength=40 name=newname value="Gogarth"> <input class=button type=submit value="Rename"></form>

  private static final Pattern CURRENT_PATTERN = Pattern.compile("Current Familiar:.*?</form>");

  // 1=id 2=image 3=name 4=race 5=exp 6=kills 7=extra
  private static final Pattern CURRENT_FAMILIAR_PATTERN =
      Pattern.compile(
          ".*?(?:onClick='fam\\((\\d+)\\)')? .*?src=\"[^>]*?(?:images.kingdomofloathing.com|/images)/(?:item|other)images/([^\"]*?)\".*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, ([\\d,]+) kills?\\)(.*?)(?:</form)>");

  private static final Pattern FROW_PATTERN = Pattern.compile("<tr class=\"frow .*?</tr>");

  // <tr class="frow " data-meat="1" data-other="1"><td valign=center><input type=radio name=newfam
  // value=211></td><td valign=center><img onClick='fam(211)'
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/robotender.gif" width=30
  // height=30 border=0></td><td valign=top style='padding-top: .45em;'><b>Frudolph</b>, the
  // 20-pound Robortender (26,336 exp, 81,162 kills) <font size="1"><br />&nbsp;&nbsp;&nbsp;&nbsp;<a
  // class="fave"
  // href="familiar.php?&action=hatseat&famid=211&pwd=4dffb982670edd69a851735088639201">[put in
  // Crown of Thrones]</a>&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?group=0&action=fave&famid=211&pwd=4dffb982670edd69a851735088639201">[unfavorite]</a>&nbsp;&nbsp;<a class="fave" href="familiar.php?&action=newfam&newfam=211&pwd=4dffb982670edd69a851735088639201">[take with you]</a></font></td><td valign=center nowrap><center><b>(</b><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/toggleswitch2.gif" class=hand onClick='descitem(431660273)' align=middle><b>)</b><br><font size=1><a href='familiar.php?pwd=4dffb982670edd69a851735088639201&action=unequip&famid=211'>[unequip]</a></font></center></td></tr>

  // <tr class="frow " data-attack="1" data-other="1"><td valign=center><input type=radio
  // name=newfam value=278></td><td valign=center><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/righthandbody.png"
  // width=30 height=30 border=0></td><td valign=top style='padding-top: .45em;'><b>Manny
  // Frashins</b>, the 14-pound Left-Hand Man (210 exp, 384 kills) <font size="1"><br
  // />&nbsp;&nbsp;&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?group=0&action=fave&famid=278&pwd=e7d9515eca111b1c36980671f7d1312c">[unfavorite]</a>&nbsp;&nbsp;<a class="fave" href="familiar.php?&action=newfam&newfam=278&pwd=e7d9515eca111b1c36980671f7d1312c">[take with you]</a></font></td></tr>

  // <tr class="frow " data-stats="1" data-hp_restore="1" data-mp_restore="1" data-other="1"><td
  // valign=center><input type=radio name=newfam value=279></td><td valign=center><span
  // style="display: block; position: relative; height: 30px; width: 55px" onClick='fam(279)'><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/camelfam_left.gif"
  // width=20 height=30 border=0><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/camelfam_right.gif"
  // width=25 height=30 border=0></span></td><td valign=top style='padding-top:
  // .45em;'><b>Gogarth</b>, the 1-pound Melodramedary (0 exp, 0 kills) <font size="1"><br
  // />&nbsp;&nbsp;&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?group=0&action=fave&famid=279&pwd=e7d9515eca111b1c36980671f7d1312c">[unfavorite]</a>&nbsp;&nbsp;<a class="fave" href="familiar.php?&action=newfam&newfam=279&pwd=e7d9515eca111b1c36980671f7d1312c">[take with you]</a></font></td></tr>

  // 1=id 2=image 3=name 4=race 5=exp 6=kills 7=extra
  private static final Pattern FAMILIAR_PATTERN =
      Pattern.compile(
          ".*?name=newfam value=(\\d+).*?<img .*?src=\"[^>]*?/(?:item|other)images/([^\"]*?)\".*?>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, ([\\d,]+) kills?\\)(.*?)(?:</tr>)");

  // <tr class="frow " data-stats="1" data-meat="1" data-items="1"><td valign=center>&nbsp;</td><td
  // valign=center><img onClick='fam(192)'
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/goldmonkey.gif" width=30
  // height=30 border=0></td><td valign=top style='padding-top: .45em;'><b>Ignominious Uncguary</b>,
  // the 1-pound Golden Monkey (0 exp, 7,832 kills) <font size="1"><br />&nbsp;&nbsp;&nbsp;&nbsp;<a
  // class="fave"
  // href="familiar.php?&action=hatseat&famid=192&pwd=44a0b1bb745243778eb07661a35b0956">[put in
  // Crown of Thrones]</a>&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?&action=backpack&famid=0&pwd=44a0b1bb745243778eb07661a35b0956">[kick out of
  // Buddy Bjorn]</a>&nbsp;&nbsp;<a class="fave"
  // href="familiar.php?group=0&action=fave&famid=192&pwd=44a0b1bb745243778eb07661a35b0956">[unfavorite]</a>&nbsp;&nbsp;<a class="fave" href="familiar.php?&action=newfam&newfam=192&pwd=44a0b1bb745243778eb07661a35b0956">[take with you]</a></font></td><td valign=center nowrap><center><b>(</b><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/goldbanana.gif" class=hand onClick='descitem(986943479)' align=middle><b>)</b><br><font size=1><a href='familiar.php?pwd=44a0b1bb745243778eb07661a35b0956&action=unequip&famid=192'>[unequip]</a></font></center></td></tr>

  // 1=id 2=image 3=name 4=race 5=exp 6=kills 7=extra
  private static final Pattern RIDER_PATTERN =
      Pattern.compile(
          ".*'fam\\((\\d+)\\)' src=\"[^>]*?/(?:item|other)images/([^\"]*?)\".*?>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (?:exp|experience|candy|candies)?, ([\\d,]+) kills?\\)(.*?)(?:</tr>)");

  // <table><tr><td>In Your Crown of Thrones:</td><td><img onClick='fam(159)'
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/medium_0.gif" width=30
  // height=30 border=0></td><td><b>Psychic Grrl</b></td><td>&nbsp;&nbsp;<a
  // href="familiar.php?&action=hatseat&famid=0&pwd=44a0b1bb745243778eb07661a35b0956"><font
  // size=1>[kick out]</font></a></td></tr></table>
  // <table><tr><td>In Your Buddy Bjorn:</td><td><img onClick='fam(192)'
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/goldmonkey.gif" width=30
  // height=30 border=0></td><td><b>Ignominious Uncguary</b></td><td>&nbsp;&nbsp;<a
  // href="familiar.php?&action=backpack&famid=0&pwd=44a0b1bb745243778eb07661a35b0956"><font
  // size=1>[kick out]</font></a></td></tr></table>

  private static final Pattern DESCID_PATTERN = Pattern.compile("descitem\\((.*?)\\)");
  private static final Pattern SHRUB_TOPPER_PATTERN = Pattern.compile("span title=\"(.*?)-heavy");
  private static final Pattern SHRUB_LIGHT_PATTERN = Pattern.compile("Deals (.*?) damage");

  public static final AdventureResult BATHYSPHERE = ItemPool.get(ItemPool.BATHYSPHERE, 1);
  public static final AdventureResult DAS_BOOT = ItemPool.get(ItemPool.DAS_BOOT, 1);
  public static final AdventureResult DOPPELGANGER =
      ItemPool.get(ItemPool.FAMILIAR_DOPPELGANGER, 1);
  public static final AdventureResult FIREWORKS = ItemPool.get(ItemPool.FIREWORKS, 1);
  public static final AdventureResult FLOWER_BOUQUET = ItemPool.get(ItemPool.MAYFLOWER_BOUQUET, 1);
  public static final AdventureResult ITTAH_BITTAH_HOOKAH =
      ItemPool.get(ItemPool.ITTAH_BITTAH_HOOKAH, 1);
  public static final AdventureResult LEAD_NECKLACE = ItemPool.get(ItemPool.LEAD_NECKLACE, 1);
  public static final AdventureResult LIFE_PRESERVER =
      ItemPool.get(ItemPool.MINI_LIFE_PRESERVER, 1);
  public static final AdventureResult MOVEABLE_FEAST = ItemPool.get(ItemPool.MOVEABLE_FEAST, 1);
  public static final AdventureResult PET_SWEATER = ItemPool.get(ItemPool.PET_SWEATER, 1);
  public static final AdventureResult PUMPKIN_BUCKET = ItemPool.get(ItemPool.PUMPKIN_BUCKET, 1);
  public static final AdventureResult RAT_HEAD_BALLOON = ItemPool.get(ItemPool.RAT_BALLOON, 1);
  public static final AdventureResult SUGAR_SHIELD = ItemPool.get(ItemPool.SUGAR_SHIELD, 1);
  public static final AdventureResult LOATHING_LEGION_HELICOPTER =
      ItemPool.get(ItemPool.LOATHING_LEGION_HELICOPTER, 1);
  public static final AdventureResult FILTHY_CHILD_LEASH =
      ItemPool.get(ItemPool.FILTHY_CHILD_LEASH, 1);
  public static final AdventureResult LUCK_INCENSE = ItemPool.get(ItemPool.LUCK_INCENSE, 1);
  public static final AdventureResult SHELL_BELL = ItemPool.get(ItemPool.SHELL_BELL, 1);
  public static final AdventureResult MUSCLE_BAND = ItemPool.get(ItemPool.MUSCLE_BAND, 1);
  public static final AdventureResult AMULET_COIN = ItemPool.get(ItemPool.AMULET_COIN, 1);
  public static final AdventureResult RAZOR_FANG = ItemPool.get(ItemPool.RAZOR_FANG, 1);
  public static final AdventureResult SMOKE_BALL = ItemPool.get(ItemPool.SMOKE_BALL, 1);

  public static final List<DropInfo> DROP_FAMILIARS = new ArrayList<DropInfo>();
  public static final List<FightInfo> FIGHT_FAMILIARS = new ArrayList<FightInfo>();
  public static final List<Integer> CRIMBO_GHOSTS =
      Arrays.asList(
          FamiliarPool.GHOST_CAROLS, FamiliarPool.GHOST_CHEER, FamiliarPool.GHOST_COMMERCE);

  private final int id;
  private final String race;
  private boolean beeware;
  private boolean glover;
  private String name;
  private int experience;
  private int weight;
  private AdventureResult item;
  private boolean feasted;
  private boolean favorite;
  private int charges;
  private int pokeLevel;

  public FamiliarData(final int id) {
    this(id, "", 1, EquipmentRequest.UNEQUIP);
  }

  public FamiliarData(
      final int id, final String name, final int weight, final AdventureResult item) {
    this.id = id;
    this.name = name;
    String race = FamiliarDatabase.getFamiliarName(id);
    this.race = (id == -1 || race == null) ? "(none)" : race;
    this.beeware = this.race.contains("b") || this.race.contains("B");
    this.glover = this.race.contains("g") || this.race.contains("G");

    this.weight = weight;
    this.item = item;
    this.feasted = false;
    this.charges = 0;
    this.pokeLevel = 0;
  }

  private FamiliarData(final Matcher dataMatcher, boolean idFirst) {
    this.race = dataMatcher.group(4);
    String idString = dataMatcher.group(idFirst ? 1 : 2);
    this.id =
        idString != null
            ? StringUtilities.parseInt(idString)
            : FamiliarDatabase.getFamiliarId(this.race, false);
    this.beeware = this.race.contains("b") || this.race.contains("B");
    this.glover = this.race.contains("g") || this.race.contains("G");

    String image = dataMatcher.group(idFirst ? 2 : 1);
    FamiliarDatabase.registerFamiliar(this.id, this.race, image);

    this.update(dataMatcher);
  }

  public FamiliarData(final int id, final String name, final int pokeLevel) {
    this.id = id;
    this.name = name;
    this.race = FamiliarDatabase.getFamiliarName(id);
    this.pokeLevel = pokeLevel;
  }

  private void update(final Matcher dataMatcher) {
    this.name = dataMatcher.group(3);
    this.experience = StringUtilities.parseInt(dataMatcher.group(5));
    this.setWeight();
    // dataMatcher.group( 6 ) => kills
    String itemData = dataMatcher.group(7);
    this.item = FamiliarData.parseFamiliarItem(this.id, itemData);
    this.favorite = itemData.contains("[unfavorite]");
  }

  public final void update(final String name, final int pokeLevel) {
    this.name = name;
    this.pokeLevel = pokeLevel;
  }

  public static final void reset() {
    FamiliarData.loadDropFamiliars();
    FamiliarData.loadFightFamiliars();
    FamiliarData.checkShrub();
  }

  public final boolean canEquip() {
    // If you are in Pokefam, you can use your familiars in a
    // PokeTeam, but you cannot take one out of your terrarium in
    // the usual way
    if (KoLCharacter.inPokefam()) {
      return false;
    }

    // Familiars cannot be equipped by most Avatar classes
    if (KoLCharacter.inAxecore()
        || KoLCharacter.isJarlsberg()
        || KoLCharacter.isSneakyPete()
        || KoLCharacter.isEd()
        || KoLCharacter.inBondcore()
        || KoLCharacter.isVampyre()) {
      return false;
    }

    // Familiars with a "B" in their race cannot be equipped in Beecore
    if (KoLCharacter.inBeecore() && this.beeware) {
      return false;
    }

    // Familiars without a "G" in their race cannot be equipped in G-Lover
    if (KoLCharacter.inGLover() && !this.glover) {
      return false;
    }

    // Familiars are only allowed with the right hat in You, Robot
    if (KoLCharacter.inRobocore() && Preferences.getInteger("youRobotTop") != 2) {
      return false;
    }

    // Unallowed familiars cannot be equipped
    if (!StandardRequest.isAllowed("Familiars", this.race)) {
      return false;
    }

    return true;
  }

  public final int getTotalExperience() {
    return this.experience;
  }

  public final void addCombatExperience(String responseText) {
    if (this.id == FamiliarPool.STOCKING_MIMIC) {
      // Doesn't automatically gain experience from winning a combat
      return;
    }

    double experienceModifier = KoLCharacter.currentNumericModifier(Modifiers.FAMILIAR_EXP);

    int itemId = getItem().getItemId();
    if (itemId == ItemPool.MAYFLOWER_BOUQUET) {
      String modifierName = Modifiers.getModifierName(Modifiers.FAMILIAR_EXP);
      double itemModifier = Modifiers.getNumericModifier("Item", itemId, modifierName);

      experienceModifier -= itemModifier;

      if (responseText.contains("offer some words of encouragement and support")) {
        experienceModifier += 3;
      }
    }

    int exp =
        (1
            + (int) experienceModifier
            + (KoLCharacter.hasSkill("Testudinal Teachings") ? determineTestTeachExperience() : 0));

    setExperience(this.experience + exp);
  }

  public final void addNonCombatExperience(int exp) {
    setExperience(this.experience + exp);
  }

  public final void setExperience(int exp) {
    if (CRIMBO_GHOSTS.contains(this.getId())) {
      for (Integer ghostId : CRIMBO_GHOSTS) {
        FamiliarData fam = KoLCharacter.findFamiliar(ghostId);
        if (fam != null) {
          fam.experience = exp;
          fam.setWeight();
        }
      }
    } else {
      this.experience = exp;
      this.setWeight();
    }
  }

  public final int determineTestTeachExperience() {
    String rawTTPref = Preferences.getString("testudinalTeachings");
    String[] splitTTPref = rawTTPref.split("\\|");

    // Check Familiar Testudinal Teachings experience
    for (int i = 0; i < splitTTPref.length; ++i) {
      String[] it = splitTTPref[i].split(":");
      if (it.length == 2) {
        if (this.id == Integer.parseInt(it[0])) {
          int testTeachExp = 0;

          int newCount = Integer.parseInt(it[1]) + 1;
          if (newCount >= 6) {
            testTeachExp++;
            newCount = 0;
          }
          String newTTProperty = it[0] + ":" + newCount;
          String newTTPref =
              StringUtilities.globalStringReplace(rawTTPref, splitTTPref[i], newTTProperty);
          Preferences.setString("testudinalTeachings", newTTPref);
          return testTeachExp;
        }
      }
    }

    // Familiar not found, so add it
    String delimiter = "";
    if (rawTTPref.length() > 0) {
      delimiter = "|";
    }
    String newTTPref = rawTTPref + delimiter + this.id + ":1";
    Preferences.setString("testudinalTeachings", newTTPref);

    return 0;
  }

  public final void recognizeCombatUse() {
    int singleFamiliarRun = getSingleFamiliarRun();

    if (singleFamiliarRun == 0) {
      Preferences.setInteger("singleFamiliarRun", this.id);
    } else if (this.id != singleFamiliarRun) {
      Preferences.setInteger("singleFamiliarRun", -1);
    }
  }

  public final boolean isUnexpectedFamiliar() {
    if (this.id == -1 && KoLCharacter.getCurrentRun() == 0) {
      return true;
    }

    int singleFamiliarRun = getSingleFamiliarRun();

    return singleFamiliarRun > 0 && this.id != singleFamiliarRun;
  }

  public static final int getSingleFamiliarRun() {
    int singleFamiliarRun = Preferences.getInteger("singleFamiliarRun");

    if (singleFamiliarRun == 0) {
      for (FamiliarData familiar : KoLCharacter.getFamiliarList()) {
        if (familiar.getTotalExperience() != 0) {
          if (singleFamiliarRun != 0) {
            singleFamiliarRun = -1;
            break;
          }

          singleFamiliarRun = familiar.getId();
        }
      }

      Preferences.setInteger("singleFamiliarRun", singleFamiliarRun);
    }

    return singleFamiliarRun;
  }

  public final int getMaxBaseWeight() {
    if (this.id == FamiliarPool.STOCKING_MIMIC || this.id == FamiliarPool.HOMEMADE_ROBOT) {
      return 100;
    }

    if (CRIMBO_GHOSTS.contains(this.id)) {
      return 40;
    }

    return 20;
  }

  private void setWeight() {
    this.weight = Math.max(Math.min(this.getMaxBaseWeight(), (int) Math.sqrt(this.experience)), 1);
  }

  public final void checkWeight(final int weight, final boolean feasted) {
    // Called from CharPaneRequest with KoL's idea of current familiar's weight and "well-fed"
    // status.
    // This does NOT include "hidden" weight modifiers

    // Sanity check: don't adjust NO_FAMILIAR
    if (this.id == -1) {
      return;
    }

    this.feasted = feasted;

    // Get modified weight excluding hidden weight modifiers
    int delta = weight - this.getModifiedWeight(false, true);
    if (delta != 0) {
      // The following is informational, not an error, but it confuses people, so don't print it.
      // RequestLogger.printLine( "Adjusting familiar weight by " + delta + " pound" + ( delta == 1
      // ? "" : "s" ) );
      this.weight += delta;
    }
  }

  public final void setName(final String name) {
    this.name = name;
  }

  private static AdventureResult parseFamiliarItem(final int id, final String text) {
    if (!text.contains("<img")) {
      return EquipmentRequest.UNEQUIP;
    }

    Matcher itemMatcher = DESCID_PATTERN.matcher(text);
    if (!itemMatcher.find()) {
      return EquipmentRequest.UNEQUIP;
    }

    String itemName = ItemDatabase.getItemName(itemMatcher.group(1));
    if (itemName == null) {
      return EquipmentRequest.UNEQUIP;
    }

    return ItemPool.get(itemName, 1);
  }

  public static final void registerFamiliarData(final String responseText) {
    // Assume he has no familiar
    FamiliarData current = FamiliarData.NO_FAMILIAR;

    if (!responseText.contains("You do not currently have a familiar")) {
      Matcher currentMatcher = FamiliarData.CURRENT_PATTERN.matcher(responseText);
      if (currentMatcher.find()) {
        Matcher familiarMatcher =
            FamiliarData.CURRENT_FAMILIAR_PATTERN.matcher(currentMatcher.group());
        if (familiarMatcher.find()) {
          current = FamiliarData.registerFamiliar(familiarMatcher, true);
          // There's no indication of whether your current familiar is a
          // favorite or not.  Safest to assume it is:
          current.setFavorite(true);
        }
      }
    }

    Matcher frowMatcher = FamiliarData.FROW_PATTERN.matcher(responseText);
    while (frowMatcher.find()) {
      String frow = frowMatcher.group();
      if (frow.contains("\"frow expired\"")) {
        continue;
      }

      Matcher familiarMatcher = FamiliarData.FAMILIAR_PATTERN.matcher(frow);
      if (familiarMatcher.find()) {
        FamiliarData familiar = FamiliarData.registerFamiliar(familiarMatcher, true);
        continue;
      }

      familiarMatcher = FamiliarData.RIDER_PATTERN.matcher(frow);
      if (familiarMatcher.find()) {
        FamiliarData familiar = FamiliarData.registerFamiliar(familiarMatcher, true);
        if (frow.contains("kick out of Crown of Thrones")) {
          KoLCharacter.setEnthroned(familiar);
        } else if (frow.contains("kick out of Buddy Bjorn")) {
          KoLCharacter.setBjorned(familiar);
        }
      }
    }

    int currentId = current.getId();
    if (currentId == FamiliarPool.REANIMATOR && currentId != KoLCharacter.getFamiliar().getId()) {
      // Visit chat to familiar page to get current parts
      KoLmafia.updateDisplay(
          "Getting current parts information for "
              + current.getName()
              + " the "
              + current.getRace()
              + ".");
      RequestThread.postRequest(new GenericRequest("main.php?talktoreanimator=1"));
    }

    KoLCharacter.setFamiliar(current);
    EquipmentManager.setEquipment(EquipmentManager.FAMILIAR, current.getItem());
    FamiliarData.checkLockedItem(responseText);
  }

  private static FamiliarData registerFamiliar(final Matcher matcher, boolean idFirst) {
    String race = matcher.group(4);
    FamiliarData familiar = KoLCharacter.findFamiliar(race);
    if (familiar == null) {
      // Add new familiar to list
      familiar = new FamiliarData(matcher, idFirst);
      KoLCharacter.addFamiliar(familiar);
    } else {
      // Update existing familiar
      familiar.update(matcher);
    }
    return familiar;
  }

  public static final FamiliarData registerFamiliar(final int id, final int experience) {
    if (id == 0) {
      return FamiliarData.NO_FAMILIAR;
    }

    FamiliarData familiar = KoLCharacter.findFamiliar(id);
    if (familiar == null) {
      // Add new familiar to list
      familiar = new FamiliarData(id);
      KoLCharacter.addFamiliar(familiar);
    }

    familiar.experience = experience;
    familiar.setWeight();
    return familiar;
  }

  public static final FamiliarData registerFamiliar(
      final int id, final String name, final int pokeLevel) {
    if (id == 0) {
      return FamiliarData.NO_FAMILIAR;
    }

    FamiliarData familiar = KoLCharacter.findFamiliar(id);
    if (familiar == null) {
      // Add new familiar to list
      familiar = new FamiliarData(id);
      familiar.update(name, pokeLevel);
      KoLCharacter.addFamiliar(familiar);
    } else {
      // Update existing familiar
      familiar.update(name, pokeLevel);
    }

    return familiar;
  }

  private static final Pattern LOCK_PATTERN =
      Pattern.compile(
          "familiar.php\\?action=lockequip.*'This Familiar Equipment is (Locked|Unlocked)'");

  public static final void checkLockedItem(final String responseText) {
    Matcher lockMatcher = FamiliarData.LOCK_PATTERN.matcher(responseText);
    boolean locked = lockMatcher.find() && lockMatcher.group(1).equals("Locked");

    EquipmentManager.lockFamiliarItem(locked);
  }

  public int getId() {
    return this.id;
  }

  public boolean getFeasted() {
    return this.feasted;
  }

  public void setItem(final AdventureResult item) {
    if (this.id < 1) {
      return;
    }

    if (this.item != null && item != null && this.item.getItemId() == item.getItemId()) {
      return;
    }

    if (!KoLmafia.isRefreshing() && this.item != null && this.item != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, this.item.getInstance(1));
    }

    if (item != null && item != EquipmentRequest.UNEQUIP) {
      this.item = item.getInstance(1);
    } else {
      this.item = item;
    }

    if (!KoLmafia.isRefreshing() && item != null && item != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, item.getInstance(-1));
    }

    if (!KoLmafia.isRefreshing()) {
      switch (this.id) {
        case FamiliarPool.HATRACK:
          // Mad Hatrack
          EquipmentManager.updateEquipmentList(EquipmentManager.HAT);
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
          break;

        case FamiliarPool.HAND:
          // Disembodied Hand
          EquipmentManager.updateEquipmentList(EquipmentManager.WEAPON);
          EquipmentManager.updateEquipmentList(EquipmentManager.OFFHAND);
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
          break;

        case FamiliarPool.LEFT_HAND:
          // Left-Hand Man
          EquipmentManager.updateEquipmentList(EquipmentManager.OFFHAND);
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
          break;

        case FamiliarPool.SCARECROW:
          // Fancypants Scarecrow
          EquipmentManager.updateEquipmentList(EquipmentManager.PANTS);
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
          break;

        default:
          // Everything else
          EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
          break;
      }
      EquipmentManager.lockFamiliarItem();
    }
  }

  public AdventureResult getItem() {
    return this.item == null ? EquipmentRequest.UNEQUIP : this.item;
  }

  public void setWeight(final int weight) {
    this.weight = weight;
  }

  public int getWeight() {
    return this.weight;
  }

  public int getModifiedWeight() {
    return this.getModifiedWeight(true, true);
  }

  public int getModifiedWeight(final boolean includeEquipment) {
    return this.getModifiedWeight(true, includeEquipment);
  }

  private int getModifiedWeight(final boolean includeHidden, final boolean includeEquipment) {
    // Start with base weight of familiar
    int weight = this.weight;

    // Get current fixed and percent weight modifiers
    Modifiers current = KoLCharacter.getCurrentModifiers();
    double fixed = current.get(Modifiers.FAMILIAR_WEIGHT);
    double hidden = current.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
    double percent = current.get(Modifiers.FAMILIAR_WEIGHT_PCT);

    FamiliarData familiar = KoLCharacter.getFamiliar();

    // If this is not the current familiar or we are not
    // considering equipment, subtract weight granted by equipment
    if (this != familiar || !includeEquipment) {
      // Subtract modifiers for current familiar's equipment
      AdventureResult item = familiar.getItem();
      if (item != EquipmentRequest.UNEQUIP) {
        Modifiers mods = Modifiers.getItemModifiers(item.getItemId());
        if (mods != null) {
          fixed -= mods.get(Modifiers.FAMILIAR_WEIGHT);
          hidden -= mods.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
          percent -= mods.get(Modifiers.FAMILIAR_WEIGHT_PCT);
        }
      }
    }

    // If this is not the current familiar and we are considering
    // equipment, add weight granted by equipment.
    if (this != familiar && includeEquipment) {
      // Add modifiers for this familiar's equipment
      item = this.getItem();
      if (item != EquipmentRequest.UNEQUIP) {
        Modifiers mods = Modifiers.getItemModifiers(item.getItemId());
        if (mods != null) {
          fixed += mods.get(Modifiers.FAMILIAR_WEIGHT);
          hidden += mods.get(Modifiers.HIDDEN_FAMILIAR_WEIGHT);
          percent += mods.get(Modifiers.FAMILIAR_WEIGHT_PCT);
        }
      }
    }

    // Add in fixed modifiers
    weight += (int) fixed;

    // If want to include hidden modifiers, do so now
    if (includeHidden) {
      weight += (int) hidden;
    }

    // Adjust by percent modifiers
    if (percent != 0.0f) {
      weight = (int) Math.floor(weight + weight * (percent / 100.0f));
    }

    // If the familiar is well-fed, it's 10 lbs. heavier
    if (this.feasted) {
      weight += 10;
    }

    // check if the familiar has a weight cap
    int cap = (int) current.get(Modifiers.FAMILIAR_WEIGHT_CAP);
    int cappedWeight = (cap == 0) ? weight : Math.min(weight, cap);

    return Math.max(1, cappedWeight);
  }

  public static final int itemWeightModifier(final int itemId) {
    Modifiers mods = Modifiers.getItemModifiers(itemId);
    return mods == null ? 0 : (int) mods.get(Modifiers.FAMILIAR_WEIGHT);
  }

  public final int getUncappedWeight() {
    if (this.id == FamiliarPool.HATRACK || this.id == FamiliarPool.SCARECROW) {
      return Math.max(Math.min(20, (int) Math.sqrt(this.experience)), 1);
    }
    return this.weight;
  }

  public String getName() {
    return this.name;
  }

  public String getRace() {
    return this.race;
  }

  public int getPokeLevel() {
    return this.pokeLevel;
  }

  public boolean getFavorite() {
    return this.favorite;
  }

  public void setFavorite(boolean favor) {
    this.favorite = favor;
  }

  public String getImageLocation() {
    String image = FamiliarDatabase.getFamiliarImageLocation(this.id);
    int index = image.lastIndexOf("/");
    return index == -1 ? image : image.substring(index + 1);
  }

  public void setCharges(int charges) {
    this.charges = charges;
  }

  public int getCharges() {
    return this.charges;
  }

  public boolean trainable() {
    if (this.id == -1) {
      return false;
    }

    int[] skills = FamiliarDatabase.getFamiliarSkills(this.id);

    // If any skill is greater than 0, we can train in that event
    for (int i = 0; i < skills.length; ++i) {
      if (skills[i] > 0) {
        return true;
      }
    }

    return false;
  }

  public boolean waterBreathing() {
    return FamiliarDatabase.isUnderwaterType(this.id);
  }

  public boolean canCarry() {
    switch (this.id) {
      case FamiliarPool.DOPPEL:
      case FamiliarPool.CHAMELEON:
      case FamiliarPool.HATRACK:
      case FamiliarPool.HAND:
      case FamiliarPool.LEFT_HAND:
      case FamiliarPool.SCARECROW:
      case FamiliarPool.UNSPEAKACHU:
      case FamiliarPool.STOOPER:
      case FamiliarPool.DISGEIST:
      case FamiliarPool.BOWLET:
      case FamiliarPool.CORNBEEFADON:
      case FamiliarPool.MU:
        return false;
    }
    return true;
  }

  public static class DropInfo {
    public final int id;
    public final AdventureResult dropItem;
    public final String dropName;
    public final String dropTracker;
    public final int dailyCap;

    public DropInfo(int id, int dropId, String dropName, String dropTracker, int dailyCap) {
      this.id = id;
      this.dropItem = dropId < 0 ? null : ItemPool.get(dropId);
      this.dropName = dropName;
      this.dropTracker = dropTracker;
      this.dailyCap = dailyCap;
    }

    public int dropsToday() {
      return Preferences.getInteger(this.dropTracker);
    }

    public boolean hasDropsLeft() {
      return this.dropsToday() < this.dailyCap;
    }
  }

  // TODO: (philosophical) Decide whether free fights count as
  // meta-drops, or if these should both extend from a base abstract
  // class for familiar counters.

  public static class FightInfo extends DropInfo {
    public FightInfo(int id, String dropTracker, int dailyCap) {
      super(id, -1, "combats", dropTracker, dailyCap);
    }

    public int fightsToday() {
      return this.dropsToday();
    }

    public int hasFightsLeft() {
      return this.dropsToday();
    }
  }

  private static void loadFightFamiliars() {
    FIGHT_FAMILIARS.clear();

    FIGHT_FAMILIARS.add(new FightInfo(FamiliarPool.HIPSTER, "_hipsterAdv", 7));
    FIGHT_FAMILIARS.add(new FightInfo(FamiliarPool.ARTISTIC_GOTH_KID, "_hipsterAdv", 7));
    FIGHT_FAMILIARS.add(new FightInfo(FamiliarPool.MACHINE_ELF, "_machineTunnelsAdv", 5));
  }

  private static void loadDropFamiliars() {
    DROP_FAMILIARS.clear();

    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.PIXIE, ItemPool.ABSINTHE, "absinthe", "_absintheDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.SANDWORM, ItemPool.AGUA_DE_VIDA, "agua", "_aguaDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.BADGER, ItemPool.ASTRAL_MUSHROOM, "astral", "_astralDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.KLOOP, ItemPool.DEVILISH_FOLIO, "folio", "_kloopDrops", 5));
    DROP_FAMILIARS.add(new DropInfo(FamiliarPool.LLAMA, ItemPool.GONG, "gong", "_gongDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.GROOSE, ItemPool.GROOSE_GREASE, "grease", "_grooseDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.TRON, ItemPool.GG_TOKEN, "token", "_tokenDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.ALIEN,
            ItemPool.TRANSPORTER_TRANSPONDER,
            "transponder",
            "_transponderDrops",
            5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.UNCONSCIOUS_COLLECTIVE,
            ItemPool.UNCONSCIOUS_COLLECTIVE_DREAM_JAR,
            "dream jar",
            "_dreamJarDrops",
            5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.ANGRY_JUNG_MAN,
            ItemPool.PSYCHOANALYTIC_JAR,
            "psycho jar",
            "_jungDrops",
            1));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.GRIM_BROTHER,
            ItemPool.GRIM_FAIRY_TALE,
            "fairy tale",
            "_grimFairyTaleDrops",
            5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.GRIMSTONE_GOLEM,
            ItemPool.GRIMSTONE_MASK,
            "grim mask",
            "_grimstoneMaskDrops",
            1));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.GALLOPING_GRILL, ItemPool.HOT_ASHES, "hot ashes", "_hotAshesDrops", 5));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.FIST_TURKEY, -1, "turkey booze", "_turkeyBooze", 5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.GOLDEN_MONKEY,
            ItemPool.POWDERED_GOLD,
            "powdered gold",
            "_powderedGoldDrops",
            5));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.ADVENTUROUS_SPELUNKER,
            ItemPool.TALES_OF_SPELUNKING,
            "tales",
            "_spelunkingTalesDrops",
            1));
    DROP_FAMILIARS.add(
        new DropInfo(FamiliarPool.CARNIE, -1, "cotton candy", "_carnieCandyDrops", 10));
    DROP_FAMILIARS.add(new DropInfo(FamiliarPool.BOOTS, -1, "pastes", "_bootStomps", 7));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.SWORD_AND_MARTINI_GUY,
            ItemPool.MINI_MARTINI,
            "mini-martini",
            "_miniMartiniDrops",
            6));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.PUCK_MAN,
            ItemPool.POWER_PILL,
            "power pill",
            "_powerPillDrops",
            Math.min(1 + KoLCharacter.getCurrentDays(), 11)));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.MS_PUCK_MAN,
            ItemPool.POWER_PILL,
            "power pill",
            "_powerPillDrops",
            Math.min(1 + KoLCharacter.getCurrentDays(), 11)));
    DROP_FAMILIARS.add(
        new DropInfo(
            FamiliarPool.MACHINE_ELF,
            ItemPool.MACHINE_SNOWGLOBE,
            "snowglobe",
            "_snowglobeDrops",
            1));
  }

  public static DropInfo getDropInfo(int id) {
    for (DropInfo info : DROP_FAMILIARS) {
      if (info.id == id) return info;
    }

    return null;
  }

  public DropInfo getDropInfo() {
    return FamiliarData.getDropInfo(this.id);
  }

  public static String dropName(int id) {
    DropInfo drops = FamiliarData.getDropInfo(id);
    return drops == null ? null : drops.dropName;
  }

  public String dropName() {
    return FamiliarData.dropName(this.id);
  }

  public static AdventureResult dropItem(int id) {
    DropInfo drops = FamiliarData.getDropInfo(id);
    return drops == null ? null : drops.dropItem;
  }

  public AdventureResult dropItem() {
    return FamiliarData.dropItem(this.id);
  }

  public static int dropsToday(int id) {
    DropInfo drops = FamiliarData.getDropInfo(id);
    return drops == null ? 0 : drops.dropsToday();
  }

  public int dropsToday() {
    return FamiliarData.dropsToday(this.id);
  }

  public static int dropDailyCap(int id) {
    DropInfo drops = FamiliarData.getDropInfo(id);
    return drops == null ? 0 : drops.dailyCap;
  }

  public int dropDailyCap() {
    return FamiliarData.dropDailyCap(this.id);
  }

  public static boolean hasDrop(int id) {
    return FamiliarData.getDropInfo(id) != null;
  }

  public boolean hasDrop() {
    return FamiliarData.hasDrop(this.id);
  }

  public static boolean hasFights(int id) {
    return FamiliarData.getFightInfo(id) != null;
  }

  public boolean hasFights() {
    return FamiliarData.hasFights(this.id);
  }

  public static FightInfo getFightInfo(int id) {
    for (FightInfo info : FIGHT_FAMILIARS) {
      if (info.id == id) return info;
    }
    return null;
  }

  public FightInfo getFightInfo() {
    return FamiliarData.getFightInfo(this.id);
  }

  public static int fightsToday(int id) {
    FightInfo fights = FamiliarData.getFightInfo(id);
    return fights == null ? 0 : fights.fightsToday();
  }

  public int fightsToday() {
    return FamiliarData.fightsToday(this.id);
  }

  public static int fightDailyCap(int id) {
    FightInfo fights = FamiliarData.getFightInfo(id);
    return fights == null ? 0 : fights.dailyCap;
  }

  public int fightDailyCap() {
    return FamiliarData.fightDailyCap(this.id);
  }

  @Override
  public String toString() {
    return this.id == -1 ? "(none)" : this.race + " (" + this.getModifiedWeight() + " lbs)";
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof FamiliarData && this.id == ((FamiliarData) o).id;
  }

  @Override
  public int hashCode() {
    return this.id;
  }

  @Override
  public int compareTo(final FamiliarData fd) {
    return this.race.compareToIgnoreCase(fd.race);
  }

  /** Returns whether or not the familiar can equip any familiar items. */
  public boolean canEquipAny() {
    switch (this.id) {
      case FamiliarPool.CHAMELEON:
      case FamiliarPool.GHOST_CAROLS:
      case FamiliarPool.GHOST_CHEER:
      case FamiliarPool.GHOST_COMMERCE:
        return false;
    }

    return true;
  }

  /** Returns whether or not the familiar can equip the given familiar item. */
  public boolean canEquip(final AdventureResult item) {
    if (item == null) {
      return false;
    }

    if (item == EquipmentRequest.UNEQUIP) {
      return true;
    }

    int itemId = item.getItemId();
    if (itemId <= 0) {
      return false;
    }

    if (!this.canEquipAny()) {
      return false;
    }

    String name = item.getName();

    switch (this.id) {
      case -1:
        return false;

      case FamiliarPool.HATRACK:
        // Hatrack can wear Hats as well as familiar items, but not Crown of Thrones
        if (itemId != ItemPool.HATSEAT
            && ItemDatabase.getConsumptionType(itemId) == KoLConstants.EQUIP_HAT) {
          return true;
        }
        break;

      case FamiliarPool.HAND:
        // Disembodied Hand can't equip Mainhand only items or Single Equip items
        if (!EquipmentDatabase.isMainhandOnly(itemId)
            && !Modifiers.getBooleanModifier("Item", name, "Single Equip")) {
          return true;
        }
        break;

      case FamiliarPool.LEFT_HAND:
        // Left-Hand Man can wear Offhand items as well as familiar items
        if (ItemDatabase.getConsumptionType(itemId) == KoLConstants.EQUIP_OFFHAND) {
          return true;
        }
        break;

      case FamiliarPool.SCARECROW:
        // Scarecrow can wear Pants as well as familiar items
        if (ItemDatabase.getConsumptionType(itemId) == KoLConstants.EQUIP_PANTS) {
          return true;
        }
        break;
    }

    if (itemId == FamiliarDatabase.getFamiliarItemId(this.id)) {
      return true;
    }

    if (!ItemDatabase.isFamiliarEquipment(itemId)) {
      return false;
    }

    Modifiers mods = Modifiers.getItemModifiers(itemId);
    if (mods == null) {
      return false;
    }

    if (mods.getBoolean(Modifiers.GENERIC)) {
      return true;
    }

    String others = mods.getString(Modifiers.EQUIPS_ON);
    if (others == null || others.equals("")) {
      return false;
    }

    for (String it : others.split("\\s*\\|\\s*")) {
      if (it.equals(this.getRace())) {
        return true;
      }
    }

    return false;
  }

  public int specialEquipmentType() {
    switch (this.id) {
      case FamiliarPool.HATRACK:
        return KoLConstants.EQUIP_HAT;
      case FamiliarPool.HAND:
        return KoLConstants.EQUIP_WEAPON;
      case FamiliarPool.LEFT_HAND:
        return KoLConstants.EQUIP_OFFHAND;
      case FamiliarPool.SCARECROW:
        return KoLConstants.EQUIP_PANTS;
      default:
        return KoLConstants.NO_CONSUME;
    }
  }

  public static boolean lockableItem(final AdventureResult item) {
    if (item == null || item == EquipmentRequest.UNEQUIP) {
      return false;
    }

    Modifiers mods = Modifiers.getItemModifiers(item.getItemId());
    return mods != null && mods.getBoolean(Modifiers.GENERIC);
  }

  public boolean isCombatFamiliar() {
    if (FamiliarDatabase.isCombatType(this.id)) {
      return true;
    }

    if (this.id == FamiliarPool.DANDY_LION) {
      return EquipmentManager.getEquipment(EquipmentManager.WEAPON).getName().endsWith("whip")
          || EquipmentManager.getEquipment(EquipmentManager.OFFHAND).getName().endsWith("whip");
    }

    return false;
  }

  public final void findAndWearItem(boolean steal) {
    AdventureResult use = this.findGoodItem(steal);
    if (use != null) {
      RequestThread.postRequest(new EquipmentRequest(use, EquipmentManager.FAMILIAR));
    }
  }

  public final AdventureResult findGoodItem(boolean steal) {
    if (KoLCharacter.inRaincore()
        && FamiliarData.availableItem(FamiliarData.LIFE_PRESERVER, steal)) {
      // The miniature life preserver is only useful in a Heavy Rains run
      return FamiliarData.LIFE_PRESERVER;
    }

    if (FamiliarData.availableItem(FamiliarData.AMULET_COIN, steal)) {
      return FamiliarData.AMULET_COIN;
    }

    if (FamiliarData.availableItem(FamiliarData.LUCK_INCENSE, steal)) {
      return FamiliarData.LUCK_INCENSE;
    }

    if (FamiliarData.availableItem(FamiliarData.RAZOR_FANG, steal)) {
      return FamiliarData.RAZOR_FANG;
    }

    if (FamiliarData.availableItem(FamiliarData.SHELL_BELL, steal)) {
      return FamiliarData.SHELL_BELL;
    }

    if (FamiliarData.availableItem(FamiliarData.SMOKE_BALL, steal)) {
      return FamiliarData.SMOKE_BALL;
    }

    if (FamiliarData.availableItem(FamiliarData.MUSCLE_BAND, steal)) {
      return FamiliarData.MUSCLE_BAND;
    }

    if (FamiliarData.availableItem(FamiliarData.PET_SWEATER, steal)) {
      return FamiliarData.PET_SWEATER;
    }

    if (FamiliarData.availableItem(FamiliarData.PUMPKIN_BUCKET, steal)) {
      return FamiliarData.PUMPKIN_BUCKET;
    }

    if (FamiliarData.availableItem(FamiliarData.FIREWORKS, steal)) {
      return FamiliarData.FIREWORKS;
    }

    if (FamiliarData.availableItem(FamiliarData.FLOWER_BOUQUET, steal)) {
      return FamiliarData.FLOWER_BOUQUET;
    }

    if (FamiliarData.availableItem(FamiliarData.MOVEABLE_FEAST, steal)) {
      return FamiliarData.MOVEABLE_FEAST;
    }

    int itemId = FamiliarDatabase.getFamiliarItemId(this.id);
    AdventureResult item = itemId > 0 ? ItemPool.get(itemId, 1) : null;
    if (item != null && FamiliarData.availableItem(item, false)) {
      return item;
    }

    if (FamiliarData.availableItem(FamiliarData.ITTAH_BITTAH_HOOKAH, steal)) {
      return FamiliarData.ITTAH_BITTAH_HOOKAH;
    }

    if (FamiliarData.availableItem(FamiliarData.LOATHING_LEGION_HELICOPTER, steal)) {
      return FamiliarData.LOATHING_LEGION_HELICOPTER;
    }

    if (FamiliarData.availableItem(FamiliarData.FILTHY_CHILD_LEASH, steal)) {
      return FamiliarData.FILTHY_CHILD_LEASH;
    }

    if (FamiliarData.availableItem(FamiliarData.LEAD_NECKLACE, steal)) {
      return FamiliarData.LEAD_NECKLACE;
    }

    return null;
  }

  private static boolean availableItem(AdventureResult item, boolean steal) {
    if (item.getCount(KoLConstants.inventory) > 0) {
      return true;
    }

    if (!steal) {
      return false;
    }

    FamiliarData current = KoLCharacter.getFamiliar();
    for (FamiliarData familiar : KoLCharacter.getFamiliarList()) {
      if (!familiar.equals(current)) {
        AdventureResult equipped = familiar.getItem();
        if (equipped != null && equipped.equals(item)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Calculates the number of combats with a Slimeling required for the nth slime stack in an
   * ascension to drop.
   *
   * @param n the number of the slime stack (reset to zero on ascension)
   * @return the number of combats
   */
  public static int getSlimeStackTurns(final int n) {
    return n * (n + 1) / 2;
  }

  public static final DefaultListCellRenderer getRenderer() {
    return new FamiliarRenderer();
  }

  public static final void checkShrub() {
    if (KoLCharacter.findFamiliar(FamiliarPool.CRIMBO_SHRUB) == null) {
      return;
    }

    GenericRequest request = new GenericRequest("desc_familiar.php?which=189");
    RequestThread.postRequest(request);
    String response = request.responseText;

    Matcher topperMatcher = SHRUB_TOPPER_PATTERN.matcher(response);
    if (topperMatcher.find()) {
      Preferences.setString("shrubTopper", topperMatcher.group(1));
    } else {
      Preferences.setString("shrubTopper", KoLCharacter.mainStat().toString());
      // If we didn't find this pattern, we won't find anything else either
      // The remaining values are either random or nothing
      Preferences.setString("shrubLights", "");
      Preferences.setString("shrubGarland", "");
      Preferences.setString("shrubGifts", "");
      return;
    }

    Matcher lightsMatcher = SHRUB_LIGHT_PATTERN.matcher(response);
    if (lightsMatcher.find()) {
      Preferences.setString("shrubLights", lightsMatcher.group(1));
    }

    if (response.contains("Restores Hit Points")) {
      Preferences.setString("shrubGarland", "HP");
    } else if (response.contains("PvP fights")) {
      Preferences.setString("shrubGarland", "PvP");
    } else if (response.contains("Prevents monsters")) {
      Preferences.setString("shrubGarland", "blocking");
    }

    if (response.contains("Blast foes")) {
      Preferences.setString("shrubGifts", "yellow");
    } else if (response.contains("Filled with Meat")) {
      Preferences.setString("shrubGifts", "meat");
    } else if (response.contains("Exchange random gifts")) {
      Preferences.setString("shrubGifts", "gifts");
    }
  }

  private static class FamiliarRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      JLabel defaultComponent =
          (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (!(value instanceof FamiliarData) || ((FamiliarData) value).id == -1) {
        defaultComponent.setIcon(JComponentUtilities.getImage("debug.gif"));
        defaultComponent.setText(
            StaticEntity.getVersion() + ", the 0 lb. \"No Familiar Plz\" Placeholder");

        defaultComponent.setVerticalTextPosition(SwingConstants.CENTER);
        defaultComponent.setHorizontalTextPosition(SwingConstants.RIGHT);
        return defaultComponent;
      }

      FamiliarData familiar = (FamiliarData) value;
      defaultComponent.setIcon(FamiliarDatabase.getFamiliarImage(familiar.id));
      defaultComponent.setText(
          familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace());

      defaultComponent.setVerticalTextPosition(SwingConstants.CENTER);
      defaultComponent.setHorizontalTextPosition(SwingConstants.RIGHT);

      return defaultComponent;
    }
  }
}
