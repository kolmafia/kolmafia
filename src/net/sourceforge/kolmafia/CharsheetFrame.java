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

/**
 * An extension of <code>KoLFrame</code> used to display the character
 * sheet for the current user.  Note that this can only be instantiated
 * when the character is logged in; if the character has logged out,
 * this method will contain blank data.  Note also that the avatar that
 * is currently displayed will be the default avatar from the class and
 * will not reflect outfits or customizations.
 */

public class CharsheetFrame extends KoLFrame
{
	private KoLCharacter characterData;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	client	The client containing the data associated with the character
	 */

	public CharsheetFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" (Character Sheet)", client );

		// For now, because character listeners haven't been implemented
		// yet, re-request the character sheet from the server

		if ( client != null )
		{
			characterData = new KoLCharacter( client.getLoginName() );
			(new CharsheetRequest( client, characterData )).run();
			(new EquipmentRequest( client, characterData )).run();
		}
		else
			characterData = new KoLCharacter( "UI Test" );

		setResizable( false );
		contentPanel = null;

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		JPanel entirePanel = new JPanel();
		entirePanel.setLayout( new BorderLayout( 20, 20 ) );

		entirePanel.add( createStatsPanel(), BorderLayout.WEST );
		entirePanel.add( createEquipPanel(), BorderLayout.EAST );
		entirePanel.add( createImagePanel(), BorderLayout.CENTER );

		getContentPane().add( entirePanel, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
	}

	/**
	 * Utility method used for creating a panel displaying the character's avatar.
	 * Because image retrieval has not been implemented, this method displays
	 * only the default avatar for the character's class.
	 *
	 * @return	a <code>JPanel</code> displaying the class-specific avatar
	 */

	private JPanel createImagePanel()
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

	/**
	 * Utility method for creating a panel displaying the character's vital
	 * statistics, including a basic stat overview and available turns/meat.
	 *
	 * @return	a <code>JPanel</code> displaying the character's statistics
	 */

	private JPanel createStatsPanel()
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

	/**
	 * Utility method for creating a panel displaying the character's current
	 * equipment, accessories and familiar item.
	 *
	 * @return	a <code>JPanel</code> displaying the character's equipment
	 */

	private JPanel createEquipPanel()
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

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new CharsheetFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
