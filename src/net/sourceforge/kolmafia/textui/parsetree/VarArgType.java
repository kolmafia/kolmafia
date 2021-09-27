package net.sourceforge.kolmafia.textui.parsetree;

public class VarArgType
	extends AggregateType
{
	public VarArgType( final Type dataType )
	{
		super( "vararg", dataType, 0 );
	}

	@Override
	public String toString()
	{
		return this.dataType.toString() + " ...";
	}
}
