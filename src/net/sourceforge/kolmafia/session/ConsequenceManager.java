package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public abstract class ConsequenceManager {
  private static final HashMap<String, Consequence> itemDescs = new HashMap<String, Consequence>();
  private static final HashMap<String, Consequence> effectDescs =
      new HashMap<String, Consequence>();
  private static final HashMap<String, Consequence> skillDescs = new HashMap<String, Consequence>();
  private static final ArrayList<String> descriptions = new ArrayList<String>();
  private static final HashMap<String, Consequence> monsters = new HashMap<String, Consequence>();
  private static final ArrayList<Consequence> accomplishments = new ArrayList<Consequence>();

  private static final Pattern GROUP_PATTERN = Pattern.compile("\\$(\\d)");
  private static final Pattern EXPR_PATTERN = Pattern.compile("\\[(.+?)\\]");

  static {
    BufferedReader reader =
        FileUtilities.getVersionedReader("consequences.txt", KoLConstants.CONSEQUENCES_VERSION);
    String[] data;

    // Format is: type / spec / regex / action...

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 4) {
        continue;
      }

      Pattern patt;
      try {
        patt = Pattern.compile(data[2]);
      } catch (PatternSyntaxException e) {
        RequestLogger.printLine("Consequence " + data[0] + "/" + data[1] + ": " + e);
        continue;
      }

      ConsequenceManager.addConsequence(new Consequence(data, patt));
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static void addConsequence(Consequence cons) {
    String type = cons.getType();
    String spec = cons.getSpec();

    if (type.equals("DESC_ITEM")) {
      String key = ItemDatabase.getDescriptionId(ItemDatabase.getItemId(spec));
      if (key == null) {
        RequestLogger.printLine("Unknown DESC_ITEM consequence: " + spec);
      } else {
        cons.register(ConsequenceManager.itemDescs, key);
        ConsequenceManager.descriptions.add("desc_item.php?whichitem=" + key);
      }
    } else if (type.equals("DESC_SKILL")) {
      int id = SkillDatabase.getSkillId(spec);
      if (id == -1) {
        RequestLogger.printLine("Unknown DESC_SKILL consequence: " + spec);
      } else {
        Integer key = IntegerPool.get(id);
        cons.register(ConsequenceManager.skillDescs, key);
        ConsequenceManager.descriptions.add("desc_skill.php?whichskill=" + id + "&self=true");
      }
    } else if (type.equals("DESC_EFFECT")) {
      String key = EffectDatabase.getDescriptionId(EffectDatabase.getEffectId(spec));
      if (key == null) {
        RequestLogger.printLine("Unknown DESC_EFFECT consequence: " + spec);
      } else {
        cons.register(ConsequenceManager.effectDescs, key);
        ConsequenceManager.descriptions.add("desc_effect.php?whicheffect=" + key);
      }
    } else if (type.equals("MONSTER")) {
      String key = spec;
      cons.register(ConsequenceManager.monsters, key);
    } else if (type.equals("QUEST_LOG")) {
      String key = spec;
      cons.register(ConsequenceManager.accomplishments);
    } else {
      RequestLogger.printLine("Unknown consequence type: " + type);
    }
  }

  public static void parseSkillDesc(int id, String responseText) {
    Consequence cons = ConsequenceManager.skillDescs.get(IntegerPool.get(id));
    if (cons != null) {
      cons.test(responseText);
    }
  }

  public static void parseItemDesc(String id, String responseText) {
    Consequence cons = ConsequenceManager.itemDescs.get(id);
    if (cons != null) {
      cons.test(responseText);
    }
  }

  public static void parseEffectDesc(String id, String responseText) {
    Consequence cons = ConsequenceManager.effectDescs.get(id);
    if (cons != null) {
      cons.test(responseText);
    }
  }

  public static void updateOneDesc() {
    int size = ConsequenceManager.descriptions.size();
    if (size == 0) { // this shouldn't happen...
      return;
    }
    // getCalendarDay is good for up to 96 description items
    int seq = HolidayDatabase.getCalendarDay(new Date());
    GenericRequest req = new GenericRequest(ConsequenceManager.descriptions.get(seq % size));
    RequestThread.postRequest(req);
  }

  public static void parseAccomplishments(String responseText) {
    for (Consequence cons : ConsequenceManager.accomplishments) {
      if (cons != null) {
        cons.test(responseText);
      }
    }
  }

  public static String disambiguateMonster(String monster, String responseText) {
    Consequence cons = ConsequenceManager.monsters.get(monster);
    if (cons != null) {
      String rv = cons.test(responseText, false);
      if (rv != null) {
        return rv;
      }
    }
    return monster;
  }

  private static class Consequence {
    private final String[] data;
    private final Pattern patt;
    private Consequence next;

    public Consequence(String[] data, Pattern patt) {
      this.data = data;
      this.patt = patt;
    }

    public String getType() {
      return this.data[0];
    }

    public String getSpec() {
      return this.data[1];
    }

    public Matcher matcher(CharSequence text) {
      return this.patt.matcher(text);
    }

    public void register(Map map, Object key) {
      this.next = (Consequence) map.get(key);
      map.put(key, this);
    }

    public void register(List<Consequence> list) {
      list.add(this);
    }

    @Override
    public String toString() {
      return "consequence " + this.getType() + "/" + this.getSpec();
    }

    public void test(CharSequence text) {
      this.test(text, true);
    }

    public String test(CharSequence text, boolean printText) {
      String rv = null;
      if (this.next != null) {
        rv = this.next.test(text, printText);
        if (rv != null && !printText) {
          return rv;
        }
      } else if (Preferences.getBoolean("debugConsequences")) {
        RequestLogger.printLine("Testing " + this);
      }

      Matcher m = this.matcher(text);
      if (m.find()) {
        for (int i = 3; i < this.data.length; ++i) {
          String res = this.fireAction(data[i], m);
          if (res != null) {
            rv = res;
            if (printText) {
              RequestLogger.printLine(res);
            }
          }
        }
      }
      return rv;
    }

    private String fireAction(String action, Matcher match) {
      StringBuffer buff = new StringBuffer();
      Matcher m = ConsequenceManager.GROUP_PATTERN.matcher(action);
      if (m.find()) {
        do {
          int group = Integer.parseInt(m.group(1));
          if (group < 0 || group > match.groupCount()) {
            RequestLogger.printLine("Bad group number in " + this);
          } else {
            String grout = match.group(group);
            if (grout == null) {
              grout = "";
            }
            m.appendReplacement(buff, grout);
          }
        } while (m.find());
        m.appendTail(buff);
        action = buff.toString();
      }

      m = ConsequenceManager.EXPR_PATTERN.matcher(action);
      if (m.find()) {
        buff.setLength(0);
        do {
          double val = ModifierExpression.getInstance(m.group(1), "consequence").eval();
          if (val == (int) val) { // Avoid decimal point for integer values
            m.appendReplacement(buff, String.valueOf((int) val));
          } else {
            m.appendReplacement(buff, String.valueOf(val));
          }
        } while (m.find());
        m.appendTail(buff);
        action = buff.toString();
      }

      if (Preferences.getBoolean("debugConsequences")) {
        RequestLogger.printLine("Firing action: " + action);
      }

      int pos;
      if (action.startsWith("\"")) {
        pos = action.length() - (action.endsWith("\"") ? 1 : 0);
        return action.substring(1, pos);
      }

      pos = action.indexOf('=');
      if (pos != -1) {
        String setting = action.substring(0, pos).trim();
        String value = action.substring(pos + 1).trim();
        if (value.equals("ascensions")) value = String.valueOf(KoLCharacter.getAscensions());
        Preferences.setString(setting, value);
        return null;
      }

      // Assume anything that didn't match a specific action type is text.
      return action;
    }
  }
}
