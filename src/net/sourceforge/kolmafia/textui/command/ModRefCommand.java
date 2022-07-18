package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

public class ModRefCommand extends AbstractCommand {
  public ModRefCommand() {
    this.usage = " [<object>] - list all modifiers, show values for player [and object].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Modifiers mods = Modifiers.getModifiers("Item", parameters);
    String colSpan = mods == null ? "2" : "3";
    StringBuilder buf =
        new StringBuilder(
            "<table border=2>" + "<tr><td colspan=" + colSpan + ">NUMERIC MODIFIERS</td></tr>");
    for (int i = 0; i < Modifiers.DOUBLE_MODIFIERS; i++) {
      String mod = Modifiers.getModifierName(i);
      buf.append("<tr><td>");
      buf.append(mod);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentNumericModifier(mod));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.get(mod));
      }
      buf.append("</td></tr>");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">BITMAP MODIFIERS</td></tr>");
    for (int i = 1; i < Modifiers.BITMAP_MODIFIERS; i++) {
      String mod = Modifiers.getBitmapModifierName(i);
      buf.append("<tr><td>");
      buf.append(mod);
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
    for (int i = 0; i < Modifiers.BOOLEAN_MODIFIERS; i++) {
      String mod = Modifiers.getBooleanModifierName(i);
      buf.append("<tr><td>");
      buf.append(mod);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentBooleanModifier(mod));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.getBoolean(mod));
      }
      buf.append("</td></tr>");
    }
    buf.append("<tr><td colspan=").append(colSpan).append(">STRING MODIFIERS</td></tr>");
    for (int i = 0; i < Modifiers.STRING_MODIFIERS; i++) {
      String mod = Modifiers.getStringModifierName(i);
      buf.append("<tr><td>");
      buf.append(mod);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentStringModifier(mod).replaceAll("\t", "<br>"));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.getString(mod));
      }
      buf.append("</td></tr>");
    }
    buf.append("</table><br>");
    RequestLogger.printLine(buf.toString());
  }
}
