package net.sourceforge.kolmafia.combat;

import java.io.PrintStream;

import javax.swing.tree.DefaultMutableTreeNode;

public class CustomCombatAction
	extends DefaultMutableTreeNode
{
	private final int index;
	private final String action;
	private final String indent;
	private final String actionString;

	private final boolean isMacro;
	private final String sectionReference;

	public CustomCombatAction( final int index, final String indent, final String action, boolean isMacro )
	{
		super( action, false );

		this.index = index;
		this.indent = indent;
		this.isMacro = isMacro;

		if ( isMacro )
		{
			if ( CombatActionManager.isMacroAction( action ) )
			{
				this.action = action;
			}
			else
			{
				this.action = "\"" + action + "\"";
			}
		}
		else
		{
			this.action = CombatActionManager.getLongCombatOptionName( action );
		}

		String actionString = this.index + ": " + this.action.replaceAll( "\\s+", " " );

		this.actionString = actionString;

		if ( this.action.equals( "default" ) )
		{
			this.sectionReference = "default";
		}
		else if ( this.action.startsWith( "section" ) )
		{
			this.sectionReference = CombatActionManager.encounterKey( this.action.substring( 8 ).trim().toLowerCase() );
		}
		else
		{
			this.sectionReference = null;
		}
	}

	public String getAction()
	{
		return this.action;
	}

	public boolean isMacro()
	{
		return this.isMacro;
	}
	
	public String getSectionReference()
	{
		return this.sectionReference;
	}

	@Override
	public String toString()
	{
		return this.actionString;
	}

	public void store( PrintStream writer )
	{
		writer.print( this.indent );
		writer.print( this.action );

		writer.println();
	}
}
