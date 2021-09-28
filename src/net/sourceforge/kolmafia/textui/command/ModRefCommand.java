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
    StringBuffer buf =
        new StringBuffer("<table border=2>" + "<tr><td colspan=2>NUMERIC MODIFIERS</td></tr>");
    String mod;
    int i = 0;
    while ((mod = Modifiers.getModifierName(i++)) != null) {
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
    buf.append("<tr><td colspan=2>BITMAP MODIFIERS</td></tr>");
    i = 1;
    while ((mod = Modifiers.getBitmapModifierName(i++)) != null) {
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
    buf.append("<tr><td colspan=2>BOOLEAN MODIFIERS</td></tr>");
    i = 0;
    while ((mod = Modifiers.getBooleanModifierName(i++)) != null) {
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
    buf.append("<tr><td colspan=2>STRING MODIFIERS</td></tr>");
    i = 0;
    while ((mod = Modifiers.getStringModifierName(i++)) != null) {
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
