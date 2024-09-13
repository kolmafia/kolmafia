package internal.matchers;

import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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

  public static Matcher<String> hasBooleanValue(Matcher<? super Boolean> prefMatcher) {
    return new FeatureMatcher<String, Boolean>(prefMatcher, "preference to be", "preference") {
      @Override
      protected Boolean featureValueOf(String pref) {
        return Preferences.getBoolean(pref);
      }
    };
  }

  public static Matcher<String> isSetTo(Object value) {
    return hasStringValue(equalTo(value.toString()));
  }

  public static Matcher<String> isSetTo(int value) {
    return hasIntegerValue(equalTo(value));
  }

  public static Matcher<String> isSetTo(float value) {
    return hasFloatValue(equalTo(value));
  }

  public static Matcher<String> isSetTo(boolean value) {
    return hasBooleanValue(equalTo(value));
  }

  public static Matcher<String> isUserPreference() {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("to be valid user preference");
      }

      @Override
      protected boolean matchesSafely(String pref) {
        return Preferences.propertyExists(pref, false);
      }
    };
  }
}
