package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;

import org.json.JSONArray;
import org.json.JSONException;

public class PluralValue
	extends AggregateValue
{
	private TreeSet<Value> lookup;
	
	public PluralValue( final Type type, List<Value> values )
	{
		super( new AggregateType( DataTypes.BOOLEAN_TYPE, type ) );

		this.content = values.toArray( new Value[ 0 ] );
	}

	@Override
	public Value aref( final Value key, final AshRuntime interpreter )
	{
		return DataTypes.makeBooleanValue( this.contains( key ) );
	}

	@Override
	public void aset( final Value key, final Value val, final AshRuntime interpreter )
	{
		throw interpreter.runtimeException( "Cannot modify constant value" );
	}

	@Override
	public Value remove( final Value key, final AshRuntime interpreter )
	{
		throw interpreter.runtimeException( "Cannot modify constant value" );
	}

	@Override
	public void clear()
	{
	}

	@Override
	public int count()
	{
		Value[] array = (Value[]) this.content;
		return array.length;
	}

	@Override
	public boolean contains( final Value key )
	{
		if ( this.lookup == null )
		{
			this.lookup = new TreeSet<Value>();
			this.lookup.addAll( Arrays.asList( (Value[]) this.content ) );
		}
		return this.lookup.contains( key );
	}

	@Override
	public Value[] keys()
	{
		return (Value[]) this.content;
	}

	@Override
	public Object toJSON() throws JSONException
	{
		JSONArray obj = new JSONArray();

		Value[] array = this.keys();

		for ( int i = 0; i < array.length; ++i )
		{
			obj.put( array[ i ].toJSON() );
		}

		return obj;
	}
}
