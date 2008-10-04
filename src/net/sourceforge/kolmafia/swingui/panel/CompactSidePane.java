/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.swingui.button.RequestButton;
import net.sourceforge.kolmafia.swingui.widget.UnanimatedLabel;

public class CompactSidePane
	extends JPanel
	implements Runnable
{
	private final JPanel levelPanel;
	private JProgressBar levelMeter;
	private JLabel levelLabel, roninLabel, mcdLabel;
	private JLabel musLabel, mysLabel, moxLabel;
	private JLabel fullLabel, drunkLabel, spleenLabel;
	private JLabel hpLabel, mpLabel, meatLabel, advLabel;
	private JLabel familiarLabel;
	private JLabel mlLabel, encLabel, initLabel;
	private JLabel expLabel, meatDropLabel, itemDropLabel;
	private JLabel hoboLabel, hoboPowerLabel;

	public CompactSidePane()
	{
		super( new BorderLayout() );

		JPanel labelPanel, valuePanel;

		JPanel[] panels = new JPanel[ 6 ];
		int panelCount = -1;

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		this.levelPanel = panels[ 0 ];

		panels[ panelCount ].add( this.levelLabel = new JLabel( " ", JLabel.CENTER ), BorderLayout.NORTH );

		panels[ panelCount ].add( this.levelMeter = new JProgressBar(), BorderLayout.CENTER );
		this.levelMeter.setOpaque( true );
		this.levelMeter.setStringPainted( true );
		JComponentUtilities.setComponentSize( this.levelMeter, 40, 6 );
		panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.WEST );
		panels[ panelCount ].add( Box.createHorizontalStrut( 10 ), BorderLayout.EAST );
		panels[ panelCount ].setOpaque( false );

		JPanel holderPanel = new JPanel( new GridLayout( 2, 1 ) );
		holderPanel.add( this.roninLabel = new JLabel( " ", JLabel.CENTER ) );
		holderPanel.add( this.mcdLabel = new JLabel( " ", JLabel.CENTER ) );
		holderPanel.setOpaque( false );
		panels[ panelCount ].add( holderPanel, BorderLayout.SOUTH );

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		panels[ panelCount ].setOpaque( false );

		labelPanel = new JPanel( new GridLayout( 3, 1 ) );
		labelPanel.setOpaque( false );

		labelPanel.add( new JLabel( "   Mus: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "   Mys: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "   Mox: ", JLabel.RIGHT ) );

		valuePanel = new JPanel( new GridLayout( 3, 1 ) );
		valuePanel.setOpaque( false );

		valuePanel.add( this.musLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.mysLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.moxLabel = new JLabel( " ", JLabel.LEFT ) );

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		panels[ panelCount ].setOpaque( false );

		labelPanel = new JPanel( new GridLayout( 4, 1 ) );
		labelPanel.setOpaque( false );

		labelPanel.add( new JLabel( "   HP: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "   MP: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "   Meat: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "   Adv: ", JLabel.RIGHT ) );

		valuePanel = new JPanel( new GridLayout( 4, 1 ) );
		valuePanel.setOpaque( false );

		valuePanel.add( this.hpLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.mpLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.meatLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.advLabel = new JLabel( " ", JLabel.LEFT ) );

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		panels[ panelCount ].setOpaque( false );

		labelPanel = new JPanel( new GridLayout( 3, 1 ) );
		labelPanel.setOpaque( false );

		labelPanel.add( new JLabel( "  Full: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "  Drunk: ", JLabel.RIGHT ) );
		labelPanel.add( new JLabel( "  Spleen: ", JLabel.RIGHT ) );

		valuePanel = new JPanel( new GridLayout( 3, 1 ) );
		valuePanel.setOpaque( false );

		valuePanel.add( this.fullLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.drunkLabel = new JLabel( " ", JLabel.LEFT ) );
		valuePanel.add( this.spleenLabel = new JLabel( " ", JLabel.LEFT ) );

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new GridLayout( 1, 1 ) );
		panels[ panelCount ].add( this.familiarLabel = new UnanimatedLabel() );

		panels[ ++panelCount ] = new JPanel( new GridLayout( 8, 2 ) );
		panels[ panelCount ].add( new JLabel( "ML: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.mlLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( new JLabel( "Enc: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.encLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( new JLabel( "Init: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.initLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( new JLabel( "Exp: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.expLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( new JLabel( "Meat: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.meatDropLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( new JLabel( "Item: ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.itemDropLabel = new JLabel( " ", JLabel.LEFT ) );
		panels[ panelCount ].add( this.hoboLabel = new JLabel( " ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.hoboPowerLabel = new JLabel( " ", JLabel.LEFT ) );

		JPanel compactContainer = new JPanel();
		compactContainer.setOpaque( false );
		compactContainer.setLayout( new BoxLayout( compactContainer, BoxLayout.Y_AXIS ) );

		for ( int i = 0; i < panels.length; ++i )
		{
			panels[ i ].setOpaque( false );
			compactContainer.add( panels[ i ] );
			compactContainer.add( Box.createVerticalStrut( 20 ) );
		}

		JPanel compactCard = new JPanel( new CardLayout( 8, 8 ) );
		compactCard.setOpaque( false );
		compactCard.add( compactContainer, "" );

		JPanel refreshPanel = new JPanel();
		refreshPanel.setOpaque( false );
		refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", new CharSheetRequest() ) );

		this.add( compactCard, BorderLayout.NORTH );
		this.add( refreshPanel, BorderLayout.SOUTH );

		this.levelLabel.setForeground( Color.BLACK );
		this.roninLabel.setForeground( Color.BLACK );
		this.mcdLabel.setForeground( Color.BLACK );
		this.musLabel.setForeground( Color.BLACK );
		this.mysLabel.setForeground( Color.BLACK );
		this.moxLabel.setForeground( Color.BLACK );
		this.fullLabel.setForeground( Color.BLACK );
		this.drunkLabel.setForeground( Color.BLACK );
		this.spleenLabel.setForeground( Color.BLACK );
		this.hpLabel.setForeground( Color.BLACK );
		this.mpLabel.setForeground( Color.BLACK );
		this.meatLabel.setForeground( Color.BLACK );
		this.advLabel.setForeground( Color.BLACK );
		this.familiarLabel.setForeground( Color.BLACK );
		this.mlLabel.setForeground( Color.BLACK );
		this.encLabel.setForeground( Color.BLACK );
		this.initLabel.setForeground( Color.BLACK );
		this.expLabel.setForeground( Color.BLACK );
		this.meatDropLabel.setForeground( Color.BLACK );
		this.itemDropLabel.setForeground( Color.BLACK );
		this.hoboPowerLabel.setForeground( Color.BLACK );
	}

	public String getStatText( final int adjusted, final int base )
	{
		return adjusted == base ? "<html>" + Integer.toString( base ) : adjusted > base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" : "<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
	}

	public void run()
	{
		this.levelLabel.setText( "Level " + KoLCharacter.getLevel() );

		if ( KoLCharacter.inBadMoon() )
		{
			this.roninLabel.setText( "(Bad Moon)" );
		}
		else if ( KoLCharacter.isHardcore() )
		{
			this.roninLabel.setText( "(Hardcore)" );
		}
		else if ( KoLCharacter.canInteract() )
		{
			this.roninLabel.setText( "(Ronin Clear)" );
		}
		else
		{
			this.roninLabel.setText( "(Ronin for " + ( 1000 - KoLCharacter.getCurrentRun() ) + ")" );
		}

		this.mcdLabel.setText( "ML @ " + KoLCharacter.getSignedMLAdjustment() );

		this.musLabel.setText( this.getStatText( KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle() ) );
		this.mysLabel.setText( this.getStatText(
			KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality() ) );
		this.moxLabel.setText( this.getStatText( KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie() ) );

		this.fullLabel.setText( KoLCharacter.getFullness() + " / " + KoLCharacter.getFullnessLimit() );
		this.drunkLabel.setText( KoLCharacter.getInebriety() + " / " + KoLCharacter.getInebrietyLimit() );
		this.spleenLabel.setText( KoLCharacter.getSpleenUse() + " / " + KoLCharacter.getSpleenLimit() );

		this.hpLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
		this.mpLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
		this.meatLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
		this.advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

		this.mlLabel.setText( KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
		this.encLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
		this.initLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
		this.expLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
		this.meatDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
		this.itemDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );
		int hobo = KoLCharacter.getHoboPower();
		if ( hobo != 0 )
		{
			this.hoboLabel.setText( "Hobo: " );
			this.hoboPowerLabel.setText( KoLConstants.MODIFIER_FORMAT.format( hobo ) );
		}
		else
		{
			this.hoboLabel.setText( "" );
			this.hoboPowerLabel.setText( "" );
		}

		int currentLevel = KoLCharacter.calculateLastLevel();
		int nextLevel = KoLCharacter.calculateNextLevel();
		int totalPrime = KoLCharacter.getTotalPrime();

		this.levelMeter.setMaximum( nextLevel - currentLevel );
		this.levelMeter.setValue( totalPrime - currentLevel );
		this.levelMeter.setString( " " );

		this.levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + "&nbsp;&nbsp;<br>&nbsp;&nbsp;(" + KoLConstants.COMMA_FORMAT.format( nextLevel - totalPrime ) + " subpoints needed)&nbsp;&nbsp;</html>" );

		FamiliarData familiar = KoLCharacter.getFamiliar();
		int id = familiar == null ? -1 : familiar.getId();

		if ( id == -1 )
		{
			this.familiarLabel.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
			this.familiarLabel.setText( "0 lbs." );
		}
		else
		{
			this.familiarLabel.setIcon( FamiliarDatabase.getFamiliarImage( id ) );
			this.familiarLabel.setText( familiar.getModifiedWeight() + ( familiar.getModifiedWeight() == 1 ? " lb." : " lbs." ) );
		}
		this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
		this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
	}
}
