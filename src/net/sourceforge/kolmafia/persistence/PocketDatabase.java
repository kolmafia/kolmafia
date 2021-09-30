package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PocketDatabase {
  public enum PocketType {
    STATS("stats"),
    MONSTER("monster"),
    COMMON("common", "a common effect"),
    EFFECT("effect", "a rare effect"),
    RESTORE("restore", "a full HP/MP restoration and an effect"),
    BUFF("buff", "an accordion buff"),
    ELEMENT("element", "an elemental resistance effect"),
    JOKE("joke"),
    CANDY1("candy1", "a type 1 candy effect"),
    CANDY2("candy2", "a type 2 candy effect"),
    CHIPS1("chips1", "a potato chip effect"),
    GUM1("gum1", "a gum effect"),
    LENS1("lens1", "a contact lens effect"),
    NEEDLE1("needle1", "a needles effect"),
    TEETH1("teeth1", "a teeth effect"),
    CANDY("candy", "2 candy effects"),
    CHIPS("chips", "2 potato chip effects"),
    GUM("gum", "2 gum effects"),
    LENS("lens", "2 contact lens effects"),
    NEEDLE("needle", "2 needles effects"),
    TEETH("teeth", "2 teeth effects"),
    ITEM("item", "an item"),
    ITEM2("item2", "two items"),
    AVATAR("avatar", "an avatar potion"),
    BELL("bell", "a desk bell"),
    BOOZE("booze"),
    CASH("cash", "an item that is usable for meat"),
    CHESS("chess", "a chess piece"),
    CHOCO("choco", "some chocolate"),
    FOOD("food"),
    FRUIT("fruit"),
    OYSTER("oyster", "an oyster egg"),
    POTION("potion", "a potion"),
    YEG("yeg", "an item from Yeg's Motel"),
    SCRAP("scrap", "part of demon name"),
    POEM("poem", "an encrypted half-line of a poem"),
    MEAT("meat", "Meat and a puzzle clue");

    private final String tag;
    private final String name;

    // Pockets self-add themselves to this map
    private final Map<Integer, Pocket> pockets = new HashMap<>();

    PocketType(String tag) {
      this(tag, tag);
    }

    PocketType(String tag, String name) {
      this.tag = tag;
      this.name = name;
    }

    public String getTag() {
      return this.tag;
    }

    public void addPocket(Pocket pocket) {
      this.pockets.put(pocket.getPocket(), pocket);
    }

    public Map<Integer, Pocket> getPockets() {
      return this.pockets;
    }

    public Pocket getPocket(int pocket) {
      return this.pockets.get(IntegerPool.get(pocket));
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static final Map<String, PocketType> tagToPocketType = new HashMap<>();

  static {
    for (PocketType type : PocketType.values()) {
      PocketDatabase.tagToPocketType.put(type.getTag(), type);
    }
  }

  // Pockets self-add themselves to this map
  public static final Map<Integer, Pocket> allPockets = new HashMap<>();

  public static class Pocket {
    protected final Integer pocket;
    protected final PocketType type;

    public Pocket(int pocket, PocketType type) {
      this.pocket = IntegerPool.get(pocket);
      this.type = type;
      // Add to map of all pockets
      PocketDatabase.allPockets.put(this.pocket, this);
      // Add to map of pockets of this type
      type.addPocket(this);
    }

    public Integer getPocket() {
      return this.pocket;
    }

    public PocketType getType() {
      return this.type;
    }

    @Override
    public String toString() {
      return this.type.toString();
    }
  }

  public static class StatsPocket extends Pocket {
    private final int muscle;
    private final int mysticality;
    private final int moxie;

    public StatsPocket(int pocket, int muscle, int mysticality, int moxie) {
      super(pocket, PocketType.STATS);
      this.muscle = muscle;
      this.mysticality = mysticality;
      this.moxie = moxie;
    }

    public int getMuscle() {
      return this.muscle;
    }

    public int getMysticality() {
      return this.mysticality;
    }

    public int getMoxie() {
      return this.moxie;
    }

    public int getCount(String name) {
      return name == null
          ? 0
          : name.equals("muscle")
              ? this.muscle
              : name.equals("mysticality")
                  ? this.mysticality
                  : name.equals("moxie") ? this.moxie : 0;
    }

    @Override
    public String toString() {
      return "stats: " + this.muscle + "/" + this.mysticality + "/" + this.moxie;
    }
  }

  public static class MonsterPocket extends Pocket {
    private final MonsterData monster;

    public MonsterPocket(int pocket, MonsterData monster) {
      super(pocket, PocketType.MONSTER);
      this.monster = monster;
    }

    public MonsterData getMonster() {
      return this.monster;
    }

    @Override
    public String toString() {
      return "a " + this.monster.getName();
    }
  }

  public static class MeatPocket extends Pocket {
    private final int meat;
    private final String text;

    public MeatPocket(int pocket, int meat, String text) {
      super(pocket, PocketType.MEAT);
      this.meat = meat;
      this.text = text;
    }

    public int getMeat() {
      return this.meat;
    }

    public String getText() {
      return this.text;
    }

    @Override
    public String toString() {
      return meat + " Meat and a clue: " + this.text;
    }
  }

  public static class PoemPocket extends Pocket {
    private final int index;
    private final String text;

    public PoemPocket(int pocket, int index, String text) {
      super(pocket, PocketType.POEM);
      this.index = index;
      this.text = text;
    }

    public int getIndex() {
      return this.index;
    }

    public String getText() {
      return this.text;
    }

    @Override
    public String toString() {
      return "encrypted half-line #" + this.index + " of a poem: " + this.text;
    }
  }

  public static class ScrapPocket extends Pocket {
    private final int scrap;

    public ScrapPocket(int pocket, int scrap) {
      super(pocket, PocketType.SCRAP);
      this.scrap = scrap;
    }

    public int getScrap() {
      return this.scrap;
    }

    @Override
    public String toString() {
      return "part #" + this.scrap + " of a demon name";
    }
  }

  public static class OneResultPocket extends Pocket {
    protected final AdventureResult result1;

    public OneResultPocket(int pocket, PocketType type, AdventureResult result1) {
      super(pocket, type);
      this.result1 = result1;
    }

    public AdventureResult getResult1() {
      return this.result1;
    }

    public int getCount(String name) {
      return this.result1.getName().equals(name) ? this.result1.getCount() : 0;
    }

    protected String normalizeResultName(AdventureResult result) {
      String name = result.getName();
      if (result.isStatusEffect()) {
        int num = result.getEffectId();
        int[] effectIds = EffectDatabase.getEffectIds(name, false);
        if (effectIds != null && effectIds.length > 1) {
          name = "[" + num + "]" + name;
        }
      }
      return name;
    }

    @Override
    public String toString() {
      return this.type.toString()
          + ": "
          + this.normalizeResultName(this.result1)
          + " ("
          + this.result1.getCount()
          + ")";
    }
  }

  public static class TwoResultPocket extends OneResultPocket {
    protected final AdventureResult result2;

    public TwoResultPocket(
        int pocket, PocketType type, AdventureResult result1, AdventureResult result2) {
      super(pocket, type, result1);
      this.result2 = result2;
    }

    public AdventureResult getResult2() {
      return this.result2;
    }

    public int getCount(String name) {
      return this.result1.getName().equals(name)
          ? this.result1.getCount()
          : this.result2.getName().equals(name) ? this.result2.getCount() : 0;
    }

    @Override
    public String toString() {
      return this.type.toString()
          + ": "
          + this.normalizeResultName(this.result1)
          + " ("
          + this.result1.getCount()
          + ") and "
          + this.normalizeResultName(this.result2)
          + " ("
          + this.result2.getCount()
          + ")";
    }
  }

  private static final AdventureResult JOKE_MAD = EffectPool.get(EffectPool.JOKE_MAD, 20);

  public static class JokePocket extends OneResultPocket {
    protected final String joke;

    public JokePocket(int pocket, String joke) {
      super(pocket, PocketType.JOKE, JOKE_MAD);
      this.joke = joke;
    }

    public String getJoke() {
      return this.joke;
    }

    @Override
    public String toString() {
      return "Joke-Mad (20) and a joke: " + this.joke;
    }
  }

  // Here are additional data structures for retrieving pocket data

  public static final Map<String, Set<OneResultPocket>> effectPockets = new HashMap<>();
  public static final Map<String, Set<OneResultPocket>> itemPockets = new HashMap<>();
  public static final Map<String, MonsterPocket> monsterPockets = new HashMap<>();
  public static final Map<String, Set<StatsPocket>> statsPockets = new HashMap<>();

  public static final Set<Integer> allEffectPockets = new HashSet<>();
  public static final Set<Integer> allItemPockets = new HashSet<>();
  public static final Set<Integer> allMonsterPockets = new HashSet<>();
  public static final Set<Integer> allStatsPockets = new HashSet<>();

  public static List<Pocket> meatPockets;
  public static List<Pocket> poemHalfLines;
  public static List<Pocket> scrapSyllables;

  static {
    PocketDatabase.reset();
    PocketDatabase.scrapSyllables =
        PocketDatabase.getPockets(PocketType.SCRAP).values().stream()
            .sorted(Comparator.comparing(p -> ((ScrapPocket) p).getScrap()))
            .collect(Collectors.toList());
    PocketDatabase.poemHalfLines =
        PocketDatabase.getPockets(PocketType.POEM).values().stream()
            .sorted(Comparator.comparing(p -> ((PoemPocket) p).getIndex()))
            .collect(Collectors.toList());
    PocketDatabase.meatPockets =
        PocketDatabase.getPockets(PocketType.MEAT).values().stream()
            .sorted(Comparator.comparing(p -> ((MeatPocket) p).getMeat()))
            .collect(Collectors.toList());
    RequestLogger.printLine("Pockets loaded: " + allPockets.size());
    int categorized =
        allEffectPockets.size()
            + allItemPockets.size()
            + allMonsterPockets.size()
            + allStatsPockets.size()
            + meatPockets.size()
            + poemHalfLines.size()
            + scrapSyllables.size();
    RequestLogger.printLine("Pockets categorized: " + categorized);
  }

  private static void reset() {
    BufferedReader reader =
        FileUtilities.getVersionedReader("cultshorts.txt", KoLConstants.CULTSHORTS_VERSION);
    String[] data;
    boolean error = false;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length >= 2) {
        int pocketId = StringUtilities.parseInt(data[0]);
        if (pocketId < 1 || pocketId > 666) {
          RequestLogger.printLine("Bogus pocket number: " + pocketId);
          error = true;
          continue;
        }

        String tag = data[1];
        PocketType type = PocketDatabase.tagToPocketType.get(tag.toLowerCase());
        if (type == null) {
          RequestLogger.printLine("Pocket " + pocketId + " has bogus pocket type: " + tag);
          error = true;
          continue;
        }

        Pocket pocket = parsePocketData(pocketId, type, data);
        if (pocket == null) {
          error = true;
          continue;
        }

        if (!PocketDatabase.addToDatabase(pocket)) {
          error = true;
        }
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
      error = true;
    }

    if (error) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Error loading pocket database.");
    }
  }

  private static Pocket parsePocketData(int pocketId, PocketType type, String[] data) {
    switch (type) {
      case STATS:
        {
          if (data.length < 5) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " does not have muscle, mysticality, and moxie");
            return null;
          }
          String muscleString = data[2];
          if (!StringUtilities.isNumeric(muscleString)) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " has bad muscle value: " + muscleString);
            return null;
          }
          int muscle = StringUtilities.parseInt(muscleString);
          String mysticalityString = data[3];
          if (!StringUtilities.isNumeric(mysticalityString)) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " has bad mysticality value: " + mysticalityString);
            return null;
          }
          int mysticality = StringUtilities.parseInt(mysticalityString);
          String moxieString = data[4];
          if (!StringUtilities.isNumeric(moxieString)) {
            RequestLogger.printLine("Pocket " + pocketId + " has bad moxie value: " + moxieString);
            return null;
          }
          int moxie = StringUtilities.parseInt(moxieString);
          return new StatsPocket(pocketId, muscle, mysticality, moxie);
        }
      case MONSTER:
        {
          if (data.length < 3) {
            RequestLogger.printLine("Pocket " + pocketId + " does not have a monster name");
            return null;
          }
          String name = data[2];
          MonsterData monster = MonsterDatabase.findMonster(name);
          if (monster == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown monster name: " + name);
            return null;
          }
          return new MonsterPocket(pocketId, monster);
        }
      case COMMON:
      case EFFECT:
      case RESTORE:
      case BUFF:
      case ELEMENT:
      case CANDY1:
      case CANDY2:
      case CHIPS1:
      case GUM1:
      case LENS1:
      case NEEDLE1:
      case TEETH1:
        {
          if (data.length < 3) {
            RequestLogger.printLine("Pocket " + pocketId + " does not have an effect");
            return null;
          }
          String effect1String = data[2];
          AdventureResult effect1 = PocketDatabase.parseEffect(effect1String);
          if (effect1 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown effect: " + effect1String);
            return null;
          }
          if (effect1.getCount() == 0) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " has effect duration 0: " + effect1String);
          }
          return new OneResultPocket(pocketId, type, effect1);
        }
      case CANDY:
      case CHIPS:
      case GUM:
      case LENS:
      case NEEDLE:
      case TEETH:
        {
          if (data.length < 4) {
            RequestLogger.printLine("Pocket " + pocketId + " does not have two effects");
            return null;
          }
          String effect1String = data[2];
          AdventureResult effect1 = PocketDatabase.parseEffect(effect1String);
          if (effect1 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown effect: " + effect1String);
            return null;
          }
          if (effect1.getCount() == 0) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " has effect duration 0: " + effect1String);
          }
          String effect2String = data[3];
          AdventureResult effect2 = PocketDatabase.parseEffect(effect2String);
          if (effect2 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown effect: " + effect2String);
            return null;
          }
          if (effect2.getCount() == 0) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " has effect duration 0: " + effect2String);
          }
          return new TwoResultPocket(pocketId, type, effect1, effect2);
        }
      case ITEM:
      case AVATAR:
      case BELL:
      case BOOZE:
      case CASH:
      case CHESS:
      case CHOCO:
      case FOOD:
      case FRUIT:
      case OYSTER:
      case POTION:
      case YEG:
        {
          if (data.length < 3) {
            RequestLogger.printLine("Pocket " + pocketId + " does not have an item");
            return null;
          }
          String item1String = data[2];
          AdventureResult item1 = PocketDatabase.parseItem(item1String);
          if (item1 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown item: " + item1String);
            return null;
          }
          if (item1.getCount() == 0) {
            RequestLogger.printLine("Pocket " + pocketId + " has item count 0: " + item1String);
          }
          return new OneResultPocket(pocketId, type, item1);
        }
      case ITEM2:
        {
          if (data.length < 4) {
            RequestLogger.printLine("Pocket " + pocketId + " does not have two items");
            return null;
          }
          String item1String = data[2];
          AdventureResult item1 = PocketDatabase.parseItem(item1String);
          if (item1 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown item: " + item1String);
            return null;
          }
          if (item1.getCount() == 0) {
            RequestLogger.printLine("Pocket " + pocketId + " has item count 0: " + item1String);
          }
          String item2String = data[3];
          AdventureResult item2 = PocketDatabase.parseItem(item2String);
          if (item2 == null) {
            RequestLogger.printLine("Pocket " + pocketId + " has unknown item: " + item2String);
            return null;
          }
          if (item2.getCount() == 0) {
            RequestLogger.printLine("Pocket " + pocketId + " has item count 0: " + item2String);
          }
          return new TwoResultPocket(pocketId, type, item1, item2);
        }
      case JOKE:
        {
          if (data.length < 3) {
            RequestLogger.printLine("Pocket " + pocketId + " must have a text string");
            return null;
          }
          String text = data[2];
          return new JokePocket(pocketId, text);
        }
      case MEAT:
        {
          if (data.length < 4) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " must have an integer and a text string");
            return null;
          }
          String meatString = data[2];
          if (!StringUtilities.isNumeric(meatString)) {
            RequestLogger.printLine("Pocket " + pocketId + " has bad meat value: " + meatString);
            return null;
          }
          int meat = StringUtilities.parseInt(meatString);
          String text = data[3];
          return new MeatPocket(pocketId, meat, text);
        }
      case POEM:
        {
          if (data.length < 4) {
            RequestLogger.printLine(
                "Pocket " + pocketId + " must have an integer and a text string");
            return null;
          }
          String indexString = data[2];
          if (!StringUtilities.isNumeric(indexString)) {
            RequestLogger.printLine("Pocket " + pocketId + " has bad index value: " + indexString);
            return null;
          }
          int index = StringUtilities.parseInt(indexString);
          String text = data[3];
          return new PoemPocket(pocketId, index, text);
        }
      case SCRAP:
        {
          if (data.length < 3) {
            RequestLogger.printLine("Pocket " + pocketId + " must have a scrap number");
            return null;
          }
          String scrapString = data[2];
          if (!StringUtilities.isNumeric(scrapString)) {
            RequestLogger.printLine("Pocket " + pocketId + " has bad scrap number: " + scrapString);
            return null;
          }
          int scrap = StringUtilities.parseInt(scrapString);
          return new ScrapPocket(pocketId, scrap);
        }
    }

    return new Pocket(pocketId, type);
  }

  private static AdventureResult parseEffect(String effectString) {
    String name;
    int duration;
    int lparen = effectString.lastIndexOf("(");
    int rparen = effectString.lastIndexOf(")");
    if (lparen < 0 || rparen < 0) {
      name = effectString;
      duration = 0;
    } else {
      String durationString = effectString.substring(lparen + 1, rparen);
      name = effectString.substring(0, lparen).trim();
      duration =
          StringUtilities.isNumeric(durationString) ? StringUtilities.parseInt(durationString) : 0;
    }
    int effectId = EffectDatabase.getEffectId(name, true);
    if (effectId < 0) {
      return null;
    }
    return EffectPool.get(effectId, duration);
  }

  private static AdventureResult parseItem(String itemString) {
    String name;
    int count;
    int lparen = itemString.lastIndexOf("(");
    int rparen = itemString.lastIndexOf(")");
    if (lparen < 0 || rparen < 0) {
      name = itemString;
      count = 1;
    } else {
      String countString = itemString.substring(lparen + 1, rparen);
      name = itemString.substring(0, lparen).trim();
      count = StringUtilities.isNumeric(countString) ? StringUtilities.parseInt(countString) : 0;
    }
    int itemId = ItemDatabase.getItemId(name, 1, false);
    if (itemId < 0) {
      return null;
    }
    return ItemPool.get(itemId, count);
  }

  private static boolean addToDatabase(Pocket pocket) {
    // Add to additional List/Set/Map as needed
    switch (pocket.getType()) {
      case COMMON:
      case EFFECT:
      case BUFF:
      case ELEMENT:
      case JOKE:
      case RESTORE:
      case CANDY1:
      case CANDY2:
      case CHIPS1:
      case GUM1:
      case LENS1:
      case NEEDLE1:
      case TEETH1:
        {
          OneResultPocket orp = (OneResultPocket) pocket;
          PocketDatabase.addResultPocket(
              PocketDatabase.effectPockets, orp.getResult1().getName(), orp);
          PocketDatabase.allEffectPockets.add(orp.getPocket());
          break;
        }
      case CANDY:
      case CHIPS:
      case GUM:
      case LENS:
      case NEEDLE:
      case TEETH:
        {
          TwoResultPocket trp = (TwoResultPocket) pocket;
          PocketDatabase.addResultPocket(
              PocketDatabase.effectPockets, trp.getResult1().getName(), trp);
          PocketDatabase.addResultPocket(
              PocketDatabase.effectPockets, trp.getResult2().getName(), trp);
          PocketDatabase.allEffectPockets.add(trp.getPocket());
          break;
        }
      case ITEM:
      case AVATAR:
      case BELL:
      case BOOZE:
      case CASH:
      case CHESS:
      case CHOCO:
      case FOOD:
      case FRUIT:
      case OYSTER:
      case POTION:
      case YEG:
        {
          OneResultPocket orp = (OneResultPocket) pocket;
          PocketDatabase.addResultPocket(
              PocketDatabase.itemPockets, orp.getResult1().getName(), orp);
          PocketDatabase.allItemPockets.add(orp.getPocket());
          break;
        }
      case ITEM2:
        {
          TwoResultPocket trp = (TwoResultPocket) pocket;
          PocketDatabase.addResultPocket(
              PocketDatabase.itemPockets, trp.getResult1().getName(), trp);
          PocketDatabase.addResultPocket(
              PocketDatabase.itemPockets, trp.getResult2().getName(), trp);
          PocketDatabase.allItemPockets.add(trp.getPocket());
          break;
        }
      case MONSTER:
        {
          MonsterPocket mp = (MonsterPocket) pocket;
          PocketDatabase.addMonsterPocket(mp);
          PocketDatabase.allMonsterPockets.add(mp.getPocket());
          break;
        }
      case STATS:
        {
          StatsPocket sp = (StatsPocket) pocket;
          PocketDatabase.addStatsPocket("muscle", sp.getMuscle(), sp);
          PocketDatabase.addStatsPocket("mysticality", sp.getMysticality(), sp);
          PocketDatabase.addStatsPocket("moxie", sp.getMoxie(), sp);
          PocketDatabase.allStatsPockets.add(sp.getPocket());
          break;
        }
    }
    return true;
  }

  private static void addResultPocket(
      Map<String, Set<OneResultPocket>> pocketMap, String name, OneResultPocket orp) {
    Set<OneResultPocket> pockets =
        pocketMap.containsKey(name) ? pocketMap.get(name) : new HashSet<>();
    if (pockets.size() == 0) {
      pocketMap.put(name, pockets);
    }
    pockets.add(orp);
  }

  private static void addStatsPocket(String name, int stat, StatsPocket sp) {
    if (stat == 0) {
      return;
    }

    Map<String, Set<StatsPocket>> pocketMap = PocketDatabase.statsPockets;
    Set<StatsPocket> pockets = pocketMap.containsKey(name) ? pocketMap.get(name) : new HashSet<>();
    if (pockets.size() == 0) {
      pocketMap.put(name, pockets);
    }
    pockets.add(sp);
  }

  private static void addMonsterPocket(MonsterPocket mp) {
    String monsterName = mp.getMonster().getName();
    PocketDatabase.monsterPockets.put(monsterName.toLowerCase(), mp);
  }

  // Filtering lists and maps of pockets

  public static Map<Integer, Pocket> removePickedPockets(Map<Integer, Pocket> pockets) {
    Set<Integer> picked = CargoCultistShortsRequest.pickedPockets;
    return pockets.entrySet().stream()
        .filter(e -> !picked.contains(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  // Sorting lists and maps of pockets

  public static List<Pocket> sortPockets(PocketType type) {
    return sortPockets(type, PocketDatabase.getPockets(type));
  }

  public static List<Pocket> sortPockets(PocketType type, Map<Integer, Pocket> pockets) {
    // PocketType is derivable from the first pocket in the
    // collection, but since caller always knows it, pass in
    switch (type) {
      case SCRAP:
        // Sort on scrap index.
        return pockets.values().stream()
            .sorted(Comparator.comparing(p -> ((ScrapPocket) p).getScrap()))
            .collect(Collectors.toList());
      case MEAT:
        // Sort on Meat
        return pockets.values().stream()
            .sorted(Comparator.comparing(p -> ((MeatPocket) p).getMeat()))
            .collect(Collectors.toList());
      case POEM:
        // Sort on line index
        return pockets.values().stream()
            .sorted(Comparator.comparing(p -> ((PoemPocket) p).getIndex()))
            .collect(Collectors.toList());
      case MONSTER:
        // Sort on monster name
        return pockets.values().stream()
            .sorted(
                Comparator.comparing(p -> ((MonsterPocket) p).getMonster().getName().toLowerCase()))
            .collect(Collectors.toList());
      case ITEM:
      case AVATAR:
      case BELL:
      case BOOZE:
      case CASH:
      case CHESS:
      case CHOCO:
      case FOOD:
      case FRUIT:
      case OYSTER:
      case POTION:
      case YEG:
      case EFFECT:
      case RESTORE:
      case BUFF:
      case CANDY1:
      case CANDY2:
      case CHIPS1:
      case GUM1:
      case LENS1:
      case NEEDLE1:
      case TEETH1:
        // Single results with a single source sort on effect name
        return pockets.values().stream()
            .sorted(Comparator.comparing(p -> ((OneResultPocket) p).getResult1().getName()))
            .collect(Collectors.toList());
      case COMMON:
      case ELEMENT:
        // Single effects with multiple sources sort first on effect name then on pocket number
        return pockets.values().stream()
            .sorted(
                Comparator.comparing(p -> ((OneResultPocket) p).getResult1().getName())
                    .thenComparing(p -> ((Pocket) p).getPocket()))
            .collect(Collectors.toList());
      case ITEM2:
      case CANDY:
      case CHIPS:
      case GUM:
      case LENS:
      case NEEDLE:
      case TEETH:
        // Two results sort first on result 1 then on result 2
        return pockets.values().stream()
            .sorted(
                Comparator.comparing(p -> ((TwoResultPocket) p).getResult1().getName())
                    .thenComparing(p -> ((TwoResultPocket) p).getResult2().getName()))
            .collect(Collectors.toList());
      case STATS:
        // I can't think of a rational full ordering for stats
      case JOKE:
      default:
        // Pocket number is good enough
        return pockets.values().stream()
            .sorted(Comparator.comparing(Pocket::getPocket))
            .collect(Collectors.toList());
    }
  }

  public static List<Pocket> sortStats(String stat, Set<StatsPocket> pockets) {
    return pockets.stream()
        .filter(p -> p.getCount(stat) > 0)
        .sorted(
            Comparator.comparing(p -> ((StatsPocket) p).getCount(stat))
                .reversed()
                .thenComparing(p -> ((Pocket) p).getPocket()))
        .collect(Collectors.toList());
  }

  public static List<Pocket> sortResults(String name, Set<OneResultPocket> pockets) {
    return pockets.stream()
        .sorted(
            Comparator.comparing(p -> ((OneResultPocket) p).getCount(name))
                .reversed()
                .thenComparing(p -> ((Pocket) p).getPocket()))
        .collect(Collectors.toList());
  }

  public static Pocket firstUnpickedPocket(List<Pocket> pockets) {
    if (pockets != null) {
      Set<Integer> picked = CargoCultistShortsRequest.pickedPockets;
      for (Pocket pocket : pockets) {
        if (!picked.contains(IntegerPool.get(pocket.getPocket()))) {
          return pocket;
        }
      }
    }

    return null;
  }

  // External files should use this, rather than fetching the map
  // directly from the enum, to force pockets to be loaded.
  public static Map<Integer, Pocket> getPockets(PocketType type) {
    return type == null ? null : type.getPockets();
  }

  public static PocketType getPocketType(String tag) {
    return PocketDatabase.tagToPocketType.get(tag.toLowerCase());
  }

  public static Pocket pocketByNumber(int pocket) {
    return PocketDatabase.allPockets.get(IntegerPool.get(pocket));
  }

  public static MonsterData monsterByNumber(int pocket) {
    Pocket mp = PocketType.MONSTER.getPocket(pocket);
    return mp == null ? null : ((MonsterPocket) mp).getMonster();
  }
}
