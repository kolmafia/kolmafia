package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSpleenUse;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;

import internal.helpers.Cleanups;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.MaximizerFrame.FilterBoosts;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.mockito.Mockito;

@DisabledIf(
    value = "java.awt.GraphicsEnvironment#isHeadless",
    disabledReason = "headless environment")
public class MaximizerFrameTest {
  @Test
  void displaysPersistentAnytimeAndCertificateSettings() {
    KoLCharacter.reset("MaximizerFrameSearchSettings");
    Preferences.reset("MaximizerFrameSearchSettings");
    try (var cleanups =
        new Cleanups(
            withProperty("maximizerTimeLimitSeconds", 27),
            withProperty("maximizerCombinationLimit", 1234),
            withProperty("maximizerBudgetUsesOr", true))) {
      MaximizerFrame frame = new MaximizerFrame();

      try {
        var components = descendants(frame).toList();
        var certificateMode = button(components, "Prove the requested gap or exact optimum");
        var limitPanel = label(components, "Limit:").getParent();
        var targetPanel = label(components, "Proof target:").getParent();
        assertThat(certificateMode.isSelected(), equalTo(false));
        assertThat(buttonInRow(components, "Limit:", "or").isSelected(), equalTo(true));
        assertThat(limitPanel.isVisible(), equalTo(true));
        assertThat(targetPanel.isVisible(), equalTo(false));
        certificateMode.doClick();
        assertThat(limitPanel.isVisible(), equalTo(false));
        assertThat(targetPanel.isVisible(), equalTo(true));
        var textValues =
            components.stream()
                .filter(JTextField.class::isInstance)
                .map(JTextField.class::cast)
                .map(JTextField::getText)
                .toList();
        assertThat(textValues, hasItem("27"));
        assertThat(textValues, hasItem("1234"));
      } finally {
        frame.dispose();
      }
    } finally {
      KoLCharacter.reset("");
    }
  }

  @Test
  void maximizerSearchPreferencesHaveProductDefaults() {
    assertThat(Preferences.getDefault("maximizerCertificateMode"), equalTo("false"));
    assertThat(Preferences.getDefault("maximizerTimeLimitSeconds"), equalTo("10"));
    assertThat(Preferences.getDefault("maximizerCombinationLimit"), equalTo("0"));
    assertThat(Preferences.getDefault("maximizerBudgetUsesOr"), equalTo("false"));
    assertThat(Preferences.getDefault("maximizerMaximumScoreDifferencePercent"), equalTo("0.0"));
    assertThat(Preferences.getDefault("maximizerMaximumScoreDifferencePoints"), equalTo("0.0"));
    assertThat(Preferences.getDefault("maximizerScoreDifferenceOr"), equalTo("false"));
  }

  @Test
  void maximizeIgnoresInvalidProofFieldsWhenCertificateModeIsInactive() {
    KoLCharacter.reset("MaximizerFrameInactiveProof");
    Preferences.reset("MaximizerFrameInactiveProof");
    try (var cleanups =
        new Cleanups(
            withProperty("maximizerCertificateMode", "false"),
            withProperty("maximizerTimeLimitSeconds", "27"),
            withProperty("maximizerCombinationLimit", "1234"),
            withProperty("maximizerMaximumScoreDifferencePercent", "not-a-number"),
            withProperty("maximizerMaximumScoreDifferencePoints", "-5"))) {
      MaximizerFrame frame = new MaximizerFrame();

      try {
        var components = descendants(frame).toList();
        textField(components, "27").setText("31");
        textField(components, "1234").setText("5678");
        clearFiltersAndMaximize(frame, components);

        assertThat(Preferences.getLong("maximizerTimeLimitSeconds"), equalTo(31L));
        assertThat(Preferences.getLong("maximizerCombinationLimit"), equalTo(5678L));
        assertThat(
            Preferences.getString("maximizerMaximumScoreDifferencePercent"),
            equalTo("not-a-number"));
        assertThat(Preferences.getString("maximizerMaximumScoreDifferencePoints"), equalTo("-5"));
      } finally {
        frame.dispose();
      }
    } finally {
      KoLCharacter.reset("");
      Preferences.reset("");
    }
  }

