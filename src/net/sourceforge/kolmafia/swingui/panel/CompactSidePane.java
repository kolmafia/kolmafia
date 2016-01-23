/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;

import net.sourceforge.kolmafia.swingui.widget.UnanimatedLabel;

import net.sourceforge.kolmafia.utilities.FileUtilities;

import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class CompactSidePane
	extends JPanel
	implements Runnable
{
	private final JPanel levelPanel;
	private final JProgressBar levelMeter;
	private final JLabel levelLabel, roninLabel, mcdLabel;
	private final int STAT_LABELS = 3;
	private final JLabel[] statLabel = new JLabel[ STAT_LABELS ];
	private final JLabel[] statValueLabel = new JLabel[ STAT_LABELS ];
	private final int STATUS_LABELS = 6;
	private final JLabel[] statusLabel = new JLabel[ STATUS_LABELS ];
	private final JLabel[] statusValueLabel = new JLabel[ STATUS_LABELS ];
	private final int CONSUMPTION_LABELS = 3;
	private final JLabel[] consumptionLabel = new JLabel[ CONSUMPTION_LABELS ];
	private final JLabel[] consumptionValueLabel = new JLabel[ CONSUMPTION_LABELS ];
	private final JLabel familiarLabel;
	private final int BONUS_LABELS = 10;
	private final JLabel[] bonusLabel = new JLabel[ BONUS_LABELS ];
	private final JLabel[] bonusValueLabel = new JLabel[ BONUS_LABELS ];
	private final JPopupMenu modPopup;
	private final JLabel modPopLabel;

	// Sneaky Pete's Motorcycle
	private final JPopupMenu motPopup;
	private final JLabel motPopLabel;

	private static final AdventureResult CLUMSY = EffectPool.get( EffectPool.CLUMSY );
	private static final AdventureResult SLIMED = EffectPool.get( EffectPool.COATED_IN_SLIME );

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

		labelPanel = new JPanel( new GridLayout( this.STAT_LABELS, 1 ) );
		labelPanel.setOpaque( false );

		for ( int i = 0; i < this.STAT_LABELS ; i++ )
		{
			labelPanel.add( this.statLabel[ i ] = new JLabel( " ", JLabel.RIGHT ) );
		}

		valuePanel = new JPanel( new GridLayout( this.STAT_LABELS, 1 ) );
		valuePanel.setOpaque( false );

		for ( int i = 0; i < this.STAT_LABELS ; i++ )
		{
			valuePanel.add( this.statValueLabel[ i ] = new JLabel( " ", JLabel.LEFT ) );
		}

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		panels[ panelCount ].setOpaque( false );

		labelPanel = new JPanel( new GridLayout( this.STATUS_LABELS, 1 ) );
		labelPanel.setOpaque( false );

		for ( int i = 0; i < this.STATUS_LABELS ; i++ )
		{
			labelPanel.add( this.statusLabel[ i ] = new JLabel( " ", JLabel.RIGHT ) );
		}

		valuePanel = new JPanel( new GridLayout( this.STATUS_LABELS, 1 ) );
		valuePanel.setOpaque( false );

		for ( int i = 0; i < this.STATUS_LABELS ; i++ )
		{
			valuePanel.add( this.statusValueLabel[ i ] = new JLabel( " ", JLabel.LEFT ) );
		}

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new BorderLayout() );
		panels[ panelCount ].setOpaque( false );

		labelPanel = new JPanel( new GridLayout( this.CONSUMPTION_LABELS, 1 ) );
		labelPanel.setOpaque( false );

		for ( int i = 0; i < this.CONSUMPTION_LABELS ; i++ )
		{
			labelPanel.add( this.consumptionLabel[ i ] = new JLabel( " ", JLabel.RIGHT ) );
		}

		valuePanel = new JPanel( new GridLayout( this.CONSUMPTION_LABELS, 1 ) );
		valuePanel.setOpaque( false );

		for ( int i = 0; i < this.CONSUMPTION_LABELS ; i++ )
		{
			valuePanel.add( this.consumptionValueLabel[ i ] = new JLabel( " ", JLabel.LEFT ) );
		}

		panels[ panelCount ].add( labelPanel, BorderLayout.WEST );
		panels[ panelCount ].add( valuePanel, BorderLayout.CENTER );

		panels[ ++panelCount ] = new JPanel( new GridLayout( 1, 1 ) );
		panels[ panelCount ].add( this.familiarLabel = new UnanimatedLabel() );
		panels[ panelCount ].addMouseListener( new FamPopListener() );

		// Make a popup label for Sneaky Pete's motorcycle. Clicking on
		// the motorcycle image (which replaces the familiar icon)
		// activates it.
		this.motPopLabel = new JLabel();
		this.motPopup = new JPopupMenu();
		this.motPopup.insert( this.motPopLabel, 0 );

		panels[ ++panelCount ] = new JPanel( new GridLayout( this.BONUS_LABELS , 2 ) );
		
		for ( int i = 0; i < this.BONUS_LABELS ; i++ )
		{
			panels[ panelCount ].add( this.bonusLabel[ i ] = new JLabel( " ", JLabel.RIGHT ) );
			panels[ panelCount ].add( this.bonusValueLabel[ i ] = new JLabel( " ", JLabel.LEFT ) );
		}

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
		refreshPanel.add( new InvocationButton( "Refresh Status", "refresh.gif", ApiRequest.class, "updateStatus" ) );

		this.add( refreshPanel, BorderLayout.SOUTH );
		this.add( compactCard, BorderLayout.NORTH );

		this.levelLabel.setForeground( Color.BLACK );
		this.roninLabel.setForeground( Color.BLACK );
		this.mcdLabel.setForeground( Color.BLACK );
		for ( int i = 0; i < this.STAT_LABELS ; i++ )
		{
			this.statLabel[ i ].setForeground( Color.BLACK );
			this.statValueLabel[ i ].setForeground( Color.BLACK );
		}
		for ( int i = 0; i < this.STATUS_LABELS ; i++ )
		{
			this.statusLabel[ i ].setForeground( Color.BLACK );
			this.statusValueLabel[ i ].setForeground( Color.BLACK );
		}
		for ( int i = 0; i < this.CONSUMPTION_LABELS ; i++ )
		{
			this.consumptionLabel[ i ].setForeground( Color.BLACK );
			this.consumptionValueLabel[ i ].setForeground( Color.BLACK );
		}
		this.familiarLabel.setForeground( Color.BLACK );
		for ( int i = 0; i < this.BONUS_LABELS ; i++ )
		{
			this.bonusLabel[ i ].setForeground( Color.BLACK );
			this.bonusValueLabel[ i ].setForeground( Color.BLACK );
		}
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
			if ( KoLCharacter.isSneakyPete() )
			{
				CompactSidePane.this.motPopup.show( e.getComponent(),
								    e.getX(), e.getY() );
				return;
			}

			JPopupMenu famPopup = new JPopupMenu();
			if ( KoLCharacter.inAxecore() )
			{
				this.addInstruments( famPopup );
			}
			else if ( KoLCharacter.isEd() )
			{
				this.addServants( famPopup );
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

		private void addServants( JPopupMenu famPopup )
		{
			EdServantData current = EdServantData.currentServant();
			for ( EdServantData servant : EdServantData.getServants() )
			{
				if ( servant != current && servant != EdServantData.NO_SERVANT )
				{
					famPopup.add( new ServantMenuItem( servant ) );
				}
			}
		}

		private void addFamiliars( JPopupMenu famPopup )
		{
			JMenu stat = new JMenu( "stat gain" );
			JMenu item = new JMenu( "item drop" );
			JMenu meat = new JMenu( "meat drop" );
			JMenu drops = new JMenu( "special drops" );

			// Combat submenus
			JMenu combat0 = new JMenu( "physical only" );
			JMenu combat1 = new JMenu( "elemental only" );
			JMenu combat01 = new JMenu( "physical and elemental" );
			JMenu block = new JMenu( "block" );
			JMenu delevel = new JMenu( "delevel" );
			JMenu hp0 = new JMenu( "restore HP" );
			JMenu mp0 = new JMenu( "restore MP" );
			JMenu other0 = new JMenu( "anything else" );

			// After Combat submenu
			JMenu hp1 = new JMenu( "restore HP" );
			JMenu mp1 = new JMenu( "restore MP" );
			JMenu other1 = new JMenu( "anything else" );

			JMenu passive = new JMenu( "passive" );
			JMenu underwater = new JMenu( "underwater" );
			JMenu variable = new JMenu( "configurable" );

			// None of the above
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
			for ( FamiliarData fam : KoLCharacter.getFamiliarList() )
			{
				if ( fam == FamiliarData.NO_FAMILIAR )
				{
					continue;	// no menu item for this one
				}

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
				Modifiers mods = Modifiers.getModifiers( "Familiar", fam.getRace() );
				boolean added = false;

				// Stat Gain
				if ( FamiliarDatabase.isVolleyType( id ) ||
				     FamiliarDatabase.isSombreroType( id ) ||
				     (mods != null && mods.get( Modifiers.VOLLEYBALL_WEIGHT ) != 0.0) )
				{
					stat.add( new FamiliarMenuItem( fam ) );
					added = true;
				}

				// Item Drop
				if ( FamiliarDatabase.isFairyType( id ) )
				{
					item.add( new FamiliarMenuItem( fam ) );
					added = true;
				}

				// Meat Drop
				if ( FamiliarDatabase.isMeatDropType( id ) )
				{
					meat.add( new FamiliarMenuItem( fam ) );
					added = true;
				}

				// Special drops
				if ( fam.hasDrop() )
				{
					drops.add( new FamiliarMenuItem( fam ) );
					added = true;
				}

				// Combat submenus
				boolean is0 = FamiliarDatabase.isCombat0Type( id );
				boolean is1 = FamiliarDatabase.isCombat1Type( id );
				
				if ( is0 && !is1  )
				{
					combat0.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( is1 && !is0 )
				{
					combat1.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( is0 && is1 )
				{
					combat01.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isBlockType( id ) )
				{
					block.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isDelevelType( id ) )
				{
					delevel.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isHp0Type( id ) )
				{
					hp0.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isMp0Type( id ) )
				{
					mp0.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isOther0Type( id ) )
				{
					other0.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isHp1Type( id ) )
				{
					hp1.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isMp1Type( id ) )
				{
					mp1.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isOther1Type( id ) )
				{
					other1.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isPassiveType( id ) )
				{
					passive.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isUnderwaterType( id ) )
				{
					underwater.add( new FamiliarMenuItem( fam ) );
					added = true;
				}
				if ( FamiliarDatabase.isVariableType( id ) )
				{
					variable.add( new FamiliarMenuItem( fam ) );
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

			if ( drops.getMenuComponentCount() > 0 )
			{
				famPopup.add( drops );
			}

			if ( combat0.getMenuComponentCount() > 0 ||
			     combat1.getMenuComponentCount() > 0 ||
			     combat01.getMenuComponentCount() > 0 ||
			     block.getMenuComponentCount() > 0 ||
			     delevel.getMenuComponentCount() > 0 ||
			     hp0.getMenuComponentCount() > 0 ||
			     mp0.getMenuComponentCount() > 0 ||
			     other0.getMenuComponentCount() > 0 )
			{
				JMenu combat = new JMenu( "combat" );

				if ( combat0.getMenuComponentCount() > 0 )
				{
					combat.add( combat0 );
				}
				if ( combat1.getMenuComponentCount() > 0 )
				{
					combat.add( combat1 );
				}
				if ( combat01.getMenuComponentCount() > 0 )
				{
					combat.add( combat01 );
				}
				if ( block.getMenuComponentCount() > 0 )
				{
					combat.add( block );
				}
				if ( delevel.getMenuComponentCount() > 0 )
				{
					combat.add( delevel );
				}
				if ( hp0.getMenuComponentCount() > 0 )
				{
					combat.add( hp0 );
				}
				if ( mp0.getMenuComponentCount() > 0 )
				{
					combat.add( mp0 );
				}
				if ( other0.getMenuComponentCount() > 0 )
				{
					combat.add( other0 );
				}

				famPopup.add( combat );
			}

			if ( hp1.getMenuComponentCount() > 0 ||
			     mp1.getMenuComponentCount() > 0 ||
			     other1.getMenuComponentCount() > 0 )
			{
				JMenu aftercombat = new JMenu( "after combat" );

				if ( hp1.getMenuComponentCount() > 0 )
				{
					aftercombat.add( hp1 );
				}
				if ( mp1.getMenuComponentCount() > 0 )
				{
					aftercombat.add( mp1 );
				}
				if ( other1.getMenuComponentCount() > 0 )
				{
					aftercombat.add( other1 );
				}

				famPopup.add( aftercombat );
			}

			if ( passive.getMenuComponentCount() > 0 )
			{
				famPopup.add( passive );
			}
			if ( underwater.getMenuComponentCount() > 0 )
			{
				famPopup.add( underwater );
			}
			if ( variable.getMenuComponentCount() > 0 )
			{
				famPopup.add( variable );
			}

			for ( int i = 0; i < 9; ++i )
			{
				JMenu menu = customMenu[ i ];

				if ( menu != null && menu.getMenuComponentCount() > 0 )
				{
					famPopup.add( menu );
				}
			}

			if ( other.getMenuComponentCount() > 0 )
			{
				famPopup.add( other );
			}
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
		private final FamiliarData familiar;

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
		private final String command;

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

	private static class ServantMenuItem
		extends ThreadedMenuItem
	{
		public ServantMenuItem( final EdServantData servant )
		{
			super( servant.getType(), new ChangeServantListener( servant ) );
			ImageIcon icon = FileUtilities.downloadIcon( servant.getImage(), "itemimages", "debug.gif" );
			this.setIcon( icon );
			icon.setImageObserver( this );
		}
	}

	private static class ChangeServantListener
		extends ThreadedListener
	{
		private final EdServantData servant;

		public ChangeServantListener( EdServantData servant )
		{
			this.servant = servant;
		}

		@Override
		protected void execute()
		{
			CommandDisplayFrame.executeCommand( "servant " + this.servant.getType() );
		}
	}

	public String getStatText( final int adjusted, final int base )
	{
		return adjusted == base ? "<html>" + Integer.toString( base ) : adjusted > base ? "<html><font color=blue>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")" : "<html><font color=red>" + Integer.toString( adjusted ) + "</font> (" + Integer.toString( base ) + ")";
	}

	public void run()
	{
		String limitmode = KoLCharacter.getLimitmode();

		if ( limitmode != Limitmode.SPELUNKY )
		{
			this.levelLabel.setText( "Level " + KoLCharacter.getLevel() );
		}
		else
		{
			this.levelLabel.setText( " " );
		}

		if ( limitmode == Limitmode.SPELUNKY )
		{
			this.roninLabel.setText( "(Spelunkin')" );
		}
		else if ( CharPaneRequest.inValhalla() )
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

		if ( limitmode != Limitmode.SPELUNKY )
		{
			this.mcdLabel.setText( "ML @ " + KoLCharacter.getMindControlLevel() );
		}
		else
		{
			this.mcdLabel.setText( "" );
		}

		int count = 0;
		this.statLabel[ count ].setText( "   Mus: " );
		this.statValueLabel[ count ].setText( this.getStatText( KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle() ) );
		count++;
		if ( limitmode != Limitmode.SPELUNKY )
		{
			this.statLabel[ count ].setText( "   Mys: " );
			this.statValueLabel[ count ].setText( this.getStatText( KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality() ) );
			count++;
		}
		this.statLabel[ count ].setText( "   Mox: " );
		this.statValueLabel[ count ].setText( this.getStatText( KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie() ) );
		count++;
		for( int i = count ; i < STAT_LABELS ; i++ )
		{
			this.statLabel[ i ].setText( "" );
			this.statValueLabel[ i ].setText( "" );
		}

		count = 0;
		int limit = KoLCharacter.getFullnessLimit();
		if ( limit > 0 )
		{
			this.consumptionLabel[ count ].setText( "  Full: " );
			this.consumptionValueLabel[ count ].setText( KoLCharacter.getFullness() + " / " + limit );
			count++;
		}
		limit = KoLCharacter.getInebrietyLimit();
		if ( limit > 0 )
		{
			this.consumptionLabel[ count ].setText( " Drunk: " );
			this.consumptionValueLabel[ count ].setText( KoLCharacter.getInebriety() + " / " + limit );
			count++;
		}
		limit = KoLCharacter.getSpleenLimit();
		if ( limit > 0 )
		{
			this.consumptionLabel[ count ].setText( "Spleen: " );
			this.consumptionValueLabel[ count ].setText( KoLCharacter.getSpleenUse() + " / " + limit );
			count++;
		}
		for( int i = count ; i < CONSUMPTION_LABELS ; i++ )
		{
			this.consumptionLabel[ i ].setText( "" );
			this.consumptionValueLabel[ i ].setText( "" );
		}

		count = 0;
		this.statusLabel[ count ].setText( "    HP: " );
		this.statusValueLabel[ count ].setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
		count++;
		if ( limitmode != Limitmode.SPELUNKY )
		{
			if ( KoLCharacter.inZombiecore() )
			{
				this.statusLabel[ count ].setText( " Horde: " );
				this.statusValueLabel[ count ].setText( String.valueOf( KoLCharacter.getCurrentMP() ) );
				count++;
			}
			else
			{
				this.statusLabel[ count ].setText( "    MP: " );
				this.statusValueLabel[ count ].setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );
				count++;
			}
			if ( KoLCharacter.getFuryLimit() > 0 )
			{
				this.statusLabel[ count ].setText( "  Fury: " );
				this.statusValueLabel[ count ].setText( KoLCharacter.getFury() + " / " + KoLCharacter.getFuryLimit() );
				count++;
			}
			else if ( KoLCharacter.getClassType() == KoLCharacter.SAUCEROR )
			{
				this.statusLabel[ count ].setText( "Soulsauce: ");
				this.statusValueLabel[ count ].setText( KoLCharacter.getSoulsauce() + " / 100" );
				count++;
			}
			else if ( KoLCharacter.getClassType() == KoLCharacter.DISCO_BANDIT )
			{
				this.statusLabel[ count ].setText( " Disco: " );
				this.statusValueLabel[ count ].setText( KoLCharacter.getDiscoMomentum() + " / 3" );
				count++;
			}
			else if ( KoLCharacter.isSneakyPete() )
			{
				limit = KoLCharacter.getAudienceLimit();
				this.statusLabel[ count ].setText( "   Aud: " );
				this.statusValueLabel[ count ].setText( KoLCharacter.getAudience() + " / " + limit );
				count++;
			}
			this.statusLabel[ count ].setText( "  Meat: " );
			this.statusValueLabel[ count ].setText( KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			this.statusValueLabel[ count ].setToolTipText( "Closet: " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getClosetMeat() ) );
			count++;
			if ( KoLCharacter.getHippyStoneBroken() )
			{
				this.statusLabel[ count ].setText( "   PvP: " );
				this.statusValueLabel[ count ].setText( String.valueOf( KoLCharacter.getAttacksLeft() ) );
				count++;
			}
			this.statusLabel[ count ].setText( "   Adv: " );
			this.statusValueLabel[ count ].setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
			count++;
			if ( KoLCharacter.isEd() )
			{
				this.statusLabel[ count ].setText( "    Ka: " );
				this.statusValueLabel[ count ].setText( String.valueOf( InventoryManager.getCount( ItemPool.KA_COIN ) ) );
				count++;
			}
		}
		else
		{
			this.statusLabel[ count ].setText( "  Gold: " );
			this.statusValueLabel[ count ].setText( String.valueOf( SpelunkyRequest.getGold() ) );
			count++;
			this.statusLabel[ count ].setText( "  Bomb: " );
			this.statusValueLabel[ count ].setText( String.valueOf( SpelunkyRequest.getBomb() ) );
			count++;
			this.statusLabel[ count ].setText( "  Rope: " );
			this.statusValueLabel[ count ].setText( String.valueOf( SpelunkyRequest.getRope() ) );
			count++;
			this.statusLabel[ count ].setText( "   Key: " );
			this.statusValueLabel[ count ].setText( String.valueOf( SpelunkyRequest.getKey() ) );
			count++;
			this.statusLabel[ count ].setText( " Turns: " );
			this.statusValueLabel[ count ].setText( String.valueOf( SpelunkyRequest.getTurnsLeft() ) );
			count++;
		}
		for( int i = count ; i < STATUS_LABELS ; i++ )
		{
			this.statusLabel[ i ].setText( "" );
			this.statusValueLabel[ i ].setText( "" );
		}

		count = 0;
		if ( limitmode != Limitmode.SPELUNKY )
		{
			// Remove this if/when KoL supports Water Level effect on Oil Peak/Tavern
			if ( KoLCharacter.inRaincore() )
			{
				this.bonusLabel[ count ].setText( "    ML: " );
				this.bonusValueLabel[ count ].setText( KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) +
					" (" + KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.currentNumericModifier( Modifiers.MONSTER_LEVEL ) ) + ")" );
				count++;
			}
			else
			{
				this.bonusLabel[ count ].setText( "    ML: " );
				this.bonusValueLabel[ count ].setText( KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
				count++;
			}
			this.bonusLabel[ count ].setText( "   Enc: " );
			this.bonusValueLabel[ count ].setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			count++;
			this.bonusLabel[ count ].setText( "  Init: " );
			this.bonusValueLabel[ count ].setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );
			count++;
			this.bonusLabel[ count ].setText( "   Exp: " );
			this.bonusValueLabel[ count ].setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
			count++;
			this.bonusLabel[ count ].setText( "  Meat: " );
			this.bonusValueLabel[ count ].setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			count++;
			this.bonusLabel[ count ].setText( "  Item: " );
			this.bonusValueLabel[ count ].setText( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );
			count++;
			int hobo = KoLCharacter.getHoboPower();
			if ( hobo != 0 && count < this.BONUS_LABELS )
			{
				this.bonusLabel[ count ].setText( "Hobo: " );
				this.bonusValueLabel[ count ].setText( KoLConstants.MODIFIER_FORMAT.format( hobo ) );
				count++;
			}
			int smithsness = KoLCharacter.getSmithsness();
			if ( smithsness != 0 && count < this.BONUS_LABELS )
			{
				this.bonusLabel[ count ].setText( "Smithsness: " );
				this.bonusValueLabel[ count ].setText( KoLConstants.MODIFIER_FORMAT.format( smithsness ) );
				count++;
			}
			if ( KoLCharacter.inBeecore() && count < this.BONUS_LABELS )
			{
				int bee = KoLCharacter.getBeeosity();
				this.bonusLabel[ count ].setText( "Bees: " );
				this.bonusValueLabel[ count ].setText( String.valueOf( bee ) );
				count++;
			}
			int surgeon = (int) KoLCharacter.currentNumericModifier( Modifiers.SURGEONOSITY );
			if ( surgeon != 0 && count < this.BONUS_LABELS )
			{
				this.bonusLabel[ count ].setText( "Surgeon: " );
				this.bonusValueLabel[ count ].setText( surgeon + " / 5" );
				count++;
			}
			int rave = KoLCharacter.currentBitmapModifier( Modifiers.RAVEOSITY );
			if ( rave != 0 && count < this.BONUS_LABELS )
			{
				this.bonusLabel[ count ].setText( "Rave: " );
				this.bonusValueLabel[ count ].setText( rave + " / 7" );
				count++;
			}
			int clown = KoLCharacter.getClownosity();
			if ( clown != 0 && count < this.BONUS_LABELS )
			{
				this.bonusLabel[ count ].setText( "Clown: " );
				this.bonusValueLabel[ count ].setText( clown + " / 4" );
				count++;
			}
		}
		else
		{
			this.bonusLabel[ count ].setText( "DR: " );
			this.bonusValueLabel[ count ].setText( String.valueOf( (int)KoLCharacter.currentNumericModifier( Modifiers.DAMAGE_REDUCTION ) ) );
			count++;
			this.bonusLabel[ count ].setText( "Luck: " );
			this.bonusValueLabel[ count ].setText( String.valueOf( (int)KoLCharacter.currentNumericModifier( Modifiers.LUCK ) ) );
			count++;
		}
		
		for( int i = count ; i < BONUS_LABELS ; i++ )
		{
			this.bonusLabel[ i ].setText( "" );
			this.bonusValueLabel[ i ].setText( "" );
		}

		try
		{
			String popText = CompactSidePane.modifierPopupText();
			this.modPopLabel.setText( popText );
		}
		catch ( Exception e )
		{
			// Ignore errors - there seems to be a Java bug that
			// occasionally gets triggered during the setText().
		}

		if ( limitmode != Limitmode.SPELUNKY )
		{
			long currentLevel = KoLCharacter.calculateLastLevel();
			long nextLevel = KoLCharacter.calculateNextLevel();
			long totalPrime = KoLCharacter.getTotalPrime();
			this.levelMeter.setMaximum( (int) (nextLevel - currentLevel) );
			this.levelMeter.setValue( (int) (totalPrime - currentLevel) );
			this.levelMeter.setString( " " );
			this.levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + 
				"&nbsp;&nbsp;<br>&nbsp;&nbsp;(" + KoLConstants.COMMA_FORMAT.format( nextLevel - totalPrime ) +
				" subpoints needed)&nbsp;&nbsp;</html>" );
		}
		else
		{
			this.levelMeter.setMaximum( 1 );
			this.levelMeter.setValue( 1 );
			this.levelMeter.setString( " " );
			this.levelPanel.setToolTipText( "" ); 
		}

		if ( limitmode == Limitmode.SPELUNKY )
		{
			String imageName = SpelunkyRequest.getBuddyImageName();
			if ( imageName == null )
			{
				ImageIcon icon = FamiliarDatabase.getNoFamiliarImage();
				this.familiarLabel.setIcon( icon );
				this.familiarLabel.setText( "" );
			}
			else
			{
				FileUtilities.downloadImage( "http://images.kingdomofloathing.com/otherimages/" + imageName );
				ImageIcon icon = JComponentUtilities.getImage( "otherimages/" + imageName );
				this.familiarLabel.setIcon( icon );
				icon.setImageObserver( this );
				String buddy = SpelunkyRequest.getBuddyName();
				this.familiarLabel.setText( buddy );
			}
		}
		else if ( KoLCharacter.inAxecore() )
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
		else if ( KoLCharacter.isJarlsberg() )
		{
			if ( KoLCharacter.getCompanion() == null )
			{
				ImageIcon icon = FamiliarDatabase.getNoFamiliarImage();
				this.familiarLabel.setIcon( icon );
			}
			else
			{
				FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + KoLCharacter.getCompanion().imageName() );
				ImageIcon icon = JComponentUtilities.getImage( "itemimages/" + KoLCharacter.getCompanion().imageName() );
				this.familiarLabel.setIcon( icon );
				icon.setImageObserver( this );
			}
		}
		else if ( KoLCharacter.isSneakyPete() )
		{
			FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/motorbike.gif" );
			ImageIcon icon = JComponentUtilities.getImage( "itemimages/motorbike.gif" );
			this.familiarLabel.setIcon( icon );
			icon.setImageObserver( this );

			String popText = CompactSidePane.motorcyclePopupText();
			try
			{
				this.motPopLabel.setText( popText );
			}
			catch ( Exception e )
			{
				// Ignore errors - there seems to be a Java bug that
				// occasionally gets triggered during the setText().
			}
		}
		else if ( KoLCharacter.isEd() )
		{
			EdServantData servant = EdServantData.currentServant();
			if ( servant == EdServantData.NO_SERVANT )
			{
				ImageIcon icon = FamiliarDatabase.getNoFamiliarImage();
				this.familiarLabel.setIcon( icon );
			}
			else
			{
				String image = servant.getImage();
				FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + image );
				ImageIcon icon = JComponentUtilities.getImage( "itemimages/" + image );
				this.familiarLabel.setIcon( icon );
				icon.setImageObserver( this );
				int level = servant.getLevel();
				this.familiarLabel.setText( "<HTML><center>level " + level + "</center></HTML>" );
			}
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
								( anno == null ? "" : "<br>" + anno.toString() ) + "</center></HTML>" );
			}
		}

		this.familiarLabel.setVerticalTextPosition( JLabel.BOTTOM );
		this.familiarLabel.setHorizontalTextPosition( JLabel.CENTER );
	}

	private static String motorcyclePopupText()
	{
		String tires = Preferences.getString( "peteMotorbikeTires" );
		String gasTank = Preferences.getString( "peteMotorbikeGasTank" );
		String headlight = Preferences.getString( "peteMotorbikeHeadlight" );
		String cowling = Preferences.getString( "peteMotorbikeCowling" );
		String muffler = Preferences.getString( "peteMotorbikeMuffler" );
		String seat = Preferences.getString( "peteMotorbikeSeat" );

		StringBuilder buf = new StringBuilder( "<html><table border=1>" );
		buf.append( "<tr><td>Tires</td><td>" );
		buf.append( tires );
		buf.append( "</td></tr>" );
		buf.append( "<tr><td>Gas Tank</td><td>" );
		buf.append( gasTank );
		buf.append( "</td></tr>" );
		buf.append( "<tr><td>Headlight</td><td>" );
		buf.append( headlight );
		buf.append( "</td></tr>" );
		buf.append( "<tr><td>Cowling</td><td>" );
		buf.append( cowling );
		buf.append( "</td></tr>" );
		buf.append( "<tr><td>Muffler</td><td>" );
		buf.append( muffler );
		buf.append( "</td></tr>" );
		buf.append( "<tr><td>Seat</td><td>" );
		buf.append( seat );
		buf.append( "</td></tr>" );
		buf.append( "</table></html>" );

		return buf.toString();
	}

	private static String modifierPopupText()
	{
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
		CompactSidePane.addElement( buf, "Hot", Modifiers.HOT_DAMAGE );
		CompactSidePane.addElement( buf, "Cold", Modifiers.COLD_DAMAGE );
		CompactSidePane.addElement( buf, "Stench", Modifiers.STENCH_DAMAGE );
		CompactSidePane.addElement( buf, "Spooky", Modifiers.SPOOKY_DAMAGE );
		CompactSidePane.addElement( buf, "Sleaze", Modifiers.SLEAZE_DAMAGE );
		CompactSidePane.addSlime( buf );
		CompactSidePane.addSupercold( buf );
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

		return buf.toString();
	}

	private static void addElement( StringBuffer buf, String name, int dmgModifier )
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

	private static void addSlime( StringBuffer buf )
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

	private static void addSupercold( StringBuffer buf )
	{
		int resist = (int) KoLCharacter.currentNumericModifier(
			Modifiers.SUPERCOLD_RESISTANCE );
		double percent = KoLCharacter.elementalResistanceByLevel( resist, false );
		if ( resist == 0 )
		{
			return;	// skip this row entirely, it's all zeros
		}
		buf.append( "<tr><td>Supercold</td><td colspan=2>" );
		buf.append( "</td><td>" );
		buf.append( KoLConstants.MODIFIER_FORMAT.format( resist ) );
		buf.append( " (" );
		buf.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( percent ) );
		buf.append( "%)</td></tr>" );
	}
}
