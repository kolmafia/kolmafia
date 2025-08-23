package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

import java.util.LinkedList;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.swingui.MaximizerFrame.FilterBoosts;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerFrameTest {
  @Nested
  class FilterBoostsTest {
    @Test
    void singleWordFiltersStrictly() {
      var filter = createFilterBoosts();

      filter.setText("car");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(3));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("cargo effect Finding Stuff"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("cargo effect Super Vision"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("daycare mysticality"))));
    }

    @Test
    void singleWordFiltersLoosely() {
      var filter = createFilterBoosts();

      filter.setText("kacc");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(2));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep acc1: lucky gold ring"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep acc2: thumb ring"))));
    }

    @Test
    void singleNegativeWordFiltersStrictly() {
      var filter = createFilterBoosts();

      filter.setText("-keep");
      var model = filter.getModel();
      var visible = visibleItems(model);
      // backwards compat
      assertThat(visible, hasSize(4));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("ledcandle disco"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("cargo effect Finding Stuff"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("cargo effect Super Vision"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("daycare mysticality"))));
    }

    @Test
    void singleNegativeWordFiltersLoosely() {
      var filter = createFilterBoosts();

      filter.setText("-kacc");
      var model = filter.getModel();
      var visible = visibleItems(model);
      // everything, backwards compat
      assertThat(visible, hasSize(8));
    }

    @Test
    void multipleWordsFilter() {
      var filter = createFilterBoosts();

      filter.setText("keep ring");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(2));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep acc1: lucky gold ring"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep acc2: thumb ring"))));
    }

    @Test
    void multipleWordsFilterLoosely() {
      var filter = createFilterBoosts();

      filter.setText("keep tr");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(1));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep acc2: thumb ring"))));
    }

    @Test
    void multipleWordsCustomStrictFilter() {
      var filter = createFilterBoosts();

      filter.setText("keep 'tr");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(0));
    }

    @Test
    void multipleWordsNegatives() {
      var filter = createFilterBoosts();

      filter.setText("!keep !cargo");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(2));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("ledcandle disco"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("daycare mysticality"))));
    }

    @Test
    void multipleWordsStrictAndNonStrict() {
      var filter = createFilterBoosts();

      filter.setText("'keep cl");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(2));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep weapon: June cleaver"))));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep back: vampyric cloake"))));
    }

    @Test
    void singleWordExplicitStrictNoMatches() {
      var filter = createFilterBoosts();

      filter.setText("'ce");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(0));
    }

    @Test
    void multipleWordsStrictMiddle() {
      var filter = createFilterBoosts();

      filter.setText("'keep 'ea");
      var model = filter.getModel();
      var visible = visibleItems(model);
      assertThat(visible, hasSize(1));
      assertThat(visible, hasItem(hasProperty("cmd", equalTo("keep weapon: June cleaver"))));
    }

    @Test
    void strictNot() {
      var filter = createFilterBoosts();

      filter.setText("!'yst 'i");
      var model = filter.getModel();
      var visible = visibleItems(model);
      // everything except cleaver and daycare
      assertThat(visible, hasSize(6));
    }

    private FilterBoosts createFilterBoosts() {
      var check =
          List.of(
              "keep weapon: June cleaver",
              "keep back: vampyric cloake",
              "keep acc1: lucky gold ring",
              "keep acc2: thumb ring",
              "ledcandle disco",
              "cargo effect Finding Stuff",
              "cargo effect Super Vision",
              "daycare mysticality");
      var boosts = check.stream().map(x -> new Boost(x, x, x, 0.0)).toList();
      var llm = new LockableListModel<>(boosts);
      var jlist = new ShowDescriptionList<>(llm);
      return new FilterBoosts(jlist);
    }

    private List<Boost> visibleItems(LockableListModel<Boost> boosts) {
      var lst = new LinkedList<Boost>();

      for (int i = 0; i < boosts.getSize(); i++) {
        lst.add(boosts.getElementAt(i));
      }

      return lst;
    }
  }
}
