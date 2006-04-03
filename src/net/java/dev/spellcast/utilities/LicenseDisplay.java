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

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

public class LicenseDisplay extends javax.swing.JFrame
{
	public static final int DATA_FILE  = 0;
	public static final int IMAGE_FILE = 1;

	private String [] fileNames;
	private int [] fileTypes;
	private String [] tabNames;

	public LicenseDisplay( String title, String [] fileNames, String [] tabNames )
	{	this( title, null, fileNames, tabNames );
	}

	public LicenseDisplay( String title, JComponent versionData, String [] fileNames, String [] tabNames )
	{
		super( title );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		this.fileNames = fileNames;

		this.fileTypes = new int[ fileNames.length ];
		for ( int i = 0; i < fileNames.length; ++i )
			this.fileTypes[i] = fileNames[i].endsWith( ".txt" ) || fileNames[i].endsWith( ".htm" ) || fileNames[i].endsWith( ".html" ) ? DATA_FILE : IMAGE_FILE;

		this.tabNames = tabNames;

		JPanel contentPanel = (JPanel) getContentPane();
		JTabbedPane tabbedPane = new JTabbedPane();

		if ( versionData != null )
		{
			JScrollPane scroller = new JScrollPane( versionData, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( scroller, 540, 400 );
			tabbedPane.addTab( "Version Info", scroller );
		}

		for ( int i = 0; i < fileNames.length; ++i )
		{
			JComponent nextLicense = new JScrollPane( getLicenseDisplay(i),
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( nextLicense, 540, 400 );
			tabbedPane.addTab( tabNames[i], nextLicense );
		}

		contentPanel.add( tabbedPane );

		setResizable( false );
		pack();  setVisible( true );
	}

	private JComponent getLicenseDisplay( int index )
	{
		JComponent licenseDisplay = null;

		switch ( fileTypes[index] )
		{
			case DATA_FILE:
			{
				licenseDisplay = new JEditorPane();
				java.io.BufferedReader buf = DataUtilities.getReaderForSharedDataFile( fileNames[index] );

				// in the event that the license display could not be found, return a blank
				// label indicating that the license could not be found
				if ( buf == null )
					return getNoLicenseNotice();

				StringBuffer licenseText = new StringBuffer();
				String line;

				try
				{
					while ( (line = buf.readLine()) != null )
					{
						licenseText.append( line );
						licenseText.append( System.getProperty( "line.separator" ) );
					}
				}
				catch ( java.io.IOException e )
				{
				}

				if ( fileNames[ index ].endsWith( ".txt" ) )
					((JEditorPane)licenseDisplay).setContentType( "text/plain" );
				else
					((JEditorPane)licenseDisplay).setContentType( "text/html" );

				((JEditorPane)licenseDisplay).setText( licenseText.toString() );
				((JEditorPane)licenseDisplay).setCaretPosition( 0 );
				((JEditorPane)licenseDisplay).setEditable( false );

				break;
			}
			case IMAGE_FILE:
			{
				javax.swing.ImageIcon licenseImage = JComponentUtilities.getSharedImage( fileNames[index] );

				if ( licenseImage == null )
					return getNoLicenseNotice();

				licenseDisplay = new JLabel( licenseImage );
				break;
			}
		}

		return licenseDisplay;
	}

	private JComponent getNoLicenseNotice()
	{
		JLabel noLicenseNotice = JComponentUtilities.createLabel( "No license could be found", JLabel.CENTER,
			java.awt.SystemColor.activeCaption, java.awt.Color.white );
		return noLicenseNotice;
	}
}