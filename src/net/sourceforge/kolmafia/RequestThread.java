package net.sourceforge.kolmafia;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.utilities.PauseObject;

public abstract class RequestThread {
  private static final AtomicInteger nextRequestId = new AtomicInteger();
  private static final Map<Integer, Thread> threadMap = new HashMap<>();
  private static final ExecutorService EXECUTOR;

  static {
    int fixedPoolSize = Preferences.getInteger("fixedThreadPoolSize");
    if (fixedPoolSize == 0) {
      fixedPoolSize = 100;
    }

    EXECUTOR = Executors.newFixedThreadPool(fixedPoolSize);
  }

  public static final void runInParallel(final Runnable action) {
    RequestThread.runInParallel(action, true);
  }

  public static final void runInParallel(final Runnable action, final boolean sequence) {
    EXECUTOR.submit(new SequencedRunnable(action, sequence));
  }

  public static final boolean runInParallel(final List<Runnable> actions, final boolean verbose) {
    CompletionService<Boolean> completionService = new ExecutorCompletionService<>(EXECUTOR);

    for (Runnable action : actions) {
      completionService.submit(action, true);
    }

    int received = 0;
    boolean result = true;
    int lastAnnounce = 0;

    while (received < actions.size() && result) {
      try {
        Future<Boolean> resultFuture = completionService.take(); // blocks if none available
        result = result && resultFuture.get();
        received++;
        if (lastAnnounce < received && verbose && received % 100 == 1) {
          KoLmafia.updateDisplay("Progress: " + received + "/" + actions.size());
          lastAnnounce = received;
        }
      } catch (Exception e) {
        result = false;
      }
    }

    return result;
  }

  public static final void postRequestAfterInitialization(final GenericRequest request) {
    RequestThread.runInParallel(new PostDelayedRequestRunnable(request));
  }

  private static class PostDelayedRequestRunnable implements Runnable {
    private final GenericRequest request;
    private final PauseObject pauser;

    public PostDelayedRequestRunnable(final GenericRequest request) {
      this.request = request;
      this.pauser = new PauseObject();
    }

    @Override
    public void run() {
      while (KoLmafia.isRefreshing()) {
        this.pauser.pause(100);
      }

      RequestThread.postRequest(this.request);
    }
  }

  public static final void executeMethodAfterInitialization(
      final Object object, final String method) {
    RequestThread.runInParallel(new ExecuteDelayedMethodRunnable(object, method));
  }

  private static class ExecuteDelayedMethodRunnable implements Runnable {
    private final Class<?> objectClass;
    private final Object object;
    private final String methodName;
    private Method method;
    private final PauseObject pauser;

    public ExecuteDelayedMethodRunnable(final Object object, final String methodName) {
      if (object instanceof Class) {
        this.objectClass = (Class<?>) object;
        this.object = null;
      } else {
        this.objectClass = object.getClass();
        this.object = object;
      }

      this.methodName = methodName;
      try {
        Class<?>[] parameters = new Class[0];
        this.method = this.objectClass.getMethod(methodName, parameters);
      } catch (Exception e) {
        this.method = null;
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Could not invoke " + this.objectClass + "." + this.methodName);
      }

      this.pauser = new PauseObject();
    }

    @Override
    public void run() {
      if (this.method == null) {
        return;
      }

      while (KoLmafia.isRefreshing()) {
        this.pauser.pause(100);
      }

      try {
        Object[] args = new Object[0];
        this.method.invoke(this.object, args);
      } catch (Exception e) {
      }
    }
  }

  public static final void executeMethod(final Object object, final String method) {
    RequestThread.runInParallel(new ExecuteMethodRunnable(object, method));
  }

  private static class ExecuteMethodRunnable implements Runnable {
    private final Class<?> objectClass;
    private final Object object;
    private final String methodName;
    private Method method;

    public ExecuteMethodRunnable(final Object object, final String methodName) {
      if (object instanceof Class) {
        this.objectClass = (Class<?>) object;
        this.object = null;
      } else {
        this.objectClass = object.getClass();
        this.object = object;
      }

      this.methodName = methodName;
      try {
        Class<?>[] parameters = new Class[0];
        this.method = this.objectClass.getMethod(methodName, parameters);
      } catch (Exception e) {
        this.method = null;
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Could not invoke " + this.objectClass + "." + this.methodName);
      }
    }

    @Override
    public void run() {
      if (this.method == null) {
        return;
      }

      try {
        Object[] args = new Object[0];
        this.method.invoke(this.object, args);
      } catch (Exception e) {
      }
    }
  }

