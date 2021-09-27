package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.session.BuffBotManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuffbotCommand
	extends AbstractCommand
{
	public BuffbotCommand()
	{
		this.usage = " <number> - run buffbot for number iterations.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		BuffBotManager.loadSettings();
		BuffBotManager.runBuffBot( StringUtilities.parseInt( parameters ) );
		KoLmafia.updateDisplay( "Buffbot execution complete." );
	}
}
