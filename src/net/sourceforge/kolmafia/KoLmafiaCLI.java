package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.Aliases;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.command.*;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.PrefixMap;

public class KoLmafiaCLI {
  public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI(System.in);

  private static final Pattern HTMLTAG_PATTERN = Pattern.compile("<.*?>", Pattern.DOTALL);
  private static final Pattern HEAD_PATTERN = Pattern.compile("<head>.*?</head>", Pattern.DOTALL);
  private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

  private final LinkedList<String> queuedLines = new LinkedList<>();
  private final CommandRetrieverThread retriever = new CommandRetrieverThread();
  private final CommandProcessorThread processor = new CommandProcessorThread();

  public String previousLine = null;
  public String currentLine = null;
  private BufferedReader commandStream;
  private boolean isGUI = true;
  private boolean elseValid = false;
  private boolean elseRuns = false;

  public static boolean isExecutingCheckOnlyCommand = false;

  // Flag values for Commands:
  public static final int FULL_LINE_CMD = 1;
  public static final int FLOW_CONTROL_CMD = 2;

  /**
   * Constructs a new <code>KoLmafiaCLI</code> object. All data fields are initialized to their
   * default values, the global settings are loaded from disk.
   */
  public KoLmafiaCLI(final InputStream inputStream) {
    try {
      this.commandStream = FileUtilities.getReader(inputStream);
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e, "Error opening input stream.");
    }
  }

  /**
   * Utility method used to prompt the user for their login and password. Later on, when profiles
   * are added, prompting for the user will automatically look up a password.
   */
  public void attemptLogin(String username) {
    try {
      if (username == null || username.length() == 0) {
        System.out.println();
        System.out.print("username: ");
        username = this.commandStream.readLine();
      }

      if (username == null || username.length() == 0) {
        System.out.println("Invalid login.");
        return;
      }

      if (username.startsWith("login ")) {
        username = username.substring(6).trim();
      }

      String password = KoLmafia.getSaveState(username);

      if (password == null) {
        System.out.print("password: ");
        password = this.commandStream.readLine();
      }

      if (password == null || password.length() == 0) {
        System.out.println("Invalid password.");
        return;
      }

      System.out.println();
      Preferences.setBoolean("saveStateActive", true);
      RequestThread.postRequest(new LoginRequest(username, password));
    } catch (IOException e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e, "Error in login attempt");
    }
  }

  private class CommandRetrieverThread extends Thread {
    @Override
    public void run() {
      String line;

      try {
        while ((line = KoLmafiaCLI.this.commandStream.readLine()) != null) {
          if (line.equals("--")) {
            RequestThread.declareWorldPeace();

            synchronized (KoLmafiaCLI.this.queuedLines) {
              KoLmafiaCLI.this.queuedLines.clear();
            }
          } else if (line.length() == 0
              || line.startsWith("#")
              || line.startsWith("//")
              || line.startsWith("'")) {
            synchronized (KoLmafiaCLI.this.queuedLines) {
              KoLmafiaCLI.this.queuedLines.addLast(null);
            }
          } else {
            synchronized (KoLmafiaCLI.this.queuedLines) {
              KoLmafiaCLI.this.queuedLines.addLast(line);
            }
          }
        }

        KoLmafiaCLI.this.commandStream.close();
        KoLmafiaCLI.this.currentLine = null;
      } catch (IOException e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }
  }

  private class CommandProcessorThread extends Thread {
    @Override
    public void run() {
      String line;

      do {
        line = KoLmafiaCLI.this.getNextLine(" > ");

        if (KoLmafiaCLI.DEFAULT_SHELL == KoLmafiaCLI.this) {
          KoLmafia.forceContinue();
        }

        if (line != null) {
          KoLmafiaCLI.this.isGUI = false;
          KoLmafiaCLI.this.executeLine(line);
          KoLmafiaCLI.this.isGUI = true;
        }

        if (KoLmafiaCLI.DEFAULT_SHELL == KoLmafiaCLI.this) {
          KoLmafia.forceContinue();
        }
      } while (line != null && KoLmafia.permitsContinue());
    }
  }

  public String getNextLine(String message) {
    String line = null;

    while (line == null) {
      if (message != null
          && KoLmafiaCLI.this.queuedLines.isEmpty()
          && KoLmafiaCLI.DEFAULT_SHELL == KoLmafiaCLI.this
          && KoLmafiaCLI.this.retriever.isAlive()) {
        RequestLogger.printLine();

        System.out.print(message);
      }

      if (KoLmafiaCLI.this.queuedLines.isEmpty() && KoLmafiaCLI.this.retriever.isAlive()) {
        PauseObject pauser = new PauseObject();

        do {
          pauser.pause(100);
        } while (KoLmafiaCLI.this.queuedLines.isEmpty() && KoLmafiaCLI.this.retriever.isAlive());
      }

      if (KoLmafiaCLI.this.queuedLines.isEmpty()) {
        return null;
      }

      if (KoLmafiaCLI.DEFAULT_SHELL == KoLmafiaCLI.this) {
        RequestLogger.printLine();
      }

      synchronized (KoLmafiaCLI.this.queuedLines) {
        line = KoLmafiaCLI.this.queuedLines.removeFirst();
      }
    }

    return line;
  }

  /**
   * A utility method which waits for commands from the user, then executing each command as it
   * arrives.
   */
  public void listenForCommands() {
    KoLmafia.forceContinue();

    if (KoLmafiaCLI.DEFAULT_SHELL == KoLmafiaCLI.this) {
      this.retriever.start();
      this.processor.start();
    } else {
      this.retriever.run();
      this.processor.run();
    }
  }

  public void executeLine(String line) {
    this.executeLine(line, null);
  }

  public void executeLine(String line, final ScriptRuntime controller) {
    if (line == null || KoLmafia.refusesContinue()) {
      return;
    }

    String origLine = line;

    line = line.replaceAll("[ \t]+", " ").trim();
    if (line.length() == 0) {
      return;
    }

    // Pass through escaped character entities to ASH and JS
    if (!(line.startsWith("ash") || line.startsWith("js"))) {
      line = CharacterEntities.unescape(line);
    }

    // First, handle all the aliasing that may be
    // defined by the user.

    this.currentLine = line = Aliases.apply(line);
    if (!line.startsWith("repeat")) {
      this.previousLine = line;
    }

    while (KoLmafia.permitsContinue() && line.length() > 0) {
      line = line.trim();

      int splitIndex = line.indexOf(";");
      String parameters;

      if (splitIndex != -1) {
        parameters = line.substring(0, splitIndex);
        line = line.substring(splitIndex + 1);
      } else {
        parameters = line;
        line = "";
      }

      // At this point, "parameters" has no leading
      // spaces. It may have trailing spaces.

      String trimmed = parameters.trim();
      if (trimmed.length() == 0) {
        continue;
      }

      if (trimmed.startsWith("/")) {
        ChatSender.executeMacro(trimmed);
        continue;
      }

      // "trimmed" has no leading or trailing spaces. Its
      // first word is the command.

      splitIndex = trimmed.indexOf(" ");

      String lcommand = trimmed.toLowerCase();
      String command;

      if (splitIndex == -1) {
        // Single word command. No parameters.
        command = trimmed;
        trimmed = "";
      } else if (AbstractCommand.lookup.get(lcommand) != null
          && AbstractCommand.lookup.getKeyType(lcommand) == PrefixMap.EXACT_KEY) {
        // Multiword command
        command = lcommand;
        trimmed = "";
      } else {
        command = trimmed.substring(0, splitIndex);
        lcommand = command.toLowerCase();
        parameters = parameters.substring(splitIndex + 1);
        trimmed = parameters.trim();
      }

      // "parameters" has no leading spaces. It may have
      // trailing spaces.
      // "trimmed" has no leading or trailing spaces.

      if (command.endsWith("?")) {
        KoLmafiaCLI.isExecutingCheckOnlyCommand = true;
        int length = command.length();
        command = command.substring(0, length - 1);
        lcommand = lcommand.substring(0, length - 1);
      }

      AbstractCommand handler = AbstractCommand.lookup.get(lcommand);
      int flags = handler == null ? 0 : handler.flags;
      if (flags == KoLmafiaCLI.FULL_LINE_CMD && !line.equals("")) {
        // parameters are un-trimmed original
        // parameters + rest of line
        trimmed = parameters + ";" + line;
        line = "";
      }

      if (flags == KoLmafiaCLI.FLOW_CONTROL_CMD) {
        String continuation = this.getContinuation(line);
        if (!KoLmafia.permitsContinue()) {
          return;
        }
        handler.continuation = continuation;
        handler.CLI = this;
        handler.run(lcommand, trimmed);
        handler.CLI = null;
        KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
        this.previousLine = command + " " + trimmed + ";" + continuation;
        return;
      }

      this.executeCommand(command, trimmed, controller);
      KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
    }

    if (KoLmafia.permitsContinue()) {
      // Notify user-entered Daily Deeds that the command was
      // successful.
      PreferenceListenerRegistry.firePreferenceChanged(origLine);
    }
  }

  private String getContinuation(String line) {
    line = line.trim();

    StringBuilder block = new StringBuilder(line);
    boolean seenCmd = false, needAnotherCmd = false;
    while (true) {
      while (line.length() > 0) {
        String command;
        int splitIndex = line.indexOf(";");
        if (splitIndex == -1) {
          command = line.toLowerCase();
          line = "";
        } else {
          command = line.substring(0, splitIndex).toLowerCase();
          line = line.substring(splitIndex + 1).trim();
        }
        if (command.equals("")) {
          continue;
        }
        seenCmd = true;
        needAnotherCmd = false;
        command = command.split(" ")[0];
        if (command.endsWith("?")) {
          command = command.substring(0, command.length() - 1);
        }
        AbstractCommand handler = AbstractCommand.lookup.get(command);
        int flags = handler == null ? 0 : handler.flags;
        if (flags == KoLmafiaCLI.FULL_LINE_CMD) {
          break;
        }
        if (flags == KoLmafiaCLI.FLOW_CONTROL_CMD) {
          needAnotherCmd = true;
        }
      }
      if (seenCmd && !needAnotherCmd) {
        return block.toString();
      }

      // We need another line to complete the command.  However, if the
      // original command didn't come from the input stream (the gCLI
      // entry field, perhaps), trying to read a line would just hang.
      if (this.isGUI) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Multi-line statements cannot be used from the gCLI.");
        return "";
      }

      line = this.getNextLine("+> ");

      if (line == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unterminated conditional statement.");
        return "";
      }
      block.append(";");
      block.append(line);
    }
  }

  /**
   * A utility command which decides, based on the command to be executed, what to be done with it.
   * It can either delegate this to other functions, or do it itself.
   */
  public void executeCommand(String command, String parameters) {
    this.executeCommand(command, parameters, null);
  }

  private void executeCommand(String command, String parameters, ScriptRuntime caller) {
    Integer requestId = RequestThread.openRequestSequence();

    try {
      this.doExecuteCommand(command, parameters, caller);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    } finally {
      RequestThread.closeRequestSequence(requestId);
    }
  }

  private void doExecuteCommand(String command, String parameters, ScriptRuntime caller) {
    String lcommand = command.toLowerCase();

    // If the command has already been disabled, then return
    // from this function.

    if (StaticEntity.isDisabled(lcommand)) {
      RequestLogger.printLine("Called disabled command: " + lcommand + " " + parameters);
      return;
    }

    if (parameters.equals("refresh")) {
      parameters = lcommand;
      lcommand = command = "refresh";
    }

    AbstractCommand handler = AbstractCommand.lookup.get(lcommand);

    if (handler == null) {
      handler = AbstractCommand.getSubstringMatch(lcommand);
    }

    if (handler != null) {
      if (lcommand.endsWith("*")) {
        RequestLogger.printLine(
            "(A * after a command name indicates that it can be "
                + "typed in a longer form.  There's no need to type the asterisk!)");
      }

      handler.CLI = this;
      handler.callerController = caller;
      handler.run(lcommand, parameters);
      handler.CLI = null;
      handler.callerController = null;

      return;
    }

    // If all else fails, then assume that the
    // person was trying to call a script.

    CallScriptCommand.call("call", command + " " + parameters, caller);
  }

  public void elseRuns(final boolean shouldRun) {
    this.elseRuns = shouldRun;
    this.elseValid = true;
  }

  public boolean elseValid() {
    return this.elseValid;
  }

  /** Indicates that a following "else" command is not valid here. */
  public void elseInvalid() {
    this.elseValid = false;
  }

  /**
   * Tests whether a "else" command should be executed, and mark further "else"s as invalid. If this
   * "else" is invalid, generate an error and return false.
   */
  public boolean elseRuns() {
    if (!this.elseValid) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "'else' must follow a conditional command, and both must be at the outermost level.");
      return false;
    }
    this.elseValid = false;
    return this.elseRuns;
  }

  public static void registerCommands() {
    new AbortCommand().register("abort");
    new AbsorbCommand().register("absorb");
    new AccordionsCommand().register("accordions");
    new AcquireCommand().register("acquire").register("find").register("retrieve");
    new AdventureCommand().register("adv").register("adventure");
    new AliasCommand().register("alias");
    new AreaSummaryCommand().register("safe");
    new AsdonMartinCommand().register("asdonmartin");
    new AshMultiLineCommand().register("<inline-ash-script>");
    new AshRefCommand().register("ashref");
    new AshSingleLineCommand().register("ash").register("ashq");
    new AutoAttackCommand().register("aa").register("autoattack");
    new AutoMallCommand().register("automall");
    new AutoSellCommand().register("sell").register("autosell");
    new BacktraceCommand().register("backtrace");
    new BackupCameraCommand().register("backupcamera");
    new BadMoonCommand().register("badmoon");
    new BallPitCommand().register("ballpit");
    new BangPotionsCommand().register("!").register("bang").register("vials");
    new BanishesCommand().register("banishes");
    new BarrelPrayerCommand().register("barrelprayer");
    new BasementCommand().register("basement");
    new BeachCommand().register("beach");
    new BjornifyCommand().register("bjornify");
    new BoomBoxCommand().register("boombox");
    new BountyCommand().register("bounty");
    new BreakfastCommand().register("breakfast");
    new BudgetCommand().register("budget");
    new BuffbotCommand().register("buffbot");
    new BugbearsCommand().register("bugbears");
    new BurnMpCommand().register("burn");
    new BuyCommand().register("buy").register("mallbuy");
    new CacheCommand().register("cache");
    new CallScriptCommand()
        .register("verify")
        .register("validate")
        .register("check")
        .register("call")
        .register("run")
        .register("exec")
        .register("exececute")
        .register("load")
        .register("start")
        .register("profile");
    new CampgroundCommand().register("camp").register("campground");
    new ChangeCombatScriptCommand().register("ccs");
    new CargoCultCommand().register("cargo");
    new CheckDataCommand()
        .register("newdata")
        .register("checkcandy")
        .register("checkconsumables")
        .register("checkconsumption")
        .register("checkeffects")
        .register("checkfamiliars")
        .register("checkitems")
        .register("checkmanuel")
        .register("checkmeat")
        .register("checkmodifiers")
        .register("checkoutfits")
        .register("checkplurals")
        .register("checkpotions")
        .register("checkpowers")
        .register("checkprofile")
        .register("checkpulverization")
        .register("checkshields")
        .register("checkskills")
        .register("checkzapgroups");
    new ChessCommand().register("chess");
    new ChoiceCommand().register("choice");
    new ChipsCommand().register("chips");
    new ClanCommand().register("clan");
    new ClanSofaCommand().register("sofa").register("sleep");
    new ClanStashCommand().register("stash");
    new CleanupJunkRequest().register("junk").register("cleanup");
    new ClearBufferCommand().register("clear").register("cls").register("reset");
    new CliRefCommand().register("help").register("which");
    new ClosetCommand().register("closet");
    new CoinmasterCommand().register("coinmaster");
    new ColorEchoCommand().register("colorecho").register("cecho");
    new ComparisonShopCommand().register("cheapest").register("expensive");
    new CompleteQuestCommand()
        .register("baron")
        .register("choice-goal")
        .register("door")
        .register("dvorak")
        .register("gourd")
        .register("guild")
        .register("maze")
        .register("sven")
        .register("tavern");
    new ConcertCommand().register("concert");
    new ConditionsCommand()
        .registerPlural("goal")
        .registerPlural("condition")
        .registerPlural("objective");
    new CondRefCommand().register("condref");
    new CouncilCommand().register("council");
    new CountersCommand().register("counters");
    new CreateItemCommand()
        .register("create")
        .register("make")
        .register("bake")
        .register("mix")
        .register("smith")
        .register("tinker")
        .register("ply");
    new CrimboTreeCommand().register("crimbotree");
    new CrossStreamsCommand().register("crossstreams");
    new DadCommand().register("dad");
    new DaycareCommand().register("daycare");
    new DebugCreateCommand().register("debugcreate");
    new DebugRequestCommand().register("debug");
    new DemonNamesCommand().register("demons");
    new DisplayCaseCommand().register("display");
    new DreadscrollCommand().register("dreadscroll");
    new DustyBottlesCommand().register("dusty");
    new DwarfFactoryCommand().register("factory");
    new EchoCommand().register("echo").register("print");
    new EditCommand().register("edit");
    new EditMoodCommand().registerPlural("trigger");
    new EdPieceCommand().register("edpiece");
    new EdServantCommand().registerPlural("servant");
    new ElseIfStatement().register("elseif");
    new ElseStatement().register("else");
    new EnableCommand().register("enable").register("disable");
    new EnthroneCommand().register("enthrone");
    new EquipCommand().register("equip").register("wear").register("wield");
    new EudoraCommand().register("eudora").register("correspondent");
    new EventsCommand().register("events");
    new ExitCommand().register("exit").register("quit");
    new ExtendEffectCommand().register("up");
    new FakeAddItemCommand().register("fakeitem");
    new FakeRemoveItemCommand().register("removeitem");
    new FamiliarCommand().register("familiar");
    new FaxCommand().register("fax");
    new FaxbotCommand().register("faxbot");
    new FullEchoCommand().register("fecho").register("fprint");
    new FlickerCommand().register("flicker");
    new FloristCommand().register("florist");
    new FlowerHuntCommand().register("flowers").register("swagger");
    new FoldItemCommand().register("fold").register("squeeze");
    new FortuneCommand().register("fortune");
    new ForumCommand().registerPlural("forum");
    new FriarBlessingCommand().register("friars");
    new GapCommand().register("gap");
    new GarbageCollectCommand().register("gc");
    new GardenCommand().register("garden");
    new GenieCommand().register("genie");
    new GongCommand().register("gong");
    new GrandpaCommand().register("grandpa");
    new GrayGUICommand().register("graygui").register("greygui").register("jstack");
    new GrimCommand().register("grim");
    new HallOfLegendsCommand().register("donate");
    new HatterCommand().register("hatter");
    new HeapDumpCommand().register("jmap").register("heapdump");
    new HermitCommand().register("hermit");
    new HorseryCommand().register("horsery");
    new HotTubCommand().register("hottub").register("soak");
    new IfStatement().register("if");
    new ItemTraceCommand().register("itrace");
    new JavaScriptCommand()
        .register("js")
        .register("jsq")
        .register("javascript")
        .register("javascriptq");
    new JsRefCommand().register("jsref");
    new JukeboxCommand().register("jukebox");
    new KitchenCommand().registerSubstring("kitchen");
    new LatteCommand().register("latte");
    new LeafletCommand().register("leaflet");
    new LogEchoCommand().register("logecho").register("logprint");
    new LoginCommand().register("login");
    new LogoutCommand().register("logout");
    new MacroTestCommand().register("macrotest");
    new MallRepriceCommand().register("reprice").register("undercut");
    new MallSellCommand().register("mallsell");
    new ManaRestoreCountCommand().register("mpitems");
    new MayoMinderCommand().register("mayominder");
    new MayosoakCommand().register("mayosoak");
    new MemoryCleanupCommand().register("gc");
    new MirrorLogCommand().registerSubstring("mirror");
    new ModifierListCommand().register("modifies");
    new ModifierMaximizeCommand().register("maximize");
    new ModifierTraceCommand().register("modtrace");
    new ModRefCommand().register("modref");
    new MoleRefCommand().register("moleref");
    new MomCommand().register("mom");
    new MonorailCommand().register("monorail");
    new MonsterDataCommand().register("monsters");
    new MonsterLevelCommand().register("mind-control").register("mcd");
    new MoodCommand().registerPlural("mood");
    new MummeryCommand().register("mummery");
    new MushroomFieldCommand().register("field");
    new NamespaceAddCommand().register("using");
    new NamespaceListCommand().register("namespace");
    new NemesisCommand().register("nemesis");
    new NewEffectCommand().register("neweffect");
    new NumberologyCommand().register("numberology");
    new NunneryCommand().register("nuns");
    new OlfactionCommand().register("olfact").register("olfaction").register("putty");
    new OutfitCheckpointCommand().register("checkpoint");
    new OutfitCommand().register("outfit");
    new PandaCommand().register("panda");
    new PastaThrallCommand().register("thralls");
    new PillKeeperCommand().register("pillkeeper");
    new PirateInsultsCommand().register("insults");
    new PlayerSnapshotCommand().register("log");
    new PlayCommand().register("play").register("cheat");
    new PoolCommand().register("pool");
    new PoolskillCommand().register("poolskill");
    new PrefRefCommand().register("prefref");
    new PrefTraceCommand().register("ptrace");
    new PripheaCommand().register("priphea");
    new PulverizeCommand().register("smash").register("pulverize");
    new PvpAttackCommand().register("attack");
    new PvpStealCommand().register("pvp").register("steal");
    new QuarkCommand().register("quark");
    new RaffleCommand().register("raffle");
    new RecipeCommand().register("recipe").register("ingredients");
    new RecoverCommand().register("restore").register("recover").register("check");
    new RefreshStatusCommand().register("refresh");
    new RegisterAdventureCommand().register("location");
    new RelayBrowserCommand().register("relay");
    new RepeatLineCommand().register("repeat");
    new RestaurantCommand().register("restaurant").registerSubstring("brewery");
    new RestoresCommand().register("restores");
    new RetroCapeCommand().register("retrocape");
    new SaberCommand().register("saber");
    new SaveAsMoodCommand().register("save as mood");
    new SearchMallCommand().register("searchmall");
    new SendMessageCommand().register("send").register("kmail").register("csend");
    new SetHolidayCommand().register("holiday");
    new SetPreferencesCommand().register("get").register("set");
    new ShopCommand().register("shop");
    new ShowDataCommand()
        .register("inv")
        .register("inventory")
        .register("storage")
        .register("session")
        .register("summary")
        .register("effects")
        .register("status")
        .register("skills")
        .register("locations")
        .register("encounters")
        .registerPlural("moon");
    new ShowerCommand().register("shower");
    new SkateParkCommand().register("skate");
    new SkeeballCommand().register("skeeball");
    new SkeletonCommand().register("skeleton");
    new SlimeStackCommand().registerPlural("slime-stack");
    new SnapperCommand().register("snapper");
    new SnowsuitCommand().register("snowsuit");
    new SpacegateCommand().register("spacegate");
    new SpeculateCommand().register("speculate").register("whatif");
    new SpookyravenCommand().register("spookyraven");
    new SpoonCommand().register("spoon");
    new StickersCommand().registerPlural("sticker");
    new StorageCommand().register("hagnk").register("pull");
    new StyxPixieCommand().register("styx");
    new SubmitSpadeDataCommand().register("spade");
    new SummonDemonCommand().register("summon");
    new SVNCommand().register("svn");
    new SwimmingPoolCommand().register("swim");
    new SynthesizeCommand().register("synthesize");
    new TaleOfDreadCommand().register("taleofdread");
    new TCRSCommand().register("tcrs");
    new TeaTreeCommand().register("teatree");
    new TelescopeCommand().register("telescope");
    new TerminalCommand().register("terminal");
    new TestCommand().register("test");
    new ThrowItemCommand().register("throw");
    new TimeSpinnerCommand().register("timespinner");
    new ToggleCommand().register("toggle");
    new TowerDoorCommand().register("tower").register("lowkey");
    new TrainFamiliarCommand().register("train");
    new TryStatement().register("try");
    new UnaliasCommand().register("unalias");
    new UneffectCommand().register("shrug").register("uneffect").register("remedy");
    new UnequipCommand().register("unequip").register("remove");
    new UntinkerCommand().register("untinker");
    new UpdateDataCommand().register("update");
    new UseItemCommand()
        .register("eat")
        .register("drink")
        .register("use")
        .register("chew")
        .register("eatsilent")
        .register("overdrink")
        .register("drinksilent")
        .register("hobo")
        .register("ghost")
        .register("slimeling")
        .register("robo");
    new UseSkillCommand().register("cast").register("skill");
    new VersionCommand().register("version");
    new VisitURLCommand().register("text").registerPrefix("http://").registerSubstring(".php");
    new VolcanoCommand().register("volcano");
    new WaitCommand().register("wait").register("waitq").register("pause");
    new WhileStatement().register("while");
    new WikiLookupCommand().register("lookup");
    new WikiMafiaSearchCommand().register("ashwiki");
    new WikiSearchCommand().register("wiki");
    new WindowOpenCommand()
        .register("chat")
        .register("mail")
        .register("opt")
        .registerPlural("option")
        .register("item")
        .register("gear");
    new WinGameCommand().register("win game");
    new WitchessCommand().register("witchess");
    new WumpusCommand().register("wumpus");
    new ZapCommand().register("zap");

    new CommandAlias("campground", "rest").register("rest");
    new CommandAlias("equip", "off-hand").register("second").register("hold").register("dualwield");
    new CommandAlias("skills", "buff").register("buff");
    new CommandAlias("skills", "passive").register("pass").register("passive");
    new CommandAlias("skills", "self").register("self");
    new CommandAlias("skills", "combat").register("combat");
  }

  static {
    registerCommands();
  }

  public static void showHTML(final String text) {
    // Remove HTML header and comments.
    String displayText = KoLmafiaCLI.HEAD_PATTERN.matcher(text).replaceAll("");
    displayText = KoLmafiaCLI.COMMENT_PATTERN.matcher(displayText).replaceAll("");

    // Strip out all the new lines found in the source
    // so you don't accidentally add more new lines than
    // necessary.

    displayText = displayText.replaceAll("[\r\n]+", "");

    // Replace all things symbolizing paragraph breaks
    // with actual new lines.

    displayText =
        displayText.replaceAll("<(br|tr)[^>]*>", "\n").replaceAll("<(p|blockquote)[^>]*>", "\n\n");

    // Replace HTML character entities with something
    // which is more readily printable.

    displayText = KoLmafiaCLI.HTMLTAG_PATTERN.matcher(displayText).replaceAll("");
    displayText = displayText.replaceAll("&nbsp;", " ");
    displayText = displayText.replaceAll("&trade;", " [tm]");
    displayText = displayText.replaceAll("&ntilde;", "n");
    displayText = displayText.replaceAll("&quot;", "");

    // Allow only one new line at a time in the HTML
    // that is printed.

    displayText = displayText.replaceAll("\n\n\n+", "\n\n");

    RequestLogger.printLine(displayText.trim());
  }

  // we return ALL matches, let callers decide whether to error if there is no unique match. (they
  // probably should)
  public static List<File> findScriptFile(final String filename) {
    List<File> matches = new ArrayList<>();

    File absoluteFile = new File(filename);

    if (absoluteFile.isAbsolute()) {
      // if it really is supposed to be an accessible script file then it needs to be
      // in /relay or /scripts or /planting.  Only return a match if it is in one of the right
      // places.
      boolean allowed = absoluteFile.exists() && !absoluteFile.isDirectory();
      boolean okPath =
          absoluteFile.getPath().startsWith(KoLConstants.PLOTS_LOCATION.getPath())
              || absoluteFile.getPath().startsWith(KoLConstants.SCRIPT_LOCATION.getPath())
              || absoluteFile.getPath().startsWith(KoLConstants.RELAY_LOCATION.getPath());
      if (allowed && okPath) matches.add(absoluteFile);

      return matches;
    }

    return findScriptFile(filename, matches);
  }

  private static List<File> findScriptFile(final String filename, List<File> matches) {
    KoLmafiaCLI.findScriptFile(KoLConstants.ROOT_LOCATION, filename, matches, false);

    if (KoLConstants.SCRIPT_LOCATION.exists()) {
      KoLmafiaCLI.findScriptFile(KoLConstants.SCRIPT_LOCATION, filename, matches, true);
    }

    if (KoLConstants.PLOTS_LOCATION.exists()) {
      KoLmafiaCLI.findScriptFile(KoLConstants.PLOTS_LOCATION, filename, matches, false);
    }

    if (KoLConstants.RELAY_LOCATION.exists()) {
      KoLmafiaCLI.findScriptFile(KoLConstants.RELAY_LOCATION, filename, matches, true);
    }

    // Only if we get here and there are no matches do we recursively try again, adding some
    // extensions.
    // Stop recursion once an extension has been added (alternatively, don't even try if an
    // extension was specified in the first place)
    if (matches.size() == 0 && !filename.contains(".")) {
      findScriptFile(filename + ".ash", matches);
      findScriptFile(filename + ".js", matches);
      findScriptFile(filename + ".cli", matches);
      findScriptFile(filename + ".txt", matches);
    }

    return matches;
  }

  private static void findScriptFile(
      final File directory,
      final String filename,
      final List<File> matches,
      final boolean searchSubdirectories) {
    File scriptFile = new File(directory, filename);

    if (scriptFile.exists()) {
      if (!scriptFile.isDirectory()) matches.add(scriptFile);
    }

    if (searchSubdirectories) {
      File[] contents = DataUtilities.listFiles(directory);
      for (File content : contents) {
        if (content.isDirectory()) {
          KoLmafiaCLI.findScriptFile(content, filename, matches, true);
        }
      }
    }
  }

  public static String buildRelayScriptMenu() {
    boolean any = false;
    StringBuilder buf = new StringBuilder();
    buf.append(
        "<select onchange='if (this.selectedIndex>0) { top.mainpane.location=this.options[this.selectedIndex].value; this.options[0].selected=true;}'><option>-run script-</option>");
    File[] files = DataUtilities.listFiles(KoLConstants.RELAY_LOCATION);
    for (File file : files) {
      String name = file.getName();
      if (name.matches("relay_.*\\.(ash|js)")) {
        String displayName =
            name.replaceAll("^relay_", "").replaceAll("\\.(ash|js)$", "").replace("_", " ");
        any = true;
        buf.append("<option value='");
        buf.append(name);
        // This differentiates between JS files relay_x.js loaded by relay scripts, and those that
        // are relay scripts themselves.
        buf.append("?relay=true'>");
        buf.append(displayName);
        buf.append("</option>");
      }
    }

    if (any) {
      buf.append("</select>");
      return buf.toString();
    }
    return "";
  }
}