  /**
   * Posts a single request one time without forcing concurrency. The display will be enabled if
   * there is no sequence.
   */
  public static final void postRequest(final GenericRequest request) {
    if (request == null) {
      return;
    }

    // Make sure there is a URL string in the request
    request.reconstructFields();

    boolean force = RequestThread.threadMap.isEmpty() && request.hasResult();

    RequestThread.postRequest(force, request);
  }

  public static final void checkpointedPostRequest(final GenericRequest request) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      RequestThread.postRequest(request);
    }
  }

  public static final void postRequest(final KoLAdventure request) {
    if (request == null) {
      return;
    }

    boolean force = true;
    RequestThread.postRequest(force, request);
  }

  public static final void postRequest(final Runnable request) {
    if (request == null) {
      return;
    }

    boolean force = RequestThread.threadMap.isEmpty();
    RequestThread.postRequest(force, request);
  }

  private static void postRequest(final boolean force, final Runnable request) {
    Integer requestId = RequestThread.openRequestSequence(force);

    try {
      if (Preferences.getBoolean("debugFoxtrotRemoval") && SwingUtilities.isEventDispatchThread()) {
        StaticEntity.printStackTrace("Runnable in event dispatch thread");
      }

      request.run();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    } finally {
      RequestThread.closeRequestSequence(requestId);
    }
  }

  public static final synchronized void checkOpenRequestSequences(final boolean flush) {
    Thread currentThread = Thread.currentThread();

    long openSequences =
        RequestThread.threadMap.values().stream().filter(t -> t != currentThread).count();

    if (flush) {
      RequestThread.threadMap.clear();

      KoLmafia.updateDisplay(openSequences + " request sequences will be ignored.");
      KoLmafia.enableDisplay();
    } else {
      KoLmafia.updateDisplay(openSequences + " open request sequences detected.");
    }

    StaticEntity.printThreadDump();
  }

  public static final synchronized boolean hasOpenRequestSequences() {
    return !RequestThread.threadMap.isEmpty();
  }

  public static final synchronized Integer openRequestSequence() {
    return RequestThread.openRequestSequence(RequestThread.threadMap.isEmpty());
  }

  public static final synchronized Integer openRequestSequence(final boolean forceContinue) {
    if (forceContinue) {
      KoLmafia.forceContinue();
    }

    int requestId = RequestThread.nextRequestId.getAndIncrement();

    Integer requestIdObj = requestId;

    // Don't include relay requests in "request sequences" - this could stop the display from being
    // enabled
    // if it ends up being the last thread removed from the threadMap.
    if (!StaticEntity.isRelayThread())
      RequestThread.threadMap.put(requestIdObj, Thread.currentThread());

    return requestIdObj;
  }

  public static final synchronized void closeRequestSequence(final Integer requestIdObj) {
    Thread thread = RequestThread.threadMap.remove(requestIdObj);

    if (thread == null || !RequestThread.threadMap.isEmpty()) {
      return;
    }

    if (KoLmafia.getLastMessage().endsWith("...")) {
      KoLmafia.updateDisplay("Requests complete.");
      SystemTrayFrame.showBalloon("Requests complete.");
      RequestLogger.printLine();
    }

    if (KoLmafia.permitsContinue() || KoLmafia.refusesContinue()) {
      KoLmafia.enableDisplay();
    }
  }

  /**
   * Declare world peace. This clears all pending requests and queued commands and notifies all
   * currently running requests that they should stop as soon as possible.
   */
  public static final void declareWorldPeace() {
    StaticEntity.userAborted = true;
    var worldPeace =
        (KoLCharacter.getFamiliar().getId() == FamiliarPool.PEACE_TURKEY)
            ? "whirled peas"
            : "world peace";
    var messageText = "KoLmafia declares " + worldPeace + ".";
    KoLmafia.updateDisplay(MafiaState.ABORT, messageText);
    KoLmafiaASH.stopAllRelayInterpreters();
    JavascriptRuntime.interruptAll();
    InternalMessage message = new InternalMessage(messageText, "red");
    ChatManager.broadcastEvent(message);
  }

  private static class SequencedRunnable implements Runnable {
    private final Runnable wrapped;
    private final boolean sequence;

    public SequencedRunnable(final Runnable wrapped, final boolean sequence) {
      this.wrapped = wrapped;
      this.sequence = sequence;
    }

    @Override
    public void run() {
      Integer requestId = null;

      try {
        if (this.sequence) {
          requestId = RequestThread.openRequestSequence();
        }
        this.wrapped.run();
      } catch (Exception e) {
        StaticEntity.printStackTrace(e);
      } finally {
        if (requestId != null) {
          RequestThread.closeRequestSequence(requestId);
        }
      }
    }
  }
}