  @Test
  void maximizePersistsEditedValidProofTargets() {
    KoLCharacter.reset("MaximizerFrameEditedProof");
    Preferences.reset("MaximizerFrameEditedProof");
    try (var cleanups =
        new Cleanups(
            withProperty("maximizerCertificateMode", "false"),
            withProperty("maximizerTimeLimitSeconds", "27"),
            withProperty("maximizerCombinationLimit", "1234"),
            withProperty("maximizerBudgetUsesOr", "false"),
            withProperty("maximizerMaximumScoreDifferencePercent", "12.5"),
            withProperty("maximizerMaximumScoreDifferencePoints", "34.5"),
            withProperty("maximizerScoreDifferenceOr", "false"))) {
      MaximizerFrame frame = new MaximizerFrame();

      try {
        var components = descendants(frame).toList();
        button(components, "Prove the requested gap or exact optimum").doClick();
        textField(components, "27").setText("31");
        textField(components, "1234").setText("5678");
        buttonInRow(components, "Limit:", "or").doClick();
        textField(components, "12.5").setText("7.5");
        textField(components, "34.5").setText("9.25");
        buttonInRow(components, "Proof target:", "or").doClick();
        clearFiltersAndMaximize(frame, components);

        assertThat(Preferences.getBoolean("maximizerCertificateMode"), equalTo(true));
        assertThat(Preferences.getLong("maximizerTimeLimitSeconds"), equalTo(31L));
        assertThat(Preferences.getLong("maximizerCombinationLimit"), equalTo(5678L));
        assertThat(Preferences.getBoolean("maximizerBudgetUsesOr"), equalTo(true));
        assertThat(Preferences.getDouble("maximizerMaximumScoreDifferencePercent"), equalTo(7.5));
        assertThat(Preferences.getDouble("maximizerMaximumScoreDifferencePoints"), equalTo(9.25));
        assertThat(Preferences.getBoolean("maximizerScoreDifferenceOr"), equalTo(true));
      } finally {
        frame.dispose();
      }
    } finally {
      KoLCharacter.reset("");
      Preferences.reset("");
    }
  }

  @Test
  void invalidActiveProofFieldsDoNotChangeAnySearchSettingOrStartSearch() {
    KoLCharacter.reset("MaximizerFrameAtomicProof");
    Preferences.reset("MaximizerFrameAtomicProof");
    try (var cleanups =
            new Cleanups(
                withProperty("maximizerCertificateMode", "false"),
                withProperty("maximizerTimeLimitSeconds", "27"),
                withProperty("maximizerCombinationLimit", "1234"),
                withProperty("maximizerBudgetUsesOr", "false"),
                withProperty("maximizerMaximumScoreDifferencePercent", "12.5"),
                withProperty("maximizerMaximumScoreDifferencePoints", "34.5"),
                withProperty("maximizerScoreDifferenceOr", "false"));
        var inputFields = Mockito.mockStatic(InputFieldUtilities.class)) {
      MaximizerFrame frame = new MaximizerFrame();

      try {
        var components = descendants(frame).toList();
        var previousEvaluator = Maximizer.eval;
        button(components, "Prove the requested gap or exact optimum").doClick();
        textField(components, "27").setText("31");
        textField(components, "1234").setText("5678");
        buttonInRow(components, "Limit:", "or").doClick();
        textField(components, "12.5").setText("not-a-number");
        textField(components, "34.5").setText("9.25");
        buttonInRow(components, "Proof target:", "or").doClick();

        frame.maximize();

        inputFields.verify(
            () -> InputFieldUtilities.alert("Proof gaps must be finite, nonnegative numbers."));
        assertThat(Maximizer.eval, sameInstance(previousEvaluator));
        assertThat(Preferences.getBoolean("maximizerCertificateMode"), equalTo(false));
        assertThat(Preferences.getLong("maximizerTimeLimitSeconds"), equalTo(27L));
        assertThat(Preferences.getLong("maximizerCombinationLimit"), equalTo(1234L));
        assertThat(Preferences.getBoolean("maximizerBudgetUsesOr"), equalTo(false));
        assertThat(Preferences.getDouble("maximizerMaximumScoreDifferencePercent"), equalTo(12.5));
        assertThat(Preferences.getDouble("maximizerMaximumScoreDifferencePoints"), equalTo(34.5));
        assertThat(Preferences.getBoolean("maximizerScoreDifferenceOr"), equalTo(false));
      } finally {
        frame.dispose();
      }
    } finally {
      KoLCharacter.reset("");
      Preferences.reset("");
    }
  }

  private static JTextField textField(List<Component> components, String text) {
    return components.stream()
        .filter(JTextField.class::isInstance)
        .map(JTextField.class::cast)
        .filter(field -> field.getText().equals(text))
        .findFirst()
        .orElseThrow();
  }

  private static AbstractButton button(List<Component> components, String text) {
    return components.stream()
        .filter(AbstractButton.class::isInstance)
        .map(AbstractButton.class::cast)
        .filter(button -> button.getText().startsWith(text))
        .findFirst()
        .orElseThrow();
  }

  private static JLabel label(List<Component> components, String text) {
    return components.stream()
        .filter(JLabel.class::isInstance)
        .map(JLabel.class::cast)
        .filter(label -> label.getText().startsWith(text))
        .findFirst()
        .orElseThrow();
  }

  private static AbstractButton buttonInRow(
      List<Component> components, String rowLabel, String text) {
    return button(descendants(label(components, rowLabel).getParent()).toList(), text);
  }

