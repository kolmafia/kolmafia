package net.sourceforge.kolmafia.textui.parsetree;

public abstract class AggregateLiteral
	extends AggregateValue
{
	protected AggregateValue aggr = null;

        public AggregateLiteral( final AggregateType type )
	{
		super( type );
	}
}
