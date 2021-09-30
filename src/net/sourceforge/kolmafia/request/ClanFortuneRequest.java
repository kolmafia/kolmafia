package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanFortuneRequest extends GenericRequest {
  public enum Buff {
    FAMILIAR("-1"),
    ITEM("-2"),
    MEAT("-3"),
    MUSCLE("-4"),
    MYSTICALITY("-5"),
    MOXIE("-6");

    private final String value;

    Buff(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  // preaction=lovetester
  // choice.php?whichchoice=1278&option=1
  // which: 1 (clanmate), -1 (susie), -2 (hagnk), -3 (meatsmith), -4 muscle -5 myst -6 moxie
  // whichid=clanmate
  // q1=food q2=character q3=word

  private static final Pattern USES_PATTERN = Pattern.compile("clanmate (\\d) time");
  private static final Pattern TARGET_PATTERN = Pattern.compile("whichid=(.*?)&");

  public ClanFortuneRequest() {
    super("choice.php");
  }

  public ClanFortuneRequest(final Buff buff) {
    this(
        buff,
        Preferences.getString("clanFortuneWord1"),
        Preferences.getString("clanFortuneWord2"),
        Preferences.getString("clanFortuneWord3"));
  }

  public ClanFortuneRequest(
      final Buff buff, final String word1, final String word2, final String word3) {
    super("choice.php");
    this.addFormField("whichchoice", "1278");
    this.addFormField("option", "1");
    this.addFormField("which", buff.getValue());
    this.addFormField("q1", word1);
    this.addFormField("q2", word2);
    this.addFormField("q3", word3);
  }

  public ClanFortuneRequest(final String name) {
    this(
        name,
        Preferences.getString("clanFortuneWord1"),
        Preferences.getString("clanFortuneWord2"),
        Preferences.getString("clanFortuneWord3"));
  }

  public ClanFortuneRequest(
      final String name, final String word1, final String word2, final String word3) {
    super("choice.php");
    this.addFormField("whichchoice", "1278");
    this.addFormField("option", "1");
    this.addFormField("which", "1");
    this.addFormField("whichid", name);
    this.addFormField("q1", word1);
    this.addFormField("q2", word2);
    this.addFormField("q3", word3);
  }
  // choice.php?whichchoice=1278&which=1&whichid=cheesefax&q1=food&q2=batman&q3=thick
  // choice.php?pwd&whichchoice=1278&option=1&which=1&whichid=cheesefax&q1=food&q2=batman&q3=thick

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (!KoLmafia.permitsContinue()) {
      return;
    }

    ClanLoungeRequest request =
        new ClanLoungeRequest(ClanLoungeRequest.FORTUNE) {
          @Override
          protected boolean shouldFollowRedirect() {
            return true;
          }
        };

    RequestThread.postRequest(request);
    super.run();
  }

  @Override
  public void processResults() {
    ClanFortuneRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if ((!urlString.startsWith("choice.php") && !urlString.contains("preaction=lovetester"))
        || responseText == null) {
      return;
    }

    if (!responseText.contains("Relationship Fortune Teller")) {
      String message = "There is no Fortune Teller in this clan";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      return;
    }

    Preferences.setBoolean(
        "_clanFortuneBuffUsed", !responseText.contains("resident of Seaside Town"));

    Matcher matcher = USES_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int usesLeft = StringUtilities.parseInt(matcher.group(1));
      Preferences.setInteger("_clanFortuneConsultUses", 3 - usesLeft);
    } else {
      Preferences.setInteger("_clanFortuneConsultUses", 3);
    }

    matcher = TARGET_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return;
    }

    // You can only consult Madame Zatara about someone in your clan.
    if (responseText.contains("about someone in your clan")) {
      String message = matcher.group(1) + " is not in your clan";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
    // Couldn't find "chesefax" to test with.
    else if (responseText.contains("Couldn't find")) {
      String message = "Couldn't find " + matcher.group(1);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
    // You're already waiting on your results with cheesefax.
    else if (responseText.contains("already waiting")) {
      String message = "Already waiting on results from " + matcher.group(1);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
    // You enter your answers and wait for cheesefax to answer, so you can get your results!
    else if (responseText.contains("enter your answers and wait")) {
      String message = "You enter your answers and wait for " + matcher.group(1);
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
  }
  // You may consult Madame Zatara about your relationship with a resident of Seaside Town.

  // You may still consult Madame Zatara about your relationship with a clanmate 3 times today.
  // You may still consult Madame Zatara about your relationship with a clanmate 1 time today.
}
