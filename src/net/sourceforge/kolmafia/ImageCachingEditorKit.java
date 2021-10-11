package net.sourceforge.kolmafia;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public class ImageCachingEditorKit extends HTMLEditorKit {
  private static final ImageCachingViewFactory DEFAULT_FACTORY = new ImageCachingViewFactory();

  @Override
  public ViewFactory getViewFactory() {
    return ImageCachingEditorKit.DEFAULT_FACTORY;
  }

  private static class ImageCachingViewFactory extends HTMLFactory {
    @Override
    public View create(final Element elem) {
      if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG) {
        return new CachedImageView(elem);
      }

      return super.create(elem);
    }
  }

  private static class CachedImageView extends ImageView {
    public CachedImageView(final Element elem) {
      super(elem);
    }

    @Override
    public URL getImageURL() {
      String src = (String) this.getElement().getAttributes().getAttribute(HTML.Attribute.SRC);

      if (src == null) {
        return null;
      }

      File imageFile = FileUtilities.downloadImage(src);

      try {
        return imageFile.toURI().toURL();
      } catch (IOException e) {
        return null;
      }
    }
  }
}
