/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
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
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.button.RequestButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;

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
	private JLabel beeLabel, beeosityLabel;
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
		labelPanel.add( new JLabel( KoLCharacter.inZombiecore() ? "Horde: " : "   MP: ", JLabel.RIGHT ) );
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
		panels[ panelCount ].addMouseListener( new FamPopListener() );

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
		panels[ panelCount ].add( this.beeLabel = new JLabel( " ", JLabel.RIGHT ) );
		panels[ panelCount ].add( this.beeosityLabel = new JLabel( " ", JLabel.LEFT ) );
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

		compactContainer.add( Box.createHorizontalStrut( 110 ) );

		JPanel compactCard = new JPanel( new CardLayout( 8, 8 ) );
		compactCard.setOpaque( false );
		compactCard.add( compactContainer, "" );

		JPanel refreshPanel = new JPanel();
		refreshPanel.setOpaque( false );
		refreshPanel.add( new RequestButton( "Refresh Status", "refresh.gif", new ApiRequest() ) );

		this.add( refreshPanel, BorderLayout.SOUTH );
		this.add( compactCard, BorderLayout.NORTH );

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
		this.beeosityLabel.setForeground( Color.BLACK );
	}

	private class ModPopListener
		extends MouseAdapter
	{
		@Override
		public void mousePressed( MouseEvent e )
		{
			CompactSidePane.this.modPopup.show( e.getComponent(),
				e.getX(), e.getY() );
		}
	}

	private class FamPopListener
		extends MouseAdapter
	{
		@Override
		public void mousePressed( MouseEvent e )
		{
			JPopupMenu famPopup = new JPopupMenu();
			if ( KoLCharacter.inAxecore() )
			{
				this.addInstruments( famPopup );
			}
			else
			{
				this.addFamiliars( famPopup );
			}

			famPopup.show( e.getComponent(), e.getX(), e.getY() );
		}

		private void addInstruments( JPopupMenu famPopup )
		{
			AdventureResult item = CharPaneRequest.SACKBUT;
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				famPopup.add( new InstrumentMenuItem( item ) );
			}
			item = CharPaneRequest.CRUMHORN;
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				famPopup.add( new InstrumentMenuItem( item ) );
			}
			item = CharPaneRequest.LUTE;
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				famPopup.add( new InstrumentMenuItem( item ) );
			}
		}

		private void addFamiliars( JPopupMenu famPopup )
		{
			JMenu stat = new JMenu( "statgain" );
			JMenu item = new JMenu( "itemdrop" );
			JMenu meat = new JMenu( "meatdrop" );
			JMenu combat = new JMenu( "combat" );
			JMenu other = new JMenu( "other" );
			String custom[] = new String[9];
			JMenu customMenu[] = new JMenu[9];
			for ( int i = 0; i < 9; ++i )
			{
				String pref = Preferences.getString( "familiarCategory" + (i + 1) );
				if ( pref.length() > 0 )
				{
					custom[ i ] = pref.toLowerCase();
					customMenu[ i ] = new JMenu( pref.split( "\\|", 2 )[ 0 ] );
				}
			}

			Iterator it = KoLCharacter.getFamiliarList().iterator();
			while ( it.hasNext() )
			{
				FamiliarData fam = (FamiliarData) it.next();

				// If we cannot equip this familiar for some reason, skip it.
				if ( !fam.canEquip() )
				{
					continue;
				}

				if ( fam.equals( KoLCharacter.getFamiliar() ) )
				{
					continue;	// no menu item for this one
				}
				if ( fam.getFavorite() )
				{
					famPopup.add( new FamiliarMenuItem( fam ) );
					continue;
				}

				int id = fam.getId();
				Modifiers mods = Modifiers.getModifiers( "Fam:" + fam.getRace() );
				boolean added = false;
				if ( FamiliarDatabase.isVolleyType( id ) ||
					FamiliarDatabase.isSombreroType( id ) ||
					(mods != null && mods.get( Modifiers.VOLLEYBALL_WEIGHT ) != 0.0) )
				{
					stat.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isFairyType( id ) )
				{
					item.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isMeatDropType( id ) )
				{
					meat.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( fam.isCombatFamiliar() )
				{
					combat.add( new FamiliarMenuItem( fam ) );
					added = true;
				}

				String key = "|" + fam.getRace().toLowerCase();
				for ( int i = 0; i < 9; ++i )
				{
					if ( custom[ i ] != null && custom[ i ].indexOf( key ) != -1 )
					{
						customMenu[ i ].add( new FamiliarMenuItem( fam ) );
						added = true;
					}
				}

				if ( !added )
				{
					other.add( new FamiliarMenuItem( fam ) );
				}
			}

			if ( stat.getMenuComponentCount() > 0 )
			{
				famPopup.add( stat );
			}
			if ( item.getMenuComponentCount() > 0 )
			{
				famPopup.add( item );
			}
			if ( meat.getMenuComponentCount() > 0 )
			{
				famPopup.add( meat );
			}
			if ( combat.getMenuComponentCount() > 0 )
			{
				famPopup.add( combat );
			}

			for ( int i = 0; i < 9; ++i )
			{
				JMenu menu = customMenu[ i ];

				if ( menu != null && menu.getMenuComponentCount() > 0 )
				{
					famPopup.add( menu );
				}
			}

			famPopup.add( other );
		}
	}

	private static class FamiliarMenuItem
		extends ThreadedMenuItem
	{
		public FamiliarMenuItem( final FamiliarData fam )
		{
			super( fam.getRace(), new FamiliarListener( fam ) );

			if ( fam.getFavorite() )
			{
				ImageIcon icon = FamiliarDatabase.getFamiliarImage( fam.getId() );
				this.setIcon( icon );
				icon.setImageObserver( this );
			}
		}
	}

	private static class FamiliarListener
		extends ThreadedListener
	{
		private FamiliarData familiar;

		public FamiliarListener( FamiliarData familiar )
		{
			this.familiar = familiar;
		}

		@Override
		protected void execute()
		{
			CommandDisplayFrame.executeCommand( "familiar " + this.familiar.getRace() );
		}
	}

	private static class InstrumentMenuItem
		extends ThreadedMenuItem
	{
		public InstrumentMenuItem( final AdventureResult item )
		{
			super( item.getName(), new UseItemListener( item ) );
			ImageIcon icon = ItemDatabase.getItemImage( item.getItemId() );
			this.setIcon( icon );
			icon.setImageObserver( this );
		}
	}

	private static class UseItemListener
		extends ThreadedListener
	{
		private String command;

		public UseItemListener( AdventureResult item )
		{
			this.command = "use " + item.getName();
		}

		@Override
		protected void execute()
		{
			CommandDisplayFrame.executeCommand( this.command );
		}
	}

	public String getStatText( final int adjusted, final int base )
	{
		return adjusted == base ? "<html>" + Integer.toString( base ) : adjusted > base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" : "<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
	}

	public void run()
	{
		this.levelLabel.setText( "Level " + KoLCharacter.getLevel() );

		if ( CharPaneRequest.inValhalla() )
		{
			this.roninLabel.setText( "(Valhalla)" );
		}
		else if ( KoLCharacter.inBadMoon() )
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
		this.mpLabel.setText( 
			KoLCharacter.inZombiecore() ?
			KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) :
			KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
		this.meatLabel.setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
                this.meatLabel.setToolTipText( "Closet: " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) );
		this.advLabel.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

		this.mlLabel.setText( KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
		this.encLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
		this.initLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
		this.expLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
		this.meatDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
		this.itemDropLabel.setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );
		int hobo = KoLCharacter.getHoboPower();
		int clown = KoLCharacter.getClownosity();
		int rave = KoLCharacter.currentBitmapModifier( Modifiers.RAVEOSITY );
		if ( hobo != 0 )
		{
			this.hoboLabel.setText( "Hobo: " );
			this.hoboPowerLabel.setText( KoLConstants.MODIFIER_FORMAT.format( hobo ) );
		}
		else if ( rave != 0 )
		{
			this.hoboLabel.setText( "Rave: " );
			this.hoboPowerLabel.setText( rave + "/7" );
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

		if ( KoLCharacter.inBeecore() )
		{
			int bee = KoLCharacter.getBeeosity();
			this.beeLabel.setText( "Bees: " );
			this.beeosityLabel.setText( bee + "" );
		}
		else
		{
			this.beeLabel.setText( "" );
			this.beeosityLabel.setText( "" );
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
			Math.max( 0.0, ( (double) Math.sqrt( Math.min( 10000.0, KoLCharacter.getDamageAbsorption() * 10.0 ) ) - 10.0 ) ) ) );
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
		double food = KoLCharacter.currentNumericModifier( Modifiers.FOODDROP );
		double booze = KoLCharacter.currentNumericModifier( Modifiers.BOOZEDROP );
		double hat = KoLCharacter.currentNumericModifier( Modifiers.HATDROP );
		double weapon = KoLCharacter.currentNumericModifier( Modifiers.WEAPONDROP );
		double offhand = KoLCharacter.currentNumericModifier( Modifiers.OFFHANDDROP );
		double shirt = KoLCharacter.currentNumericModifier( Modifiers.SHIRTDROP );
		double pants = KoLCharacter.currentNumericModifier( Modifiers.PANTSDROP );
		double acc = KoLCharacter.currentNumericModifier( Modifiers.ACCESSORYDROP );
		if ( food != 0 || booze != 0 || hat != 0 || weapon != 0 ||
			offhand != 0 || shirt != 0 || pants != 0 || acc != 0 )
		{
			buf.append( "<tr><td colspan=4>Special drops:" );
			if ( food != 0 )
			{
				buf.append( " Food " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( food ) );
				buf.append( '%' );
			}
			if ( booze != 0 )
			{
				buf.append( " Booze " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( booze ) );
				buf.append( '%' );
			}
			if ( hat != 0 )
			{
				buf.append( " Hat " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( hat ) );
				buf.append( '%' );
			}
			if ( weapon != 0 )
			{
				buf.append( " Weapon " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( weapon ) );
				buf.append( '%' );
			}
			if ( offhand != 0 )
			{
				buf.append( " Offhand " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( offhand ) );
				buf.append( '%' );
			}
			if ( shirt != 0 )
			{
				buf.append( " Shirt " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( shirt ) );
				buf.append( '%' );
			}
			if ( pants != 0 )
			{
				buf.append( " Pants " );
				buf.append( KoLConstants.MODIFIER_FORMAT.format( pants ) );
				buf.append( '%' );
			}
			if ( acc != 0 )
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

		if ( KoLCharacter.inAxecore() )
		{
			AdventureResult item = KoLCharacter.getCurrentInstrument();
			if ( item == null )
			{
				ImageIcon icon = FamiliarDatabase.getNoFamiliarImage();
				this.familiarLabel.setIcon( icon );
			}
			else
			{
				ImageIcon icon = ItemDatabase.getItemImage( item.getItemId() );
				this.familiarLabel.setIcon( icon );
				icon.setImageObserver( this );
			}
			int level = KoLCharacter.getMinstrelLevel();
			this.familiarLabel.setText( "Level " + level );
		}
		else
		{
			FamiliarData current = KoLCharacter.getFamiliar();
			FamiliarData effective = KoLCharacter.getEffectiveFamiliar();
			int id = effective == null ? -1 : effective.getId();

			if ( id == -1 )
			{
				ImageIcon icon = FamiliarDatabase.getNoFamiliarImage();
				this.familiarLabel.setIcon( icon );
				this.familiarLabel.setText( "0 lbs." );
			}
			else
			{
				StringBuffer anno = CharPaneDecorator.getFamiliarAnnotation();
				ImageIcon icon = FamiliarDatabase.getCurrentFamiliarImage();
				this.familiarLabel.setIcon( icon );
				icon.setImageObserver( this );
				int weight = current.getModifiedWeight();
				this.familiarLabel.setText( "<HTML><center>" + weight +
							    ( weight == 1 ? " lb." : " lbs." ) +
							    ( anno == null ? "" : ", " + anno.toString() ) + "</center></HTML>" );
			}
		}

		this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
		this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
	}

	private void addElement( StringBuffer buf, String name, int dmgModifier )
	{
		double wdmg = KoLCharacter.currentNumericModifier( dmgModifier );
		double sdmg = KoLCharacter.currentNumericModifier(
			dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_SPELL_DAMAGE );
		int resist = (int) KoLCharacter.currentNumericModifier(
			dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_RESISTANCE );
		if ( wdmg == 0.0 && sdmg == 0.0 && resist == 0 )
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
		double percent = KoLCharacter.elementalResistanceByLevel( resist, false );
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
