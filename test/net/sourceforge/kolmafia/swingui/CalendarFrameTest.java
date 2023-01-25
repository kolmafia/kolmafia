package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JRootPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalendarFrameTest {

  private final String USER_NAME = "CalendarFrameTestFakeUser";

  // These need to be before and after each because leakage has been observed between tests
  // in this class.
  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset(USER_NAME);
    KoLCharacter.reset(true);
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    File userFile = new File("settings/" + USER_NAME.toLowerCase() + "_prefs.txt");
    if (userFile.exists()) {
      userFile.delete();
    }
  }

  private String writeDocumentToString(HTMLDocument htmlDocument) {
    StringWriter sw = new StringWriter();
    HTMLWriter w =
        new HTMLWriter(sw, htmlDocument, 0, 10000) {
          {
            setLineLength(999_999);
          }
        };
    try {
      w.write();
      return sw.toString();
    } catch (IOException | BadLocationException e) {
      e.printStackTrace();
      fail();
    }
    return "";
  }

  public String getDataFromTab(Component aTab) {
    assertThat(aTab, instanceOf(RequestPane.class));
    RequestPane rPane = (RequestPane) aTab;
    Document document = rPane.getDocument();
    assertThat(document, instanceOf(HTMLDocument.class));
    HTMLDocument hDoc = (HTMLDocument) document;
    return writeDocumentToString(hDoc);
  }

  private static final String expectedKOLDay =
      "<html>"
          + KoLConstants.LINE_BREAK
          + "  <head>"
          + KoLConstants.LINE_BREAK
          + "    "
          + KoLConstants.LINE_BREAK
          + "  </head>"
          + KoLConstants.LINE_BREAK
          + "  <body>"
          + KoLConstants.LINE_BREAK
          + "    <center>"
          + KoLConstants.LINE_BREAK
          + "      <table>"
          + KoLConstants.LINE_BREAK
          + "        <tr>"
          + KoLConstants.LINE_BREAK
          + "          <td valign=\"top\">"
          + KoLConstants.LINE_BREAK
          + "            <center>"
          + KoLConstants.LINE_BREAK
          + "              <table border=\"1\">"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"center\">"
          + KoLConstants.LINE_BREAK
          + "                    drawn by <b><a href=\"http://elfwood.lysator.liu.se/loth/l/e/leigh/leigh.html\">SpaceMonkey</a></b>"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    <img src=\"https://d2uyhvukfffg5a.cloudfront.net/otherimages/bikini/nov.gif\">"
          + KoLConstants.LINE_BREAK
          + "                    "
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"center\">"
          + KoLConstants.LINE_BREAK
          + "                    September 1, 2010"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"center\">"
          + KoLConstants.LINE_BREAK
          + "                    <font size=\"+1\"><b>Boozember 2</b></font>"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "              </table>"
          + KoLConstants.LINE_BREAK
          + "            </center>"
          + KoLConstants.LINE_BREAK
          + "          </td>"
          + KoLConstants.LINE_BREAK
          + "          <td valign=\"top\">"
          + KoLConstants.LINE_BREAK
          + "            <center>"
          + KoLConstants.LINE_BREAK
          + "              <table>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td colspan=\"2\" align=\"center\">"
          + KoLConstants.LINE_BREAK
          + "                    "
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td colspan=\"2\">"
          + KoLConstants.LINE_BREAK
          + "                    "
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td colspan=\"2\" align=\"center\">"
          + KoLConstants.LINE_BREAK
          + "                    <img src=\"https://d2uyhvukfffg5a.cloudfront.net/itemimages/smoon2.gif\">"
          + KoLConstants.LINE_BREAK
          + "                    <img src=\"https://d2uyhvukfffg5a.cloudfront.net/itemimages/smoon1a.gif\">"
          + KoLConstants.LINE_BREAK
          + "                    "
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td colspan=\"2\">"
          + KoLConstants.LINE_BREAK
          + "                    "
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Ronald</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    waxing crescent"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Grimace</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    new moon"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Stats</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    3 days until Mysticism."
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Grue</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    bloodlusty"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Blood</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    +38%"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Baio</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    +20%"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "                <tr>"
          + KoLConstants.LINE_BREAK
          + "                  <td align=\"right\">"
          + KoLConstants.LINE_BREAK
          + "                    <b>Jekyllin</b>:&#160;"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                  <td>"
          + KoLConstants.LINE_BREAK
          + "                    +7 stats, 25% items"
          + KoLConstants.LINE_BREAK
          + "                  </td>"
          + KoLConstants.LINE_BREAK
          + "                </tr>"
          + KoLConstants.LINE_BREAK
          + "              </table>"
          + KoLConstants.LINE_BREAK
          + "            </center>"
          + KoLConstants.LINE_BREAK
          + "          </td>"
          + KoLConstants.LINE_BREAK
          + "        </tr>"
          + KoLConstants.LINE_BREAK
          + "      </table>"
          + KoLConstants.LINE_BREAK
          + "    </center>"
          + KoLConstants.LINE_BREAK
          + "  </body>"
          + KoLConstants.LINE_BREAK
          + "</html>"
          + KoLConstants.LINE_BREAK;
  private static final String expectedEvents =
      "<html>"
          + KoLConstants.LINE_BREAK
          + "  <head>"
          + KoLConstants.LINE_BREAK
          + "    "
          + KoLConstants.LINE_BREAK
          + "  </head>"
          + KoLConstants.LINE_BREAK
          + "  <body>"
          + KoLConstants.LINE_BREAK
          + "    <b><u>September 1, 2010</u></b><br><i>Boozember 2</i><br>&#160;<br><nobr><b>Muscle Day</b>:&#160;7 days</nobr><br><nobr><b>Mysticality Day</b>:&#160;3 days</nobr><br><nobr><b>Moxie Day</b>:&#160;14 days</nobr><br>&#160;<br><nobr><b>Feast of Boris:</b>&#160; 5 days</nobr><br><nobr><b>Yuletide:</b>&#160; 10 days</nobr><br><nobr><b>Festival of Jarlsberg:</b>&#160; 15 days</nobr><br><nobr><b>Valentine's Day:</b>&#160; 26 days</nobr><br><nobr><b>St. Sneaky Pete's Day:</b>&#160; 33 days</nobr><br><nobr><b>Oyster Egg Day:</b>&#160; 40 days</nobr><br><nobr><b>El Dia De Los Muertos Borrachos:</b>&#160; 48 days</nobr><br><nobr><b>Generic Summer Holiday:</b>&#160; 57 days</nobr><br><nobr><b>Halloween:</b>&#160; 60 days</nobr><br><nobr><b>Dependence Day:</b>&#160; 66 days</nobr><br><nobr><b>Arrrbor Day:</b>&#160; 74 days</nobr><br><nobr><b>Lab&#243;r Day:</b>&#160; 84 days</nobr><br>"
          + KoLConstants.LINE_BREAK
          + "  </body>"
          + KoLConstants.LINE_BREAK
          + "</html>"
          + KoLConstants.LINE_BREAK;

  @Test
  public void calendarFrameShouldHaveBasicFunctionality() {
    CalendarFrame testFrame = new CalendarFrame();
    assertNotEquals(null, testFrame, "CalendarFrame expected to exist when constructed.");
    assertTrue(testFrame.exists(), "CalendarFrame should exist in the Swing sense.");
    assertEquals("CalendarFrame", testFrame.getFrameName());
    assertEquals("Farmer's Almanac (CalendarFrameTestFakeUser)", testFrame.getTitle());
    Component[] components = testFrame.getComponents();
    assertEquals(1, components.length);
    assertThat(components[0], instanceOf(JRootPane.class));
  }

  @Test
  public void itShouldCalculateExpectedCalendarDataForKnownTime() {
    Calendar useTime = new GregorianCalendar();
    useTime.set(2010, Calendar.SEPTEMBER, 1);
    CalendarFrame testFrame = new CalendarFrame(useTime);
    assertNotEquals(null, testFrame, "CalendarFrame expected to exist when constructed.");
    testFrame.updateTabs();
    CalendarFrame.updateDailyPage(1);
    assertEquals(2, testFrame.tabs.getTabCount());
    Component aTab = testFrame.tabs.getComponentAt(0);
    String x = getDataFromTab(aTab);
    assertEquals(expectedKOLDay, x);
  }

  @Test
  public void itShouldCalculateExpectedEventsForKnownTime() {
    Calendar useTime = new GregorianCalendar();
    useTime.set(2010, Calendar.SEPTEMBER, 1);
    CalendarFrame testFrame = new CalendarFrame(useTime);
    assertNotEquals(null, testFrame, "CalendarFrame expected to exist when constructed.");
    testFrame.updateTabs();
    CalendarFrame.updateDailyPage(1);
    assertEquals(2, testFrame.tabs.getTabCount());
    Component aTab = testFrame.tabs.getComponentAt(1);
    String x = getDataFromTab(aTab);
    assertEquals(expectedEvents, x);
  }
}
