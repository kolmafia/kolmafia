package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.preferences.Preferences;

public class RecoverCommand
	extends AbstractCommand
{
	public RecoverCommand()
	{
		this.usage = " hp | health | mp | mana | both - attempt to regain some HP or MP.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		boolean recoverHP = parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) || parameters.equalsIgnoreCase( "both" );
		boolean recoverMP = parameters.equalsIgnoreCase( "mp" ) || parameters.equalsIgnoreCase( "mana" ) || parameters.equalsIgnoreCase( "both" );
		boolean wasRecoveryActive = RecoveryManager.isRecoveryActive();

		try ( Checkpoint checkpoint = new Checkpoint() )
		{
			RecoveryManager.setRecoveryActive( true );

			if ( recoverHP )
			{
				long target = (long) ( Preferences.getFloat( "hpAutoRecoveryTarget" ) * KoLCharacter.getMaximumHP() );
				RecoveryManager.recoverHP( Math.max( target, KoLCharacter.getCurrentHP() + 1 ) );
			}

			if ( recoverMP )
			{
				long target = (long) ( Preferences.getFloat( "mpAutoRecoveryTarget" ) * KoLCharacter.getMaximumMP() );
				RecoveryManager.recoverMP( Math.max( target, KoLCharacter.getCurrentMP() + 1 ) );
			}
		}
		finally
		{
			RecoveryManager.setRecoveryActive( wasRecoveryActive );
		}
	}
}
