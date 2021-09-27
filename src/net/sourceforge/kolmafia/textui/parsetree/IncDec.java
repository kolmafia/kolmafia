package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class IncDec
	extends Value
{
	private final VariableReference lhs;
	private final Operator oper;

	public IncDec( final VariableReference lhs, final Operator oper )
	{
		this.lhs = lhs;
		this.oper = oper;
	}

	public VariableReference getLeftHandSide()
	{
		return this.lhs;
	}

	public Type getType()
	{
		return this.lhs.getType();
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( ScriptRuntime.State.EXIT );
			return null;
		}

		String operStr = oper.operator;
		Value value;

		interpreter.traceIndent();
		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Eval: " + this.lhs );
		}

		value = this.lhs.execute( interpreter );
		interpreter.captureValue( value );

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Orig: " + value );
		}

		Value newValue = this.oper.applyTo( interpreter, value );

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "New: " + newValue );
		}

		this.lhs.setValue( interpreter, newValue );

		interpreter.traceUnindent();

		if ( interpreter.getState() == ScriptRuntime.State.EXIT )
		{
			return null;
		}

		return ( operStr == Parser.PRE_INCREMENT || operStr == Parser.PRE_DECREMENT ) ? newValue : value;
	}

	@Override
	public String toString()
	{
		String operStr = oper.operator;
		return  operStr == Parser.PRE_INCREMENT ? ( "++" + this.lhs.getName() ) :
			operStr == Parser.PRE_DECREMENT ? ( "--" + this.lhs.getName() ) :
			operStr == Parser.POST_INCREMENT ? ( this.lhs.getName() +  "++" ) :
			operStr == Parser.POST_DECREMENT ? ( this.lhs.getName() +  "--" ) :
			"";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		String operStr = oper.operator;
		String type =
			operStr == Parser.PRE_INCREMENT ? "PRE-INCREMENT" :
			operStr == Parser.PRE_DECREMENT ? "PRE-DECREMENT" :
			operStr == Parser.POST_INCREMENT ? "POST-INCREMENT" :
			operStr == Parser.POST_DECREMENT ? "POST-DECREMENT" :
			"UNKNOWN";
		AshRuntime.indentLine( stream, indent );
		stream.println( "<" + type + " " + this.lhs.getName() + ">" );
		VariableReference lhs = this.getLeftHandSide();
		Parser.printIndices( lhs.getIndices(), stream, indent + 1 );
	}
}
