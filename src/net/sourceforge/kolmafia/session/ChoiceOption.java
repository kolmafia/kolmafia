package net.sourceforge.kolmafia.session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.DateTimeManager;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.ClanFortuneDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;

public class ChoiceOption {

  private static final String[] NO_ITEM_NAMES = new String[0];

  private final String name;
  private final int option;
  private final AdventureResult[] items;

  public ChoiceOption(final String name) {
    this(name, 0, NO_ITEM_NAMES);
  }

  public ChoiceOption(final String name, final String... itemNames) {
    this(name, 0, itemNames);
  }

  public ChoiceOption(final String name, final int option) {
    this(name, option, NO_ITEM_NAMES);
  }

  public ChoiceOption(final String name, final int option, final String... itemNames) {
    this.name = name;
    this.option = option;

    int count = itemNames.length;
    this.items = new AdventureResult[count];

    for (int index = 0; index < count; ++index) {
      this.items[index] = ItemPool.get(ItemDatabase.getItemId(itemNames[index]));
    }
  }

  public String getName() {
    return this.name;
  }

  public int getOption() {
    return this.option;
  }

  public int getDecision(final int def) {
    return this.option == 0 ? def : this.option;
  }

  public AdventureResult[] getItems() {
    return this.items;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
