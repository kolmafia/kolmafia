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
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

import javax.swing.text.View;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.FormView;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.HTMLEditorKit;

import java.net.URL;
import java.util.TreeMap;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;

/**
 * An extension of a standard <code>HTMLEditorKit</code> which overrides the
 * <code>getViewFactory()</code> method in order to return a different factory
 * instance which properly handles data submission requests.
 */

public class RequestEditorKit extends HTMLEditorKit implements KoLConstants
{
	private static TreeMap images = new TreeMap();
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
			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.INPUT )
				return new KoLSubmitView( elem );

			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.IMG )
				return new KoLImageView( elem );

			return super.create( elem );
		}
	}

	/**
	 * Downloads the given file from the KoL images server
	 * and stores it locally.
	 */

	public static URL downloadImage( String filename )
	{
		if ( images.containsKey( filename ) )
			return (URL) images.get( filename );

		String localname = filename.replaceAll( "http://images.kingdomofloathing.com/", "" ).replaceAll( "/",
			File.separator.replaceAll( "\\\\", "\\\\\\\\" ) );

		File localfile = new File( "images" + File.separator + localname );

		try
		{
			// If the file has already been downloaded, then there
			// is nothing more to do - return from this method.

			BufferedInputStream in = new BufferedInputStream( localfile.exists() ? new FileInputStream( localfile ) :
				(new URL( filename )).openConnection().getInputStream() );
			localfile.getParentFile().mkdirs();

			ByteArrayOutputStream outbytes = new ByteArrayOutputStream( 1024 );
			byte [] buffer = new byte[1024];

			int offset;
			while ((offset = in.read(buffer)) > 0)
				outbytes.write(buffer, 0, offset);

			in.close();
			outbytes.flush();

			buffer = outbytes.toByteArray();

			FileOutputStream out = new FileOutputStream( localfile );
			out.write( buffer, 0, buffer.length );

			in.close();
			out.flush();
			out.close();

			images.put( filename, localfile.toURL() );
			return (URL) images.get( filename );
		}
		catch ( Exception e )
		{
			// If an IOException occurs at any time during the
			// attempt to retrieve the image, report the exception
			// to the console and return null;

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
			return null;
		}
	}

	private static class KoLImageView extends ImageView
	{
		public KoLImageView( Element elem )
		{	super( elem );
		}

	    public URL getImageURL()
		{
			String src = (String) getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
				return null;

			return downloadImage( src );
		}
	}

	/**
	 * Utility method which converts the given text into a form which
	 * can be displayed properly in a <code>JEditorPane</code>.  This
	 * method is necessary primarily due to the bad HTML which is used
	 * but can still be properly rendered by post-3.2 browsers.
	 */

	public static final String getDisplayHTML( String responseText )
	{
		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		String displayHTML = responseText.replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll( "<[Hh][Rr].*?>", "<br>" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML = displayHTML.replaceAll( "font-size: .8em;", "" ).replaceAll( "<font size=[12]>", "" ).replaceAll(
			" class=small", "" ).replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML = displayHTML.replaceAll( "<tr><td([^>]*?) bgcolor=black([^>]*?)>((</td>)?)</tr>", "<tr><td$1$2></td></tr>" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// turn:  <form...><tr...><td...>...</td></tr></form>
		// into:  <tr...><td...><form...>...</form></td></tr>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)<tr>(<td[^>]*>)", "<tr>$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\([\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML = displayHTML.replaceAll( "<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>" , "</style>" );

		// For some reason, character entitites are not properly
		// handled by the mini browser.

		displayHTML = displayHTML.replaceAll( "&ntilde;", "n" ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&infin;", "**" );
		return displayHTML;
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
			// Get the element

			Element inputElement = getElement();

			if ( inputElement == null )
				return;

			// Get the "value" associated with this input

			String value = (String)inputElement.getAttributes().getAttribute( HTML.Attribute.VALUE );

			// If there is no value, we won't be able to find the
			// frame that handles this form.

			if ( value == null )
				return;

			// Retrieve the frame which is being used by this form
			// viewer.

			RequestFrame frame = findFrame( value );

			// If there is no frame, then there's nothing to
			// refresh, so return.

			if ( frame == null )
				return;

			// Retrieve the form element so that you know where you
			// need to submit the data.

			Element formElement = inputElement;

			while ( formElement != null && formElement.getAttributes().getAttribute( StyleConstants.NameAttribute ) != HTML.Tag.FORM )
				formElement = formElement.getParentElement();

			// If the form element is null, then there was no
			// enclosing form for the <INPUT> tag, so you can
			// return, doing nothing.

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

			// Now get the data fields we will submit to this form

			String [] elements = data.split( "&" );
			String [] fields = new String[ elements.length ];

			int valueIndex = 0;

			if ( elements[0].length() > 0 )
			{
				for ( int i = 0; i < elements.length; ++i )
					fields[i] = elements[i].substring( 0, elements[i].indexOf( "=" ) );
			}
			else
				fields[0] = "";

			// Prepare the element string -- make sure that
			// you don't have duplicate fields.

			for ( int i = 0; i < elements.length; ++i )
				for ( int j = i + 1; j < elements.length; ++j )
					if ( elements[i] != null && elements[j] != null && fields[i].equals( fields[j] ) )
						elements[j] = null;

			// Now, prepare the request string that will
			// be posted to KoL.

			KoLRequest request;

			if ( action.indexOf( "?" ) != -1 )
			{
				// For quirky URLs where there's a question mark
				// in the middle of the URL, just string the data
				// onto the URL.  This is the way browsers work,
				// so it's the way KoL expects the data.

				StringBuffer actionString = new StringBuffer();
				actionString.append( action );

				for ( int i = 0; i < elements.length; ++i )
					if ( elements[i] != null )
					{
						actionString.append( '&' );
						actionString.append( elements[i] );
					}

				request = new KoLRequest( frame.client, actionString.toString(), true );
			}
			else
			{
				// For normal URLs, the form data can be submitted
				// just like in every other request.

				request = new KoLRequest( frame.client, action, true );
				if ( elements[0].length() > 0 )
					for ( int i = 0; i < elements.length; ++i )
						if ( elements[i] != null )
							request.addFormField( elements[i] );
			}

			frame.refresh( request );
		}

		private RequestFrame findFrame( String value )
		{
			Object [] frames = existingFrames.toArray();
			String search = "value=\"" + value + "\"";

			for ( int i = 0; i < frames.length; ++i )
			{
				if ( !( frames[i] instanceof RequestFrame ) )
					continue;

				RequestFrame frame = (RequestFrame)frames[i];
				if  ( frame.mainDisplay.getText().indexOf( search ) != -1 )
				      return frame;
			}

			return null;
		}
	}
}
