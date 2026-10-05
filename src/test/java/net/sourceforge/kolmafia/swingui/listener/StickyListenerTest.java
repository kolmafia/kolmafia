package net.sourceforge.kolmafia.swingui.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StickyListenerTest {
  private final JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL, 0, 400, 0, 400);
  private StickyListener listener;

  private void onEdt(Runnable action) throws Exception {
    SwingUtilities.invokeAndWait(action);
    SwingUtilities.invokeAndWait(() -> {});
  }

  @BeforeEach
  void addListener() throws Exception {
    onEdt(() -> listener = new StickyListener(bar));
    onEdt(() -> bar.setMaximum(1000));
  }

  @Test
  void followsContentGrowingBeneathTheView() throws Exception {
    onEdt(() -> bar.setMaximum(5000));

    assertThat(bar.getValue(), is(4600));
    assertThat(listener.isSticky(), is(true));
  }

  @Test
  void scrollingUpUnsticks() throws Exception {
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(100)));

    assertThat(listener.isSticky(), is(false));
  }

  @Test
  void doesNotFollowOnceUnstuck() throws Exception {
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(100)));
    onEdt(() -> bar.setMaximum(5000));

    assertThat(bar.getValue(), is(100));
  }

  @Test
  void scrollingBackDownSticks() throws Exception {
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(100)));
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(600)));
    onEdt(() -> bar.setMaximum(5000));

    assertThat(listener.isSticky(), is(true));
    assertThat(bar.getValue(), is(4600));
  }

  @Test
  void sticksWhenContentShrinksToFit() throws Exception {
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(100)));
    onEdt(() -> bar.setMaximum(400));

    assertThat(listener.isSticky(), is(true));
  }

  @Test
  void staysUnstuckWhenContentShrinksToTheView() throws Exception {
    onEdt(() -> listener.scrollAsUser(() -> bar.setValue(100)));
    onEdt(() -> bar.setMaximum(500));

    assertThat(listener.isSticky(), is(false));
  }

  @Test
  void scrollingUpWhileContentGrowsUnsticks() throws Exception {
    onEdt(
        () -> {
          bar.setMaximum(2000);
          listener.scrollAsUser(() -> bar.setValue(100));
          bar.setMaximum(3000);
        });

    assertThat(listener.isSticky(), is(false));
    assertThat(bar.getValue(), is(100));
  }

  @Test
  void programmaticScrollsDoNotUnstick() throws Exception {
    onEdt(() -> bar.setValue(100));

    assertThat(listener.isSticky(), is(true));
    assertThat(bar.getValue(), is(600));
  }

  @Test
  void followsTheViewShrinking() throws Exception {
    onEdt(() -> bar.setVisibleAmount(200));

    assertThat(listener.isSticky(), is(true));
    assertThat(bar.getValue(), is(800));
  }
}
