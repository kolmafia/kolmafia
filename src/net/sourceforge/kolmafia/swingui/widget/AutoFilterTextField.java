package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JList;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Script;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.session.StoreManager.StoreLogEntry;
import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoFilterTextField extends AutoHighlightTextField
    implements ActionListener, ListElementFilter {
  protected JList list;
  protected String text;
  protected LockableListModel model;
  protected boolean strict;
  protected int quantity;
  protected int price;
  protected boolean qtyChecked;
  protected boolean qtyEQ, qtyLT, qtyGT;
  protected boolean asChecked;
  protected boolean asEQ, asLT, asGT;
  protected boolean notChecked;

  private static final Pattern QTYSEARCH_PATTERN =
      Pattern.compile("\\s*#\\s*([<=>]+)\\s*([\\d,]+)\\s*");

  private static final Pattern ASSEARCH_PATTERN =
      Pattern.compile("\\s*\\p{Sc}\\s*([<=>]+)\\s*([\\d,]+)\\s*");

  private static final Pattern NOTSEARCH_PATTERN = Pattern.compile("\\s*!\\s*=\\s*(.+)\\s*");

  public AutoFilterTextField(final JList list) {
    this.setList(list);

    this.addKeyListener(new FilterListener());

    // Make this look like a normal search field on OS X.
    // Note that the field MUST NOT be forced to a height other than its
    // preferred height; that produces some ugly visual glitches.

    this.putClientProperty("JTextField.variant", "search");
  }

  public AutoFilterTextField(final JList list, Object initial) {
    this.setList(list);

    this.addKeyListener(new FilterListener());

    // Make this look like a normal search field on OS X.
    // Note that the field MUST NOT be forced to a height other than its
    // preferred height; that produces some ugly visual glitches.

    this.putClientProperty("JTextField.variant", "search");

    if (initial != null) {
      this.setText(initial.toString());
    }
  }

  public AutoFilterTextField(LockableListModel displayModel) {
    this.addKeyListener(new FilterListener());

    this.model = displayModel;
    this.model.setFilter(this);

    // Make this look like a normal search field on OS X.
    // Note that the field MUST NOT be forced to a height other than its
    // preferred height; that produces some ugly visual glitches.

    this.putClientProperty("JTextField.variant", "search");
  }

  public void setList(final JList list) {
    this.list = list;
    this.model = (LockableListModel) list.getModel();
    this.model.setFilter(this);
    this.list.clearSelection();
  }

  public void setModel(final LockableListModel model) {
    this.model = model;
    this.model.setFilter(this);
  }

  public void actionPerformed(final ActionEvent e) {
    this.update();
  }

  @Override
  public void setText(final String text) {
    super.setText(text);
    this.update();
  }

  public boolean isVisible(final Object element) {
    if (this.qtyChecked) {
      int qty = AutoFilterTextField.getResultQuantity(element);
      if ((qty == this.quantity && !this.qtyEQ)
          || (qty < this.quantity && !this.qtyLT)
          || (qty > this.quantity && !this.qtyGT)) {
        return false;
      }
    }

    if (this.asChecked) {
      int as = AutoFilterTextField.getResultPrice(element);
      if ((as == this.price && !this.asEQ)
          || (as < this.price && !this.asLT)
          || (as > this.price && !this.asGT)) {
        return false;
      }
    }

    if (this.text == null || this.text.length() == 0) {
      return true;
    }

    // If it's not a result, then check to see if you need to
    // filter based on its string form.

    String elementName = AutoFilterTextField.getResultName(element);

    if (this.notChecked) {
      return elementName.indexOf(this.text) == -1;
    }

    return this.strict
        ? elementName.indexOf(this.text) != -1
        : StringUtilities.fuzzyMatches(elementName, this.text);
  }

  public static final String getResultName(final Object element) {
    if (element == null) {
      return "";
    }

    if (element instanceof AdventureResult) {
      return ((AdventureResult) element).getName().toLowerCase();
    }
    if (element instanceof CreateItemRequest) {
      return ((CreateItemRequest) element).getName().toLowerCase();
    }
    if (element instanceof Concoction) {
      return ((Concoction) element).getName().toLowerCase();
    }
    if (element instanceof QueuedConcoction) {
      return ((QueuedConcoction) element).getName().toLowerCase();
    }
    if (element instanceof SoldItem) {
      return ((SoldItem) element).getItemName().toLowerCase();
    }
    if (element instanceof StoreLogEntry) {
      return element.toString().toLowerCase();
    }
    if (element instanceof LowerCaseEntry) {
      return ((LowerCaseEntry<?, ?>) element).getLowerCase();
    }
    if (element instanceof KoLAdventure) {
      return ((KoLAdventure) element).toLowerCaseString();
    }
    if (element instanceof Monster) {
      return ((Monster) element).toLowerCaseString();
    }
    if (element instanceof Script) {
      return ((Script) element).getScriptName().toLowerCase();
    }

    return element.toString();
  }

  public static final int getResultPrice(final Object element) {
    if (element == null) {
      return -1;
    }

    if (element instanceof AdventureResult) {
      return ItemDatabase.getPriceById(((AdventureResult) element).getItemId());
    }

    return -1;
  }

  public static final int getResultQuantity(final Object element) {
    if (element == null) {
      return -1;
    }

    if (element instanceof AdventureResult) {
      return ((AdventureResult) element).getCount();
    }
    if (element instanceof CreateItemRequest) {
      return ((CreateItemRequest) element).getQuantityPossible();
    }
    if (element instanceof Concoction) {
      return ((Concoction) element).getAvailable();
    }
    if (element instanceof SoldItem) {
      return ((SoldItem) element).getQuantity();
    }
    if (element instanceof LowerCaseEntry) { // no meaningful integer fields
      return -1;
    }
    if (element instanceof KoLAdventure) {
      return StringUtilities.parseInt(((KoLAdventure) element).getAdventureId());
    }

    return -1;
  }

  public void update() {
    try {
      AutoFilterTextField.this.qtyChecked = false;
      AutoFilterTextField.this.asChecked = false;
      AutoFilterTextField.this.notChecked = false;
      AutoFilterTextField.this.text = AutoFilterTextField.this.getText().toLowerCase();

      if (AutoFilterTextField.this.text != null
          && AutoFilterTextField.this.text.length() > 1
          && AutoFilterTextField.this.text.charAt(0) == "-".charAt(0)) {
        AutoFilterTextField.this.notChecked = true;
        AutoFilterTextField.this.text = AutoFilterTextField.this.text.substring(1).trim();
      }

      Matcher mqty = AutoFilterTextField.QTYSEARCH_PATTERN.matcher(AutoFilterTextField.this.text);
      if (mqty.find()) {
        AutoFilterTextField.this.qtyChecked = true;
        AutoFilterTextField.this.quantity = StringUtilities.parseInt(mqty.group(2));

        String op = mqty.group(1);

        AutoFilterTextField.this.qtyEQ = op.indexOf("=") != -1;
        AutoFilterTextField.this.qtyLT = op.indexOf("<") != -1;
        AutoFilterTextField.this.qtyGT = op.indexOf(">") != -1;
        AutoFilterTextField.this.text = mqty.replaceFirst("");
      }

      Matcher mas = AutoFilterTextField.ASSEARCH_PATTERN.matcher(AutoFilterTextField.this.text);
      if (mas.find()) {
        AutoFilterTextField.this.asChecked = true;
        AutoFilterTextField.this.price = StringUtilities.parseInt(mas.group(2));

        String op = mas.group(1);

        AutoFilterTextField.this.asEQ = op.indexOf("=") != -1;
        AutoFilterTextField.this.asLT = op.indexOf("<") != -1;
        AutoFilterTextField.this.asGT = op.indexOf(">") != -1;
        AutoFilterTextField.this.text = mas.replaceFirst("");
      }

      Matcher mnot = AutoFilterTextField.NOTSEARCH_PATTERN.matcher(AutoFilterTextField.this.text);
      if (mnot.find()) {
        AutoFilterTextField.this.notChecked = true;
        AutoFilterTextField.this.text = mnot.group(1);
      }

      AutoFilterTextField.this.strict = true;
      AutoFilterTextField.this.model.updateFilter(false);

      if (AutoFilterTextField.this.model.getSize() == 0) {
        AutoFilterTextField.this.strict = false;
        AutoFilterTextField.this.model.updateFilter(false);
      }

      if (AutoFilterTextField.this.list != null) {
        JList list = AutoFilterTextField.this.list;
        if (AutoFilterTextField.this.model.getSize() == 1) {
          list.setSelectedIndex(0);
        } else if (list.getSelectedIndices().length == 1) {
          list.ensureIndexIsVisible(list.getSelectedIndex());
        } else {
          list.clearSelection();
        }
      }
    } finally {
      if (AutoFilterTextField.this.model.size() > 0) {
        AutoFilterTextField.this.model.fireContentsChanged(
            AutoFilterTextField.this.model, 0, AutoFilterTextField.this.model.size() - 1);
      }
    }
  }

  private class FilterListener extends KeyAdapter {
    @Override
    public void keyReleased(final KeyEvent e) {
      AutoFilterTextField.this.update();
    }
  }
}
