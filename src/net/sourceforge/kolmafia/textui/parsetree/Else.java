package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Else
	extends Conditional
{
	public Else( final Scope scope, final Value condition )
	{
		super( scope, condition );
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( ScriptRuntime.State.EXIT );
			return null;
		}

		interpreter.traceIndent();
		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "else" );
		}
		Value result = this.scope.execute( interpreter );
		interpreter.traceUnindent();

		if ( interpreter.getState() != ScriptRuntime.State.NORMAL )
		{
			return result;
		}

		return DataTypes.TRUE_VALUE;
	}

	@Override
	public String toString()
	{
		return "else";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<ELSE>" );
		this.getScope().print( stream, indent + 1 );
	}
}
