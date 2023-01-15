package net.sourceforge.kolmafia.swingui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JRootPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;

import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import org.junit.jupiter.api.Test;

class CalendarFrameTest {

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
    String x = hDoc.toString();
  }
}
