package internal.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public class TimeTests
    implements BeforeAllCallback,
        AfterAllCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback {
  private static final String TEST_START = "TEST_START";
  private static final String TEST_METHOD_START = "TEST_METHOD_START";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    getClassStore(context).put(TEST_START, System.currentTimeMillis());
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    getMethodStore(context).put(TEST_METHOD_START, System.currentTimeMillis());
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    var testClass = context.getRequiredTestClass();
    var testMethod = context.getRequiredTestMethod();
    var startTime = getMethodStore(context).remove(TEST_METHOD_START, long.class);
    var duration = System.currentTimeMillis() - startTime;

    System.out.printf(
        "%s ms\t[%s:%s]%n", duration, testClass.getSimpleName(), testMethod.getName());
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    var testClass = context.getRequiredTestClass();
    var startTime = getClassStore(context).remove(TEST_START, long.class);
    var duration = System.currentTimeMillis() - startTime;

    System.out.printf("%s ms\t[%s]%n", duration, testClass.getSimpleName());
  }

  private Store getClassStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass()));
  }

  private Store getMethodStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }
}
