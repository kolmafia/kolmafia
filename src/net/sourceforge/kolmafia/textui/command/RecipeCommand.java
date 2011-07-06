package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RecipeCommand
	extends AbstractCommand
{
	public RecipeCommand()
	{
		this.usage = "<item> [, <item>]... - get ingredients or recipe for items.";
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
				RequestLogger.printLine( "Skipping unknown or ambiguous item: <b>" + c + "</b>" );
				continue;
			}

			AdventureResult item = new AdventureResult( itemId, 1 );
			String name = item.getName();

			int mixingMethod = ConcoctionDatabase.getMixingMethod( itemId );
			if ( mixingMethod == KoLConstants.NOCREATE )
			{
				RequestLogger.printLine( "This item cannot be created: <b>" + name + "</b>" );
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

			RequestLogger.printLine( buffer.toString() );
		}
	}
	
	private static void getIngredients( final AdventureResult ar, final StringBuffer sb )
	{
		sb.append( "<b>" );
		sb.append( ar.getName() );
		sb.append( "</b>: " );

		List ingredients = RecipeCommand.getFlattenedIngredients( ar, new ArrayList() );
		Collections.sort( ingredients );

		Iterator it = ingredients.iterator();
		boolean first = true;
		while ( it.hasNext() )
		{
			AdventureResult ingredient = (AdventureResult) it.next();
			int need = ingredient.getCount();
			int have = InventoryManager.getAccessibleCount( ingredient );
			int missing = need - have;

			if ( !first )
			{
				sb.append( ", " );
			}

			first = false;

			if ( missing < 1 )
			{
				sb.append( ingredient.toString() );
				continue;
			}
		
			sb.append( "<i>" );
			sb.append( ingredient.getName() );
			sb.append( " (" );
			sb.append( String.valueOf( have ) );
			sb.append( "/" );
			sb.append( String.valueOf( need ) );	
			sb.append( ")</i>" );
		}
	}

	private static List getFlattenedIngredients( AdventureResult ar, List list )
	{
		AdventureResult [] ingredients = ConcoctionDatabase.getIngredients( ar.getItemId() );
		for ( int i = 0; i < ingredients.length; ++i )
		{
			AdventureResult ingredient = ingredients[ i ];
			int mixingMethod = ConcoctionDatabase.getMixingMethod( ingredient.getItemId() );
			if ( mixingMethod != KoLConstants.NOCREATE)
			{
				if ( !RecipeCommand.isRecursing( ar, ingredient ) )
				{
					RecipeCommand.getFlattenedIngredients( ingredient, list );
					continue;
				}
			}
			AdventureResult.addResultToList( list, ingredient );
		}

		return list;
	}

	private static boolean isRecursing( final AdventureResult parent, final AdventureResult child )
	{
		if ( parent.equals( child ) )
		{
			// should never actually happen, but eh
			return true;
		}

		int pm = ConcoctionDatabase.getMixingMethod( parent.getItemId() ) & KoLConstants.CT_MASK;
		if ( pm == KoLConstants.ROLLING_PIN )
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
	
	private static void getRecipe( final AdventureResult ar, final StringBuffer sb, final int depth )
	{
		if ( depth > 0 )
		{
			sb.append( "<br>" );
			for ( int i = 0; i < depth; i++ )
			{
				sb.append( "\u00a0\u00a0\u00a0" );
			}
		}
		
		String name = ar.getName();
		
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
}
