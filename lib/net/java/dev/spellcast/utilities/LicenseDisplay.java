/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.centerkey.BareBonesBrowserLaunch;

public class LicenseDisplay
	extends JFrame
{
	public static final int DATA_FILE = 0;
	public static final int IMAGE_FILE = 1;

	private final String[] fileNames;
	private final int[] fileTypes;
	private final CardLayout cards;
	private final JPanel content;
	private final JList listing;

	public LicenseDisplay( final String title, final String[] fileNames, final String[] tabNames )
	{
		this( title, null, fileNames, tabNames );
	}

	public LicenseDisplay( final String title, final JComponent versionData, final String[] fileNames,
		final String[] tabNames )
	{
		super( title );
		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

		this.fileNames = fileNames;

		this.fileTypes = new int[ fileNames.length ];
		for ( int i = 0; i < fileNames.length; ++i )
		{
			this.fileTypes[ i ] =
				fileNames[ i ].endsWith( ".txt" ) || fileNames[ i ].endsWith( ".htm" ) || fileNames[ i ].endsWith( ".html" ) ? LicenseDisplay.DATA_FILE : LicenseDisplay.IMAGE_FILE;
		}

		this.cards = new CardLayout( 5, 5 );
		this.content = new JPanel( this.cards );

		LockableListModel model = new LockableListModel();
		this.listing = new JList( model );

		if ( versionData != null )
		{
			JScrollPane scroller =
				new JScrollPane(
					versionData, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( scroller, 540, 400 );

			model.add( "Version Info" );
			this.content.add( scroller, "Version Info" );
		}

		for ( int i = 0; i < fileNames.length; ++i )
		{
			JComponent nextLicense =
				new JScrollPane(
					this.getLicenseDisplay( i ), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( nextLicense, 540, 400 );

			model.add( tabNames[ i ] );
			this.content.add( nextLicense, tabNames[ i ] );
		}

		this.listing.addListSelectionListener( new CardSwitchListener() );

		JPanel listHolder = new JPanel( new CardLayout( 5, 5 ) );
		listHolder.add(
			new JScrollPane(
				this.listing, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER ), "" );

		( (JPanel) this.getContentPane() ).setLayout( new BorderLayout( 0, 0 ) );
		( (JPanel) this.getContentPane() ).add( listHolder, BorderLayout.WEST );
		( (JPanel) this.getContentPane() ).add( this.content, BorderLayout.CENTER );
	}

	private class CardSwitchListener
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			String card = (String) LicenseDisplay.this.listing.getSelectedValue();

			if ( card != null )
			{
				LicenseDisplay.this.cards.show( LicenseDisplay.this.content, card );
			}
		}
	}

	private JComponent getLicenseDisplay( final int index )
	{
		JComponent licenseDisplay = null;

		switch ( this.fileTypes[ index ] )
		{
		case DATA_FILE:
		{
			licenseDisplay = new JEditorPane();
			java.io.BufferedReader buf = DataUtilities.getReader( "licenses", this.fileNames[ index ] );

			boolean plainText = this.fileNames[ index ].endsWith( ".txt" );


			// in the event that the license display could not be found, return a blank
			// label indicating that the license could not be found
			if ( buf == null )
			{
				return this.getNoLicenseNotice();
			}

			StringBuilder licenseText = new StringBuilder();
			String line;

			try
			{
				while ( ( line = buf.readLine() ) != null )
				{
					int start = 0;

					if ( plainText )
					{
						int end = line.indexOf( '<' );

						while ( end != -1 )
						{
							licenseText.append( line.substring( start, end ) );
							licenseText.append( "&lt;" );

							start = end + 1;

							if ( start == line.length() )
							{
								break;
							}

							end = line.indexOf( '<', start );
						}
					}

					if ( start < line.length() )
					{
						licenseText.append( line.substring( start ) );
					}

					licenseText.append( System.getProperty( "line.separator" ) );
				}
			}
			catch ( java.io.IOException e )
			{
			}

			if ( plainText )
			{
				licenseText.insert( 0, "<blockquote><pre style=\"font-family: Verdana; font-size: small\">" );
				licenseText.append( "</pre></blockquote>" );
			}
			else
			{
				licenseText.insert( 0, "<blockquote style=\"font-family: Verdana; font-size: small\">" );
				licenseText.append( "</blockquote>" );
			}

			( (JEditorPane) licenseDisplay ).setContentType( "text/html" );
			( (JEditorPane) licenseDisplay ).setText( licenseText.toString() );
			( (JEditorPane) licenseDisplay ).setCaretPosition( 0 );
			( (JEditorPane) licenseDisplay ).setEditable( false );
			( (JEditorPane) licenseDisplay ).addHyperlinkListener( new HyperlinkAdapter() );

			break;
		}
		case IMAGE_FILE:
		{
			try
			{
				javax.swing.ImageIcon licenseImage = JComponentUtilities.getImage( "licenses", this.fileNames[ index ] );
				if ( licenseImage == null )
				{
					return this.getNoLicenseNotice();
				}

				licenseDisplay = new JLabel( licenseImage );
				break;
			}
			catch ( Exception e )
			{
				System.out.println( e );
				e.printStackTrace();
			}
		}
		}

		return licenseDisplay;
	}

	private JComponent getNoLicenseNotice()
	{
		JLabel noLicenseNotice =
			JComponentUtilities.createLabel(
				"No license could be found", SwingConstants.CENTER, java.awt.SystemColor.activeCaption,
				java.awt.Color.white );
		return noLicenseNotice;
	}

	private static class HyperlinkAdapter
		implements HyperlinkListener
	{
		public void hyperlinkUpdate( final HyperlinkEvent e )
		{
			if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
			{
				String location = e.getDescription();
				BareBonesBrowserLaunch.openURL( location );
			}
		}
	}

}
