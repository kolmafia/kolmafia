package internal.helpers;

import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class Preference {
  public static Matcher<String> hasStringValue(Matcher<? super String> prefMatcher) {
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

  public static Matcher<String> hasFloatValue(Matcher<? super Float> prefMatcher) {
    return new FeatureMatcher<String, Float>(prefMatcher, "preference to be", "preference") {
      @Override
      protected Float featureValueOf(String pref) {
        return Preferences.getFloat(pref);
      }
    };
  }

  public static Matcher<String> isSetTo(Object value) {
    return hasStringValue(equalTo(value.toString()));
  }

  public static Matcher<String> isSetTo(float value) {
    return hasFloatValue(equalTo(value));
  }
}
