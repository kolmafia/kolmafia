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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// containers
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

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
	private JLabel [] equipment, familiarData;
	private JComboBox outfitSelect, familiarSelect;
	private JButton changeOutfitButton, changeFamiliarButton;

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
			characterData = client.getCharacterData();
			(new CharsheetRequest( client )).run();
			client.applyRecentEffects();
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
		entirePanel.add( createSouthPanel(), BorderLayout.SOUTH );

		getContentPane().add( entirePanel, "" );
		addWindowListener( new ReturnFocusAdapter() );
	}

	/**
	 * Utility method used for creating a panel displaying the character's current
	 * effects and their available outfits, as well as the ability to change the
	 * character's current effects and outfit.
	 *
	 * @return	a <code>JPanel</code> displaying the current effects
	 */

	private JPanel createSouthPanel()
	{
		JPanel southPanel = new JPanel();
		southPanel.setLayout( new BorderLayout( 10, 10 ) );

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new BorderLayout( 5, 5 ) );
		JLabel outfitLabel = new JLabel( "Outfits:  ", JLabel.RIGHT );
		JComponentUtilities.setComponentSize( outfitLabel, 80, 24 );
		labelPanel.add( outfitLabel, BorderLayout.NORTH );
		JLabel familiarLabel = new JLabel( "Familiar:  ", JLabel.RIGHT );
		JComponentUtilities.setComponentSize( familiarLabel, 80, 24 );
		labelPanel.add( familiarLabel, BorderLayout.SOUTH );

		southPanel.add( labelPanel, BorderLayout.WEST );

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new GridLayout( 2, 1 ) );
		outfitSelect = new JComboBox( characterData.getOutfits().getMirrorImage() );
		selectPanel.add( outfitSelect, "" );
		familiarSelect = new JComboBox( characterData.getFamiliars().getMirrorImage() );
		selectPanel.add( familiarSelect, "" );

		southPanel.add( selectPanel, BorderLayout.CENTER );

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BorderLayout( 5, 5 ) );
		changeOutfitButton = new JButton( "change" );
		JComponentUtilities.setComponentSize( changeOutfitButton, 84, 24 );
		changeOutfitButton.addActionListener( new ChangeOutfitListener() );
		buttonPanel.add( changeOutfitButton, BorderLayout.NORTH );
		changeFamiliarButton = new JButton( "change" );
		JComponentUtilities.setComponentSize( changeFamiliarButton, 84, 24 );
		changeFamiliarButton.addActionListener( new ChangeFamiliarListener() );
		buttonPanel.add( changeFamiliarButton, BorderLayout.SOUTH );

		southPanel.add( buttonPanel, BorderLayout.EAST );

		return southPanel;
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

		StringTokenizer parsedName = new StringTokenizer( characterData.getClassType() );
		StringBuffer imagename = new StringBuffer();
		while ( parsedName.hasMoreTokens() )
			imagename.append( parsedName.nextToken().toLowerCase() );

		imagePanel.add( new JLabel( JComponentUtilities.getSharedImage( imagename.toString() + ".gif" ) ), BorderLayout.CENTER );
		imagePanel.add( new JLabel( " " ), BorderLayout.SOUTH );
		return imagePanel;
	}

	/**
	 * Utility method for creating a panel that displays the given stats,
	 * using formatting if the values are different.
	 */

	private JPanel createValuePanel( int baseValue, int adjustedValue, int tillNextPoint )
	{
		JPanel displayPanel = new JPanel();
		displayPanel.setLayout( new BorderLayout( 0, 0 ) );
		if ( baseValue != adjustedValue )
		{
			JLabel adjustedLabel = new JLabel( "" + adjustedValue, JLabel.LEFT );
			adjustedLabel.setForeground( Color.BLUE );
			displayPanel.add( adjustedLabel, BorderLayout.WEST );
			displayPanel.add( new JLabel( " (" + baseValue + ")", JLabel.LEFT ), BorderLayout.CENTER );
		}
		else
			displayPanel.add( new JLabel( "" + baseValue, JLabel.LEFT ), BorderLayout.CENTER );

		displayPanel.setToolTipText( "" + tillNextPoint + " until " + (baseValue + 1) );
		return displayPanel;
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
		statsPanel.setLayout( new BoxLayout( statsPanel, BoxLayout.Y_AXIS ) );

		statsPanel.add( new JLabel( " " ) );

		statsPanel.add( new JLabel( characterData.getCurrentHP() + " / " + characterData.getMaximumHP() + " (HP)", JLabel.CENTER ), "");
		statsPanel.add( new JLabel( characterData.getCurrentMP() + " / " + characterData.getMaximumMP() + " (MP)", JLabel.CENTER ), "" );
		statsPanel.add( new JLabel( " " ) );

		JPanel primeStatLabels = new JPanel();
		primeStatLabels.setLayout( new GridLayout( 3, 1 ) );
		primeStatLabels.add( new JLabel( "Mus: ", JLabel.RIGHT ) );
		primeStatLabels.add( new JLabel( "Mys: ", JLabel.RIGHT ) );
		primeStatLabels.add( new JLabel( "Mox: ", JLabel.RIGHT ) );

		JPanel primeStatValues = new JPanel();
		primeStatValues.setLayout( new GridLayout( 3, 1 ) );
		primeStatValues.add( createValuePanel( characterData.getBaseMuscle(), characterData.getAdjustedMuscle(), characterData.getMuscleTNP() ) );
		primeStatValues.add( createValuePanel( characterData.getBaseMysticality(), characterData.getAdjustedMysticality(), characterData.getMysticalityTNP() ) );
		primeStatValues.add( createValuePanel( characterData.getBaseMoxie(), characterData.getAdjustedMoxie(), characterData.getMoxieTNP() ) );

		JPanel primeStatPanel = new JPanel();
		primeStatPanel.setLayout( new BoxLayout( primeStatPanel, BoxLayout.X_AXIS ) );
		primeStatPanel.add( primeStatLabels, BorderLayout.WEST );
		primeStatPanel.add( primeStatValues, BorderLayout.CENTER );
		statsPanel.add( primeStatPanel, "" );

		statsPanel.add( new JLabel( " " ), "" );
		statsPanel.add( new JLabel( characterData.getAvailableMeat() + " meat", JLabel.CENTER ), "" );
		statsPanel.add( new JLabel( characterData.getInebriety() + " drunkenness", JLabel.CENTER ), "" );
		statsPanel.add( new JLabel( characterData.getAdventuresLeft() + " adventures left", JLabel.CENTER ), "" );

		statsPanel.add( new JLabel( " " ), "" );
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
		fieldPanel.setLayout( new GridLayout( 12, 1 ) );

		fieldPanel.add( new JLabel( "" ) );
		fieldPanel.add( new JLabel( "Hat:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Weapon:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Pants:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Accessory:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "" ) );
		fieldPanel.add( new JLabel( "Familiar:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Item:  ", JLabel.RIGHT ) );
		fieldPanel.add( new JLabel( "Weight:  ", JLabel.RIGHT ) );

		JPanel valuePanel = new JPanel();
		valuePanel.setLayout( new GridLayout( 12, 1 ) );

		valuePanel.add( new JLabel( "" ) );

		equipment = new JLabel[6];
		for ( int i = 0; i < 6; ++i )
		{
			equipment[i] = new JLabel( "", JLabel.LEFT );
			valuePanel.add( equipment[i] );
		}

		valuePanel.add( new JLabel( "" ) );

		familiarData = new JLabel[3];
		for ( int i = 0; i < 3; ++i )
		{
			familiarData[i] = new JLabel( "", JLabel.LEFT );
			valuePanel.add( familiarData[i] );
		}

		JPanel equipPanel = new JPanel();
		equipPanel.setLayout( new BorderLayout() );
		equipPanel.add( fieldPanel, BorderLayout.WEST );
		equipPanel.add( valuePanel, BorderLayout.EAST );

		refreshEquipPanel();
		return equipPanel;
	}

	private void refreshEquipPanel()
	{	SwingUtilities.invokeLater( new EquipPanelRefresher() );
	}

	private class EquipPanelRefresher implements Runnable
	{
		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			equipment[0].setText( characterData.getHat() );
			equipment[1].setText( characterData.getWeapon() );
			equipment[2].setText( characterData.getPants() );
			equipment[3].setText( characterData.getAccessory1() );
			equipment[4].setText( characterData.getAccessory2() );
			equipment[5].setText( characterData.getAccessory3() );

			familiarData[0].setText( characterData.getFamiliarRace() );
			familiarData[1].setText( characterData.getFamiliarItem() );
			familiarData[2].setText( characterData.getFamiliarWeight() < 0 ? "unknown" :
				"" + characterData.getFamiliarWeight() );
		}
	}

	private class ChangeFamiliarListener implements ActionListener
	{
		private String change;

		public void actionPerformed( ActionEvent e )
		{
			change = (String) familiarSelect.getSelectedItem();
			if ( change != null )
				(new ChangeFamiliarThread()).start();
		}

		private class ChangeFamiliarThread extends Thread
		{
			public ChangeFamiliarThread()
			{
				super( "Change-Familiar-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				SwingUtilities.invokeLater( new FamiliarChangeGUIUpdater( true ) );
				(new FamiliarRequest( client, change )).run();
				SwingUtilities.invokeLater( new FamiliarChangeGUIUpdater( false ) );
			}

			private class FamiliarChangeGUIUpdater implements Runnable
			{
				private boolean isStart;

				public FamiliarChangeGUIUpdater( boolean isStart )
				{	this.isStart = isStart;
				}

				public void run()
				{
					if ( isStart )
						client.updateDisplay( KoLFrame.NOCHANGE_STATE, "Changing familiar..." );
					else
					{
						client.updateDisplay( KoLFrame.NOCHANGE_STATE, "" );
						refreshEquipPanel();
					}

					CharsheetFrame.this.familiarSelect.setEnabled( !isStart );
					CharsheetFrame.this.changeFamiliarButton.setEnabled( !isStart );
				}
			}
		}
	}

	private class ChangeOutfitListener implements ActionListener
	{
		private SpecialOutfit change;

		public void actionPerformed( ActionEvent e )
		{
			change = (SpecialOutfit) outfitSelect.getSelectedItem();
			if ( change != null )
				(new ChangeEquipmentThread()).start();
		}

		private class ChangeEquipmentThread extends Thread
		{
			public ChangeEquipmentThread()
			{
				super( "Change-Equipment-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				SwingUtilities.invokeLater( new OutfitChangeGUIUpdater( true ) );
				(new EquipmentRequest( client, change )).run();
				SwingUtilities.invokeLater( new OutfitChangeGUIUpdater( false ) );
			}

			private class OutfitChangeGUIUpdater implements Runnable
			{
				private boolean isStart;

				public OutfitChangeGUIUpdater( boolean isStart )
				{	this.isStart = isStart;
				}

				public void run()
				{
					if ( isStart )
						client.updateDisplay( KoLFrame.NOCHANGE_STATE, "Changing outfit..." );
					else
					{
						client.updateDisplay( KoLFrame.NOCHANGE_STATE, "" );
						refreshEquipPanel();
					}

					CharsheetFrame.this.outfitSelect.setEnabled( !isStart );
					CharsheetFrame.this.changeOutfitButton.setEnabled( !isStart );
				}
			}
		}
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
