package net.sourceforge.kolmafia.textui.command;


public class CommandAlias
	extends AbstractCommand
{
	private final String actualCmd, actualParams;

	public CommandAlias( final String actualCmd, final String actualParams )
	{
		super();
		this.actualCmd = actualCmd;
		this.actualParams = actualParams;
		this.usage = " => " + actualCmd + " " + actualParams;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		this.CLI.executeCommand( this.actualCmd, ( this.actualParams + " " + parameters ).trim() );
	}
}
