package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

public class SlimeStackCommand 
	extends AbstractCommand
{
	public SlimeStackCommand()
	{
		this.usage = " - show details of slime stacks disgorged by Slimeling";
	}
	
	@Override
	public void run( final String cmd, final String parameters )
	{
		int got = Preferences.getInteger( "slimelingStacksDropped" );
		int due = Preferences.getInteger( "slimelingStacksDue" );
		
		if ( due == 0 )
		{
			KoLmafia.updateDisplay( "No slime stacks due.  Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive slime stacks." );
			return;
		}
		
		if ( got == due )
		{
			KoLmafia.updateDisplay( "Got all " + due + " expected slime stacks this ascension.  Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive more." );
			return;
		}
		
		int missing = due - got;
		int next = got + 1;
		KoLmafia.updateDisplay( missing + " slime stacks queued.  Next: #" + next + " (expected after " + FamiliarData.getSlimeStackTurns( next ) + " total Slimeling combats)." );
	}
}
