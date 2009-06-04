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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.KeyboardFocusManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DailyDeedsPanel
	extends Box
{
	private static final ArrayList allPanels = new ArrayList();
	
	public DailyDeedsPanel()
	{
		super( BoxLayout.Y_AXIS );
		DailyDeedsPanel.allPanels.add( new WeakReference( this ) );
		
		this.add( new BooleanDaily( "breakfastCompleted", "breakfast" ) );
		this.add( new RunawaysDaily() );
		this.add( new DropsDaily() );
		this.add( new SealsDaily() );
		this.add( new RestsDaily() );
		this.add( new HotTubDaily() );
		this.add( new BooleanDaily( "dailyDungeonDone", "adv * Daily Dungeon" ) );
		this.add( new TelescopeDaily() );
		this.add( new StyxDaily() );
		this.add( new FriarsDaily() );
		this.add( new ConcertDaily() );
		this.add( new DemonDaily( 1, 2 ) );
		this.add( new DemonDaily( 3, 4 ) );
		this.add( new DemonDaily( 5, 7 ) );
		this.add( new NunsDaily() );
		if ( KoLCharacter.hasSkill( "Vent Rage Gland" ) )
		{
			this.add( new BooleanDaily( "rageGlandVented", "cast Vent Rage Gland" ) );
		}
		this.add( new BooleanDaily( "libraryCardUsed", "use library card" ) );
		this.add( new BooleanDaily( "outrageousSombreroUsed", "use outrageous sombrero" ) );
		this.add( new BooleanDaily( "oscusSodaUsed", "use Oscus's neverending soda" ) );
		this.add( new BooleanDaily( "expressCardUsed", "use Yendorian Express card" ) );
		this.add( new MojoDaily() );
		this.add( new MelangeDaily() );
		this.add( new StillsDaily() );
		this.add( new PuttyDaily() );
		if ( Preferences.getInteger( "blackPuddingsDefeated" ) < 240 )
		{
			this.add( new PuddingDaily() );
		}
		this.add( new SpadeDaily() );
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
		implements ActionListener, Preferences.ChangeListener
	{
		private ArrayList buttons;
		private JLabel label;
		
		public Daily()
		{
			super( BoxLayout.X_AXIS );
		}
		
		public void addListener( String preference )
		{
			Preferences.registerListener( preference, this );
		}
		
		public void addButton( String command )
		{
			JButton button = new JButton( command );
			button.setActionCommand( command );
			button.addActionListener( this );
			button.setBackground( this.getBackground() );
			button.setDefaultCapable( false );
			if ( this.buttons == null )
			{
				this.buttons = new ArrayList();
			}
			this.buttons.add( button );
			this.add( button );
		}
		
		public void buttonText( int idx, String command )
		{
			JButton button = (JButton) this.buttons.get( idx );
			button.setText( command );
			button.setActionCommand( command );
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
			this.setEnabled( nv < 3 &&
				!Preferences.getString( "sidequestNunsCompleted" ).equals( "none" ) );
			this.setText( nv + "/3" );
		}
	}

	public static class DemonDaily
		extends Daily
	{
		private int demon1, demon2;
		
		public DemonDaily( int demon1, int demon2 )
		{
			this.demon1 = demon1;
			this.demon2 = demon2;
			this.addListener( "(character)" );
			this.addListener( "demonSummoned" );
			this.addListener( "demonName" + demon1 );
			this.addListener( "demonName" + demon2 );
			this.addButton( "summon " + KoLAdventure.DEMON_TYPES[ demon1 - 1 ][ 1 ] );
			this.addButton( "summon " + KoLAdventure.DEMON_TYPES[ demon2 - 1 ][ 1 ] );
		}
		
		public void update()
		{
			boolean summoned = Preferences.getBoolean( "demonSummoned" );
			int level = KoLCharacter.getLevel();
			this.setEnabled( 0, !summoned && level >= 11 &&
				!Preferences.getString( "demonName" + this.demon1 ).equals( "" ) );
			this.setEnabled( 1, !summoned && level >= 11 &&
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
			this.addButton( "telescope high" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			int nu = Preferences.getInteger( "telescopeUpgrades" );
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
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Elvish" );
				this.buttonText( 1, "concert Winklered" );
				this.buttonText( 2, "concert White-boy Angst" );
			}
			else if ( side.equals( "hippy" ) )
			{
				this.setEnabled( !cv );
				this.buttonText( 0, "concert Moon'd" );
				this.buttonText( 1, "concert Dilated Pupils" );
				this.buttonText( 2, "concert Optimist Primal" );
			}
			else {
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
			this.addButton( "rest" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			int nr = Preferences.getInteger( "timesRested" );
			int fr = 0;
			if ( KoLCharacter.hasSkill( "Disco Nap" ) ) ++fr;
			if ( KoLCharacter.hasSkill( "Disco Power Nap" ) ) fr += 2;
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
			this.addListener( "(character)" );
			this.addButton( "friars food" );
			this.addButton( "friars familiar" );
			this.addButton( "friars booze" );
		}
		
		public void update()
		{
			this.setEnabled( KoLCharacter.getLevel() >= 6 &&
				!Preferences.getBoolean( "friarsBlessingReceived" ) );
		}
	}

	public static class StyxDaily
		extends Daily
	{
		public StyxDaily()
		{
			this.addListener( "styxPixieVisited" );
			this.addButton( "styx muscle" );
			this.addButton( "styx mysticality" );
			this.addButton( "styx moxie" );
		}
		
		public void update()
		{
			this.setEnabled( !Preferences.getBoolean( "styxPixieVisited" ) &&
				KoLCharacter.inBadMoon() );
		}
	}

	public static class MojoDaily
		extends Daily
	{
		public MojoDaily()
		{
			this.addListener( "currentMojoFilters" );
			this.addButton( "use mojo filter" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			int nf = Preferences.getInteger( "currentMojoFilters" );
			this.setEnabled( nf < 3 );
			this.setText( nf + "/3" );
		}
	}

	public static class HotTubDaily
		extends Daily
	{
		public HotTubDaily()
		{
			this.addListener( "_hotTubSoaks" );
			this.addButton( "hottub" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			int nf = Preferences.getInteger( "_hotTubSoaks" );
			this.setEnabled( nf < 5 );
			this.setText( nf + "/5" );
		}
	}

	public static class MelangeDaily
		extends Daily
	{
		public MelangeDaily()
		{
			this.addListener( "spiceMelangeUsed" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			if ( Preferences.getBoolean( "spiceMelangeUsed" ) )
			{
				this.setText( "SPICE MELANGE USED" );
			}
			else
			{
				this.setText( "spice melange not used" );
			}
		}
	}

	public static class StillsDaily
		extends Daily
	{
		public StillsDaily()
		{
			this.addListener( "(stills)" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			this.setText( KoLCharacter.getStillsAvailable() +
				"/10 stills available" );
		}
	}

	public static class SealsDaily
		extends Daily
	{
		public SealsDaily()
		{
			this.addListener( "_sealsSummoned" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			this.setText( Preferences.getInteger( "_sealsSummoned" ) +
				"/5 seals summoned" );
		}
	}

	public static class RunawaysDaily
		extends Daily
	{
		public RunawaysDaily()
		{
			this.addListener( "_navelRunaways" );
			this.addListener( "_banderRunaways" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			this.setText( "Runaways: " + Preferences.getInteger( "_navelRunaways" )
				+ " navel ring, " + Preferences.getInteger( "_banderRunaways" )
				+ " bandersnatch" );
		}
	}

	public static class DropsDaily
		extends Daily
	{
		public DropsDaily()
		{
			this.addListener( "_aguaDrops" );
			this.addListener( "_gongDrops" );
			this.addListener( "_absintheDrops" );
			this.addListener( "_astralDrops" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			this.setText( "Drops: " + Preferences.getInteger( "_aguaDrops" )
				+ " agua, " + Preferences.getInteger( "_gongDrops" )
				+ " gong, " + Preferences.getInteger( "_absintheDrops" )
				+ " absinthe, " + Preferences.getInteger( "_astralDrops" )
				+ " astral mushroom" );
		}
	}

	public static class PuttyDaily
		extends Daily
	{
		public PuttyDaily()
		{
			this.addListener( "spookyPuttyCopiesMade" );
			this.addListener( "spookyPuttyMonster" );
			this.addLabel( "" );
		}
		
		public void update()
		{
			String text = Preferences.getInteger( "spookyPuttyCopiesMade" ) +
				"/5 putty uses";
			String monster = Preferences.getString( "spookyPuttyMonster" );
			if ( !monster.equals( "" ) )
			{
				text = text + ", now " + monster;
			}
			this.setText( text );
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
			this.setText( Preferences.getInteger( "blackPuddingsDefeated" ) + " defeated!" );
		}
	}

	static { new Later().register( "later" ); }
	public static class Later
		extends KoLmafiaCLI.Command
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
