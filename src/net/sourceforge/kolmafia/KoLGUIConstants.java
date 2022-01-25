package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JTextField;
import net.java.dev.spellcast.utilities.LockableListModel;

public class KoLGUIConstants {
  // Constants which are used in order to do things inside of
  // the GUI.  Ensures that all GUI information can be accessed
  // at any time.

  public static final Font DEFAULT_FONT = (new JTextField()).getFont();
  public static final JLabel BLANK_LABEL = new JLabel();
  public static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
  public static final LockableListModel<String> existingFrames = new LockableListModel<String>();

  // Colors which are used to handle the various KoLmafia states.
  // Used when changing the display.

  public static final Color ERROR_COLOR = new Color(255, 192, 192);
  public static final Color ENABLED_COLOR = new Color(192, 255, 192);
  public static final Color DISABLED_COLOR = null;

  public static final Color ERROR_COLOR_DARK = new Color(100, 23, 25);
  public static final Color ENABLED_COLOR_DARK = new Color(25, 103, 23);
  public static final Color DISABLED_COLOR_DARK = new Color(89, 89, 89);

  // New Look and Feel list goes here.
  public static final Map<String, String> FLATMAP_LIGHT_LOOKS =
      new TreeMap<String, String>() {
        {
          put("Arc - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme");
          put("Arc", "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme");
          put(
              "Atom One Light Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightContrastIJTheme");
          put(
              "Atom One Light",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme");
          put("Cyan light", "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme");
          put("FlatMap Light", "com.formdev.flatlaf.FlatLightLaf");
          put(
              "GitHub Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubContrastIJTheme");
          put("GitHub", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme");
          put("Gray", "com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme");
          put("Light Flat", "com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme");
          put(
              "Light Owl Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlContrastIJTheme");
          put(
              "Light Owl",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme");
          put(
              "Material Lighter Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterContrastIJTheme");
          put(
              "Material Lighter",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme");
          put(
              "Solarized Light Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedLightContrastIJTheme");
          put("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme");
          put(
              "Material Solarized Light",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedLightIJTheme");
        }
      };

  public static final Map<String, String> FLATMAP_DARK_LOOKS =
      new TreeMap<String, String>() {
        {
          put("Arc Dark - Orange", "com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme");
          put(
              "Arc Dark Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkContrastIJTheme");
          put("Arc Dark", "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme");
          put(
              "Material Arc Dark",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkIJTheme");
          put(
              "Atom One Dark Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkContrastIJTheme");
          put(
              "Atom One Dark",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme");
          put("Carbon", "com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme");
          put("Cobalt 2", "com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme");
          put("Flatmap Darcula", "com.formdev.flatlaf.FlatDarculaLaf");
          put("Dark Flat", "com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme");
          put("Dark Purple", "com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme");
          put(
              "Dracula Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatDraculaContrastIJTheme");
          put("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme");
          put(
              "Dracula Theme",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatDraculaIJTheme");
          put("FlatMap Dark", "com.formdev.flatlaf.FlatDarkLaf");
          put(
              "Gradianto Dark Fuchsia",
              "com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme");
          put(
              "Gradianto Deep Ocean",
              "com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme");
          put(
              "Gradianto Midnight Blue",
              "com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme");
          put("Gruvbox Dark Hard", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme");
          put(
              "Gruvbox Dark Medium",
              "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme");
          put("Gruvbox Dark Soft", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme");
          put("Hiberbee Dark", "com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme");
          put("High contrast", "com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme");
          put(
              "Material Darker Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerContrastIJTheme");
          put(
              "Material Darker",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme");
          put(
              "Material Deep Ocean Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanContrastIJTheme");
          put(
              "Material Deep Ocean",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme");
          put(
              "Material Design Dark",
              "com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme");
          put(
              "Material Oceanic Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicContrastIJTheme");
          put(
              "Material Oceanic",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme");
          put(
              "Material Palenight Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightContrastIJTheme");
          put(
              "Material Palenight",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme");
          put("Monocai", "com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme");
          put(
              "Monokai Pro Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProContrastIJTheme");
          put(
              "Monokai Pro",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProIJTheme");
          put(
              "Night Owl Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlContrastIJTheme");
          put(
              "Night Owl",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlIJTheme");
          put("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme");
          put("One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme");
          put(
              "Solarized Dark Contrast",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedDarkContrastIJTheme");
          put("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme");
          put(
              "Material Solarized Dark",
              "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedDarkIJTheme");
          put("Spacegray", "com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme");
          put("Vuesion", "com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme");
        }
      };

  private KoLGUIConstants() {}
}
