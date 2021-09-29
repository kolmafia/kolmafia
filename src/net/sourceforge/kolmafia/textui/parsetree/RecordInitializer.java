package net.sourceforge.kolmafia.textui.parsetree;

import java.util.List;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class RecordInitializer
	extends TypeInitializer
{
	List<Value> params;

	public RecordInitializer( final RecordType type, List<Value> params )
	{
		super( type );
		this.params = params;
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		RecordType type = (RecordType) this.type;
		Type [] types = type.getFieldTypes();
		RecordValue record = (RecordValue) type.initialValue();
		Value[] content = (Value []) record.rawValue();

		interpreter.traceIndent();

		int fieldCount = 0;
		for ( Value fieldValue : this.params )
		{
			if ( fieldValue == DataTypes.VOID_VALUE )
			{
				fieldCount++;
				continue;
			}

			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "Field #" + (fieldCount + 1) + ": " + fieldValue.toQuotedString() );
			}

			Value value = fieldValue.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() == ScriptRuntime.State.EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			// Perform type coercion, just as an Assignment does
			Type fieldType = types[fieldCount];
			Value coercedValue = fieldType.equals( DataTypes.TYPE_STRING ) ? value.toStringValue() : fieldType.equals( DataTypes.TYPE_INT ) ? value.toIntValue() : fieldType.equals( DataTypes.TYPE_FLOAT ) ? value.toFloatValue() : fieldType.equals( DataTypes.TYPE_BOOLEAN ) ? value.toBooleanValue() : value;

			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + coercedValue.toQuotedString() );
			}

			content[fieldCount] = coercedValue;
			fieldCount++;
		}

		interpreter.traceUnindent();

		return record;
	}

	@Override
	public String toString()
	{
		return "<" + this.type + " initializer>";
	}
}
