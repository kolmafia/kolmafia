package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoAttackCommand extends AbstractCommand {
  private static final GenericRequest AUTO_ATTACKER =
      new GenericRequest("account.php?action=autoattack&ajax=1&pwd");

  public AutoAttackCommand() {
    this.usage = " <skill> - set default attack method.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();

    if (parameters.equals("")) {
      printCurrentAutoAttack();
      return;
    }

    if (setAutoAttackSkill(parameters.toLowerCase())) {
      printCurrentAutoAttack();
      return;
    }

    if (parameters.startsWith("/")) {
      return;
    }

    if (!ChatManager.chatLiterate()) {
      KoLmafia.updateDisplay("Chat commands are not available for this user.");
      return;
    }

    ChatSender.executeMacro("/aa " + parameters);
  }

  protected void printCurrentAutoAttack() {
    int aa = KoLCharacter.getAutoAttackAction();
    if (aa == 0) {
      KoLmafia.updateDisplay("Autoattack is disabled.");
    } else if (aa == 1) {
      KoLmafia.updateDisplay("Autoattack: attack with weapon.");
    } else if (aa == 3) {
      KoLmafia.updateDisplay("Autoattack: pick pocket.");
    } else {
      String skillName = SkillDatabase.getSkillName(aa);
      KoLmafia.updateDisplay("Autoattack: " + skillName);
    }
  }

  protected boolean setAutoAttackSkill(String attackName) {
    int skillId = -1;

    // Check to see if it's a known skill / attack

    if (attackName.equals("none") || attackName.indexOf("disable") != -1) {
      skillId = 0;
    } else if (attackName.equals("attack") || attackName.startsWith("attack ")) {
      skillId = 1;
    } else if (attackName.equals("steal")
        || attackName.equals("pickpocket")
        || attackName.equals("pick pocket")) {
      skillId = 3;
    } else {
      if (Character.isDigit(attackName.charAt(0))) {
        skillId = StringUtilities.parseInt(attackName);
      } else {
        List<UseSkillRequest> combatSkills = SkillDatabase.getSkillsByType(SkillDatabase.COMBAT);
        String skillName = SkillDatabase.getSkillName(attackName, combatSkills);

        if (skillName != null) {
          skillId = SkillDatabase.getSkillId(skillName);
        }
      }

      // If it's not something that KoLmafia recognizes, fall through to KoL chat's implementation

      if (skillId == -1 || ((skillId == 2 || skillId > 3) && !KoLCharacter.hasSkill(skillId))) {
        return false;
      }
    }

    if (skillId != KoLCharacter.getAutoAttackAction()) {
      AutoAttackCommand.AUTO_ATTACKER.addFormField("value", String.valueOf(skillId));
      RequestThread.postRequest(AutoAttackCommand.AUTO_ATTACKER);
    }

    return true;
  }
}
