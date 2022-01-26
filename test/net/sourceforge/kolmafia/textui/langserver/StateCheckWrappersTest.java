package net.sourceforge.kolmafia.textui.langserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class StateCheckWrappersTest extends AshLanguageServerTest {
  // Maintain an array of references to the loggers we disable
  // because they are stored as WeakRefs and we need them to last
  // for the duration of this collection of tests.
  static Set<Logger> disabledLoggers = new HashSet<>();

  @BeforeAll
  private static void disableLoggers() {
    Set.of(StreamMessageProducer.class, RemoteEndpoint.class)
        .forEach(
            c -> {
              var logger = Logger.getLogger(c.getName());
              logger.setUseParentHandlers(false);
              disabledLoggers.add(logger);
            });
  }

  @AfterAll
  private static void disposeDisabledLoggers() {
    disabledLoggers.clear();
  }

  private static class AshLanguageServerNotingIgnoredNotifications
      extends StateCheckWrappers.AshLanguageServer {
    private boolean ignoredNotification;

    @Override
    boolean isActive() {
      if (super.isActive()) {
        return true;
      }

      this.ignoredNotification = true;
      return false;
    }
  }

  @Override
  protected AshLanguageServer launchServer(InputStream in, OutputStream out) {
    return AshLanguageServer.launch(in, out, new AshLanguageServerNotingIgnoredNotifications());
  }

  private static Map<Method[], List<Method>> getRequestsAndNotifications() {
    Map<Method[], List<Method>> map = new HashMap<>();
    populateRequestsAndNotifications(map, LanguageServer.class.getMethods(), new Method[0]);
    return map;
  }

  private static void populateRequestsAndNotifications(
      Map<Method[], List<Method>> map, Method[] currentMethods, Method[] currentDelegates) {
    List<Method> lspMethods = new LinkedList<>();

    for (Method method : currentMethods) {
      if (isStateRelatedMethod(method) || method.getAnnotation(Deprecated.class) != null) {
        // Ignore
      } else if (method.getAnnotation(JsonRequest.class) != null
          || method.getAnnotation(JsonNotification.class) != null) {
        lspMethods.add(method);
      } else if (method.getAnnotation(JsonDelegate.class) != null) {
        Method[] newDelegates = new Method[currentDelegates.length + 1];
        System.arraycopy(currentDelegates, 0, newDelegates, 0, currentDelegates.length);
        newDelegates[currentDelegates.length] = method;

        populateRequestsAndNotifications(map, method.getReturnType().getMethods(), newDelegates);
      }
    }

    map.put(currentDelegates, lspMethods);
  }

  private static boolean isStateRelatedMethod(Method method) {
    return methodEquals(method, "initialize", InitializeParams.class)
        || methodEquals(method, "initialized", InitializedParams.class)
        || methodEquals(method, "initialized")
        || methodEquals(method, "shutdown")
        || methodEquals(method, "exit");
  }

  private static boolean methodEquals(Method method, String name, Class<?>... parameterTypes) {
    try {
      return method.equals(LanguageServer.class.getMethod(name, parameterTypes));
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private static Object makeSimplestInstancePossible(final Class<?> clazz) {
    // Try with the version with no arguments
    try {
      return clazz.getConstructor().newInstance();
    } catch (NoSuchMethodException
        | InstantiationException
        | InvocationTargetException
        | IllegalAccessException e) {
      // No luck
      // Breadth-first would probably be better, but implementing it here seems like a pain...
      for (Constructor<?> constructor : clazz.getConstructors()) {
        Class<?>[] parameters = constructor.getParameterTypes();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
          arguments[i] = makeSimplestInstancePossible(parameters[i]);
        }

        try {
          return constructor.newInstance(arguments);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
        }
      }
    }

    // Our efforts were fruitless. Last chance is that null works
    return null;
  }

  private static Object invokeWithDefaultParameters(Method method, Object obj)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException,
          InstantiationException {
    Object[] params;
    Class<?>[] paramTypes = method.getParameterTypes();

    if (paramTypes.length == 0) {
      params = new Object[0];
    } else {
      // LSP4J methods have at most 1 parameter (a single object grouping every "actual" parameter)
      // These parameter classes always have a default constructor
      // ...or so I thought. Damn.
      params = new Object[1];

      params[0] = makeSimplestInstancePossible(paramTypes[0]);
    }

    return method.invoke(obj, params);
  }

  private void clearIgnoredNotification() {
    ((AshLanguageServerNotingIgnoredNotifications) trueServer).ignoredNotification = false;
  }

  private boolean ignoredNotification() {
    return ((AshLanguageServerNotingIgnoredNotifications) trueServer).ignoredNotification;
  }

  public void testWithAllMethods(
      BiConsumer<String, Supplier<CompletableFuture<?>>> requestConsumer,
      BiConsumer<String, Runnable> notificationConsumer)
      throws IllegalAccessException, InvocationTargetException {
    for (final Entry<Method[], List<Method>> entry : getRequestsAndNotifications().entrySet()) {
      final Method[] delegateMethods = entry.getKey();
      final List<Method> methods = entry.getValue();

      Object delegate = proxyServer;

      for (final Method delegateMethod : delegateMethods) {
        delegate = delegateMethod.invoke(delegate, new Object[0]);
      }

      final Object finalDelegate = delegate;

      for (final Method method : methods) {
        clearIgnoredNotification();

        // Prepare a short method name for the error messages
        StringBuilder s = new StringBuilder(method.getDeclaringClass().getSimpleName());
        s.append(".");
        s.append(method.getName());
        StringJoiner params = new StringJoiner(", ", "(", ")");
        for (Class<?> param : method.getParameterTypes()) {
          params.add(param.getSimpleName());
        }
        s.append(params.toString());

        String methodName = s.toString();

        if (method.getAnnotation(JsonRequest.class) != null) {
          requestConsumer.accept(
              methodName,
              () -> {
                try {
                  return (CompletableFuture<?>) invokeWithDefaultParameters(method, finalDelegate);
                } catch (Throwable e) {
                  Assertions.fail("Access to " + methodName + " failed", e);
                  return null;
                }
              });
        } else {
          notificationConsumer.accept(
              methodName,
              () -> {
                try {
                  invokeWithDefaultParameters(method, finalDelegate);
                } catch (Throwable e) {
                  Assertions.fail("Access to " + methodName + " failed", e);
                }

                pauser.pause(100);
              });
        }
      }
    }
  }

  @Test
  public void preInitializationTest() throws IllegalAccessException, InvocationTargetException {
    testWithAllMethods(
        (methodName, requestCaller) -> {
          // Pre-initialization requests should complete exceptionally with
          // serverNotInitialized
          ExecutionException error =
              Assertions.assertThrows(
                  ExecutionException.class, requestCaller.get()::get, methodName);

          ResponseErrorException response =
              Assertions.assertInstanceOf(
                  ResponseErrorException.class, error.getCause(), methodName);

          Assertions.assertEquals("Server was not initialized", response.getMessage(), methodName);
          Assertions.assertEquals(
              ResponseErrorCode.serverNotInitialized.getValue(),
              response.getResponseError().getCode(),
              methodName);
        },
        (methodName, notificationRunner) -> {
          // Pre-initialization notifications should simply be ignored
          notificationRunner.run();
          pauser.pause(100);

          Assertions.assertTrue(ignoredNotification(), methodName);
        });

    // State-related methods

    // initialize(InitializeParams) is expected. Would change the state.

    // initialized(InitializedParams) acts like any other notification pre-initialization
    clearIgnoredNotification();
    proxyServer.initialized(new InitializedParams());
    pauser.pause(100);

    Assertions.assertTrue(ignoredNotification());

    // shutdown() acts like any other request pre-initialization
    ExecutionException error =
        Assertions.assertThrows(ExecutionException.class, proxyServer.shutdown()::get);

    ResponseErrorException response =
        Assertions.assertInstanceOf(ResponseErrorException.class, error.getCause());

    Assertions.assertEquals("Server was not initialized", response.getMessage());
    Assertions.assertEquals(
        ResponseErrorCode.serverNotInitialized.getValue(), response.getResponseError().getCode());

    // exit() is always accepted
    clearIgnoredNotification();
    proxyServer.exit();
    pauser.pause(100);

    Assertions.assertFalse(ignoredNotification());

    Assertions.assertTrue(trueServer.executor.isShutdown());
  }

  @Test
  public void postInitializationTest() throws IllegalAccessException, InvocationTargetException {
    initialize(new InitializeParams());

    testWithAllMethods(
        (methodName, requestCaller) -> {
          // Post-initialization requests should be accepted
          try {
            requestCaller.get().get();
          } catch (InterruptedException e) {
            Assertions.fail("We were interrupted during " + methodName);
          } catch (ExecutionException e) {
            ResponseErrorException exception =
                Assertions.assertInstanceOf(ResponseErrorException.class, e.getCause());
            ResponseError response = exception.getResponseError();

            Assertions.assertNotEquals(
                ResponseErrorCode.serverNotInitialized.getValue(), response.getCode(), methodName);
            Assertions.assertNotEquals(
                "Server was already initialized", response.getMessage(), methodName);
            Assertions.assertNotEquals("Server was shut down", response.getMessage(), methodName);
          }
        },
        (methodName, notificationRunner) -> {
          // Post-initialization notifications should be accepted
          notificationRunner.run();
          pauser.pause(100);

          Assertions.assertFalse(ignoredNotification(), methodName);
        });

    // State-related methods

    // initialize(InitializeParams) may only be sent once
    ExecutionException error =
        Assertions.assertThrows(
            ExecutionException.class, proxyServer.initialize(new InitializeParams())::get);

    ResponseErrorException response =
        Assertions.assertInstanceOf(ResponseErrorException.class, error.getCause());

    Assertions.assertEquals("Server was already initialized", response.getMessage());
    Assertions.assertEquals(
        ResponseErrorCode.InvalidRequest.getValue(), response.getResponseError().getCode());

    // initialized(InitializedParams) should only be accepted if the first notification after
    // initialize(InitializeParams). Our implementation currently doesn't do that, so skip

    // shutdown() is expected. Would change the state.

    // exit() is always accepted
    clearIgnoredNotification();
    proxyServer.exit();
    pauser.pause(100);

    Assertions.assertFalse(ignoredNotification());

    Assertions.assertTrue(trueServer.executor.isShutdown());
  }

  @Test
  public void postShutdownTest() throws IllegalAccessException, InvocationTargetException {
    initialize(new InitializeParams());

    Assertions.assertDoesNotThrow(
        (ThrowingSupplier<Object>) proxyServer.shutdown()::get, "Shutdown failed");

    testWithAllMethods(
        (methodName, requestCaller) -> {
          // Post-shutdown requests should complete exceptionally with InvalidRequest
          ExecutionException error =
              Assertions.assertThrows(
                  ExecutionException.class, requestCaller.get()::get, methodName);

          ResponseErrorException response =
              Assertions.assertInstanceOf(
                  ResponseErrorException.class, error.getCause(), methodName);

          Assertions.assertEquals("Server was shut down", response.getMessage(), methodName);
          Assertions.assertEquals(
              ResponseErrorCode.InvalidRequest.getValue(),
              response.getResponseError().getCode(),
              methodName);
        },
        (methodName, notificationRunner) -> {
          // Post-shutdown notifications should simply be ignored
          notificationRunner.run();
          pauser.pause(100);

          Assertions.assertTrue(ignoredNotification(), methodName);
        });

    // State-related methods

    // initialize(InitializeParams) acts like any other requests post-shutdown
    ExecutionException error =
        Assertions.assertThrows(
            ExecutionException.class, proxyServer.initialize(new InitializeParams())::get);

    ResponseErrorException response =
        Assertions.assertInstanceOf(ResponseErrorException.class, error.getCause());

    Assertions.assertEquals("Server was shut down", response.getMessage());
    Assertions.assertEquals(
        ResponseErrorCode.InvalidRequest.getValue(), response.getResponseError().getCode());

    // initialized(InitializedParams) acts like any other notification post-shutdown
    clearIgnoredNotification();
    proxyServer.initialized(new InitializedParams());
    pauser.pause(100);

    Assertions.assertTrue(ignoredNotification());

    // shutdown() acts like any other request post-shutdown
    error = Assertions.assertThrows(ExecutionException.class, proxyServer.shutdown()::get);

    response = Assertions.assertInstanceOf(ResponseErrorException.class, error.getCause());

    Assertions.assertEquals("Server was shut down", response.getMessage());
    Assertions.assertEquals(
        ResponseErrorCode.InvalidRequest.getValue(), response.getResponseError().getCode());

    // exit() is always accepted
    clearIgnoredNotification();
    proxyServer.exit();
    pauser.pause(100);

    Assertions.assertFalse(ignoredNotification());

    Assertions.assertTrue(trueServer.executor.isShutdown());
  }
}
