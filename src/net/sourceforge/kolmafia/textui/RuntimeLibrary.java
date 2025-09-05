package net.sourceforge.kolmafia.textui;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.Expression;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterExpression;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.chat.ChatMessage;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.chat.WhoMessage;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.maximizer.PriceLevel;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.ModifierValueType;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.moods.Mood;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.CandyDatabase.Candy;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FactDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.MonsterDrop;
import net.sourceforge.kolmafia.persistence.MonsterDrop.DropFlag;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.JokePocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MeatPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.MonsterPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.OneResultPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PocketType;
import net.sourceforge.kolmafia.persistence.PocketDatabase.PoemPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.ScrapPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.StatsPocket;
import net.sourceforge.kolmafia.persistence.PocketDatabase.TwoResultPocket;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.WardrobeOMaticDatabase;
import net.sourceforge.kolmafia.persistence.WardrobeOMaticDatabase.FuturisticClothing;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.*;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;
import net.sourceforge.kolmafia.request.ClanStashRequest.ClanStashRequestType;
import net.sourceforge.kolmafia.request.ClosetRequest.ClosetRequestType;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.EveryCard;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.scripts.git.GitManager;
import net.sourceforge.kolmafia.scripts.svn.SVNManager;
import net.sourceforge.kolmafia.session.AutumnatonManager;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.FamiliarManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.GuildUnlockManager;
import net.sourceforge.kolmafia.session.HeistManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LocketManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.session.MonsterManuelManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.NumberologyManager;
import net.sourceforge.kolmafia.session.PingManager;
import net.sourceforge.kolmafia.session.PingManager.PingTest;
import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TowerDoorManager;
import net.sourceforge.kolmafia.session.TrackManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.UnusualConstructManager;
import net.sourceforge.kolmafia.session.VotingBoothManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.swingui.widget.InterruptableDialog;
import net.sourceforge.kolmafia.textui.AshRuntime.CallFrame;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.command.ColdMedicineCabinetCommand;
import net.sourceforge.kolmafia.textui.command.EudoraCommand;
import net.sourceforge.kolmafia.textui.command.MonkeyPawCommand;
import net.sourceforge.kolmafia.textui.command.SetPreferencesCommand;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.AggregateValue;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

@SuppressWarnings({"incomplete-switch", "unused"})
public abstract class RuntimeLibrary {
  private static final RecordType itemDropRec =
      new RecordType(
          "{item drop; float rate; string type;}",
          new String[] {"drop", "rate", "type"},
          new Type[] {DataTypes.ITEM_TYPE, DataTypes.FLOAT_TYPE, DataTypes.STRING_TYPE});

  private static final RecordType maximizerResult =
      new RecordType(
          "{string display; string command; float score; effect effect; item item; skill skill;}",
          new String[] {"display", "command", "score", "effect", "item", "skill"},
          new Type[] {
            DataTypes.STRING_TYPE,
            DataTypes.STRING_TYPE,
            DataTypes.FLOAT_TYPE,
            DataTypes.EFFECT_TYPE,
            DataTypes.ITEM_TYPE,
            DataTypes.SKILL_TYPE
          });

  private static final RecordType svnInfoRec =
      new RecordType(
          "{string url; int revision; string last_changed_author; int last_changed_rev; string last_changed_date;}",
          new String[] {
            "url", "revision", "last_changed_author", "last_changed_rev", "last_changed_date"
          },
          new Type[] {
            DataTypes.STRING_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.STRING_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.STRING_TYPE
          });

  private static final RecordType gitInfoRec =
      new RecordType(
          "{string url; string branch; string commit; string last_changed_author; string last_changed_date;}",
          new String[] {"url", "branch", "commit", "last_changed_author", "last_changed_date"},
          new Type[] {
            DataTypes.STRING_TYPE,
            DataTypes.STRING_TYPE,
            DataTypes.STRING_TYPE,
            DataTypes.STRING_TYPE,
            DataTypes.STRING_TYPE
          });

  private static final RecordType pingTestRec =
      new RecordType(
          "{string page; int count; int low; int high; int total; int bytes; int average; int bps;}",
          new String[] {"page", "count", "low", "high", "total", "bytes", "average", "bps"},
          new Type[] {
            DataTypes.STRING_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE,
            DataTypes.INT_TYPE
          });

  private static final RecordType stackTraceRec =
      new RecordType(
          "{string file; string name; int line;}",
          new String[] {"file", "name", "line"},
          new Type[] {DataTypes.STRING_TYPE, DataTypes.STRING_TYPE, DataTypes.INT_TYPE});

