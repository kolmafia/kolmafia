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

// layout
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ImageIcon;

// utilities
import java.net.URL;
import java.net.MalformedURLException;
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CharsheetFrame extends javax.swing.JFrame
{
	public CharsheetFrame( KoLmafia client )
	{
		super( "KoLmafia: " + client.getLoginName() + " (Character Sheet)" );

		// For now, because character listeners haven't been implemented
		// yet, re-request the character sheet from the server

		KoLCharacter characterData = new KoLCharacter( client.getLoginName() );
		(new CharsheetRequest( client, characterData )).run();
		(new EquipmentRequest( client, characterData )).run();

		setResizable( false );

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		JPanel entirePanel = new JPanel();
		entirePanel.setLayout( new BorderLayout( 20, 20 ) );

		entirePanel.add( createStatsPanel( characterData ), BorderLayout.WEST );
		entirePanel.add( createEquipPanel( characterData ), BorderLayout.EAST );
		entirePanel.add( createImagePanel( characterData ), BorderLayout.CENTER );

		getContentPane().add( entirePanel, "" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	private JPanel createImagePanel( KoLCharacter characterData )
	{
		JPanel imagePanel = new JPanel();
		imagePanel.setLayout( new BorderLayout( 10, 10 ) );

		JPanel namePanel = new JPanel();
		namePanel.setLayout( new GridLayout( 2, 1 ) );
		namePanel.add( new JLabel( characterData.getUsername() + " (#" + characterData.getUserID() + ")", JLabel.CENTER ) );
		namePanel.add( new JLabel( "Level " + characterData.getLevel() + " " + characterData.getClassName(), JLabel.CENTER ) );

		imagePanel.add( namePanel, BorderLayout.NORTH );

		StringTokenizer parsedName = new StringTokenizer( characterData.getClassName() );
		StringBuffer imagename = new StringBuffer();
		while ( parsedName.hasMoreTokens() )
			imagename.append( parsedName.nextToken().toLowerCase() );

		try
		{
			imagePanel.add( new JLabel( new ImageIcon( new URL(
				"http://images.kingdomofloathing.com/otherimages/" + imagename.toString() + ".gif" ) ) ), BorderLayout.CENTER );
		}
		catch ( MalformedURLException e )
		{
		}

		imagePanel.add( new JLabel( " " ), BorderLayout.SOUTH );
		return imagePanel;
	}

	private JPanel createStatsPanel( KoLCharacter characterData )
	{
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout( new GridLayout( 10, 1 ) );

		statsPanel.add( new JLabel( " " ) );

		statsPanel.add( new JLabel( characterData.getCurrentHP() + " / " + characterData.getMaximumHP() + " (HP)", JLabel.CENTER ) );
		statsPanel.add( new JLabel( characterData.getCurrentMP() + " / " + characterData.getMaximumMP() + " (MP)", JLabel.CENTER ) );
		statsPanel.add( new JLabel( " " ) );

		statsPanel.add( new JLabel(
			characterData.getAdjustedMuscle() + " / " +
				characterData.getAdjustedMysticality() + " / " +
					characterData.getAdjustedMoxie(), JLabel.CENTER ) );

		statsPanel.add( new JLabel( " " ) );
		statsPanel.add( new JLabel( characterData.getAvailableMeat() + " meat", JLabel.CENTER ) );
		statsPanel.add( new JLabel( characterData.getInebriety() + " drunkenness", JLabel.CENTER ) );
		statsPanel.add( new JLabel( characterData.getAdventuresLeft() + " adventures left", JLabel.CENTER ) );

		statsPanel.add( new JLabel( " " ) );

		return statsPanel;
	}

	private JPanel createEquipPanel( KoLCharacter characterData )
	{
		JPanel fieldPanel = new JPanel();
		fieldPanel.setLayout( new GridLayout( 9, 1 ) );

		fieldPanel.add( new JLabel( "" ) );
		fieldPanel.add( new JLabel( "Hat:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Weapon:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Pants:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Familiar:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "" ) );

		JPanel valuePanel = new JPanel();
		valuePanel.setLayout( new GridLayout( 9, 1 ) );

		valuePanel.add( new JLabel( "" ) );
		valuePanel.add( new JLabel( characterData.getHat(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getWeapon(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getPants(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getAccessory1(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getAccessory2(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getAccessory3(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( characterData.getFamiliarItem(), JLabel.LEFT ) );
		valuePanel.add( new JLabel( "" ) );

		JPanel equipPanel = new JPanel();
		equipPanel.setLayout( new BorderLayout() );
		equipPanel.add( fieldPanel, BorderLayout.WEST );
		equipPanel.add( valuePanel, BorderLayout.EAST );

		return equipPanel;
	}
}
