/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JEditorPane;

import javax.swing.text.View;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.FormView;
import javax.swing.text.html.HTMLEditorKit;

/**
 * An extension of a standard <code>HTMLEditorKit</code> which overrides the
 * <code>getViewFactory()</code> method in order to return a different factory
 * instance which properly handles data submission requests.
 */

public class RequestEditorKit extends HTMLEditorKit implements KoLConstants
{
	private static KoLmafia client;
	private static RequestFrame frame;

	private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

	/**
	 * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts
	 * some of the form handling to ensure that <code>KoLRequest</code> objects
	 * are instantiated on form submission rather than the <code>HttpRequest</code>
	 * objects created by the default HTML editor kit.
	 */

	public ViewFactory getViewFactory()
	{	return DEFAULT_FACTORY;
	}

	/**
	 * Registers the client that is supposed to be used for handling data submission
	 * to the Kingdom of Loathing server.
	 */

	private static class RequestViewFactory extends HTMLFactory
	{
		public View create( Element elem )
		{
			return elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.INPUT ?
				new KoLSubmitView( elem ) : super.create( elem );
		}
	}

	private static class KoLSubmitView extends FormView
	{
		public KoLSubmitView( Element elem )
		{	super( elem );
		}

		protected Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && (c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox) )
				c.setBackground( Color.white );

			return c;
		}

		protected void submitData( String data )
		{
			String [] splits = data.split( "[&=]" );

			// First, attempt to retrieve the frame which
			// is being used by this form viewer.

			String frameText = null;
			RequestFrame frame = null;
			Object [] frames = existingFrames.toArray();

			for ( int i = 0; i < frames.length && frame == null; ++i )
			{
				if ( frames[i] instanceof RequestFrame )
				{
					frame = (RequestFrame) frames[i];
					frameText = frame.display1.getText();

					for ( int j = 0; j < splits.length && frame != null; ++j )
						if ( frameText.indexOf( splits[j] ) == -1 )
							frame = null;
				}
			}

			// If there is no frame, then there's nothing to
			// refresh, so return.

			if ( frame == null )
				return;

			// Next, retrieve the form element so that
			// you know where you need to submit the data.

			Element formElement = getElement();

			while ( formElement != null && formElement.getAttributes().getAttribute( StyleConstants.NameAttribute ) != HTML.Tag.FORM )
				formElement = formElement.getParentElement();

			// At this point, if the form element is null,
			// then there was no enclosing form for the
			// <INPUT> tag, so you can return, doing nothing.

			if ( formElement == null )
				return;

			// Now that you know you have a form element,
			// get the action field, attach the data, and
			// refresh the appropriate request frame.

			String action = (String) formElement.getAttributes().getAttribute( HTML.Attribute.ACTION );

			// If there is no action, how do we know which page to
			// connect to?  We assume it's the originating page.

			if ( action == null )
				action = frame.getCurrentLocation();

			KoLRequest request = new KoLRequest( frame.client, action, true );

			if ( splits.length > 1 )
				for ( int i = 0; i < splits.length; ++i )
					request.addFormField( splits[i], (++i) < splits.length ? splits[i] : "" );

			frame.refresh( request );

			// In the event that it's something that required a
			// password hash, you'll probably need to refresh
			// the side panel.

			if ( action.indexOf( "pwd=" ) != -1 || data.indexOf( "pwd=" ) != -1 )
				frame.refreshSidePane();
		}
	}
}
