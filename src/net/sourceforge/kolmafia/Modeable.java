package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.command.*;

public enum Modeable {
  EDPIECE(
      "edpiece", "edPiece", ItemPool.get(ItemPool.CROWN_OF_ED), "Edpiece", new EdPieceCommand()),
  RETROCAPE(
      "retrocape",
      null,
      ItemPool.get(ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE),
      "RetroCape",
      new RetroCapeCommand()),
  BACKUPCAMERA(
      "backupcamera",
      "backupCameraMode",
      ItemPool.get(ItemPool.BACKUP_CAMERA),
      "BackupCamera",
      new BackupCameraCommand()),
  UMBRELLA(
      "umbrella",
      "umbrellaState",
      ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA),
      "UnbreakableUmbrella",
      new UmbrellaCommand()),
  SNOWSUIT(
      "snowsuit", "snowsuit", ItemPool.get(ItemPool.SNOW_SUIT), "Snowsuit", new SnowsuitCommand());

  private final String command;
  private final String statePref;
  private final AdventureResult item;
  private final String modifier;
  private final AbstractModeCommand commandInstance;

  Modeable(
      final String command,
      final String statePref,
      final AdventureResult item,
      final String modifier,
      final AbstractModeCommand commandInstance) {
    this.command = command;
    this.statePref = statePref;
    this.item = item;
    this.modifier = modifier;
    this.commandInstance = commandInstance;
  }

  public String getCommand() {
    return this.command;
  }

  public AdventureResult getItem() {
    return this.item;
  }

  public boolean validate(final String command, final String parameters) {
    return this.commandInstance.validate(command, parameters);
  }

  public String getState() {
    if (this == RETROCAPE) {
      return Preferences.getString("retroCapeSuperhero")
          + " "
          + Preferences.getString("retroCapeWashingInstructions");
    }

    return Preferences.getString(this.statePref);
  }

  public String getModifier() {
    return this.modifier;
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

  public static Map<Modeable, String> getStateMap() {
    return Arrays.stream(values()).collect(Collectors.toMap(m -> m, Modeable::getState));
  }

  public static Map<Modeable, Boolean> getBooleanMap() {
    return Arrays.stream(values()).collect(Collectors.toMap(m -> m, m -> false));
  }
}
