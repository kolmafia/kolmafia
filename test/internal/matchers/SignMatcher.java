package internal.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class SignMatcher {
  public enum Sign {
    POSITIVE(1),
    ZERO(0),
    NEGATIVE(-1);

    private final int signum;

    Sign(final int signum) {
      this.signum = signum;
    }

    public int getSignum() {
      return signum;
    }
  }

  public static Matcher<Integer> hasSign(Sign sign) {
    return new BaseMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Expected an integer with the sign of: ").appendValue(sign);
      }

      @Override
      public boolean matches(Object actual) {
        if (!(actual instanceof Integer value)) return false;
        return Integer.signum(value) == sign.getSignum();
      }
    };
  }
}
