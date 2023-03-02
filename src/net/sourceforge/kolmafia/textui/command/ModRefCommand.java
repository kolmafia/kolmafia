package net.sourceforge.kolmafia.textui.command;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ModRefCommand extends AbstractCommand {
  public ModRefCommand() {
    this.usage =
        " [<filter>] [<object>] - list all modifiers, show values for player [and object].";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String filter = null;
    Modifiers mods = null;
    String[] tokens = parameters.split("\\s+", 2);
    if (tokens.length > 0 && !tokens[0].equals("")) {
      mods = ModifierDatabase.getModifiers(ModifierType.ITEM, parameters);
      if (mods == null) {
        // assume first word is a filter
        filter = tokens[0].toLowerCase();
        if (tokens.length == 2) {
          parameters = tokens[1];
        }
        mods = ModifierDatabase.getModifiers(ModifierType.ITEM, parameters);
      }
    }
    String colSpan = mods == null ? "2" : "3";
    StringBuilder buf =
        new StringBuilder(
            "<table border=2>" + "<tr><td colspan=" + colSpan + ">NUMERIC MODIFIERS</td></tr>");
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      addModRow(
          buf,
          mod,
          filter,
          mods,
          (m) -> String.valueOf(KoLCharacter.currentNumericModifier(m)),
          (n, m) -> String.valueOf(n.getDouble(m)));
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">BITMAP MODIFIERS</td></tr>");
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      addModRow(
          buf,
          mod,
          filter,
          mods,
          (m) ->
              "0x"
                  + Integer.toHexString(KoLCharacter.currentRawBitmapModifier(m))
                  + " ("
                  + KoLCharacter.currentBitmapModifier(m)
                  + ")",
          (n, m) -> "0x" + Integer.toHexString(n.getRawBitmap(m)) + " (" + n.getBitmap(m) + ")");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">BOOLEAN MODIFIERS</td></tr>");
    for (var modifier : BooleanModifier.BOOLEAN_MODIFIERS) {
      addModRow(
          buf,
          modifier,
          filter,
          mods,
          (m) -> String.valueOf(KoLCharacter.currentBooleanModifier(m)),
          (n, m) -> String.valueOf(n.getBoolean(m)));
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">STRING MODIFIERS</td></tr>");
    for (var modifier : StringModifier.STRING_MODIFIERS) {
      addModRow(
          buf,
          modifier,
          filter,
          mods,
          (m) -> KoLCharacter.currentStringModifier(m).replaceAll("\t", "<br>"),
          Modifiers::getString);
    }
    buf.append("</table><br>");
    RequestLogger.printLine(buf.toString());
  }

  private <T extends Modifier> void addModRow(
      StringBuilder buf,
      T modifier,
      String filter,
      Modifiers mods,
      Function<T, String> charModString,
      BiFunction<Modifiers, T, String> modString) {
    String mod = modifier.getName();
    if (filter != null && !StringUtilities.matchesFilter(mod.toLowerCase(), filter)) {
      return;
    }
    buf.append("<tr><td>");
    buf.append(mod);
    buf.append("</td><td>");
    buf.append(charModString.apply(modifier));
    if (mods != null) {
      buf.append("</td><td>");
      buf.append(modString.apply(mods, modifier));
    }
    buf.append("</td></tr>");
  }
}
