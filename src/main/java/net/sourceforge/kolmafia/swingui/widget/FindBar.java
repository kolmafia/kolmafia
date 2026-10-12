package net.sourceforge.kolmafia.swingui.widget;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.icons.FlatTabbedPaneCloseIcon;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;

public class FindBar extends JPanel {
  private static final Color MATCH_COLOR = new Color(255, 214, 0);
  private static final Color CURRENT_COLOR = new Color(255, 140, 0);
  private static final int ARROW_SIZE = 9;

  private final JEditorPane pane;
  private final Runnable onClose;
  private final JTextField field = new JTextField(20);
  private final JLabel status = new JLabel();
  private final OutputListener outputListener = new OutputListener();
  private final MatchPainter painter = new MatchPainter();

  private Pattern pattern;
  private int length;
  private final List<Integer> starts = new ArrayList<>();
  private int current = -1;
  private int anchor;
  private Object highlight;

  public FindBar(final JEditorPane pane, final Runnable onClose) {
    super(new BorderLayout(4, 0));
    this.pane = pane;
    this.onClose = onClose;

    var tools = new JToolBar();
    tools.putClientProperty(FlatClientProperties.STYLE_CLASS, "inTextField");
    tools.setFloatable(false);
    this.status.setBorder(new FlatEmptyBorder(0, 4, 0, 4));
    tools.add(this.status);
    tools.addSeparator();
    tools.add(
        FindBar.button(
            new ArrowIcon(SwingConstants.NORTH), "Previous match (Shift+Enter)", this::previous));
    tools.add(
        FindBar.button(new ArrowIcon(SwingConstants.SOUTH), "Next match (Enter)", this::next));
    var close = FindBar.button(new FlatTabbedPaneCloseIcon(), "Close (Esc)", this::close);
    close.putClientProperty(
        FlatClientProperties.STYLE,
        "toolbar.hoverBackground: null; toolbar.pressedBackground: null");
    tools.add(close);

    this.field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Find");
    this.field.putClientProperty(
        FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
    this.add(this.field, BorderLayout.CENTER);

    if (UIManager.getLookAndFeel() instanceof FlatLaf) {
      this.field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, tools);
    } else {
      close.setIcon(null);
      close.setText("×");
      this.add(tools, BorderLayout.EAST);
    }

    this.field
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                FindBar.this.search();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                FindBar.this.search();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {}
            });
    this.field.addActionListener(e -> this.next());
    JComponentUtilities.addHotKey(
        this.field, KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK, e -> this.previous());
    JComponentUtilities.addHotKey(this.field, KeyEvent.VK_ESCAPE, e -> this.close());

    this.setVisible(false);
  }

  private static JButton button(final Icon icon, final String tooltip, final Runnable action) {
    var button = new JButton(icon);
    button.putClientProperty(FlatClientProperties.STYLE_CLASS, "inTextField");
    button.setToolTipText(tooltip);
    button.setFocusable(false);
    button.addActionListener(e -> action.run());
    return button;
  }

  public void installShortcut(final JComponent root) {
    JComponentUtilities.addHotKey(
        root,
        KeyEvent.VK_F,
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
        e -> this.open());
  }

  public void open() {
    if (!this.isVisible()) {
      this.setVisible(true);
      this.pane.getDocument().addDocumentListener(this.outputListener);
      try {
        this.highlight = this.pane.getHighlighter().addHighlight(0, 0, this.painter);
      } catch (BadLocationException e) {
      }
    }

    this.field.selectAll();
    this.field.requestFocusInWindow();
    this.search();
  }

  public void close() {
    this.setVisible(false);
    this.pane.getDocument().removeDocumentListener(this.outputListener);
    if (this.highlight != null) {
      this.pane.getHighlighter().removeHighlight(this.highlight);
      this.highlight = null;
    }
    this.starts.clear();
    this.pattern = null;
    this.pane.repaint();
    this.onClose.run();
  }

  void find(final String query) {
    this.field.setText(query);
  }

  int matchCount() {
    return this.starts.size();
  }

  int currentIndex() {
    return this.current;
  }

  int currentOffset() {
    return this.anchor;
  }

  void next() {
    this.step(1);
  }

  void previous() {
    this.step(-1);
  }

  private void step(final int direction) {
    if (this.starts.isEmpty()) {
      return;
    }

    this.select(Math.floorMod(this.current + direction, this.starts.size()));
  }

  private void search() {
    var query = this.field.getText();
    this.length = query.length();
    this.pattern =
        query.isEmpty()
            ? null
            : Pattern.compile(
                Pattern.quote(query), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    this.starts.clear();
    this.starts.addAll(this.scan(0, this.pane.getDocument().getLength(), Integer.MAX_VALUE));
    this.select(this.starts.isEmpty() ? -1 : 0);
  }

  private List<Integer> scan(final int from, final int to, final int startsBefore) {
    var found = new ArrayList<Integer>();
    var start = Math.max(0, from);
    var end = Math.min(this.pane.getDocument().getLength(), to);

    if (this.pattern == null || end - start < this.length) {
      return found;
    }

    var text = new Segment();
    try {
      this.pane.getDocument().getText(start, end - start, text);
    } catch (BadLocationException e) {
      return found;
    }

    var matcher = this.pattern.matcher(text);
    while (matcher.find() && start + matcher.start() < startsBefore) {
      found.add(start + matcher.start());
    }
    return found;
  }

  private void edited(final int offset, final int inserted, final int removed) {
    if (this.pattern == null) {
      return;
    }

    var end = offset + removed;
    var delta = inserted - removed;
    var from = this.lowerBound(offset - this.length + 1);

    this.starts.subList(from, this.lowerBound(end)).clear();
    for (int i = from; i < this.starts.size(); i++) {
      this.starts.set(i, this.starts.get(i) + delta);
    }
    this.anchor = this.anchor >= end ? this.anchor + delta : Math.min(this.anchor, offset);

    this.starts.addAll(
        from,
        this.scan(
            offset - this.length + 1, offset + inserted + this.length - 1, offset + inserted));

    this.follow();
    this.updateStatus();
  }

  private int lowerBound(final int offset) {
    var index = Collections.binarySearch(this.starts, offset);
    return index >= 0 ? index : -index - 1;
  }

  private void follow() {
    if (this.starts.isEmpty()) {
      this.current = -1;
      return;
    }

    this.current = Math.min(this.lowerBound(this.anchor), this.starts.size() - 1);
    this.anchor = this.starts.get(this.current);
  }

  private void select(final int index) {
    this.current = index;
    if (index != -1) {
      this.anchor = this.starts.get(index);
      this.reveal(this.anchor);
    }
    this.updateStatus();
    this.pane.repaint();
  }

  private void reveal(final int offset) {
    try {
      var start = this.pane.modelToView2D(offset);
      var end = this.pane.modelToView2D(offset + this.length);
      if (start == null || end == null) {
        return;
      }

      var bounds = start.createUnion(end).getBounds();
      Runnable scroll = () -> this.pane.scrollRectToVisible(bounds);

      if (this.pane.getClientProperty(StickyListener.class) instanceof StickyListener sticky) {
        sticky.scrollAsUser(scroll);
      } else {
        scroll.run();
      }
    } catch (BadLocationException e) {
    }
  }

  private void updateStatus() {
    if (this.pattern == null) {
      this.status.setText("");
    } else if (this.starts.isEmpty()) {
      this.status.setText("No matches");
    } else {
      this.status.setText((this.current + 1) + " of " + this.starts.size());
    }
  }

  private class OutputListener implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) {
      FindBar.this.edited(e.getOffset(), e.getLength(), 0);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      FindBar.this.edited(e.getOffset(), 0, e.getLength());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {}
  }

  private class MatchPainter implements Highlighter.HighlightPainter {
    private Color background;
    private Highlighter.HighlightPainter matchPainter;
    private Highlighter.HighlightPainter currentPainter;

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
      var starts = FindBar.this.starts;
      var length = FindBar.this.length;
      if (starts.isEmpty()) {
        return;
      }

      this.updateColors(c.getBackground());

      var clip = g.getClipBounds();
      if (clip == null) {
        clip = bounds.getBounds();
      }

      var first = c.viewToModel2D(new Point(0, clip.y)) - length;
      var last = c.viewToModel2D(new Point(c.getWidth(), clip.y + clip.height)) + length;

      for (int i = FindBar.this.lowerBound(first); i < starts.size(); i++) {
        var start = starts.get(i);
        if (start > last) {
          break;
        }

        var painter = i == FindBar.this.current ? this.currentPainter : this.matchPainter;
        painter.paint(g, start, start + length, bounds, c);
      }
    }

    private void updateColors(final Color background) {
      if (background.equals(this.background)) {
        return;
      }

      this.background = background;
      this.matchPainter =
          new DefaultHighlighter.DefaultHighlightPainter(
              ColorFunctions.mix(MATCH_COLOR, background, 0.4f));
      this.currentPainter =
          new DefaultHighlighter.DefaultHighlightPainter(
              ColorFunctions.mix(CURRENT_COLOR, background, 0.65f));
    }
  }

  private record ArrowIcon(int direction) implements Icon {
    @Override
    public int getIconWidth() {
      return UIScale.scale(16);
    }

    @Override
    public int getIconHeight() {
      return UIScale.scale(16);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      var g2 = (Graphics2D) g.create();
      try {
        FlatUIUtils.setRenderingHints(g2);
        var color = UIManager.getColor("ComboBox.buttonArrowColor");
        g2.setColor(color != null && c.isEnabled() ? color : c.getForeground());
        FlatUIUtils.paintArrow(
            g2,
            x,
            y,
            this.getIconWidth(),
            this.getIconHeight(),
            this.direction,
            FlatUIUtils.isChevron(UIManager.getString("Component.arrowType")),
            ARROW_SIZE,
            1f,
            0,
            0);
      } finally {
        g2.dispose();
      }
    }
  }
}
