package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collection;

import net.sourceforge.kolmafia.utilities.FileUtilities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith( Parameterized.class )
public class DataFileMechanicsTest {
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				//file name, version number, low field count, high field count
				{"adventures.txt", 6, 4, 5},
				{"bounty.txt", 2, 7, 7},
				{"buffbots.txt", 1, 3, 3},
				{"cafe_booze.txt", 1, 2, 2},
				{"cafe_food.txt", 1, 2, 2},
				{"classskills.txt", 4, 6, 7},
				{"coinmasters.txt", 2, 4, 5},
				//combats.txt too complex
				//concoctions.txt too complex
				//consequences.txt too complex
				//cultshorts.txt too complex
				//defaults.txt too complex
				{"encounters.txt", 1, 3, 3},
				{"equipment.txt", 2, 3, 4},
				{"fambattle.txt", 1, 8, 8},
				{"familiars.txt", 4, 10, 11},
				{"faxbots.txt", 1, 2, 2},
				//foldgroups.txt is too complex
				{"fullness.txt", 2, 8, 9},
				{"inebriety.txt", 2, 8, 10},
				{"items.txt", 1, 7, 8},
				{"mallprices.txt", 1, 3, 3},  //not normally manually edited, but...
				//modifiers.txt is too complex
				//monsters.txt is too complex
				{"nonfilling.txt", 1, 2, 3},
				{"npcstores.txt", 2, 4, 5},
				// Trick-or-treat candy is optional if too complicated
				{"outfits.txt", 3, 4, 5},
				{"packages.txt", 1, 4, 4},
				{"pulverize.txt", 2, 2, 2},
				{"questscouncil.txt", 1, 3, 5},
				//questslogs.txt is too complex
				{"restores.txt", 2, 7, 8},
				{"spleenhit.txt", 1, 8, 9},
				{"statuseffects.txt", 1, 6, 7},
				{"TCRS.astral_consumables.txt", 0, 4, 4},
				{"TCRS.astral_pets.txt", 0, 4, 4},
				//zapgroups.txt is too simple
				{"zonelist.txt", 1, 3, 3},
		});
	}

	private final String fname;
	private final int version;
	private final int lowCount;
	private final int highCount;

	//This is a simplistic test that just counts tab delimited fields in src/data.  It is primarily
	//expected to catch mechanical issues such as using spaces instead of tabs and does not look at
	//content.  It is possible that an error would remain undetected in files where the low and high
	//counts are not the same.
	public DataFileMechanicsTest(String fname, int version, int lowCount, int highCount) {
		this.fname = fname;
		this.version = version;
		this.lowCount = lowCount;
		this.highCount = highCount;
	}

	//Field counts that are not really an error.
	boolean skipMe(int checkVal) {
		if (checkVal == 0) return true;
		return checkVal == 1;
	}

	//This will only catch data entry errors when editing the test parameters.
	boolean precheck()
	{
		if (lowCount <= 1) return false;
		return highCount >= lowCount;
		//Need to check that file exists, is internal and not an external file overriding an
		//internal file and that the file has the expected version.  FileUtilities doesn't
		//really expose that information to a test.
	}

	@Test
	public void testDataFileFieldCounts()
	{
		//If the precheck fails then the parameters in this test file are wrong and this file needs
		//to be edited.
		assertTrue(fname+" failed precheck.", precheck());
		//FileUtilities will log "errors" but tries very hard to return a reader no matter what.
		BufferedReader reader = FileUtilities.getVersionedReader( fname, version );
		String[] fields;
		boolean noLines = true;
		while ( ( fields = FileUtilities.readData( reader ) ) != null ) {
			noLines = false;
			int fieldsRead = fields.length;
			if (skipMe(fieldsRead)) continue;
			String msg = fname + " " + fields[0];
			//Line has too many or too few fields.
			assertThat(msg, fieldsRead, allOf(greaterThanOrEqualTo(lowCount), lessThanOrEqualTo(highCount)));
		}
		//No lines is sometimes a symptom caused by a bad file name.
		assertFalse("No lines in "+fname, noLines);
		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			fail( "Exception in tearing down reader:" + e.toString() );
		}
	}
}
