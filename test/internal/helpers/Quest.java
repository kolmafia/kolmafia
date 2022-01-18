package internal.helpers;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Quest {
  private static class IsStep extends TypeSafeMatcher<QuestDatabase.Quest> {
    private String step;

    public IsStep(String step) {
      this.step = step;
    }

    @Override
    protected boolean matchesSafely(QuestDatabase.Quest q) {
      return QuestDatabase.isQuestStep(q, step);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("be at step " + step);
    }
  }

  public static Matcher<QuestDatabase.Quest> isStep(String step) {
    return new IsStep(step);
  }

  public static Matcher<QuestDatabase.Quest> isUnstarted() {
    return new IsStep(QuestDatabase.UNSTARTED);
  }

  public static Matcher<QuestDatabase.Quest> isStarted() {
    return new IsStep(QuestDatabase.STARTED);
  }

  public static Matcher<QuestDatabase.Quest> isFinished() {
    return new IsStep(QuestDatabase.FINISHED);
  }
}
