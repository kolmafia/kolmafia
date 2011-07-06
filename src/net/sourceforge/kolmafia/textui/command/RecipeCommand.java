package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RecipeCommand
	extends AbstractCommand
{
	public RecipeCommand()
	{
		this.usage = "[<item>] - get ingredients of recipe for item.";
	}

	public void run( final String cmd, final String params )
	{
		String[] concoctions = params.split( "\\s*,\\s*" );
		
		if ( concoctions.length == 0 )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();

		for ( int i = 0; i < concoctions.length; ++i )
		{
			String c = concoctions[ i ];
			int itemId = ItemDatabase.getItemId( c );

			if ( itemId == -1 )
			{
				KoLmafia.updateDisplay( "Skipping unknown or ambiguous item: <b>" + c + "</b>" );
				continue;
			}

			AdventureResult item = new AdventureResult( itemId, 1 );
			String name = item.getName();

			int mixingMethod = ConcoctionDatabase.getMixingMethod( itemId );
			if ( mixingMethod == KoLConstants.NOCREATE )
			{
				KoLmafia.updateDisplay( "This item cannot be created: <b>" + name + "</b>" );
				continue;
			}

			buffer.setLength( 0 );
			if ( concoctions.length > 1 )
			{
				buffer.append( String.valueOf( i + 1 ) );
				buffer.append( ". " );
			}

			if ( cmd.equals( "ingredients" ) )
			{
				RecipeCommand.getIngredients( item, buffer );
			}
			else if ( cmd.equals( "recipe" ) )
			{
				RecipeCommand.getRecipe( item, buffer, 0 );
			}

			KoLmafia.updateDisplay( buffer.toString() );
		}
	}
	
	private static void getIngredients( final AdventureResult item, final StringBuffer sb )
	{
		List ingredients = RecipeCommand.getFlattenedIngredients( item, new ArrayList() );
		Collections.sort( ingredients );

		sb.append( "<b>" );
		sb.append( item.getName() );
		sb.append( "</b>: " );

		Iterator it = ingredients.iterator();
		boolean first = true;
		while ( it.hasNext() )
		{
			AdventureResult ar = (AdventureResult) it.next();
			int need = ar.getCount();
			int have = InventoryManager.getAccessibleCount( ar );
			int missing = need - have;

			if ( !first )
			{
				sb.append( ", " );
			}

			first = false;

			if ( missing < 1 )
			{
				sb.append( ar.toString() );
				continue;
			}
		
			sb.append( "<i>" );
			sb.append( ar.getName() );
			sb.append( " (" );
			sb.append( String.valueOf( have ) );
			sb.append( "/" );
			sb.append( String.valueOf( need ) );	
			sb.append( ")</i>" );
		}
	}

	private static List getFlattenedIngredients( AdventureResult item, List list )
	{
		AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( item.getItemId() );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ar = ingredients[ i ];
			if ( RecipeCommand.isRecursing( item, ar ) )
			{
				continue;
			}

			int mixingMethod = ConcoctionDatabase.getMixingMethod( ar.getItemId() );
			if ( mixingMethod != KoLConstants.NOCREATE)
			{
				RecipeCommand.getFlattenedIngredients( ar, list );
			}
			else
			{
				AdventureResult.addResultToList( list, ar );
			}
		}

		return list;
	}
	
	private static void getRecipe( final AdventureResult ar, final StringBuffer sb, final int depth )
	{
		String name = ar.getName();
		
		if ( depth > 0 )
		{
			sb.append( "<br>" );
			for ( int i = 0; i < depth; i++ )
			{
				sb.append( "\u00a0\u00a0\u00a0" );
			}
		}
		
		sb.append( "<b>" );
		sb.append( name );
		sb.append( "</b>" );
		
		int mixingMethod = ConcoctionDatabase.getMixingMethod( name );
		if ( mixingMethod != KoLConstants.NOCREATE )
		{
			sb.append( "<b>:</b> <i>[" );
			sb.append( ConcoctionDatabase.mixingMethodDescription( mixingMethod ) );
			sb.append( "]</i> " );

			AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( name );
			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( i > 0 )
				{
					sb.append( " + " );
				}
				sb.append( ingredient.getName() );
			}

			for ( int i = 0; i < ingredients.length; ++i )
			{
				AdventureResult ingredient = ingredients[ i ];
				if ( RecipeCommand.isRecursing( ar, ingredient ) )
				{
					continue;
				}
				RecipeCommand.getRecipe( ingredient, sb, depth + 1 );
			}
		}
	}

	private static boolean isRecursing( final AdventureResult parent, final AdventureResult child )
	{
		if ( parent.equals( child ) )
		{
			// should never actually happen, but eh
			return true;
		}

		int pm = ConcoctionDatabase.getMixingMethod( parent.getItemId() ) & KoLConstants.CT_MASK;
		int cm = ConcoctionDatabase.getMixingMethod( child.getItemId() ) & KoLConstants.CT_MASK;

		if ( pm == KoLConstants.ROLLING_PIN && cm == KoLConstants.ROLLING_PIN )
		{
			return true;
		}
		
		AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( child.getItemId() );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			if ( ingredients[ i ].equals( parent ) )
			{
				return true;
			}
		}
		
		return false;
	}
}