  private static void clearFiltersAndMaximize(MaximizerFrame frame, List<Component> components) {
    components.stream()
        .filter(JButton.class::isInstance)
        .map(JButton.class::cast)
        .filter(button -> button.getText().equals("Clear Filters"))
        .findFirst()
        .orElseThrow()
        .doClick();
    MaximizerFrame.expressionSelect.setSelectedItem("item drop, -tie");
    frame.maximize();
  }

  private static Stream<Component> descendants(Container container) {
    return Arrays.stream(container.getComponents())
        .flatMap(
            component ->
                component instanceof Container child
                    ? Stream.concat(Stream.of(component), descendants(child))
                    : Stream.of(component));
  }

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

  @Nested
  class PersistentFiltersTest {
    private static final String TESTUSER = "PersistentFiltersTestUser";

    @BeforeEach
    void beforeEach() {
      KoLCharacter.reset(TESTUSER);
    }

    @AfterAll
    static void afterAll() {
      KoLCharacter.reset(TESTUSER);
    }

    @Test
    void enableAllFiltersByDefault() {
      MaximizerFrame maximizerFrame = new MaximizerFrame();
      for (KoLConstants.filterType filter : KoLConstants.filterType.values()) {
        assertThat(maximizerFrame.getActiveFilters(), hasItem(filter));
      }
    }

    @Test
    void enableOnlyPersistedFilters() {
      var cleanups = new Cleanups(withProperty("maximizerLastFilters", "equip,cast"));

      try (cleanups) {
        MaximizerFrame maximizerFrame = new MaximizerFrame();
        KoLConstants.filterType[] persisted = {
          KoLConstants.filterType.EQUIP, KoLConstants.filterType.CAST
        };
        for (KoLConstants.filterType filter : KoLConstants.filterType.values()) {
          if (Arrays.asList(persisted).contains(filter)) {
            assertThat(maximizerFrame.getActiveFilters(), hasItem(filter));
          } else {
            assertThat(maximizerFrame.getActiveFilters(), not(hasItem(filter)));
          }
        }
      }
    }

    @Test
    void disableFullOrgansEvenIfPersisted() {
      var cleanups =
          new Cleanups(
              withProperty(
                  "maximizerLastFilters", "equip,cast,wish,other,usable,booze,food,spleen"),
              withInebriety(KoLCharacter.getLiverCapacity()),
              withFullness(KoLCharacter.getStomachCapacity()),
              withSpleenUse(KoLCharacter.getSpleenLimit()));

      try (cleanups) {
        MaximizerFrame maximizerFrame = new MaximizerFrame();
        KoLConstants.filterType[] fullOrgans = {
          KoLConstants.filterType.BOOZE,
          KoLConstants.filterType.FOOD,
          KoLConstants.filterType.SPLEEN
        };
        for (KoLConstants.filterType filter : KoLConstants.filterType.values()) {
          if (Arrays.asList(fullOrgans).contains(filter)) {
            assertThat(maximizerFrame.getActiveFilters(), not(hasItem(filter)));
          } else {
            assertThat(maximizerFrame.getActiveFilters(), hasItem(filter));
          }
        }
      }
    }

    @Nested
    class UpdateFilters {
      @Test
      void disableFilters() {
        var cleanups =
            new Cleanups(withProperty("maximizerLastFilters", "equip,cast,wish,other,usable"));

        try (cleanups) {
          MaximizerFrame maximizerFrame = new MaximizerFrame();
          KoLConstants.filterType[] disabled = {
            KoLConstants.filterType.WISH, KoLConstants.filterType.USABLE,
          };
          for (KoLConstants.filterType disabledFilter : disabled) {
            maximizerFrame.updateFilter(disabledFilter, false);
            assertThat(maximizerFrame.getActiveFilters(), not(hasItem(disabledFilter)));
          }

          String[] lastFilters = Preferences.getString("maximizerLastFilters").split(",");
          for (String lastFilter : new String[] {"equip", "cast", "other"}) {
            assertThat(Arrays.asList(lastFilters), hasItem(lastFilter));
          }
        }
      }

      @Test
      void enableFilters() {
        var cleanups = new Cleanups(withProperty("maximizerLastFilters", "equip"));

        try (cleanups) {
          MaximizerFrame maximizerFrame = new MaximizerFrame();
          KoLConstants.filterType[] enabled = {
            KoLConstants.filterType.BOOZE,
            KoLConstants.filterType.FOOD,
            KoLConstants.filterType.SPLEEN
          };
          for (KoLConstants.filterType enabledFilter : enabled) {
            maximizerFrame.updateFilter(enabledFilter, true);
            assertThat(maximizerFrame.getActiveFilters(), hasItem(enabledFilter));
          }

          String[] lastFilters = Preferences.getString("maximizerLastFilters").split(",");
          for (String lastFilter : new String[] {"equip", "booze", "food", "spleen"}) {
            assertThat(Arrays.asList(lastFilters), hasItem(lastFilter));
          }
        }
      }
    }
  }
}
