package internal.helpers;

import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class Preference {
  public static Matcher<String> hasValue(Matcher<? super String> prefMatcher) {
    return new FeatureMatcher<String, String>(prefMatcher, "preference to be", "preference") {
      @Override
      protected String featureValueOf(String pref) {
        return Preferences.getString(pref);
      }
    };
  }

  public static Matcher<String> hasIntegerValue(Matcher<? super Integer> prefMatcher) {
    return new FeatureMatcher<String, Integer>(prefMatcher, "preference to be", "preference") {
      @Override
      protected Integer featureValueOf(String pref) {
        return Preferences.getInteger(pref);
      }
    };
  }

  public static Matcher<String> isSetTo(Object value) {
    return hasValue(equalTo(value.toString()));
  }
}
