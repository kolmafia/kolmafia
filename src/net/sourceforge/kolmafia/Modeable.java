package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.textui.command.BackupCameraCommand;
import net.sourceforge.kolmafia.textui.command.EdPieceCommand;
import net.sourceforge.kolmafia.textui.command.ModeCommand;
import net.sourceforge.kolmafia.textui.command.RetroCapeCommand;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.textui.command.UmbrellaCommand;

public enum Modeable {
  EDPIECE(
      "edpiece",
      "edPiece",
      ItemPool.get(ItemPool.CROWN_OF_ED),
      "Edpiece",
      new EdPieceCommand(),
      false),
  RETROCAPE(
      "retrocape",
      null,
      ItemPool.get(ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE),
      "RetroCape",
      new RetroCapeCommand(),
      false),
  BACKUPCAMERA(
      "backupcamera",
      "backupCameraMode",
      ItemPool.get(ItemPool.BACKUP_CAMERA),
      "BackupCamera",
      new BackupCameraCommand(),
      true),
  UMBRELLA(
      "umbrella",
      "umbrellaState",
      ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA),
      "UnbreakableUmbrella",
      new UmbrellaCommand(),
      true),
  SNOWSUIT(
      "snowsuit",
      "snowsuit",
      ItemPool.get(ItemPool.SNOW_SUIT),
      "Snowsuit",
      new SnowsuitCommand(),
      false);

  private final String command;
  private final String statePref;
  private final AdventureResult item;
  private final String modifier;
  private final ModeCommand commandInstance;
  private final boolean equipAfterChange;

  Modeable(
      final String command,
      final String statePref,
      final AdventureResult item,
      final String modifier,
      final ModeCommand commandInstance,
      final boolean equipAfterChange) {
    this.command = command;
    this.statePref = statePref;
    this.item = item;
    this.modifier = modifier;
    this.commandInstance = commandInstance;
    this.equipAfterChange = equipAfterChange;
  }

  public String getCommand() {
    return this.command;
  }

  public AdventureResult getItem() {
    return this.item;
  }

  public int getItemId() {
    return this.item.getItemId();
  }

  public int getSlot() {
    return EquipmentManager.consumeFilterToEquipmentType(
        ItemDatabase.getConsumptionType(this.item.getItemId()));
  }

  public boolean validate(final String command, final String parameters) {
    return this.commandInstance.validate(command, parameters);
  }

  public Set<String> getModes() {
    return this.commandInstance.getModes();
  }

  public String getState() {
    if (this == RETROCAPE) {
      return (Preferences.getString("retroCapeSuperhero")
              + " "
              + Preferences.getString("retroCapeWashingInstructions"))
          .trim();
    }

    return Preferences.getString(this.statePref);
  }

  public String getModifier() {
    return this.modifier;
  }

  public boolean getEquipAfterChange() {
    return this.equipAfterChange;
  }

  public static Modeable find(final AdventureResult item) {
    return find(item.getItemId());
  }

  public static Modeable find(final String command) {
    return Arrays.stream(values())
        .filter(m -> m.getCommand().equalsIgnoreCase(command))
        .findAny()
        .orElse(null);
  }

  public static Modeable find(final int itemId) {
    return Arrays.stream(values())
        .filter(m -> m.getItem().getItemId() == itemId)
        .findAny()
        .orElse(null);
  }

  public static Map<Modeable, String> getStringMap(Function<Modeable, String> cb) {
    return Arrays.stream(values()).collect(Collectors.toMap(m -> m, cb));
  }

  public static Map<Modeable, String> getStateMap() {
    return getStringMap(Modeable::getState);
  }

  public static Map<Modeable, Boolean> getBooleanMap(final Function<Modeable, Boolean> cb) {
    return Arrays.stream(values()).collect(Collectors.toMap(m -> m, cb));
  }

  public static Map<Modeable, Boolean> getBooleanMap() {
    return getBooleanMap(m -> false);
  }
}
