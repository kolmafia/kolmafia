package internal.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Expected an integer with the sign of: ").appendValue(sign);
      }

      @Override
      public boolean matchesSafely(Integer actual) {
        return Integer.signum(actual) == sign.getSignum();
      }
    };
  }
}
