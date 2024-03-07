package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TaleOfDreadCommand extends AbstractCommand {
  public TaleOfDreadCommand() {
    this.usage = " element monster - read the Tale of Dread unlocked by the monster";
  }

  private static final Pattern STORY_PATTERN =
      Pattern.compile(
          "<div class=tiny style='position: absolute; top: 55; left: 365; width: 285; height: 485; overflow-y:scroll; '>(.*?)</div>",
          Pattern.DOTALL);

  public static String extractTale(final String html) {
    Matcher storyMatcher = STORY_PATTERN.matcher(html);
    if (!storyMatcher.find()) {
      return "";
    }

    StringBuffer buffer = new StringBuffer(storyMatcher.group(1));
    StringUtilities.globalStringReplace(buffer, "<br>", "");
    return buffer.toString();
  }

  @Override
  public void run(final String cmd, String parameters) {
    String[] split = parameters.trim().split(" ");
    if (split.length < 2) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Syntax: taleofdread element monster");
      return;
    }

    int story = 0;

    String element = split[0];
    String monster = split[1];

    switch (monster) {
      case "bugbear" -> story = 1;
      case "werewolf" -> story = 6;
      case "zombie" -> story = 11;
      case "ghost" -> story = 16;
      case "vampire" -> story = 21;
      case "skeleton" -> story = 26;
      default -> {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "What kind of dreadful monster is a '" + monster + "'?");
        return;
      }
    }

    switch (element) {
      case "hot" -> story += 0;
      case "cold" -> story += 1;
      case "spooky" -> story += 2;
      case "stench" -> story += 3;
      case "sleaze" -> story += 4;
      default -> {
        KoLmafia.updateDisplay(MafiaState.ERROR, "What kind of element is '" + element + "'?");
        return;
      }
    }

    // inv_use.php?pwd&which=3&whichitem=6423&ajax=1
    // -> choice.php?forceoption=0
    // choice.php?whichchoice=767&whichstory=1

    GenericRequest request =
        new GenericRequest(
            "inv_use.php?pwd&which=3&whichitem=" + ItemPool.TALES_OF_DREAD + "&ajax=1");
    RequestThread.postRequest(request);

    if (!request.responseText.contains("<b>Tales of Dread</b>")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't own the Tales of Dread");
      return;
    }

    // This will redirect to choice.php
    String storyURL = "choice.php?whichchoice=767&whichstory=" + story;

    if (split.length > 2 && split[2].equals("redirect")) {
      // Leave it to the Relay Browser to display the story
      RelayRequest.redirectedCommandURL = "/" + storyURL;
      return;
    }

    // Otherwise, get the story
    request.constructURLString(storyURL);
    RequestThread.postRequest(request);

    RequestLogger.printHtml(TaleOfDreadCommand.extractTale(request.responseText));
  }
}
