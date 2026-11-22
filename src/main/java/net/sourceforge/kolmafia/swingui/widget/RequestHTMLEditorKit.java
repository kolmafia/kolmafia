package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;

/**
 * The HTML editor kit for KoLmafia's Swing HTML views: form submissions become KoLmafia requests
 * and images come from KoLmafia's image cache.
 */
public class RequestHTMLEditorKit extends HTMLEditorKit {
  private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

  /**
   * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts some of the form
   * handling to ensure that <code>GenericRequest</code> objects are instantiated on form submission
   * rather than the <code>HttpRequest</code> objects created by the default HTML editor kit.
   */
  @Override
  public ViewFactory getViewFactory() {
    return RequestHTMLEditorKit.DEFAULT_FACTORY;
  }

  /**
   * Registers thethat is supposed to be used for handling data submission to the Kingdom of
   * Loathing server.
   */
  private static class RequestViewFactory extends HTMLFactory {
    @Override
    public View create(final Element elem) {
      if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.INPUT) {
        return new KoLSubmitView(elem);
      }

      if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMG) {
        return new KoLImageView(elem);
      }

      return super.create(elem);
    }
  }

  private static class KoLImageView extends ImageView {
    public KoLImageView(final Element elem) {
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

  private static class KoLSubmitView extends FormView {
    public KoLSubmitView(final Element elem) {
      super(elem);
    }

    @Override
    public Component createComponent() {
      Component c = super.createComponent();

      if ((c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox)) {
        c.setBackground(Color.white);
      }

      return c;
    }

    @Override
    public void submitData(final String data) {
      // Get the element

      Element inputElement = this.getElement();

      if (inputElement == null) {
        return;
      }

      // Retrieve the frame which is being used by this form
      // viewer.

      RequestFrame frame = this.findFrame();

      // If there is no frame, then there's nothing to
      // refresh, so return.

      if (frame == null) {
        return;
      }

      // Retrieve the form element so that you know where you
      // need to submit the data.

      Element formElement = inputElement;

      while (formElement != null
          && formElement.getAttributes().getAttribute(StyleConstants.NameAttribute)
              != HTML.Tag.FORM) {
        formElement = formElement.getParentElement();
      }

      // If the form element is null, then there was no
      // enclosing form for the <INPUT> tag, so you can
      // return, doing nothing.

      if (formElement == null) {
        return;
      }

      // Now that you know you have a form element,
      // get the action field, attach the data, and
      // refresh the appropriate request frame.

      String action = (String) formElement.getAttributes().getAttribute(HTML.Attribute.ACTION);

      // If there is no action, how do we know which page to
      // connect to?  We assume it's the originating page.

      if (action == null) {
        action = frame.getCurrentLocation();
      }

      // Now get the data fields we will submit to this form

      String[] elements = data.split("&");
      String[] fields = new String[elements.length];

      if (elements[0].length() > 0) {
        for (int i = 0; i < elements.length; ++i) {
          fields[i] = elements[i].substring(0, elements[i].indexOf("="));
        }
      } else {
        fields[0] = "";
      }

      // Prepare the element string -- make sure that
      // you don't have duplicate fields.

      for (int i = 0; i < elements.length; ++i) {
        for (int j = i + 1; j < elements.length; ++j) {
          if (elements[i] != null && elements[j] != null && fields[i].equals(fields[j])) {
            elements[j] = null;
          }
        }
      }

      GenericRequest formSubmitter = new GenericRequest("");

      if (action.contains("?")) {
        // For quirky URLs where there's a question mark
        // in the middle of the URL, just string the data
        // onto the URL.  This is the way browsers work,
        // so it's the way KoL expects the data.

        StringBuilder actionString = new StringBuilder();
        actionString.append(action);

        for (String element : elements) {
          if (element != null) {
            actionString.append('&');
            actionString.append(element);
          }
        }

        formSubmitter.constructURLString(
            GenericRequest.decodeField(actionString.toString(), StandardCharsets.ISO_8859_1));
      } else {
        // For normal URLs, the form data can be submitted
        // just like in every other request.

        formSubmitter.constructURLString(action);
        if (elements[0].length() > 0) {
          for (String element : elements) {
            if (element != null) {
              formSubmitter.addEncodedFormField(element);
            }
          }
        }
      }

      frame.refresh(formSubmitter);
    }

    private RequestFrame findFrame() {
      // Goal: find the RequestFrame that contains the RequestPane that
      // contains the HTML field containing this form submit button.
      // Original solution: enumerate all Frames, choose the one containing
      // text that matches the button name.  This broke in the presence
      // of HTML entities, and wasn't guaranteed to be unique anyway.
      // Try 1: enumerate enclosing containers until an instance of
      // RequestFrame is found.  This works for standalone windows, but
      // not for frames that open in a tab because they aren't actually
      // part of the containment hierarchy in that case - the contentPane
      // of the frame gets reparented into the main tabs.
      // Try 2: enumerate containers to find the RequestPane, enumerate
      // Frames to find the RequestFrame that owns it.

      Container c = this.getContainer();
      while (c != null && !(c instanceof RequestPane)) {
        c = c.getParent();
      }

      Frame[] frames = Frame.getFrames();
      for (Frame frame : frames) {
        if (frame instanceof RequestFrame rf && rf.mainDisplay == c) {
          return rf;
        }
      }
      return null;
    }
  }

  /**
   * Utility method used to determine the GenericRequest that should be sent, given the appropriate
   * location.
   */
}
