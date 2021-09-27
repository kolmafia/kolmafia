package net.sourceforge.kolmafia.textui.parsetree;

public abstract class Symbol
	extends ParseTreeNode
	implements Comparable<Symbol>
{
	public String name;

	public Symbol()
	{
	}

	public Symbol( final String name )
	{
		this.name = name;
	}

	public String getName()
	{
		return this.name;
	}

	public int compareTo( final Symbol o )
	{
		if ( !( o instanceof Symbol ) )
		{
			throw new ClassCastException();
		}
		if ( this.name == null )
		{
			return 1;
		}
		return this.name.compareToIgnoreCase( o.name );
	}
}
