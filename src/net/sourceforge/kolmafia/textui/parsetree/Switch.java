package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.Location;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Switch
	extends Command
{
	private final Value condition;
	private final Value [] tests;
	private final Integer [] offsets;
	private final int defaultIndex;
	private final SwitchScope scope;
	private final Map<Value, Integer> labels;

	public Switch( final Location location, final Value condition, final List<Value> tests, final List<Integer> offsets, final int defaultIndex, final SwitchScope scope, final Map<Value, Integer> labels )
	{
		super( location );
		this.condition = condition;
		this.tests = tests.toArray( new Value[tests.size()] );
		this.offsets = offsets.toArray( new Integer[offsets.size()] );
		this.defaultIndex = defaultIndex;
		this.scope = scope;
		this.labels = labels;
	}

	public Value getCondition()
	{
		return this.condition;
	}

	public SwitchScope getScope()
	{
		return this.scope;
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
			interpreter.trace( this.toString() );
		}

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Value: " + this.condition );
		}

		Value value = this.condition.execute( interpreter );
		interpreter.captureValue( value );

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + value );
		}

		if ( value == null )
		{
			interpreter.traceUnindent();
			return null;
		}

		int offset = this.defaultIndex;

		if ( labels != null )
		{
			Integer mapped = labels.get( value );
			if ( mapped != null )
			{
				offset = mapped.intValue();
			}
		}
		else
		{
			for ( int index = 0; index < tests.length; ++index )
			{
				Value test = tests[index];
				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "test: " + test );
				}

				Value result = test.execute( interpreter );
				interpreter.captureValue( result );

				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "[" + interpreter.getState() + "] <- " + result );
				}

				if ( result == null )
				{
					interpreter.traceUnindent();
					return null;
				}

				if ( result.equals( value ) )
				{
					offset = offsets[ index ].intValue();
					break;
				}
			}
		}

		if ( offset >= 0 && offset < this.scope.commandCount() )
		{
			this.scope.setOffset( offset );
			Value result = this.scope.execute( interpreter );

			if ( interpreter.getState() == ScriptRuntime.State.BREAK )
			{
				interpreter.setState( ScriptRuntime.State.NORMAL );
				interpreter.traceUnindent();
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() != ScriptRuntime.State.NORMAL )
			{
				interpreter.traceUnindent();
				return result;
			}
		}

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	@Override
	public String toString()
	{
		return "switch";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<SWITCH" + (labels != null ? " (OPTIMIZED)" : "" ) + ">" );
		this.getCondition().print( stream, indent + 1 );
		this.getScope().print( stream, indent + 1, tests, offsets, defaultIndex );
	}

	@Override
	public boolean assertBarrier()
	{
		return this.defaultIndex != -1 &&
			this.scope.assertBarrier() &&
			!this.scope.assertBreakable();
	}
}
