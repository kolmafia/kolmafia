package net.sourceforge.kolmafia.textui.parsetree;


public class AggregateValue
	extends CompositeValue
{
	public AggregateValue( final AggregateType type )
	{
		super( type );
	}

	public Type getDataType()
	{
		return ( (AggregateType) this.type ).getDataType();
	}

	@Override
	public int count()
	{
		return 0;
	}

	@Override
	public boolean contains( final Value index )
	{
		return false;
	}

	@Override
	public String toString()
	{
		return "aggregate " + this.type.toString();
	}
}
