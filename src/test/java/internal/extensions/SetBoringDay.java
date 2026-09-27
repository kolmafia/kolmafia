package internal.extensions;

import static internal.extensions.CheckNested.isNested;

import java.time.LocalDateTime;
import java.time.Month;
import net.sourceforge.kolmafia.utilities.TestStatics;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SetBoringDay implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    if (isNested(context)) return;
    SetBoringDay.setDay();
  }

  public static void setDay() {
    // a boring day in which nothing special happens
    TestStatics.setDate(LocalDateTime.of(2023, Month.AUGUST, 1, 12, 0));
  }
}
