package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI.ParameterHandling;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

public class SpeculateCommand extends AbstractCommand {
  public SpeculateCommand() {
    this.usage =
        " MCD <num> | equip [<slot>] <item> | unequip <slot> | familiar <type> | enthrone <type> | bjornify <type> | up <eff> | uneffect <eff> | quiet ; [<another>;...] - predict modifiers.";

    this.flags = ParameterHandling.FULL_LINE;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Speculation spec = new Speculation();
    boolean quiet = spec.parse(parameters);
    Modifiers mods = spec.calculate();
    ModifierDatabase.overrideModifier(ModifierType.GENERATED, "_spec", mods);
    if (quiet) {
      return;
    }
    String table = SpeculateCommand.getHTML(mods, "");
    if (table != null) {
      RequestLogger.printLine(table + "<br>");
    } else {
      RequestLogger.printLine("No modifiers changed.");
    }
  }

  public static String getHTML(Modifiers mods, String attribs) {
    StringBuilder buf = new StringBuilder("<table border=2 ");
    buf.append(attribs);
    buf.append(">");
    int len = buf.length();
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      handleDouble(mod, mods, buf);
    }
    for (var mod : DerivedModifier.DERIVED_MODIFIERS) {
      handleDerived(mod, mods, buf);
    }
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      handleBitmap(mod, mods, buf);
    }
    for (var mod : BooleanModifier.BOOLEAN_MODIFIERS) {
      String modName = mod.getName();
      boolean was = KoLCharacter.currentBooleanModifier(mod);
      boolean now = mods.getBoolean(mod);
      if (now == was) {
        continue;
      }
      buf.append("<tr><td>");
      buf.append(modName);
      buf.append("</td><td>");
      buf.append(now);
      buf.append("</td></tr>");
    }
    for (var modifier : StringModifier.STRING_MODIFIERS) {
      String mod = modifier.getName();
      String was = KoLCharacter.currentStringModifier(modifier);
      String now = mods.getString(modifier);
      if (now.equals(was)) {
        continue;
      }
      if (was.equals("")) {
        buf.append("<tr><td>");
        buf.append(mod);
        buf.append("</td><td>");
        buf.append(now.replaceAll("\t", "<br>"));
        buf.append("</td></tr>");
      } else {
        buf.append("<tr><td rowspan=2>");
        buf.append(mod);
        buf.append("</td><td>");
        buf.append(was.replaceAll("\t", "<br>"));
        buf.append("</td></tr><tr><td>");
        buf.append(now.replaceAll("\t", "<br>"));
        buf.append("</td></tr>");
      }
    }
    if (buf.length() > len) {
      buf.append("</table>");
      return buf.toString();
    }
    return null;
  }

  private static void handleDouble(
      final DoubleModifier mod, final Modifiers mods, final StringBuilder buf) {
    double was = KoLCharacter.currentNumericModifier(mod);
    double now = mods.getDouble(mod);
    if (now == was) {
      return;
    }
    doNumeric(mod.getName(), was, now, buf);
  }

  private static void handleDerived(
      final DerivedModifier mod, final Modifiers mods, final StringBuilder buf) {
    double was = KoLCharacter.currentDerivedModifier(mod);
    double now = mods.getDerived(mod);
    if (now == was) {
      return;
    }
    doNumeric(mod.getName(), was, now, buf);
  }

  private static void handleBitmap(
      final BitmapModifier mod, final Modifiers mods, final StringBuilder buf) {
    double was = KoLCharacter.currentBitmapModifier(mod);
    double now = mods.getBitmap(mod);
    if (now == was) {
      return;
    }
    doNumeric(mod.getName(), was, now, buf);
  }

  private static void doNumeric(
      final String mod, final double was, final double now, final StringBuilder buf) {
    buf.append("<tr><td>");
    buf.append(mod);
    buf.append("</td><td>");
    buf.append(KoLConstants.FLOAT_FORMAT.format(now));
    buf.append(" (");
    if (now > was) {
      buf.append("+");
    }
    buf.append(KoLConstants.FLOAT_FORMAT.format(now - was));
    buf.append(")</td></tr>");
  }
}
