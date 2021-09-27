package net.sourceforge.kolmafia.textui.parsetree;
import net.sourceforge.kolmafia.textui.DataTypes;

public class TypeDef
	extends Type
{
	Type base;

	public TypeDef( final String name, final Type base )
	{
		super( name, DataTypes.TYPE_TYPEDEF );
		this.base = base;
	}

	@Override
	public Type getBaseType()
	{
		return this.base.getBaseType();
	}

	@Override
	public Value initialValue()
	{
		return this.base.initialValue();
	}

	@Override
	public Value parseValue( final String name, final boolean returnDefault )
	{
		return this.base.parseValue( name, returnDefault );
	}

	@Override
	public Value initialValueExpression()
	{
		return new TypeInitializer( this.base.getBaseType() );
	}

	@Override
	public boolean equals( final Type o )
	{
		return o instanceof TypeDef && this.name.equals( o.name );
	}
}
