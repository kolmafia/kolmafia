package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

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
    for (var mod : Modifiers.DOUBLE_MODIFIERS) {
      String modName = mod.getName();
      buf.append("<tr><td>");
      buf.append(modName);
      buf.append("</td><td>");
      buf.append(KoLCharacter.currentNumericModifier(modName));
      if (mods != null) {
        buf.append("</td><td>");
        buf.append(mods.get(modName));
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
    for (var modifier : Modifiers.STRING_MODIFIERS) {
      String mod = modifier.getName();
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
