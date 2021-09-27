package net.sourceforge.kolmafia.textui.parsetree;

public abstract class AggregateValue
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
	public abstract int count();

	@Override
	public abstract boolean contains( final Value index );

	@Override
	public String toString()
	{
		return "aggregate " + this.type.toString();
	}
}
