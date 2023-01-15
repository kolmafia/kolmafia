package net.sourceforge.kolmafia.swingui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JRootPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.Test;

class CalendarFrameTest {

  private String writeDocumentToString(HTMLDocument htmlDocument) {
    StringWriter sw = new StringWriter();
    HTMLWriter w =
            new HTMLWriter(
                    sw,
                    htmlDocument,
                    0,
                    10000) {
              {
                setLineLength(999_999);
              }
            };
    try {
      w.write();
      return sw.toString();
    } catch (IOException | BadLocationException e) {
      e.printStackTrace();
      return "";
    }
  }

  @Test
  public void calendarFrameShouldHaveBasicFunctionality() {
    CalendarFrame testFrame = new CalendarFrame();
    assertNotEquals(null, testFrame, "CalendarFrame expected to exist when constructed.");
    assertTrue(testFrame.exists(), "CalendarFrame should exist in the Swing sense.");
    assertEquals("CalendarFrame", testFrame.getFrameName());
    assertEquals("Farmer's Almanac (Not Logged In)", testFrame.getTitle());
    Component[] components = testFrame.getComponents();
    assertEquals(1, components.length);
    assertTrue(components[0] instanceof JRootPane);
  }

  @Test
  public void itShouldCalculateExpectedDataForKnownTime() {
    String expectedKOLDay = "drawn by SpaceMonkeySeptember 1, 2010Boozember 2Ronald: waxing crescentGrimace: new moonStats: 3 days until Mysticism.Grue: bloodlustyBlood: +38%Baio: +20%Jekyllin: +7 stats, 25% items";
    Calendar useTime = new GregorianCalendar();
    useTime.set(2010, Calendar.SEPTEMBER, 1);
    CalendarFrame testFrame = new CalendarFrame(useTime);
    assertNotEquals(null, testFrame, "CalendarFrame expected to exist when constructed.");
    testFrame.updateTabs();
    assertEquals(2, testFrame.tabs.getTabCount());
    Component aTab = testFrame.tabs.getComponentAt(0);
    assertTrue(aTab instanceof RequestPane);
    RequestPane rPane = (RequestPane) aTab;
    Document document = rPane.getDocument();
    assertTrue(document instanceof HTMLDocument);
    HTMLDocument hDoc = (HTMLDocument) document;
    String x = writeDocumentToString(hDoc);
    x = StringUtilities.stripHtml(x);
    assertEquals(expectedKOLDay, x);
  }
}
