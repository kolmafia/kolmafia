package net.sourceforge.kolmafia.swingui;

import ca.bcit.geekkit.CalendarTableModel;
import ca.bcit.geekkit.JCalendar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CalendarFrame extends GenericFrame implements ListSelectionListener {
  public static final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);
  public static final SimpleDateFormat LONG_FORMAT =
      new SimpleDateFormat("MMMM d, yyyy", Locale.US);

  static {
    // all dates are presented as if the day begins at rollover

    CalendarFrame.SHORT_FORMAT.setTimeZone(KoLmafia.KOL_TIME_ZONE);
    CalendarFrame.LONG_FORMAT.setTimeZone(KoLmafia.KOL_TIME_ZONE);
  }

  // static final array of file names (not including .gif extension)
  // for the various months in the KoL calendar.

  public static final String[] CALENDARS = {
    "", "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
  };

  // The following are static final variables used to track the calendar.
  // They are made static final as a design decision to allow the oracle
  // table nested inside of this class the access it needs to data.

  private static int calendarDay = 0;
  private static int ronaldPhase = -1;
  private static int grimacePhase = -1;
  private static int hamburglarPosition = -1;

  private static JCalendar calendar;
  private static OracleTable oracleTable;

  private static RequestPane dailyDisplay;
  private static RequestPane predictDisplay;

  private static Calendar selectedDate;
  private static int selectedRow, selectedColumn;

  public CalendarFrame() {
    super("Farmer's Almanac");
    try {
      Calendar useMe = Calendar.getInstance(KoLmafia.KOL_TIME_ZONE, Locale.US);
      buildCalendarFrame(useMe);
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.
      StaticEntity.printStackTrace(e);
    }
  }

  // for testing
  public CalendarFrame(Calendar calendarToUse) {
    super("Farmer's Almanac");
    buildCalendarFrame(calendarToUse);
  }

  private void buildCalendarFrame(Calendar calendarToUse) {
    CalendarFrame.selectedRow = -1;
    CalendarFrame.selectedColumn = -1;
    CalendarFrame.selectedDate = calendarToUse;

    CalendarFrame.calculatePhases(
        CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId()));

    dailyDisplay = new RequestPane();
    JComponentUtilities.setComponentSize(dailyDisplay, 400, 335);

    predictDisplay = new RequestPane();
    JComponentUtilities.setComponentSize(predictDisplay, 400, 335);

    this.tabs.addTab("KoL One-a-Day", dailyDisplay);
    this.tabs.addTab("Upcoming Events", predictDisplay);
    CalendarFrame.calendar = new JCalendar(OracleTable.class);
    CalendarFrame.oracleTable = (OracleTable) CalendarFrame.calendar.getTable();
    CalendarFrame.oracleTable.getSelectionModel().addListSelectionListener(this);
    CalendarFrame.oracleTable.getColumnModel().getSelectionModel().addListSelectionListener(this);

    JPanel calendarPanel = new JPanel(new BorderLayout());
    calendarPanel.add(this.tabs, BorderLayout.CENTER);
    calendarPanel.add(CalendarFrame.calendar, BorderLayout.EAST);
    this.setCenterComponent(calendarPanel);
    this.updateTabs();
  }

  /**
   * Listener method which updates the main HTML panel with information, pending on the user's
   * calendar day selection.
   */
  @Override
  public void valueChanged(final ListSelectionEvent e) {
    // If the person has not yet released the
    // mouse, then do nothing.

    if (e.getValueIsAdjusting()) {
      return;
    }

    // Compute which date is being selected
    // in the calendar table and update the
    // HTML on the center pane as appropriate

    if (CalendarFrame.oracleTable.getSelectedRow() != CalendarFrame.selectedRow
        || CalendarFrame.oracleTable.getSelectedColumn() != CalendarFrame.selectedColumn) {
      try {
        CalendarFrame.selectedRow = CalendarFrame.oracleTable.getSelectedRow();
        CalendarFrame.selectedColumn = CalendarFrame.oracleTable.getSelectedColumn();

        CalendarFrame.selectedDate.set(
            CalendarFrame.calendar.getModel().getCurrentYear(),
            CalendarFrame.calendar.getModel().getCurrentMonth(),
            StringUtilities.parseInt(
                (String)
                    CalendarFrame.calendar
                        .getModel()
                        .getValueAt(CalendarFrame.selectedRow, CalendarFrame.selectedColumn)));

        CalendarFrame.calculatePhases(
            CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId()));
        this.updateTabs();
      } catch (Exception e1) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e1);
      }
    }
  }

  /**
   * Recalculates the moon phases given the time noted in the constructor. This calculation assumes
   * that the straightforward algorithm has no errors.
   */
  private static void calculatePhases(final ZonedDateTime dateTime) {
    // In order to ensure that everything is computed
    // based on new-year, wrap the date inside of the
    // formatter (which strips time information) and
    // reparse the date.

    CalendarFrame.calendarDay = HolidayDatabase.getDayInKoLYear(dateTime);
    int phaseStep = (CalendarFrame.calendarDay + 16) % 16;

    CalendarFrame.ronaldPhase = phaseStep % 8;
    CalendarFrame.grimacePhase = phaseStep / 2;
    CalendarFrame.hamburglarPosition = HolidayDatabase.getHamburglarPosition(dateTime);
  }

  /**
   * Updates the HTML which displays the date and the information relating to the given date. This
   * should be called after all recalculation attempts.
   */
  private static void updateDailyPage() {
    updateDailyPage(KoLConstants.RNG.nextInt(2));
  }

  // visible for testing
  public static void updateDailyPage(int rngVal) {
    if (KoLConstants.DAILY_FORMAT.format(CalendarFrame.selectedDate.getTime()).equals("20051027")) {
      CalendarFrame.dailyDisplay.setText("<center><h1>White Wednesday</h1></center>");
      return;
    }

    StringBuffer displayHTML = new StringBuffer();

    // First display today's date along with the
    // appropriate calendar picture.  Include the
    // link shown in the clan calendar.

    displayHTML.append("<center><table><tr><td valign=top>");
    displayHTML.append("<center><table border=1><tr><td align=center>drawn by <b>");

    // Display either girls or boys of loathing, as desired

    String artistURL;
    String artistName;
    String artDirectory;

    if (rngVal == 1) {
      artistURL = "http://elfwood.lysator.liu.se/loth/l/e/leigh/leigh.html";
      artistName = "SpaceMonkey";
      artDirectory = "bikini";
    } else {
      artistURL = "http://www.myimagehosting.com/album.php?u_id=2341UT9vj";
      artistName = "Cynn";
      artDirectory = "beefcake";
    }

    displayHTML
        .append("<a href=\"")
        .append(artistURL)
        .append("\">")
        .append(artistName)
        .append("</a></b></td></tr>");
    displayHTML.append("<tr><td><img src=\"");
    displayHTML.append(KoLmafia.imageServerPath());
    displayHTML.append("otherimages/");
    displayHTML.append(artDirectory);
    displayHTML.append("/");
    displayHTML.append(
        CalendarFrame.CALENDARS[
            HolidayDatabase.getCalendarMonth(
                CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId()))]);
    displayHTML.append(".gif\"></td></tr><tr><td align=center>");
    displayHTML.append(CalendarFrame.LONG_FORMAT.format(CalendarFrame.selectedDate.getTime()));
    displayHTML.append("</td></tr><tr><td align=center><font size=+1><b>");
    displayHTML.append(
        HolidayDatabase.getCalendarDayAsString(
            CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId())));
    displayHTML.append("</b></font></td></tr></table></center>");

    displayHTML.append("</td><td valign=top>");
    displayHTML.append("<center><table>");

    // Holidays should probably be in the first
    // row, just in case.

    displayHTML.append("<tr><td colspan=2 align=center><b>");
    displayHTML.append(
        HolidayDatabase.getHoliday(
            CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId())));
    displayHTML.append("</b></td></tr><tr><td colspan=2></td></tr>");

    // Next display today's moon phases, including
    // the uber-spiffy name for each phase.  Just
    // like in the browser, Ronald then Grimace.

    displayHTML.append("<tr><td colspan=2 align=\"center\">");
    int hamburglarLight =
        HolidayDatabase.getHamburglarLight(
            CalendarFrame.ronaldPhase,
            CalendarFrame.grimacePhase,
            CalendarFrame.hamburglarPosition);

    if (CalendarFrame.hamburglarPosition == 7) {
      displayHTML.append("<img src=\"");
      displayHTML.append(KoLmafia.imageServerPath());
      displayHTML.append("itemimages/minimoon");
      if (hamburglarLight == 0) {
        displayHTML.append("2");
      }
      displayHTML.append(".gif\">");
    }

    displayHTML.append("<img src=\"");
    displayHTML.append(KoLmafia.imageServerPath());
    displayHTML.append("itemimages/smoon");
    displayHTML.append(CalendarFrame.ronaldPhase + 1);

    if (CalendarFrame.hamburglarPosition == 8 || CalendarFrame.hamburglarPosition == 9) {
      displayHTML.append(CalendarFrame.hamburglarPosition == 8 ? "a" : "b");
    }

    displayHTML.append(".gif\">");

    if (CalendarFrame.hamburglarPosition == 4
        || CalendarFrame.hamburglarPosition == 5
        || CalendarFrame.hamburglarPosition == 10) {
      displayHTML.append("<img src=\"");
      displayHTML.append(KoLmafia.imageServerPath());
      displayHTML.append("itemimages/minimoon");
      if (hamburglarLight == 0) {
        displayHTML.append("2");
      }
      displayHTML.append(".gif\">");
    }

    displayHTML.append("<img src=\"");
    displayHTML.append(KoLmafia.imageServerPath());
    displayHTML.append("itemimages/smoon");
    displayHTML.append(CalendarFrame.grimacePhase + 1);

    if (CalendarFrame.hamburglarPosition == 0 || CalendarFrame.hamburglarPosition == 1) {
      displayHTML.append(CalendarFrame.hamburglarPosition == 0 ? "a" : "b");
    }

    displayHTML.append(".gif\">");

    if (CalendarFrame.hamburglarPosition == 2) {
      displayHTML.append("<img src=\"");
      displayHTML.append(KoLmafia.imageServerPath());
      displayHTML.append("itemimages/minimoon");
      if (hamburglarLight == 0) {
        displayHTML.append("2");
      }
      displayHTML.append(".gif\">");
    }

    displayHTML.append("</td></tr><tr><td colspan=2></td></tr>");

    displayHTML.append("<tr><td align=right><b>Ronald</b>:&nbsp;</td><td>");
    displayHTML.append(HolidayDatabase.getPhaseName(CalendarFrame.ronaldPhase));
    displayHTML.append("</td></tr>");
    displayHTML.append("<tr><td align=right><b>Grimace</b>:&nbsp;</td><td>");
    displayHTML.append(HolidayDatabase.getPhaseName(CalendarFrame.grimacePhase));
    displayHTML.append("</td></tr>");
    displayHTML.append("<tr><td align=right><b>Stats</b>:&nbsp;</td><td>");
    displayHTML.append(
        HolidayDatabase.getMoonEffect(CalendarFrame.ronaldPhase, CalendarFrame.grimacePhase));
    displayHTML.append("</td></tr><td align=right><b>Grue</b>:&nbsp;</td><td>");
    displayHTML.append(
        HolidayDatabase.getGrueEffect(
                CalendarFrame.ronaldPhase,
                CalendarFrame.grimacePhase,
                CalendarFrame.hamburglarPosition)
            ? "bloodlusty"
            : "pacifistic");
    displayHTML.append("</td></tr><td align=right><b>Blood</b>:&nbsp;</td><td>");
    CalendarFrame.appendModifierPercentage(
        displayHTML,
        (int)
            HolidayDatabase.getBloodEffect(
                CalendarFrame.ronaldPhase,
                CalendarFrame.grimacePhase,
                CalendarFrame.hamburglarPosition));
    displayHTML.append("</td></tr><td align=right><b>Baio</b>:&nbsp;</td><td>");
    CalendarFrame.appendModifierPercentage(
        displayHTML,
        HolidayDatabase.getBaioEffect(
            CalendarFrame.ronaldPhase,
            CalendarFrame.grimacePhase,
            CalendarFrame.hamburglarPosition));
    displayHTML.append("</td></tr><td align=right><b>Jekyllin</b>:&nbsp;</td><td>");
    displayHTML.append(
        HolidayDatabase.getJekyllinEffect(
            CalendarFrame.ronaldPhase,
            CalendarFrame.grimacePhase,
            CalendarFrame.hamburglarPosition));
    displayHTML.append("</td></tr></table></center>");

    // That completes the table display!  More data
    // relevant to the current date may follow.
    // A forecast section, maybe, too - but for now,
    // this simple data should be enough.

    displayHTML.append("</td></tr></table></center>");

    // Now that the HTML has been completely
    // constructed, clear the display dailyBuffer
    // and append the appropriate text.

    CalendarFrame.dailyDisplay.setText(displayHTML.toString());
  }

  /** Updates the HTML which displays the predictions for upcoming events on the KoL calendar. */
  private static void updatePredictionsPage() {
    StringBuffer displayHTML = new StringBuffer();
    int phaseStep =
        HolidayDatabase.getPhaseStep(CalendarFrame.ronaldPhase, CalendarFrame.grimacePhase);

    // First display today's date along with the
    // appropriate calendar picture.  Include the
    // link shown in the clan calendar.

    displayHTML.append("<b><u>");
    displayHTML.append(CalendarFrame.LONG_FORMAT.format(CalendarFrame.selectedDate.getTime()));
    displayHTML.append("</u></b><br><i>");
    displayHTML.append(
        HolidayDatabase.getCalendarDayAsString(
            CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId())));
    displayHTML.append("</i><br>&nbsp;<br>");

    HolidayDatabase.addPredictionHTML(
        displayHTML,
        CalendarFrame.selectedDate.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId()),
        phaseStep);

    CalendarFrame.predictDisplay.setText(displayHTML.toString());
  }

  /**
   * Utility method which appends the given percentage to the given string dailyBuffer, complete
   * with + and % signs, wherever applicable. Also appends "no effect" if the percentage is zero.
   */
  private static void appendModifierPercentage(final StringBuffer buffer, final int percentage) {
    if (percentage > 0) {
      buffer.append('+');
      buffer.append(percentage);
      buffer.append('%');
    } else if (percentage < 0) {
      buffer.append(percentage);
      buffer.append('%');
    } else {
      buffer.append("no effect");
    }
  }

  /**
   * Internal class which functions as a table for the JCalendar object. Unlike the standard
   * implementation used by JCalendar, this also highlights stat days and holidays on the KoL
   * calendar.
   */
  public static class OracleTable extends JTable {
    private final Calendar dateCalculator;
    private final CalendarTableModel model;
    private final DefaultTableCellRenderer normalRenderer,
        todayRenderer,
        specialRenderer,
        holidayRenderer;
    private final DefaultTableCellRenderer muscleRenderer, mysticalityRenderer, moxieRenderer;

    public OracleTable(final CalendarTableModel model) {
      super(model);
      this.model = model;

      this.dateCalculator = Calendar.getInstance(KoLmafia.KOL_TIME_ZONE, Locale.US);
      this.normalRenderer = new DefaultTableCellRenderer();

      this.todayRenderer = new DefaultTableCellRenderer();
      this.todayRenderer.setForeground(new Color(255, 255, 255));
      this.todayRenderer.setBackground(new Color(128, 128, 128));

      this.specialRenderer = new DefaultTableCellRenderer();
      this.specialRenderer.setForeground(new Color(255, 255, 255));
      this.specialRenderer.setBackground(new Color(0, 0, 0));

      this.holidayRenderer = new DefaultTableCellRenderer();
      this.holidayRenderer.setForeground(new Color(0, 0, 0));
      this.holidayRenderer.setBackground(new Color(255, 255, 204));

      this.muscleRenderer = new DefaultTableCellRenderer();
      this.muscleRenderer.setForeground(new Color(0, 0, 0));
      this.muscleRenderer.setBackground(new Color(255, 204, 204));

      this.mysticalityRenderer = new DefaultTableCellRenderer();
      this.mysticalityRenderer.setForeground(new Color(0, 0, 0));
      this.mysticalityRenderer.setBackground(new Color(204, 204, 255));

      this.moxieRenderer = new DefaultTableCellRenderer();
      this.moxieRenderer.setForeground(new Color(0, 0, 0));
      this.moxieRenderer.setBackground(new Color(204, 255, 204));
    }

    @Override
    public TableCellRenderer getCellRenderer(final int row, final int column) {
      try {
        // First, if the date today is equal to the
        // date selected, highlight it.

        String dayString = (String) this.model.getValueAt(row, column);
        if (dayString.equals("")) {
          return this.normalRenderer;
        }

        this.dateCalculator.set(
            this.model.getCurrentYear(),
            this.model.getCurrentMonth(),
            StringUtilities.parseInt(dayString));
        ZonedDateTime selectedTime =
            this.dateCalculator.toInstant().atZone(KoLmafia.KOL_TIME_ZONE.toZoneId());

        if (CalendarFrame.SHORT_FORMAT
            .format(new Date())
            .equals(CalendarFrame.SHORT_FORMAT.format(this.dateCalculator.getTime()))) {
          return this.todayRenderer;
        }

        // White wednesday special highlighting.
        // But, because white doesn't show up,
        // make it black instead.

        if (KoLConstants.DAILY_FORMAT.format(this.dateCalculator.getTime()).equals("20051027")) {
          return this.specialRenderer;
        }

        // Otherwise, if the date selected is equal
        // to a special day, then highlight it.

        if (HolidayDatabase.isRealLifeHoliday(selectedTime)) {
          return this.holidayRenderer;
        }

        if (HolidayDatabase.isHoliday(selectedTime)) {
          return this.holidayRenderer;
        }

        if (HolidayDatabase.isMuscleDay(selectedTime)) {
          return this.muscleRenderer;
        }

        if (HolidayDatabase.isMysticalityDay(selectedTime)) {
          return this.mysticalityRenderer;
        }

        if (HolidayDatabase.isMoxieDay(selectedTime)) {
          return this.moxieRenderer;
        }
      } catch (Exception e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }

      return this.normalRenderer;
    }
  }

  public synchronized void updateTabs() {
    CalendarFrame.updateDailyPage();
    CalendarFrame.updatePredictionsPage();
  }
}
