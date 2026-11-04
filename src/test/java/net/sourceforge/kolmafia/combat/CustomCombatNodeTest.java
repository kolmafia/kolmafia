package net.sourceforge.kolmafia.combat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class CustomCombatNodeTest {
  private static class Node extends CustomCombatNode {
    Node(final Object userObject, final boolean allowsChildren) {
      super(userObject, allowsChildren);
    }
  }

  @Test
  void showsItsUserObject() {
    assertThat(new Node("default", true).toString(), is("default"));
    assertThat(new Node(null, true).toString(), is(""));
  }

  @Test
  void keepsChildrenInOrder() {
    var parent = new Node("parent", true);
    var first = new Node("first", false);
    var last = new Node("last", false);

    parent.add(first);
    parent.add(last);

    assertThat(parent.getChildCount(), is(2));
    assertThat(parent.getChildAt(0), sameInstance(first));
    assertThat(parent.getLastChild(), sameInstance(last));

    parent.removeAllChildren();
    assertThat(parent.getChildCount(), is(0));
  }

  @Test
  void hasNoLastChildWhenEmpty() {
    assertThrows(NoSuchElementException.class, () -> new Node("empty", true).getLastChild());
  }

  @Test
  void leavesCannotHaveChildren() {
    var leaf = new Node("leaf", false);

    assertThrows(IllegalStateException.class, () -> leaf.add(new Node("child", false)));
  }

  @Test
  void strategiesShowTheirEncounter() {
    var strategy = new CustomCombatStrategy("knob goblin");

    assertThat(strategy.toString(), is("knob goblin"));
    assertThat(strategy.getAllowsChildren(), is(true));
  }
}
