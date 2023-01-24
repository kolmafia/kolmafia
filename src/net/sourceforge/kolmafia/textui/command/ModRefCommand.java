package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;

public class ModRefCommand extends AbstractCommand {
  public ModRefCommand() {
    this.usage = " [<object>] - list all modifiers, show values for player [and object].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, parameters);
    String colSpan = mods == null ? "2" : "3";
    StringBuilder buf =
        new StringBuilder(
            "<table border=2>" + "<tr><td colspan=" + colSpan + ">NUMERIC MODIFIERS</td></tr>");
    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      String modName = mod.getName();
      buf.append("<tr><td>");
      buf.append(modName);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentNumericModifier(mod));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.get(mod));
      }
      buf.append("</td></tr>");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">BITMAP MODIFIERS</td></tr>");
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      String modName = mod.getName();
      buf.append("<tr><td>");
      buf.append(modName);
      buf.append("</td><td>0x");
      buf.append(Integer.toHexString(KoLCharacter.currentRawBitmapModifier(mod)));
      buf.append(" (");
      buf.append(KoLCharacter.currentBitmapModifier(mod));
      buf.append(")");
      if (mods != null) {
        buf.append("</td><td>0x");
        buf.append(Integer.toHexString(mods.getRawBitmap(mod)));
        buf.append(" (");
        buf.append(mods.getBitmap(mod));
        buf.append(")");
      }
      buf.append("</td></tr>");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">BOOLEAN MODIFIERS</td></tr>");
    for (var modifier : BooleanModifier.BOOLEAN_MODIFIERS) {
      String mod = modifier.getName();
      buf.append("<tr><td>");
      buf.append(mod);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentBooleanModifier(modifier));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.getBoolean(modifier));
      }
      buf.append("</td></tr>");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">STRING MODIFIERS</td></tr>");
    for (var modifier : StringModifier.STRING_MODIFIERS) {
      String mod = modifier.getName();
      buf.append("<tr><td>");
      buf.append(mod);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentStringModifier(modifier).replaceAll("\t", "<br>"));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.getString(modifier));
      }
      buf.append("</td></tr>");
    }
    buf.append("</table><br>");
    RequestLogger.printLine(buf.toString());
  }
}
