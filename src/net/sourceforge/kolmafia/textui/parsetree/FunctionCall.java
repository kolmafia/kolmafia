package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.List;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.Profiler;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class FunctionCall
	extends Value
{
	protected Function target;
	protected final List<Value> params;
	protected final String fileName;
	protected final int lineNumber;

	public FunctionCall( final Function target, final List<Value> params, final Parser parser )
	{
		this.target = target;
		this.params = params;
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	public Function getTarget()
	{
		return this.target;
	}

	public List<Value> getParams()
	{
		return this.params;
	}

	@Override
	public Type getType()
	{
		return this.target.getType();
	}

	@Override
	public Type getRawType()
	{
		return this.target.getType();
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

		Object[] values = new Object[ params.size() + 1 ];
		values[ 0 ] = interpreter;

		int paramCount = 1;

		for ( Value paramValue : this.params )
		{
			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );
			}

			Value value = paramValue.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );
			}

			if ( interpreter.getState() == ScriptRuntime.State.EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			values[paramCount++] = value;
		}

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Entering function " + this.target.getName() );
		}

		interpreter.setLineAndFile( this.fileName, this.lineNumber );

		// push to interpreter stack
		interpreter.pushFrame( this.target.getName() );

		Value result;
		Profiler prev = interpreter.profiler;
		if ( prev != null )
		{
			long t0 = System.nanoTime();
			prev.net += t0 - prev.net0;
			Profiler curr = Profiler.create( this.target.getSignature() );
			curr.net0 = t0;
			interpreter.profiler = curr;

			result = this.target.execute( interpreter, values );

			long t1 = System.nanoTime();
			prev.net0 = t1;
			interpreter.profiler = prev;
			curr.total = t1 - t0;
			curr.net += t1 - curr.net0;
			curr.finish();
		}
		else
		{
			result = this.target.execute( interpreter, values );
		}

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Function " + this.target.getName() + " returned: " + result );
		}

		if ( interpreter.getState() != ScriptRuntime.State.EXIT )
		{
			interpreter.setState( ScriptRuntime.State.NORMAL );
		}

		interpreter.traceUnindent();

		interpreter.popFrame();
		return result;
	}

	@Override
	public String toString()
	{
		return this.target.getName() + "()";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<CALL " + this.getTarget().getName() + ">" );

		for ( Value current : this.params )
		{
			current.print( stream, indent + 1 );
		}
	}
}
