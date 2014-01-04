/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.swingui.listener.RelayBrowserListener;

/**
 * An internal class which displays KoLmafia's current version information. This is passed to the constructor for
 * the <code>LicenseDisplay</code>.
 */

public class VersionDataPanel
	extends JPanel
{
	private final String[] versionData =
	{
		StaticEntity.getVersion(),
		KoLConstants.VERSION_DATE,
		" ",
		"Copyright \u00a9 2005-2014 KoLmafia development team",
		"Berkeley Software Development (BSD) License",
		"http://kolmafia.sourceforge.net/",
		" ",
		"Current Running on " + System.getProperty( "os.name" ),
		"Local Directory is " + System.getProperty( "user.dir" ),
		"Settings in " + KoLConstants.ROOT_LOCATION.getAbsolutePath(),
		"Using Java v" + System.getProperty( "java.runtime.version" )
	};

	public VersionDataPanel()
	{
		JPanel versionPanel = new JPanel( new BorderLayout( 20, 20 ) );
		versionPanel.add(
			new JLabel( JComponentUtilities.getImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.NORTH );

		JPanel labelPanel = new JPanel( new GridLayout( this.versionData.length, 1 ) );
		for ( int i = 0; i < this.versionData.length; ++i )
		{
			labelPanel.add( new JLabel( this.versionData[ i ], JLabel.CENTER ) );
		}

		versionPanel.add( labelPanel, BorderLayout.CENTER );

		JButton donateButton = new JButton( JComponentUtilities.getImage( "paypal.gif" ) );
		JComponentUtilities.setComponentSize( donateButton, 74, 31 );
		donateButton.addActionListener( new RelayBrowserListener( "http://sourceforge.net/project/project_donations.php?group_id=126572" ) );

		JPanel donatePanel = new JPanel();
		donatePanel.add( donateButton );

		JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
		centerPanel.add( versionPanel, BorderLayout.CENTER );
		centerPanel.add( donatePanel, BorderLayout.SOUTH );

		this.setLayout( new CardLayout( 20, 20 ) );
		this.add( centerPanel, "" );
	}
}