  private static final AggregateType NumberologyType =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.INT_TYPE);
  private static final AggregateType HeistType =
      new AggregateType(DataTypes.INT_TO_ITEM_TYPE, DataTypes.MONSTER_TYPE);

  private static final AggregateType ItemSetType =
      new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.INT_TYPE);

  private static final AggregateType PocketListType =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.INT_TYPE);
  private static final AggregateType PocketSetType =
      new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.INT_TYPE);
  private static final AggregateType PocketEffectsType =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.EFFECT_TYPE);
  private static final AggregateType PocketItemsType =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.ITEM_TYPE);
  private static final AggregateType PocketStatsType =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.STAT_TYPE);
  private static final AggregateType IndexedTextType =
      new AggregateType(DataTypes.STRING_TYPE, DataTypes.INT_TYPE);

  private static final AggregateType SelectMapType =
      new AggregateType(DataTypes.STRING_TO_STRING_TYPE, DataTypes.STRING_TYPE);

  public static final FunctionList functions = new FunctionList();

  // *** Why can't the following go in KoLConstants?
  public static final Set<String> frameNames = new HashSet<>();

  static {
    for (String[] frame : KoLConstants.FRAME_NAMES) {
      RuntimeLibrary.frameNames.add(frame[1]);
    }
    RuntimeLibrary.frameNames.add("CakeArenaFrame");
    RuntimeLibrary.frameNames.add("ChatFrame");
    RuntimeLibrary.frameNames.add("CouncilFrame");
    RuntimeLibrary.frameNames.add("DescriptionFrame");
    RuntimeLibrary.frameNames.add("LoginFrame");
    RuntimeLibrary.frameNames.add("MonsterDescriptionFrame");
    RuntimeLibrary.frameNames.add("ProfileFrame");
    RuntimeLibrary.frameNames.add("RequestSynchFrame");
    RuntimeLibrary.frameNames.add("ScriptManageFrame");
    RuntimeLibrary.frameNames.add("SendMessageFrame");
    RuntimeLibrary.frameNames.add("TabbedChatFrame");
    RuntimeLibrary.frameNames.add("TrophyFrame");
  }

  public static FunctionList getFunctions() {
    return RuntimeLibrary.functions;
  }

  private static VariableReference namedParam(String name, Type type) {
    return new VariableReference(null, new Variable(name, type, null));
  }

  static {
    List<VariableReference> params;

    // Basic utility functions which print information
    // or allow for easy testing.

    Type stackTraceRecArray = new AggregateType(stackTraceRec, 0);

    params = List.of();
    functions.add(new LibraryFunction("get_stack_trace", stackTraceRecArray, params));

    params = List.of();
    functions.add(new LibraryFunction("get_version", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_revision", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_path", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_path_full", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_path_variables", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("batch_open", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("batch_close", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("enable", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("disable", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("message", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("user_confirm", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("message", DataTypes.STRING_TYPE),
            namedParam("timeOut", DataTypes.INT_TYPE),
            namedParam("defaultBoolean", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("user_confirm", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("message", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("user_prompt", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("message", DataTypes.STRING_TYPE),
            namedParam("options", DataTypes.AGGREGATE_TYPE));
    functions.add(new LibraryFunction("user_prompt", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("message", DataTypes.STRING_TYPE),
            namedParam("timeOut", DataTypes.INT_TYPE),
            namedParam("defaultString", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("user_prompt", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("message", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("user_notify", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("message", DataTypes.STRING_TYPE),
            namedParam("onlyShowWhenHidden", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("user_notify", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("logprint", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("debugprint", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("traceprint", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("print", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("print", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("color", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("print", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("logToSession", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("print_html", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("print_html", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("arg", DataTypes.ANY_TYPE));
    functions.add(new LibraryFunction("dump", DataTypes.VOID_TYPE, params));

    params =
        List.of(namedParam("arg", DataTypes.ANY_TYPE), namedParam("color", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("dump", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("abort", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("abort", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("is_adventuring", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("has_queued_commands", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("cli_execute", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("cli_execute_output", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("load_html", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("write", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("writeln", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("key", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("form_field", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "form_fields",
            new AggregateType(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE),
            params));

    params = List.of();
    functions.add(new LibraryFunction("visit_url", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("visit_url", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("usePostMethod", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("visit_url", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("usePostMethod", DataTypes.BOOLEAN_TYPE),
            namedParam("encoded", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("visit_url", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("usePostMethod", DataTypes.BOOLEAN_TYPE),
            namedParam("encoded", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("make_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("delay", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("wait", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("delay", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("waitq", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("is_dark_mode", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("is_headless", DataTypes.BOOLEAN_TYPE, params));

    // Type conversion functions which allow conversion
    // of one data format to another.

    params = List.of(namedParam("val", DataTypes.ANY_TYPE));
    functions.add(new LibraryFunction("to_json", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("val", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_string", DataTypes.STRING_TYPE, params));
    params =
        List.of(namedParam("val", DataTypes.INT_TYPE), namedParam("fmt", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_string", DataTypes.STRING_TYPE, params));
    params =
        List.of(namedParam("val", DataTypes.FLOAT_TYPE), namedParam("fmt", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_string", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_boolean", DataTypes.BOOLEAN_TYPE, params));
    params = List.of(namedParam("value", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("to_boolean", DataTypes.BOOLEAN_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_boolean", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.CLASS_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.THRALL_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.SERVANT_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.VYKEA_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.PATH_TYPE));
    functions.add(new LibraryFunction("to_int", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_float", DataTypes.FLOAT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("to_float", DataTypes.FLOAT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_float", DataTypes.FLOAT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("to_float", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_buffer", DataTypes.BUFFER_TYPE, params));
    params = List.of(namedParam("value", DataTypes.BUFFER_TYPE));
    functions.add(new LibraryFunction("to_buffer", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_item", DataTypes.ITEM_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_item", DataTypes.ITEM_TYPE, params));
    params =
        List.of(namedParam("name", DataTypes.STRING_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_item", DataTypes.ITEM_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_location", DataTypes.LOCATION_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_location", DataTypes.LOCATION_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_class", DataTypes.CLASS_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_class", DataTypes.CLASS_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_stat", DataTypes.STAT_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_skill", DataTypes.SKILL_TYPE, params));
    params =
        List.of(
            namedParam("name", DataTypes.STRICT_STRING_TYPE),
            namedParam("type", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_skill", DataTypes.SKILL_TYPE, params));
    params = List.of(namedParam("id", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_skill", DataTypes.SKILL_TYPE, params));
    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("to_skill", DataTypes.SKILL_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_effect", DataTypes.EFFECT_TYPE, params));
    params = List.of(namedParam("id", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_effect", DataTypes.EFFECT_TYPE, params));
    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("to_effect", DataTypes.EFFECT_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_familiar", DataTypes.FAMILIAR_TYPE, params));
    params = List.of(namedParam("id", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_familiar", DataTypes.FAMILIAR_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_modifier", DataTypes.MODIFIER_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_monster", DataTypes.MONSTER_TYPE, params));
    params = List.of(namedParam("id", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_monster", DataTypes.MONSTER_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("image_to_monster", DataTypes.MONSTER_TYPE, params));

    params = List.of(namedParam("item", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_slot", DataTypes.SLOT_TYPE, params));
    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("to_slot", DataTypes.SLOT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_element", DataTypes.ELEMENT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_coinmaster", DataTypes.COINMASTER_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_phylum", DataTypes.PHYLUM_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_bounty", DataTypes.BOUNTY_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_thrall", DataTypes.THRALL_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_thrall", DataTypes.THRALL_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_servant", DataTypes.SERVANT_TYPE, params));
    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_servant", DataTypes.SERVANT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_vykea", DataTypes.VYKEA_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("to_path", DataTypes.PATH_TYPE, params));

    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("to_path", DataTypes.PATH_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("to_plural", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("to_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_wiki_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("to_wiki_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("to_wiki_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("to_wiki_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("to_wiki_url", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("desc_to_effect", DataTypes.EFFECT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("desc_to_item", DataTypes.ITEM_TYPE, params));

    // Experimental
    params = List.of(namedParam("value", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "path_name_to_id",
            DataTypes.INT_TYPE,
            params,
            "Changing 'path_name_to_id(xxx)' to 'to_path(xxx).id' will remove this warning"));

    params = List.of(namedParam("value", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "path_id_to_name",
            DataTypes.STRING_TYPE,
            params,
            "Changing 'path_id_to_name(xxx)' to 'my_path(xxx).name' will remove this warning"));

    // Functions related to daily information which get
    // updated usually once per day.

    params = List.of();
    functions.add(new LibraryFunction("holiday", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("today_to_string", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("time_to_string", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("dateFormatValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("now_to_string", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("now_to_int", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("inFormat", DataTypes.STRING_TYPE),
            namedParam("dateTimeString", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("date_to_timestamp", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("timestamp", DataTypes.INT_TYPE),
            namedParam("outFormat", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("timestamp_to_date", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("inFormat", DataTypes.STRING_TYPE),
            namedParam("dateTimeString", DataTypes.STRING_TYPE),
            namedParam("outFormat", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("format_date_time", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("gameday_to_string", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("gameday_to_int", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("gametime_to_int", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("rollover", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("moon_phase", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("moon_light", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("stat_bonus_today", DataTypes.STAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("stat_bonus_tomorrow", DataTypes.STAT_TYPE, params));

    params = List.of(namedParam("dayCount", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("session_logs", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("player", DataTypes.STRING_TYPE),
            namedParam("dayCount", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("session_logs", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("playerName", DataTypes.STRING_TYPE),
            namedParam("baseDate", DataTypes.STRING_TYPE),
            namedParam("count", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("session_logs", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    // Major functions related to adventuring and item management.

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("pre_validate_adventure", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("can_adventure", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("prepare_for_adventure", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("set_location", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("adventure", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("filterFunction", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("adventure", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("adventure", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("filterFunction", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("adventure", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("locationValue", DataTypes.LOCATION_TYPE),
            namedParam("adventuresUsedValue", DataTypes.INT_TYPE),
            namedParam("filterFunction", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("adv1", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("locationValue", DataTypes.LOCATION_TYPE),
            namedParam("adventuresUsedValue", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("adv1", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("locationValue", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("adv1", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("locationValue", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("eight_bit_points", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("locationValue", DataTypes.LOCATION_TYPE),
            namedParam("colorValue", DataTypes.STRING_TYPE),
            namedParam("modValue", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("eight_bit_points", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("index", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("get_ccs_action", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("can_still_steal", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("add_item_condition", DataTypes.VOID_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("add_item_condition", DataTypes.VOID_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("remove_item_condition", DataTypes.VOID_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("remove_item_condition", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("check", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("goal_exists", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_goal", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_goals", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_moods", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(
        new LibraryFunction("mood_list", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("quantity", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("price", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("quantity", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("price", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy_using_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("quantity", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy_using_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("price", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy_using_storage", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("quantity", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy_using_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("price", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("buy_using_storage", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("master", DataTypes.COINMASTER_TYPE));
    functions.add(new LibraryFunction("is_accessible", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("master", DataTypes.COINMASTER_TYPE));
    functions.add(new LibraryFunction("inaccessible_reason", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("master", DataTypes.COINMASTER_TYPE));
    functions.add(new LibraryFunction("visit", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("coinmaster", DataTypes.COINMASTER_TYPE),
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("countValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sell", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("modeValue", DataTypes.STRING_TYPE),
            namedParam("countValue", DataTypes.INT_TYPE),
            namedParam("item1", DataTypes.ITEM_TYPE),
            namedParam("item2", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("craft", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("create", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("create", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("create", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("use", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("use", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("use", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("eat", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("eat", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("eat", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("eatsilent", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("eatsilent", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("eatsilent", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("clear_food_helper", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("drink", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("drink", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("drink", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("overdrink", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("overdrink", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("overdrink", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("drinksilent", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("drinksilent", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("drinksilent", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("clear_booze_helper", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("chew", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("chew", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("chew", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("itemId", DataTypes.ITEM_TYPE), namedParam("target", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("curse", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("itemId", DataTypes.ITEM_TYPE),
            namedParam("target", DataTypes.STRING_TYPE),
            namedParam("message", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("curse", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("itemId", DataTypes.ITEM_TYPE),
            namedParam("target", DataTypes.STRING_TYPE),
            namedParam("message", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("curse", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("last_item_message", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("empty_closet", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("meat", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("put_closet", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_closet", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("put_closet", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_closet", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("limitValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("limitValue", DataTypes.INT_TYPE),
            namedParam("qtyValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("limitValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_shop_using_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("limitValue", DataTypes.INT_TYPE),
            namedParam("qtyValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_shop_using_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("reprice_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("priceValue", DataTypes.INT_TYPE),
            namedParam("limitValue", DataTypes.INT_TYPE),
            namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("reprice_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("put_stash", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_stash", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("put_display", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("put_display", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("meat", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("take_closet", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_closet", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("take_closet", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_closet", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("itemValue", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_shop", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("take_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_storage", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("take_display", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_display", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("take_stash", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("take_stash", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("autosell", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("autosell", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("hermit", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("hermit", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("retrieve_item", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("retrieve_item", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("retrieve_item", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("retrieve_price", DataTypes.INT_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("retrieve_price", DataTypes.INT_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("retrieve_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("exact", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("retrieve_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("exact", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("retrieve_price", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("receive_fax", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("send_fax", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("monsterName", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("faxbot", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("monsterName", DataTypes.MONSTER_TYPE),
            namedParam("botName", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("faxbot", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("can_faxbot", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("monster", DataTypes.MONSTER_TYPE),
            namedParam("faxbot", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("can_faxbot", DataTypes.BOOLEAN_TYPE, params));

    // Major functions which provide item-related
    // information.

    params = List.of();
    functions.add(new LibraryFunction("get_inventory", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_closet", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_storage", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_display", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_free_pulls", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_shop", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_shop_log", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(new LibraryFunction("get_stash", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_campground", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_workshed", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_clan_lounge", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of(namedParam("itemsSource", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("get_items_hash", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_fishing_locations", DataTypes.STRING_TO_LOCATION_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_clan_rumpus", DataTypes.STRING_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_chateau", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_dwelling", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_garden_type", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_zap_wand", DataTypes.ITEM_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("type", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("get_related", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_npc_item", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_coinmaster_item", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_tradeable", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_giftable", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_displayable", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_discardable", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("autosell_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("mall_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("concoction_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("value", DataTypes.VYKEA_TYPE));
    functions.add(new LibraryFunction("concoction_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("maxAge", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("mall_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("items", RuntimeLibrary.ItemSetType));
    functions.add(new LibraryFunction("mall_prices", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("category", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("mall_prices", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("category", DataTypes.STRING_TYPE),
            namedParam("tiers", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("mall_prices", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("itemName", DataTypes.STRING_TYPE),
            namedParam("quantity", DataTypes.INT_TYPE),
            namedParam("price", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("well_stocked", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("npc_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("shop_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buys_item", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("buy_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sells_item", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("sells_skill", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sell_cost", DataTypes.ITEM_TO_INT_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("sell_cost", DataTypes.ITEM_TO_INT_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sell_price", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("master", DataTypes.COINMASTER_TYPE),
            namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("sell_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("historical_price", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("historical_age", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("daily_special", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("refresh_shop", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("refresh_stash", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("available_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("item_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("closet_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("equipped_amount", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("includeAllFamiliars", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("equipped_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("creatable_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("itemId", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("creatable_turns", DataTypes.INT_TYPE, params));

    params =
        List.of(namedParam("itemId", DataTypes.ITEM_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("creatable_turns", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("itemId", DataTypes.ITEM_TYPE),
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("freeCrafting", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("creatable_turns", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("get_ingredients", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("storage_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("display_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("shop_amount", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("shop_limit", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("stash_amount", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("pulls_remaining", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("stills_available", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("have_mushroom_plot", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("craft_type", DataTypes.STRING_TYPE, params));

    // The following functions pertain to providing updated
    // information relating to the player.

    params = List.of();
    functions.add(new LibraryFunction("refresh_status", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("amount", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("restore_hp", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("amount", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("restore_mp", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("multiplicity", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("mood_execute", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_name", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_id", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_hash", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_sign", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_path", DataTypes.PATH_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "my_path_id",
            DataTypes.INT_TYPE,
            params,
            "Changing 'my_path_id()' to 'my_path().id' will remove this warning"));

    params = List.of();
    functions.add(new LibraryFunction("in_muscle_sign", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_mysticality_sign", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_moxie_sign", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_bad_moon", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_class", DataTypes.CLASS_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_level", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_hp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_maxhp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_mp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_maxmp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_pp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_maxpp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_robot_energy", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_robot_scraps", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_primestat", DataTypes.STAT_TYPE, params));

    params = List.of(namedParam("stat", DataTypes.STAT_TYPE));
    functions.add(new LibraryFunction("my_basestat", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("stat", DataTypes.STAT_TYPE));
    functions.add(new LibraryFunction("my_buffedstat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_fury", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_soulsauce", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_discomomentum", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_audience", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_absorbs", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_thunder", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_rain", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_lightning", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_mask", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_paradoxicity", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_maxfury", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_meat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_closet_meat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_storage_meat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_session_meat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_adventures", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_session_adv", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "my_session_items",
            new AggregateType(DataTypes.INT_TYPE, DataTypes.ITEM_TYPE),
            params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("my_session_items", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_session_results", DataTypes.STRING_TO_INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("daycount", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_daycount", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_turncount", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_total_turns_spent", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_fullness", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("fullness_limit", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_inebriety", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("inebriety_limit", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_spleen_use", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("spleen_limit", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_wildfire_water", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("can_eat", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("can_drink", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("turns_played", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("total_turns_played", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_ascensions", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("can_interact", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_hardcore", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_casual", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("pvp_attacks_left", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "current_pvp_stances",
            new AggregateType(DataTypes.INT_TYPE, DataTypes.STRING_TYPE),
            params));

    params = List.of();
    functions.add(new LibraryFunction("get_clan_id", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_clan_name", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("limit_mode", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_florist_plants",
            new AggregateType(new AggregateType(DataTypes.STRING_TYPE, 3), DataTypes.LOCATION_TYPE),
            params));

    params = List.of();
    functions.add(new LibraryFunction("total_free_rests", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_ignore_zone_warnings", DataTypes.BOOLEAN_TYPE, params));

    // Basic skill and effect functions, including those used
    // in custom combat consult scripts.

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("have_skill", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("combat_skill_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_ram", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("mp_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("adv_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("soulsauce_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("thunder_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("rain_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("lightning_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("fuel_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("hp_cost", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("turns_per_cast", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("have_effect", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "my_effects", new AggregateType(DataTypes.INT_TYPE, DataTypes.EFFECT_TYPE), params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("is_shruggable", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("is_removable", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("skill", DataTypes.SKILL_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("use_skill", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("use_skill", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("skill", DataTypes.SKILL_TYPE),
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("target", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("use_skill", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("skill", DataTypes.SKILL_TYPE),
            namedParam("target", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("use_skill", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("last_skill_message", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_auto_attack", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("attackValue", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("set_auto_attack", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("attackValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("set_auto_attack", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("attack", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("twiddle", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("steal", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("runaway", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("skill", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("use_skill", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("throw_item", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(namedParam("item1", DataTypes.ITEM_TYPE), namedParam("item2", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("throw_items", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("decision", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("run_choice", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("decision", DataTypes.INT_TYPE), namedParam("extra", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("run_choice", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("decision", DataTypes.INT_TYPE),
            namedParam("extra", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("run_choice", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("decision", DataTypes.INT_TYPE),
            namedParam("custom", DataTypes.BOOLEAN_TYPE),
            namedParam("more", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("run_choice", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("last_choice", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("last_decision", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("available_choice_options", DataTypes.INT_TO_STRING_TYPE, params));

    params = List.of(namedParam("spoilers", DataTypes.BOOLEAN_TYPE));
    functions.add(
        new LibraryFunction("available_choice_options", DataTypes.INT_TO_STRING_TYPE, params));

    params = List.of(namedParam("decision", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "available_choice_select_inputs", RuntimeLibrary.SelectMapType, params));

    params = List.of(namedParam("decision", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "available_choice_text_inputs", DataTypes.STRING_TO_STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("in_multi_fight", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("choice_follows_fight", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("fight_follows_choice", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("handling_choice", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("run_combat", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("filterFunction", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("run_combat", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("run_turn", DataTypes.BUFFER_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("stun_skill", DataTypes.SKILL_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("reverse_numberology", NumberologyType, params));

    params =
        List.of(
            namedParam("advDelta", DataTypes.INT_TYPE),
            namedParam("spleenDelta", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("reverse_numberology", NumberologyType, params));

    params = List.of(namedParam("num", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("numberology_prize", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRICT_STRING_TYPE));
    functions.add(new LibraryFunction("every_card_name", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("heist_targets", HeistType, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("heist", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("num", DataTypes.INT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("heist", DataTypes.BOOLEAN_TYPE, params));

    // Equipment functions.

    params = List.of(namedParam("equipment", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("can_equip", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("can_equip", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("familiar", DataTypes.FAMILIAR_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("can_equip", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("equip", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item", DataTypes.ITEM_TYPE), namedParam("slot", DataTypes.SLOT_TYPE));
    functions.add(new LibraryFunction("equip", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("slot", DataTypes.SLOT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("equip", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("equip", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("familiar", DataTypes.FAMILIAR_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("equip", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("slot", DataTypes.SLOT_TYPE));
    functions.add(new LibraryFunction("equipped_item", DataTypes.ITEM_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("have_equipped", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_outfits", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_custom_outfits", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "all_normal_outfits", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("outfit", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("have_outfit", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("is_wearing_outfit", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("outfit_pieces", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("outfit_tattoo", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("outfit", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "outfit_treats", new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.ITEM_TYPE), params));

    // Familiar functions.

    params = List.of();
    functions.add(new LibraryFunction("my_familiar", DataTypes.FAMILIAR_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_effective_familiar", DataTypes.FAMILIAR_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_enthroned_familiar", DataTypes.FAMILIAR_TYPE, params));

    params = List.of(namedParam("slot", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("my_poke_fam", DataTypes.FAMILIAR_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_bjorned_familiar", DataTypes.FAMILIAR_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("have_familiar", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("in_terrarium", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("use_familiar", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("enthrone_familiar", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("bjornify_familiar", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("familiar_equipment", DataTypes.ITEM_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("familiar_equipped_equipment", DataTypes.ITEM_TYPE, params));

    params = List.of(namedParam("familiar", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("familiar_weight", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("equip_all_familiars", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("is_familiar_equipment_locked", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "favorite_familiars",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.FAMILIAR_TYPE),
            params));

    params = List.of(namedParam("lock", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("lock_familiar_equipment", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("weapon_hands", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("item_type", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("weapon_type", DataTypes.STAT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("get_power", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("minstrel_level", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("minstrel_instrument", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("minstrel_quest", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_companion", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_thrall", DataTypes.THRALL_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_servant", DataTypes.SERVANT_TYPE, params));

    params = List.of(namedParam("servant", DataTypes.SERVANT_TYPE));
    functions.add(new LibraryFunction("have_servant", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("servant", DataTypes.SERVANT_TYPE));
    functions.add(new LibraryFunction("use_servant", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("my_vykea_companion", DataTypes.VYKEA_TYPE, params));

    // Random other functions related to current in-game
    // state, not directly tied to the character.

    params = List.of();
    functions.add(new LibraryFunction("council", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("current_mcd", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("level", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("change_mcd", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("current_rad_sickness", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("have_chef", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("have_bartender", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("have_shop", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("have_display", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("hippy_stone_broken", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("label", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("get_counter", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("label", DataTypes.STRING_TYPE),
            namedParam("min", DataTypes.INT_TYPE),
            namedParam("max", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("get_counters", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("label", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("stop_counter", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("eudora", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("newEudora", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("eudora", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("eudora_item", DataTypes.ITEM_TYPE, params));

    // String parsing functions.

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("is_integer", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("search", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("contains_text", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("prefix", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("starts_with", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("suffix", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("ends_with", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("extract_meat", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("extract_items", DataTypes.ITEM_TO_INT_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("length", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE), namedParam("index", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("char_at", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("search", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("index_of", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("search", DataTypes.STRING_TYPE),
            namedParam("start", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("index_of", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("search", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("last_index_of", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("search", DataTypes.STRING_TYPE),
            namedParam("start", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("last_index_of", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE), namedParam("start", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("substring", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("start", DataTypes.INT_TYPE),
            namedParam("finish", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("substring", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_lower_case", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("to_upper_case", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("leetify", DataTypes.STRING_TYPE, params));

    // String buffer functions

    params =
        List.of(
            namedParam("buffer", DataTypes.BUFFER_TYPE), namedParam("s", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("append", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("buffer", DataTypes.BUFFER_TYPE),
            namedParam("index", DataTypes.INT_TYPE),
            namedParam("s", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("insert", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("buffer", DataTypes.BUFFER_TYPE),
            namedParam("start", DataTypes.INT_TYPE),
            namedParam("finish", DataTypes.INT_TYPE),
            namedParam("s", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("replace", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("buffer", DataTypes.BUFFER_TYPE),
            namedParam("start", DataTypes.INT_TYPE),
            namedParam("finish", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("delete", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(namedParam("buffer", DataTypes.BUFFER_TYPE), namedParam("i", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("set_length", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("buffer", DataTypes.BUFFER_TYPE));
    functions.add(new LibraryFunction("append_tail", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("buffer", DataTypes.BUFFER_TYPE),
            namedParam("replacement", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("append_replacement", DataTypes.BUFFER_TYPE, params));

    // Regular expression functions

    params =
        List.of(
            namedParam("patternValue", DataTypes.STRING_TYPE),
            namedParam("stringValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("create_matcher", DataTypes.MATCHER_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("find", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("start", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE), namedParam("group", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("start", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("end", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE), namedParam("group", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("end", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("group", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE), namedParam("group", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("group", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("group", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("group", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("group_count", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(
        new LibraryFunction(
            "group_names",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE),
            params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("replacement", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("replace_first", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("replacement", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("replace_all", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("matcher", DataTypes.MATCHER_TYPE));
    functions.add(new LibraryFunction("reset", DataTypes.MATCHER_TYPE, params));

    params =
        List.of(
            namedParam("matcher", DataTypes.MATCHER_TYPE),
            namedParam("input", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("reset", DataTypes.MATCHER_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.BUFFER_TYPE),
            namedParam("searchValue", DataTypes.STRING_TYPE),
            namedParam("replaceValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("replace_string", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("source", DataTypes.STRING_TYPE),
            namedParam("searchValue", DataTypes.STRING_TYPE),
            namedParam("replaceValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("replace_string", DataTypes.BUFFER_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("split_string", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("regex", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("split_string", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("strings", new AggregateType(DataTypes.STRING_TYPE, 0)));
    functions.add(new LibraryFunction("join_strings", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("strings", new AggregateType(DataTypes.STRING_TYPE, 0)),
            namedParam("joiner", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("join_strings", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("string", DataTypes.STRING_TYPE),
            namedParam("regex", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("group_string", DataTypes.REGEX_GROUP_TYPE, params));

    // PHP RNG functions
    params = List.of(namedParam("seed", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("php_seed", DataTypes.RNG_TYPE, params));

    params = List.of(namedParam("rng", DataTypes.RNG_TYPE));
    functions.add(new LibraryFunction("php_rand", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("rng", DataTypes.RNG_TYPE));
    functions.add(new LibraryFunction("php_mt_rand", DataTypes.INT_TYPE, params));

    // Assorted functions

    params = List.of(namedParam("expr", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("expression_eval", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("expr", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("modifier_eval", DataTypes.FLOAT_TYPE, params));

    Type maximizerResultArray = new AggregateType(maximizerResult, 0);

    params =
        List.of(
            namedParam("maximizerStringValue", DataTypes.STRING_TYPE),
            namedParam("isSpeculateOnlyValue", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("maximize", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("maximizerStringValue", DataTypes.STRING_TYPE),
            namedParam("maxPriceValue", DataTypes.INT_TYPE),
            namedParam("priceLevelValue", DataTypes.INT_TYPE),
            namedParam("isSpeculateOnlyValue", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("maximize", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("maximizerStringValue", DataTypes.STRING_TYPE),
            namedParam("maxPriceValue", DataTypes.INT_TYPE),
            namedParam("priceLevelValue", DataTypes.INT_TYPE),
            namedParam("isSpeculateOnlyValue", DataTypes.BOOLEAN_TYPE),
            namedParam("showEquipment", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("maximize", maximizerResultArray, params));

    params = List.of(namedParam("expr", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("monster_eval", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("is_online", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("slash_count", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("macroValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("chat_macro", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("messageValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("chat_clan", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("messageValue", DataTypes.STRING_TYPE),
            namedParam("recipientValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("chat_clan", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("recipientValue", DataTypes.STRING_TYPE),
            namedParam("messageValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("chat_private", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("messageValue", DataTypes.STRING_TYPE),
            namedParam("colorValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("chat_notify", DataTypes.VOID_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("who_clan", DataTypes.STRING_TO_BOOLEAN_TYPE, params));

    params = List.of(namedParam("playerNameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("get_player_id", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("playerIdValue", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("get_player_name", DataTypes.STRING_TYPE, params));

    // Quest handling functions.

    params = List.of();
    functions.add(new LibraryFunction("tavern", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("goal", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("tavern", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("goal", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("hedge_maze", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("tower_door", DataTypes.BOOLEAN_TYPE, params));

    // Arithmetic utility functions.

    params = List.of(namedParam("range", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("random", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("round", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("truncate", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("floor", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("ceil", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("square_root", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("val", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("log_n", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(namedParam("val", DataTypes.FLOAT_TYPE), namedParam("base", DataTypes.FLOAT_TYPE));
    functions.add(new LibraryFunction("log_n", DataTypes.FLOAT_TYPE, params));

    // Versions of min and max that return int (if both arguments
    // are int) or float (if at least one arg is a float)
    //
    // The float versions must come first.

    params =
        List.of(
            namedParam("val", DataTypes.FLOAT_TYPE),
            namedParam("otherVal", DataTypes.VARARG_FLOAT_TYPE));
    functions.add(new LibraryFunction("min", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("val", DataTypes.INT_TYPE),
            namedParam("otherVal", DataTypes.VARARG_INT_TYPE));
    functions.add(new LibraryFunction("min", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("val", DataTypes.FLOAT_TYPE),
            namedParam("otherVal", DataTypes.VARARG_FLOAT_TYPE));
    functions.add(new LibraryFunction("max", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("val", DataTypes.INT_TYPE),
            namedParam("otherVal", DataTypes.VARARG_INT_TYPE));
    functions.add(new LibraryFunction("max", DataTypes.INT_TYPE, params));

    // String encoding/decoding functions.

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("url_encode", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("url_decode", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("entity_encode", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("string", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("entity_decode", DataTypes.STRING_TYPE, params));

    // Functions to manipulate settings

    params =
        List.of(
            namedParam("filterValue", DataTypes.STRING_TYPE),
            namedParam("globalValue", DataTypes.BOOLEAN_TYPE));
    functions.add(
        new LibraryFunction("get_all_properties", DataTypes.STRING_TO_BOOLEAN_TYPE, params));

    params = List.of(namedParam("nameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("property_exists", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("nameValue", DataTypes.STRING_TYPE),
            namedParam("globalValue", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("property_exists", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("nameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("property_has_default", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("nameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("property_default_value", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("get_property", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("name", DataTypes.STRING_TYPE),
            namedParam("globalValue", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("get_property", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("nameValue", DataTypes.STRING_TYPE),
            namedParam("value", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("set_property", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("nameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("remove_property", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("nameValue", DataTypes.STRING_TYPE),
            namedParam("globalValue", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("remove_property", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("oldNameValue", DataTypes.STRING_TYPE),
            namedParam("newNameValue", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("rename_property", DataTypes.BOOLEAN_TYPE, params));

    // Functions for aggregates.

    params = List.of(namedParam("agg", DataTypes.AGGREGATE_TYPE));
    functions.add(new LibraryFunction("count", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("agg", DataTypes.AGGREGATE_TYPE));
    functions.add(new LibraryFunction("clear", DataTypes.VOID_TYPE, params));

    params =
        List.of(
            namedParam("filename", DataTypes.STRING_TYPE),
            namedParam("result", DataTypes.AGGREGATE_TYPE));
    functions.add(new LibraryFunction("file_to_map", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("filename", DataTypes.STRING_TYPE),
            namedParam("result", DataTypes.AGGREGATE_TYPE),
            namedParam("compact", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("file_to_map", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("map", DataTypes.AGGREGATE_TYPE),
            namedParam("filename", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("map_to_file", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("map", DataTypes.AGGREGATE_TYPE),
            namedParam("filename", DataTypes.STRING_TYPE),
            namedParam("compact", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("map_to_file", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("filename", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("file_to_array", DataTypes.INT_TO_STRING_TYPE, params));

    params = List.of(namedParam("filename", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("file_to_buffer", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("buffer", DataTypes.BUFFER_TYPE),
            namedParam("filename", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("buffer_to_file", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("set_ccs", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("read_ccs", DataTypes.BUFFER_TYPE, params));

    params =
        List.of(
            namedParam("data", DataTypes.BUFFER_TYPE), namedParam("name", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("write_ccs", DataTypes.BOOLEAN_TYPE, params));

    // Custom combat helper functions.

    params = List.of();
    functions.add(new LibraryFunction("my_location", DataTypes.LOCATION_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("last_monster", DataTypes.MONSTER_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(
        new LibraryFunction("get_monsters", new AggregateType(DataTypes.MONSTER_TYPE, 0), params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(
        new LibraryFunction(
            "get_location_monsters",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_locket_monsters",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_permed_skills",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.SKILL_TYPE),
            params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_monster_mapping",
            new AggregateType(DataTypes.MONSTER_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of(namedParam("path", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "get_monster_mapping",
            new AggregateType(DataTypes.MONSTER_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(
        new LibraryFunction(
            "appearance_rates",
            new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params =
        List.of(
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("includeQueue", DataTypes.BOOLEAN_TYPE));
    functions.add(
        new LibraryFunction(
            "appearance_rates",
            new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of();
    functions.add(new LibraryFunction("expected_damage", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("expected_damage", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_level_adjustment", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("weight_adjustment", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("mana_cost_modifier", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("combat_mana_cost_modifier", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("raw_damage_absorption", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("damage_absorption_percent", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("damage_reduction", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("element", DataTypes.ELEMENT_TYPE));
    functions.add(new LibraryFunction("elemental_resistance", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("elemental_resistance", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("elemental_resistance", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("combat_rate_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("initiative_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("experience_bonus", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("meat_drop_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("item_drop_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("buffed_hit_stat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("current_hit_stat", DataTypes.STAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("current_round", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_element", DataTypes.ELEMENT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_element", DataTypes.ELEMENT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_attack", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_attack", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_defense", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_defense", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_initiative", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_initiative", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_hp", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_hp", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_phylum", DataTypes.PHYLUM_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_phylum", DataTypes.PHYLUM_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("is_banished", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("phylum", DataTypes.PHYLUM_TYPE));
    functions.add(new LibraryFunction("is_banished", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(
        new LibraryFunction("banished_by", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("track_copy_count", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("track_ignore_queue", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(
        new LibraryFunction("tracked_by", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("monster", DataTypes.MONSTER_TYPE), namedParam("init", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("monster", DataTypes.MONSTER_TYPE),
            namedParam("init", DataTypes.INT_TYPE),
            namedParam("ml", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("init", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("location", DataTypes.LOCATION_TYPE),
            namedParam("init", DataTypes.INT_TYPE),
            namedParam("ml", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("jump_chance", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("item_drops", DataTypes.ITEM_TO_FLOAT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("item_drops", DataTypes.ITEM_TO_FLOAT_TYPE, params));

    Type itemDropRecArray = new AggregateType(itemDropRec, 0);

    params = List.of();
    functions.add(new LibraryFunction("item_drops_array", itemDropRecArray, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("item_drops_array", itemDropRecArray, params));

    params = List.of();
    functions.add(new LibraryFunction("meat_drop", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("meat_drop", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("will_usually_miss", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("will_usually_dodge", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("round", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("dad_sea_monkee_weakness", DataTypes.ELEMENT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("unusual_construct_disc", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("flush_monster_manuel_cache", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("monster_manuel_text", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("monster", DataTypes.MONSTER_TYPE),
            namedParam("cachedOnly", DataTypes.BOOLEAN_TYPE));
    functions.add(new LibraryFunction("monster_factoids_available", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "all_monsters_with_id",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE),
            params));

    // Modifier introspection

    params = List.of(namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("skill", DataTypes.SKILL_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("skill", DataTypes.SKILL_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("familiar", DataTypes.FAMILIAR_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE),
            namedParam("weight", DataTypes.INT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("thrall", DataTypes.THRALL_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params =
        List.of(
            namedParam("thrall", DataTypes.THRALL_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("numeric_modifier", DataTypes.FLOAT_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("boolean_modifier", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("string_modifier", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "strings_modifier", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("effect_modifier", DataTypes.EFFECT_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("effect_modifier", DataTypes.EFFECT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("effect_modifier", DataTypes.EFFECT_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("effect_modifier", DataTypes.EFFECT_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "effects_modifier", new AggregateType(DataTypes.EFFECT_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "effects_modifier", new AggregateType(DataTypes.EFFECT_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "effects_modifier", new AggregateType(DataTypes.EFFECT_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction(
            "effects_modifier", new AggregateType(DataTypes.EFFECT_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("class_modifier", DataTypes.CLASS_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("class_modifier", DataTypes.CLASS_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("class_modifier", DataTypes.CLASS_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("class_modifier", DataTypes.CLASS_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("monster_modifier", DataTypes.MONSTER_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("monster_modifier", DataTypes.MONSTER_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("skill_modifier", DataTypes.SKILL_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("skill_modifier", DataTypes.SKILL_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("skill_modifier", DataTypes.SKILL_TYPE, params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("skill_modifier", DataTypes.SKILL_TYPE, params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("skills_modifier", new AggregateType(DataTypes.SKILL_TYPE, 0), params));

    params =
        List.of(
            namedParam("type", DataTypes.STRING_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction("skills_modifier", new AggregateType(DataTypes.SKILL_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE), namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("skills_modifier", new AggregateType(DataTypes.SKILL_TYPE, 0), params));

    params =
        List.of(
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(
        new LibraryFunction("skills_modifier", new AggregateType(DataTypes.SKILL_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("stat_modifier", DataTypes.STAT_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("modifier", DataTypes.MODIFIER_TYPE));
    functions.add(new LibraryFunction("stat_modifier", DataTypes.STAT_TYPE, params));

    params = List.of(namedParam("modifiers", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction(
            "split_modifiers",
            new AggregateType(DataTypes.STRING_TYPE, DataTypes.MODIFIER_TYPE),
            params));

    // Quest status inquiries

    params = List.of();
    functions.add(new LibraryFunction("white_citadel_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("friars_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("black_market_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("hippy_store_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("dispensary_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("guild_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("guild_store_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("hidden_temple_unlocked", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("knoll_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("canadia_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("gnomads_available", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("florist_available", DataTypes.BOOLEAN_TYPE, params));

    // Path Support

    params = List.of(namedParam("thing", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_trendy", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("is_trendy", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("is_trendy", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("is_trendy", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("is_unrestricted", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.SKILL_TYPE));
    functions.add(new LibraryFunction("is_unrestricted", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.FAMILIAR_TYPE));
    functions.add(new LibraryFunction("is_unrestricted", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("thing", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("is_unrestricted", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("project", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("svn_exists", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("project", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("svn_at_head", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("svn_list", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("script", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("svn_info", svnInfoRec, params));

    params = List.of(namedParam("project", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("git_exists", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("project", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("git_at_head", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("git_list", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of(namedParam("script", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("git_info", gitInfoRec, params));

    params =
        List.of(
            namedParam("html", DataTypes.STRING_TYPE), namedParam("xpath", DataTypes.STRING_TYPE));
    functions.add(
        new LibraryFunction("xpath", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    // Sweet Synthesis

    params = List.of();
    functions.add(new LibraryFunction("update_candy_prices", DataTypes.VOID_TYPE, params));

    params = List.of(namedParam("tier", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("candy_for_tier", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params =
        List.of(namedParam("tier", DataTypes.INT_TYPE), namedParam("flags", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("candy_for_tier", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE), namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(
        new LibraryFunction(
            "sweet_synthesis_pairing", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("item", DataTypes.ITEM_TYPE),
            namedParam("flags", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "sweet_synthesis_pairing", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params =
        List.of(namedParam("item1", DataTypes.ITEM_TYPE), namedParam("item2", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis_result", DataTypes.EFFECT_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(
        new LibraryFunction(
            "sweet_synthesis_pair", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE), namedParam("flags", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "sweet_synthesis_pair", new AggregateType(DataTypes.ITEM_TYPE, 0), params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE), namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("effect", DataTypes.EFFECT_TYPE), namedParam("count", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("effect", DataTypes.EFFECT_TYPE),
            namedParam("flags", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("item1", DataTypes.ITEM_TYPE), namedParam("item2", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(
            namedParam("count", DataTypes.INT_TYPE),
            namedParam("item1", DataTypes.ITEM_TYPE),
            namedParam("item2", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("sweet_synthesis", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("get_fuel", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("clss", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("daycount", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "voting_booth_initiatives",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE),
            params));

    params =
        List.of(
            namedParam("clss", DataTypes.INT_TYPE),
            namedParam("path", DataTypes.INT_TYPE),
            namedParam("daycount", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "voting_booth_initiatives",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE),
            params));

    // Busking Beret support

    params = List.of();
    functions.add(
        new LibraryFunction("beret_busking_effects", DataTypes.EFFECT_TO_INT_TYPE, params));

    params =
        List.of(namedParam("power", DataTypes.INT_TYPE), namedParam("cast", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction("beret_busking_effects", DataTypes.EFFECT_TO_INT_TYPE, params));

    // Cargo Cultist Shorts support

    params = List.of();
    functions.add(new LibraryFunction("picked_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("picked_scraps", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("monster_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("effect_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("item_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("stats_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("meat_pockets", PocketListType, params));

    params = List.of();
    functions.add(new LibraryFunction("poem_pockets", PocketListType, params));

    params = List.of();
    functions.add(new LibraryFunction("scrap_pockets", PocketListType, params));

    params = List.of();
    functions.add(new LibraryFunction("joke_pockets", PocketSetType, params));

    params = List.of();
    functions.add(new LibraryFunction("restoration_pockets", PocketSetType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_monster", DataTypes.MONSTER_TYPE, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_effects", PocketEffectsType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_items", PocketItemsType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_stats", PocketStatsType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_scrap", IndexedTextType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_poem", IndexedTextType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_meat", IndexedTextType, params));

    params = List.of(namedParam("pocket", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pocket_joke", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("potential_pockets", PocketListType, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("potential_pockets", PocketListType, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("potential_pockets", PocketListType, params));

    params = List.of(namedParam("stat", DataTypes.STAT_TYPE));
    functions.add(new LibraryFunction("potential_pockets", PocketListType, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("available_pocket", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("available_pocket", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("available_pocket", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("stat", DataTypes.STAT_TYPE));
    functions.add(new LibraryFunction("available_pocket", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("pick_pocket", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("pick_pocket", PocketEffectsType, params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("pick_pocket", PocketItemsType, params));

    params = List.of(namedParam("stat", DataTypes.STAT_TYPE));
    functions.add(new LibraryFunction("pick_pocket", PocketStatsType, params));

    params = List.of(namedParam("pocketNumber", DataTypes.INT_TYPE));
    functions.add(new LibraryFunction("pick_pocket", DataTypes.BOOLEAN_TYPE, params));

    // Cold Medicine Cabinet support

    params = List.of();
    functions.add(
        new LibraryFunction(
            "expected_cold_medicine_cabinet", DataTypes.STRING_TO_ITEM_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "absorbed_monsters",
            new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE),
            params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("zap", DataTypes.ITEM_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction(
            "get_autumnaton_locations", new AggregateType(DataTypes.LOCATION_TYPE, 0), params));

    params = List.of(namedParam("item", DataTypes.ITEM_TYPE));
    functions.add(new LibraryFunction("monkey_paw", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("effect", DataTypes.EFFECT_TYPE));
    functions.add(new LibraryFunction("monkey_paw", DataTypes.BOOLEAN_TYPE, params));

    params = List.of(namedParam("wish", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("monkey_paw", DataTypes.BOOLEAN_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("sausage_goblin_chance", DataTypes.FLOAT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("ping", pingTestRec, params));

    params =
        List.of(namedParam("count", DataTypes.INT_TYPE), namedParam("page", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("ping", pingTestRec, params));

    params = List.of(namedParam("pingTest", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("ping", pingTestRec, params));

    params =
        List.of(
            namedParam("cls", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("fact_type", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("fact_type", DataTypes.STRING_TYPE, params));

    params =
        List.of(
            namedParam("cls", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("item_fact", DataTypes.ITEM_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("item_fact", DataTypes.ITEM_TYPE, params));

    params =
        List.of(
            namedParam("cls", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("effect_fact", DataTypes.EFFECT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("effect_fact", DataTypes.EFFECT_TYPE, params));

    params =
        List.of(
            namedParam("cls", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("numeric_fact", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("numeric_fact", DataTypes.INT_TYPE, params));

    params =
        List.of(
            namedParam("cls", DataTypes.CLASS_TYPE),
            namedParam("path", DataTypes.PATH_TYPE),
            namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("string_fact", DataTypes.STRING_TYPE, params));

    params = List.of(namedParam("monster", DataTypes.MONSTER_TYPE));
    functions.add(new LibraryFunction("string_fact", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("dart_parts_to_skills", DataTypes.STRING_TO_SKILL_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("dart_skills_to_parts", DataTypes.SKILL_TO_STRING_TYPE, params));

    params = List.of(namedParam("location", DataTypes.LOCATION_TYPE));
    functions.add(new LibraryFunction("turns_until_forced_noncombat", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(
        new LibraryFunction("get_avatar", new AggregateType(DataTypes.STRING_TYPE, 0), params));

    params = List.of();
    functions.add(new LibraryFunction("get_title", DataTypes.STRING_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("free_crafts", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("free_cooks", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("free_mixes", DataTypes.INT_TYPE, params));

    params = List.of();
    functions.add(new LibraryFunction("free_smiths", DataTypes.INT_TYPE, params));

    params = List.of(namedParam("request", DataTypes.STRING_TYPE));
    functions.add(new LibraryFunction("allied_radio", DataTypes.BOOLEAN_TYPE, params));

    params =
        List.of(namedParam("slot", DataTypes.SLOT_TYPE), namedParam("tier", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "futuristic_wardrobe",
            new AggregateType(DataTypes.INT_TYPE, DataTypes.MODIFIER_TYPE),
            params));

    params =
        List.of(
            namedParam("day", DataTypes.INT_TYPE),
            namedParam("slot", DataTypes.SLOT_TYPE),
            namedParam("tier", DataTypes.INT_TYPE));
    functions.add(
        new LibraryFunction(
            "futuristic_wardrobe",
            new AggregateType(DataTypes.INT_TYPE, DataTypes.MODIFIER_TYPE),
            params));
  }

  public static Method findMethod(final String name, final Class<?>[] args)
      throws NoSuchMethodException {
    return RuntimeLibrary.class.getMethod(name, args);
  }

  private static Value continueValue() {
    boolean continueValue = AshRuntime.getContinueValue();

    AshRuntime.forgetPendingState();

    return DataTypes.makeBooleanValue(continueValue);
  }

  // Support for batching of server requests

  private static void batchCommand(
      ScriptRuntime controller, String cmd, String prefix, String params) {
    LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched = controller.getBatched();
    if (batched == null) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
          cmd, prefix == null ? params : (prefix + " " + params));
      return;
    }

    LinkedHashMap<String, StringBuilder> prefixMap =
        batched.computeIfAbsent(cmd, k -> new LinkedHashMap<>());

    String key = prefix == null ? "" : prefix;
    StringBuilder buf = prefixMap.get(key);
    if (buf == null) {
      buf = new StringBuilder(params);
      prefixMap.put(key, buf);
    } else {
      buf.append(", ");
      buf.append(params);
    }
  }

  public static Value get_stack_trace(ScriptRuntime controller) {
    if (!(controller instanceof AshRuntime interpreter)) {
      throw controller.runtimeException("Stack trace only supported when called from ASH.");
    }

    List<CallFrame> callStack = interpreter.getCallFrames();

    int frameCount = callStack.size();
    AggregateType type = new AggregateType(RuntimeLibrary.stackTraceRec, frameCount);
    ArrayValue value = new ArrayValue(type);

    // Topmost frame is get_stack_trace().
    for (int i = 0; i < frameCount - 1; ++i) {
      CallFrame frame = callStack.get(frameCount - 2 - i);

      RecordValue rec = (RecordValue) value.aref(new Value(i));

      rec.aset(0, new Value(frame.getFileName()), null);
      rec.aset(1, new Value(frame.getName()), null);
      rec.aset(2, new Value(frame.getLineNumber()), null);
    }

    return value;
  }

  public static Value get_version(ScriptRuntime controller) {
    return new Value(StaticEntity.getVersion());
  }

  public static Value get_revision(ScriptRuntime controller) {
    return new Value(StaticEntity.getRevision());
  }

  public static Value get_path(ScriptRuntime controller) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.STRING_INIT;
    }
    return new Value(relayRequest.getBasePath());
  }

  public static Value get_path_full(ScriptRuntime controller) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.STRING_INIT;
    }
    return new Value(relayRequest.getPath());
  }

  public static Value get_path_variables(ScriptRuntime controller) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.STRING_INIT;
    }
    String value = relayRequest.getPath();
    int quest = value.indexOf("?");
    return quest == -1 ? DataTypes.STRING_INIT : new Value(value.substring(quest));
  }

  public static Value batch_open(ScriptRuntime controller) {
    if (controller.getBatched() == null) {
      controller.setBatched(new LinkedHashMap<>());
    }
    return DataTypes.VOID_VALUE;
  }

  public static Value batch_close(ScriptRuntime controller) {
    LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched = controller.getBatched();
    if (batched != null) {
      Iterator<Entry<String, LinkedHashMap<String, StringBuilder>>> i1 =
          batched.entrySet().iterator();
      while (i1.hasNext() && KoLmafia.permitsContinue()) {
        Entry<String, LinkedHashMap<String, StringBuilder>> e1 = i1.next();
        String cmd = e1.getKey();
        LinkedHashMap<String, StringBuilder> prefixes = e1.getValue();
        Iterator<Entry<String, StringBuilder>> i2 = prefixes.entrySet().iterator();
        while (i2.hasNext() && KoLmafia.permitsContinue()) {
          Entry<String, StringBuilder> e2 = i2.next();
          String prefix = e2.getKey();
          String params = e2.getValue().toString();
          KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
              cmd, prefix.equals("") ? params : (prefix + " " + params));
        }
      }
      controller.setBatched(null);
    }

    return RuntimeLibrary.continueValue();
  }

  // Basic utility functions which print information
  // or allow for easy testing.

  public static Value enable(ScriptRuntime controller, final Value name) {
    StaticEntity.enable(name.toString().toLowerCase());
    return DataTypes.VOID_VALUE;
  }

  public static Value disable(ScriptRuntime controller, final Value name) {
    StaticEntity.disable(name.toString().toLowerCase());
    return DataTypes.VOID_VALUE;
  }

  public static Value user_confirm(ScriptRuntime controller, final Value message) {
    return DataTypes.makeBooleanValue(InputFieldUtilities.confirm(message.toString()));
  }

  public static Value user_confirm(
      ScriptRuntime controller,
      final Value message,
      final Value timeOut,
      final Value defaultBoolean) {
    return InterruptableDialog.confirm(message, timeOut, defaultBoolean);
  }

  public static Value user_prompt(ScriptRuntime controller, final Value message) {
    return DataTypes.makeStringValue(InputFieldUtilities.input(message.toString()));
  }

  public static Value user_prompt(
      ScriptRuntime controller, final Value message, final Value options) {
    AggregateValue aggregate = (AggregateValue) options;
    int size = aggregate.count();
    Value[] keys = aggregate.keys();
    Object[] javaOptions = new Object[size];

    // Extract the item ids into an array
    System.arraycopy(keys, 0, javaOptions, 0, size);

    Object result = InputFieldUtilities.input(message.toString(), javaOptions);

    return DataTypes.makeStringValue(result == null ? "" : result.toString());
  }

  public static Value user_prompt(
      ScriptRuntime controller,
      final Value message,
      final Value timeOut,
      final Value defaultString) {
    return InterruptableDialog.input(message, timeOut, defaultString);
  }

  public static Value user_notify(
      ScriptRuntime controller, final Value message, final Value onlyShowWhenHidden) {
    SystemTrayFrame.showBalloon(message.toString(), onlyShowWhenHidden.intValue() == 1);
    return DataTypes.VOID_VALUE;
  }

  public static Value user_notify(ScriptRuntime controller, final Value message) {
    return user_notify(controller, message, DataTypes.FALSE_VALUE);
  }

  private static String cleanString(Value string) {
    String parameters = string.toString();

    parameters = StringUtilities.globalStringDelete(parameters, "\n");
    parameters = StringUtilities.globalStringDelete(parameters, "\r");

    return parameters;
  }

  public static Value logprint(ScriptRuntime controller, final Value string) {
    String parameters = RuntimeLibrary.cleanString(string);
    RequestLogger.getSessionStream().println("> " + parameters);
    return DataTypes.VOID_VALUE;
  }

  public static Value debugprint(ScriptRuntime controller, final Value string) {
    if (RequestLogger.isDebugging()) {
      String parameters = RuntimeLibrary.cleanString(string);
      java.util.Date noteTime = new java.util.Date();
      RequestLogger.updateDebugLog(
          "-----User Note: " + noteTime + "-----\n" + parameters + "\n-----");
    }

    return DataTypes.VOID_VALUE;
  }

  public static Value traceprint(ScriptRuntime controller, final Value string) {
    if (RequestLogger.isTracing()) {
      String parameters = RuntimeLibrary.cleanString(string);
      RequestLogger.trace("trace: " + parameters);
    }

    return DataTypes.VOID_VALUE;
  }

  public static Value print(ScriptRuntime controller) {
    RequestLogger.printLine();
    return DataTypes.VOID_VALUE;
  }

  public static Value print(ScriptRuntime controller, final Value string) {
    RuntimeLibrary.print(controller, string, new Value(""));
    return DataTypes.VOID_VALUE;
  }

  public static Value print(ScriptRuntime controller, final Value string, final Value color) {
    String parameters = RuntimeLibrary.cleanString(string);

    RequestLogger.getSessionStream().println("> " + parameters);

    String colorString = color.toString();

    if (colorString.isEmpty()) {
      RequestLogger.printLine(parameters);
    } else {
      parameters = StringUtilities.getEntityEncode(parameters, false);
      parameters = "<font color=\"" + colorString + "\">" + parameters + "</font>";
      RequestLogger.printHtml(parameters);
    }

    return DataTypes.VOID_VALUE;
  }

  public static Value print_html(
      ScriptRuntime controller, final Value string, final Value logToSession) {
    if (logToSession.intValue() == 1) {
      String parameters = RuntimeLibrary.cleanString(string);
      parameters = StringUtilities.stripHtml(parameters);

      // Unlike print(), print_html() can do newlines in gCLI, which is why they're preserved in
      // session log
      for (String split : parameters.split("\n")) {
        RequestLogger.getSessionStream().println("> " + split.trim());
      }
    }

    RequestLogger.printHtml(string.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value print_html(ScriptRuntime controller, final Value string) {
    RequestLogger.printHtml(string.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value dump(ScriptRuntime controller, final Value arg) {
    RuntimeLibrary.dump(controller, arg, new Value(""));
    return DataTypes.VOID_VALUE;
  }

  public static Value dump(ScriptRuntime controller, final Value arg, final Value color) {
    RuntimeLibrary.print(controller, new Value(arg.toString()), color);

    Value val = Value.asProxy(arg);
    if (val instanceof CompositeValue) {
      RuntimeLibrary.dump((CompositeValue) val, "", color, true);
    }

    return DataTypes.VOID_VALUE;
  }

  public static void dump(
      final CompositeValue
          obj) { // When coming from AshSingleLineCommand.java; don't add to the session logs.
    RuntimeLibrary.dump(obj, "", new Value(""), false);
  }

  private static void dump(
      final CompositeValue obj,
      final String indent,
      final Value color,
      final boolean addToSessionStream) {
    Value[] keys = obj.keys();
    for (Value key : keys) {
      Value v = obj.aref(key);
      String value = v == null ? null : v.toString();
      if (!addToSessionStream) {
        value = StringUtilities.getEntityEncode(value);
      }
      String line = indent + key + " => " + value;

      if (addToSessionStream) {
        RuntimeLibrary.print(new AshRuntime(), new Value(line), color);
      } else {
        RequestLogger.printLine(line);
      }
      if (v instanceof CompositeValue cv) {
        RuntimeLibrary.dump(cv, indent + "\u00A0\u00A0", color, addToSessionStream);
      }
    }
  }

  public static Value abort(ScriptRuntime controller) {
    RuntimeLibrary.abort(controller, "Script aborted.");
    return DataTypes.VOID_VALUE;
  }

  public static Value abort(ScriptRuntime controller, final Value string) {
    RuntimeLibrary.abort(controller, string.toString());
    return DataTypes.VOID_VALUE;
  }

  private static Value abort(ScriptRuntime controller, final String string) {
    KoLmafia.updateDisplay(MafiaState.ABORT, string);
    controller.setState(ScriptRuntime.State.EXIT);
    return DataTypes.VOID_VALUE;
  }

  public static Value is_adventuring(ScriptRuntime controller) {
    return new Value(KoLmafia.isAdventuring());
  }

  public static Value has_queued_commands(ScriptRuntime controller) {
    return new Value(CommandDisplayFrame.hasQueuedCommands());
  }

  public static Value cli_execute(ScriptRuntime controller, final Value string) {
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(string.toString(), controller);
    return RuntimeLibrary.continueValue();
  }

  public static Value cli_execute_output(ScriptRuntime controller, final Value string) {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);

    RequestLogger.openCustom(out);
    KoLmafiaCLI.DEFAULT_SHELL.executeLine(string.toString(), controller);
    RequestLogger.closeCustom();

    return new Value(ostream.toString());
  }

  public static Value load_html(ScriptRuntime controller, final Value string) {
    StringBuffer buffer = new StringBuffer();
    Value returnValue = new Value(DataTypes.BUFFER_TYPE, "", buffer);

    String location = string.toString();
    if (!location.endsWith(".htm") && !location.endsWith(".html")) {
      return returnValue;
    }

    byte[] bytes = DataFileCache.getBytes(location);
    buffer.append(new String(bytes));
    return returnValue;
  }

  public static Value write(ScriptRuntime controller, final Value string) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.VOID_VALUE;
    }

    StringBuffer serverReplyBuffer = controller.getServerReplyBuffer();
    serverReplyBuffer.append(string.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value writeln(ScriptRuntime controller, final Value string) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.VOID_VALUE;
    }

    StringBuffer serverReplyBuffer = controller.getServerReplyBuffer();
    serverReplyBuffer.append(string.toString());
    serverReplyBuffer.append(KoLConstants.LINE_BREAK);
    return DataTypes.VOID_VALUE;
  }

  public static Value form_field(ScriptRuntime controller, final Value key) {
    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return DataTypes.STRING_INIT;
    }

    String value = relayRequest.getFormField(key.toString());
    return value == null ? DataTypes.STRING_INIT : new Value(value);
  }

  public static Value form_fields(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);
    MapValue value = new MapValue(type);

    RelayRequest relayRequest = controller.getRelayRequest();
    if (relayRequest == null) {
      return value;
    }
    for (String field : relayRequest.getFormFields()) {
      String[] pieces = field.split("=", 2);
      String name = pieces[0];
      Value keyval = DataTypes.STRING_INIT;
      if (pieces.length > 1) {
        keyval = new Value(GenericRequest.decodeField(pieces[1]));
      }
      Value keyname = new Value(name);
      while (value.contains(keyname)) { // Make a unique name for duplicate fields
        name = name + "_";
        keyname = new Value(name);
      }
      value.aset(keyname, keyval);
    }
    return value;
  }

  public static Value xpath(ScriptRuntime controller, final Value html, final Value xpath) {
    HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();

    TagNode doc;
    doc = cleaner.clean(html.toString());

    Object[] result;
    try {
      result = doc.evaluateXPath(xpath.toString());
    } catch (XPatherException e) {
      throw controller.runtimeException("invalid xpath expression");
    }

    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, result.length);
    ArrayValue value = new ArrayValue(type);

    // convert Tagnode objects to strings consisting of their inner HTML

    SimpleXmlSerializer serializer = new SimpleXmlSerializer(cleaner.getProperties());

    for (int i = 0; i < result.length; i++) {
      Object ob = result[i];

      if (ob instanceof TagNode tag) {
        result[i] = serializer.getAsString(tag);
      }

      value.aset(new Value(i), new Value(result[i].toString()));
    }

    return value;
  }

  public static Value visit_url(ScriptRuntime controller) {
    RelayRequest relayRequest = controller.getRelayRequest();
    StringBuffer buffer = new StringBuffer();
    Value returnValue = new Value(DataTypes.BUFFER_TYPE, "", buffer);

    if (relayRequest == null) {
      return returnValue;
    }

    while (true) {
      RequestThread.postRequest(relayRequest);
      if (relayRequest.redirectLocation == null) {
        break;
      }
      relayRequest.constructURLString(relayRequest.redirectLocation, false, false);
      if (KoLmafiaASH.getClientHTML(relayRequest)) {
        break;
      }
    }

    if (relayRequest.responseText != null) {
      buffer.append(relayRequest.responseText);
    }
    return returnValue;
  }

  public static Value visit_url(ScriptRuntime controller, final Value string) {
    return RuntimeLibrary.visit_url(controller, string.toString(), true, false);
  }

  public static Value visit_url(
      ScriptRuntime controller, final Value string, final Value usePostMethod) {
    return RuntimeLibrary.visit_url(
        controller, string.toString(), usePostMethod.intValue() == 1, false);
  }

  public static Value visit_url(
      ScriptRuntime controller,
      final Value string,
      final Value usePostMethod,
      final Value encoded) {
    return RuntimeLibrary.visit_url(
        controller, string.toString(), usePostMethod.intValue() == 1, encoded.intValue() == 1);
  }

  private static Value visit_url(ScriptRuntime controller, final String location) {
    return RuntimeLibrary.visit_url(controller, location, true, false);
  }

  private static Value visit_url(
      ScriptRuntime controller,
      final String location,
      final boolean usePostMethod,
      final boolean encoded) {
    StringBuffer buffer = new StringBuffer();
    Value returnValue = new Value(DataTypes.BUFFER_TYPE, "", buffer);

    // See if we are inside a relay override
    boolean inRelayOverride = controller.getRelayRequest() != null;

    // If so, use a RelayRequest rather than a GenericRequest
    GenericRequest request = inRelayOverride ? new RelayRequest(false) : new GenericRequest("");

    // Build the desired URL
    request.constructURLString(RelayServer.trimPrefix(location), usePostMethod, encoded);
    if (GenericRequest.shouldIgnore(request)) {
      return returnValue;
    }

    // If we are not in a relay script, ignore a request to an unstarted fight
    if (!inRelayOverride
        && request.getPath().equals("fight.php")
        && FightRequest.getCurrentRound() == 0) {
      return returnValue;
    }

    // Post the request and get the response!  Note that if we are
    // in a relay script, we have to follow all redirection here.
    while (true) {
      RequestThread.postRequest(request);
      if (!inRelayOverride || request.redirectLocation == null) {
        break;
      }
      request.constructURLString(request.redirectLocation, false, false);
      if (KoLmafiaASH.getClientHTML((RelayRequest) request)) {
        break;
      }
    }

    if (request.responseText != null) {
      buffer.append(request.responseText);
    }

    return returnValue;
  }

  public static Value make_url(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    String location = arg1.toString();
    boolean usePostMethod = arg2.intValue() == 1;
    boolean encoded = arg3.intValue() == 1;
    GenericRequest request = new GenericRequest("");
    request.constructURLString(location, usePostMethod, encoded);
    return new Value(request.getURLString());
  }

  public static Value wait(ScriptRuntime controller, final Value delay) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("wait", delay.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value waitq(ScriptRuntime controller, final Value delay) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("waitq", delay.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value is_dark_mode(ScriptRuntime controller) {
    return new Value(KoLmafiaGUI.isDarkTheme());
  }

  public static Value is_headless(ScriptRuntime controller) {
    return new Value(StaticEntity.isHeadless());
  }

  // Type conversion functions which allow conversion
  // of one data format to another.
  public static Value to_json(ScriptRuntime controller, Value val) throws JSONException {
    Object obj = val.asProxy().toJSON();
    return new Value(JSON.toJSONString(obj));
  }

  public static Value to_string(ScriptRuntime controller, Value val) {
    // This function previously just returned val, except in the
    // case of buffers in which case it's necessary to capture the
    // current string value of the buffer.
    //
    // That works fine in most cases, but NOT if the value ever gets
    // used as a key in a map; having a key that's actually an int (for
    // example) in a string map causes the map ordering to become
    // inconsistent, because int Values compare differently than string
    // Values.
    return val.toStringValue();
  }

  public static Value to_string(ScriptRuntime controller, Value val, Value fmt) {
    try {
      Object arg;
      if (val.getType().equals(TypeSpec.FLOAT)) {
        arg = val.floatValue();
      } else {
        arg = val.intValue();
      }
      return new Value(String.format(fmt.toString(), arg));
    } catch (IllegalFormatException e) {
      throw controller.runtimeException("Invalid format pattern");
    }
  }

  public static Value to_boolean(ScriptRuntime controller, final Value value) {
    return DataTypes.makeBooleanValue(
        (value.intValue() != 0 || value.toString().equalsIgnoreCase("true")));
  }

  public static Value to_int(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.STRING)) {
      String string = value.toString();
      try {
        return new Value(StringUtilities.parseLongInternal1(string, true));
      } catch (NumberFormatException e) {
      }

      // Try again with lax parsing
      try {
        long retval = StringUtilities.parseLongInternal2(string);
        Exception ex =
            controller.runtimeException(
                "The string \"" + string + "\" is not an integer; returning " + retval);
        RequestLogger.printLine(ex.getMessage());
        return new Value(retval);
      } catch (NumberFormatException e) {
        // Even with lax parsing, we failed.
        Exception ex =
            controller.runtimeException(
                "The string \"" + string + "\" does not look like an integer; returning 0");
        RequestLogger.printLine(ex.getMessage());
        return DataTypes.ZERO_VALUE;
      }
    }

    if (value.getType().equals(DataTypes.LOCATION_TYPE) && value.content != null) {
      return new Value(((KoLAdventure) value.content).getAdventureNumber());
    }

    return new Value(value.intValue());
  }

  public static Value to_float(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.STRING)) {
      String string = value.toString();
      try {
        return new Value(StringUtilities.parseDouble(string));
      } catch (NumberFormatException e) {
        Exception ex =
            controller.runtimeException(
                "The string \"" + string + "\" is not a float; returning 0.0");
        RequestLogger.printLine(ex.getMessage());
        return DataTypes.ZERO_FLOAT_VALUE;
      }
    }

    return value.toFloatValue();
  }

  public static Value to_buffer(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.STRING)) {
      String string = value.toString();
      return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer(string));
    }
    StringBuffer buffer = (StringBuffer) value.rawValue();
    return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer(buffer));
  }

  public static Value to_item(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeItemValue((int) value.intValue(), true);
    }

    String s1 = value.toString();
    Value item = DataTypes.parseItemValue(s1, true, true);
    DataTypes.ITEM_TYPE.validateValue(controller, s1, item);

    return item;
  }

  public static Value to_item(ScriptRuntime controller, final Value name, final Value count) {
    return DataTypes.makeItemValue(
        ItemDatabase.getItemId(name.toString(), (int) count.intValue()), true);
  }

  public static Value desc_to_item(ScriptRuntime controller, final Value value) {
    return DataTypes.makeItemValue(ItemDatabase.getItemIdFromDescription(value.toString()), true);
  }

  public static Value path_name_to_id(ScriptRuntime controller, final Value value) {
    Path path = AscensionPath.nameToPath(value.toString());
    return DataTypes.makeIntValue(path == null ? -1 : path.getId());
  }

  public static Value path_id_to_name(ScriptRuntime controller, final Value value) {
    Path path = AscensionPath.idToPath((int) value.intValue());
    return DataTypes.makeStringValue(path.getName());
  }

  public static Value to_class(ScriptRuntime controller, final Value value) {
    String name = null;

    if (value.getType().equals(DataTypes.INT_TYPE)) {
      int num = (int) value.intValue();

      if (num >= 0) {
        return DataTypes.makeClassValue(AscensionClass.find(num), true);
      }
    } else {
      name = value.toString();
    }

    return DataTypes.parseClassValue(name, true);
  }

  public static Value to_stat(ScriptRuntime controller, final Value value) {
    return DataTypes.parseStatValue(value.toString(), true);
  }

  public static Value to_skill(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeSkillValue((int) value.intValue(), true);
    }

    if (value.getType().equals(TypeSpec.EFFECT)) {
      return DataTypes.parseSkillValue(UneffectRequest.effectToSkill(value.toString()), true);
    }

    String s1 = value.toString();
    Value skill = DataTypes.parseSkillValue(s1, true);
    DataTypes.SKILL_TYPE.validateValue(controller, s1, skill);

    return skill;
  }

  public static Value to_skill(ScriptRuntime controller, final Value value1, final Value value2) {
    String name = value1.toString();
    String type = value2.toString();
    return DataTypes.parseSkillValue(name, type, true);
  }

  public static Value desc_to_effect(ScriptRuntime controller, final Value value) {
    return DataTypes.makeEffectValue(
        EffectDatabase.getEffectIdFromDescription(value.toString()), true);
  }

  public static Value to_effect(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeEffectValue((int) value.intValue(), true);
    }

    if (value.getType().equals(TypeSpec.SKILL)) {
      return DataTypes.parseEffectValue(UneffectRequest.skillToEffect(value.toString()), true);
    }

    String s1 = value.toString();
    Value effect = DataTypes.parseEffectValue(s1, true);
    DataTypes.EFFECT_TYPE.validateValue(controller, s1, effect);

    return effect;
  }

  public static Value to_location(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.parseLocationValue((int) value.intValue(), true);
    } else {
      return DataTypes.parseLocationValue(value.toString(), true);
    }
  }

  public static Value to_familiar(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeFamiliarValue((int) value.intValue(), true);
    }

    return DataTypes.parseFamiliarValue(value.toString(), true);
  }

  public static Value to_modifier(ScriptRuntime controller, final Value value) {
    return DataTypes.parseModifierValue(value.toString(), true);
  }

  public static Value to_monster(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeMonsterValue((int) value.intValue(), true);
    }

    String s1 = value.toString();
    Value monster = DataTypes.parseMonsterValue(s1, true);
    DataTypes.MONSTER_TYPE.validateValue(controller, s1, monster);

    return monster;
  }

  public static Value image_to_monster(ScriptRuntime controller, final Value value) {
    MonsterData monster = MonsterDatabase.findMonsterByImage(value.toString());
    return DataTypes.makeMonsterValue(monster);
  }

  public static Value to_slot(ScriptRuntime controller, final Value item) {
    if (!item.getType().equals(TypeSpec.ITEM)) {
      return DataTypes.parseSlotValue(item.toString(), true);
    }
    return switch (ItemDatabase.getConsumptionType((int) item.intValue())) {
      case HAT -> DataTypes.parseSlotValue("hat", true);
      case WEAPON -> DataTypes.parseSlotValue("weapon", true);
      case OFFHAND -> DataTypes.parseSlotValue("off-hand", true);
      case SHIRT -> DataTypes.parseSlotValue("shirt", true);
      case PANTS -> DataTypes.parseSlotValue("pants", true);
      case CONTAINER -> DataTypes.parseSlotValue("container", true);
      case FAMILIAR_EQUIPMENT -> DataTypes.parseSlotValue("familiar", true);
      case ACCESSORY -> DataTypes.parseSlotValue("acc1", true);
      default -> DataTypes.parseSlotValue("none", true);
    };
  }

  public static Value to_element(ScriptRuntime controller, final Value value) {
    return DataTypes.parseElementValue(value.toString(), true);
  }

  public static Value to_coinmaster(ScriptRuntime controller, final Value value) {
    return DataTypes.parseCoinmasterValue(value.toString(), true);
  }

  public static Value to_phylum(ScriptRuntime controller, final Value value) {
    return DataTypes.parsePhylumValue(value.toString(), true);
  }

  public static Value to_bounty(ScriptRuntime controller, final Value value) {
    String stringValue = value.toString();
    int numberIndex = stringValue.indexOf(":");
    return numberIndex != -1
        ? DataTypes.parseBountyValue(stringValue.substring(0, numberIndex), true)
        : DataTypes.parseBountyValue(stringValue, true);
  }

  public static Value to_thrall(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeThrallValue((int) value.intValue(), true);
    }

    return DataTypes.parseThrallValue(value.toString(), true);
  }

  public static Value to_servant(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.makeServantValue((int) value.intValue(), true);
    }

    return DataTypes.parseServantValue(value.toString(), true);
  }

  public static Value to_vykea(ScriptRuntime controller, final Value value) {
    return DataTypes.parseVykeaValue(value.toString(), true);
  }

  public static Value to_path(ScriptRuntime controller, final Value value) {
    if (value.getType().equals(TypeSpec.INT)) {
      return DataTypes.parsePathValue((int) value.intValue(), true);
    }

    return DataTypes.parsePathValue(value.toString(), true);
  }

  public static Value to_plural(ScriptRuntime controller, final Value item) {
    return new Value(ItemDatabase.getPluralName((int) item.intValue()));
  }

  public static Value to_url(ScriptRuntime controller, final Value value) {
    KoLAdventure adventure = (KoLAdventure) value.rawValue();
    return (adventure == null)
        ? DataTypes.STRING_INIT
        : new Value(adventure.getRequest().getURLString());
  }

  public static Value to_wiki_url(ScriptRuntime controller, final Value value) {
    var type =
        switch (value.getType().getType()) {
          case EFFECT -> WikiUtilities.WikiType.EFFECT;
          case ITEM -> WikiUtilities.WikiType.ITEM;
          case MONSTER -> WikiUtilities.WikiType.MONSTER;
          case SKILL -> WikiUtilities.WikiType.SKILL;
          default -> WikiUtilities.WikiType.ANY;
        };

    var link = WikiUtilities.getWikiLocation(value.toString(), type, false);
    return new Value(link);
  }

  // Functions related to daily information which get
  // updated usually once per day.

  public static Value holiday(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getHoliday());
  }

  public static Value today_to_string(ScriptRuntime controller) {
    return new Value(KoLConstants.DAILY_FORMAT.format(new Date()));
  }

  public static Value time_to_string(ScriptRuntime controller) {
    Calendar timestamp = new GregorianCalendar();
    return new Value(KoLConstants.TIME_FORMAT.format(timestamp.getTime()));
  }

  public static Value now_to_string(ScriptRuntime controller, Value dateFormatValue) {
    Calendar timestamp = new GregorianCalendar();
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatValue.toString());
    return new Value(dateFormat.format(timestamp.getTime()));
  }

  public static Value now_to_int(ScriptRuntime controller) {
    Calendar timestamp = new GregorianCalendar();
    return new Value(timestamp.getTimeInMillis());
  }

  public static Value date_to_timestamp(
      ScriptRuntime controller, Value inFormat, Value dateTimeString) {
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat(inFormat.toString());
      Date inDate = dateFormat.parse(dateTimeString.toString());
      return new Value(inDate.getTime());
    } catch (Exception e) {
      e.printStackTrace();
      controller.runtimeException("Bad parameter(s) passed to date_to_timestamp");
    }

    return DataTypes.VOID_VALUE;
  }

  public static Value timestamp_to_date(
      ScriptRuntime controller, Value timestamp, Value outFormat) {
    try {
      Date in = new Date(timestamp.toIntValue().intValue());
      SimpleDateFormat dateFormat = new SimpleDateFormat(outFormat.toString());
      return new Value(dateFormat.format(in));
    } catch (Exception e) {
      e.printStackTrace();
      controller.runtimeException("Bad parameter(s) passed to timestamp_to_date");
    }

    return DataTypes.VOID_VALUE;
  }

  public static Value format_date_time(
      ScriptRuntime controller, Value inFormat, Value dateTimeString, Value outFormat) {
    Date inDate;
    SimpleDateFormat dateFormat;
    Value retVal;

    try {
      dateFormat = new SimpleDateFormat(inFormat.toString());
      inDate = dateFormat.parse(dateTimeString.toString());
      dateFormat = new SimpleDateFormat(outFormat.toString());
      retVal = new Value(dateFormat.format(inDate));
    } catch (Exception e) {
      e.printStackTrace();
      retVal = new Value("Bad parameter(s) passed to format_date_time");
    }
    return retVal;
  }

  public static Value gameday_to_string(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getCalendarDayAsString(HolidayDatabase.getDayInKoLYear()));
  }

  public static Value gameday_to_int(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getDayInKoLYear());
  }

  public static Value gametime_to_int(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getTimeInKoLDay());
  }

  public static Value rollover(ScriptRuntime controller) {
    return new Value(KoLCharacter.getRollover());
  }

  public static Value moon_phase(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getPhaseStep());
  }

  public static Value moon_light(ScriptRuntime controller) {
    return new Value(HolidayDatabase.getMoonlight());
  }

  public static Value stat_bonus_today(ScriptRuntime controller) {
    return DataTypes.parseStatValue(HolidayDatabase.getStatDay().toString(), true);
  }

  public static Value stat_bonus_tomorrow(ScriptRuntime controller) {
    return DataTypes.parseStatValue(
        HolidayDatabase.getStatDay(DateTimeManager.getRolloverDateTime().plusDays(1)).toString(),
        true);
  }

  public static Value session_logs(ScriptRuntime controller, final Value dayCount) {
    return RuntimeLibrary.getSessionLogs(
        controller, KoLCharacter.getUserName(), (int) dayCount.intValue());
  }

  public static Value session_logs(
      ScriptRuntime controller, final Value player, final Value dayCount) {
    return RuntimeLibrary.getSessionLogs(controller, player.toString(), (int) dayCount.intValue());
  }

  private static Value getSessionLogs(
      ScriptRuntime controller, final String name, final int dayCount) {
    if (dayCount < 0) {
      throw controller.runtimeException("Can't get session logs for a negative number of days");
    }

    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, dayCount);
    ArrayValue value = new ArrayValue(type);

    if (dayCount < 1) {
      return value;
    }

    Calendar timestamp = Calendar.getInstance(KoLmafia.KOL_TIME_ZONE);

    for (int i = 0; i < dayCount; ++i) {
      String logContents =
          getContentsOfSessionLog(name, KoLConstants.DAILY_FORMAT.format(timestamp.getTime()));
      timestamp.add(Calendar.DATE, -1);
      value.aset(new Value(i), new Value(logContents));
    }
    return value;
  }

  public static Value session_logs(
      ScriptRuntime controller, final Value playerName, final Value baseDate, final Value count) {
    String pName = playerName.contentString;
    int countVal = (int) count.contentLong;
    int size = Math.abs(countVal) + 1;
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, size);
    ArrayValue value = new ArrayValue(type);
    Calendar timestamp = getBaseTimeStamp(baseDate.contentString, countVal);
    for (int i = 0; i < size; i++) {
      String logContents =
          getContentsOfSessionLog(pName, KoLConstants.DAILY_FORMAT.format(timestamp.getTime()));
      timestamp.add(Calendar.DATE, 1);
      value.aset(new Value(i), new Value(logContents));
    }
    return value;
  }

  private static String getContentsOfSessionLog(String playerName, String logDate) {
    StringBuilder contents = new StringBuilder();
    String filename =
        StringUtilities.globalStringReplace(playerName, " ", "_") + "_" + logDate + ".txt";

    File path = new File(KoLConstants.SESSIONS_LOCATION, filename);
    BufferedReader reader = null;
    if (!path.exists()) {
      filename = filename + ".gz";
      File gzpath = new File(KoLConstants.SESSIONS_LOCATION, filename);
      if (gzpath.exists()) {
        try {
          reader =
              DataUtilities.getReader(new GZIPInputStream(DataUtilities.getInputStream(gzpath)));
        } catch (IOException e) {
          StaticEntity.printStackTrace(e);
          reader = null;
        }
      }
    } else {
      reader = FileUtilities.getReader(path);
    }

    if (reader != null) {
      try {
        contents.setLength(0);
        String line;
        while ((line = reader.readLine()) != null) {
          contents.append(line);
          contents.append(KoLConstants.LINE_BREAK);
        }
      } catch (Exception e) {
        StaticEntity.printStackTrace(e);
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          StaticEntity.printStackTrace(e);
        }
      }
    }
    return contents.toString();
  }

  private static Calendar getBaseTimeStamp(String base, int count) {
    Calendar timestamp = Calendar.getInstance(KoLmafia.KOL_TIME_ZONE);
    timestamp.clear();
    int year = Integer.parseInt(base.substring(0, 4));
    int mon = Integer.parseInt(base.substring(4, 6));
    int day = Integer.parseInt(base.substring(6, 8));
    mon = mon - 1; // Calendar thinks January is 0
    timestamp.set(year, mon, day);
    if (count < 0) {
      timestamp.add(Calendar.DATE, count);
    }
    return timestamp;
  }

  // Major functions related to adventuring and item management.

  public static Value pre_validate_adventure(ScriptRuntime controller, final Value arg) {
    KoLAdventure location = (KoLAdventure) arg.content;
    if (location == null) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeBooleanValue(location.preValidateAdventure());
  }

  public static Value can_adventure(ScriptRuntime controller, final Value arg) {
    KoLAdventure location = (KoLAdventure) arg.content;
    if (location == null) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeBooleanValue(location.canAdventure());
  }

  public static Value prepare_for_adventure(ScriptRuntime controller, final Value arg) {
    KoLAdventure location = (KoLAdventure) arg.content;
    if (location == null) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeBooleanValue(location.prepareForAdventure());
  }

  public static Value adventure(ScriptRuntime controller, final Value arg1, final Value arg2) {
    boolean countThenLocation =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);
    int count = (int) (countThenLocation ? arg1.intValue() : arg2.intValue());

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    Value location = (countThenLocation ? arg2 : arg1);
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("adventure", count + " " + location);
    return RuntimeLibrary.continueValue();
  }

  public static Value adventure(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value filterFunction) {
    try {
      String filter = filterFunction.toString();
      Macrofier.setMacroOverride(filter, controller);

      RuntimeLibrary.adventure(controller, arg1, arg2);
    } finally {
      Macrofier.resetMacroOverride();
    }

    if (controller.getState() == ScriptRuntime.State.EXIT) {
      return DataTypes.VOID_VALUE;
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value adv1(ScriptRuntime controller, final Value locationValue) {
    return adv1(controller, locationValue, new Value(-1), new Value(""));
  }

  public static Value adv1(
      ScriptRuntime controller, final Value locationValue, final Value adventuresUsedValue) {
    return adv1(controller, locationValue, adventuresUsedValue, new Value(""));
  }

  public static Value adv1(
      ScriptRuntime controller,
      final Value locationValue,
      final Value adventuresUsedValue,
      final Value filterFunction) {
    KoLAdventure adventure = (KoLAdventure) locationValue.rawValue();

    if (adventure == null) {
      return RuntimeLibrary.continueValue();
    }

    boolean redoSkippedAdventures = KoLmafia.redoSkippedAdventures;
    try {
      adventure.overrideAdventuresUsed((int) adventuresUsedValue.intValue());
      Macrofier.setMacroOverride(filterFunction.toString(), controller);
      KoLmafia.redoSkippedAdventures = false;

      KoLmafia.makeRequest(adventure, 1);
    } finally {
      KoLmafia.redoSkippedAdventures = redoSkippedAdventures;
      Macrofier.resetMacroOverride();
      adventure.overrideAdventuresUsed(-1);
    }

    if (controller.getState() == ScriptRuntime.State.EXIT) {
      return DataTypes.VOID_VALUE;
    }

    return RuntimeLibrary.continueValue();
  }

  private record EightBitZone(DoubleModifier mod, int base, String color) {}

  private static final Map<Integer, EightBitZone> EIGHT_BIT_ZONES =
      Map.ofEntries(
          Map.entry(
              AdventurePool.FUNGUS_PLAINS, new EightBitZone(DoubleModifier.MEATDROP, 150, "red")),
          Map.entry(
              AdventurePool.HEROS_FIELD, new EightBitZone(DoubleModifier.ITEMDROP, 100, "green")),
          Map.entry(
              AdventurePool.VANYAS_CASTLE,
              new EightBitZone(DoubleModifier.INITIATIVE, 300, "black")),
          Map.entry(
              AdventurePool.MEGALO_CITY,
              new EightBitZone(DoubleModifier.DAMAGE_ABSORPTION, 300, "blue")));

  public static Value eight_bit_points(ScriptRuntime controller, final Value locationValue) {
    var location = (KoLAdventure) locationValue.rawValue();
    EightBitZone zone = EIGHT_BIT_ZONES.getOrDefault(location.getSnarfblat(), null);
    if (zone == null) {
      return DataTypes.ZERO_VALUE;
    }

    String color = Preferences.getString("8BitColor");
    double modValue = KoLCharacter.currentNumericModifier(zone.mod());

    return eight_bit_points(zone, color, modValue);
  }

  public static Value eight_bit_points(EightBitZone zone, String color, double modValue) {
    boolean isBonus = zone.color().equals(color);
    int base = isBonus ? 100 : 50;
    int divisor = isBonus ? 10 : 20;
    long bonus = Math.round(Math.min(300, Math.max(0, modValue - zone.base)) / divisor) * 10;
    return new Value(base + bonus);
  }

  public static Value eight_bit_points(
      ScriptRuntime controller,
      final Value locationValue,
      final Value colorValue,
      final Value modValue) {
    var location = (KoLAdventure) locationValue.rawValue();
    EightBitZone zone = EIGHT_BIT_ZONES.getOrDefault(location.getSnarfblat(), null);
    if (zone == null) {
      return DataTypes.ZERO_VALUE;
    }

    String color = RuntimeLibrary.cleanString(colorValue);
    double mod = modValue.floatValue();

    return eight_bit_points(zone, color, mod);
  }

  public static Value get_ccs_action(ScriptRuntime controller, final Value index) {
    return new Value(
        CombatActionManager.getCombatAction(
            FightRequest.getCurrentKey(), (int) index.intValue(), true));
  }

  public static Value can_still_steal(ScriptRuntime controller) {
    return new Value(FightRequest.canStillSteal());
  }

  public static Value add_item_condition(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);
    int count = (int) (countThenItem ? arg1 : arg2).intValue();
    int itemId = (int) (countThenItem ? arg2 : arg1).intValue();
    if (count <= 0 || itemId <= 0) {
      return DataTypes.VOID_VALUE;
    }

    GoalManager.addItemGoal(itemId, count);
    return DataTypes.VOID_VALUE;
  }

  public static Value remove_item_condition(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);
    int count = (int) (countThenItem ? arg1 : arg2).intValue();
    int itemId = (int) (countThenItem ? arg2 : arg1).intValue();

    if (count <= 0 || itemId <= 0) {
      return DataTypes.VOID_VALUE;
    }

    GoalManager.addItemGoal(itemId, -count);
    return DataTypes.VOID_VALUE;
  }

  public static Value goal_exists(ScriptRuntime controller, final Value check) {
    String checkType = check.toString();

    for (AdventureResult goal : GoalManager.getGoals()) {
      if (checkType.equals(goal.getConditionType())) {
        return DataTypes.TRUE_VALUE;
      }
    }

    return DataTypes.FALSE_VALUE;
  }

  public static Value is_goal(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(GoalManager.hasItemGoal((int) item.intValue()));
  }

  public static Value get_goals(ScriptRuntime controller) {
    List<AdventureResult> goals = GoalManager.getGoals();
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, goals.size());
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < goals.size(); ++i) {
      value.aset(new Value(i), new Value(goals.get(i).toConditionString()));
    }

    return value;
  }

  public static Value get_moods(ScriptRuntime controller) {
    List<Mood> moods = MoodManager.getAvailableMoods();
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, moods.size());
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < moods.size(); ++i) {
      value.aset(new Value(i), new Value(moods.get(i).getName()));
    }

    return value;
  }

  public static Value mood_list(ScriptRuntime controller) {
    List<MoodTrigger> moodTriggers = MoodManager.getTriggers();
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, moodTriggers.size());
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < moodTriggers.size(); ++i) {
      MoodTrigger mt = moodTriggers.get(i);
      String sv = mt.getType() + " | " + mt.getName() + " | " + mt.getAction();
      value.aset(new Value(i), new Value(sv));
    }
    return value;
  }

  public static Value buy(ScriptRuntime controller, final Value item) {
    return buy(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value buy(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int item = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    AdventureResult itemToBuy = ItemPool.get(item);
    int initialAmount = itemToBuy.getCount(KoLConstants.inventory);
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("buy", count + " \u00B6" + item);
    return DataTypes.makeBooleanValue(
        initialAmount + count == itemToBuy.getCount(KoLConstants.inventory));
  }

  public static Value buy(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    if (arg1.getType().equals(TypeSpec.COINMASTER)) {
      return RuntimeLibrary.coinmaster_buy(controller, arg1, arg2, arg3);
    }

    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;

    if (count <= 0) {
      return DataTypes.ZERO_VALUE;
    }

    int itemId = countThenItem ? arg2Value : arg1Value;
    int limit = (int) arg3.intValue();

    AdventureResult itemToBuy = ItemPool.get(itemId);

    int initialAmount = itemToBuy.getCount(KoLConstants.inventory);
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("buy", count + " \u00B6" + itemId + "@" + limit);
    return new Value(itemToBuy.getCount(KoLConstants.inventory) - initialAmount);
  }

  public static Value buy_using_storage(ScriptRuntime controller, final Value item) {
    return buy_using_storage(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value buy_using_storage(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    if (KoLCharacter.canInteract()) {
      return DataTypes.FALSE_VALUE;
    }

    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int item = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return DataTypes.TRUE_VALUE;
    }

    AdventureResult itemToBuy = ItemPool.get(item);
    int initialAmount = itemToBuy.getCount(KoLConstants.storage);
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("buy", "using storage " + count + " \u00B6" + item);
    return DataTypes.makeBooleanValue(
        initialAmount + count == itemToBuy.getCount(KoLConstants.storage));
  }

  public static Value buy_using_storage(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    if (KoLCharacter.canInteract()) {
      return DataTypes.ZERO_VALUE;
    }

    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;

    if (count <= 0) {
      return DataTypes.ZERO_VALUE;
    }

    int itemId = countThenItem ? arg2Value : arg1Value;
    int limit = (int) arg3.intValue();

    AdventureResult itemToBuy = ItemPool.get(itemId);

    int initialAmount = itemToBuy.getCount(KoLConstants.storage);
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
        "buy", "using storage " + count + " \u00B6" + itemId + "@" + limit);
    return new Value(itemToBuy.getCount(KoLConstants.storage) - initialAmount);
  }

  // Coinmaster functions

  public static Value is_accessible(ScriptRuntime controller, final Value master) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    return DataTypes.makeBooleanValue(data != null && data.isAccessible());
  }

  public static Value inaccessible_reason(ScriptRuntime controller, final Value master) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    String reason = data != null ? data.accessible() : null;
    return new Value(reason != null ? reason : "");
  }

  public static Value visit(ScriptRuntime controller, final Value master) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    CoinMasterRequest.visit(data);
    return RuntimeLibrary.continueValue();
  }

  private static Value coinmaster_buy(
      ScriptRuntime controller, final Value master, final Value countValue, final Value itemValue) {
    int count = (int) countValue.intValue();
    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }
    CoinmasterData data = (CoinmasterData) master.rawValue();
    AdventureResult item = ItemPool.get((int) itemValue.intValue(), count);
    int initialAmount = item.getCount(KoLConstants.inventory);
    CoinMasterRequest.buy(data, item);
    return DataTypes.makeBooleanValue(
        initialAmount + count == item.getCount(KoLConstants.inventory));
  }

  public static Value sell(
      ScriptRuntime controller, final Value master, final Value countValue, final Value itemValue) {
    int count = (int) countValue.intValue();
    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    CoinmasterData data = (CoinmasterData) master.rawValue();
    int itemId = (int) itemValue.intValue();

    if (controller.getBatched() != null) {
      String cmd = "coinmaster";
      String prefix = "sell " + data.getNickname();
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult item = ItemPool.get(itemId, count);
      CoinMasterRequest.sell(data, item);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value craft(
      ScriptRuntime controller,
      final Value modeValue,
      final Value countValue,
      final Value item1,
      final Value item2) {
    int count = (int) countValue.intValue();
    if (count <= 0) {
      return DataTypes.ZERO_VALUE;
    }

    String mode = modeValue.toString();
    int id1 = (int) item1.intValue();
    int id2 = (int) item2.intValue();

    CraftRequest req = new CraftRequest(mode, count, id1, id2);
    RequestThread.postRequest(req);
    return new Value(req.created());
  }

  private static Value execute_item_quantity(
      final String command, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int item = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand(command, count + " \u00B6" + item);
    return UseItemRequest.lastUpdate.equals("")
        ? RuntimeLibrary.continueValue()
        : DataTypes.FALSE_VALUE;
  }

  public static Value create(ScriptRuntime controller, final Value item) {
    return create(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value create(ScriptRuntime controller, final Value arg1, final Value arg2) {
    execute_item_quantity("create", arg1, arg2);
    return RuntimeLibrary.continueValue();
  }

  public static Value use(ScriptRuntime controller, final Value item) {
    return use(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value use(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("use", arg1, arg2);
  }

  public static Value eat(ScriptRuntime controller, final Value item) {
    return eat(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value eat(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("eat", arg1, arg2);
  }

  public static Value eatsilent(ScriptRuntime controller, final Value item) {
    return eatsilent(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value eatsilent(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("eatsilent", arg1, arg2);
  }

  public static Value clear_food_helper(ScriptRuntime controller) {
    EatItemRequest.clearFoodHelper();
    return DataTypes.VOID_VALUE;
  }

  public static Value drink(ScriptRuntime controller, final Value item) {
    return drink(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value drink(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("drink", arg1, arg2);
  }

  public static Value overdrink(ScriptRuntime controller, final Value item) {
    return overdrink(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value overdrink(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("overdrink", arg1, arg2);
  }

  public static Value drinksilent(ScriptRuntime controller, final Value item) {
    return drinksilent(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value drinksilent(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("drinksilent", arg1, arg2);
  }

  public static Value clear_booze_helper(ScriptRuntime controller) {
    DrinkItemRequest.clearBoozeHelper();
    return DataTypes.VOID_VALUE;
  }

  public static Value chew(ScriptRuntime controller, final Value item) {
    return chew(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value chew(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return execute_item_quantity("chew", arg1, arg2);
  }

  public static Value curse(ScriptRuntime controller, final Value itemId, final Value target) {
    return curse(controller, itemId, target, new Value(""));
  }

  public static Value curse(
      ScriptRuntime controller, final Value itemId, final Value target, final Value message) {
    return curse(controller, DataTypes.ONE_VALUE, itemId, target, message);
  }

  public static Value curse(
      ScriptRuntime controller,
      final Value quantity,
      final Value itemId,
      final Value target,
      final Value message) {

    AdventureResult item = ItemPool.get((int) itemId.intValue(), (int) quantity.intValue());
    if (!ItemDatabase.getAttribute(item.getItemId(), Attribute.CURSE)) {
      throw controller.runtimeException("The " + item.getName() + " cannot be used for cursing.");
    }

    CurseRequest request = new CurseRequest(item, target.toString(), message.toString());

    RequestThread.postRequest(request);

    return RuntimeLibrary.continueValue();
  }

  public static Value last_item_message(ScriptRuntime controller) {
    return new Value(UseItemRequest.lastUpdate);
  }

  public static Value empty_closet(ScriptRuntime controller) {
    if (controller.getBatched() != null) {
      RuntimeLibrary.batchCommand(controller, "closet", null, "empty");
    } else {
      ClosetRequest request = new ClosetRequest(ClosetRequestType.EMPTY_CLOSET);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value put_closet(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "closet";
      String prefix = "put";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      ClosetRequest request =
          new ClosetRequest(ClosetRequestType.INVENTORY_TO_CLOSET, ItemPool.get(itemId, count));
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value put_closet(ScriptRuntime controller, final Value arg1) {
    if (!arg1.getType().equals(DataTypes.INT_TYPE)) {
      return put_closet(controller, DataTypes.ONE_VALUE, arg1);
    }

    long meat = arg1.intValue();
    if (meat <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "closet";
      String prefix = "put";
      String params = meat + " meat";
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      ClosetRequest request = new ClosetRequest(ClosetRequestType.MEAT_TO_CLOSET, meat);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value put_shop(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final Value itemValue) {
    return put_shop(
        controller,
        priceValue,
        limitValue,
        InventoryManager.getCount((int) itemValue.contentLong),
        itemValue,
        false);
  }

  public static Value put_shop(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final Value qtyValue,
      final Value itemValue) {
    return put_shop(controller, priceValue, limitValue, qtyValue.intValue(), itemValue, false);
  }

  public static Value put_shop_using_storage(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final Value itemValue) {
    AdventureResult item = ItemPool.get((int) itemValue.intValue(), 0);
    return put_shop(
        controller, priceValue, limitValue, item.getCount(KoLConstants.storage), itemValue, true);
  }

  public static Value put_shop_using_storage(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final Value qtyValue,
      final Value itemValue) {
    return put_shop(controller, priceValue, limitValue, qtyValue.intValue(), itemValue, true);
  }

  // Used internally only.
  private static Value put_shop(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final long qty,
      final Value itemValue,
      boolean usingStorage) {
    if (qty <= 0) {
      return RuntimeLibrary.continueValue();
    }

    int itemId = (int) itemValue.intValue();
    long price = priceValue.intValue();
    int limit = (int) limitValue.intValue();

    if (controller.getBatched() != null) {
      String cmd = "shop";
      String prefix = usingStorage ? "put using storage" : "put";
      String params = qty + " \u00B6" + itemId + " @ " + price + " limit " + limit;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult[] items = {ItemPool.get(itemId, (int) qty)};
      long[] prices = {price};
      int[] limits = {limit};

      ManageStoreRequest request = new ManageStoreRequest(items, prices, limits, usingStorage);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value reprice_shop(
      ScriptRuntime controller, final Value priceValue, final Value itemValue) {
    int itemId = (int) itemValue.intValue();
    return reprice_shop(
        controller, priceValue, new Value(StoreManager.getLimit(itemId)), itemValue);
  }

  public static Value reprice_shop(
      ScriptRuntime controller,
      final Value priceValue,
      final Value limitValue,
      final Value itemValue) {
    int itemId = (int) itemValue.intValue();
    long price = priceValue.intValue();
    int limit = (int) limitValue.intValue();

    if (controller.getBatched() != null) {
      String cmd = "shop";
      String prefix = "reprice";
      String params = "\u00B6" + itemId + " @ " + price + " limit " + limit;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      int[] itemIds = {itemId};
      long[] prices = {price};
      int[] limits = {limit};

      ManageStoreRequest request = new ManageStoreRequest(itemIds, prices, limits);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value put_stash(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "stash";
      String prefix = "put";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = ItemPool.get(itemId, count);
      ClanStashRequest request = new ClanStashRequest(items, ClanStashRequestType.ITEMS_TO_STASH);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value put_display(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "display";
      String prefix = "put";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = ItemPool.get(itemId, count);
      DisplayCaseRequest request = new DisplayCaseRequest(items, true);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value take_closet(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "closet";
      String prefix = "take";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      ClosetRequest request =
          new ClosetRequest(ClosetRequestType.CLOSET_TO_INVENTORY, ItemPool.get(itemId, count));
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value take_closet(ScriptRuntime controller, final Value arg1) {
    if (!arg1.getType().equals(DataTypes.INT_TYPE)) {
      return take_closet(controller, DataTypes.ONE_VALUE, arg1);
    }

    long meat = arg1.intValue();
    if (meat <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "closet";
      String prefix = "take";
      String params = meat + " meat";
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      ClosetRequest request = new ClosetRequest(ClosetRequestType.MEAT_TO_INVENTORY, meat);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value take_shop(ScriptRuntime controller, final Value itemValue) {
    int itemId = (int) itemValue.intValue();

    if (controller.getBatched() != null) {
      String cmd = "shop";
      String prefix = "take";
      String params = "* \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      List<SoldItem> list = StoreManager.getSoldItemList();
      SoldItem soldItem = new SoldItem(itemId, 0, 0, 0, 0);
      int index = list.indexOf(soldItem);

      if (index < 0) {
        return RuntimeLibrary.continueValue();
      }

      soldItem = list.get(index);

      int count = soldItem.getQuantity();

      ManageStoreRequest request = new ManageStoreRequest(itemId, count);
      RequestThread.postRequest(request);
    }
    return RuntimeLibrary.continueValue();
  }

  public static Value take_shop(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "shop";
      String prefix = "take";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      ManageStoreRequest request = new ManageStoreRequest(itemId, count);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value take_storage(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "hagnk";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, null, params);
    } else {
      StorageRequest request =
          new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, ItemPool.get(itemId, count));
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value take_display(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "display";
      String prefix = "take";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = ItemPool.get(itemId, count);
      DisplayCaseRequest request = new DisplayCaseRequest(items, false);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value take_stash(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "stash";
      String prefix = "take";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, prefix, params);
    } else {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = ItemPool.get(itemId, count);
      ClanStashRequest request = new ClanStashRequest(items, ClanStashRequestType.STASH_TO_ITEMS);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value autosell(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    if (controller.getBatched() != null) {
      String cmd = "sell";
      String params = count + " \u00B6" + itemId;
      RuntimeLibrary.batchCommand(controller, cmd, null, params);
    } else {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = ItemPool.get(itemId, count);
      AutoSellRequest request = new AutoSellRequest(items);
      RequestThread.postRequest(request);
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value hermit(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int itemId = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
        "hermit", count + " " + ItemDatabase.getItemName(itemId));
    return RuntimeLibrary.continueValue();
  }

  public static Value retrieve_item(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int item = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    return DataTypes.makeBooleanValue(InventoryManager.retrieveItem(ItemPool.get(item, count)));
  }

  public static Value retrieve_item(ScriptRuntime controller, final Value item) {
    return retrieve_item(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value retrieve_price(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenItem =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenItem ? arg1Value : arg2Value;
    int item = countThenItem ? arg2Value : arg1Value;

    if (count <= 0) {
      return DataTypes.ZERO_VALUE;
    }

    boolean exact = arg3.intValue() == 1;

    return new Value(InventoryManager.priceToAcquire(ItemPool.get(item, count), exact, false));
  }

  public static Value retrieve_price(ScriptRuntime controller, final Value arg1, final Value arg2) {
    return retrieve_price(controller, arg1, arg2, DataTypes.FALSE_VALUE);
  }

  public static Value retrieve_price(ScriptRuntime controller, final Value item) {
    return retrieve_price(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value receive_fax(ScriptRuntime controller) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("fax", "receive");
    return RuntimeLibrary.continueValue();
  }

  public static Value send_fax(ScriptRuntime controller) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("fax", "send");
    return RuntimeLibrary.continueValue();
  }

  public static Value faxbot(
      ScriptRuntime controller, final Value monsterName, final Value botName) {
    MonsterData monster = (MonsterData) monsterName.rawValue();

    if (monster == null) {
      return DataTypes.FALSE_VALUE;
    }

    FaxBotDatabase.configure();

    FaxBot bot = FaxBotDatabase.getFaxbot(botName.toString());

    if (bot == null) {
      return DataTypes.FALSE_VALUE;
    }

    return DataTypes.makeBooleanValue(bot.request(monster));
  }

  public static Value faxbot(ScriptRuntime controller, final Value monsterName) {
    MonsterData monster = (MonsterData) monsterName.rawValue();

    if (monster == null) {
      return DataTypes.FALSE_VALUE;
    }

    FaxBotDatabase.configure();

    for (FaxBot bot : FaxBotDatabase.getSortedFaxbots()) {

      if (bot == null) {
        continue;
      }

      boolean result = bot.request(monster);

      if (result) {
        return DataTypes.TRUE_VALUE;
      }
    }

    return DataTypes.FALSE_VALUE;
  }

  public static Value can_faxbot(ScriptRuntime controller, final Value arg1) {
    MonsterData monster = (MonsterData) arg1.rawValue();
    return can_faxbot(monster, null);
  }

  public static Value can_faxbot(ScriptRuntime controller, final Value arg1, final Value arg2) {
    MonsterData monster = (MonsterData) arg1.rawValue();
    String faxbot = arg2.toString();
    return can_faxbot(monster, faxbot);
  }

  private static Value can_faxbot(MonsterData monster, String faxbot) {
    if (monster == null) {
      return DataTypes.FALSE_VALUE;
    }

    // KoL doesn't care about the case of player names. Normalize.
    if (faxbot != null) {
      faxbot = faxbot.toLowerCase();
    }

    FaxBotDatabase.configure();

    int monsterId = monster.getId();
    for (FaxBot bot : FaxBotDatabase.faxbots) {
      if (bot == null) {
        continue;
      }

      String botName = bot.getName().toLowerCase();
      if (faxbot != null && !faxbot.equals(botName)) {
        continue;
      }

      Monster monsterObject = bot.getMonsterByMonsterId(monsterId);
      if (monsterObject == null) {
        continue;
      }

      if (KoLmafia.isPlayerOnline(botName)) {
        return DataTypes.TRUE_VALUE;
      }
    }
    return DataTypes.FALSE_VALUE;
  }

  // Major functions which provide item-related
  // information.

  public static Value get_inventory(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    AdventureResult[] items = new AdventureResult[KoLConstants.inventory.size()];
    KoLConstants.inventory.toArray(items);

    for (AdventureResult item : items) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_closet(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    AdventureResult[] items = new AdventureResult[KoLConstants.closet.size()];
    KoLConstants.closet.toArray(items);

    for (AdventureResult item : items) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_storage(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    AdventureResult[] items = new AdventureResult[KoLConstants.storage.size()];
    KoLConstants.storage.toArray(items);

    for (AdventureResult item : items) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_display(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    if (!KoLCharacter.hasDisplayCase()) {
      return value;
    }

    if (!DisplayCaseManager.collectionRetrieved) {
      RequestThread.postRequest(new DisplayCaseRequest());
    }

    for (AdventureResult item : KoLConstants.collection) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_free_pulls(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    AdventureResult[] items = new AdventureResult[KoLConstants.freepulls.size()];
    KoLConstants.freepulls.toArray(items);

    for (AdventureResult item : items) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_shop(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    if (!KoLCharacter.hasStore()) {
      return value;
    }

    if (!StoreManager.soldItemsRetrieved) {
      RequestThread.postRequest(new ManageStoreRequest());
    }

    List<SoldItem> list = StoreManager.getSoldItemList();
    for (SoldItem item : list) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getQuantity()));
    }

    return value;
  }

  public static Value get_shop_log(ScriptRuntime controller) {
    if (!KoLCharacter.hasStore()) {
      return new ArrayValue(new AggregateType(DataTypes.STRING_TYPE, 1));
    }
    RequestThread.postRequest(new ManageStoreRequest(true));
    List<StoreManager.StoreLogEntry> list = StoreManager.getStoreLog();
    AggregateValue value = new ArrayValue(new AggregateType(DataTypes.STRING_TYPE, list.size()));
    for (int i = 0; i < list.size(); i++) {
      StoreManager.StoreLogEntry sle = list.get(i);
      if (sle != null) {
        value.aset(new Value(i), new Value(sle.toString()));
      }
    }
    return value;
  }

  public static Value get_stash(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    String clanName = ClanManager.getClanName(false);
    if (clanName == null || clanName.isEmpty()) {
      return value;
    }

    List<AdventureResult> list = ClanManager.getStash();
    for (AdventureResult item : list) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_campground(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    if (KoLCharacter.inNuclearAutumn()) {
      for (AdventureResult item : KoLConstants.falloutShelter) {
        value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
      }
    } else {
      // Your dwelling is not in the list of campground items
      AdventureResult dwelling = CampgroundRequest.getCurrentDwelling();
      value.aset(DataTypes.makeItemValue(dwelling.getItemId(), true), DataTypes.ONE_VALUE);

      for (AdventureResult item : KoLConstants.campground) {
        value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
      }
    }

    return value;
  }

  public static Value get_workshed(ScriptRuntime controller) {
    AdventureResult workshed = CampgroundRequest.getCurrentWorkshedItem();
    return workshed == null
        ? DataTypes.ITEM_INIT
        : DataTypes.makeItemValue(workshed.getItemId(), true);
  }

  public static Value get_clan_lounge(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    for (AdventureResult item : ClanManager.getClanLounge()) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  private static final long FNV_INIT = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x00000100000001b3L;

  // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
  private static long fnvHash(long init, int n) {
    long hash = init;
    for (int i = 0; i < 4; i++) {
      hash ^= n & 0xFF;
      hash *= RuntimeLibrary.FNV_PRIME;
      n >>= 8;
    }
    return hash;
  }

  /**
   * Quickly get a hash of the contents of inventory, closet, storage, display, or shop. Useful for
   * relay scripts that want to poll for changes to inventory via the JSON API.
   *
   * @param controller Script runtime controller
   * @param itemsSource One of "inventory", "closet", "storage", "display", or "shop", specifying
   *     which item list to hash.
   * @return Long of hashed value.
   */
  public static Value get_items_hash(ScriptRuntime controller, Value itemsSource) {
    long hash = RuntimeLibrary.FNV_INIT;

    List<AdventureResult> itemsList =
        switch (itemsSource.toString()) {
          case "shop" -> {
            for (var soldItem : StoreManager.getSoldItemList()) {
              hash = RuntimeLibrary.fnvHash(hash, soldItem.getItemId());
              hash = RuntimeLibrary.fnvHash(hash, soldItem.getQuantity());
              hash = RuntimeLibrary.fnvHash(hash, (int) soldItem.getPrice());
              hash = RuntimeLibrary.fnvHash(hash, (int) (soldItem.getPrice() >> 32));
              hash = RuntimeLibrary.fnvHash(hash, soldItem.getLimit());
            }

            yield List.of();
          }
          case "inventory" -> KoLConstants.inventory;
          case "closet" -> KoLConstants.closet;
          case "storage" -> KoLConstants.storage;
          case "display" -> KoLConstants.collection;
          default -> {
            KoLmafia.updateDisplay(
                MafiaState.ERROR,
                "get_items_hash: Invalid items source. Valid are inventory, closet, storage, display, shop.");
            yield List.of();
          }
        };

    for (var itemCount : itemsList) {
      hash = RuntimeLibrary.fnvHash(hash, itemCount.getItemId());
      hash = RuntimeLibrary.fnvHash(hash, itemCount.getCount());
    }

    return DataTypes.makeIntValue(hash);
  }

  public static Value get_fishing_locations(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.STRING_TO_LOCATION_TYPE);

    for (var locs : ClanLoungeRequest.getFloundryLocations().entrySet()) {
      Value location = DataTypes.makeLocationValue(AdventureDatabase.getAdventure(locs.getValue()));
      value.aset(new Value(locs.getKey()), location);
    }

    return value;
  }

  public static Value get_clan_rumpus(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.STRING_TO_INT_TYPE);

    for (String name : ClanManager.getClanRumpus()) {
      int count = 1;
      int countIndex = name.indexOf(" (");
      if (countIndex != -1) {
        count = StringUtilities.parseInt(name.substring(countIndex + 2, name.length() - 1));
        name = name.substring(0, countIndex);
      }
      value.aset(new Value(name), new Value(count));
    }

    return value;
  }

  public static Value get_chateau(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    for (AdventureResult item : KoLConstants.chateau) {
      value.aset(DataTypes.makeItemValue(item.getItemId(), true), new Value(item.getCount()));
    }

    return value;
  }

  public static Value get_dwelling(ScriptRuntime controller) {
    return DataTypes.makeItemValue(CampgroundRequest.getCurrentDwelling().getItemId(), true);
  }

  public static Value my_garden_type(ScriptRuntime controller) {
    CropType crop = CampgroundRequest.getCropType();
    return new Value(crop == null ? "none" : crop.name().toLowerCase());
  }

  public static Value get_zap_wand(ScriptRuntime controller) {
    AdventureResult zapWand = KoLCharacter.findWand();

    return zapWand == null
        ? DataTypes.ITEM_INIT
        : DataTypes.makeItemValue(zapWand.getItemId(), true);
  }

  private static final int WAD2POWDER = -12; // <elem> powder - <elem> wad
  private static final int WAD2NUGGET = -6;
  private static final int WAD2GEM = 1321;

  public static Value get_related(ScriptRuntime controller, Value item, Value type) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);
    String which = type.toString();

    switch (which) {
      case "zap" -> {
        List<String> zapgroup = ZapRequest.getZapGroup((int) item.intValue());
        for (int i = zapgroup.size() - 1; i >= 0; --i) {
          Value key = DataTypes.parseItemValue(zapgroup.get(i), true);
          if (key.intValue() != item.intValue()) {
            value.aset(key, DataTypes.ZERO_VALUE);
          }
        }
      }
      case "fold" -> {
        FoldGroup list = ItemDatabase.getFoldGroup(item.toString());
        if (list == null) return value;
        for (int i = list.names.size() - 1; i >= 0; --i) {
          value.aset(DataTypes.parseItemValue(list.names.get(i), true), new Value(i + 1));
        }
      }
      case "pulverize" -> { // All values scaled up by one million
        int pulver = EquipmentDatabase.getPulverization((int) item.intValue());
        if (pulver == -1 || (pulver & EquipmentDatabase.MALUS_UPGRADE) != 0) {
          return value;
        }
        if (pulver > 0) {
          value.aset(DataTypes.makeItemValue(pulver, true), DataTypes.makeIntValue(1000000));
          return value;
        }
        ArrayList<Integer> elems = new ArrayList<>();
        boolean clusters = (pulver & EquipmentDatabase.YIELD_1C) != 0;
        if ((pulver & EquipmentDatabase.ELEM_HOT) != 0) {
          elems.add(clusters ? ItemPool.HOT_CLUSTER : ItemPool.HOT_WAD);
        }
        if ((pulver & EquipmentDatabase.ELEM_COLD) != 0) {
          elems.add(clusters ? ItemPool.COLD_CLUSTER : ItemPool.COLD_WAD);
        }
        if ((pulver & EquipmentDatabase.ELEM_STENCH) != 0) {
          elems.add(clusters ? ItemPool.STENCH_CLUSTER : ItemPool.STENCH_WAD);
        }
        if ((pulver & EquipmentDatabase.ELEM_SPOOKY) != 0) {
          elems.add(clusters ? ItemPool.SPOOKY_CLUSTER : ItemPool.SPOOKY_WAD);
        }
        if ((pulver & EquipmentDatabase.ELEM_SLEAZE) != 0) {
          elems.add(clusters ? ItemPool.SLEAZE_CLUSTER : ItemPool.SLEAZE_WAD);
        }
        if ((pulver & EquipmentDatabase.ELEM_TWINKLY) != 0) { // Important: twinkly must be last
          elems.add(ItemPool.TWINKLY_WAD);
        }
        int nelems = elems.size();
        if (nelems == 0) {
          return value; // shouldn't happen
        }
        int powders = 0, nuggets = 0, wads = 0;
        if ((pulver & EquipmentDatabase.YIELD_3W) != 0) {
          wads = 3000000;
        } else if ((pulver & EquipmentDatabase.YIELD_1W3N_2W) != 0) {
          wads = 1500000;
          nuggets = 1500000;
        } else if ((pulver & EquipmentDatabase.YIELD_4N_1W) != 0) {
          wads = 500000;
          nuggets = 2000000;
        } else if ((pulver & EquipmentDatabase.YIELD_3N) != 0) {
          nuggets = 3000000;
        } else if ((pulver & EquipmentDatabase.YIELD_1N3P_2N) != 0) {
          nuggets = 1500000;
          powders = 1500000;
        } else if ((pulver & EquipmentDatabase.YIELD_4P_1N) != 0) {
          nuggets = 500000;
          powders = 2000000;
        } else if ((pulver & EquipmentDatabase.YIELD_3P) != 0) {
          powders = 3000000;
        } else if ((pulver & EquipmentDatabase.YIELD_2P) != 0) {
          powders = 2000000;
        } else if ((pulver & EquipmentDatabase.YIELD_1P) != 0) {
          powders = 1000000;
        }
        int gems = wads / 100;
        wads -= gems;
        for (int wad : elems) {
          if (powders > 0) {
            value.aset(
                DataTypes.makeItemValue(wad + WAD2POWDER, true),
                DataTypes.makeIntValue(powders / nelems));
          }
          if (nuggets > 0) {
            value.aset(
                DataTypes.makeItemValue(wad + WAD2NUGGET, true),
                DataTypes.makeIntValue(nuggets / nelems));
          }
          if (wads > 0) {
            if (wad == ItemPool.TWINKLY_WAD) { // no twinkly gem!
              wads += gems;
              gems = 0;
            }
            value.aset(DataTypes.makeItemValue(wad, true), DataTypes.makeIntValue(wads / nelems));
          }
          if (gems > 0) {
            value.aset(
                DataTypes.makeItemValue(wad + WAD2GEM, true),
                DataTypes.makeIntValue(gems / nelems));
          }
          if (clusters) {
            value.aset(DataTypes.makeItemValue(wad, true), DataTypes.makeIntValue(1000000));
          }
        }
      }
    }
    return value;
  }

  public static Value is_tradeable(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(ItemDatabase.isTradeable((int) item.intValue()));
  }

  public static Value is_giftable(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(ItemDatabase.isGiftable((int) item.intValue()));
  }

  public static Value is_displayable(ScriptRuntime controller, final Value item) {
    int itemId = (int) item.intValue();
    return DataTypes.makeBooleanValue(
        !ItemDatabase.isQuestItem(itemId) && !ItemDatabase.isVirtualItem(itemId));
  }

  public static Value is_discardable(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(ItemDatabase.isDiscardable((int) item.intValue()));
  }

  public static Value is_npc_item(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(NPCStoreDatabase.contains((int) item.intValue(), false));
  }

  public static Value is_coinmaster_item(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(CoinmastersDatabase.contains((int) item.intValue(), false));
  }

  public static Value autosell_price(ScriptRuntime controller, final Value item) {
    return new Value(ItemDatabase.getPriceById((int) item.intValue()));
  }

  public static Value mall_price(ScriptRuntime controller, final Value item) {
    return new Value(MallPriceManager.getMallPrice((int) item.intValue()));
  }

  public static Value concoction_price(ScriptRuntime controller, final Value value) {
    Concoction concoction = null;
    if (value.getType().equals(DataTypes.ITEM_TYPE)) {
      concoction = ConcoctionPool.get((int) value.intValue());
    } else if (value.getType().equals(DataTypes.VYKEA_TYPE)) {
      VYKEACompanionData companion = (VYKEACompanionData) value.rawValue();

      if (companion == null) {
        return DataTypes.ZERO_VALUE;
      }

      concoction = ConcoctionPool.get(-1, companion.toString());
    }

    if (concoction == null) {
      return DataTypes.ZERO_VALUE;
    }

    long cost =
        Arrays.stream(concoction.getIngredients()).mapToLong(MallPriceManager::getMallPrice).sum();
    long creationCost = ConcoctionDatabase.getCreationCost(concoction.getMixingMethod());
    return new Value(cost + creationCost);
  }

  public static Value mall_price(ScriptRuntime controller, final Value item, final Value maxAge) {
    return new Value(
        MallPriceManager.getMallPrice((int) item.intValue(), (float) maxAge.floatValue()));
  }

  public static Value mall_prices(ScriptRuntime controller, final Value arg) {
    if (arg.getType().equals(DataTypes.STRING_TYPE)) {
      return new Value(MallPriceManager.getMallPrices(arg.toString(), ""));
    }

    // It's a set of items
    AggregateValue aggregate = (AggregateValue) arg;
    int size = aggregate.count();
    Value[] keys = aggregate.keys();
    AdventureResult[] itemIds = new AdventureResult[size];

    // Extract the item ids into an array
    for (int i = 0; i < size; ++i) {
      Value item = keys[i];
      itemIds[i] = ItemPool.get((int) item.contentLong, 1);
    }

    // Update the mall prices, one by one,
    // Note that the AdventureResults all have "count" of 1
    long result = MallPriceManager.getMallPrices(itemIds, 0.0f);

    return DataTypes.makeIntValue(result);
  }

  public static Value mall_prices(
      ScriptRuntime controller, final Value category, final Value tiers) {
    return new Value(MallPriceManager.getMallPrices(category.toString(), tiers.toString()));
  }

  public static Value well_stocked(
      ScriptRuntime controller, final Value itemName, final Value quantity, final Value price) {
    // extract parameters
    String item = itemName.toString();
    int itemID = ItemDatabase.getItemId(item);
    long checkQuant = quantity.intValue();
    long checkPrice = price.intValue();
    // check parameters and return false if problems
    if (item == null) return DataTypes.FALSE_VALUE;
    if (itemID < 1) return DataTypes.FALSE_VALUE;
    if (checkQuant < 6) return DataTypes.FALSE_VALUE;
    if (checkPrice < (2L * ItemDatabase.getPriceById(itemID))) return DataTypes.FALSE_VALUE;
    // get some data
    MallSearchRequest msr = new MallSearchRequest(item, 20);
    msr.run();
    // Now iterate over results
    // Assume sorted by price so can bail at first failure
    int available = 0;
    for (PurchaseRequest pr : msr.getResults()) {
      // only interested in mall
      if (pr instanceof MallPurchaseRequest) {
        // get price and bail if higher
        long storePrice = pr.getPrice();
        if (storePrice > checkPrice) return new Value(available >= checkQuant);
        // get available
        int canGet = Math.min(pr.getLimit(), pr.getQuantity());
        available += canGet;
        if (available >= checkQuant) return DataTypes.TRUE_VALUE;
      }
    }
    // if we get here we have failed to find enough stock
    return DataTypes.FALSE_VALUE;
  }

  public static Value npc_price(ScriptRuntime controller, final Value item) {
    int itemId = (int) item.intValue();
    String it = ItemDatabase.getCanonicalName(itemId);
    return new Value(
        NPCStoreDatabase.contains(itemId, true)
            ? NPCStoreDatabase.price(itemId)
            : ClanLoungeRequest.availableSpeakeasyDrink(it)
                ? ClanLoungeRequest.speakeasyNameToCost(it)
                : 0);
  }

  public static Value shop_price(ScriptRuntime controller, final Value item) {
    if (!KoLCharacter.hasStore()) {
      return DataTypes.ZERO_VALUE;
    }

    if (!StoreManager.soldItemsRetrieved) {
      RequestThread.postRequest(new ManageStoreRequest());
    }

    return new Value(StoreManager.getPrice((int) item.intValue()));
  }

  // Coinmaster functions

  public static Value buys_item(ScriptRuntime controller, final Value master, final Value item) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    return DataTypes.makeBooleanValue(data != null && data.canSellItem((int) item.intValue()));
  }

  public static Value buy_price(ScriptRuntime controller, final Value master, final Value item) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    return DataTypes.makeIntValue(data != null ? data.getSellPrice((int) item.intValue()) : 0);
  }

  public static Value sells_item(ScriptRuntime controller, final Value master, final Value item) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    return DataTypes.makeBooleanValue(data != null && data.canBuyItem((int) item.intValue()));
  }

  public static Value sells_skill(ScriptRuntime controller, final Value master, final Value skill) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    int skillId = (int) skill.intValue();
    return DataTypes.makeBooleanValue(data != null && data.skillBuyPrice(skillId) != null);
  }

  public static Value sell_cost(ScriptRuntime controller, final Value master, final Value thing) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    CoinmasterData data = (CoinmasterData) master.rawValue();

    if (data.getShopRows() != null) {
      int id = (int) thing.intValue();
      var shopRow = data.getShopRow(id);

      if (shopRow == null) return value;

      for (var cost : shopRow.getCosts()) {
        value.aset(DataTypes.makeItemValue(cost.getItemId(), true), new Value(cost.getCount()));
      }

      return value;
    }

    AdventureResult cost = sell_price(data, thing);
    if (cost == null) return value;
    value.aset(DataTypes.makeItemValue(cost), DataTypes.makeIntValue(cost.getCount()));

    return value;
  }

  public static Value sell_price(ScriptRuntime controller, final Value master, final Value thing) {
    CoinmasterData data = (CoinmasterData) master.rawValue();
    AdventureResult value = sell_price(data, thing);
    return DataTypes.makeIntValue(value == null ? 0 : value.getCount());
  }

  private static AdventureResult sell_price(CoinmasterData data, Value thing) {
    int id = (int) thing.intValue();
    return (data == null)
        ? null
        : switch (thing.getType().getType()) {
          case TypeSpec.ITEM -> data.itemBuyPrice(id);
          case TypeSpec.SKILL -> data.skillBuyPrice(id);
          default -> null;
        };
  }

  public static Value historical_price(ScriptRuntime controller, final Value item) {
    return new Value(MallPriceDatabase.getPrice((int) item.intValue()));
  }

  public static Value historical_age(ScriptRuntime controller, final Value item) {
    return new Value(MallPriceDatabase.getAge((int) item.intValue()));
  }

  public static Value daily_special(ScriptRuntime controller) {
    AdventureResult special =
        KoLCharacter.gnomadsAvailable()
            ? MicroBreweryRequest.getDailySpecial()
            : KoLCharacter.canadiaAvailable() ? ChezSnooteeRequest.getDailySpecial() : null;

    return special == null
        ? DataTypes.ITEM_INIT
        : DataTypes.makeItemValue(special.getItemId(), true);
  }

  public static Value refresh_shop(ScriptRuntime controller) {
    RequestThread.postRequest(new ManageStoreRequest());
    return RuntimeLibrary.continueValue();
  }

  public static Value refresh_stash(ScriptRuntime controller) {
    RequestThread.postRequest(new ClanStashRequest());
    return RuntimeLibrary.continueValue();
  }

  public static Value available_amount(ScriptRuntime controller, final Value arg) {
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return DataTypes.makeIntValue(InventoryManager.getAccessibleCount(item));
  }

  public static Value item_amount(ScriptRuntime controller, final Value arg) {
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return new Value(item.getCount(KoLConstants.inventory));
  }

  public static Value closet_amount(ScriptRuntime controller, final Value arg) {
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return new Value(item.getCount(KoLConstants.closet));
  }

  public static Value equipped_amount(ScriptRuntime controller, final Value arg) {
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    int amount = InventoryManager.getEquippedCount(item);

    return new Value(amount);
  }

  public static Value equipped_amount(
      ScriptRuntime controller, final Value arg0, final Value arg1) {
    AdventureResult item = ItemPool.get((int) arg0.intValue(), 0);
    boolean includeAllFamiliars = arg1.intValue() == 1;
    int amount = InventoryManager.getEquippedCount(item, includeAllFamiliars);

    return new Value(amount);
  }

  public static Value creatable_amount(ScriptRuntime controller, final Value arg) {
    CreateItemRequest item = CreateItemRequest.getInstance((int) arg.intValue());
    return new Value(item == null ? 0 : item.getQuantityPossible());
  }

  public static Value creatable_turns(ScriptRuntime controller, final Value itemId) {
    AdventureResult item = ItemPool.get((int) itemId.intValue());
    int initialAmount = item.getCount(KoLConstants.inventory);
    Concoction concoction = ConcoctionPool.get(item);
    return new Value(concoction == null ? 0 : concoction.getAdventuresNeeded(initialAmount + 1));
  }

  public static Value creatable_turns(
      ScriptRuntime controller, final Value itemId, final Value count) {
    AdventureResult item = ItemPool.get((int) itemId.intValue());
    int number = (int) count.intValue();
    int initialAmount = item.getCount(KoLConstants.inventory);
    Concoction concoction = ConcoctionPool.get(item);
    return new Value(
        concoction == null ? 0 : concoction.getAdventuresNeeded(initialAmount + number));
  }

  public static Value creatable_turns(
      ScriptRuntime controller, final Value itemId, final Value count, final Value freeCrafting) {
    AdventureResult item = ItemPool.get((int) itemId.intValue());
    int number = (int) count.intValue();
    boolean considerFreeCrafting = freeCrafting.intValue() == 1;
    int initialAmount = item.getCount(KoLConstants.inventory);
    Concoction concoction = ConcoctionPool.get(item);
    return new Value(
        concoction == null
            ? 0
            : concoction.getAdventuresNeeded(initialAmount + number, considerFreeCrafting));
  }

  public static Value get_ingredients(ScriptRuntime controller, final Value arg) {
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    int itemId = (int) arg.intValue();
    CraftingType method = ConcoctionDatabase.getMixingMethod(itemId);
    EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements(itemId);
    if (!ConcoctionDatabase.isPermittedMethod(method, requirements)) {
      return value; // can't make it
    }

    AdventureResult[] data = ConcoctionDatabase.getIngredients(itemId);
    for (AdventureResult ingredient : data) {
      if (ingredient.getItemId() < 0) {
        // Skip pseudo-ingredients: coinmaster tokens
        continue;
      }
      int count = ingredient.getCount();
      Value key = DataTypes.makeItemValue(ingredient.getItemId(), true);
      if (value.contains(key)) {
        count += (int) value.aref(key).intValue();
      }
      value.aset(key, new Value(count));
    }

    return value;
  }

  public static Value storage_amount(ScriptRuntime controller, final Value arg) {
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return new Value(item.getCount(KoLConstants.storage) + item.getCount(KoLConstants.freepulls));
  }

  public static Value display_amount(ScriptRuntime controller, final Value arg) {
    if (!KoLCharacter.hasDisplayCase()) {
      return DataTypes.ZERO_VALUE;
    }

    if (!DisplayCaseManager.collectionRetrieved) {
      RequestThread.postRequest(new DisplayCaseRequest());
    }

    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return new Value(item.getCount(KoLConstants.collection));
  }

  private static SoldItem getSoldItem(int itemId) {
    if (!KoLCharacter.hasStore()) {
      return null;
    }

    if (!StoreManager.soldItemsRetrieved) {
      RequestThread.postRequest(new ManageStoreRequest());
    }

    SoldItem item = new SoldItem(itemId, 0, 0, 0, 0);

    List<SoldItem> list = StoreManager.getSoldItemList();
    int index = list.indexOf(item);
    if (index < 0) {
      return null;
    }

    return list.get(index);
  }

  public static Value shop_amount(ScriptRuntime controller, final Value arg) {
    SoldItem item = getSoldItem((int) arg.intValue());

    if (item == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(item.getQuantity());
  }

  public static Value shop_limit(ScriptRuntime controller, final Value arg) {
    SoldItem item = getSoldItem((int) arg.intValue());

    if (item == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(item.getLimit());
  }

  public static Value stash_amount(ScriptRuntime controller, final Value arg) {
    if (!ClanManager.stashRetrieved) {
      RequestThread.postRequest(new ClanStashRequest());
    }

    List<AdventureResult> stash = ClanManager.getStash();
    AdventureResult item = ItemPool.get((int) arg.intValue(), 0);
    return new Value(item.getCount(stash));
  }

  public static Value pulls_remaining(ScriptRuntime controller) {
    return new Value(ConcoctionDatabase.getPullsRemaining());
  }

  public static Value stills_available(ScriptRuntime controller) {
    return new Value(KoLCharacter.getStillsAvailable());
  }

  public static Value have_mushroom_plot(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(MushroomManager.ownsPlot());
  }

  public static Value craft_type(ScriptRuntime controller, final Value arg) {
    int itemId = (int) arg.intValue();
    Concoction conc = ConcoctionPool.get(itemId);
    if (conc == null) {
      return new Value("none");
    }
    CraftingType method = conc.getMixingMethod();
    EnumSet<CraftingRequirements> requirements = conc.getRequirements();
    return new Value(ConcoctionDatabase.mixingMethodDescription(method, requirements));
  }

  // The following functions pertain to providing updated
  // information relating to the player.

  public static Value refresh_status(ScriptRuntime controller) {
    ApiRequest.updateStatus();
    return RuntimeLibrary.continueValue();
  }

  public static Value restore_hp(ScriptRuntime controller, final Value amount) {
    return RuntimeLibrary.restore(true, (int) amount.intValue());
  }

  public static Value restore_mp(ScriptRuntime controller, final Value amount) {
    return RuntimeLibrary.restore(false, (int) amount.intValue());
  }

  private static Value restore(boolean hp, int amount) {
    boolean wasRecoveryActive = RecoveryManager.isRecoveryActive();
    try {
      RecoveryManager.setRecoveryActive(true);
      return DataTypes.makeBooleanValue(
          hp
              ? RecoveryManager.checkpointedRecoverHP(amount)
              : RecoveryManager.checkpointedRecoverMP(amount));
    } finally {
      RecoveryManager.setRecoveryActive(wasRecoveryActive);
    }
  }

  public static Value mood_execute(ScriptRuntime controller, final Value multiplicity) {
    if (RecoveryManager.isRecoveryActive() || MoodManager.isExecuting()) {
      return DataTypes.VOID_VALUE;
    }

    MoodManager.checkpointedExecute((int) multiplicity.intValue());
    return DataTypes.VOID_VALUE;
  }

  public static Value my_name(ScriptRuntime controller) {
    return new Value(KoLCharacter.getUserName());
  }

  public static Value my_id(ScriptRuntime controller) {
    return new Value(KoLCharacter.getPlayerId());
  }

  public static Value my_hash(ScriptRuntime controller) {
    return new Value(GenericRequest.passwordHash);
  }

  public static Value my_sign(ScriptRuntime controller) {
    return new Value(KoLCharacter.getSign().getName());
  }

  public static Value my_path(ScriptRuntime controller) {
    return DataTypes.makePathValue(KoLCharacter.getPath());
  }

  public static Value my_path_id(ScriptRuntime controller) {
    return new Value(KoLCharacter.getPath().getId());
  }

  public static Value in_muscle_sign(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.inMuscleSign());
  }

  public static Value in_mysticality_sign(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.inMysticalitySign());
  }

  public static Value in_moxie_sign(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.inMoxieSign());
  }

  public static Value in_bad_moon(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.inBadMoon());
  }

  public static Value my_class(ScriptRuntime controller) {
    return DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), false);
  }

  public static Value my_level(ScriptRuntime controller) {
    return new Value(KoLCharacter.getLevel());
  }

  public static Value my_hp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentHP());
  }

  public static Value my_maxhp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMaximumHP());
  }

  public static Value my_mp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentMP());
  }

  public static Value my_maxmp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMaximumMP());
  }

  public static Value my_pp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentPP());
  }

  public static Value my_maxpp(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMaximumPP());
  }

  public static Value my_robot_energy(ScriptRuntime controller) {
    return new Value(KoLCharacter.getYouRobotEnergy());
  }

  public static Value my_robot_scraps(ScriptRuntime controller) {
    return new Value(KoLCharacter.getYouRobotScraps());
  }

  public static Value my_wildfire_water(ScriptRuntime controller) {
    return new Value(KoLCharacter.getWildfireWater());
  }

  public static Value my_primestat(ScriptRuntime controller) {
    int primeIndex = KoLCharacter.getPrimeIndex();
    return primeIndex == 0
        ? DataTypes.MUSCLE_VALUE
        : primeIndex == 1 ? DataTypes.MYSTICALITY_VALUE : DataTypes.MOXIE_VALUE;
  }

  public static Value my_basestat(ScriptRuntime controller, final Value arg) {
    String stat = arg.toString();

    if (stat.equalsIgnoreCase(Stat.MUSCLE.toString())) {
      return new Value(KoLCharacter.getBaseMuscle());
    }
    if (stat.equalsIgnoreCase(Stat.MYSTICALITY.toString())) {
      return new Value(KoLCharacter.getBaseMysticality());
    }
    if (stat.equalsIgnoreCase(Stat.MOXIE.toString())) {
      return new Value(KoLCharacter.getBaseMoxie());
    }

    if (stat.equalsIgnoreCase(Stat.SUBMUSCLE.toString())) {
      return new Value(KoLCharacter.getTotalMuscle());
    }
    if (stat.equalsIgnoreCase(Stat.SUBMYST.toString())) {
      return new Value(KoLCharacter.getTotalMysticality());
    }
    if (stat.equalsIgnoreCase(Stat.SUBMOXIE.toString())) {
      return new Value(KoLCharacter.getTotalMoxie());
    }

    return DataTypes.ZERO_VALUE;
  }

  public static Value my_buffedstat(ScriptRuntime controller, final Value arg) {
    String stat = arg.toString();

    if (stat.equalsIgnoreCase(Stat.MUSCLE.toString())) {
      return new Value(KoLCharacter.getAdjustedMuscle());
    }
    if (stat.equalsIgnoreCase(Stat.MYSTICALITY.toString())) {
      return new Value(KoLCharacter.getAdjustedMysticality());
    }
    if (stat.equalsIgnoreCase(Stat.MOXIE.toString())) {
      return new Value(KoLCharacter.getAdjustedMoxie());
    }

    return DataTypes.ZERO_VALUE;
  }

  public static Value my_fury(ScriptRuntime controller) {
    return new Value(KoLCharacter.getFury());
  }

  public static Value my_maxfury(ScriptRuntime controller) {
    return new Value(KoLCharacter.getFuryLimit());
  }

  public static Value my_soulsauce(ScriptRuntime controller) {
    return new Value(KoLCharacter.getSoulsauce());
  }

  public static Value my_discomomentum(ScriptRuntime controller) {
    return new Value(KoLCharacter.getDiscoMomentum());
  }

  public static Value my_audience(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAudience());
  }

  public static Value my_absorbs(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAbsorbs());
  }

  public static Value my_thunder(ScriptRuntime controller) {
    return new Value(KoLCharacter.getThunder());
  }

  public static Value my_rain(ScriptRuntime controller) {
    return new Value(KoLCharacter.getRain());
  }

  public static Value my_lightning(ScriptRuntime controller) {
    return new Value(KoLCharacter.getLightning());
  }

  public static Value my_mask(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMask());
  }

  public static Value my_paradoxicity(ScriptRuntime controller) {
    return new Value(KoLCharacter.getParadoxicity());
  }

  public static Value my_meat(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAvailableMeat());
  }

  public static Value my_closet_meat(ScriptRuntime controller) {
    return new Value(KoLCharacter.getClosetMeat());
  }

  public static Value my_storage_meat(ScriptRuntime controller) {
    return new Value(KoLCharacter.getStorageMeat());
  }

  public static Value my_session_meat(ScriptRuntime controller) {
    return new Value(KoLCharacter.getSessionMeat());
  }

  public static Value my_adventures(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAdventuresLeft());
  }

  public static Value my_session_adv(ScriptRuntime controller) {
    int adv = 0;
    for (AdventureResult result : KoLConstants.tally) {
      if (result.getConditionType().equals("choiceadv")) {
        adv = result.getCount();
      }
    }
    return new Value(adv);
  }

  public static Value my_session_items(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.INT_TYPE, DataTypes.ITEM_TYPE);
    MapValue value = new MapValue(type);

    for (AdventureResult result : KoLConstants.tally) {
      if (result.isItem()) {
        value.aset(DataTypes.makeItemValue(result), new Value(result.getCount()));
      }
    }

    return value;
  }

  public static Value my_session_items(ScriptRuntime controller, Value item) {
    for (AdventureResult result : KoLConstants.tally) {
      if (result.getItemId() == item.intValue()) {
        return new Value(result.getCount());
      }
    }

    return DataTypes.ZERO_VALUE;
  }

  public static Value my_session_results(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.INT_TYPE, DataTypes.ITEM_TYPE);
    MapValue value = new MapValue(type);

    for (AdventureResult result : KoLConstants.tally) {
      value.aset(DataTypes.makeStringValue(result.toString()), new Value(result.getCount()));
    }

    return value;
  }

  public static Value daycount(ScriptRuntime controller) {
    return new Value(KoLCharacter.getGlobalDays());
  }

  public static Value my_daycount(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentDays());
  }

  public static Value my_turncount(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentRun());
  }

  public static Value my_total_turns_spent(ScriptRuntime controller) {
    return new Value(AdventureSpentDatabase.getTotalTrackedTurns());
  }

  public static Value my_fullness(ScriptRuntime controller) {
    return new Value(KoLCharacter.getFullness());
  }

  public static Value fullness_limit(ScriptRuntime controller) {
    return new Value(KoLCharacter.getStomachCapacity());
  }

  public static Value my_inebriety(ScriptRuntime controller) {
    return new Value(KoLCharacter.getInebriety());
  }

  public static Value inebriety_limit(ScriptRuntime controller) {
    return new Value(KoLCharacter.getLiverCapacity());
  }

  public static Value my_spleen_use(ScriptRuntime controller) {
    return new Value(KoLCharacter.getSpleenUse());
  }

  public static Value spleen_limit(ScriptRuntime controller) {
    return new Value(KoLCharacter.getSpleenLimit());
  }

  public static Value can_eat(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.canEat());
  }

  public static Value can_drink(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.canDrink());
  }

  public static Value turns_played(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCurrentRun());
  }

  public static Value total_turns_played(ScriptRuntime controller) {
    return new Value(KoLCharacter.getTurnsPlayed());
  }

  public static Value my_ascensions(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAscensions());
  }

  public static Value can_interact(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.canInteract());
  }

  public static Value in_hardcore(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.isHardcore());
  }

  public static Value in_casual(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.isCasual());
  }

  public static Value pvp_attacks_left(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAttacksLeft());
  }

  public static Value current_pvp_stances(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.INT_TYPE, DataTypes.STRING_TYPE);
    MapValue value = new MapValue(type);

    if (PvpManager.checkStances()) {
      for (Entry<String, Integer> entry : PvpManager.stanceToOption.entrySet()) {
        value.aset(new Value(entry.getKey()), new Value(entry.getValue()));
      }
    }

    return value;
  }

  public static Value get_clan_id(ScriptRuntime controller) {
    return new Value(ClanManager.getClanId());
  }

  public static Value get_clan_name(ScriptRuntime controller) {
    return new Value(ClanManager.getClanName(true));
  }

  public static Value limit_mode(ScriptRuntime controller) {
    return new Value(KoLCharacter.getLimitMode().getName());
  }

  public static Value get_florist_plants(ScriptRuntime controller) {
    AggregateType plantType = new AggregateType(DataTypes.STRING_TYPE, 3);

    AggregateType type = new AggregateType(plantType, DataTypes.LOCATION_TYPE);
    MapValue value = new MapValue(type);

    for (String loc : FloristRequest.floristPlants.keySet()) {
      KoLAdventure adventure = AdventureDatabase.getAdventureByName(loc);
      if (adventure == null) {
        // The location string from KoL couldn't be
        // matched to a current location
        continue;
      }

      List<Florist> plants = FloristRequest.getPlants(loc);
      if (plants.size() == 0) {
        continue;
      }

      ArrayValue plantValue = new ArrayValue(plantType);
      for (int i = 0; i < plants.size(); i++) {
        plantValue.aset(new Value(i), new Value(plants.get(i).toString()));
      }

      Value location = DataTypes.makeLocationValue(adventure);
      value.aset(location, plantValue);
    }

    return value;
  }

  public static Value total_free_rests(ScriptRuntime controller) {
    return new Value(KoLCharacter.freeRestsAvailable());
  }

  public static Value get_ignore_zone_warnings(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.getIgnoreZoneWarnings());
  }

  // Basic skill and effect functions, including those used
  // in custom combat consult scripts.

  public static Value have_skill(ScriptRuntime controller, final Value arg) {
    int skillId = (int) arg.intValue();
    return DataTypes.makeBooleanValue(
        KoLCharacter.hasSkill(skillId) || KoLCharacter.hasCombatSkill(skillId));
  }

  public static Value combat_skill_available(ScriptRuntime controller, final Value arg) {
    int skillId = (int) arg.intValue();
    return DataTypes.makeBooleanValue(KoLCharacter.hasCombatSkill(skillId));
  }

  public static Value my_ram(ScriptRuntime controller) {
    return DataTypes.makeIntValue(FightRequest.getCurrentRAM());
  }

  public static Value mp_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getMPConsumptionById((int) skill.intValue()));
  }

  public static Value adv_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getAdventureCost((int) skill.intValue()));
  }

  public static Value soulsauce_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getSoulsauceCost((int) skill.intValue()));
  }

  public static Value thunder_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getThunderCost((int) skill.intValue()));
  }

  public static Value rain_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getRainCost((int) skill.intValue()));
  }

  public static Value lightning_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getLightningCost((int) skill.intValue()));
  }

  public static Value fuel_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getFuelCost((int) skill.intValue()));
  }

  public static Value hp_cost(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getHPCost((int) skill.intValue()));
  }

  public static Value turns_per_cast(ScriptRuntime controller, final Value skill) {
    return new Value(SkillDatabase.getEffectDuration((int) skill.intValue()));
  }

  public static Value have_effect(ScriptRuntime controller, final Value arg) {
    if (arg == DataTypes.EFFECT_INIT) {
      return DataTypes.ZERO_VALUE;
    }
    int effectId = (int) arg.intValue();
    AdventureResult effect = EffectPool.get(effectId, 0);
    return new Value(effect.getCount(KoLConstants.activeEffects));
  }

  public static Value my_effects(ScriptRuntime controller) {
    AdventureResult[] effectsArray = new AdventureResult[KoLConstants.activeEffects.size()];
    KoLConstants.activeEffects.toArray(effectsArray);

    AggregateType type = new AggregateType(DataTypes.INT_TYPE, DataTypes.EFFECT_TYPE);
    MapValue value = new MapValue(type);

    for (AdventureResult effect : effectsArray) {
      int duration = effect.getCount();
      if (duration == Integer.MAX_VALUE) {
        duration = -1;
      }

      value.aset(DataTypes.makeEffectValue(effect.getEffectId(), true), new Value(duration));
    }

    return value;
  }

  public static Value is_shruggable(ScriptRuntime controller, final Value arg) {
    int effectId = (int) arg.intValue();
    return new Value(UneffectRequest.isShruggable(effectId));
  }

  public static Value is_removable(ScriptRuntime controller, final Value arg) {
    int effectId = (int) arg.intValue();
    return new Value(UneffectRequest.isRemovable(effectId));
  }

  public static Value use_skill(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenSkill =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenSkill ? arg1Value : arg2Value;
    int skillId = countThenSkill ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    // Just in case someone assumed that use_skill would also work
    // in combat, go ahead and allow it here.

    if (SkillDatabase.isCombat(skillId)) {
      // If we are in combat, go ahead and cast using fight.php

      if (FightRequest.getCurrentRound() > 0) {
        for (int i = 0; i < count; ++i) {
          RuntimeLibrary.use_skill(controller, countThenSkill ? arg2 : arg1);
        }

        return DataTypes.TRUE_VALUE;
      }

      // If we are not in combat, bail if the skill can't be cast

      if (!SkillDatabase.isNonCombat(skillId)) {
        return DataTypes.FALSE_VALUE;
      }
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
        "cast", count + " " + SkillDatabase.getSkillName(skillId));
    return UseSkillRequest.lastUpdate.equals("")
        ? RuntimeLibrary.continueValue()
        : DataTypes.FALSE_VALUE;
  }

  public static Value use_skill(ScriptRuntime controller, final Value skill) {
    int skillId = (int) skill.intValue();

    // Just in case someone assumed that use_skill would also work
    // in combat, go ahead and allow it here.

    if (SkillDatabase.isCombat(skillId)) {
      // If we are in combat, go ahead and cast using fight.php

      if (FightRequest.getCurrentRound() > 0) {
        return RuntimeLibrary.visit_url(controller, "fight.php?action=skill&whichskill=" + skillId);
      }

      // If we are not in combat, bail if the skill can't be cast

      if (!SkillDatabase.isNonCombat(skillId)) {
        return DataTypes.STRING_INIT;
      }
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("cast", "1 " + SkillDatabase.getSkillName(skillId));
    return new Value(UseSkillRequest.lastUpdate);
  }

  public static Value use_skill(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value target) {
    int arg1Value = (int) arg1.intValue();
    int arg2Value = (int) arg2.intValue();

    boolean countThenSkill =
        arg1.getType().equals(DataTypes.INT_TYPE) || arg1.getType().equals(DataTypes.FLOAT_TYPE);

    int count = countThenSkill ? arg1Value : arg2Value;
    int skillId = countThenSkill ? arg2Value : arg1Value;

    if (count <= 0) {
      return RuntimeLibrary.continueValue();
    }

    // Just in case someone assumed that use_skill would also work
    // in combat, go ahead and allow it here.

    if (SkillDatabase.isCombat(skillId)) {
      // If we are in combat, go ahead and cast using fight.php

      if (FightRequest.getCurrentRound() > 0) {
        for (int i = 0; i < count; ++i) {
          RuntimeLibrary.use_skill(controller, countThenSkill ? arg2 : arg1);
        }

        return DataTypes.TRUE_VALUE;
      }

      // If we are not in combat, bail if the skill can't be cast

      if (!SkillDatabase.isNonCombat(skillId)) {
        return DataTypes.FALSE_VALUE;
      }
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
        "cast", count + " " + SkillDatabase.getSkillName(skillId) + " on " + target);
    return UseSkillRequest.lastUpdate.equals("")
        ? RuntimeLibrary.continueValue()
        : DataTypes.FALSE_VALUE;
  }

  public static Value last_skill_message(ScriptRuntime controller) {
    return new Value(UseSkillRequest.lastUpdate);
  }

  public static Value get_auto_attack(ScriptRuntime controller) {
    return new Value(KoLCharacter.getAutoAttackAction());
  }

  public static Value set_auto_attack(ScriptRuntime controller, Value attackValue) {
    Type type = attackValue.getType();
    String arg =
        type.equals(TypeSpec.STRING)
            ? attackValue.toString()
            : type.equals(TypeSpec.INT) ? String.valueOf(attackValue.intValue()) : "none";

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("autoattack", arg);

    return DataTypes.VOID_VALUE;
  }

  public static Value attack(ScriptRuntime controller) {
    return RuntimeLibrary.visit_url(controller, "fight.php?action=attack");
  }

  public static Value twiddle(ScriptRuntime controller) {
    return RuntimeLibrary.visit_url(controller, "fight.php?action=twiddle");
  }

  public static Value steal(ScriptRuntime controller) {
    return RuntimeLibrary.visit_url(controller, "fight.php?action=steal");
  }

  public static Value runaway(ScriptRuntime controller) {
    return RuntimeLibrary.visit_url(controller, "fight.php?action=runaway");
  }

  public static Value throw_item(ScriptRuntime controller, final Value item) {
    return RuntimeLibrary.visit_url(
        controller, "fight.php?action=useitem&whichitem=" + (int) item.intValue());
  }

  public static Value throw_items(ScriptRuntime controller, final Value item1, final Value item2) {
    return RuntimeLibrary.visit_url(
        controller,
        "fight.php?action=useitem&whichitem="
            + (int) item1.intValue()
            + "&whichitem2="
            + (int) item2.intValue());
  }

  public static Value run_choice(ScriptRuntime controller, final Value decision) {
    return run_choice(controller, decision, DataTypes.TRUE_VALUE, DataTypes.STRING_INIT);
  }

  public static Value run_choice(
      ScriptRuntime controller, final Value decision, final Value extra) {
    if (extra.getType().equals(TypeSpec.BOOLEAN)) {
      // if extra is a boolean, it specifies whether to automate fights
      return run_choice(controller, decision, extra, DataTypes.STRING_INIT);
    }

    // Otherwise it must be a string and it specifies additional form fields
    return run_choice(controller, decision, DataTypes.TRUE_VALUE, extra);
  }

  public static Value run_choice(
      ScriptRuntime controller, final Value decision, final Value custom, final Value more) {
    int option = (int) decision.intValue();
    boolean handleFights = custom.intValue() != 0;
    String extraFields = more.toString();

    String response = null;

    // If we have finished a fight which is followed by a choice
    // but are not yet handling it, visit choice.php to load it up.
    if (FightRequest.choiceFollowsFight) {
      RuntimeLibrary.visit_url(controller, "choice.php", true, false);
    }

    if (!ChoiceManager.handlingChoice || ChoiceManager.lastResponseText == null || option == 0) {
      // If you are not in a choice, or you send 0, just return the last response
      response = ChoiceManager.lastResponseText;
    } else if (option == -1) {
      // Try to automate using existing settings
      response = ChoiceManager.gotoGoal();
    } else if (option > 0) {
      // Submit the option chosen
      String message =
          "Submitting option " + option + " for choice " + ChoiceManager.getLastChoice();
      RequestLogger.printLine(message);

      // If want to handle fights via CCS, use ChoiceManager.CHOICE_HANDLER
      if (handleFights) {
        response = ChoiceManager.processChoiceAdventure(option, extraFields, false);
      }
      // Otherwise, submit it in a new GenericRequest
      else {
        GenericRequest request = new GenericRequest("choice.php");
        request.addFormField("whichchoice", String.valueOf(ChoiceManager.lastChoice));
        request.addFormField("option", String.valueOf(decision));
        if (!extraFields.equals("")) {
          String[] fields = extraFields.split("&");
          for (String field : fields) {
            int equals = field.indexOf("=");
            if (equals == -1) {
              request.addFormField(field);
            } else {
              request.addFormField(field.substring(0, equals), field.substring(equals + 1));
            }
          }
        }
        request.addFormField("pwd", GenericRequest.passwordHash);
        request.run();
        response = request.responseText;
      }
    }
    return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer(response == null ? "" : response));
  }

  public static Value last_choice(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ChoiceManager.lastChoice);
  }

  public static Value last_decision(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ChoiceManager.lastDecision);
  }

  public static Value available_choice_options(ScriptRuntime controller) {
    return RuntimeLibrary.available_choice_options(false);
  }

  public static Value available_choice_options(ScriptRuntime controller, Value spoilers) {
    return RuntimeLibrary.available_choice_options(spoilers.intValue() == 1);
  }

  private static Value available_choice_options(boolean spoilers) {
    MapValue value = new MapValue(DataTypes.INT_TO_STRING_TYPE);

    String responseText = ChoiceManager.lastResponseText;
    if (responseText != null && !responseText.equals("")) {
      Map<Integer, String> choices =
          spoilers
              ? ChoiceUtilities.parseChoicesWithSpoilers(responseText)
              : ChoiceUtilities.parseChoices(responseText);

      for (Entry<Integer, String> entry : choices.entrySet()) {
        value.aset(DataTypes.makeIntValue(entry.getKey()), new Value(entry.getValue()));
      }
    }

    return value;
  }

  public static Value available_choice_select_inputs(ScriptRuntime controller, Value decision) {
    MapValue value = new MapValue(RuntimeLibrary.SelectMapType);
    String responseText = ChoiceManager.lastResponseText;
    if (responseText != null && !responseText.equals("")) {
      Map<Integer, Map<String, Map<String, String>>> selectForms =
          ChoiceUtilities.parseSelectInputsWithTags(responseText);
      Map<String, Map<String, String>> selects = selectForms.get((int) decision.intValue());
      if (selects != null) {
        for (Entry<String, Map<String, String>> entry : selects.entrySet()) {
          String name = entry.getKey();
          MapValue selectMap = new MapValue(DataTypes.STRING_TO_STRING_TYPE);
          for (Entry<String, String> mapping : entry.getValue().entrySet()) {
            selectMap.aset(new Value(mapping.getKey()), new Value(mapping.getValue()));
          }
          value.aset(new Value(name), selectMap);
        }
      }
    }
    return value;
  }

  public static Value available_choice_text_inputs(ScriptRuntime controller, Value decision) {
    MapValue value = new MapValue(DataTypes.STRING_TO_STRING_TYPE);
    String responseText = ChoiceManager.lastResponseText;
    if (responseText != null && !responseText.equals("")) {
      Map<Integer, Set<String>> textForms = ChoiceUtilities.parseTextInputs(responseText);
      Set<String> texts = textForms.get((int) decision.intValue());
      if (texts != null) {
        for (String text : texts) {
          value.aset(new Value(text), new Value(""));
        }
      }
    }
    return value;
  }

  public static Value in_multi_fight(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(FightRequest.inMultiFight);
  }

  public static Value choice_follows_fight(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(FightRequest.choiceFollowsFight);
  }

  public static Value fight_follows_choice(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(FightRequest.fightFollowsChoice);
  }

  public static Value handling_choice(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(ChoiceManager.handlingChoice);
  }

  public static Value run_combat(ScriptRuntime controller) {
    RelayRequest relayRequest = controller.getRelayRequest();

    if (FightRequest.currentRound > 0 || FightRequest.inMultiFight) {
      RequestThread.postRequest(FightRequest.INSTANCE);
    }

    return new Value(
        DataTypes.BUFFER_TYPE, "", new StringBuffer(FightRequest.lastDecoratedResponseText));
  }

  public static Value run_combat(ScriptRuntime controller, Value filterFunction) {
    if (FightRequest.currentRound == 0 && !FightRequest.inMultiFight) {
      return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer());
    }

    try {
      String filter = filterFunction.toString();
      Macrofier.setMacroOverride(filter, controller);
      RequestThread.postRequest(FightRequest.INSTANCE);
    } finally {
      Macrofier.resetMacroOverride();
    }

    if (controller.getState() == ScriptRuntime.State.EXIT) {
      return DataTypes.VOID_VALUE;
    }

    String response = FightRequest.lastResponseText;

    return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer(response == null ? "" : response));
  }

  public static Value run_turn(ScriptRuntime controller) {
    if (FightRequest.currentRound > 0 || FightRequest.inMultiFight) {
      return RuntimeLibrary.run_combat(controller);
    } else if ((ChoiceManager.handlingChoice && ChoiceManager.lastResponseText != null)
        || FightRequest.choiceFollowsFight) {
      return RuntimeLibrary.run_choice(controller, new Value(-1));
    }
    return new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer());
  }

  public static Value stun_skill(ScriptRuntime controller) {
    AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
    String stunSkill = ascensionClass == null ? "none" : ascensionClass.getStun();
    int skill = -1;

    if (!stunSkill.equals("none")) {
      skill = SkillDatabase.getSkillId(stunSkill);
    }

    return DataTypes.makeSkillValue(skill, true);
  }

  public static Value reverse_numberology(ScriptRuntime controller) {
    return reverse_numberology(controller, DataTypes.ZERO_VALUE, DataTypes.ZERO_VALUE);
  }

  public static Value reverse_numberology(
      ScriptRuntime controller, final Value advDelta, final Value spleenDelta) {
    MapValue value = new MapValue(NumberologyType);

    Map<Integer, Integer> map =
        NumberologyManager.reverseNumberology(
            (int) advDelta.intValue(), (int) spleenDelta.intValue());

    for (Map.Entry<Integer, Integer> e : map.entrySet()) {
      value.aset(new Value(e.getKey()), new Value(e.getValue()));
    }

    return value;
  }

  public static Value numberology_prize(ScriptRuntime controller, final Value num) {
    return DataTypes.makeStringValue(NumberologyManager.numberologyPrize((int) num.intValue()));
  }

  public static Value every_card_name(ScriptRuntime controller, final Value name) {
    // Use logic from CLI "play" command
    List<String> matchingNames = DeckOfEveryCardRequest.getMatchingNames(name.toString());
    // No match
    if (matchingNames.size() != 1) { // Ambiguous
      return DataTypes.STRING_INIT;
    }

    EveryCard card = DeckOfEveryCardRequest.canonicalNameToCard(matchingNames.get(0));
    return (card == null) ? DataTypes.STRING_INIT : DataTypes.makeStringValue(card.name);
  }

  public static Value heist_targets(ScriptRuntime controller) {
    if (!KoLCharacter.canUseFamiliar(FamiliarPool.CAT_BURGLAR)) {
      throw controller.runtimeException("You don't have a Cat Burglar");
    }
    FamiliarData current = KoLCharacter.getFamiliar();
    FamiliarManager.changeFamiliar(FamiliarPool.CAT_BURGLAR, false);

    MapValue returnValue = new MapValue(HeistType);
    var heistData = new HeistManager().getHeistTargets();
    for (var heistable : heistData.heistables.entrySet()) {
      var monster = heistable.getKey();
      MapValue value = new MapValue(DataTypes.INT_TO_ITEM_TYPE);
      int i = 0;
      for (var item : heistable.getValue()) {
        value.aset(DataTypes.makeIntValue(i), DataTypes.makeItemValue(item.id, false));
        i++;
      }
      returnValue.aset(DataTypes.makeMonsterValue(monster.id, false), value);
    }

    FamiliarManager.changeFamiliar(current);
    return returnValue;
  }

  public static Value heist(ScriptRuntime controller, final Value item) {
    return heist(controller, DataTypes.ONE_VALUE, item);
  }

  public static Value heist(ScriptRuntime controller, final Value num, final Value item) {
    if (!KoLCharacter.canUseFamiliar(FamiliarPool.CAT_BURGLAR)) {
      throw controller.runtimeException("You don't have a Cat Burglar");
    }
    FamiliarData current = KoLCharacter.getFamiliar();
    FamiliarManager.changeFamiliar(FamiliarPool.CAT_BURGLAR, false);

    int count = (int) num.intValue();
    int itemId = (int) item.intValue();
    var heisted = new HeistManager().heist(count, itemId);

    FamiliarManager.changeFamiliar(current);
    return DataTypes.makeBooleanValue(heisted);
  }

  // Equipment functions.

  public static Value can_equip(ScriptRuntime controller, final Value itemOrFamiliar) {
    if (itemOrFamiliar.getType().equals(DataTypes.ITEM_TYPE)) {
      int itemId = (int) itemOrFamiliar.intValue();
      switch (ItemDatabase.getConsumptionType(itemId)) {
        case HAT:
        case WEAPON:
        case OFFHAND:
        case SHIRT:
        case PANTS:
        case CONTAINER:
        case FAMILIAR_EQUIPMENT:
        case ACCESSORY:
          break;
        default:
          return DataTypes.FALSE_VALUE;
      }

      return DataTypes.makeBooleanValue(
          EquipmentManager.canEquip(ItemDatabase.getItemName(itemId)));
    } else {
      FamiliarData fam = new FamiliarData((int) itemOrFamiliar.intValue());
      return DataTypes.makeBooleanValue(fam.canEquip());
    }
  }

  public static Value can_equip(ScriptRuntime controller, final Value familiar, final Value item) {
    AdventureResult it = ItemPool.get((int) item.intValue());
    FamiliarData fam = new FamiliarData((int) familiar.intValue());
    return DataTypes.makeBooleanValue(fam.canEquip(it));
  }

  public static Value equip(ScriptRuntime controller, final Value item) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("equip", "\u00B6" + (int) item.intValue());
    return RuntimeLibrary.continueValue();
  }

  public static Value equip(ScriptRuntime controller, final Value arg1, final Value arg2) {
    boolean destinationThenItem = !arg1.getType().equals(DataTypes.ITEM_TYPE);

    Value destination = destinationThenItem ? arg1 : arg2;
    Value item = destinationThenItem ? arg2 : arg1;

    // If we're trying to equip an item to a familiar...
    if (destination.getType().equals(DataTypes.FAMILIAR_TYPE)) {
      var familiar = new FamiliarData((int) destination.intValue());
      if (KoLCharacter.getFamiliar().equals(familiar)) {
        // ...which is our current familiar, just use the "familiar" slot
        destination = new Value("familiar");
      } else {
        AdventureResult it;
        // ...otherwise check if it's "none"
        if (item.equals(DataTypes.ITEM_INIT)) {
          it = EquipmentRequest.UNEQUIP;
        } else {
          // ...or if we can equip it...
          it = ItemPool.get((int) item.intValue());

          if (!familiar.canEquip(it)) {
            return DataTypes.FALSE_VALUE;
          }
        }

        // ...and fire a request to do so.
        RequestThread.postRequest(new FamiliarRequest(familiar, it));
        return RuntimeLibrary.continueValue();
      }
    }

    var slot = destination.toString();

    if (item.equals(DataTypes.ITEM_INIT)) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("unequip", slot);
    } else {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("equip", slot + " \u00B6" + (int) item.intValue());
    }

    return RuntimeLibrary.continueValue();
  }

  public static Value equipped_item(ScriptRuntime controller, final Value slot) {
    return DataTypes.makeItemValue(
        EquipmentManager.getEquipment(Slot.byOrdinal((int) slot.intValue())));
  }

  public static Value have_equipped(ScriptRuntime controller, final Value item) {
    return DataTypes.makeBooleanValue(
        KoLCharacter.hasEquipped(ItemPool.get((int) item.intValue())));
  }

  public static Value outfit(ScriptRuntime controller, final Value outfit) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("outfit", outfit.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value have_outfit(ScriptRuntime controller, final Value outfit) {
    SpecialOutfit so = EquipmentManager.getMatchingOutfit(outfit.toString());

    if (so == null) {
      return DataTypes.FALSE_VALUE;
    }

    int id = so.getOutfitId();
    return DataTypes.makeBooleanValue(id < 0 || EquipmentManager.hasOutfit(id));
  }

  public static Value is_wearing_outfit(ScriptRuntime controller, final Value outfit) {
    SpecialOutfit so = EquipmentManager.getMatchingOutfit(outfit.toString());

    if (so == null) {
      return DataTypes.FALSE_VALUE;
    }

    return DataTypes.makeBooleanValue(EquipmentManager.isWearingOutfit(so));
  }

  public static Value outfit_pieces(ScriptRuntime controller, final Value outfit) {
    SpecialOutfit so = EquipmentManager.getMatchingOutfit(outfit.toString());
    if (so == null) {
      return new ArrayValue(new AggregateType(DataTypes.ITEM_TYPE, 0));
    }

    AdventureResult[] pieces = so.getPieces();
    AggregateType type = new AggregateType(DataTypes.ITEM_TYPE, pieces.length);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < pieces.length; ++i) {
      AdventureResult piece = pieces[i];
      value.aset(DataTypes.makeIntValue(i), DataTypes.makeItemValue(piece));
    }

    return value;
  }

  public static Value outfit_tattoo(ScriptRuntime controller, final Value outfit) {
    SpecialOutfit so = EquipmentManager.getMatchingOutfit(outfit.toString());

    if (so == null || so.getImage() == null) {
      return DataTypes.STRING_INIT;
    }

    return new Value(so.getImage());
  }

  public static Value outfit_treats(ScriptRuntime controller, final Value outfit) {
    SpecialOutfit so = EquipmentManager.getMatchingOutfit(outfit.toString());
    AggregateType type = new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.ITEM_TYPE);
    MapValue value = new MapValue(type);

    if (so == null) {
      return value;
    }

    List<SpecialOutfit.TreatChance> treats = so.getTreats();

    for (var treat : treats) {
      value.aset(DataTypes.makeItemValue(treat.treat()), DataTypes.makeFloatValue(treat.chance()));
    }

    return value;
  }

  public static Value get_outfits(ScriptRuntime controller) {
    return RuntimeLibrary.outfitListToValue(controller, EquipmentManager.getOutfits());
  }

  public static Value get_custom_outfits(ScriptRuntime controller) {
    return RuntimeLibrary.outfitListToValue(controller, EquipmentManager.getCustomOutfits());
  }

  public static Value all_normal_outfits(ScriptRuntime controller) {
    return RuntimeLibrary.outfitMapToValue(EquipmentDatabase.normalOutfits);
  }

  private static Value outfitListToValue(ScriptRuntime controller, List<SpecialOutfit> outfits) {
    AggregateValue value = new ArrayValue(new AggregateType(DataTypes.STRING_TYPE, outfits.size()));

    for (int i = 1; i < outfits.size(); ++i) {
      SpecialOutfit it = outfits.get(i);
      if (it != null) {
        value.aset(new Value(i), new Value(it.toString()));
      }
    }

    return value;
  }

  private static Value outfitMapToValue(Map<Integer, SpecialOutfit> outfits) {
    AggregateValue value =
        new MapValue(new AggregateType(DataTypes.STRING_TYPE, DataTypes.INT_TYPE));

    for (var entry : outfits.entrySet()) {
      SpecialOutfit it = entry.getValue();
      if (it != null) {
        value.aset(new Value(entry.getKey()), new Value(it.toString()));
      }
    }

    return value;
  }

  public static Value weapon_hands(ScriptRuntime controller, final Value item) {
    return new Value(EquipmentDatabase.getHands((int) item.intValue()));
  }

  public static Value item_type(ScriptRuntime controller, final Value item) {
    String type = EquipmentDatabase.getItemType((int) item.intValue());
    return new Value(type);
  }

  public static Value weapon_type(ScriptRuntime controller, final Value item) {
    Stat stat = EquipmentDatabase.getWeaponStat((int) item.intValue());
    return stat == Stat.MUSCLE
        ? DataTypes.MUSCLE_VALUE
        : stat == Stat.MYSTICALITY
            ? DataTypes.MYSTICALITY_VALUE
            : stat == Stat.MOXIE ? DataTypes.MOXIE_VALUE : DataTypes.STAT_INIT;
  }

  public static Value get_power(ScriptRuntime controller, final Value item) {
    return new Value(EquipmentDatabase.getPower((int) item.intValue()));
  }

  public static Value my_familiar(ScriptRuntime controller) {
    return DataTypes.makeFamiliarValue(KoLCharacter.getFamiliar().getId(), true);
  }

  public static Value my_effective_familiar(ScriptRuntime controller) {
    return DataTypes.makeFamiliarValue(KoLCharacter.getEffectiveFamiliar().getId(), true);
  }

  public static Value my_enthroned_familiar(ScriptRuntime controller) {
    return DataTypes.makeFamiliarValue(KoLCharacter.getEnthroned().getId(), true);
  }

  public static Value my_bjorned_familiar(ScriptRuntime controller) {
    return DataTypes.makeFamiliarValue(KoLCharacter.getBjorned().getId(), true);
  }

  public static Value my_poke_fam(ScriptRuntime controller, final Value arg) {
    int slot = (int) arg.intValue();
    return DataTypes.makeFamiliarValue(KoLCharacter.getPokeFam(slot).getId(), true);
  }

  public static Value have_familiar(ScriptRuntime controller, final Value familiar) {
    int familiarId = (int) familiar.intValue();
    return familiarId == -1
        ? DataTypes.FALSE_VALUE
        : DataTypes.makeBooleanValue(KoLCharacter.usableFamiliar(familiarId) != null);
  }

  public static Value in_terrarium(ScriptRuntime controller, final Value familiar) {
    int familiarId = (int) familiar.intValue();
    return familiarId == -1
        ? DataTypes.FALSE_VALUE
        : DataTypes.makeBooleanValue(KoLCharacter.ownedFamiliar(familiarId).isPresent());
  }

  public static Value use_familiar(ScriptRuntime controller, final Value familiar) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("familiar", familiar.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value have_servant(ScriptRuntime controller, final Value servant) {
    return DataTypes.makeBooleanValue(EdServantData.findEdServant(servant.toString()) != null);
  }

  public static Value use_servant(ScriptRuntime controller, final Value servant) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("servant", servant.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value enthrone_familiar(ScriptRuntime controller, final Value familiar) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("enthrone", familiar.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value bjornify_familiar(ScriptRuntime controller, final Value familiar) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("bjornify", familiar.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value familiar_equipment(ScriptRuntime controller, final Value familiar) {
    return DataTypes.makeItemValue(
        FamiliarDatabase.getFamiliarItemId((int) familiar.intValue()), true);
  }

  public static Value familiar_equipped_equipment(ScriptRuntime controller, final Value familiar) {
    var fam = KoLCharacter.ownedFamiliar((int) familiar.intValue());
    AdventureResult item = fam.map(FamiliarData::getItem).orElse(EquipmentRequest.UNEQUIP);
    return item == EquipmentRequest.UNEQUIP
        ? DataTypes.ITEM_INIT
        : DataTypes.makeItemValue(item.getItemId(), true);
  }

  public static Value familiar_weight(ScriptRuntime controller, final Value familiar) {
    FamiliarData fam = KoLCharacter.usableFamiliar((int) familiar.intValue());
    return fam == null ? DataTypes.ZERO_VALUE : new Value(fam.getWeight());
  }

  public static Value is_familiar_equipment_locked(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(EquipmentManager.familiarItemLocked());
  }

  public static Value favorite_familiars(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.FAMILIAR_TYPE);
    MapValue value = new MapValue(type);

    for (FamiliarData fam : KoLCharacter.usableFamiliars()) {
      if (fam.getFavorite()) {
        value.aset(
            DataTypes.makeFamiliarValue(fam.getId(), true),
            DataTypes.makeBooleanValue(fam.canEquip()));
      }
    }

    return value;
  }

  public static Value lock_familiar_equipment(ScriptRuntime controller, Value lock) {
    if ((lock.intValue() == 1) != EquipmentManager.familiarItemLocked()) {
      RequestThread.postRequest(new FamiliarRequest(true));
    }
    return DataTypes.VOID_VALUE;
  }

  public static Value equip_all_familiars(ScriptRuntime controller) {
    FamiliarManager.equipAllFamiliars();
    return RuntimeLibrary.continueValue();
  }

  public static Value minstrel_level(ScriptRuntime controller) {
    return DataTypes.makeIntValue(KoLCharacter.getMinstrelLevel());
  }

  public static Value minstrel_instrument(ScriptRuntime controller) {
    AdventureResult item = KoLCharacter.getCurrentInstrument();
    return item == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue(item.getItemId(), true);
  }

  public static Value minstrel_quest(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.minstrelAttention);
  }

  public static Value my_companion(ScriptRuntime controller) {
    if (KoLCharacter.getCompanion() == null) {
      return DataTypes.STRING_INIT;
    }
    return new Value(KoLCharacter.getCompanion().toString());
  }

  public static Value my_thrall(ScriptRuntime controller) {
    return DataTypes.makeThrallValue(KoLCharacter.currentPastaThrall(), true);
  }

  public static Value my_servant(ScriptRuntime controller) {
    return DataTypes.makeServantValue(EdServantData.currentServant(), true);
  }

  public static Value my_vykea_companion(ScriptRuntime controller) {
    return DataTypes.makeVykeaValue(VYKEACompanionData.currentCompanion(), true);
  }

  // Random other functions related to current in-game
  // state, not directly tied to the character.

  public static Value council(ScriptRuntime controller) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("council", "");
    return DataTypes.VOID_VALUE;
  }

  public static Value current_mcd(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMindControlLevel());
  }

  public static Value change_mcd(ScriptRuntime controller, final Value level) {
    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("mcd", level.toString());
    return RuntimeLibrary.continueValue();
  }

  public static Value current_rad_sickness(ScriptRuntime controller) {
    return new Value(KoLCharacter.getRadSickness());
  }

  public static Value have_chef(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.hasChef());
  }

  public static Value have_bartender(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.hasBartender());
  }

  public static Value have_shop(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.hasStore());
  }

  public static Value have_display(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.hasDisplayCase());
  }

  public static Value hippy_stone_broken(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.getHippyStoneBroken());
  }

  public static Value get_counter(ScriptRuntime controller, final Value label) {
    return new Value(TurnCounter.getCounter(label.toString()));
  }

  public static Value get_counters(
      ScriptRuntime controller, final Value label, final Value min, final Value max) {
    return new Value(
        TurnCounter.getCounters(label.toString(), (int) min.intValue(), (int) max.intValue()));
  }

  public static Value stop_counter(ScriptRuntime controller, final Value label) {
    TurnCounter.stopCounting(label.toString());
    return DataTypes.VOID_VALUE;
  }

  public static Value eudora(ScriptRuntime controller) {
    String name = KoLCharacter.getEudora().getName();
    if (name.equals("Pen Pal")) {
      // For compatibility
      name = "Penpal";
    }
    return new Value(name);
  }

  public static Value eudora_item(ScriptRuntime controller) {
    return DataTypes.makeItemValue(
        ItemDatabase.getItemId(KoLCharacter.getEudora().getItem()), true);
  }

  public static Value eudora(ScriptRuntime controller, final Value newEudora) {
    String correspondent = newEudora.toString();

    return DataTypes.makeBooleanValue(EudoraCommand.switchTo(correspondent));
  }

  // String parsing functions.

  public static Value is_integer(ScriptRuntime controller, final Value string) {
    return DataTypes.makeBooleanValue(StringUtilities.isNumeric(string.toString()));
  }

  public static Value contains_text(
      ScriptRuntime controller, final Value source, final Value search) {
    return DataTypes.makeBooleanValue(source.toString().contains(search.toString()));
  }

  public static Value starts_with(
      ScriptRuntime controller, final Value source, final Value prefix) {
    return DataTypes.makeBooleanValue(source.toString().startsWith(prefix.toString()));
  }

  public static Value ends_with(ScriptRuntime controller, final Value source, final Value suffix) {
    return DataTypes.makeBooleanValue(source.toString().endsWith(suffix.toString()));
  }

  public static Value extract_meat(ScriptRuntime controller, final Value string) {
    ArrayList<AdventureResult> data = new ArrayList<>();
    ResultProcessor.processResults(
        false, StringUtilities.globalStringReplace(string.toString(), "- ", "-"), data);

    long meat = 0;

    for (AdventureResult result : data) {
      if (result.getName().equals(AdventureResult.MEAT)) {
        meat += result.getLongCount();
      }
    }

    return new Value(meat);
  }

  public static Value extract_items(ScriptRuntime controller, final Value string) {
    ArrayList<AdventureResult> data = new ArrayList<>();
    ResultProcessor.processResults(
        false, StringUtilities.globalStringReplace(string.toString(), "- ", "-"), data);
    MapValue value = new MapValue(DataTypes.ITEM_TO_INT_TYPE);

    for (AdventureResult result : data) {
      if (result.isItem()) {
        value.aset(
            DataTypes.makeItemValue(result.getItemId(), true),
            DataTypes.parseIntValue(String.valueOf(result.getCount()), true));
      }
    }

    return value;
  }

  public static Value length(ScriptRuntime controller, final Value string) {
    return new Value(string.toString().length());
  }

  public static Value char_at(ScriptRuntime controller, final Value source, final Value index) {
    String string = source.toString();
    int offset = (int) index.intValue();
    if (offset < 0 || offset >= string.length()) {
      throw controller.runtimeException("Offset " + offset + " out of bounds");
    }
    return new Value(Character.toString(string.charAt(offset)));
  }

  public static Value index_of(ScriptRuntime controller, final Value source, final Value search) {
    String string = source.toString();
    String substring = search.toString();
    return new Value(string.indexOf(substring));
  }

  public static Value index_of(
      ScriptRuntime controller, final Value source, final Value search, final Value start) {
    String string = source.toString();
    String substring = search.toString();
    int begin = (int) start.intValue();
    if (begin < 0 || begin > string.length()) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    return new Value(string.indexOf(substring, begin));
  }

  public static Value last_index_of(
      ScriptRuntime controller, final Value source, final Value search) {
    String string = source.toString();
    String substring = search.toString();
    return new Value(string.lastIndexOf(substring));
  }

  public static Value last_index_of(
      ScriptRuntime controller, final Value source, final Value search, final Value start) {
    String string = source.toString();
    String substring = search.toString();
    int begin = (int) start.intValue();
    if (begin < 0 || begin > string.length()) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    return new Value(string.lastIndexOf(substring, begin));
  }

  public static Value substring(ScriptRuntime controller, final Value source, final Value start) {
    String string = source.toString();
    int begin = (int) start.intValue();
    if (begin < 0 || begin > string.length()) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    return new Value(string.substring(begin));
  }

  public static Value substring(
      ScriptRuntime controller, final Value source, final Value start, final Value finish) {
    String string = source.toString();
    int begin = (int) start.intValue();
    if (begin < 0) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    int end = (int) finish.intValue();
    if (end > string.length()) {
      throw controller.runtimeException("End index " + end + " out of bounds");
    }
    if (begin > end) {
      throw controller.runtimeException("Begin index " + begin + " greater than end index " + end);
    }
    return new Value(string.substring(begin, end));
  }

  public static Value to_upper_case(ScriptRuntime controller, final Value string) {
    return new Value(string.toString().toUpperCase());
  }

  public static Value to_lower_case(ScriptRuntime controller, final Value string) {
    return new Value(string.toString().toLowerCase());
  }

  public static Value leetify(ScriptRuntime controller, final Value string) {
    return new Value(StringUtilities.leetify(string.toString()));
  }

  public static Value append(ScriptRuntime controller, final Value buffer, final Value s) {
    StringBuffer current = (StringBuffer) buffer.rawValue();
    current.append(s.toString());
    return buffer;
  }

  public static Value insert(
      ScriptRuntime controller, final Value buffer, final Value index, final Value s) {
    StringBuffer current = (StringBuffer) buffer.rawValue();
    int offset = (int) index.intValue();
    if (offset < 0 || offset > current.length()) {
      throw controller.runtimeException("Index " + index + " out of bounds");
    }
    current.insert(offset, s.toString());
    return buffer;
  }

  public static Value replace(
      ScriptRuntime controller,
      final Value buffer,
      final Value start,
      final Value finish,
      final Value s) {
    StringBuffer current = (StringBuffer) buffer.rawValue();
    int begin = (int) start.intValue();
    if (begin < 0) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    int end = (int) finish.intValue();
    if (end > current.length()) {
      throw controller.runtimeException("End index " + end + " out of bounds");
    }
    if (begin > end) {
      throw controller.runtimeException("Begin index " + begin + " greater than end index " + end);
    }
    current.replace(begin, end, s.toString());
    return buffer;
  }

  public static Value delete(
      ScriptRuntime controller, final Value buffer, final Value start, final Value finish) {
    StringBuffer current = (StringBuffer) buffer.rawValue();
    int begin = (int) start.intValue();
    if (begin < 0) {
      throw controller.runtimeException("Begin index " + begin + " out of bounds");
    }
    int end = (int) finish.intValue();
    if (end > current.length()) {
      throw controller.runtimeException("End index " + end + " out of bounds");
    }
    if (begin > end) {
      throw controller.runtimeException("Begin index " + begin + " greater than end index " + end);
    }
    current.delete(begin, end);
    return buffer;
  }

  public static Value set_length(ScriptRuntime controller, final Value buffer, final Value i) {
    StringBuffer current = (StringBuffer) buffer.rawValue();
    int length = (int) i.intValue();
    if (length < 0) {
      throw controller.runtimeException("Desired length is less than zero");
    }
    current.setLength(length);
    return DataTypes.VOID_VALUE;
  }

  public static Value append_tail(
      ScriptRuntime controller, final Value matcher, final Value buffer) {
    Matcher m = (Matcher) matcher.rawValue();
    StringBuffer current = (StringBuffer) buffer.rawValue();
    m.appendTail(current);
    return buffer;
  }

  public static Value append_replacement(
      ScriptRuntime controller, final Value matcher, final Value buffer, final Value replacement) {
    Matcher m = (Matcher) matcher.rawValue();
    StringBuffer current = (StringBuffer) buffer.rawValue();
    m.appendReplacement(current, replacement.toString());
    return buffer;
  }

  public static Value create_matcher(
      ScriptRuntime controller, final Value patternValue, final Value stringValue) {
    String pattern = patternValue.toString();
    String string = stringValue.toString();

    if (!(patternValue.content instanceof Pattern)) {
      try {
        patternValue.content = Pattern.compile(pattern, Pattern.DOTALL);
      } catch (PatternSyntaxException e) {
        throw controller.runtimeException("Invalid pattern syntax");
      }
    }

    Pattern p = (Pattern) patternValue.content;
    return new Value(DataTypes.MATCHER_TYPE, pattern, p.matcher(string));
  }

  public static Value find(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    return DataTypes.makeBooleanValue(m.find());
  }

  public static Value start(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    try {
      return new Value(m.start());
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    }
  }

  public static Value start(ScriptRuntime controller, final Value matcher, final Value group) {
    Matcher m = (Matcher) matcher.rawValue();
    int index = (int) group.intValue();
    try {
      return new Value(m.start(index));
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    } catch (IndexOutOfBoundsException e) {
      throw controller.runtimeException(
          "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups");
    }
  }

  public static Value end(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    try {
      return new Value(m.end());
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    }
  }

  public static Value end(ScriptRuntime controller, final Value matcher, final Value group) {
    Matcher m = (Matcher) matcher.rawValue();
    int index = (int) group.intValue();
    try {
      return new Value(m.end(index));
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    } catch (IndexOutOfBoundsException e) {
      throw controller.runtimeException(
          "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups");
    }
  }

  public static Value group(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    try {
      return new Value(m.group());
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    }
  }

  public static Value group(ScriptRuntime controller, final Value matcher, final Value group) {
    Matcher m = (Matcher) matcher.rawValue();
    Type type = group.getType();
    try {
      if (type.equals(DataTypes.INT_TYPE)) {
        int index = (int) group.intValue();

        try {
          return new Value(m.group(index));
        } catch (IndexOutOfBoundsException e) {
          throw controller.runtimeException(
              "Group " + index + " requested, but pattern only has " + m.groupCount() + " groups");
        }
      } else {
        String name = group.toString();

        try {
          return new Value(m.group(name));
        } catch (IllegalArgumentException e) {
          throw controller.runtimeException(
              "Group " + name + " requested, but that group name was not found");
        }
      }
    } catch (IllegalStateException e) {
      throw controller.runtimeException("No match attempted or previous match failed");
    }
  }

  public static Value group_count(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    return new Value(m.groupCount());
  }

  static final Pattern GROUP_NAME_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  public static Value group_names(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    Pattern p = m.pattern();
    Matcher names = RuntimeLibrary.GROUP_NAME_PATTERN.matcher(p.toString());

    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE);
    MapValue value = new MapValue(type);

    while (names.find()) {
      value.aset(new Value(names.group(1)), DataTypes.TRUE_VALUE);
    }

    return value;
  }

  public static Value replace_first(
      ScriptRuntime controller, final Value matcher, final Value replacement) {
    Matcher m = (Matcher) matcher.rawValue();
    return new Value(m.replaceFirst(replacement.toString()));
  }

  public static Value replace_all(
      ScriptRuntime controller, final Value matcher, final Value replacement) {
    Matcher m = (Matcher) matcher.rawValue();
    return new Value(m.replaceAll(replacement.toString()));
  }

  public static Value reset(ScriptRuntime controller, final Value matcher) {
    Matcher m = (Matcher) matcher.rawValue();
    m.reset();
    return matcher;
  }

  public static Value reset(ScriptRuntime controller, final Value matcher, final Value input) {
    Matcher m = (Matcher) matcher.rawValue();
    m.reset(input.toString());
    return matcher;
  }

  public static Value replace_string(
      ScriptRuntime controller,
      final Value source,
      final Value searchValue,
      final Value replaceValue) {
    StringBuffer buffer;
    Value returnValue;

    if (source.rawValue() instanceof StringBuffer) {
      buffer = (StringBuffer) source.rawValue();
      returnValue = source;
    } else {
      buffer = new StringBuffer(source.toString());
      returnValue = new Value(DataTypes.BUFFER_TYPE, "", buffer);
    }

    String search = searchValue.toString();
    String replace = replaceValue.toString();

    StringUtilities.globalStringReplace(buffer, search, replace);
    return returnValue;
  }

  public static Value split_string(ScriptRuntime controller, final Value string) {
    return split_string(string.toString().split(KoLConstants.LINE_BREAK));
  }

  public static Value split_string(
      ScriptRuntime controller, final Value string, final Value regex) {
    Pattern p;
    if (regex.rawValue() instanceof Pattern) {
      p = (Pattern) regex.rawValue();
    } else {
      try {
        p = Pattern.compile(regex.toString());
        if (regex.content == null) {
          regex.content = p;
        }
      } catch (PatternSyntaxException e) {
        throw controller.runtimeException("Invalid pattern syntax");
      }
    }

    return split_string(p.split(string.toString()));
  }

  private static Value split_string(String[] pieces) {
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, pieces.length);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < pieces.length; ++i) {
      value.aset(new Value(i), new Value(pieces[i]));
    }

    return value;
  }

  public static Value join_strings(ScriptRuntime controller, final Value strings) {
    return join_strings(strings, KoLConstants.LINE_BREAK);
  }

  public static Value join_strings(
      ScriptRuntime controller, final Value strings, final Value joiner) {
    return join_strings(strings, joiner.toString());
  }

  private static Value join_strings(final Value strings, final String joiner) {
    Value[] array = (Value[]) strings.content;
    return new Value(Arrays.stream(array).map(Value::toString).collect(Collectors.joining(joiner)));
  }

  public static Value group_string(
      ScriptRuntime controller, final Value string, final Value regex) {
    Pattern p;
    if (regex.rawValue() instanceof Pattern) {
      p = (Pattern) regex.rawValue();
    } else {
      try {
        p = Pattern.compile(regex.toString());
        if (regex.content == null) {
          regex.content = p;
        }
      } catch (PatternSyntaxException e) {
        throw controller.runtimeException("Invalid pattern syntax");
      }
    }
    Matcher userPatternMatcher = p.matcher(string.toString());
    MapValue value = new MapValue(DataTypes.REGEX_GROUP_TYPE);

    int matchCount = 0;
    int groupCount = userPatternMatcher.groupCount();

    Value[] groupIndexes = new Value[groupCount + 1];
    for (int i = 0; i <= groupCount; ++i) {
      groupIndexes[i] = new Value(i);
    }

    Value matchIndex;
    CompositeValue slice;

    try {
      while (userPatternMatcher.find()) {
        matchIndex = new Value(matchCount);
        slice = (CompositeValue) value.initialValue(matchIndex);

        value.aset(matchIndex, slice);
        for (int i = 0; i <= groupCount; ++i) {
          slice.aset(groupIndexes[i], new Value(userPatternMatcher.group(i)));
        }

        ++matchCount;
      }
    } catch (Exception e) {
      // Because we're doing everything ourselves, this
      // error shouldn't get generated.  Print a stack
      // trace, just in case.

      StaticEntity.printStackTrace(e);
    }

    return value;
  }

  public static Value php_seed(ScriptRuntime controller, final Value seed) {
    return new Value(new Rng(seed.intValue()));
  }

  public static Value php_rand(ScriptRuntime controller, final Value rng) {
    Rng r = (Rng) rng.rawValue();
    return new Value(r.nextRandInt());
  }

  public static Value php_mt_rand(ScriptRuntime controller, final Value rng) {
    Rng r = (Rng) rng.rawValue();
    return new Value(r.nextMtRandInt());
  }

  public static Value expression_eval(ScriptRuntime controller, final Value expr) {
    Expression e;
    if (expr.content instanceof Expression) {
      e = (Expression) expr.content;
    } else {
      e = new Expression(expr.toString(), "expression_eval()");
      String errors = e.getExpressionErrors();
      if (errors != null) {
        throw controller.runtimeException(errors);
      }

      if (expr.content == null) {
        expr.content = e;
      }
    }
    return RuntimeLibrary.eval(controller, e);
  }

  public static Value modifier_eval(ScriptRuntime controller, final Value expr) {
    ModifierExpression e;
    if (expr.content instanceof ModifierExpression) {
      e = (ModifierExpression) expr.content;
    } else {
      e = new ModifierExpression(expr.toString(), ModifierType.GENERATED, "modifier_eval()");
      String errors = e.getExpressionErrors();
      if (errors != null) {
        throw controller.runtimeException(errors);
      }
      if (expr.content == null) {
        expr.content = e;
      }
    }
    return RuntimeLibrary.eval(controller, e);
  }

  private static Value eval(ScriptRuntime controller, final Expression expr) {
    try {
      return new Value(expr.evalInternal());
    } catch (Exception e) {
      throw controller.runtimeException("Expression evaluation error: " + e.getMessage());
    }
  }

  public static Value maximize(
      ScriptRuntime controller,
      final Value maximizerStringValue,
      final Value isSpeculateOnlyValue) {
    return maximize(
        controller,
        maximizerStringValue,
        DataTypes.ZERO_VALUE,
        DataTypes.ZERO_VALUE,
        isSpeculateOnlyValue);
  }

  public static Value maximize(
      ScriptRuntime controller,
      final Value maximizerStringValue,
      final Value maxPriceValue,
      final Value priceLevelValue,
      final Value isSpeculateOnlyValue) {
    String maximizerString = maximizerStringValue.toString();
    int maxPrice = (int) maxPriceValue.intValue();
    int priceLevel = (int) priceLevelValue.intValue();
    boolean isSpeculateOnly = isSpeculateOnlyValue.intValue() != 0;

    return new Value(
        Maximizer.maximize(
            maximizerString, maxPrice, PriceLevel.byIndex(priceLevel), isSpeculateOnly));
  }

  public static Value maximize(
      ScriptRuntime controller,
      final Value maximizerStringValue,
      final Value maxPriceValue,
      final Value priceLevelValue,
      final Value isSpeculateOnlyValue,
      final Value showEquipment) {
    String maximizerString = maximizerStringValue.toString();
    int maxPrice = (int) maxPriceValue.intValue();
    int priceLevel = (int) priceLevelValue.intValue();
    boolean isSpeculateOnly = isSpeculateOnlyValue.intValue() != 0;
    boolean showEquip = showEquipment.intValue() == 1;

    Maximizer.maximize(maximizerString, maxPrice, PriceLevel.byIndex(priceLevel), isSpeculateOnly);

    List<Boost> m = Maximizer.boosts;

    int lastEquipIndex = 0;

    if (!showEquip) {
      for (Boost boo : m) {
        if (!boo.isEquipment()) break;
        lastEquipIndex++;
      }
    }

    AggregateType type =
        new AggregateType(RuntimeLibrary.maximizerResult, m.size() - lastEquipIndex);
    ArrayValue value = new ArrayValue(type);

    for (int i = lastEquipIndex; i < m.size(); ++i) {
      Boost boo = m.get(i);
      String text = boo.toString();
      String cmd = boo.getCmd();
      double boost = boo.getBoost();
      AdventureResult arEffect = boo.isEquipment() ? null : boo.getItem();
      AdventureResult arItem = boo.getItem(false);
      String skill =
          cmd.startsWith("cast") ? UneffectRequest.effectToSkill(arEffect.getName()) : null;

      // remove the (+ X) from the display text, that info is in the score
      int cutIndex = boo.toString().indexOf(" (");
      if (cutIndex != -1) {
        text = text.substring(0, cutIndex);
      }

      RecordValue rec = (RecordValue) value.aref(new Value(i - lastEquipIndex));

      rec.aset(0, DataTypes.parseStringValue(text), null);
      rec.aset(1, DataTypes.parseStringValue(cmd), null);
      rec.aset(2, new Value(boost), null);
      rec.aset(
          3,
          arEffect == null
              ? DataTypes.EFFECT_INIT
              : DataTypes.parseEffectValue(arEffect.getName(), true),
          null);
      rec.aset(
          4,
          arItem == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue(arItem.getItemId(), true),
          null);
      rec.aset(
          5, skill == null ? DataTypes.SKILL_INIT : DataTypes.parseSkillValue(skill, true), null);
    }

    return value;
  }

  public static Value monster_eval(ScriptRuntime controller, final Value expr) {
    MonsterExpression e;
    if (expr.content instanceof MonsterExpression) {
      e = (MonsterExpression) expr.rawValue();
    } else {
      e = new MonsterExpression(expr.toString(), "monster_eval()");
      String errors = e.getExpressionErrors();
      if (errors != null) {
        throw controller.runtimeException(errors);
      }
      if (expr.content == null) {
        expr.content = e;
      }
    }
    return RuntimeLibrary.eval(controller, e);
  }

  public static Value is_online(ScriptRuntime controller, final Value arg) {
    String name = arg.toString();
    return DataTypes.makeBooleanValue(KoLmafia.isPlayerOnline(name));
  }

  static final Pattern COUNT_PATTERN = Pattern.compile("You have (\\d+) ");

  public static Value slash_count(ScriptRuntime controller, final Value arg) {
    String itemName = ItemDatabase.getItemName((int) arg.intValue());
    InternalChatRequest request = new InternalChatRequest("/count " + itemName);
    RequestThread.postRequest(request);
    Matcher m = RuntimeLibrary.COUNT_PATTERN.matcher(request.responseText);
    return new Value(m.find() ? StringUtilities.parseInt(m.group(1)) : 0);
  }

  public static Value chat_macro(ScriptRuntime controller, final Value macroValue) {
    String macro = macroValue.toString().trim();

    ChatSender.executeMacro(macro);

    return DataTypes.VOID_VALUE;
  }

  public static Value chat_clan(ScriptRuntime controller, final Value messageValue) {
    String channel = "/clan";
    String message = messageValue.toString().trim();

    ChatSender.sendMessage(channel, message, true);
    return DataTypes.VOID_VALUE;
  }

  public static Value chat_clan(
      ScriptRuntime controller, final Value messageValue, final Value recipientValue) {
    String channel = "/" + recipientValue.toString().trim();
    String message = messageValue.toString().trim();

    ChatSender.sendMessage(channel, message, true);
    return DataTypes.VOID_VALUE;
  }

  public static Value chat_private(
      ScriptRuntime controller, final Value recipientValue, final Value messageValue) {
    String recipient = recipientValue.toString();

    String message = messageValue.toString();

    if (message.equals("") || message.startsWith("/")) {
      return DataTypes.VOID_VALUE;
    }

    ChatSender.sendMessage(recipient, message, false);

    return DataTypes.VOID_VALUE;
  }

  public static Value chat_notify(
      ScriptRuntime controller, final Value messageValue, final Value colorValue) {
    String messageString = StringUtilities.getEntityEncode(messageValue.toString(), false);

    String colorString = StringUtilities.globalStringDelete(colorValue.toString(), "\"");
    colorString = "\"" + colorString + "\"";

    InternalMessage message = new InternalMessage(messageString, colorString);

    ChatPoller.addEntry(message);

    return DataTypes.VOID_VALUE;
  }

  public static Value who_clan(ScriptRuntime controller) {
    InternalChatRequest request = new InternalChatRequest("/who clan");
    List<ChatMessage> chatMessages = ChatSender.sendRequest(request);

    MapValue value = new MapValue(DataTypes.STRING_TO_BOOLEAN_TYPE);
    for (ChatMessage chatMessage : chatMessages) {
      if (chatMessage instanceof WhoMessage message) {
        for (Entry<String, Boolean> entry : message.getContacts().entrySet()) {
          value.aset(new Value(entry.getKey()), DataTypes.makeBooleanValue(entry.getValue()));
        }

        break;
      }
    }

    return value;
  }

  public static Value get_player_id(ScriptRuntime controller, final Value playerNameValue) {
    String playerName = playerNameValue.toString();

    return new Value(ContactManager.getPlayerId(playerName, true));
  }

  public static Value get_player_name(ScriptRuntime controller, final Value playerIdValue) {
    String playerId = playerIdValue.toIntValue().toString();

    return new Value(ContactManager.getPlayerName(playerId, true));
  }

  // Quest completion functions.

  public static Value tavern(ScriptRuntime controller) {
    int result = TavernManager.locateTavernFaucet();
    return new Value(KoLmafia.permitsContinue() ? result : -1);
  }

  public static Value tavern(ScriptRuntime controller, final Value arg) {
    String goal = arg.toString();
    int result = -1;
    if (goal.equalsIgnoreCase("faucet")) {
      result = TavernManager.locateTavernFaucet();
    } else if (goal.equalsIgnoreCase("baron")) {
      result = TavernManager.locateBaron();
    } else if (goal.equalsIgnoreCase("fight")) {
      result = TavernManager.fightBaron();
    } else if (goal.equalsIgnoreCase("explore")) {
      result = TavernManager.exploreTavern();
    }
    return new Value(KoLmafia.permitsContinue() ? result : -1);
  }

  public static Value hedge_maze(ScriptRuntime controller, final Value arg) {
    String goal = arg.toString();
    SorceressLairManager.hedgeMazeScript(goal);
    return RuntimeLibrary.continueValue();
  }

  public static Value tower_door(ScriptRuntime controller) {
    TowerDoorManager.towerDoorScript();
    return RuntimeLibrary.continueValue();
  }

  // Arithmetic utility functions.

  public static Value random(ScriptRuntime controller, final Value arg) {
    int range = (int) arg.intValue();
    if (range < 2) {
      throw controller.runtimeException("Random range must be at least 2");
    }
    return new Value(KoLConstants.RNG.nextInt(range));
  }

  public static Value round(ScriptRuntime controller, final Value arg) {
    return new Value(Math.round(arg.floatValue()));
  }

  public static Value truncate(ScriptRuntime controller, final Value arg) {
    return new Value((long) arg.floatValue());
  }

  public static Value floor(ScriptRuntime controller, final Value arg) {
    return new Value((long) Math.floor(arg.floatValue()));
  }

  public static Value ceil(ScriptRuntime controller, final Value arg) {
    return new Value((long) Math.ceil(arg.floatValue()));
  }

  public static Value square_root(ScriptRuntime controller, final Value val) {
    double value = val.floatValue();
    if (value < 0.0) {
      throw controller.runtimeException("Can't take square root of a negative value");
    }
    return new Value(Math.sqrt(value));
  }

  public static Value min(ScriptRuntime controller, final Value arg1, final Value arg2) {
    if (arg1.getType().equals(DataTypes.INT_TYPE)
        && arg2.getType().equals(DataTypes.VARARG_INT_TYPE)) {
      long min = arg1.toIntValue().intValue();
      ArrayValue array = (ArrayValue) arg2;
      int length = array.count();
      for (int i = 0; i < length; ++i) {
        Value value = array.aref(new Value(i));
        min = Math.min(min, value.toIntValue().intValue());
      }
      return new Value(min);
    } else {
      double min = arg1.toFloatValue().floatValue();
      ArrayValue array = (ArrayValue) arg2;
      int length = array.count();
      for (int i = 0; i < length; ++i) {
        Value value = array.aref(new Value(i));
        min = Math.min(min, value.toFloatValue().floatValue());
      }
      return new Value(min);
    }
  }

  public static Value max(ScriptRuntime controller, final Value arg1, final Value arg2) {
    if (arg1.getType().equals(DataTypes.INT_TYPE)
        && arg2.getType().equals(DataTypes.VARARG_INT_TYPE)) {
      long max = arg1.toIntValue().intValue();
      ArrayValue array = (ArrayValue) arg2;
      int length = array.count();
      for (int i = 0; i < length; ++i) {
        Value value = array.aref(new Value(i));
        max = Math.max(max, value.toIntValue().intValue());
      }
      return new Value(max);
    } else {
      double max = arg1.toFloatValue().floatValue();
      ArrayValue array = (ArrayValue) arg2;
      int length = array.count();
      for (int i = 0; i < length; ++i) {
        Value value = array.aref(new Value(i));
        max = Math.max(max, value.toFloatValue().floatValue());
      }
      return new Value(max);
    }
  }

  public static Value log_n(ScriptRuntime controller, final Value arg, final Value base) {
    return new Value(Math.log(arg.floatValue()) / Math.log(base.floatValue()));
  }

  public static Value log_n(ScriptRuntime controller, final Value arg) {
    return new Value(Math.log(arg.floatValue()));
  }

  // Settings-type functions.

  public static Value url_encode(ScriptRuntime controller, final Value arg) {
    return new Value(GenericRequest.encodeURL(arg.toString()));
  }

  public static Value url_decode(ScriptRuntime controller, final Value arg) {
    return new Value(GenericRequest.decodeField(arg.toString()));
  }

  public static Value entity_encode(ScriptRuntime controller, final Value arg)
      throws UnsupportedEncodingException {
    return new Value(CharacterEntities.escape(arg.toString()));
  }

  public static Value entity_decode(ScriptRuntime controller, final Value arg)
      throws UnsupportedEncodingException {
    return new Value(CharacterEntities.unescape(arg.toString()));
  }

  private static boolean built_in_property(String name) {
    return name.startsWith("choiceAdventure")
        || name.startsWith("skillBurn")
        || RuntimeLibrary.frameNames.contains(name)
        || name.equals("KoLDesktop");
  }

  public static Value get_all_properties(
      ScriptRuntime controller, final Value filterValue, final Value globalValue) {
    // This returns a map from string -> boolean which is property name -> builtin
    // filter is a substring (ignoring case) of the property name.
    // If filter is "", all properties in the specified scope are returned.

    String filter = filterValue.toString().trim().toLowerCase();
    boolean all = filter.equals("");
    boolean global = globalValue.intValue() != 0;

    // The following create a case-insensitive map. This makes
    // properties sort prettily, but, unfortunately, Preferences
    // really are case sensitive.
    //
    // MapValue value = new MapValue( DataTypes.STRING_TO_BOOLEAN_TYPE, true );

    MapValue value = new MapValue(DataTypes.STRING_TO_BOOLEAN_TYPE);

    Map<String, String> properties = Preferences.getMap(false, !global);
    Map<String, String> defaults = Preferences.getMap(true, !global);

    for (String name : properties.keySet()) {
      if (!Preferences.isUserEditable(name)) {
        continue;
      }

      if (all || name.toLowerCase().contains(filter)) {
        boolean builtIn = defaults.containsKey(name);
        if (!builtIn) {
          if (global) {
            builtIn = Preferences.isPerUserGlobalProperty(name);
          } else {
            builtIn = RuntimeLibrary.built_in_property(name);
          }
        }
        Value key = new Value(name);
        Value val = DataTypes.makeBooleanValue(builtIn);
        value.aset(key, val);
      }
    }

    return value;
  }

  public static Value property_exists(ScriptRuntime controller, final Value nameValue) {
    // Look up a property (in the specified scope) and return true
    // if is present and false otherwise
    String name = nameValue.toString();

    if (Preferences.propertyExists(name)) {
      return DataTypes.TRUE_VALUE;
    }

    // All choiceAdventureXXX and skillBurnXXX properties are
    // considered to exist in the user scope even if they don't
    // appear in defaults.txt.

    if (RuntimeLibrary.built_in_property(name)) {
      return DataTypes.TRUE_VALUE;
    }

    return DataTypes.FALSE_VALUE;
  }

  public static Value property_exists(
      ScriptRuntime controller, final Value nameValue, final Value globalValue) {
    // Look up a property (in the specified scope) and return true
    // if is present and false otherwise
    String name = nameValue.toString();
    boolean global = globalValue.intValue() != 0;

    if (Preferences.propertyExists(name, global)) {
      return DataTypes.TRUE_VALUE;
    }

    // All choiceAdventureXXX and skillBurnXXX properties are
    // considered to exist in the user scope even if they don't
    // appear in defaults.txt.

    if (!global && RuntimeLibrary.built_in_property(name)) {
      return DataTypes.TRUE_VALUE;
    }

    return DataTypes.FALSE_VALUE;
  }

  public static Value property_has_default(ScriptRuntime controller, final Value nameValue) {
    String name = nameValue.toString();
    return DataTypes.makeBooleanValue(Preferences.containsDefault(name));
  }

  public static Value property_default_value(ScriptRuntime controller, final Value nameValue) {
    String name = nameValue.toString();
    return Preferences.containsDefault(name)
        ? DataTypes.makeStringValue(Preferences.getDefault(name))
        : DataTypes.STRING_INIT;
  }

  public static Value get_property(ScriptRuntime controller, final Value name) {
    String property = name.toString();

    if (property.startsWith("System.")) {
      return new Value(System.getProperty(property.substring(7)));
    }

    if (Preferences.isUserEditable(property)) {
      Preferences.warnIfDeprecated(property);
      return DataTypes.makeStringValue(Preferences.getString(property));
    }

    return DataTypes.STRING_INIT;
  }

  public static Value get_property(
      ScriptRuntime controller, final Value name, final Value globalValue) {
    String property = name.toString();

    if (!Preferences.isUserEditable(property)) {
      return DataTypes.STRING_INIT;
    }

    boolean global = globalValue.intValue() != 0;

    // Look up a property (in the specified scope) and return the current value.
    if (Preferences.propertyExists(property, global)) {
      return DataTypes.makeStringValue(Preferences.getString(property, global));
    }

    // If the property is not found (in the specified scope), "" is returned
    return DataTypes.STRING_INIT;
  }

  public static Value set_property(
      ScriptRuntime controller, final Value nameValue, final Value value) {
    String name = nameValue.toString();

    if (!Preferences.isUserEditable(name)) {
      return DataTypes.VOID_VALUE;
    }

    // Only print to CLI if changing built-in property
    boolean builtin = Preferences.containsDefault(name);

    // Avoid code duplication for combat related settings; call "set" command
    SetPreferencesCommand.setProperty(name, value.toString(), builtin);

    return DataTypes.VOID_VALUE;
  }

  public static Value remove_property(ScriptRuntime controller, final Value nameValue) {
    String name = nameValue.toString();

    if (!Preferences.isUserEditable(name) || Preferences.isPerUserGlobalProperty(name)) {
      return DataTypes.STRING_INIT;
    }

    String oldValue;

    // If it is listed in defaults.txt, set property back to default value.
    if (Preferences.containsDefault(name)) {
      oldValue = Preferences.getString(name);
      Preferences.resetToDefault(name);
    }
    // If it is in the user map, remove from there
    else if (Preferences.propertyExists(name, false)) {
      oldValue = Preferences.getString(name, false);
      Preferences.removeProperty(name, false);
    }
    // If it is in the global map, remove from there
    else if (Preferences.propertyExists(name, true)) {
      oldValue = Preferences.getString(name, true);
      Preferences.removeProperty(name, true);
    }
    // If it's not in either map, nothing to do
    else {
      oldValue = "";
    }

    return DataTypes.makeStringValue(oldValue);
  }

  public static Value remove_property(
      ScriptRuntime controller, final Value nameValue, final Value globalValue) {
    String name = nameValue.toString();

    if (!Preferences.isUserEditable(name) || Preferences.isPerUserGlobalProperty(name)) {
      return DataTypes.STRING_INIT;
    }

    boolean global = globalValue.intValue() != 0;
    String oldValue;

    // If it's not a known global property but we want to remove
    // from global map, we are cleaning up orphaned globals
    if (!Preferences.isGlobalProperty(name) && global) {
      oldValue = Preferences.getString(name, true);
      Preferences.removeProperty(name, true);
    }
    // If it is a known global property but we want to remove from
    // user map, we are cleaning up orphaned user properties
    else if (Preferences.isGlobalProperty(name) && !global) {
      oldValue = Preferences.getString(name, false);
      Preferences.removeProperty(name, false);
    }
    // If it is listed in defaults.txt, set property back to default value.
    else if (Preferences.containsDefault(name)) {
      oldValue = Preferences.getString(name);
      Preferences.resetToDefault(name);
    }
    // Otherwise, remove from the specified map.
    else {
      oldValue = Preferences.getString(name, global);
      Preferences.removeProperty(name, global);
    }

    return DataTypes.makeStringValue(oldValue);
  }

  public static Value rename_property(
      ScriptRuntime controller, final Value oldNameValue, final Value newNameValue) {
    String oldName = oldNameValue.toString();
    String newName = newNameValue.toString();

    // User scripts cannot rename built-in properties
    if (Preferences.containsDefault(oldName) || Preferences.containsDefault(newName)) {
      return DataTypes.FALSE_VALUE;
    }

    // If the old name does not exist, do nothing
    if (!Preferences.propertyExists(oldName, false)) {
      return DataTypes.FALSE_VALUE;
    }

    // If the new name does exist, do nothing
    if (Preferences.propertyExists(newName, false)) {
      return DataTypes.FALSE_VALUE;
    }

    // Get value of old property in user map
    String oldValue = Preferences.getString(oldName, false);

    // Remove old property from user map
    Preferences.removeProperty(oldName, false);

    // Create new property in user map
    Preferences.setString(newName, oldValue);

    // Return success
    return DataTypes.TRUE_VALUE;
  }

  // Functions for aggregates.

  public static Value count(ScriptRuntime controller, final Value arg) {
    return new Value(arg.count());
  }

  public static Value clear(ScriptRuntime controller, final Value arg) {
    arg.clear();
    return DataTypes.VOID_VALUE;
  }

  public static Value file_to_map(ScriptRuntime controller, final Value var1, final Value var2) {
    return file_to_map(controller, var1, var2, DataTypes.TRUE_VALUE);
  }

  public static Value file_to_map(
      ScriptRuntime controller, final Value var1, final Value var2, final Value var3) {
    String filename = var1.toString();
    CompositeValue result = (CompositeValue) var2;
    boolean compact = var3.intValue() == 1;

    BufferedReader reader = DataFileCache.getReader(filename);
    if (reader == null) {
      return DataTypes.FALSE_VALUE;
    }

    String[] data = null;
    result.clear();

    try (reader) {
      int line = 0;
      while ((data = FileUtilities.readData(reader)) != null) {
        line++;
        if (data.length > 1) {
          result.read(data, 0, compact, filename, line);
        }
      }
    } catch (Exception e) {
      StringBuilder buffer = new StringBuilder("Invalid line in data file");
      if (data != null) {
        buffer.append(": \"");
        for (int i = 0; i < data.length; ++i) {
          if (i > 0) {
            buffer.append('\t');
          }
          buffer.append(data[i]);
        }
        buffer.append("\"");
      }

      // Print the bad data that caused the error
      Exception ex = controller.runtimeException(buffer.toString());

      // If it's a ScriptException, we generated it ourself
      if (e instanceof ScriptException) {
        // Print the bad data and the resulting error
        RequestLogger.printLine(ex.getMessage());
        RequestLogger.printLine(e.getMessage());
      } else {
        // Otherwise, print a stack trace
        StaticEntity.printStackTrace(e, ex.getMessage());
      }
      return DataTypes.FALSE_VALUE;
    }

    return DataTypes.TRUE_VALUE;
  }

  public static Value map_to_file(ScriptRuntime controller, final Value var1, final Value var2) {
    return map_to_file(controller, var1, var2, DataTypes.TRUE_VALUE);
  }

  public static Value map_to_file(
      ScriptRuntime controller, final Value var1, final Value var2, final Value var3) {
    CompositeValue map_variable = (CompositeValue) var1;
    String filename = var2.toString();
    boolean compact = var3.intValue() == 1;

    ByteArrayOutputStream cacheStream = new ByteArrayOutputStream();

    PrintStream writer = LogStream.openStream(cacheStream, StandardCharsets.UTF_8);
    map_variable.dump(writer, "", compact);
    writer.close();

    byte[] data = cacheStream.toByteArray();
    return DataFileCache.printBytes(filename, data);
  }

  public static Value file_to_array(ScriptRuntime controller, final Value var1) {
    AshRuntime interpreter = controller instanceof AshRuntime ? (AshRuntime) controller : null;

    String filename = var1.toString();
    MapValue result = new MapValue(DataTypes.INT_TO_STRING_TYPE);

    BufferedReader reader = DataFileCache.getReader(filename);
    if (reader == null) {
      return result;
    }

    String data;

    try (reader) {
      int line = 0;
      while ((data = FileUtilities.readLine(reader)) != null) {
        line++;
        result.aset(new Value(line), new Value(data), interpreter);
      }
    } catch (Exception e) {
      return result;
    }

    return result;
  }

  public static Value file_to_buffer(ScriptRuntime controller, final Value var1) {
    String location = var1.toString();
    byte[] bytes = DataFileCache.getBytes(location);
    String string = new String(bytes, StandardCharsets.UTF_8);
    StringBuffer buffer = new StringBuffer(string);
    return new Value(DataTypes.BUFFER_TYPE, "", buffer);
  }

  public static Value buffer_to_file(ScriptRuntime controller, final Value var1, final Value var2) {
    StringBuffer buffer = (StringBuffer) var1.rawValue();
    String string = buffer.toString();
    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
    String location = var2.toString();
    return DataFileCache.printBytes(location, bytes);
  }

  public static Value set_ccs(ScriptRuntime controller, final Value name) {
    String ccsName = name.toString();
    Optional<String> strategy =
        CombatActionManager.getAvailableLookups().stream()
            .filter(script -> script.equalsIgnoreCase(ccsName))
            .findFirst();
    if (strategy.isPresent()) {
      CombatActionManager.loadStrategyLookup(strategy.get());
      return DataTypes.TRUE_VALUE;
    } else {
      return DataTypes.FALSE_VALUE;
    }
  }

  public static Value read_ccs(ScriptRuntime controller, final Value name) {
    String ccsName = name.toString();
    byte[] bytes = CcsFileManager.getBytes(ccsName);
    String string = new String(bytes, StandardCharsets.UTF_8);
    StringBuffer buffer = new StringBuffer(string);
    return new Value(DataTypes.BUFFER_TYPE, "", buffer);
  }

  public static Value write_ccs(ScriptRuntime controller, final Value data, final Value name) {
    StringBuffer buffer = (StringBuffer) data.rawValue();
    String string = buffer.toString();
    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
    String ccsName = name.toString();
    return DataTypes.makeBooleanValue(CcsFileManager.printBytes(ccsName, bytes));
  }

  // Custom combat helper functions.

  public static Value my_location(ScriptRuntime controller) {
    String location = Preferences.getString("nextAdventure");
    return DataTypes.parseLocationValue(location, true);
  }

  public static Value set_location(ScriptRuntime controller, final Value location) {
    KoLAdventure adventure = (KoLAdventure) location.rawValue();
    if (adventure == null) {
      Preferences.setString("nextAdventure", "None");
      KoLCharacter.updateSelectedLocation(null);
    } else {
      Preferences.setString("nextAdventure", adventure.getAdventureName());
      KoLCharacter.updateSelectedLocation(adventure);
    }
    return DataTypes.VOID_VALUE;
  }

  public static Value last_monster(ScriptRuntime controller) {
    return DataTypes.makeMonsterValue(MonsterStatusTracker.getLastMonster());
  }

  private static MonsterData mapMonster(
      MonsterData mon,
      Map<MonsterData, MonsterData> classMap,
      Map<MonsterData, MonsterData> pathMap) {
    if (pathMap != null) {
      MonsterData mapped = pathMap.get(mon);
      if (mapped != null) {
        return mapped;
      }
    }
    if (classMap != null) {
      MonsterData mapped = classMap.get(mon);
      if (mapped != null) {
        return mapped;
      }
    }
    return mon;
  }

  public static Value get_monsters(ScriptRuntime controller, final Value location) {
    KoLAdventure adventure = (KoLAdventure) location.rawValue();
    AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

    int monsterCount = data == null ? 0 : data.getMonsterCount();
    int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();

    AggregateType type =
        new AggregateType(DataTypes.MONSTER_TYPE, monsterCount + superlikelyMonsterCount);
    ArrayValue value = new ArrayValue(type);

    Map<MonsterData, MonsterData> classMap =
        MonsterDatabase.getMonsterClassMap(KoLCharacter.getAscensionClass());
    Map<MonsterData, MonsterData> pathMap =
        MonsterDatabase.getMonsterPathMap(KoLCharacter.getPath().getName());

    for (int i = 0; i < monsterCount; ++i) {
      MonsterData mon = mapMonster(data.getMonster(i), classMap, pathMap);
      value.aset(new Value(i), DataTypes.makeMonsterValue(mon));
    }

    for (int i = 0; i < superlikelyMonsterCount; ++i) {
      MonsterData mon = mapMonster(data.getSuperlikelyMonster(i), classMap, pathMap);
      value.aset(new Value(i + monsterCount), DataTypes.makeMonsterValue(mon));
    }

    return value;
  }

  public static Value get_location_monsters(ScriptRuntime controller, final Value location) {
    KoLAdventure adventure = (KoLAdventure) location.rawValue();
    AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();

    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);

    Map<MonsterData, MonsterData> classMap =
        MonsterDatabase.getMonsterClassMap(KoLCharacter.getAscensionClass());
    Map<MonsterData, MonsterData> pathMap =
        MonsterDatabase.getMonsterPathMap(KoLCharacter.getPath().getName());

    int monsterCount = data == null ? 0 : data.getMonsterCount();
    for (int i = 0; i < monsterCount; ++i) {
      MonsterData mon = mapMonster(data.getMonster(i), classMap, pathMap);
      value.aset(DataTypes.makeMonsterValue(mon), DataTypes.TRUE_VALUE);
    }

    int superlikelyMonsterCount = data == null ? 0 : data.getSuperlikelyMonsterCount();
    for (int i = 0; i < superlikelyMonsterCount; ++i) {
      MonsterData mon = mapMonster(data.getSuperlikelyMonster(i), classMap, pathMap);
      value.aset(DataTypes.makeMonsterValue(mon), DataTypes.TRUE_VALUE);
    }

    return value;
  }

  public static Value get_locket_monsters(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);

    for (var id : LocketManager.getMonsters()) {
      var monster = MonsterDatabase.findMonsterById(id);
      var fought = LocketManager.foughtMonster(id);
      value.aset(DataTypes.makeMonsterValue(monster), DataTypes.makeBooleanValue(!fought));
    }

    return value;
  }

  public static Value get_permed_skills(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.SKILL_TYPE);
    MapValue value = new MapValue(type);

    for (var permedSkill : KoLConstants.permedSkills) {
      var skill = DataTypes.makeSkillValue(permedSkill.getSkillId(), true);
      var hardcore =
          DataTypes.makeBooleanValue(
              KoLConstants.hardcorePermedSkills.contains(permedSkill.getSkillId()));
      value.aset(skill, hardcore);
    }
    return value;
  }

  public static Value get_monster_mapping(ScriptRuntime controller) {
    return get_monster_mapping(controller, KoLCharacter.getPath().getName());
  }

  public static Value get_monster_mapping(ScriptRuntime controller, final Value path) {
    return get_monster_mapping(controller, path.toString());
  }

  private static Value get_monster_mapping(ScriptRuntime controller, final String path) {
    AggregateType type = new AggregateType(DataTypes.MONSTER_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);

    Map<MonsterData, MonsterData> mapping = MonsterDatabase.getMonsterPathMap(path);
    if (mapping != null) {
      for (Map.Entry<MonsterData, MonsterData> entry : mapping.entrySet()) {
        MonsterData mon1 = entry.getKey();
        MonsterData mon2 = entry.getValue();
        value.aset(DataTypes.makeMonsterValue(mon1), DataTypes.makeMonsterValue(mon2));
      }
    }
    return value;
  }

  public static Value appearance_rates(ScriptRuntime controller, final Value location) {
    return appearance_rates(controller, location, DataTypes.makeBooleanValue(false));
  }

  public static Value appearance_rates(
      ScriptRuntime controller, final Value location, final Value includeQueue) {
    KoLAdventure adventure = (KoLAdventure) location.rawValue();
    AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();
    boolean stateful = includeQueue.intValue() == 1;

    AggregateType type = new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);
    if (data == null) return value;

    value.aset(
        DataTypes.MONSTER_INIT,
        new Value(data.combats() < 0 ? -1.0F : 100.0f - data.areaCombatPercent()));

    for (Map.Entry<MonsterData, Double> entry : data.getMonsterData(stateful).entrySet()) {
      value.aset(DataTypes.makeMonsterValue(entry.getKey()), new Value(entry.getValue()));
    }

    return value;
  }

  public static Value expected_damage(ScriptRuntime controller) {
    return expected_damage(
        controller,
        MonsterStatusTracker.getLastMonster(),
        MonsterStatusTracker.getMonsterAttackModifier());
  }

  public static Value expected_damage(ScriptRuntime controller, final Value arg) {
    return expected_damage(controller, (MonsterData) arg.rawValue(), 0);
  }

  private static Value expected_damage(
      ScriptRuntime controller, MonsterData monster, int attackModifier) {
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    // https://wiki.kingdomofloathing.com/Damage
    int attack = monster.getAttack() + attackModifier;
    int defenseStat = KoLCharacter.getAdjustedMoxie();

    if (KoLCharacter.hasSkill(SkillPool.HERO_OF_THE_HALF_SHELL)
        && EquipmentManager.usingShield()
        && KoLCharacter.getAdjustedMuscle() > defenseStat) {
      defenseStat = KoLCharacter.getAdjustedMuscle();
    }
    int baseValue;
    double damageAbsorb;
    double elementAbsorb;

    // https://wiki.kingdomofloathing.com/Ninja_snowman_assassin
    if (monster.getName().equals("ninja snowman assassin")) {
      baseValue = Math.max(0, attack - defenseStat) + 120;
      damageAbsorb =
          1.0 - (Math.sqrt(Math.min(1000, KoLCharacter.getDamageAbsorption()) / 10.0) - 1.0) / 10.0;
      int modifiedRes = Math.max(0, KoLCharacter.getElementalResistanceLevels(Element.COLD) - 5);
      elementAbsorb = 1.0 - KoLCharacter.elementalResistanceByLevel(modifiedRes, true) / 100.0;
    } else {
      baseValue =
          Math.max(0, attack - defenseStat) + attack / 4 - KoLCharacter.getDamageReduction();
      damageAbsorb =
          1.0 - (Math.sqrt(Math.min(1000, KoLCharacter.getDamageAbsorption()) / 10.0) - 1.0) / 10.0;
      elementAbsorb = 1.0 - KoLCharacter.getElementalResistance(monster.getAttackElement()) / 100.0;
    }

    return new Value((int) Math.ceil(baseValue * damageAbsorb * elementAbsorb));
  }

  public static Value monster_level_adjustment(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMonsterLevelAdjustment());
  }

  public static Value weight_adjustment(ScriptRuntime controller) {
    return new Value(KoLCharacter.getFamiliarWeightAdjustment());
  }

  public static Value mana_cost_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getManaCostAdjustment());
  }

  public static Value combat_mana_cost_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getManaCostAdjustment(true));
  }

  public static Value raw_damage_absorption(ScriptRuntime controller) {
    return new Value(KoLCharacter.getDamageAbsorption());
  }

  public static Value damage_absorption_percent(ScriptRuntime controller) {
    int raw = Math.min(1000, KoLCharacter.getDamageAbsorption());
    if (raw == 0) {
      return DataTypes.ZERO_FLOAT_VALUE;
    }

    // http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
    // ( sqrt( raw / 10 ) - 1 ) / 10

    double percent = (Math.sqrt(raw / 10.0) - 1.0) * 10.0;
    return new Value(percent);
  }

  public static Value damage_reduction(ScriptRuntime controller) {
    return new Value(KoLCharacter.getDamageReduction());
  }

  public static Value elemental_resistance(ScriptRuntime controller) {
    return new Value(
        KoLCharacter.getElementalResistance(MonsterStatusTracker.getMonsterAttackElement()));
  }

  public static Value elemental_resistance(ScriptRuntime controller, final Value arg) {
    if (arg.getType().equals(TypeSpec.ELEMENT)) {
      String elementName = arg.toString();
      Element elem = Element.fromString(elementName);
      return new Value(KoLCharacter.getElementalResistance(elem));
    }

    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(KoLCharacter.getElementalResistance(monster.getAttackElement()));
  }

  public static Value combat_rate_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getCombatRateAdjustment());
  }

  public static Value initiative_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getInitiativeAdjustment());
  }

  public static Value experience_bonus(ScriptRuntime controller) {
    return new Value(KoLCharacter.getExperienceAdjustment());
  }

  public static Value meat_drop_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getMeatDropPercentAdjustment());
  }

  public static Value item_drop_modifier(ScriptRuntime controller) {
    return new Value(KoLCharacter.getItemDropPercentAdjustment());
  }

  public static Value buffed_hit_stat(ScriptRuntime controller) {
    int hitStat = EquipmentManager.getAdjustedHitStat();
    return new Value(hitStat);
  }

  public static Value current_hit_stat(ScriptRuntime controller) {
    return EquipmentManager.getHitStatType() == Stat.MOXIE
        ? DataTypes.MOXIE_VALUE
        : DataTypes.MUSCLE_VALUE;
  }

  public static Value current_round(ScriptRuntime controller) {
    return new Value(FightRequest.getCurrentRound());
  }

  public static Value monster_element(ScriptRuntime controller) {
    Element element = MonsterStatusTracker.getMonsterDefenseElement();
    return new Value(DataTypes.ELEMENT_TYPE, element.toString(), element);
  }

  public static Value monster_element(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ELEMENT_INIT;
    }

    Element element = monster.getDefenseElement();
    return new Value(DataTypes.ELEMENT_TYPE, element.toString(), element);
  }

  public static Value monster_attack(ScriptRuntime controller) {
    return new Value(MonsterStatusTracker.getMonsterAttack());
  }

  public static Value monster_attack(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(monster.getAttack());
  }

  public static Value monster_defense(ScriptRuntime controller) {
    return new Value(MonsterStatusTracker.getMonsterDefense());
  }

  public static Value monster_defense(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(monster.getDefense());
  }

  public static Value monster_initiative(ScriptRuntime controller) {
    return new Value(MonsterStatusTracker.getMonsterInitiative());
  }

  public static Value monster_initiative(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(monster.getInitiative());
  }

  public static Value monster_hp(ScriptRuntime controller) {
    return new Value(MonsterStatusTracker.getMonsterHealth());
  }

  public static Value monster_hp(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.ZERO_VALUE;
    }

    return new Value(monster.getHP());
  }

  public static Value monster_phylum(ScriptRuntime controller) {
    Phylum phylum = MonsterStatusTracker.getMonsterPhylum();
    return new Value(DataTypes.PHYLUM_TYPE, phylum.toString(), phylum);
  }

  public static Value monster_phylum(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.PHYLUM_INIT;
    }

    Phylum phylum = monster.getPhylum();
    return new Value(DataTypes.PHYLUM_TYPE, phylum.toString(), phylum);
  }

  public static Value is_banished(ScriptRuntime controller, final Value arg) {
    if (arg.getType().equals(TypeSpec.MONSTER)) {
      MonsterData monster = (MonsterData) arg.rawValue();
      if (monster == null) {
        return DataTypes.FALSE_VALUE;
      }
      return DataTypes.makeBooleanValue(BanishManager.isBanished(monster.getName()));
    } else if (arg.getType().equals(TypeSpec.PHYLUM)) {
      Phylum phylum = (Phylum) arg.rawValue();
      if (phylum == null) {
        return DataTypes.FALSE_VALUE;
      }
      return DataTypes.makeBooleanValue(BanishManager.isBanishedPhylum(phylum));
    } else {
      return DataTypes.FALSE_VALUE;
    }
  }

  public static Value banished_by(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    var banishedBy = BanishManager.banishedBy(monster);

    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, banishedBy.length);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < banishedBy.length; i++) {
      value.aset(new Value(i), DataTypes.makeStringValue(banishedBy[i].getName()));
    }

    return value;
  }

  public static Value track_copy_count(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeIntValue(TrackManager.countCopies(monster.getName()));
  }

  public static Value track_ignore_queue(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeBooleanValue(TrackManager.isQueueIgnored(monster.getName()));
  }

  public static Value tracked_by(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    var trackedBy = TrackManager.trackedBy(monster);

    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, trackedBy.length);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < trackedBy.length; i++) {
      value.aset(new Value(i), DataTypes.makeStringValue(trackedBy[i].getName()));
    }

    return value;
  }

  public static Value jump_chance(ScriptRuntime controller) {
    return new Value(MonsterStatusTracker.getJumpChance());
  }

  public static Value jump_chance(ScriptRuntime controller, final Value arg) {
    if (arg.getType().equals(TypeSpec.MONSTER)) {
      MonsterData monster = (MonsterData) arg.rawValue();
      if (monster == null) {
        return DataTypes.ZERO_VALUE;
      }
      return new Value(monster.getJumpChance());
    }

    if (arg.getType().equals(TypeSpec.LOCATION)) {
      KoLAdventure adventure = (KoLAdventure) arg.rawValue();
      AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();
      return new Value(data.getJumpChance());
    }
    return DataTypes.ZERO_VALUE;
  }

  public static Value jump_chance(ScriptRuntime controller, final Value arg, final Value init) {
    int initiative = (int) init.intValue();

    if (arg.getType().equals(TypeSpec.MONSTER)) {
      MonsterData monster = (MonsterData) arg.rawValue();
      if (monster == null) {
        return DataTypes.ZERO_VALUE;
      }
      return new Value(monster.getJumpChance(initiative));
    }

    if (arg.getType().equals(TypeSpec.LOCATION)) {
      KoLAdventure adventure = (KoLAdventure) arg.rawValue();
      AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();
      if (data == null) {
        return DataTypes.ZERO_VALUE;
      }
      return new Value(data.getJumpChance(initiative));
    }
    return DataTypes.ZERO_VALUE;
  }

  public static Value jump_chance(
      ScriptRuntime controller, final Value arg, final Value init, final Value ml) {
    int initiative = (int) init.intValue();
    int monsterLevel = (int) ml.intValue();
    if (arg.getType().equals(TypeSpec.MONSTER)) {
      MonsterData monster = (MonsterData) arg.rawValue();
      if (monster == null) {
        return DataTypes.ZERO_VALUE;
      }
      return new Value(monster.getJumpChance(initiative, monsterLevel));
    }

    if (arg.getType().equals(TypeSpec.LOCATION)) {
      KoLAdventure adventure = (KoLAdventure) arg.rawValue();
      AreaCombatData data = adventure == null ? null : adventure.getAreaSummary();
      if (data == null) {
        return DataTypes.ZERO_VALUE;
      }
      return new Value(data.getJumpChance(initiative, monsterLevel));
    }

    return DataTypes.ZERO_VALUE;
  }

  public static Value item_drops(ScriptRuntime controller) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    return item_drops(monster);
  }

  public static Value item_drops(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    return item_drops(monster);
  }

  private static Value item_drops(MonsterData monster) {
    List<MonsterDrop> data = monster == null ? new ArrayList<>() : monster.getItems();

    MapValue value = new MapValue(DataTypes.ITEM_TO_FLOAT_TYPE);

    for (MonsterDrop result : data) {
      value.aset(
          DataTypes.makeItemValue(result.item().getItemId(), true), new Value(result.chance()));
    }

    return value;
  }

  public static Value item_drops_array(ScriptRuntime controller) {
    return item_drops_array(controller, MonsterStatusTracker.getLastMonster());
  }

  public static Value item_drops_array(ScriptRuntime controller, final Value arg) {
    return item_drops_array(controller, (MonsterData) arg.rawValue());
  }

  public static Value item_drops_array(ScriptRuntime controller, MonsterData monster) {
    List<MonsterDrop> data = monster == null ? new ArrayList<>() : monster.getItems();
    int dropCount = data.size();
    AggregateType type = new AggregateType(RuntimeLibrary.itemDropRec, dropCount);
    ArrayValue value = new ArrayValue(type);
    for (int i = 0; i < dropCount; ++i) {
      MonsterDrop result = data.get(i);
      DropFlag dropType = result.flag();
      RecordValue rec = (RecordValue) value.aref(new Value(i));

      rec.aset(0, DataTypes.makeItemValue(result.item().getItemId(), true), null);
      rec.aset(1, new Value(result.chance()), null);
      if (dropType != DropFlag.NONE) { // leave as an empty string if no special type was given
        rec.aset(2, new Value(dropType.toString()), null);
      }
    }

    return value;
  }

  public static Value svn_list(ScriptRuntime controller) {
    String[] projects = KoLConstants.SVN_LOCATION.list();

    int projectCount = projects != null ? projects.length : 0;

    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, projectCount);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < projectCount; ++i) {
      value.aset(new Value(i), new Value(projects[i]));
    }

    return value;
  }

  public static Value git_list(ScriptRuntime controller) {
    return DataTypes.makeStringArrayValue(GitManager.listAll());
  }

  public static Value svn_info(ScriptRuntime controller, final Value script) {
    AshRuntime interpreter = controller instanceof AshRuntime ? (AshRuntime) controller : null;

    String[] projects = KoLConstants.SVN_LOCATION.list();

    if (projects == null) return getRecInit(interpreter);

    ArrayList<String> matches = new ArrayList<>();
    for (String s : projects) {
      if (s.contains(script.toString())) {
        matches.add(s);
      }
    }

    if (matches.size() != 1) {
      return getRecInit(interpreter);
    }
    File projectFile = new File(KoLConstants.SVN_LOCATION, matches.get(0));
    try {
      if (!SVNWCUtil.isWorkingCopyRoot(projectFile)) {
        return getRecInit(interpreter);
      }
    } catch (SVNException e1) {
      return getRecInit(interpreter);
    }
    RecordType type = RuntimeLibrary.svnInfoRec;
    RecordValue rec = new RecordValue(type);

    // get info

    SVNInfo info;
    try {
      info = SVNManager.doInfo(projectFile);
    } catch (SVNException e) {
      SVNManager.error(e, null);
      return getRecInit(interpreter);
    }

    // URL
    rec.aset(0, new Value(info.getURL().toString()), interpreter);
    // revision
    rec.aset(1, DataTypes.makeIntValue(info.getRevision().getNumber()), interpreter);
    // lastChangedAuthor
    rec.aset(2, new Value(info.getAuthor()), interpreter);
    // lastChangedRev
    rec.aset(3, DataTypes.makeIntValue(info.getCommittedRevision().getNumber()), interpreter);
    // lastChangedDate
    // use format that is similar to what 'svn info' gives, ex:
    // Last Changed Date: 2003-01-16 23:21:19 -0600 (Thu, 16 Jan 2003)
    SimpleDateFormat SVN_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z (EEE, dd MMM yyyy)", Locale.US);
    rec.aset(4, new Value(SVN_FORMAT.format(info.getCommittedDate())), interpreter);

    return rec;
  }

  private static RecordValue getRecInit(AshRuntime interpreter) {
    RecordType type = RuntimeLibrary.svnInfoRec;
    RecordValue rec = new RecordValue(type);
    rec.aset(0, DataTypes.STRING_INIT, interpreter);
    // revision
    rec.aset(1, DataTypes.INT_INIT, interpreter);
    // lastChangedAuthor
    rec.aset(2, DataTypes.STRING_INIT, interpreter);
    // lastChangedRev
    rec.aset(3, DataTypes.INT_INIT, interpreter);
    // lastChangedDate
    rec.aset(4, DataTypes.STRING_INIT, interpreter);

    return rec;
  }

  public static Value git_info(ScriptRuntime controller, final Value script) {
    AshRuntime interpreter = controller instanceof AshRuntime ? (AshRuntime) controller : null;

    var infoOpt = GitManager.getInfo(script.toString());
    if (infoOpt.isEmpty()) return getGitRecInit(interpreter);
    var info = infoOpt.get();

    RecordValue rec = new RecordValue(RuntimeLibrary.gitInfoRec);

    // URL
    rec.aset(0, new Value(info.url()), interpreter);
    // branch
    rec.aset(1, new Value(info.branch()), interpreter);
    // revision
    rec.aset(2, new Value(info.commit()), interpreter);
    // lastChangedAuthor
    rec.aset(3, new Value(info.lastChangedAuthor()), interpreter);
    // lastChangedDate
    var date = GitManager.formatCommitDate(info.lastChangedDate());
    rec.aset(4, new Value(date), interpreter);

    return rec;
  }

  private static RecordValue getGitRecInit(AshRuntime interpreter) {
    RecordValue rec = new RecordValue(RuntimeLibrary.gitInfoRec);
    rec.aset(0, DataTypes.STRING_INIT, interpreter);
    rec.aset(1, DataTypes.STRING_INIT, interpreter);
    rec.aset(2, DataTypes.STRING_INIT, interpreter);
    rec.aset(3, DataTypes.STRING_INIT, interpreter);
    rec.aset(4, DataTypes.STRING_INIT, interpreter);

    return rec;
  }

  public static Value meat_drop(ScriptRuntime controller) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return new Value(-1);
    }

    return new Value(monster.getBaseMeat());
  }

  public static Value meat_drop(ScriptRuntime controller, final Value arg) {
    MonsterData monster = (MonsterData) arg.rawValue();
    if (monster == null) {
      return new Value(-1);
    }

    return new Value(monster.getBaseMeat());
  }

  public static Value will_usually_dodge(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(MonsterStatusTracker.willUsuallyDodge());
  }

  public static Value will_usually_miss(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(MonsterStatusTracker.willUsuallyMiss());
  }

  public static Value dad_sea_monkee_weakness(ScriptRuntime controller, final Value arg) {
    DadManager.Element element = DadManager.weakness((int) arg.intValue());
    return switch (element) {
      case HOT -> new Value(DataTypes.ELEMENT_TYPE, "hot", element);
      case COLD -> new Value(DataTypes.ELEMENT_TYPE, "cold", element);
      case STENCH -> new Value(DataTypes.ELEMENT_TYPE, "stench", element);
      case SPOOKY -> new Value(DataTypes.ELEMENT_TYPE, "spooky", element);
      case SLEAZE -> new Value(DataTypes.ELEMENT_TYPE, "sleaze", element);
      default -> DataTypes.ELEMENT_INIT;
    };
  }

  public static Value unusual_construct_disc(ScriptRuntime controller) {
    return DataTypes.makeItemValue(UnusualConstructManager.disc(), true);
  }

  public static Value flush_monster_manuel_cache(ScriptRuntime controller) {
    MonsterManuelManager.flushCache();
    return DataTypes.TRUE_VALUE;
  }

  public static Value monster_manuel_text(ScriptRuntime controller, final Value arg) {
    return new Value(MonsterManuelManager.getManuelText((int) arg.intValue()));
  }

  public static Value monster_factoids_available(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    return new Value(
        MonsterManuelManager.getFactoidsAvailable((int) arg1.intValue(), arg2.intValue() != 0));
  }

  public static Value all_monsters_with_id(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);

    for (Integer id : MonsterDatabase.idKeySet()) {
      if (id == 0) continue;
      Value v = DataTypes.makeMonsterValue(id, false);
      if (v != null) value.aset(v, DataTypes.TRUE_VALUE);
    }

    return value;
  }

  private static ModifierType getModifierType(final Value arg) {
    Type type = arg.getType();
    String name = arg.toString();
    int id = (int) arg.intValue();
    if (type.equals(DataTypes.ITEM_TYPE)) {
      return ModifierType.ITEM;
    }
    if (type.equals(DataTypes.EFFECT_TYPE)) {
      return ModifierType.EFFECT;
    }
    if (type.equals(DataTypes.SKILL_TYPE)) {
      return ModifierType.SKILL;
    }
    if (type.equals(DataTypes.THRALL_TYPE)) {
      return ModifierType.THRALL;
    }
    if (name.contains(":")) {
      ModifierType modifierType = ModifierType.fromString(name.substring(0, name.indexOf(":")));
      if (modifierType != null) return modifierType;
    }
    return ModifierType.ITEM;
  }

  private static String getModifierName(final Value arg) {
    Type type = arg.getType();
    String name = arg.toString();
    int id = (int) arg.intValue();
    if (type.equals(DataTypes.ITEM_TYPE)
        || type.equals(DataTypes.SKILL_TYPE)
        || type.equals(DataTypes.EFFECT_TYPE)) {
      return "[" + id + "]";
    }
    int index = name.indexOf(":");
    if (index != -1) {
      return name.substring(index + 1);
    }
    return name;
  }

  private static Modifier getNumericModifier(ScriptRuntime controller, final Value modifier) {
    Type type = modifier.getType();
    if (type.equals(DataTypes.MODIFIER_TYPE)) {
      Modifier content = (Modifier) modifier.content;
      if (content != null && content.getType() == ModifierValueType.NUMERIC) {
        return content;
      }
      throw controller.runtimeException("numeric modifier required");
    }
    String mod = modifier.toString();
    return ModifierDatabase.numericByCaselessName(mod);
  }

  private static BooleanModifier getBooleanModifier(
      ScriptRuntime controller, final Value modifier) {
    Type type = modifier.getType();
    if (type.equals(DataTypes.MODIFIER_TYPE)) {
      Modifier content = (Modifier) modifier.content;
      if (content != null && content.getType() == ModifierValueType.BOOLEAN) {
        return (BooleanModifier) content;
      }
      throw controller.runtimeException("boolean modifier required");
    }
    String mod = modifier.toString();
    return BooleanModifier.byCaselessName(mod);
  }

  private static Modifier getStringModifier(ScriptRuntime controller, final Value modifier) {
    Type type = modifier.getType();
    if (type.equals(DataTypes.MODIFIER_TYPE)) {
      Modifier content = (Modifier) modifier.content;
      if (content != null) {
        switch (content.getType()) {
          case STRING -> {
            return (StringModifier) content;
          }
          case MULTISTRING -> {
            return (MultiStringModifier) content;
          }
        }
      }
      throw controller.runtimeException("string modifier required");
    }
    String mod = modifier.toString();
    var str = StringModifier.byCaselessName(mod);
    if (str != null) {
      return str;
    }
    return MultiStringModifier.byCaselessName(mod);
  }

  private static MultiStringModifier getMultiStringModifier(
      ScriptRuntime controller, final Value modifier) {
    Type type = modifier.getType();
    if (type.equals(DataTypes.MODIFIER_TYPE)) {
      Modifier content = (Modifier) modifier.content;
      if (content != null && content.getType() == ModifierValueType.MULTISTRING) {
        return (MultiStringModifier) content;
      }
      throw controller.runtimeException("string modifier required");
    }
    String mod = modifier.toString();
    return MultiStringModifier.byCaselessName(mod);
  }

  public static Value numeric_modifier(ScriptRuntime controller, final Value modifier) {
    Modifier realMod = getNumericModifier(controller, modifier);
    return new Value(KoLCharacter.currentNumericModifier(realMod));
  }

  public static Value numeric_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    Modifier realMod = getNumericModifier(controller, modifier);
    return new Value(ModifierDatabase.getNumericModifier(type, name, realMod));
  }

  public static Value numeric_modifier(
      ScriptRuntime controller,
      final Value familiar,
      final Value modifier,
      final Value weight,
      final Value item) {
    FamiliarData fam = new FamiliarData((int) familiar.intValue());
    String mod = modifier.toString();
    Modifier realMod = ModifierDatabase.numericByCaselessName(mod);
    int w = Math.max(1, (int) weight.intValue());
    AdventureResult it = ItemPool.get((int) item.intValue());

    return new Value(ModifierDatabase.getNumericModifier(fam, realMod, w, it));
  }

  public static Value boolean_modifier(ScriptRuntime controller, final Value modifier) {
    BooleanModifier mod = getBooleanModifier(controller, modifier);
    return DataTypes.makeBooleanValue(KoLCharacter.currentBooleanModifier(mod));
  }

  public static Value boolean_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    BooleanModifier boolMod = getBooleanModifier(controller, modifier);
    return DataTypes.makeBooleanValue(ModifierDatabase.getBooleanModifier(type, name, boolMod));
  }

  public static Value string_modifier(ScriptRuntime controller, final Value modifier) {
    var strMod = getStringModifier(controller, modifier);
    return new Value(KoLCharacter.currentStringModifier(strMod));
  }

  public static Value string_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(ModifierDatabase.getStringModifier(type, name, strMod));
  }

  public static Value strings_modifier(ScriptRuntime controller, final Value modifier) {
    var mStrMod = getMultiStringModifier(controller, modifier);

    var values = KoLCharacter.currentMultiStringModifier(mStrMod);
    ArrayValue value = new ArrayValue(new AggregateType(DataTypes.STRING_TYPE, values.size()));

    int i = 0;
    for (var str : values) {
      value.aset(DataTypes.makeIntValue(i++), new Value(str));
    }

    return value;
  }

  public static Value strings_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    var mStrMod = getMultiStringModifier(controller, modifier);

    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);

    var values = ModifierDatabase.getMultiStringModifier(type, name, mStrMod);
    ArrayValue value = new ArrayValue(new AggregateType(DataTypes.STRING_TYPE, values.size()));

    int i = 0;
    for (var str : values) {
      value.aset(DataTypes.makeIntValue(i++), new Value(str));
    }

    return value;
  }

  public static Value effect_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(
        DataTypes.parseEffectValue(ModifierDatabase.getStringModifier(type, name, strMod), true));
  }

  public static Value effects_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    var mStrMod = getMultiStringModifier(controller, modifier);

    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);

    var values = ModifierDatabase.getMultiStringModifier(type, name, mStrMod);
    ArrayValue value = new ArrayValue(new AggregateType(DataTypes.EFFECT_TYPE, values.size()));

    int i = 0;
    for (var eff : values) {
      Value effect = DataTypes.parseEffectValue(eff, true);
      value.aset(DataTypes.makeIntValue(i++), effect);
    }

    return value;
  }

  public static Value class_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(
        DataTypes.parseClassValue(ModifierDatabase.getStringModifier(type, name, strMod), true));
  }

  public static Value skill_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(
        DataTypes.parseSkillValue(ModifierDatabase.getStringModifier(type, name, strMod), true));
  }

  public static Value skills_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    var mStrMod = getMultiStringModifier(controller, modifier);

    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);

    var values = ModifierDatabase.getMultiStringModifier(type, name, mStrMod);
    ArrayValue value = new ArrayValue(new AggregateType(DataTypes.SKILL_TYPE, values.size()));

    int i = 0;
    for (var sk : values) {
      Value skill = DataTypes.parseSkillValue(sk, true);
      value.aset(DataTypes.makeIntValue(i++), skill);
    }

    return value;
  }

  public static Value stat_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(
        DataTypes.parseStatValue(ModifierDatabase.getStringModifier(type, name, strMod), true));
  }

  public static Value monster_modifier(
      ScriptRuntime controller, final Value arg, final Value modifier) {
    ModifierType type = RuntimeLibrary.getModifierType(arg);
    String name = RuntimeLibrary.getModifierName(arg);
    var strMod = getStringModifier(controller, modifier);
    return new Value(
        DataTypes.parseMonsterValue(ModifierDatabase.getStringModifier(type, name, strMod), true));
  }

  public static Value split_modifiers(ScriptRuntime controller, final Value arg) {
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, DataTypes.MODIFIER_TYPE);
    MapValue value = new MapValue(type);

    for (ModifierValue mVal : ModifierDatabase.splitModifiers(arg.toString())) {
      var modifierName = mVal.getName();
      var modifier = ModifierDatabase.getModifierByName(modifierName);
      if (modifier == null) {
        // splitModifiers doesn't validate the passed-in string, so just drop it
        continue;
      }
      value.aset(new Value(modifier), new Value(mVal.getValue()));
    }

    return value;
  }

  public static Value white_citadel_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(QuestLogRequest.isWhiteCitadelAvailable());
  }

  public static Value friars_available(ScriptRuntime controller) {
    if (QuestLogRequest.areFriarsAvailable())
      Preferences.setInteger(
          "lastFriarCeremonyAscension", Preferences.getInteger("knownAscensions"));
    return DataTypes.makeBooleanValue(QuestLogRequest.areFriarsAvailable());
  }

  public static Value black_market_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(QuestLogRequest.isBlackMarketAvailable());
  }

  public static Value hippy_store_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(QuestLogRequest.isHippyStoreAvailable());
  }

  public static Value dispensary_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.getDispensaryOpen());
  }

  public static Value guild_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(GuildUnlockManager.canUnlockGuild());
  }

  public static Value guild_store_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.getGuildStoreOpen());
  }

  public static Value hidden_temple_unlocked(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.getTempleUnlocked());
  }

  public static Value knoll_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.knollAvailable());
  }

  public static Value canadia_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.canadiaAvailable());
  }

  public static Value gnomads_available(ScriptRuntime controller) {
    return DataTypes.makeBooleanValue(KoLCharacter.gnomadsAvailable());
  }

  public static Value florist_available(ScriptRuntime controller) {
    if (!Preferences.getBoolean("floristFriarChecked")) {
      FloristRequest.checkFloristAvailable();
    }
    return DataTypes.makeBooleanValue(FloristRequest.haveFlorist());
  }

  public static Value is_trendy(ScriptRuntime controller, final Value thing) {
    // Types: "Items", "Campground", "Bookshelf", "Familiars", "Skills", "Clan Item".
    String key = thing.toString();
    Type type = thing.getType();
    boolean result;

    if (type.equals(TypeSpec.STRING)) {

      result =
          TrendyRequest.isTrendy(RestrictedItemType.ITEMS, key)
              && TrendyRequest.isTrendy(RestrictedItemType.CAMPGROUND, key)
              && TrendyRequest.isTrendy(RestrictedItemType.BOOKSHELF_BOOKS, key)
              && TrendyRequest.isTrendy(RestrictedItemType.FAMILIARS, key)
              && TrendyRequest.isTrendy(RestrictedItemType.SKILLS, key)
              && TrendyRequest.isTrendy(RestrictedItemType.CLAN_ITEMS, key);
    } else if (type.equals(TypeSpec.ITEM)) {
      result = TrendyRequest.isTrendy(RestrictedItemType.ITEMS, key);
    } else if (type.equals(TypeSpec.FAMILIAR)) {
      result = TrendyRequest.isTrendy(RestrictedItemType.FAMILIARS, key);
    } else if (type.equals(TypeSpec.SKILL)) {
      if (SkillDatabase.isBookshelfSkill(key)) {
        int itemId = SkillDatabase.skillToBook(key);
        key = ItemDatabase.getItemName(itemId);
        result = TrendyRequest.isTrendy(RestrictedItemType.BOOKSHELF_BOOKS, key);
      } else {
        result = TrendyRequest.isTrendy(RestrictedItemType.SKILLS, key);
      }
    } else {
      result = false;
    }

    return DataTypes.makeBooleanValue(result);
  }

  public static Value is_unrestricted(ScriptRuntime controller, final Value thing) {
    // Types: "Items", "Bookshelf Books", "Skills", "Familiars", "Clan Items".
    String key = thing.toString();
    Type type = thing.getType();
    boolean result;

    if (type.equals(TypeSpec.STRING)) {
      result =
          StandardRequest.isNotRestricted(RestrictedItemType.ITEMS, key)
              && StandardRequest.isNotRestricted(RestrictedItemType.BOOKSHELF_BOOKS, key)
              && StandardRequest.isNotRestricted(RestrictedItemType.SKILLS, key)
              && StandardRequest.isNotRestricted(RestrictedItemType.FAMILIARS, key)
              && StandardRequest.isNotRestricted(RestrictedItemType.CLAN_ITEMS, key);
    } else if (type.equals(TypeSpec.ITEM)) {
      result = StandardRequest.isNotRestricted(RestrictedItemType.ITEMS, key);
    } else if (type.equals(TypeSpec.FAMILIAR)) {
      result = StandardRequest.isNotRestricted(RestrictedItemType.FAMILIARS, key);
    } else if (type.equals(TypeSpec.SKILL)) {
      if (SkillDatabase.isBookshelfSkill(key)) {
        int itemId = SkillDatabase.skillToBook(key);
        key = ItemDatabase.getItemName(itemId);
        // Work around a KoL bug: most restricted books are
        // listed both under Bookshelf Books and Items, but
        // 3 are listed under only one or the other.
        result =
            StandardRequest.isNotRestricted(RestrictedItemType.BOOKSHELF_BOOKS, key)
                && StandardRequest.isNotRestricted(RestrictedItemType.ITEMS, key);
      } else {
        result = StandardRequest.isNotRestricted(RestrictedItemType.SKILLS, key);
      }
    } else {
      result = false;
    }

    return DataTypes.makeBooleanValue(result);
  }

  public static Value svn_exists(ScriptRuntime controller, final Value project) {
    File f = new File(KoLConstants.SVN_LOCATION, project.toString());

    boolean isWCRoot = false;
    try {
      isWCRoot = SVNWCUtil.isWorkingCopyRoot(f);
    } catch (SVNException e) {
      StaticEntity.printStackTrace(e);
    }
    return DataTypes.makeBooleanValue(isWCRoot);
  }

  public static Value git_exists(ScriptRuntime controller, final Value project) {
    var isValid = GitManager.isValidRepo(project.toString());
    return DataTypes.makeBooleanValue(isValid);
  }

  public static Value svn_at_head(ScriptRuntime controller, final Value project) {
    File f = new File(KoLConstants.SVN_LOCATION, project.toString());

    if (!f.exists() || !f.isDirectory()) {
      return DataTypes.FALSE_VALUE;
    }
    return DataTypes.makeBooleanValue(SVNManager.WCAtHead(f, true));
  }

  public static Value git_at_head(ScriptRuntime controller, final Value project) {
    var isUpToDate = GitManager.isUpToDate(project.toString());
    return DataTypes.makeBooleanValue(isUpToDate);
  }

  // Sweet Synthesis

  public static Value update_candy_prices(ScriptRuntime controller) {
    CandyDatabase.updatePrices();
    return DataTypes.VOID_VALUE;
  }

  public static Value candy_for_tier(ScriptRuntime controller, final Value arg) {
    return RuntimeLibrary.candy_for_tier(
        controller, arg, DataTypes.makeIntValue(CandyDatabase.defaultFlags()));
  }

  public static Value candy_for_tier(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int tier = (int) arg1.intValue();
    int flags = (int) arg2.intValue();

    if ((flags & CandyDatabase.FLAG_NO_BLACKLIST) == 0) {
      CandyDatabase.loadBlacklist();
    }

    Set<Integer> candies = CandyDatabase.candyForTier(tier, flags);

    int count = (candies == null) ? 0 : candies.size();

    AggregateType type = new AggregateType(DataTypes.ITEM_TYPE, count);
    ArrayValue value = new ArrayValue(type);

    if (candies != null) {
      int index = 0;
      for (Integer itemId : candies) {
        Value key = new Value(index++);
        Value val = DataTypes.makeItemValue(itemId, true);
        value.aset(key, val);
      }
    }

    return value;
  }

  public static Value sweet_synthesis_pairing(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    return RuntimeLibrary.sweet_synthesis_pairing(
        controller, arg1, arg2, DataTypes.makeIntValue(CandyDatabase.defaultFlags()));
  }

  public static Value sweet_synthesis_pairing(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    int effectId = (int) arg1.intValue();
    int itemId = (int) arg2.intValue();
    int flags = (int) arg3.intValue();

    if ((flags & CandyDatabase.FLAG_NO_BLACKLIST) == 0) {
      CandyDatabase.loadBlacklist();
    }

    Set<Integer> candies = CandyDatabase.sweetSynthesisPairing(effectId, itemId, flags);

    int count = candies.size();

    AggregateType type = new AggregateType(DataTypes.ITEM_TYPE, count);
    ArrayValue value = new ArrayValue(type);

    int index = 0;
    for (Integer itemId2 : candies) {
      Value key = new Value(index++);
      Value val = DataTypes.makeItemValue(itemId2, true);
      value.aset(key, val);
    }

    return value;
  }

  public static Value sweet_synthesis_pair(ScriptRuntime controller, final Value arg1) {
    return RuntimeLibrary.sweet_synthesis_pair(
        controller, arg1, DataTypes.makeIntValue(CandyDatabase.defaultFlags()));
  }

  public static Value sweet_synthesis_pair(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    int effectId = (int) arg1.intValue();
    int flags = (int) arg2.intValue();

    if ((flags & CandyDatabase.FLAG_NO_BLACKLIST) == 0) {
      CandyDatabase.loadBlacklist();
    }

    Candy[] candies = CandyDatabase.synthesisPair(effectId, flags);

    AggregateType type = new AggregateType(DataTypes.ITEM_TYPE, 2);
    ArrayValue value = new ArrayValue(type);

    if (candies.length == 2) {
      value.aset(DataTypes.ZERO_VALUE, DataTypes.makeItemValue(candies[0].getItemId(), true));
      value.aset(DataTypes.ONE_VALUE, DataTypes.makeItemValue(candies[1].getItemId(), true));
    }

    return value;
  }

  public static Value sweet_synthesis_result(
      ScriptRuntime controller, final Value item1, final Value item2) {
    int itemId1 = (int) item1.intValue();
    int itemId2 = (int) item2.intValue();
    int effectId = CandyDatabase.synthesisResult(itemId1, itemId2);
    return effectId == -1 ? DataTypes.EFFECT_INIT : DataTypes.makeEffectValue(effectId, true);
  }

  public static Value sweet_synthesis(ScriptRuntime controller, final Value effect) {
    // one-argument forms

    // sweet_synthesis( effect )
    int count = 1;
    int effectId = (int) effect.intValue();
    int flags = CandyDatabase.defaultFlags();
    return RuntimeLibrary.synthesize_effect(controller, count, effectId, flags);
  }

  public static Value sweet_synthesis(
      ScriptRuntime controller, final Value arg1, final Value arg2) {
    // two-argument forms

    Type type1 = arg1.getType();

    // sweet_synthesis( effect, flags )
    if (type1.equals(TypeSpec.EFFECT)) {
      int effectId = (int) arg1.intValue();
      int flags = (int) arg2.intValue() | CandyDatabase.defaultFlags();
      return RuntimeLibrary.synthesize_effect(controller, 1, effectId, flags);
    }

    Type type2 = arg2.getType();

    // sweet_synthesis( count, effect )
    if (type2.equals(TypeSpec.EFFECT)) {
      int count = (int) arg1.intValue();
      int effectId = (int) arg2.intValue();
      int flags = CandyDatabase.defaultFlags();
      return RuntimeLibrary.synthesize_effect(controller, count, effectId, flags);
    }

    // sweet_synthesis( candy1, candy2 )
    int itemId1 = (int) arg1.intValue();
    int itemId2 = (int) arg2.intValue();

    if (!ItemDatabase.isCandyItem(itemId1) || !ItemDatabase.isCandyItem(itemId2)) {
      return DataTypes.FALSE_VALUE;
    }

    return RuntimeLibrary.synthesize_pair(controller, 1, itemId1, itemId2);
  }

  public static Value sweet_synthesis(
      ScriptRuntime controller, final Value arg1, final Value arg2, final Value arg3) {
    // three-argument forms

    Type type2 = arg2.getType();

    // sweet_synthesis( count, effect, flags )
    if (type2.equals(TypeSpec.EFFECT)) {
      int count = (int) arg1.intValue();
      int effectId = (int) arg2.intValue();
      int flags = (int) arg3.intValue() | CandyDatabase.defaultFlags();
      return RuntimeLibrary.synthesize_effect(controller, count, effectId, flags);
    }

    // sweet_synthesis( count, candy1, candy2 )

    int count = (int) arg1.intValue();
    int itemId1 = (int) arg2.intValue();
    int itemId2 = (int) arg3.intValue();

    if (!ItemDatabase.isCandyItem(itemId1) || !ItemDatabase.isCandyItem(itemId2)) {
      return DataTypes.FALSE_VALUE;
    }

    return RuntimeLibrary.synthesize_pair(controller, count, itemId1, itemId2);
  }

  private static Value synthesize_effect(
      ScriptRuntime controller, int count, int effectId, int flags) {
    if (count <= 0) {
      return DataTypes.FALSE_VALUE;
    }

    if ((flags & CandyDatabase.FLAG_NO_BLACKLIST) == 0) {
      CandyDatabase.loadBlacklist();
    }

    // It is always beneficial to synthesize 1 call at a time, since the "best"
    // pairing can change if you buy the last candy from a store, for example.

    while (KoLmafia.permitsContinue() && count-- > 0) {
      Candy[] candies = CandyDatabase.synthesisPair(effectId, flags);

      if (candies.length != 2) {
        return DataTypes.FALSE_VALUE;
      }

      int itemId1 = candies[0].getItemId();
      int itemId2 = candies[1].getItemId();

      RuntimeLibrary.synthesize_pair(controller, 1, itemId1, itemId2);
    }

    return RuntimeLibrary.continueValue();
  }

  private static Value synthesize_pair(
      ScriptRuntime controller, int count, int itemId1, int itemId2) {
    // SweetSynthesisRequest will retrieve the candies and fail if they are unavailable

    SweetSynthesisRequest request = new SweetSynthesisRequest(count, itemId1, itemId2);
    RequestThread.postRequest(request);
    return RuntimeLibrary.continueValue();
  }

  public static Value get_fuel(ScriptRuntime controller) {
    return new Value(CampgroundRequest.getFuel());
  }

  public static Value voting_booth_initiatives(
      ScriptRuntime controller, final Value clss, final Value path, final Value daycount) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE);
    MapValue value = new MapValue(type);

    for (ModifierValue modifier :
        VotingBoothManager.getInitiatives(
            (int) clss.intValue(), (int) path.intValue(), (int) daycount.intValue())) {
      value.aset(new Value(modifier.toString()), DataTypes.TRUE_VALUE);
    }

    return value;
  }

  public static Value beret_busking_effects(ScriptRuntime controller) {
    var power = KoLCharacter.getTotalPower();
    var cast = Preferences.getInteger("_beretBuskingUses");
    return beret_busking_effects(controller, new Value(power), new Value(cast));
  }

  public static Value beret_busking_effects(
      ScriptRuntime controller, final Value power, final Value cast) {
    var results = new ArrayList<AdventureResult>();

    // Calculate the capped power
    var cappedPower =
        Math.min(power.contentLong, 1100)
            + (long) Math.floor(Math.pow(Math.max(0, power.contentLong - 1100), 0.8));

    // $effect[none] will indicate the meat gained
    AdventureResult.addResultToList(
        results, new AdventureResult(AdventureResult.MEAT, (int) Math.ceil(cappedPower / 5.0) + 1));

    // Grab list of valid effects
    var validEffectIds =
        new ArrayList<>(
            IntStream.range(1, 2991)
                .filter(i -> EffectDatabase.getEffectName(i) != null)
                .filter(i -> EffectDatabase.getQuality(i) == EffectDatabase.GOOD)
                .filter(i -> !EffectDatabase.hasAttribute(i, "nohookah") || i == EffectPool.FISHY)
                .filter(i -> !EffectDatabase.hasAttribute(i, "notcrs"))
                .boxed()
                .toList());

    // The last entry is duplicated
    validEffectIds.add(validEffectIds.getLast());

    // Roll the effects
    var seed = cappedPower + cast.contentLong;
    var rng = new PHPMTRandom(seed);
    var total = Math.ceil(cappedPower / 100.0);
    for (int i = 0; i < total; i++) {
      var effectId = rng.pickOne(validEffectIds);
      var effect =
          new AdventureResult(
              EffectDatabase.getEffectName(effectId), effectId == EffectPool.FISHY ? 1 : 10, true);
      AdventureResult.addResultToList(results, effect);
    }

    var value = new MapValue(DataTypes.EFFECT_TO_INT_TYPE);
    for (var effect : results) {
      value.aset(
          DataTypes.makeEffectValue(effect.getEffectId(), true), new Value(effect.getCount()));
    }
    return value;
  }

  // pocket_set picked_pockets();
  public static Value picked_pockets(ScriptRuntime controller) {
    return makePocketSet(CargoCultistShortsRequest.pickedPockets);
  }

  // pocket_set picked_scraps()
  public static Value picked_scraps(ScriptRuntime controller) {
    return makePocketSet(CargoCultistShortsRequest.knownScrapPockets().keySet());
  }

  // pocket_set monster_pockets();
  public static Value monster_pockets(ScriptRuntime controller) {
    return makePocketSet(PocketDatabase.allMonsterPockets);
  }

  // pocket_set effect_pockets();
  public static Value effect_pockets(ScriptRuntime controller) {
    return makePocketSet(PocketDatabase.allEffectPockets);
  }

  // pocket_set item_pockets();
  public static Value item_pockets(ScriptRuntime controller) {
    return makePocketSet(PocketDatabase.allItemPockets);
  }

  // pocket_set stats_pockets();
  public static Value stats_pockets(ScriptRuntime controller) {
    return makePocketSet(PocketDatabase.allStatsPockets);
  }

  // pocket_list meat_pockets();
  public static Value meat_pockets(ScriptRuntime controller) {
    return makePocketList(PocketDatabase.meatPockets);
  }

  // pocket_list poem_pockets();
  public static Value poem_pockets(ScriptRuntime controller) {
    return makePocketList(PocketDatabase.poemHalfLines);
  }

  // pocket_list scrap_pockets();
  public static Value scrap_pockets(ScriptRuntime controller) {
    return makePocketList(PocketDatabase.scrapSyllables);
  }

  // pocket_set joke_pockets();
  public static Value joke_pockets(ScriptRuntime controller) {

    return makePocketSet(PocketDatabase.getPockets(PocketType.JOKE).keySet());
  }

  // pocket_set restoration_pockets();
  public static Value restoration_pockets(ScriptRuntime controller) {

    return makePocketSet(PocketDatabase.getPockets(PocketType.RESTORE).keySet());
  }

  // monster pocket_monster( pocket p );
  public static Value pocket_monster(ScriptRuntime controller, final Value pocket) {
    Pocket p = PocketDatabase.pocketByNumber((int) pocket.intValue());
    if (p instanceof MonsterPocket mp) {
      return DataTypes.makeMonsterValue(mp.getMonster());
    }
    return DataTypes.MONSTER_INIT;
  }

  // pocket_effects pocket_effects( pocket p );
  public static Value pocket_effects(ScriptRuntime controller, final Value pocket) {
    return makePocketEffects((int) pocket.intValue());
  }

  // pocket_items pocket_items( pocket p );
  public static Value pocket_items(ScriptRuntime controller, final Value pocket) {
    return makePocketItems((int) pocket.intValue());
  }

  // pocket_stats pocket_stats( pocket p );
  public static Value pocket_stats(ScriptRuntime controller, final Value pocket) {
    return makePocketStats((int) pocket.intValue());
  }

  // indexed_text pocket_scrap( pocket p );
  public static Value pocket_scrap(ScriptRuntime controller, final Value pocket) {
    return makeIndexedText((int) pocket.intValue());
  }

  // indexed_text pocket_poem( pocket p );
  public static Value pocket_poem(ScriptRuntime controller, final Value pocket) {
    return makeIndexedText((int) pocket.intValue());
  }

  // int pocket_meat( pocket p );
  public static Value pocket_meat(ScriptRuntime controller, final Value pocket) {
    return makeIndexedText((int) pocket.intValue());
  }

  // int pocket_meat( pocket p );
  public static Value pocket_joke(ScriptRuntime controller, final Value pocket) {
    Pocket p = PocketDatabase.pocketByNumber((int) pocket.intValue());
    if (p != null && p.getType() == PocketType.JOKE) {
      JokePocket jp = (JokePocket) p;
      return new Value(jp.getJoke());
    }
    return DataTypes.STRING_INIT;
  }

  // pocket_list potential_pockets( monster m );
  // pocket_list potential_pockets( effect e );
  // pocket_list potential_pockets( item e );
  // pocket_list potential_pockets( stat s );
  public static Value potential_pockets(ScriptRuntime controller, final Value arg) {
    List<Pocket> sorted = sortedPockets(arg.getType(), arg.toString());
    return makePocketList(sorted);
  }

  // pocket available_pocket( monster m );
  // pocket available_pocket( effect e );
  // pocket available_pocket( item i );
  // pocket available_pocket( stat s );
  public static Value available_pocket(ScriptRuntime controller, final Value arg) {
    List<Pocket> sorted = sortedPockets(arg.getType(), arg.toString());
    Pocket pocket = PocketDatabase.firstUnpickedPocket(sorted);
    return (pocket == null) ? DataTypes.ZERO_VALUE : new Value(pocket.getPocket());
  }

  // boolean pick_pocket( int p );
  // boolean pick_pocket( monster m );
  // boolean pick_pocket( effect e );
  // boolean pick_pocket( item i );
  // boolean pick_pocket( stat s );
  public static Value pick_pocket(ScriptRuntime controller, final Value arg) {
    Type type = arg.getType();
    Pocket pocket =
        type.equals(TypeSpec.INT)
            ? PocketDatabase.pocketByNumber((int) arg.intValue())
            : PocketDatabase.firstUnpickedPocket(
                RuntimeLibrary.sortedPockets(type, arg.toString()));

    if (pocket == null) {
      return DataTypes.FALSE_VALUE;
    }

    CargoCultistShortsRequest pick = new CargoCultistShortsRequest(pocket.getPocket());
    pick.run();

    return RuntimeLibrary.continueValue();
  }

  private static List<Pocket> sortedPockets(Type type, String name) {
    if (type.equals(TypeSpec.EFFECT)) {
      Set<OneResultPocket> pockets = PocketDatabase.effectPockets.get(name);
      if (pockets != null) {
        return PocketDatabase.sortResults(name, pockets);
      }
    } else if (type.equals(TypeSpec.ITEM)) {
      Set<OneResultPocket> pockets = PocketDatabase.itemPockets.get(name);
      if (pockets != null) {
        return PocketDatabase.sortResults(name, pockets);
      }
    } else if (type.equals(TypeSpec.MONSTER)) {
      MonsterPocket pocket = PocketDatabase.monsterPockets.get(name.toLowerCase());
      if (pocket != null) {
        return Collections.singletonList(pocket);
      }
    } else if (type.equals(TypeSpec.STAT)) {
      name = name.toLowerCase();
      Set<StatsPocket> pockets = PocketDatabase.statsPockets.get(name);
      if (pockets != null) {
        return PocketDatabase.sortStats(name, pockets);
      }
    }

    return Collections.emptyList();
  }

  private static Value makePocketSet(Collection<Integer> pockets) {
    MapValue value = new MapValue(PocketSetType);
    for (Integer pocket : pockets) {
      value.aset(new Value(pocket), DataTypes.TRUE_VALUE);
    }
    return value;
  }

  private static Value makePocketList(List<Pocket> pockets) {
    MapValue value = new MapValue(PocketListType);
    int index = 0;
    for (Pocket pocket : pockets) {
      value.aset(new Value(index++), new Value(pocket.getPocket()));
    }
    return value;
  }

  private static Value makePocketEffects(int pocket) {
    MapValue value = new MapValue(PocketEffectsType);
    Pocket p = PocketDatabase.pocketByNumber(pocket);
    if (p != null && PocketDatabase.allEffectPockets.contains(p.getPocket())) {
      if (p instanceof OneResultPocket orp) {
        value.aset(
            DataTypes.makeEffectValue(orp.getResult1().getEffectId(), true),
            new Value(orp.getResult1().getCount()));
      }
      if (p instanceof TwoResultPocket trp) {
        value.aset(
            DataTypes.makeEffectValue(trp.getResult2().getEffectId(), true),
            new Value(trp.getResult2().getCount()));
      }
    }
    return value;
  }

  private static Value makePocketItems(int pocket) {
    MapValue value = new MapValue(PocketItemsType);
    Pocket p = PocketDatabase.pocketByNumber(pocket);
    if (p != null && PocketDatabase.allItemPockets.contains(p.getPocket())) {
      if (p instanceof OneResultPocket orp) {
        value.aset(
            DataTypes.makeItemValue(orp.getResult1().getItemId(), true),
            new Value(orp.getResult1().getCount()));
      }
      if (p instanceof TwoResultPocket trp) {
        value.aset(
            DataTypes.makeItemValue(trp.getResult2().getItemId(), true),
            new Value(trp.getResult2().getCount()));
      }
    }
    return value;
  }

  private static Value makePocketStats(int pocket) {
    MapValue value = new MapValue(PocketStatsType);
    Pocket p = PocketDatabase.pocketByNumber(pocket);
    if (p != null && PocketDatabase.allStatsPockets.contains(p.getPocket())) {
      StatsPocket sp = (StatsPocket) p;
      value.aset(DataTypes.MUSCLE_VALUE, new Value(sp.getMuscle()));
      value.aset(DataTypes.MYSTICALITY_VALUE, new Value(sp.getMysticality()));
      value.aset(DataTypes.MOXIE_VALUE, new Value(sp.getMoxie()));
    }
    return value;
  }

  private static Value makeIndexedText(int pocket) {
    MapValue value = new MapValue(IndexedTextType);
    Pocket p = PocketDatabase.pocketByNumber(pocket);
    if (p != null) {
      switch (p.getType()) {
        case SCRAP -> {
          ScrapPocket sp = (ScrapPocket) p;
          Map<Integer, String> knownScraps = CargoCultistShortsRequest.knownScrapPockets();
          String syllable = knownScraps.getOrDefault(sp.getPocket(), "");
          value.aset(new Value(sp.getScrap()), new Value(syllable));
        }
        case POEM -> {
          PoemPocket pp = (PoemPocket) p;
          value.aset(new Value(pp.getIndex()), new Value(pp.getText()));
        }
        case MEAT -> {
          MeatPocket mp = (MeatPocket) p;
          value.aset(new Value(mp.getMeat()), new Value(mp.getText()));
        }
      }
    }
    return value;
  }

  public static Value expected_cold_medicine_cabinet(ScriptRuntime controller) {
    var value = new MapValue(DataTypes.STRING_TO_ITEM_TYPE);

    var cabinet = ColdMedicineCabinetCommand.getCabinet();

    if (cabinet == null) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "Could not parse cabinet.");

      for (var t : ColdMedicineCabinetCommand.ITEM_TYPES) {
        Value key = new Value(t);
        Value val = DataTypes.makeItemValue((AdventureResult) null);
        value.aset(key, val);
      }
    } else {
      for (var e : cabinet.entrySet()) {
        Value key = new Value(e.getKey());
        Value val = DataTypes.makeItemValue(e.getValue());
        value.aset(key, val);
      }
    }

    return value;
  }

  public static Value absorbed_monsters(ScriptRuntime controller) {
    AggregateType type = new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.MONSTER_TYPE);
    MapValue value = new MapValue(type);

    int index = 0;
    for (Integer monsterId : GreyYouManager.absorbedMonsters) {
      value.aset(DataTypes.makeMonsterValue(monsterId, true), DataTypes.TRUE_VALUE);
    }

    return value;
  }

  public static Value zap(ScriptRuntime controller, Value item) {
    var request = new ZapRequest(ItemPool.get((int) item.intValue()));
    RequestThread.postRequest(request);

    var acquired = request.getAcquired();
    int itemId = acquired == null ? -1 : acquired.getItemId();

    return DataTypes.makeItemValue(itemId, true);
  }

  public static Value get_autumnaton_locations(ScriptRuntime controller) {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      return new ArrayValue(new AggregateType(DataTypes.LOCATION_TYPE, 0));
    }

    var response = AutumnatonManager.useAutumnaton();
    var locs = AutumnatonManager.parseLocations(response);

    var size = locs.size();
    AggregateType type = new AggregateType(DataTypes.LOCATION_TYPE, size);
    ArrayValue value = new ArrayValue(type);

    int i = 0;
    for (var adv : locs) {
      Value location = DataTypes.makeLocationValue(adv);
      value.aset(DataTypes.makeIntValue(i++), location);
    }

    return value;
  }

  // boolean monkey_paw( string s );
  // boolean monkey_paw( item i );
  // boolean monkey_paw( effect e );
  public static Value monkey_paw(ScriptRuntime controller, final Value arg) {
    String wish = null;
    Type type = arg.getType();
    if (type.equals(TypeSpec.ITEM)) {
      AdventureResult item = ItemPool.get((int) arg.intValue());
      wish = MonkeyPawCommand.getValidItemSubstring(item.getName());
    } else if (type.equals(TypeSpec.EFFECT)) {
      AdventureResult effect = EffectPool.get((int) arg.intValue());
      wish = MonkeyPawCommand.getValidEffectSubstring(effect.getName());
    } else {
      wish = arg.toString();
    }
    if (wish == null) {
      return DataTypes.FALSE_VALUE;
    } else {
      return DataTypes.makeBooleanValue(monkey_paw(wish));
    }
  }

  private static boolean monkey_paw(String wish) {
    var req = new MonkeyPawRequest(wish);
    RequestThread.postRequest(req);
    return req.responseText != null && !req.responseText.contains("impossible");
  }

  public static Value sausage_goblin_chance(ScriptRuntime controller) {
    var sausageFights = Preferences.getInteger("_sausageFights");
    var maxTurnsToNextGoblin = 4 + 3 * sausageFights + Math.pow(Math.max(0, sausageFights - 5), 3);
    var lastGoblin = Preferences.getInteger("_lastSausageMonsterTurn");
    var turnsSinceLastGoblin = KoLCharacter.getTurnsPlayed() - lastGoblin;

    if (turnsSinceLastGoblin >= maxTurnsToNextGoblin) {
      return DataTypes.makeFloatValue(1.0);
    }

    var prob = (turnsSinceLastGoblin + 1.0) / (maxTurnsToNextGoblin + 1);
    return DataTypes.makeFloatValue(prob);
  }

  public static Value ping(ScriptRuntime controller) {
    return ping(controller, PingManager.runPingTest(false));
  }

  public static Value ping(ScriptRuntime controller, final Value arg1) {
    String property = arg1.toString();
    return ping(controller, PingTest.parseProperty(property));
  }

  public static Value ping(ScriptRuntime controller, final Value arg1, final Value arg2) {
    int count = (int) arg1.intValue();
    String page = arg2.toString();
    return ping(controller, PingManager.runPingTest(count, page, false, false));
  }

  private static Value ping(ScriptRuntime controller, final PingTest result) {
    AshRuntime interpreter = controller instanceof AshRuntime ? (AshRuntime) controller : null;

    RecordType type = RuntimeLibrary.pingTestRec;
    RecordValue rec = new RecordValue(type);

    // page
    rec.aset(0, new Value(result.getPage()), interpreter);
    // count
    rec.aset(1, DataTypes.makeIntValue(result.getCount()), interpreter);
    // low
    rec.aset(2, DataTypes.makeIntValue(result.getLow()), interpreter);
    // high
    rec.aset(3, DataTypes.makeIntValue(result.getHigh()), interpreter);
    // total
    rec.aset(4, DataTypes.makeIntValue(result.getTotal()), interpreter);
    // bytes
    rec.aset(5, DataTypes.makeIntValue(result.getBytes()), interpreter);
    // average
    rec.aset(6, DataTypes.makeIntValue(Math.round(result.getAverage())), interpreter);
    // bps
    rec.aset(7, DataTypes.makeIntValue(Math.round(result.getBPS())), interpreter);

    return rec;
  }

  public static Value fact_type(
      ScriptRuntime controller, final Value cls, final Value path, final Value monster) {
    if (cls.content == null || monster.content == null) return DataTypes.STRING_INIT;
    var fact =
        FactDatabase.getFact(
            (AscensionClass) cls.content,
            (Path) path.content,
            (MonsterData) monster.content,
            false);
    return new Value(fact.getType().toString());
  }

  public static Value fact_type(ScriptRuntime controller, final Value monster) {
    return fact_type(
        controller,
        DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), true),
        DataTypes.makePathValue(KoLCharacter.getPath()),
        monster);
  }

  public static Value effect_fact(
      ScriptRuntime controller, final Value cls, final Value path, final Value monster) {
    if (cls.content == null || monster.content == null) return DataTypes.EFFECT_INIT;
    var f =
        FactDatabase.getFact(
            (AscensionClass) cls.content,
            (Path) path.content,
            (MonsterData) monster.content,
            false);

    if (!(f instanceof FactDatabase.AdventureResultFact fact)) {
      return DataTypes.EFFECT_INIT;
    }

    var result = fact.getResult();

    return DataTypes.makeEffectValue(result.getEffectId(), true);
  }

  public static Value effect_fact(ScriptRuntime controller, final Value monster) {
    return effect_fact(
        controller,
        DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), true),
        DataTypes.makePathValue(KoLCharacter.getPath()),
        monster);
  }

  public static Value item_fact(
      ScriptRuntime controller, final Value cls, final Value path, final Value monster) {
    if (cls.content == null || monster.content == null) return DataTypes.ITEM_INIT;
    var f =
        FactDatabase.getFact(
            (AscensionClass) cls.content,
            (Path) path.content,
            (MonsterData) monster.content,
            false);

    if (!(f instanceof FactDatabase.AdventureResultFact fact)) {
      return DataTypes.ITEM_INIT;
    }

    var result = fact.getResult();

    return DataTypes.makeItemValue(result.getItemId(), true);
  }

  public static Value item_fact(ScriptRuntime controller, final Value monster) {
    return item_fact(
        controller,
        DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), true),
        DataTypes.makePathValue(KoLCharacter.getPath()),
        monster);
  }

  public static Value numeric_fact(
      ScriptRuntime controller, final Value cls, final Value path, final Value monster) {
    if (cls.content == null || monster.content == null) return DataTypes.INT_INIT;
    var f =
        FactDatabase.getFact(
            (AscensionClass) cls.content,
            (Path) path.content,
            (MonsterData) monster.content,
            false);

    if (f instanceof FactDatabase.MeatFact fact) {
      return new Value(fact.getMeat());
    }

    if (f instanceof FactDatabase.AdventureResultFact fact) {
      return new Value(fact.getResult().getCount());
    }

    if (f instanceof FactDatabase.StatsFact fact) {
      return new Value(fact.getStatValue());
    }

    if (f.getType() == FactDatabase.FactType.HP || f.getType() == FactDatabase.FactType.MP) {
      return new Value(StringUtilities.parseInt(f.getValue()));
    }

    return DataTypes.INT_INIT;
  }

  public static Value numeric_fact(ScriptRuntime controller, final Value monster) {
    return numeric_fact(
        controller,
        DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), true),
        DataTypes.makePathValue(KoLCharacter.getPath()),
        monster);
  }

  public static Value string_fact(
      ScriptRuntime controller, final Value cls, final Value path, final Value monster) {
    if (cls.content == null || monster.content == null) return DataTypes.STRING_INIT;
    var f =
        FactDatabase.getFact(
            (AscensionClass) cls.content,
            (Path) path.content,
            (MonsterData) monster.content,
            false);

    if (f instanceof FactDatabase.StatsFact fact) {
      return new Value(fact.getStat());
    }

    return new Value(f.toString());
  }

  public static Value string_fact(ScriptRuntime controller, final Value monster) {
    return string_fact(
        controller,
        DataTypes.makeClassValue(KoLCharacter.getAscensionClass(), true),
        DataTypes.makePathValue(KoLCharacter.getPath()),
        monster);
  }

  public static Value dart_parts_to_skills(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.STRING_TO_SKILL_TYPE);

    String[] darts = Preferences.getString("_currentDartboard").split("\\s*,\\s*");
    for (String dart : darts) {
      int colon = dart.indexOf(":");
      if (colon != -1) {
        int skillId = StringUtilities.parseInt(dart.substring(0, colon));
        Value skill = DataTypes.makeSkillValue(skillId, true);
        Value part = new Value(dart.substring(colon + 1));
        value.aset(part, skill);
      }
    }

    return value;
  }

  public static Value dart_skills_to_parts(ScriptRuntime controller) {
    MapValue value = new MapValue(DataTypes.SKILL_TO_STRING_TYPE);

    String[] darts = Preferences.getString("_currentDartboard").split("\\s*,\\s*");
    for (String dart : darts) {
      int colon = dart.indexOf(":");
      if (colon != -1) {
        int skillId = StringUtilities.parseInt(dart.substring(0, colon));
        Value skill = DataTypes.makeSkillValue(skillId, true);
        Value part = new Value(dart.substring(colon + 1));
        value.aset(skill, part);
      }
    }

    return value;
  }

  public static Value turns_until_forced_noncombat(
      ScriptRuntime controller, final Value locationValue) {
    if (locationValue.rawValue() == null) return DataTypes.makeIntValue(-1);

    var location = (KoLAdventure) locationValue.rawValue();
    var preference = "lastNoncombat" + location.getAdventureNumber();
    if (location.getForceNoncombat() < 0
        || !Preferences.containsDefault(preference)
        || Preferences.getInteger(preference) < 0) {
      return DataTypes.makeIntValue(-1);
    }

    return DataTypes.makeIntValue(
        Math.max(
            0,
            location.getForceNoncombat()
                - (AdventureSpentDatabase.getTurns(location)
                    - Preferences.getInteger(preference))));
  }

  public static Value get_avatar(ScriptRuntime controller) {
    return DataTypes.makeStringArrayValue(KoLCharacter.getAvatar());
  }

  public static Value get_title(ScriptRuntime controller) {
    return DataTypes.makeStringValue(KoLCharacter.getTitle());
  }

  public static Value free_crafts(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ConcoctionDatabase.getFreeCraftingTurns());
  }

  public static Value free_cooks(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ConcoctionDatabase.getFreeCookingTurns());
  }

  public static Value free_mixes(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ConcoctionDatabase.getFreeCocktailcraftingTurns());
  }

  public static Value free_smiths(ScriptRuntime controller) {
    return DataTypes.makeIntValue(ConcoctionDatabase.getFreeSmithingTurns());
  }

  public static Value allied_radio(ScriptRuntime controller, final Value requestValue) {
    String request = requestValue.toString();
    AlliedRadioRequest req = new AlliedRadioRequest(request);
    RequestThread.postRequest(req);
    return DataTypes.makeBooleanValue(req.responseText != null);
  }

  public static Value futuristic_wardrobe(
      ScriptRuntime controller, final Value slotVal, final Value tierVal) {
    var today = KoLCharacter.getGlobalDays();
    var slot = Slot.byOrdinal((int) slotVal.intValue());
    var tier = (int) tierVal.intValue();
    return futuristic_wardrobe(today, slot, tier);
  }

  public static Value futuristic_wardrobe(
      ScriptRuntime controller, final Value dayVal, final Value slotVal, final Value tierVal) {
    var day = (int) dayVal.intValue();
    var slot = Slot.byOrdinal((int) slotVal.intValue());
    var tier = (int) tierVal.intValue();
    return futuristic_wardrobe(day, slot, tier);
  }

  private static Value futuristic_wardrobe(final int day, final Slot slot, final int tier) {
    AggregateType type = new AggregateType(DataTypes.INT_TYPE, DataTypes.MODIFIER_TYPE);
    MapValue value = new MapValue(type);

    FuturisticClothing clothing = null;
    switch (slot) {
      case SHIRT -> clothing = WardrobeOMaticDatabase.shirt(day, tier);
      case HAT -> clothing = WardrobeOMaticDatabase.hat(day, tier);
      case FAMILIAR -> clothing = WardrobeOMaticDatabase.collar(day, tier);
    }
    if (clothing == null) {
      return value;
    }
    var mods = clothing.modifiers();

    for (var entry : mods.entrySet()) {
      value.aset(new Value(entry.getKey()), new Value(entry.getValue()));
    }

    return value;
  }
}
