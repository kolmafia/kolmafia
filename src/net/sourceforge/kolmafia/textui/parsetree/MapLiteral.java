package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.textui.AshRuntime;

public class MapLiteral
	extends AggregateLiteral
{
	private AggregateValue aggr = null;
	private final List<Value> keys;
	private final List<Value> values;

	public MapLiteral( final AggregateType type, final List<Value> keys, final List<Value> values )
	{
		super( type );
		this.keys = keys;
		this.values = values;
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		this.aggr = (AggregateValue)this.type.initialValue();

		Iterator<Value> keyIterator = this.keys.iterator();
		Iterator<Value> valIterator = this.values.iterator();

		while ( keyIterator.hasNext() && valIterator.hasNext() )
		{
			Value key = keyIterator.next().execute( interpreter );
			Value val = valIterator.next().execute( interpreter );
			this.aggr.aset( key, val );
		}

		return this.aggr;
	}

	@Override
	public int count()
	{
		if ( this.aggr != null )
		{
			return this.aggr.count();
		}
		return this.keys.size();
	}
}
