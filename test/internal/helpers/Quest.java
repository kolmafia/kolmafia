package internal.helpers;

import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class Quest {
  public static Matcher<QuestDatabase.Quest> isStep(Matcher<? super String> stepMatcher) {
    return new FeatureMatcher<QuestDatabase.Quest, String>(
        stepMatcher, "quest step to be", "quest step") {
      @Override
      protected String featureValueOf(QuestDatabase.Quest actual) {
        return QuestDatabase.getQuest(actual);
      }
    };
  }

  public static Matcher<QuestDatabase.Quest> isStep(String step) {
    return isStep(equalTo(step));
  }

  public static Matcher<QuestDatabase.Quest> isStep(int stepNumber) {
    return isStep(equalTo("step" + stepNumber));
  }

  public static Matcher<QuestDatabase.Quest> isUnstarted() {
    return isStep(QuestDatabase.UNSTARTED);
  }

  public static Matcher<QuestDatabase.Quest> isStarted() {
    return isStep(QuestDatabase.STARTED);
  }

  public static Matcher<QuestDatabase.Quest> isFinished() {
    return isStep(QuestDatabase.FINISHED);
  }
}
