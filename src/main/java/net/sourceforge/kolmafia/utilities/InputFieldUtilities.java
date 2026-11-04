package net.sourceforge.kolmafia.utilities;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class InputFieldUtilities {
  private InputFieldUtilities() {}

  public static final void alert(final String message) {
    UserInterfaceManager.getUserInterface().alert(message);
  }

  public static final boolean confirm(final String message) {
    return UserInterfaceManager.getUserInterface().confirm(message);
  }

  public static final String input(final String message) {
    return UserInterfaceManager.getUserInterface().input(message);
  }

  public static final String input(final String message, final String initial) {
    return UserInterfaceManager.getUserInterface().input(message, initial);
  }

  public static final <T> T input(
      final String message, final LockableListModel<T> inputs, final T initial) {

    return input(message, inputs, null, initial);
  }

  public static final <T> T input(
      final String message,
      final LockableListModel<T> inputs,
      BiFunction<Object, Boolean, String> toHTMLFunction,
      final T initial) {
    return UserInterfaceManager.getUserInterface().input(message, inputs, toHTMLFunction, initial);
  }

  public static final <T> T input(final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs) {
    if (inputs == null || inputs.length == 0) {
      return null;
    }

    return InputFieldUtilities.input(message, inputs, null);
  }

  public static final <T> T input(final String message, final T[] inputs, final T initial) {
    if (inputs == null || inputs.length == 0) {
      return null;
    }

    // Keep simple input dialog (no AutoFilterTextField) if there
    // are only a few input choices: booleans, stats, classes, ...
    if (inputs.length <= 12) {
      return UserInterfaceManager.getUserInterface().input(message, inputs, initial);
    }

    return InputFieldUtilities.input(
        message, new LockableListModel<>(Arrays.asList(inputs)), initial);
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs) {
    return InputFieldUtilities.multiple(message, inputs, null);
  }

  public static final <T> List<T> multiple(
      final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
    return UserInterfaceManager.getUserInterface().multiple(message, inputs, filter);
  }

  public static final Integer getQuantity(final String title, final int maximumValue) {
    return InputFieldUtilities.getQuantity(title, maximumValue, maximumValue);
  }

  public static final Integer getQuantity(
      final String message, final int maximumValue, int defaultValue) {
    // Check parameters; avoid programmer error.
    if (defaultValue > maximumValue) {
      defaultValue = maximumValue;
    }

    if (maximumValue == 1 && maximumValue == defaultValue) {
      return 1;
    }

    String currentValue =
        InputFieldUtilities.input(message, KoLConstants.COMMA_FORMAT.format(defaultValue));

    if (currentValue == null) {
      return null;
    }

    if (currentValue.equals("*")) {
      return maximumValue;
    }

    int desiredValue = StringUtilities.parseIntInternal2(currentValue);
    if (desiredValue < 0) {
      return maximumValue - desiredValue;
    }

    return Math.min(desiredValue, maximumValue);
  }
}
