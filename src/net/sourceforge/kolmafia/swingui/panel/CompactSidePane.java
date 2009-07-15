/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.swingui.button.RequestButton;
import net.sourceforge.kolmafia.swingui.widget.UnanimatedLabel;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;

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
	private JPopupMenu modPopup;
	private JLabel modPopLabel;
	
	private static final AdventureResult CLUMSY = new AdventureResult( "Clumsy", 1, true );
	private static final AdventureResult SLIMED = new AdventureResult( "Coated in Slime", 1, true );

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
		this.modPopLabel = new JLabel();
		this.modPopup = new JPopupMenu();
		this.modPopup.insert( this.modPopLabel, 0 );
		panels[ panelCount ].addMouseListener( new ModPopListener() );	

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
	
	private class ModPopListener
		extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{
			CompactSidePane.this.modPopup.show( e.getComponent(),
				e.getX(), e.getY() );
		}
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

		this.mcdLabel.setText( "ML @ " + KoLCharacter.getMindControlLevel() );

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
		int clown = KoLCharacter.getClownosity();
		if ( hobo != 0 )
		{
			this.hoboLabel.setText( "Hobo: " );
			this.hoboPowerLabel.setText( KoLConstants.MODIFIER_FORMAT.format( hobo ) );
		}
		else if ( clown != 0 )
		{
			this.hoboLabel.setText( "Clown: " );
			this.hoboPowerLabel.setText( clown + "/4" );
		}
		else
		{
			this.hoboLabel.setText( "" );
			this.hoboPowerLabel.setText( "" );
		}
		
		StringBuffer buf = new StringBuffer( "<html><table border=1>" );
		int[] predicted = KoLCharacter.getCurrentModifiers().predict();
		int mus = Math.max( 1, predicted[ Modifiers.BUFFED_MUS ] );
		int mys = Math.max( 1, predicted[ Modifiers.BUFFED_MYS ] );
		int mox = Math.max( 1, predicted[ Modifiers.BUFFED_MOX ] );
		int dmus = KoLCharacter.getAdjustedMuscle() - mus;
		int dmys = KoLCharacter.getAdjustedMysticality() - mys;
		int dmox = KoLCharacter.getAdjustedMoxie() - mox;
		if ( dmus != 0 || dmys != 0 || dmox != 0 )
		{
			buf.append( "<tr><td colspan=4>Predicted: Mus " );
			buf.append( mus );
			buf.append( " (" );
			buf.append( KoLConstants.MODIFIER_FORMAT.format( dmus ) );
			buf.append( "), Mys " );
			buf.append( mys );
			buf.append( " (" );
			buf.append( KoLConstants.MODIFIER_FORMAT.format( dmys ) );
			buf.append( "), Mox " );
			buf.append( mox );
			buf.append( " (" );
			buf.append( KoLConstants.MODIFIER_FORMAT.format( dmox ) );
			buf.append( ")</td></tr>" );
		}
		int hp = Math.max( 1, predicted[ Modifiers.BUFFED_HP ] );
		int mp = Math.max( 1, predicted[ Modifiers.BUFFED_MP ] );
		int dhp = KoLCharacter.getMaximumHP() - hp;
		int dmp = KoLCharacter.getMaximumMP() - mp;
		if ( dhp != 0 || dmp != 0 )
		{
			buf.append( "<tr><td colspan=4>Predicted: Max HP " );
			buf.append( hp );
			buf.append( " (" );
			buf.append( KoLConstants.MODIFIER_FORMAT.format( dhp ) );
			buf.append( "), Max MP " );
			buf.append( mp );
			buf.append( " (" );
			buf.append( KoLConstants.MODIFIER_FORMAT.format( dmp ) );
			buf.append( ")</td></tr>" );
		}
	
		buf.append( "<tr><td></td><td>Damage</td><td>Spell dmg</td><td>Resistance</td></tr>" );
		this.addElement( buf, "Hot", Modifiers.HOT_DAMAGE );
		this.addElement( buf, "Cold", Modifiers.COLD_DAMAGE );
		this.addElement( buf, "Stench", Modifiers.STENCH_DAMAGE );
		this.addElement( buf, "Spooky", Modifiers.SPOOKY_DAMAGE );
		this.addElement( buf, "Sleaze", Modifiers.SLEAZE_DAMAGE );
		this.addSlime( buf );
		buf.append( "<tr><td>Weapon</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.WEAPON_DAMAGE ) ) );
		buf.append( "<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.WEAPON_DAMAGE_PCT ) ) );
		buf.append( "%</td><td rowspan=2>General<br>spell dmg:<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.SPELL_DAMAGE ) ) );
		buf.append( "<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.SPELL_DAMAGE_PCT ) ) );
		buf.append( "%</td><td rowspan=2>DA: " );
		buf.append( KoLConstants.COMMA_FORMAT.format(
			KoLCharacter.getDamageAbsorption() ) );
		buf.append( "<br>(" );
		buf.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
			Math.max( 0.0f, ( (float) Math.sqrt( Math.min( 10000.0f, KoLCharacter.getDamageAbsorption() * 10.0f ) ) - 10.0f ) ) ) );
		buf.append( "%)<br>DR: " );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.getDamageReduction() ) );
		buf.append( "</td></tr><tr><td>Ranged</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.RANGED_DAMAGE ) ) );
		buf.append( "<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.RANGED_DAMAGE_PCT ) ) );
		buf.append( "%</td></tr><tr><td>Critical</td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.CRITICAL ) ) );
		buf.append( " X<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.CRITICAL_PCT ) ) );
		buf.append( "%</td><td rowspan=2>MP cost:<br>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.getManaCostAdjustment() ) );
		int hpmin = (int) KoLCharacter.currentNumericModifier( Modifiers.HP_REGEN_MIN );
		int hpmax = (int) KoLCharacter.currentNumericModifier( Modifiers.HP_REGEN_MAX );
		int mpmin = (int) KoLCharacter.currentNumericModifier( Modifiers.MP_REGEN_MIN );
		int mpmax = (int) KoLCharacter.currentNumericModifier( Modifiers.MP_REGEN_MAX );
		if ( hpmax != 0 || mpmax != 0 )
		{
			buf.append( "<br>Regenerate:<br>HP " );
			buf.append( hpmin );
			if ( hpmin != hpmax )
			{
				buf.append( "-" );
				buf.append( hpmax );
			}
			buf.append( "<br>MP " );
			buf.append( mpmin );
			if ( mpmin != mpmax )
			{
				buf.append( "-" );
				buf.append( mpmax );
			}
		}
		buf.append( "</td><td rowspan=2>Rollover:<br>Adv " );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.ADVENTURES ) +
			Preferences.getInteger( "extraRolloverAdventures" ) ) );
		buf.append( "<br>PvP " );
		buf.append( KoLConstants.MODIFIER_FORMAT.format(
			KoLCharacter.currentNumericModifier( Modifiers.PVP_FIGHTS ) ) );
		buf.append( "<br>HP ~" );
		buf.append( KoLCharacter.getRestingHP() );
		buf.append( "<br>MP " );
		buf.append( KoLCharacter.getRestingMP() );
		buf.append( "</td></tr><tr><td>Fumble</td><td>" );
		if ( KoLConstants.activeEffects.contains( CompactSidePane.CLUMSY ) )
		{
			buf.append( "always" );
		}
		else if ( KoLCharacter.currentBooleanModifier( Modifiers.NEVER_FUMBLE ) )
		{
			buf.append( "never" );
		}
		else
		{
			buf.append( KoLConstants.MODIFIER_FORMAT.format(
				KoLCharacter.currentNumericModifier( Modifiers.FUMBLE ) ) );
			buf.append( " X" );
		}
		buf.append( "</td></tr>" );
		float food = KoLCharacter.currentNumericModifier( Modifiers.FOODDROP );
		float booze = KoLCharacter.currentNumericModifier( Modifiers.BOOZEDROP );
		float hat = KoLCharacter.currentNumericModifier( Modifiers.HATDROP );
		float weapon = KoLCharacter.currentNumericModifier( Modifiers.WEAPONDROP );
		float offhand = KoLCharacter.currentNumericModifier( Modifiers.OFFHANDDROP );
		float shirt = KoLCharacter.currentNumericModifier( Modifiers.SHIRTDROP );
		float pants = KoLCharacter.currentNumericModifier( Modifiers.PANTSDROP );
		float acc = KoLCharacter.currentNumericModifier( Modifiers.ACCESSORYDROP );
		if ( food != 0f || booze != 0f || hat != 0f || weapon != 0f ||
			offhand != 0f || shirt != 0f || pants != 0f || acc != 0f )
		{
			buf.append( "<tr><td colspan=4>Special drops:" );
			if ( food != 0f )
			{
				buf.append( " Food " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( food ) );
				buf.append( '%' );
			}
			if ( booze != 0f )
			{
				buf.append( " Booze " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( booze ) );
				buf.append( '%' );
			}
			if ( hat != 0f )
			{
				buf.append( " Hat " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( hat ) );
				buf.append( '%' );
			}
			if ( weapon != 0f )
			{
				buf.append( " Weapon " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( weapon ) );
				buf.append( '%' );
			}
			if ( offhand != 0f )
			{
				buf.append( " Offhand " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( offhand ) );
				buf.append( '%' );
			}
			if ( shirt != 0f )
			{
				buf.append( " Shirt " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( shirt ) );
				buf.append( '%' );
			}
			if ( pants != 0f )
			{
				buf.append( " Pants " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( pants ) );
				buf.append( '%' );
			}
			if ( acc != 0f )
			{
				buf.append( " Accessory " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( acc ) );
				buf.append( '%' );
			}
			buf.append( "</td></tr>" );
		}
		buf.append( "</table></html>" );
		try
		{
			this.modPopLabel.setText( buf.toString() );
		}
		catch ( Exception e )
		{
			// Ignore errors - there seems to be a Java bug that
			// occasionally gets triggered during the setText().
		}

		long currentLevel = KoLCharacter.calculateLastLevel();
		long nextLevel = KoLCharacter.calculateNextLevel();
		long totalPrime = KoLCharacter.getTotalPrime();

		this.levelMeter.setMaximum( (int) (nextLevel - currentLevel) );
		this.levelMeter.setValue( (int) (totalPrime - currentLevel) );
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
			String anno = CharPaneDecorator.getFamiliarAnnotation();
			this.familiarLabel.setIcon( FamiliarDatabase.getFamiliarImage( id ) );
			this.familiarLabel.setText( familiar.getModifiedWeight() +
				( familiar.getModifiedWeight() == 1 ? " lb." : " lbs." ) +
				( anno == null ? "" : ", " + anno ) );
		}
		this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
		this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
	}
	
	private int predictStat( int base, int stat_pct, int stat )
	{
		int rv = (int) (Math.ceil( base * (100.0 + KoLCharacter.currentNumericModifier( stat_pct )) / 100.0 ) + KoLCharacter.currentNumericModifier( stat ));
		return Math.max( 1, rv );
	}
	
	private void addElement( StringBuffer buf, String name, int dmgModifier )
	{
		float wdmg = KoLCharacter.currentNumericModifier( dmgModifier );
		float sdmg = KoLCharacter.currentNumericModifier(
			dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_SPELL_DAMAGE );
		int resist = (int) KoLCharacter.currentNumericModifier(
			dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_RESISTANCE );
		if ( wdmg == 0.0f && sdmg == 0.0f && resist == 0 )
		{
			return;	// skip this row entirely, it's all zeros
		}
		buf.append( "<tr><td>" );
		buf.append( name );
		buf.append( "</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format( wdmg ) );
		buf.append( "</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format( sdmg ) );
		buf.append( "</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format( resist ) );
		buf.append( " (" );
		buf.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
			KoLCharacter.elementalResistanceByLevel( resist ) ) );
		buf.append( "%)</td></tr>" );
	}

	private void addSlime( StringBuffer buf )
	{
		int resist = (int) KoLCharacter.currentNumericModifier(
			Modifiers.SLIME_RESISTANCE );
		float percent = KoLCharacter.elementalResistanceByLevel( resist, false );
		int turns = CompactSidePane.SLIMED.getCount( KoLConstants.activeEffects );
		if ( resist == 0 && turns == 0 )
		{
			return;	// skip this row entirely, it's all zeros
		}
		buf.append( "<tr><td>Slime</td><td colspan=2>" );
		if ( turns > 0 )
		{
			buf.append( "Expected dmg " );
			buf.append( KoLConstants.COMMA_FORMAT.format( Math.ceil( Math.pow( Math.max( 0, 11 - turns ), 2.727 ) * ( 100.0 - percent ) * KoLCharacter.getMaximumHP() / 10000.0 ) ) );
		}
		buf.append( "</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format( resist ) );
		buf.append( " (" );
		buf.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( percent ) );
		buf.append( "%)</td></tr>" );
	}
}
