package net.sourceforge.kolmafia;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.FileUtilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;

/* Checks for consistency across datafiles.

   For instance, items marked as equipment in items.txt should also have
   corresponding entries in equipment.txt.
 */
public class DataFileConsistencyTest
{
	@Rule
	public ErrorCollector collector = new ErrorCollector();

	Set<String> datafileItems( String file, int version ) throws IOException
	{
		Set<String> items = new HashSet<String>();
		try ( BufferedReader reader = FileUtilities.getVersionedReader( file, version ) )
		{
			String[] fields;
			while ( ( fields = FileUtilities.readData( reader ) ) != null )
			{
				if ( fields.length == 1 )
				{
					continue;
				}
				items.add( fields[ 0 ] );
			}
		}
		return items;
	}

	List<Integer> allItems()
	{
		ArrayList<Integer> items = new ArrayList<Integer>();

		int limit = ItemDatabase.maxItemId();
		for ( int i = 1; i <= limit; ++i )
		{
			String name = ItemDatabase.getItemDataName( i );
			if ( i != 13 && name != null )
			{
				items.add( i );
			}
		}

		return items;
	}

	@Test
	public void testEquipmentPresence()
	{
		Set<String> equipment;
		try
		{
			equipment = datafileItems( "equipment.txt", 2 );
		}
		catch ( IOException exception )
		{
			fail( "failed initialization of equipment.txt" );
			return;
		}
		List<Integer> items = allItems();

		for ( int id : items )
		{
			// Familiar equipment is not present in equipment.txt...
			if ( ItemDatabase.isEquipment( id ) && !ItemDatabase.isFamiliarEquipment( id ) )
			{
				// At least one of "seal-clubbing club", "[1]seal-clubbing club" should be present.
				String name = ItemDatabase.getItemDataName( id );
				String bracketedName = "[" + id + "]" + name;
				collector.checkThat( bracketedName + " is not present in equipment.txt",
									 true,
									 // Explicitly apply the matcher to keep the error message manageable.
									 equalTo( anyOf( hasItem( name ), hasItem( bracketedName ) ).matches( equipment ) ) );
			}
		}
	}
}
