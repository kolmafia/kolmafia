package internal.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class StringMatcher {
  /** Matches when {@code substring} appears exactly {@code times} times in the string. */
  public static Matcher<String> containsStringTimes(String substring, int times) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description
            .appendText("Expected a string containing " + times + " occurrences of: ")
            .appendValue(substring);
      }

      @Override
      public boolean matchesSafely(String string) {
        int count = 0;
        int index = string.indexOf(substring);
        while (index != -1) {
          count++;
          index = string.indexOf(substring, index + substring.length());
        }
        return count == times;
      }
    };
  }
}
