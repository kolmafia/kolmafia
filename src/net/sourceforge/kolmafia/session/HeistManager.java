package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.GenericRequest;

public class HeistManager {
  private String response;

  private static final Pattern HEIST_COUNT = Pattern.compile("(\\d+) more heists available");
  private static final Pattern MONSTER =
      Pattern.compile("From (?<pronoun>[^ ]*) (?<monster>.*?):<br />(?<items>(<input [^/]+ />)+)");
  private static final Pattern ITEM =
      Pattern.compile(
          "<input type=\"submit\" name=\"st:(?<monsterId>\\d+):(?<itemId>\\d+)\" value=\"(?<itemName>[^\"]+)\" class=\"button\" />");

  public static class HeistData {
    public final int heists;
    public final LinkedHashMap<HeistMonster, List<HeistItem>> heistables;

    public HeistData(int heists, LinkedHashMap<HeistMonster, List<HeistItem>> heistables) {
      this.heists = heists;
      this.heistables = heistables;
    }
  }

  public static class HeistMonster {
    public final int id;
    public final String pronoun;
    public final String name;

    public HeistMonster(int id, String pronoun, String name) {
      this.id = id;
      this.pronoun = pronoun;
      this.name = name;
    }
  }

  public static class HeistItem {
    public final int id;
    public final String name;

    public HeistItem(int id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  public HeistData getHeistTargets() {
    var response = getRequest();
    int heists = 0;

    Matcher countMatcher = HEIST_COUNT.matcher(response);
    if (countMatcher.find()) {
      heists = Integer.parseInt(countMatcher.group(1));
    }

    LinkedHashMap<HeistMonster, List<HeistItem>> heistables = new LinkedHashMap<>();

    Matcher monsterMatcher = MONSTER.matcher(response);
    while (monsterMatcher.find()) {
      String pronoun = monsterMatcher.group("pronoun");
      String monster = monsterMatcher.group("monster");
      int monsterId = -1;

      List<HeistItem> items = new ArrayList<>();
      Matcher itemMatcher = ITEM.matcher(monsterMatcher.group("items"));
      while (itemMatcher.find()) {
        monsterId = Integer.parseInt(itemMatcher.group("monsterId"));
        items.add(
            new HeistItem(
                Integer.parseInt(itemMatcher.group("itemId")), itemMatcher.group("itemName")));
      }

      heistables.put(new HeistMonster(monsterId, pronoun, monster), items);
    }

    return new HeistData(heists, heistables);
  }

  public boolean heist(int itemId) {
    return heist(String.valueOf(itemId));
  }

  private boolean heist(String itemId) {
    var response = getRequest();
    Matcher itemMatcher = ITEM.matcher(response);
    boolean found = false;
    while (itemMatcher.find()) {
      if (!itemId.equals(itemMatcher.group("itemId"))) continue;

      found = true;
      break;
    }

    if (!found) {
      return false;
    }

    String monsterId = itemMatcher.group("monsterId");
    String itemName = itemMatcher.group("itemName");

    GenericRequest request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1320");
    request.addFormField("option", "1");
    request.addFormField("st:" + monsterId + ":" + itemId, itemName);
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    return true;
  }

  protected String heistRequest() {
    var heistRequest = new GenericRequest("main.php?heist=1");
    RequestThread.postRequest(heistRequest);
    response = heistRequest.responseText;
    return response;
  }

  private String getRequest() {
    if (response != null) {
      return response;
    }
    return heistRequest();
  }
}
