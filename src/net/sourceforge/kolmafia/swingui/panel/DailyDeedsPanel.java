/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.textui.command.AbstractCommand;

public class DailyDeedsPanel
	extends Box
{
	private static final ArrayList allPanels = new ArrayList();
	public static final AdventureResult GREAT_PANTS = ItemPool.get( ItemPool.GREAT_PANTS, 1 );
	public static final AdventureResult INFERNAL_SEAL_CLAW = ItemPool.get( ItemPool.INFERNAL_SEAL_CLAW, 1 );
	public static final AdventureResult NAVEL_RING = ItemPool.get( ItemPool.NAVEL_RING, 1 );

	public DailyDeedsPanel()
	{
		super( BoxLayout.Y_AXIS );
		DailyDeedsPanel.allPanels.add( new WeakReference( this ) );

		this.add( new BooleanDaily( "breakfastCompleted", "breakfast" ) );
		this.add( new BooleanDaily( "dailyDungeonDone", "adv * Daily Dungeon" ) );
		this.add( new SpadeDaily() );
		this.add( new CrimboTreeDaily() );
		this.add( new ChipsDaily() );
		this.add( new BooleanItemDaily( "libraryCardUsed",
			ItemPool.LIBRARY_CARD, "use library card" ) );
		this.add( new TelescopeDaily() );
		this.add( new PitDaily() );
		this.add( new StyxDaily() );
		this.add( new PoolDaily() );
		this.add( new ShowerStatsDaily() );
		this.add( new ShowerOtherDaily() );
		this.add( new BooleanItemDaily( "_bagOTricksUsed",
			ItemPool.BAG_O_TRICKS, "use Bag o' Tricks" ) );
		this.add( new BooleanItemDaily( "_legendaryBeat",
			ItemPool.LEGENDARY_BEAT, "use Legendary Beat" ) );
		this.add( new BooleanItemDaily( "outrageousSombreroUsed",
			ItemPool.OUTRAGEOUS_SOMBRERO, "use outrageous sombrero" ) );
		this.add( new FriarsDaily() );
		this.add( new SkateDaily( "lutz", "ice", "_skateBuff1",
			"Fishy" ) );
		this.add( new SkateDaily( "comet", "roller", "_skateBuff2",
			"-30% to Sea penalties" ) );
		this.add( new SkateDaily( "band shell", "peace", "_skateBuff3",
			"+sand dollars" ) );
		this.add( new SkateDaily( "eels", "peace", "_skateBuff4",
			"+10 lbs. underwater" ) );
		this.add( new SkateDaily( "merry-go-round", "peace", "_skateBuff5",
			"+25% items underwater" ) );
		this.add( new ConcertDaily() );
		this.add( new DemonDaily( 1, "yum!",
			2, "+100% meat, 30 turns" ) );
		this.add( new DemonDaily( 3, "+5-16 HP/MP, 30 turns",
			4, "+20 hot damage, +5 DR, 30 turns" ) );
		this.add( new DemonDaily( 5, "+30 stench damage, 30 turns",
			7, null ) );
		this.add( new DemonDaily( 8, "torment, 20 turns",
			9, "+80-100 hot damage, 30 turns" ) );
		this.add( new BooleanSkillDaily( "rageGlandVented",
				"Vent Rage Gland", "cast Vent Rage Gland" ) );
		this.add( new RestsDaily() );
		this.add( new HotTubDaily() );
		this.add( new NunsDaily() );
		this.add( new BooleanItemDaily( "oscusSodaUsed",
			ItemPool.NEVERENDING_SODA, "use Oscus's neverending soda" ) );
		this.add( new BooleanItemDaily( "expressCardUsed",
			ItemPool.EXPRESS_CARD, "use Yendorian Express card" ) );
		this.add( new MojoDaily() );
		this.add( new PuddingDaily() );
		this.add( new MelangeDaily() );
		this.add( new StillsDaily() );
		this.add( new TeaPartyDaily() );
		this.add( new PhotocopyDaily() );
		this.add( new PuttyDaily() );
		this.add( new CameraDaily() );
		this.add( new AdvsDaily() );
		this.add( new DropsDaily() );
		this.add( new FreeFightsDaily() );
		this.add( new RunawaysDaily() );
	}

	public void add( Daily daily )
	{
		daily.add( Box.createHorizontalGlue() );
		daily.initialUpdate();
		super.add( daily );
	}

	public static void addCommand( String command )
	{
		Iterator i = DailyDeedsPanel.allPanels.iterator();
		while ( i.hasNext() )
		{
			DailyDeedsPanel panel = (DailyDeedsPanel) ((WeakReference) i.next()).get();
			if ( panel != null )
			{
				panel.add( new CommandDaily( command ) );
				panel.revalidate();
			}
		}
	}

	public static void updateAll()
	{
		Iterator i = DailyDeedsPanel.allPanels.iterator();
		while ( i.hasNext() )
		{
			DailyDeedsPanel panel = (DailyDeedsPanel) ((WeakReference) i.next()).get();
			if ( panel != null )
			{
				for ( int j = panel.getComponentCount() - 1; j >= 0; --j )
				{
					if ( panel.getComponent( j ) instanceof Daily )
					{
						((Daily) panel.getComponent( j )).update();
					}
				}
			}
		}
	}

	public abstract static class Daily
		extends Box
		implements ActionListener, PreferenceListener
	{
		private ArrayList buttons;
		private JLabel label;

		public Daily()
		{
			super( BoxLayout.X_AXIS );
		}

		public void addListener( String preference )
		{
			PreferenceListenerRegistry.registerListener( preference, this );
		}

		public void addItem( int itemId )
		{
			InventoryManager.registerListener( itemId, this );
		}

		public JButton addButton( String command )
		{
			JButton button = new JButton( command );
			button.setActionCommand( command );
			button.addActionListener( this );
			button.setBackground( this.getBackground() );
			button.setDefaultCapable( false );
			button.putClientProperty( "JButton.buttonType", "segmented" );
			if ( this.buttons == null )
			{
				this.buttons = new ArrayList();
				button.putClientProperty( "JButton.segmentPosition", "only" );
			}
			else
			{
				button.putClientProperty( "JButton.segmentPosition", "last" );
				int last = this.buttons.size() - 1;
				((JButton) this.buttons.get( last )).putClientProperty(
					"JButton.segmentPosition", last == 0 ? "first" : "middle" );
			}
			this.buttons.add( button );
			this.add( button );
			return button;
		}

		public void addButton( String command, String tip )
		{
			this.addButton( command ).setToolTipText( tip );
		}

		public JButton buttonText( int idx, String command )
		{
			JButton button = (JButton) this.buttons.get( idx );
			button.setText( command );
			button.setActionCommand( command );
			return button;
		}

		public void buttonText( int idx, String command, String tip )
		{
			this.buttonText( idx, command ).setToolTipText( tip );
		}

		public void addLabel( String text )
		{
			this.label = new JLabel( text );
			this.add( this.label );
		}

		public void setText( String text )
		{
			this.label.setText( text );
		}

		public void setEnabled( boolean enabled )
		{
			Iterator i = this.buttons.iterator();
			while ( i.hasNext() )
			{
				((JButton) i.next()).setEnabled( enabled );
			}
		}

		public void setEnabled( int index, boolean enabled )
		{
			((JButton) this.buttons.get( index )).setEnabled( enabled );
		}

		public void setShown( boolean shown )
		{
			if ( shown != this.isVisible() )
			{
				this.setVisible( shown );
				this.revalidate();
			}
		}

		public void actionPerformed( ActionEvent e )
		{
			CommandDisplayFrame.executeCommand( e.getActionCommand() );
			// Try to avoid having a random button, possibly with a high associated
			// cost, set as the default button when this one is disabled.
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
		}

		public void initialUpdate()
		{
			this.update();
		}

		public void update()
		{
		}
	}

	public static class BooleanDaily
		extends Daily
	{
		String preference;

		public BooleanDaily( String preference, String command )
		{
			this.preference = preference;
			this.addListener( preference );
			this.addButton( command );
		}

		public void update()
		{
			this.setEnabled( !Preferences.getBoolean( this.preference ) );
		}
	}

	public static class BooleanItemDaily
		extends Daily
	{
		String preference;
		int itemId;

		public BooleanItemDaily( String preference, int itemId, String command )
		{
			this.preference = preference;
			this.itemId = itemId;
			this.addItem( itemId );
			this.addListener( preference );
			this.addButton( command );
		}

		public void update()
		{
			boolean pref = Preferences.getBoolean( this.preference );
			this.setShown( pref || InventoryManager.getCount( this.itemId ) > 0 );
			this.setEnabled( !pref );
		}
	}

	public static class BooleanSkillDaily
		extends Daily
	{
		String preference;
		String skill;

		public BooleanSkillDaily( String preference, String skill, String command )
		{
			this.preference = preference;
			this.skill = skill;
			this.addListener( preference );
			this.addListener( "(skill)" );
			this.addButton( command );
		}

		public void update()
		{
			boolean pref = Preferences.getBoolean( this.preference );
			this.setShown( KoLCharacter.hasSkill( this.skill ) );
			this.setEnabled( !pref );
		}
	}

	public static class CommandDaily
		extends Daily
	{
		public CommandDaily( String command )
		{
			this.addButton( command );
			this.addListener( command );
		}

		public void initialUpdate()
		{
		}

		public void update()
		{
			this.setEnabled( false );
		}
	}

	public static class NunsDaily
		extends Daily
	{
		public NunsDaily()
		{
			this.addListener( "nunsVisits" );
			this.addListener( "sidequestNunsCompleted" );
			this.addButton( "Nuns" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nv = Preferences.getInteger( "nunsVisits" );
			boolean snc = Preferences.getString( "sidequestNunsCompleted" ).equals( "none" );
			this.setShown( !snc );
			this.setEnabled( nv < 3 && !snc );
			this.setText( nv + "/3" );
		}
	}

	public static class SkateDaily
		extends Daily
	{
		private String state, visited;

		public SkateDaily( String name, String state, String visited, String desc )
		{
			this.state = state;
			this.visited = visited;
			this.addListener( "skateParkStatus" );
			this.addListener( visited );
			this.addButton( "skate " + name );
			this.addLabel( desc );
		}

		public void update()
		{
			this.setShown( Preferences.getString( "skateParkStatus" ).equals( this.state ) );
			this.setEnabled( !Preferences.getBoolean( this.visited ) );
		}
	}

	public static class DemonDaily
		extends Daily
	{
		private int demon1, demon2;

		public DemonDaily( int demon1, String tip1, int demon2, String tip2 )
		{
			this.demon1 = demon1;
			this.demon2 = demon2;
			this.addListener( "(character)" );
			this.addListener( "demonSummoned" );
			this.addListener( "demonName" + demon1 );
			this.addListener( "demonName" + demon2 );
			this.addItem( ItemPool.EYE_OF_ED );
			this.addItem( ItemPool.HEADPIECE_OF_ED );
			this.addItem( ItemPool.STAFF_OF_ED );
			this.addButton( "summon " + KoLAdventure.DEMON_TYPES[ demon1 - 1 ][ 1 ],
				tip1 );
			this.addButton( "summon " + KoLAdventure.DEMON_TYPES[ demon2 - 1 ][ 1 ],
				tip2 );
		}

		public void update()
		{
			boolean summoned = Preferences.getBoolean( "demonSummoned" );
			boolean have = InventoryManager.getCount( ItemPool.EYE_OF_ED ) > 0
				|| InventoryManager.getCount( ItemPool.HEADPIECE_OF_ED ) > 0
				|| InventoryManager.getCount( ItemPool.STAFF_OF_ED ) > 0;
			this.setShown( have );
			this.setEnabled( 0, !summoned && have &&
				!Preferences.getString( "demonName" + this.demon1 ).equals( "" ) );
			this.setEnabled( 1, !summoned && have &&
				!Preferences.getString( "demonName" + this.demon2 ).equals( "" ) );
		}
	}

	public static class SpadeDaily
		extends Daily
	{
		public SpadeDaily()
		{
			this.addListener( "spadingData" );
			this.addButton( "spade" );
			this.addLabel( "" );
		}

		public void update()
		{
			int ns = Preferences.getString( "spadingData" ).split( "\\|" ).length / 3;
			this.setEnabled( ns > 0 );
			this.setText( ns == 1 ? "one item to submit" : (ns + " items to submit") );
		}
	}

	public static class TelescopeDaily
		extends Daily
	{
		public TelescopeDaily()
		{
			this.addListener( "telescopeLookedHigh" );
			this.addListener( "telescopeUpgrades" );
			this.addListener( "kingLiberated" );
			this.addButton( "telescope high" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			int nu = Preferences.getInteger( "telescopeUpgrades" );
			this.setShown( ( !bm || kf ) && ( nu > 0 ) );
			this.setEnabled( nu > 0 && !Preferences.getBoolean( "telescopeLookedHigh" ) );
			this.setText( nu == 0 ? "" : ("+" + nu*5 + "% all, 10 turns") );
		}
	}

	public static class ConcertDaily
		extends Daily
	{
		public ConcertDaily()
		{
			this.addListener( "concertVisited" );
			this.addListener( "sidequestArenaCompleted" );
			this.addButton( "concert ?" );
			this.addButton( "concert ?" );
			this.addButton( "concert ?" );
		}

		public void update()
		{
			boolean cv = Preferences.getBoolean( "concertVisited" );
			String side = Preferences.getString( "sidequestArenaCompleted" );
			if ( side.equals( "fratboy" ) )
			{
				this.setShown( true );
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Elvish", "+10% all stats, 20 turns" );
				this.buttonText( 1, "concert Winklered", "+40% meat, 20 turns" );
				this.buttonText( 2, "concert White-boy Angst", "+50% initiative, 20 turns" );
			}
			else if ( side.equals( "hippy" ) )
			{
				this.setShown( true );
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Moon'd", "+5 stats per fight, 20 turns" );
				this.buttonText( 1, "concert Dilated Pupils", "+20% items, 20 turns" );
				this.buttonText( 2, "concert Optimist Primal", "+5 lbs., 20 turns" );
			}
			else {
				this.setShown( false );
				this.setEnabled( false );
			}
		}
	}

	public static class RestsDaily
		extends Daily
	{
		public RestsDaily()
		{
			this.addListener( "timesRested" );
			this.addListener( "(skill)" );
			this.addButton( "rest" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nr = Preferences.getInteger( "timesRested" );
			int fr = 0;
			if ( KoLCharacter.hasSkill( "Disco Nap" ) ) ++fr;
			if ( KoLCharacter.hasSkill( "Disco Power Nap" ) ) fr += 2;
			if ( KoLCharacter.hasSkill( "Executive Narcolepsy" ) ) ++fr;
			this.setShown( fr > 0 );
			this.setEnabled( nr < fr );
			this.setText( nr + " (" + fr + " free)" );
		}
	}

	public static class FriarsDaily
		extends Daily
	{
		public FriarsDaily()
		{
			this.addListener( "friarsBlessingReceived" );
			this.addListener( "lastFriarCeremonyAscension" );
			this.addListener( "kingLiberated" );
			this.addListener( "(character)" );
			this.addButton( "friars food", "+30% food drops, 20 turns" );
			this.addButton( "friars familiar", "+2 familiar exp per fight, 20 turns" );
			this.addButton( "friars booze", "+30% booze drops, 20 turns" );
		}

		public void update()
		{
			boolean kf = KoLCharacter.kingLiberated();
			int lfc = Preferences.getInteger( "lastFriarCeremonyAscension" );
			int ka = Preferences.getInteger( "knownAscensions" );
			this.setShown( kf || lfc == ka );
			this.setEnabled( !Preferences.getBoolean( "friarsBlessingReceived" ) );
		}
	}

	public static class StyxDaily
		extends Daily
	{
		public StyxDaily()
		{
			this.addListener( "styxPixieVisited" );
			this.addButton( "styx muscle", "+25% musc, +10 weapon dmg, +5 DR, 10 turns" );
			this.addButton( "styx mysticality", "+25% myst, +15 spell dmg, 10-15 MP regen, 10 turns" );
			this.addButton( "styx moxie", "+25% mox, +40% meat, +20% item, 10 turns" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			this.setShown( bm );
			this.setEnabled( !Preferences.getBoolean( "styxPixieVisited" ) &&
				bm );
		}
	}

	public static class MojoDaily
		extends Daily
	{
		public MojoDaily()
		{
			this.addListener( "currentMojoFilters" );
			this.addListener( "kingLiberated" );
			this.addItem( ItemPool.MOJO_FILTER );
			this.addButton( "use mojo filter" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean have = InventoryManager.getCount( ItemPool.MOJO_FILTER ) > 0;
			int nf = Preferences.getInteger( "currentMojoFilters" );
			this.setShown( have || nf > 0 );
			this.setEnabled( have && nf < 3 );
			this.setText( nf + "/3" );
		}
	}

	public static class HotTubDaily
		extends Daily
	{
		public HotTubDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_hotTubSoaks" );
			this.addListener( "kingLiberated" );
			this.addButton( "hottub" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			int nf = Preferences.getInteger( "_hotTubSoaks" );
			this.setShown( ( !bm || kf ) && ( have || nf > 0 ) );
			this.setEnabled( nf < 5 );
			this.setText( nf + "/5" );
		}
	}

	public static class PoolDaily
		extends Daily
	{
		public PoolDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_poolGames" );
			this.addListener( "kingLiberated" );
			this.addButton( "pool 1", "weapon dmg +50%, +5 lbs, 10 turns" );
			this.addButton( "pool 2", "spell dmg +50%, 10 MP per Adv, 10 turns" );
			this.addButton( "pool 3", "init +50%, +10% item, 10 turns" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			int nf = Preferences.getInteger( "_poolGames" );
			this.setShown( ( !bm || kf ) && ( have || nf > 0 ) );
			this.setEnabled( nf < 3 );
			this.setText( nf + "/3" );
		}
	}

	public static class ShowerStatsDaily
		extends Daily
	{
		public ShowerStatsDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_aprilShower" );
			this.addListener( "kingLiberated" );
			this.addButton( "shower muscle", "+5% to all Muscle Gains, 50 turns" );
			this.addButton( "shower mysticality", "+5% to all Mysticality Gains, 50 turns" );
			this.addButton( "shower moxie", "+5% to all Moxie Gains, 50 turns" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean as = Preferences.getBoolean( "_aprilShower" );
			this.setShown( ( !bm || kf ) && ( have || as ) );
			this.setEnabled( !as );
		}
	}

	public static class ShowerOtherDaily
		extends Daily
	{
		public ShowerOtherDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_aprilShower" );
			this.addListener( "kingLiberated" );
			this.addButton( "shower ice", "shards of double-ice" );
			this.addButton( "shower mp", "mp or amazing idea" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean as = Preferences.getBoolean( "_aprilShower" );
			this.setShown( ( !bm || kf ) && ( have || as ) );
			this.setEnabled( !as );
		}
	}

	public static class CrimboTreeDaily
		extends Daily
	{
		public CrimboTreeDaily()
		{
			this.addListener( "_crimboTree" );
			this.addListener( "crimboTreeDays" );
			this.addListener( "kingLiberated" );
			this.addButton( "crimbotree get" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean tree = Preferences.getBoolean( "_crimboTree" );
			int ctd = Preferences.getInteger( "crimboTreeDays" );
			this.setShown( ( !bm || kf ) && tree );
			this.setEnabled( ctd == 0 );
			this.setText( ctd + " days to go." );
		}
	}

	public static class MelangeDaily
		extends Daily
	{
		public MelangeDaily()
		{
			this.addItem( ItemPool.SPICE_MELANGE );
			this.addListener( "spiceMelangeUsed" );
			this.addLabel( "" );
		}

		public void update()
		{
			int have = InventoryManager.getCount( ItemPool.SPICE_MELANGE );
			if ( Preferences.getBoolean( "spiceMelangeUsed" ) )
			{
				this.setShown( true );
				this.setText( "SPICE MELANGE USED, have " + have );
			}
			else
			{
				this.setShown( have > 0 );
				this.setText( "spice melange not used, have " + have );
			}
		}
	}

	public static class StillsDaily
		extends Daily
	{
		public StillsDaily()
		{
			this.addListener( "(stills)" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			this.setShown( KoLCharacter.isMoxieClass() &&
				KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) );
			this.setText( (10 - KoLCharacter.getStillsAvailable()) +
				"/10 stills used" );
		}
	}

	public static class TeaPartyDaily
		extends Daily
	{
		public TeaPartyDaily()
		{
			this.addItem( ItemPool.DRINK_ME_POTION );
			this.addListener( "_madTeaParty" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			int have = InventoryManager.getCount( ItemPool.DRINK_ME_POTION );
			if ( Preferences.getBoolean( "_madTeaParty" ) )
			{
				this.setShown( !bm || kf );
				if ( have == 1 )
				{
					this.setText( "Mad Tea Party used, have " + have + " potion");
				}
				else
				{
					this.setText( "Mad Tea Party used, have " + have + " potions");
				}
			}
			else
			{
				this.setShown( have > 0 );
				if ( have == 1 )
				{
					this.setText( "Mad Tea Party not used, have " + have + " potion" );
				}
				else
				{
					this.setText( "Mad Tea Party not used, have " + have + " potions" );
				}
			}
		}
	}

	public static class FreeFightsDaily
		extends Daily
	{
		public FreeFightsDaily()
		{
			this.addListener( "_brickoFights" );
			this.addListener( "_hipsterAdv" );
			this.addListener( "_sealsSummoned" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bf = !KoLCharacter.isHardcore() ||
				(KoLCharacter.isHardcore() && KoLCharacter.hasSkill( "Summon BRICKOs" ));
			boolean hh = KoLCharacter.findFamiliar( FamiliarPool.HIPSTER ) != null ;
			boolean sc = KoLCharacter.getClassType().equals(KoLCharacter.SEAL_CLUBBER);

			this.setShown( bf || hh || sc );
			int maxSummons = 5;
			if ( KoLCharacter.hasEquipped( DailyDeedsPanel.INFERNAL_SEAL_CLAW ) ||
			     DailyDeedsPanel.INFERNAL_SEAL_CLAW.getCount( KoLConstants.inventory ) > 0 )
			{
				maxSummons = 10;
			}
			String text = "Fights: ";
			if( bf ) text = text + Preferences.getInteger( "_brickoFights" ) + "/10 BRICKO";
			if( bf && ( hh || sc ) ) text = text + ", ";
			if( hh ) text = text + Preferences.getInteger( "_hipsterAdv" ) + "/7 hipster";
			if( hh && sc ) text = text + ", ";
			if( sc ) text = text + Preferences.getInteger( "_sealsSummoned" ) + "/" + maxSummons + " seals summoned";
			this.setText( text );
		}
	}

	public static class RunawaysDaily
		extends Daily
	{
		public RunawaysDaily()
		{
//			this.addItem( ItemPool.NAVEL_RING );
			this.addListener( "_banderRunaways" );
			this.addListener( "_navelRunaways" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hb = KoLCharacter.findFamiliar( FamiliarPool.BANDER ) != null;
			boolean gp = InventoryManager.getCount( ItemPool.GREAT_PANTS ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.GREAT_PANTS );
			boolean nr = Preferences.getInteger( "_navelRunaways" ) > 0
				|| InventoryManager.getCount( ItemPool.NAVEL_RING ) > 0
				|| KoLCharacter.hasEquipped( DailyDeedsPanel.NAVEL_RING );
			this.setShown( hb || gp || nr );
			String text = "Runaways: ";
			if( hb ) text = text + Preferences.getInteger( "_banderRunaways" ) + " bandersnatch" ;
			if( hb && ( gp || nr ) ) text = text + ", ";
			if( nr && !gp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " navel ring";
			if( nr && gp ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap+navel";
			if( gp && !nr ) text = text + Preferences.getInteger( "_navelRunaways" ) + " gap pants";
			this.setText( text );
		}
	}

	public static class DropsDaily
		extends Daily
	{
		public DropsDaily()
		{
			this.addListener( "_absintheDrops" );
			this.addListener( "_aguaDrops" );
			this.addListener( "_astralDrops" );
			this.addListener( "_gongDrops" );
			this.addListener( "_pieDrops" );
			this.addListener( "_piePartsCount" );
			this.addListener( "_tokenDrops" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hf1 = KoLCharacter.findFamiliar( FamiliarPool.PIXIE ) != null;
			boolean hf2 = KoLCharacter.findFamiliar( FamiliarPool.SANDWORM ) != null;
			boolean hf3 = KoLCharacter.findFamiliar( FamiliarPool.BADGER ) != null;
			boolean hf4 = KoLCharacter.findFamiliar( FamiliarPool.LLAMA ) != null;
			boolean hf5 = KoLCharacter.findFamiliar( FamiliarPool.GRINDER ) != null;
			boolean hf6 = KoLCharacter.findFamiliar( FamiliarPool.TRON ) != null;
			this.setShown( hf1 || hf2 || hf3 || hf4 || hf5 || hf6 );
			String text = "Drops: ";
			if( hf1 ) text = text + Preferences.getInteger( "_absintheDrops" ) + " absinthe";
			if( hf1 && ( hf2 || hf3 || hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf2 ) text = text + Preferences.getInteger( "_aguaDrops" ) + " agua";
			if( hf2 && ( hf3 || hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf3 ) text = text + Preferences.getInteger( "_astralDrops" ) + " astral";
			if( hf3 && ( hf4 || hf5 || hf6 ) ) text = text + ", ";
			if( hf4 ) text = text + Preferences.getInteger( "_gongDrops" ) + " gong";
			if( hf4 && ( hf5  || hf6 ) ) text = text + ", ";
			if( hf5 )
			{
				if( Preferences.getInteger( "_pieDrops" )==1 )
					text = text + Preferences.getInteger( "_pieDrops" ) + " pie (";
				else text = text + Preferences.getInteger( "_pieDrops" ) + " pies (";
				text = text + Preferences.getInteger( "_piePartsCount" ) +")";
			}
			if( hf5 && hf6 ) text = text + ", ";
			if( hf6 ) text = text + Preferences.getInteger( "_tokenDrops" ) + " token";
			this.setText( text );
		}
	}

	public static class AdvsDaily
		extends Daily
	{
		public AdvsDaily()
		{
			// this.addItem( ItemPool.TIME_HELMET );
			// this.addItem( ItemPool.V_MASK );
			this.addListener( "_gibbererAdv" );
			this.addListener( "_hareAdv" );
			this.addListener( "_hipsterAdv" );
			this.addListener( "_riftletAdv" );
			this.addListener( "_timeHelmetAdv" );
			this.addListener( "_vmaskAdv" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean hf1 = KoLCharacter.findFamiliar( FamiliarPool.GIBBERER ) != null;
			boolean hf2 = KoLCharacter.findFamiliar( FamiliarPool.HARE ) != null;
			boolean hf3 = KoLCharacter.findFamiliar( FamiliarPool.RIFTLET ) != null;
			boolean hf4 = InventoryManager.getCount( ItemPool.TIME_HELMET ) > 0
				|| Preferences.getInteger( "_timeHelmetAdv" ) > 0;
			boolean hf5 = InventoryManager.getCount( ItemPool.V_MASK ) > 0
				|| Preferences.getInteger( "_vmaskAdv" ) > 0;
			String text = "Advs: ";
			if( hf1 ) text = text + Preferences.getInteger( "_gibbererAdv" ) + " gibberer";
			if( hf1 && (hf2 || hf3 || hf4 || hf5) ) text = text + ", ";
			if( hf2 ) text = text + Preferences.getInteger( "_hareAdv" ) + " hare";
			if( hf2 && (hf3 || hf4 || hf5) ) text = text + ", ";
			if( hf3 ) text = text + Preferences.getInteger( "_riftletAdv" ) + " riftlet";
			if( hf3 && (hf4 || hf5) ) text = text + ", ";
			if( hf4 ) text = text + Preferences.getInteger( "_timeHelmetAdv" ) + " time helmet";
			if( hf4 && hf5 ) text = text + ", ";
			if( hf5 ) text = text + Preferences.getInteger( "_vmaskAdv" ) + " V mask";
			this.setShown( hf1 || hf2 || hf3 || hf4 || hf5 );
			this.setText( text );
		}
	}

	public static class PuttyDaily
		extends Daily
	{
		public PuttyDaily()
		{
			this.addListener( "spookyPuttyCopiesMade" );
			this.addListener( "spookyPuttyMonster" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean kf = KoLCharacter.kingLiberated();
			boolean hc = KoLCharacter.isHardcore();
			boolean have = InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MITRE ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_LEOTARD ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_BALL ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SHEET ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_SNAKE ) > 0
				|| InventoryManager.getCount( ItemPool.SPOOKY_PUTTY_MONSTER ) > 0
				|| Preferences.getInteger( "spookyPuttyCopiesMade" ) > 0;
			String text = Preferences.getInteger( "spookyPuttyCopiesMade" ) +
				"/5 putty uses";
			String monster = Preferences.getString( "spookyPuttyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setShown( ( kf || !hc ) && have );
			this.setText( text );
		}
	}

	public static class CameraDaily
		extends Daily
	{
		public CameraDaily()
		{
			this.addListener( "_cameraUsed" );
			this.addListener( "cameraMonster" );
			this.addLabel( "" );
		}

		public void update()
		{
			String text = Preferences.getBoolean( "_cameraUsed" ) ?
				"4-d camera used"
				: "4-d camera not used yet";
			String monster = Preferences.getString( "cameraMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
		}
	}

	public static class PhotocopyDaily
		extends Daily
	{
		public PhotocopyDaily()
		{
			this.addItem( ItemPool.VIP_LOUNGE_KEY );
			this.addListener( "_photocopyUsed" );
			this.addListener( "photocopyMonster" );
			this.addListener( "kingLiberated" );
			this.addLabel( "" );
		}

		public void update()
		{
			boolean bm = KoLCharacter.inBadMoon();
			boolean kf = KoLCharacter.kingLiberated();
			boolean have = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			String text = Preferences.getBoolean( "_photocopyUsed" ) ?
				"photocopied monster used"
				: "photocopied monster not used yet";
			String monster = Preferences.getString( "photocopyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
			this.setShown( (!bm || kf) && have );
		}
	}

	public static class PuddingDaily
		extends Daily
	{
		public PuddingDaily()
		{
			this.addListener( "blackPuddingsDefeated" );
			this.addButton( "eat black pudding" );
			this.addLabel( "" );
		}

		public void update()
		{
			int bpd = Preferences.getInteger( "blackPuddingsDefeated" );
			this.setText( bpd + " defeated!" );
			this.setShown( bpd < 240 && KoLCharacter.canEat() );
		}
	}

	public static class ChipsDaily
	extends Daily
	{
		public ChipsDaily()
		{
			this.addListener( "_chipBags" );
			this.addButton( "chips radium", "moxie +30 for 10" );
			this.addButton( "chips wintergreen", "muscle +30 for 10" );
			this.addButton( "chips ennui", "mysticality +30 for 10" );
			this.addLabel( "" );
		}

		public void update()
		{
			int nf = Preferences.getInteger( "_chipBags" );
			this.setShown( KoLCharacter.hasClan() &&
				KoLCharacter.canInteract() );
			this.setEnabled( nf < 3 );
			this.setText( nf + "/3" );
		}
	}
	public static class PitDaily
	extends Daily
	{
		public PitDaily()
		{
			this.addListener( "_ballpit" );
			this.addListener( "kingLiberated" );
			this.addButton( "ballpit", "stat boost for 20" );
		}

		public void update()
		{
			boolean dun = Preferences.getBoolean( "_ballpit" );
			this.setShown( KoLCharacter.hasClan() &&
				KoLCharacter.canInteract());
			this.setEnabled( !dun );
		}
	}


	static { new Later().register( "later" ); }
	public static class Later
		extends AbstractCommand
	{
		{ flags = KoLmafiaCLI.FULL_LINE_CMD; }
		{ usage = " <commands> - adds a button to do commands to the Daily Deeds list."; }
		public void run( String cmd, String parameters )
		{
			if ( parameters.length() == 0 )
			{
				KoLmafia.updateDisplay( "No command(s) specified!" );
				return;
			}
			DailyDeedsPanel.addCommand( parameters );
			KoLmafia.updateDisplay( "Daily Deed added." );
		}
	}
}
