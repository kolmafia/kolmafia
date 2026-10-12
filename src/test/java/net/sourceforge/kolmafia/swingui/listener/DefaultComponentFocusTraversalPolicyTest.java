package net.sourceforge.kolmafia.swingui.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

public class DefaultComponentFocusTraversalPolicyTest {
  @Test
  void defaultComponentSortsFirstButAfterItsAncestors() {
    var root = new JPanel();
    var before = new JTextField();
    var row = new JPanel();
    var defaultField = new JTextField();
    var after = new JTextField();
    root.add(before);
    root.add(row);
    row.add(defaultField);
    root.add(after);

    var layoutOrder = List.<Component>of(root, before, row, defaultField, after);
    var policy =
        new DefaultComponentFocusTraversalPolicy(defaultField) {
          Comparator<? super Component> comparator() {
            return getComparator();
          }
        };
    policy.setComparator(Comparator.comparingInt(layoutOrder::indexOf));

    var cycle = new ArrayList<>(layoutOrder);
    cycle.sort(policy.comparator());

    assertThat(cycle, contains(root, row, defaultField, before, after));
  }
}
