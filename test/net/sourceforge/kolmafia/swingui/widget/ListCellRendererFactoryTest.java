package net.sourceforge.kolmafia.swingui.widget;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLevel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import javax.swing.*;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

class ListCellRendererFactoryTest {
  @Test
  void canRenderFortuneCookie() {
    var cleanups = new Cleanups(withItem(ItemPool.FORTUNE_COOKIE));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.FORTUNE_COOKIE);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=#999999>fortune cookie (1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;1 full, +1.00 adv, semi-rare numbers</nobr></html>"));
    }
  }

  @Test
  void canRenderPlainPizza() {
    var cleanups = new Cleanups(withItem(ItemPool.PLAIN_PIZZA));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.PLAIN_PIZZA);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b>plain pizza (1 possible, 1 current)</b></nobr><br><nobr>&nbsp;2 full, +3.00 adv, PIZZA</nobr></html>"));
    }
  }

  @Test
  void canRenderBlackberry() {
    var cleanups = new Cleanups(withItem(ItemPool.BLACKBERRY), withLevel(2));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.BLACKBERRY);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=green>blackberry (1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;1 full, +2.50 adv, +3.0 mus, +3.0 mys, +3.0 mox</nobr></html>"));
    }
  }

  @Test
  void graysOutWhenRequirementsNotMet() {
    var cleanups = new Cleanups(withItem(ItemPool.BLACKBERRY), withLevel(1));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.BLACKBERRY);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=gray>blackberry (1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;1 full, +2.50 adv, +3.0 mus, +3.0 mys, +3.0 mox</nobr></html>"));
    }
  }

  @Test
  void canRenderDrippyPlum() {
    var cleanups = new Cleanups(withItem(ItemPool.DRIPPY_PLUM));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.DRIPPY_PLUM);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=#964B00>drippy plum(?) (1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;5 full, +5.00 adv, 5 µg of Drippy Juice</nobr></html>"));
    }
  }

  @Test
  void canRenderAstralHotDog() {
    var cleanups = new Cleanups(withItem(ItemPool.ASTRAL_HOT_DOG), withLevel(8));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.ASTRAL_HOT_DOG);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b>astral hot dog (<font color=green>?</font><font color=red>?</font><font color=gray>?</font>, 1 possible, 1 current)</b></nobr><br><nobr>&nbsp;3 full, +16.00 adv, +144.0 mus, +144.0 mys, +144.0 mox</nobr></html>"));
    }
  }

  @Test
  void canRenderFishTaco() {
    var cleanups = new Cleanups(withItem(ItemPool.TACO_DAN_FISH_TACO), withLevel(8));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.TACO_DAN_FISH_TACO);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=blue>Taco Dan's Taco Fish Fish Taco (2 Beach Bucks, 1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;2 full, +8.00 adv, +50.0 mus, +50.0 mys, +50.0 mox</nobr></html>"));
    }
  }

  @Test
  void canRenderMrBurnsger() {
    var cleanups = new Cleanups(withItem(ItemPool.MR_BURNSGER), withLevel(8));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.MR_BURNSGER);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, false);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b><font color=#8a2be2>Mr. Burnsger (1 possible, 1 current)</font></b></nobr><br><nobr>&nbsp;4 full, +28.00 adv, +30.0 mus, +30.0 mys, +30.0 mox, -2 Drunkenness</nobr></html>"));
    }
  }

  @Test
  void noColourRenderedWhenSelected() {
    var cleanups = new Cleanups(withItem(ItemPool.FORTUNE_COOKIE));

    try (cleanups) {
      var food = ConcoctionPool.get(ItemPool.FORTUNE_COOKIE);
      food.calculate2();
      var renderer = new ListCellRendererFactory.DefaultRenderer();
      var component = (JLabel) renderer.getRenderer(new JLabel(), food, 200, true);

      assertThat(
          component.getText(),
          is(
              "<html><nobr><b>fortune cookie (1 possible, 1 current)</b></nobr><br><nobr>&nbsp;1 full, +1.00 adv, semi-rare numbers</nobr></html>"));
    }
  }
}
