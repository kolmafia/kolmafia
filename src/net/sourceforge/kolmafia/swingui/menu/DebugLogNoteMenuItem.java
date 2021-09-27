package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class DebugLogNoteMenuItem
	extends ThreadedMenuItem
{
	public DebugLogNoteMenuItem()
	{
		super( "Add Note to Debug Log", new DebugLogNoteListener() );
	}
	private static class DebugLogNoteListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{			
		//Pop up a dialog to get a note, and then send it to the debug log.
		String userNote = InputFieldUtilities.input( "Enter note for debug log" );
		if (userNote == null || userNote.equals("")) return;
		java.util.Date noteTime = new java.util.Date();
		if ( ! RequestLogger.isDebugging() )
			{
				RequestLogger.openDebugLog();
				RequestLogger.updateDebugLog( "-----User Note: "
						+ noteTime
						+ "-----\n"
						+ userNote
						+ "\n-----" );

				RequestLogger.closeDebugLog();
			}
			else 
			{
				RequestLogger.updateDebugLog( "-----User Note: "
						+ noteTime
						+ "-----\n"
						+ userNote
						+ "\n-----" );
			
			}

		
		}
	}
}
