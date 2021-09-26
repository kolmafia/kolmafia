package net.sourceforge.kolmafia.textui.parsetree;


public class CompositeType
	extends Type
{
	public CompositeType( final String name, final int type )
	{
		super( name, type );
		this.primitive = false;
	}

	public Type getIndexType()
	{
		return null;
	}

	public Type getDataType()
	{
		return null;
	}

	public Type getDataType( final Object key )
	{
		return null;
	}

	public Value getKey( final Value key )
	{
		return key;
	}

	@Override
	public Value initialValueExpression()
	{
		return new TypeInitializer( this );
	}
}
