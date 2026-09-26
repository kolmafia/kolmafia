package internal.extensions;

import org.junit.jupiter.api.extension.ExtensionContext;

public class CheckNested {
  public static boolean isNested(ExtensionContext extensionContext) {
    var testClassOpt = extensionContext.getTestClass();
    if (testClassOpt.isEmpty()) return false;
    var testClass = testClassOpt.get();
    return testClass.isMemberClass();
  }
}
