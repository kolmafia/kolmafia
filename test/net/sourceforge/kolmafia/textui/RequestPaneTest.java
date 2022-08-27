package net.sourceforge.kolmafia.textui;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import org.junit.jupiter.api.Test;

public class RequestPaneTest {
  @Test
  public void testCopyAsHtml() {
    var cleanups = withProperty("copyAsHTML", true);

    try (cleanups) {
      var pane = new RequestPane();
      var html =
          """
                <table border="2" cols="4">
                      <tr>
                        <td>
                          <p>
                            roninStoragePulls
                          </p>
                        </td>
                      </tr>
                    </table>""";
      pane.setText(html);

      pane.select(0, pane.getText().length());

      assertThat(pane.getSelectedText(), equalTo(html));
    }
  }

  @Test
  public void testCopyWithoutHtml() {
    var cleanups = withProperty("copyAsHTML", false);

    try (cleanups) {
      var pane = new RequestPane();
      var html =
          """
                <table border="2" cols="4">
                      <tr>
                        <td>
                          <p>
                            roninStoragePulls
                          </p>
                        </td>
                      </tr>
                    </table>""";
      var withoutHtml = "roninStoragePulls";
      pane.setText(html);

      pane.select(0, pane.getText().length());

      assertThat(pane.getSelectedText(), not(containsString("<")));
      assertThat(pane.getSelectedText(), containsString(withoutHtml));
    }
  }

  @Test
  public void testCopyWithoutHtmlTrimmed() {
    var cleanups = withProperty("copyAsHTML", false);

    try (cleanups) {
      var pane = new RequestPane();
      var html =
          """
                <table border="2" cols="4">
                      <tr>
                        <td>
                          <p>
                            roninStoragePulls
                          </p>
                        </td>
                      </tr>
                    </table>""";
      var withoutHtml = "roninStoragePulls";
      pane.setText(html);

      pane.select(0, pane.getText().length());

      assertThat(pane.getSelectedText(), equalTo(withoutHtml));
    }
  }
}
