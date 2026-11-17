package net.sourceforge.kolmafia.swingui.widget;

import java.io.File;
import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.FileUtilities;

/** Loads game images (items, familiars, ...) as Swing icons. */
public class ImageIconFactory {
  private ImageIconFactory() {}

  /** Downloads an image from the KoL image server and returns it as an icon */
  public static final ImageIcon downloadIcon(
      final String image, final String container, final String defaultImage) {
    if (image == null || image.equals("")) {
      return JComponentUtilities.getImage(defaultImage);
    }

    String path = container == null || container.equals("") ? image : container + "/" + image;

    File file = FileUtilities.downloadImage(KoLmafia.imageServerPath() + path);
    if (file == null) {
      return JComponentUtilities.getImage(defaultImage);
    }
    ImageIcon icon = JComponentUtilities.getImage(path);
    return icon != null ? icon : JComponentUtilities.getImage(defaultImage);
  }

  private static ImageIcon getFamiliarIcon(final String location) {
    if (location == null || location.equals("debug.gif")) {
      return ImageIconFactory.getNoFamiliarImage();
    }
    String path = (location.contains("/") ? "" : "itemimages/") + location;
    String url = KoLmafia.imageServerPath() + path;
    File file = FileUtilities.downloadImage(url);
    if (file == null) {
      return ImageIconFactory.getNoFamiliarImage();
    }
    ImageIcon icon = JComponentUtilities.getImage(path);
    return icon != null ? icon : ImageIconFactory.getNoFamiliarImage();
  }

  public static final ImageIcon getNoFamiliarImage() {
    return JComponentUtilities.getImage("debug.gif");
  }

  public static final ImageIcon getFamiliarImage(final String name) {
    return ImageIconFactory.getFamiliarImage(FamiliarDatabase.getFamiliarId(name));
  }

  public static final ImageIcon getFamiliarImage(final int familiarId) {
    String location = FamiliarDatabase.getFamiliarImageLocation(familiarId);
    return ImageIconFactory.getFamiliarIcon(location);
  }

  public static final ImageIcon getItemImage(final int itemId) {
    String path = ItemDatabase.getItemImagePath(itemId);
    ItemDatabase.downloadItemImage(path);
    return JComponentUtilities.getImage(path);
  }
}
