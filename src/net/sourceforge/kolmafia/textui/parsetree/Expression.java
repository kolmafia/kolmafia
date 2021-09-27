package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

public abstract class Expression
	extends Value
{
	Value lhs;
	Value rhs;

	public Value getLeftHandSide()
	{
		return this.lhs;
	}

	public Value getRightHandSide()
	{
		return this.rhs;
	}

	@Override
	public String toQuotedString()
	{
		return this.toString();
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		this.lhs.print( stream, indent + 1 );
		if ( this.rhs != null )
		{
			this.rhs.print( stream, indent + 1 );
		}
	}
}
