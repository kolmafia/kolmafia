package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpecialOutfit implements Comparable<SpecialOutfit> {
  private static final Pattern OPTION_PATTERN =
      Pattern.compile("<option value=['\"]?(.*?)['\"]?>(.*?)</option>");

  private static final Stack<Checkpoint> explicitPoints = new Stack<>();
  private static final Set<Checkpoint> allCheckpoints = new HashSet<>();

  private int outfitId;
  private final String outfitName;
  private String outfitImage;

  // This is TreeMap so that the pieces will be ordered by slot
  private final EnumMap<Slot, AdventureResult> pieces;

  private final ArrayList<AdventureResult> treats;

  private int hash;

  public static final SpecialOutfit NO_CHANGE =
      new SpecialOutfit(Integer.MAX_VALUE, " - No Change - ");
  public static final SpecialOutfit BIRTHDAY_SUIT =
      new SpecialOutfit(Integer.MAX_VALUE, "Birthday Suit");
  public static final SpecialOutfit PREVIOUS_OUTFIT =
      new SpecialOutfit(Integer.MAX_VALUE, "Your Previous Outfit");

  // Pool of outfit ids
  public static final int COLD_COMFORTS = 80;

  public SpecialOutfit(final int outfitId, final String outfitName) {
    this.outfitId = outfitId;
    // The name is normally a substring of the equipment page,
    // and would keep that entire page in memory if not copied.
    this.outfitName = outfitName;
    this.outfitImage = null;
    this.pieces = new EnumMap<>(Slot.class);
    this.hash = 0;
    this.treats = new ArrayList<>();
  }

  public int pieceCount(AdventureResult piece) {
    Slot type = EquipmentManager.itemIdToEquipmentType(piece.getItemId());

    // Everything aside from weapons and accessories can only be equipped once.
    if (type != Slot.WEAPON && type != Slot.ACCESSORY1) {
      return this.pieces.containsValue(piece) ? 1 : 0;
    }

    int count = 0;
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult outfitPiece = this.pieces.get(slot);
      if (null == outfitPiece) {
        continue;
      }

      if (piece.getItemId() == outfitPiece.getItemId()) {
        count++;
      }
    }

    return count;
  }

  public boolean hasAllPieces() {
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult piece = this.pieces.get(slot);
      if (null == piece) {
        continue;
      }

      if (!EquipmentManager.canEquip(piece.getName())) {
        return false;
      }

      if (InventoryManager.getAccessibleCount(piece) < this.pieceCount(piece)) {
        return false;
      }
    }

    return true;
  }

  public boolean isWearing() {
    return this.isWearing(-1);
  }

  public boolean isWearing(AdventureResult piece, Slot type) {
    if (SlotSet.ACCESSORY_SLOTS.contains(type)) {
      int accessoryCount =
          (KoLCharacter.hasEquipped(piece, Slot.ACCESSORY1) ? 1 : 0)
              + (KoLCharacter.hasEquipped(piece, Slot.ACCESSORY2) ? 1 : 0)
              + (KoLCharacter.hasEquipped(piece, Slot.ACCESSORY3) ? 1 : 0);

      if (accessoryCount < this.pieceCount(piece)) {
        return false;
      }
    } else if (type == Slot.WEAPON
        || (type == Slot.OFFHAND
            && ItemDatabase.getConsumptionType(piece.getItemId()) == ConsumptionType.WEAPON)) {
      int weaponCount =
          (KoLCharacter.hasEquipped(piece, Slot.WEAPON) ? 1 : 0)
              + (KoLCharacter.hasEquipped(piece, Slot.OFFHAND) ? 1 : 0);

      if (weaponCount < this.pieceCount(piece)) {
        return false;
      }
    } else if (!KoLCharacter.hasEquipped(piece, type)) {
      return false;
    }

    return true;
  }

  public boolean isWearing(int hash) {
    if ((hash & this.hash) != this.hash) return false;

    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult piece = this.pieces.get(slot);
      if (null == piece) {
        continue;
      }

      if (!this.isWearing(piece, slot)) {
        return false;
      }
    }

    return true;
  }

  public boolean isWearing(Map<Slot, AdventureResult> equipment) {
    return this.isWearing(equipment, -1);
  }

  public boolean isWearing(Map<Slot, AdventureResult> equipment, int hash) {
    if ((hash & this.hash) != this.hash) return false;

    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult piece = this.pieces.get(slot);
      if (null == piece) {
        continue;
      }

      if (!KoLCharacter.hasEquipped(equipment, piece)) {
        return false;
      }
    }

    return true;
  }

  public boolean retrieve() {
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult piece = this.pieces.get(slot);
      if (null == piece) {
        continue;
      }

      // How many copies of this piece of equipment are in this outfit
      int pieceCount = this.pieceCount(piece);

      // How many copies of this piece of equipment are being worn
      int equippedCount = EquipmentManager.equippedCount(piece);

      // How many are not being worn
      int missingCount = pieceCount - equippedCount;

      if (missingCount <= 0) {
        continue;
      }

      if (InventoryManager.getAccessibleCount(piece) >= pieceCount
          && InventoryManager.retrieveItem(
              ItemPool.get(piece.getItemId(), missingCount), true, false)) {
        continue;
      }

      this.updateDisplayMissing();
      return false;
    }

    return true;
  }

  public AdventureResult[] getPieces() {
    return this.pieces.values().toArray(new AdventureResult[0]);
  }

  public static int pieceHash(final AdventureResult piece) {
    if (piece == null || piece == EquipmentRequest.UNEQUIP) {
      return 0;
    }
    return 1 << (piece.getItemId() & 0x1F);
  }

  public static int equipmentHash(Map<Slot, AdventureResult> equipment) {
    int hash = 0;
    // Must consider every slot that can contain an outfit piece
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      hash |= SpecialOutfit.pieceHash(equipment.get(slot));
    }
    return hash;
  }

  public void addTreat(final AdventureResult treat) {
    this.treats.add(treat);
  }

  public void addPiece(final AdventureResult piece) {
    if (piece != EquipmentRequest.UNEQUIP) {
      Slot type = EquipmentManager.itemIdToEquipmentType(piece.getItemId());

      if (null != this.pieces.get(type)) {
        // If a weapon is already equipped, set this piece to the offhand slot.
        // If it is an accessory, find the next empty accessory slot.
        if (type == Slot.WEAPON) {
          type = Slot.OFFHAND;
        } else if (type == Slot.ACCESSORY1) {
          type = Slot.ACCESSORY2;
          if (null != this.pieces.get(type)) {
            type = Slot.ACCESSORY3;
          }
        }
      }

      this.pieces.put(type, piece);
      this.hash |= SpecialOutfit.pieceHash(piece);
    }
  }

  private void updateDisplayMissing() {
    ArrayList<AdventureResult> missing = new ArrayList<>();
    for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
      AdventureResult piece = this.pieces.get(slot);
      if (null == piece) {
        continue;
      }

      if (missing.stream().anyMatch(a -> a.getItemId() == piece.getItemId())) {
        continue;
      }

      int pieceCount = this.pieceCount(piece);
      int accessibleCount = InventoryManager.getAccessibleCount(piece);

      if (accessibleCount < pieceCount) {
        missing.add(ItemPool.get(piece.getItemId(), pieceCount - accessibleCount));
      }
    }

    for (AdventureResult item : missing) {
      RequestLogger.printLine(
          MafiaState.ERROR,
          "You need " + item.getCount() + " more " + item.getName() + " to continue.");
    }
    KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to wear outfit " + this.getName() + ".");
  }

  @Override
  public String toString() {
    return this.outfitName;
  }

  public int getOutfitId() {
    return this.outfitId;
  }

  public String getName() {
    return this.outfitName;
  }

  public void setImage(final String image) {
    this.outfitImage = image;
  }

  public String getImage() {
    return this.outfitImage;
  }

  public ArrayList<AdventureResult> getTreats() {

    if (this.getOutfitId() == SpecialOutfit.COLD_COMFORTS) {
      if (ItemPool.get(ItemPool.RUSSIAN_ICE).getCount(KoLConstants.inventory) > 0) {
        this.treats.add(ItemPool.get(ItemPool.DOUBLE_ICE_GUM));
      } else {
        this.treats.clear();
      }
    }

    return this.treats;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SpecialOutfit)) {
      return false;
    }

    if (this.outfitId != ((SpecialOutfit) o).outfitId) {
      return false;
    }

    return this.outfitName.equalsIgnoreCase(((SpecialOutfit) o).outfitName);
  }

  @Override
  public int hashCode() {
    return this.outfitId;
  }

  @Override
  public int compareTo(final SpecialOutfit o) {
    if (!(o instanceof SpecialOutfit)) {
      return -1;
    }

    return this.outfitName.compareToIgnoreCase(o.outfitName);
  }

  /**
   * Creates a checkpoint. This should be called whenever the player needs an outfit marked to
   * revert to.
   */
  public static final void createExplicitCheckpoint() {
    synchronized (SpecialOutfit.class) {
      SpecialOutfit.explicitPoints.push(new Checkpoint());
    }
  }

  /**
   * Restores a checkpoint. This should be called whenever the player needs to revert to their
   * checkpointed outfit.
   */
  public static final void restoreExplicitCheckpoint() {
    synchronized (SpecialOutfit.class) {
      if (SpecialOutfit.explicitPoints.isEmpty()) {
        return;
      }

      SpecialOutfit.explicitPoints.pop().close();
    }
  }

  /**
   * static final method used to determine all of the custom outfits, based on the given HTML
   * enclosed in <code><select></code> tags.
   */
  public static final void checkOutfits(final String selectHTML) {
    // Punt immediately if no outfits
    if (selectHTML == null) {
      return;
    }

    Matcher singleOutfitMatcher = SpecialOutfit.OPTION_PATTERN.matcher(selectHTML);

    while (singleOutfitMatcher.find()) {
      int outfitId = StringUtilities.parseInt(singleOutfitMatcher.group(1));
      if (outfitId >= 0) {
        continue;
      }

      String outfitName = singleOutfitMatcher.group(2);
      SpecialOutfit outfit = EquipmentManager.getCustomOutfit(outfitName);
      if (outfit == null) {
        outfit = new SpecialOutfit(outfitId, outfitName);
        EquipmentManager.addCustomOutfit(outfit);
      }

      if (outfitId != outfit.outfitId) {
        // Id has changed
        outfit.outfitId = outfitId;
      }
    }
  }

  /**
   * Method to remove a particular piece of equipment from all active checkpoints, after it has been
   * transformed or consumed.
   */
  public static final void forgetEquipment(AdventureResult item) {
    SpecialOutfit.replaceEquipment(item, EquipmentRequest.UNEQUIP);
  }

  /**
   * Method to replace a particular piece of equipment in all active checkpoints, after it has been
   * transformed or consumed.
   */
  public static void replaceEquipment(AdventureResult item, AdventureResult replaceWith) {
    synchronized (SpecialOutfit.class) {
      for (Checkpoint checkpoint : SpecialOutfit.allCheckpoints) {
        for (var entry : checkpoint.entries()) {
          if (item.equals(entry.getValue())) {
            checkpoint.set(entry.getKey(), replaceWith);
          }
        }
      }
    }
  }

  public static void replaceEquipmentInSlot(AdventureResult item, Slot slot) {
    synchronized (SpecialOutfit.class) {
      for (Checkpoint checkpoint : SpecialOutfit.allCheckpoints) {
        if (SlotSet.SLOTS.contains(slot)) {
          checkpoint.set(slot, item);
        }
      }
    }
  }

  /**
   * Method to remove all active checkpoints, in cases where restoring the original outfit is
   * undesirable (currently, Fernswarthy's Basement).
   */
  public static final void forgetCheckpoints() {
    synchronized (SpecialOutfit.class) {
      SpecialOutfit.allCheckpoints.clear();
      SpecialOutfit.explicitPoints.clear();
    }
  }

  public static class Checkpoint implements AutoCloseable {
    private final EnumMap<Slot, AdventureResult> slots = new EnumMap<>(Slot.class);

    private boolean checking;
    private Supplier<Boolean> checkingCallback = null;

    public Checkpoint(boolean checking) {
      this.checking = checking;
      boolean notEmpty = false;
      for (var slot : SlotSet.SLOTS) {
        AdventureResult item = EquipmentManager.getEquipment(slot);
        this.slots.put(slot, item);
        notEmpty |= slot != Slot.FAMILIAR && !item.equals(EquipmentRequest.UNEQUIP);
      }
      if (!notEmpty) {
        String message = "Created an empty checkpoint.";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
      synchronized (SpecialOutfit.class) {
        SpecialOutfit.allCheckpoints.add(this);
      }
    }

    public Checkpoint() {
      this(false);
    }

    public Checkpoint(Supplier<Boolean> checkingCallback) {
      this();
      this.checkingCallback = checkingCallback;
    }

    public boolean checking() {
      if (checkingCallback != null) {
        return checkingCallback.get();
      }

      return checking;
    }

    public AdventureResult get(final Slot slot) {
      return this.slots.getOrDefault(slot, null);
    }

    public void set(final Slot slot, final AdventureResult item) {
      if (SlotSet.SLOTS.contains(slot)) {
        this.slots.put(slot, item);
      }
    }

    private void restore() {
      for (var entry : slots.entrySet()) {
        if (KoLmafia.refusesContinue()) break;
        var slot = entry.getKey();
        AdventureResult item = entry.getValue();

        if (item == null) {
          continue;
        }

        AdventureResult equippedItem = EquipmentManager.getEquipment(slot);

        if (equippedItem.equals(item)) {
          continue;
        }

        if (slot == Slot.FAMILIAR) {
          FamiliarData familiar = KoLCharacter.getFamiliar();
          if (familiar == FamiliarData.NO_FAMILIAR) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "You have no familiar with you.");
            continue;
          }
          if (!familiar.canEquip(item)) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "Your " + familiar.getRace() + " can't wear a " + item.getName());
            continue;
          }
        }

        RequestThread.postRequest(new EquipmentRequest(item, slot));
      }
    }

    @Override
    public void close() {
      if (!this.checking()) {
        // If this checkpoint has been closed, don't restore using it
        if (!this.known()) {
          return;
        }

        this.restore();
      }

      synchronized (SpecialOutfit.class) {
        SpecialOutfit.allCheckpoints.remove(this);
      }
    }

    public boolean known() {
      // For use with "try with resource", once we upgrade to a better Java version
      synchronized (SpecialOutfit.class) {
        return SpecialOutfit.allCheckpoints.contains(this);
      }
    }

    public Set<Map.Entry<Slot, AdventureResult>> entries() {
      return slots.entrySet();
    }
  }
}
