package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FamiliarRequest extends GenericRequest {
  private static final Pattern FAMID_PATTERN = Pattern.compile("famid=(-?\\d+)");
  private static final Pattern NEWFAM_PATTERN = Pattern.compile("newfam=(\\d+)");
  private static final Pattern WHICHFAM_PATTERN = Pattern.compile("whichfam=(-?\\d+)");

  private static int getFamId(final String urlString) {
    Matcher matcher = FamiliarRequest.FAMID_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static int getNewFam(final String urlString) {
    Matcher matcher = FamiliarRequest.NEWFAM_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static int getWhichFam(final String urlString) {
    Matcher matcher = FamiliarRequest.WHICHFAM_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private FamiliarData changeTo;
  private final AdventureResult item;
  private final boolean locking;
  private boolean enthrone, bjornify, stealItem;

  public FamiliarRequest() {
    super("familiar.php");
    this.changeTo = null;
    this.item = null;
    this.locking = false;
    this.enthrone = false;
    this.bjornify = false;
    this.stealItem = true;
  }

  public FamiliarRequest(final FamiliarData changeTo) {
    this(changeTo, true);
  }

  public FamiliarRequest(final FamiliarData changeTo, boolean stealItem) {
    super("familiar.php");

    this.changeTo = changeTo == null ? FamiliarData.NO_FAMILIAR : changeTo;
    this.item = null;
    this.locking = false;
    this.enthrone = false;
    this.bjornify = false;
    this.stealItem = stealItem;

    if (this.changeTo == FamiliarData.NO_FAMILIAR) {
      this.addFormField("action", "putback");
    } else {
      this.addFormField("action", "newfam");
      this.addFormField("newfam", String.valueOf(this.changeTo.getId()));
    }
    this.addFormField("ajax", "1");
  }

  public FamiliarRequest(final FamiliarData familiar, final AdventureResult item) {
    super("familiar.php");

    this.changeTo = familiar;
    this.item = item == null ? EquipmentRequest.UNEQUIP : item;
    this.locking = false;
    this.enthrone = false;
    this.bjornify = false;
    this.stealItem = true;

    if (this.item != EquipmentRequest.UNEQUIP) {
      this.addFormField("action", "equip");
      this.addFormField("whichfam", String.valueOf(familiar.getId()));
      this.addFormField("whichitem", String.valueOf(item.getItemId()));
    } else {
      this.addFormField("famid", String.valueOf(familiar.getId()));
      this.addFormField("action", "unequip");
    }
    this.addFormField("ajax", "1");
  }

  public FamiliarRequest(boolean locking) {
    super("familiar.php");
    this.addFormField("action", "lockequip");
    this.changeTo = null;
    this.item = null;
    this.locking = true;
    this.enthrone = false;
    this.bjornify = false;
    this.stealItem = true;
    this.addFormField("ajax", "1");
  }

  public static FamiliarRequest enthroneRequest(final FamiliarData changeTo) {
    FamiliarRequest rv = new FamiliarRequest();
    rv.changeTo = changeTo;
    rv.enthrone = true;
    rv.addFormField("action", "hatseat");
    int id = changeTo.getId();
    rv.addFormField("famid", id <= 0 ? "0" : String.valueOf(id));
    rv.addFormField("ajax", "1");
    return rv;
  }

  public static FamiliarRequest bjornifyRequest(final FamiliarData changeTo) {
    FamiliarRequest rv = new FamiliarRequest();
    rv.changeTo = changeTo;
    rv.bjornify = true;
    rv.addFormField("action", "backpack");
    int id = changeTo.getId();
    rv.addFormField("famid", id <= 0 ? "0" : String.valueOf(id));
    rv.addFormField("ajax", "1");
    return rv;
  }

  @Override
  protected boolean retryOnTimeout() {
    return !this.locking;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (KoLCharacter.inPokefam()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot make familiar requests in Pokefam.");
      return;
    }

    if (KoLCharacter.inQuantum()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Cannot make familiar requests in Quantum Terrarium.");
      return;
    }

    if (this.locking) {
      String verb = EquipmentManager.familiarItemLocked() ? "Unlocking" : "Locking";
      KoLmafia.updateDisplay(verb + " familiar item...");
      super.run();
      return;
    }

    if (this.changeTo == null) {
      KoLmafia.updateDisplay("Retrieving familiar data...");
      super.run();
      return;
    }

    if (this.item != null) {
      if (this.changeTo.equals(KoLCharacter.getFamiliar())) {
        EquipmentRequest request = new EquipmentRequest(this.item, EquipmentManager.FAMILIAR);
        request.run();
        return;
      }

      if (this.item == EquipmentRequest.UNEQUIP) {
        KoLmafia.updateDisplay(
            "Unequipping " + this.changeTo.getName() + " the " + this.changeTo.getRace() + "...");
      } else {
        KoLmafia.updateDisplay(
            "Equipping "
                + this.changeTo.getName()
                + " the "
                + this.changeTo.getRace()
                + " with "
                + this.item.getName()
                + "...");
      }
      super.run();
      return;
    }

    FamiliarData familiar = KoLCharacter.getFamiliar();
    if (this.enthrone) {
      FamiliarData enthroned = KoLCharacter.getEnthroned();

      if (EquipmentManager.getEquipment(EquipmentManager.HAT).getItemId() != ItemPool.HATSEAT) {
        if (this.changeTo.equals(FamiliarData.NO_FAMILIAR)
            && !enthroned.equals(FamiliarData.NO_FAMILIAR)) {
          try (Checkpoint checkpoint = new Checkpoint()) {
            RequestThread.postRequest(
                new EquipmentRequest(ItemPool.get(ItemPool.HATSEAT, 1), EquipmentManager.HAT));
            RequestThread.postRequest(FamiliarRequest.enthroneRequest(FamiliarData.NO_FAMILIAR));
          }
        }
        return;
      }

      if (enthroned.getId() == this.changeTo.getId()) {
        return;
      }

      if (enthroned != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Kicking "
                + enthroned.getName()
                + " the "
                + enthroned.getRace()
                + " off the throne...");
      }

      if (this.changeTo != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Carrying "
                + this.changeTo.getName()
                + " the "
                + this.changeTo.getRace()
                + " in the Crown of Thrones...");
      }
    } else if (this.bjornify) {
      FamiliarData bjorned = KoLCharacter.getBjorned();

      if (EquipmentManager.getEquipment(EquipmentManager.CONTAINER).getItemId()
          != ItemPool.BUDDY_BJORN) {
        if (this.changeTo.equals(FamiliarData.NO_FAMILIAR)
            && !bjorned.equals(FamiliarData.NO_FAMILIAR)) {
          try (Checkpoint checkpoint = new Checkpoint()) {
            RequestThread.postRequest(
                new EquipmentRequest(
                    ItemPool.get(ItemPool.BUDDY_BJORN, 1), EquipmentManager.CONTAINER));
            RequestThread.postRequest(FamiliarRequest.bjornifyRequest(FamiliarData.NO_FAMILIAR));
          }
        }
        return;
      }

      if (bjorned.getId() == this.changeTo.getId()) {
        return;
      }

      if (bjorned != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Kicking " + bjorned.getName() + " the " + bjorned.getRace() + " out of the bjorn...");
      }

      if (this.changeTo != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Carrying "
                + this.changeTo.getName()
                + " the "
                + this.changeTo.getRace()
                + " in the Bjorn Buddy...");
      }
    } else { // !enthrone
      if (familiar.getId() == this.changeTo.getId()) {
        return;
      }

      if (familiar != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Putting "
                + familiar.getName()
                + " the "
                + familiar.getRace()
                + " back into terrarium...");
      }

      if (this.changeTo != FamiliarData.NO_FAMILIAR) {
        KoLmafia.updateDisplay(
            "Taking "
                + this.changeTo.getName()
                + " the "
                + this.changeTo.getRace()
                + " out of terrarium...");
      }
    }

    super.run();

    // If we didn't have a familiar before,
    // don't have one now,
    // or have chosen not to change equipment
    // leave equipment alone.

    if (this.enthrone
        || this.bjornify
        || familiar == FamiliarData.NO_FAMILIAR
        || this.changeTo == FamiliarData.NO_FAMILIAR
        || !this.stealItem) {
      return;
    }

    if (FamiliarRequest.invokeFamiliarScript()) {
      return;
    }

    AdventureResult item = familiar.getItem();

    // If the old familiar wasn't wearing equipment, nothing to steal.

    if (item == EquipmentRequest.UNEQUIP) {
      return;
    }

    // If equipment was locked, and the new familiar can equip it,
    // KoL itself switched equipment

    if (item == EquipmentManager.lockedFamiliarItem() && this.changeTo.canEquip(item)) {
      return;
    }

    // If we are switching to certain specialized familiars, don't
    // steal any equipment from the old familiar
    switch (this.changeTo.getId()) {
      case FamiliarPool.BLACKBIRD: // Reassembled Blackbird
      case FamiliarPool.CROW: // Reconstituted Crow
      case FamiliarPool.STOCKING_MIMIC:
        // Leave the Stocking Mimic unequipped, to allow it to
        // generate its own candy-generating item.
        return;
    }

    // If the new familiar cannot equip the item, don't try to steal
    if (!this.changeTo.canEquip(item)) {
      return;
    }

    // If the new familiar already has an item, leave it alone.
    if (!this.changeTo.getItem().equals(EquipmentRequest.UNEQUIP)) {
      return;
    }

    // The new familiar has no item. Find a good one to steal.
    AdventureResult use = this.changeTo.findGoodItem(true);
    if (use == null) {
      return;
    }

    // If you are in Beecore and the item has Beeosity, don't select it.
    if (KoLCharacter.inBeecore() && KoLCharacter.hasBeeosity(use.getName())) {
      return;
    }

    KoLmafia.updateDisplay(use.getName() + " is better than (none).  Switching items...");
    RequestThread.postRequest(new EquipmentRequest(use, EquipmentManager.FAMILIAR));
  }

  @Override
  public void processResults() {
    // Boris has no need for familiars!
    // Jarlsberg didn't trust any companion that he didn't summon himself.
    // Familiars weren't cool enough for Pete.
    // Ed already has more than enough servants.
    // In Quantum Terrarium, familiar sets you.
    if (KoLCharacter.inAxecore()
        || KoLCharacter.isJarlsberg()
        || KoLCharacter.isSneakyPete()
        || KoLCharacter.isEd()
        || KoLCharacter.inBondcore()
        || KoLCharacter.isVampyre()
        || KoLCharacter.inQuantum()) {
      return;
    }

    if (!FamiliarRequest.parseResponse(this.getURLString(), this.responseText)) {
      // *** Have more specific error message?
      KoLmafia.updateDisplay(MafiaState.ERROR, "Familiar request unsuccessful.");
      return;
    }

    if (this.item == EquipmentRequest.UNEQUIP) {
      KoLmafia.updateDisplay("Familiar unequipped.");
    } else if (this.item != null) {
      KoLmafia.updateDisplay("Familiar equipped.");
    } else if (this.locking) {
      String locked = EquipmentManager.familiarItemLocked() ? "locked" : "unlocked";
      KoLmafia.updateDisplay("Familiar item " + locked + ".");
    } else if (this.changeTo == null) {
      // This could be in parseResponse, but why do it every
      // time we visit the Terrarium in the Relay Browser?
      FamiliarData.registerFamiliarData(this.responseText);
      KoLmafia.updateDisplay("Familiar data retrieved.");
    }
  }

  public static final boolean handleFamiliarChange(FamiliarData changeTo) {
    if (changeTo == null || !changeTo.canEquip()) {
      return false;
    }

    if (changeTo.getId() == FamiliarPool.REANIMATOR) {
      // Visit chat to familiar page to get current parts
      KoLmafia.updateDisplay(
          "Getting current parts information for "
              + changeTo.getName()
              + " the "
              + changeTo.getRace()
              + ".");
      RequestThread.postRequest(new GenericRequest("main.php?talktoreanimator=1"));
    }

    // Remove granted skills from the familiar we put back
    FamiliarData changeFrom = KoLCharacter.getFamiliar();
    EquipmentManager.removeConditionalSkills(EquipmentManager.FAMILIAR, changeFrom.getItem());

    // If we have a familiar item locked and the new familiar can
    // equip it, it was automatically transferred.
    AdventureResult lockedItem =
        KoLCharacter.inQuantum()
            ? EquipmentManager.getFamiliarItem()
            : EquipmentManager.lockedFamiliarItem();

    if (lockedItem != EquipmentRequest.UNEQUIP) {
      if (changeTo.canEquip(lockedItem)) {
        FamiliarRequest.unequipFamiliar(changeFrom);
        FamiliarRequest.equipFamiliar(changeTo, lockedItem);
        EquipmentManager.lockFamiliarItem(lockedItem);
      } else {
        EquipmentManager.lockFamiliarItem(false);
      }
    }

    // Add granted skills from the familiar we took out
    EquipmentManager.addConditionalSkills(EquipmentManager.FAMILIAR, changeTo.getItem());

    return true;
  }

  public static final boolean parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("familiar.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);

    if (action == null) {
      return true;
    }

    if (action.equals("message")) {
      // EquipmentRequest did something for us and returned to the terrarium
      return true;
    }

    if (action.equals("newfam")) {
      // Failure:
      //
      // You don't have a familiar of that type in your Terrarium.
      // You are too afraid of the Bs to adventure with that familiar.
      // That familiar is way too outmoded.
      // Boris has no need for familiars!
      // That familiar seems to be dead.
      // Jarlsberg didn't trust any companion that he didn't summon himself.
      //
      // Success:
      //
      // You put Hand Jive Grrl back in the Terrarium.
      // You take Sucky Grrl with you.

      if (!responseText.contains("You take")) {
        return false;
      }

      FamiliarData changeTo = KoLCharacter.findFamiliar(FamiliarRequest.getNewFam(urlString));

      if (!handleFamiliarChange(changeTo)) {
        return false;
      }

      KoLCharacter.setFamiliar(changeTo);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);

      return true;
    }

    if (action.equals("putback")) {
      // Failure:
      //
      // How can you put away your familiar, when you don't have a familiar out. Crazy much?
      //
      // Success:
      //
      // You put Lucky Grrl back in the Terrarium.

      if (!responseText.contains("back in the Terrarium")) {
        return false;
      }

      KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
      EquipmentManager.setEquipment(EquipmentManager.FAMILIAR, EquipmentRequest.UNEQUIP);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);
      EquipmentManager.lockFamiliarItem(false);

      return true;
    }

    if (action.equals("equip")) {
      // Failure:
      //
      // You don't have a familiar of that type.
      // You either don't have the item you selected, or the item you selected isn't familiar
      // equipment. Hooray for vague error messages!
      //
      // Success:
      //
      // You equip Hand Jive Grrl with the time sword.

      if (!responseText.contains("You equip")) {
        return false;
      }

      FamiliarData familiar = KoLCharacter.findFamiliar(FamiliarRequest.getWhichFam(urlString));
      FamiliarData current = KoLCharacter.getFamiliar();
      AdventureResult item = ItemPool.get(GenericRequest.getWhichItem(urlString), 1);

      if (current.equals(familiar)) {
        EquipmentManager.removeConditionalSkills(EquipmentManager.FAMILIAR, current.getItem());
        EquipmentManager.addConditionalSkills(EquipmentManager.FAMILIAR, item);
      }

      FamiliarRequest.equipFamiliar(familiar, item);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);

      return true;
    }

    if (action.equals("unequip")) {
      // Success:
      //
      // Item unequipped.
      //
      // Failure:
      //
      // You either don't have a familiar of that type, or the familiar you selected isn't actually
      // using any equipment right now. Hooray for vague error messages!

      if (!responseText.contains("Item unequipped")) {
        return false;
      }

      FamiliarData familiar = KoLCharacter.findFamiliar(FamiliarRequest.getFamId(urlString));
      FamiliarData current = KoLCharacter.getFamiliar();

      if (current.equals(familiar)) {
        EquipmentManager.removeConditionalSkills(EquipmentManager.FAMILIAR, current.getItem());
      }

      FamiliarRequest.unequipFamiliar(familiar);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);

      return true;
    }

    if (action.equals("lockequip")) {
      // Success:
      //
      // Familiar equipment locked.
      // Familiar equipment unlocked.
      //
      // Failure:
      //
      // You cannot lock familiar equipment, on account of not having any equipped.
      // You cannot lock familiar equipment which can only be equipped by one kind of familiar.
      // Strange, but true.

      if (responseText.contains("You cannot")) {
        return false;
      }

      EquipmentManager.lockFamiliarItem(!EquipmentManager.familiarItemLocked());
      return true;
    }

    // See if we are putting the familiar into the Crown of Thrones
    if (action.equals("hatseat")) {
      // Failure:
      //
      // You're not wearing a hat seat.

      if (responseText.contains("You're not wearing a hat seat")) {
        return false;
      }

      int famid = FamiliarRequest.getFamId(urlString);
      if (famid < 0) {
        return false;
      }

      // Success:
      //
      // Crown of Thrones vacated.

      if (famid == 0) {
        KoLCharacter.setEnthroned(FamiliarData.NO_FAMILIAR);
        return true;
      }

      // Success:
      //
      // Your Jumpsuited Hound Dog, Elvis Grrl, is now carried in the Crown of Thrones.

      if (!responseText.contains("is now carried in the Crown of Thrones")) {
        return false;
      }

      FamiliarData fam = KoLCharacter.findFamiliar(famid);
      if (fam == null || !fam.canEquip()) {
        return false;
      }

      KoLCharacter.setEnthroned(fam);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);

      return true;
    }

    // See if we are putting the familiar into the Bjorn Buddy
    if (action.equals("backpack")) {
      // Failure:
      // You're not wearing a Bjorn Buddy.

      if (responseText.contains("You're not wearing a Bjorn Buddy")) {
        return false;
      }

      int famid = FamiliarRequest.getFamId(urlString);
      if (famid < 0) {
        return false;
      }

      // Success:
      //
      // Buddy Bjorn vacated.

      if (famid == 0) {
        KoLCharacter.setBjorned(FamiliarData.NO_FAMILIAR);
        return true;
      }

      // Success:
      //
      // Your Green Pixie, Al Coholic, is now safely in your Buddy Bjorn.

      if (!responseText.contains("is now safely in your Buddy Bjorn")) {
        return false;
      }

      FamiliarData fam = KoLCharacter.findFamiliar(famid);
      if (fam == null || !fam.canEquip()) {
        return false;
      }

      KoLCharacter.setBjorned(fam);
      EquipmentManager.updateEquipmentList(EquipmentManager.FAMILIAR);

      return true;
    }

    return true;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("familiar.php")) {
      return false;
    }

    if (urlString.equals("familiar.php")) {
      // Visiting the terrarium
      return true;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return false;
    }

    if (action.equals("message")) {
      return true;
    }

    if (action.equals("newfam")) {
      int newfam = FamiliarRequest.getNewFam(urlString);
      FamiliarData fam = KoLCharacter.findFamiliar(newfam);

      // If we don't have the new familiar or can't change to
      // it, this request will fail, so don't log it.
      if (fam == null || fam == FamiliarData.NO_FAMILIAR || !fam.canEquip()) {
        return true;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("familiar " + fam.toString());
      return true;
    }

    if (action.equals("putback")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("familiar none");
      return true;
    }

    if (action.equals("equip")) {
      int whichfam = FamiliarRequest.getWhichFam(urlString);
      if (whichfam == -1) {
        // This is the current familiar. It will
        // redirect to inv_equip.php.  Let
        // EquipmentRequest handle it.
        return true;
      }

      int whichitem = FamiliarRequest.getWhichItem(urlString);
      FamiliarData fam =
          whichfam == -1 ? KoLCharacter.getFamiliar() : KoLCharacter.findFamiliar(whichfam);
      AdventureResult item = ItemPool.get(whichitem, 1);

      // If we don't have the new familiar or it cannot equip
      // the item, this request will fail, so don't log it.
      if (fam == null || fam == FamiliarData.NO_FAMILIAR || !fam.canEquip(item)) {
        return true;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Equip " + fam.getRace() + " with " + item.getName());
      return true;
    }

    if (action.equals("unequip")) {
      int famid = FamiliarRequest.getFamId(urlString);
      FamiliarData fam = KoLCharacter.findFamiliar(famid);

      // If we don't have the new familiar, this request will
      // fail, so don't log it.
      if (fam == null || fam == FamiliarData.NO_FAMILIAR) {
        return true;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Unequip " + fam.getRace());
      return true;
    }

    if (action.equals("lockequip")) {
      if (!EquipmentManager.familiarItemLockable()) {
        return true;
      }

      String verb = EquipmentManager.familiarItemLocked() ? "unlock" : "lock";
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("familiar " + verb);
      return true;
    }

    // See if we are putting the familiar into the Crown of Thrones
    if (action.equals("hatseat")) {
      int famid = FamiliarRequest.getFamId(urlString);
      if (famid < 0) {
        return true;
      }

      if (famid == 0) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("enthrone none");
        return true;
      }

      FamiliarData fam = KoLCharacter.findFamiliar(famid);

      // If we don't have the familiar or can't equip it,
      // this request will fail, so don't log it.
      if (fam == null || fam == FamiliarData.NO_FAMILIAR || !fam.canEquip()) {
        return true;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("enthrone " + fam.toString());
      return true;
    }

    // See if we are putting the familiar into Buddy Bjorn
    if (action.equals("backpack")) {
      int famid = FamiliarRequest.getFamId(urlString);
      if (famid < 0) {
        return true;
      }

      if (famid == 0) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("bjornify none");
        return true;
      }

      FamiliarData fam = KoLCharacter.findFamiliar(famid);

      // If we don't have the familiar or can't equip it,
      // this request will fail, so don't log it.
      if (fam == null || fam == FamiliarData.NO_FAMILIAR || !fam.canEquip()) {
        return true;
      }

      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("bjornify " + fam.toString());
      return true;
    }

    // If it is something else, just log the URL
    return false;
  }

  private static boolean invokeFamiliarScript() {
    String scriptName = Preferences.getString("familiarScript");
    if (scriptName.length() == 0) {
      return false;
    }

    ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(KoLmafiaCLI.findScriptFile(scriptName));
    if (interpreter != null) {
      Value v = interpreter.execute("main", null);
      return v != null && v.intValue() != 0;
    }
    return false;
  }

  private static void equipFamiliar(FamiliarData familiar, final AdventureResult item) {
    familiar.setItem(item);
  }

  private static void unequipFamiliar(FamiliarData familiar) {
    familiar.setItem(EquipmentRequest.UNEQUIP);
  }

  // Utility functions for EquipmentRequest
  //
  // These are called by registerRequest for:
  //
  // inv_equip.php?action=unequip&type=familiarequip&terrarium=1
  // inv_equip.php?action=equip&type=familiarequip&terrarium=1
  //
  // They are for equipping or unequipping the current familiar.
  // We should not manipulate the equipment here.
  // Unfortunately, the response comes back as familiar.php?action=message

  public static final void equipCurrentFamiliar(final int itemId) {
    FamiliarData fam = KoLCharacter.getFamiliar();
    AdventureResult item = ItemPool.get(itemId, 1);
    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("Equip " + fam.getRace() + " with " + item.getName());
    EquipmentManager.removeConditionalSkills(EquipmentManager.FAMILIAR, fam.getItem());
    EquipmentManager.addConditionalSkills(EquipmentManager.FAMILIAR, item);
    FamiliarRequest.equipFamiliar(fam, ItemPool.get(itemId, 1));
  }

  public static final void unequipCurrentFamiliar() {
    FamiliarData fam = KoLCharacter.getFamiliar();
    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("Unequip " + fam.getRace());
    EquipmentManager.removeConditionalSkills(EquipmentManager.FAMILIAR, fam.getItem());
    FamiliarRequest.unequipFamiliar(fam);
  }
}
