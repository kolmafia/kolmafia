package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import org.eclipse.lsp4j.Location;

import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;

import net.sourceforge.kolmafia.utilities.ByteArrayStream;

public class BasicScript
	extends Command
{
	private final ByteArrayStream data;

	public BasicScript( final Location location, final ByteArrayStream data )
	{
		super( location );
		this.data = data;
	}

	public Type getType()
	{
		return DataTypes.VOID_TYPE;
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		KoLmafiaCLI script = new KoLmafiaCLI( this.data.getByteArrayInputStream() );
		script.listenForCommands();
		return DataTypes.VOID_VALUE;
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
	}
}
