/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.IOException;


import javax.swing.JTextArea;
import javax.swing.JTabbedPane;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
 * Holder for the BuffBot log (which should survive outside of
 * the BuffBot Frame
 */

public class BuffBotHome{

	private static final DateFormat logDF =
		DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	private static final SimpleDateFormat logSDF = new SimpleDateFormat("D");

	private KoLmafia client;
	private BuffBotLog BBLog;
	private PrintStream BBLogFile;
	private JTabbedPane advTabs;

	/**
	 * Creates a new instance of <code>BuffBotHome</code>.
	 */

	public BuffBotHome(KoLmafia client){
		this.client = client;
	}

	/**
	 * Create the <code>BuffBotLog and its associated file, if
	 * they don't already exist.
	 */

	public void initialize(){
		if (!(BBLog instanceof BuffBotLog))
			BBLog = new BuffBotLog();

		// First, ensure that a BBlog stream has not already been
		// initialized - this can be checked by observing what
		// class the current log stream is.

		if ( BBLogFile instanceof BuffBotLogStream )
			return;

		try {
			String DayOfYear = logSDF.format(new Date());
			String characterName = client.getLoginName();
			String noExtensionName = characterName.replaceAll( "\\p{Punct}", "" ).replaceAll( " ", "_" ).toLowerCase();
			File f = new File( KoLmafia.DATA_DIRECTORY + noExtensionName + "_BuffBot" + DayOfYear + ".log" );

			if ( !f.exists() )
				f.createNewFile();

			BBLogFile = new BuffBotLogStream( f );
		} catch ( IOException e ) {
			// This should not happen, unless the user
			// security settings are too high to allow
			// programs to write output; therefore,
			// pretend for now that everything works.
		}
	}

	public BuffBotLog getLog(){
		return BBLog;
	}

	/**
	 * For now, this will be a simple JTextArea.
	 * Future upgrades may support formatted text .
	 */
	public class BuffBotLog extends JTextArea {

		private BuffBotLog(){
			this.setEditable( false );
		}

		public void BBLogEntry(String logEntry){
			String ExtendedLogEntry = logDF.format(new Date()) + " " + logEntry + "\n";
			this.append(ExtendedLogEntry);
			BBLogFile.println(ExtendedLogEntry);
//            Not currently adding to system log. Remove comments to do this.
//            if (client != null){
//                client.getLogStream().println("BuffBot:" + ExtendedLogEntry );
//            }
		}

		public void ClearLogDisplay(){
			BBLog.selectAll();
			BBLog.replaceSelection("");
		}
	}

	/**
	 * De-initializes the BBlog stream.  This method is not currently
	 * ever called.
	 * TODO Change to call when BuffBotPanel is closed.
	 */

	public void deinitialize() {
		if ( BBLogFile != null )
			BBLogFile.close();
		BBLogFile = new NullStream();
	}


	/**
	 * An extension of {@link java.io.PrintStream} which handles BuffBot
	 * logging message. All logs generated
	 * by this class will be appended to provided files.
	 */

	private class BuffBotLogStream extends java.io.PrintStream{

		public BuffBotLogStream( String fileName ) throws FileNotFoundException{
			this( new File(fileName ) );
		}
		public BuffBotLogStream( File file ) throws FileNotFoundException{
			super( new FileOutputStream( file, true ) );
			println();
			println();
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println( "          Beginning New BuffBot Logging Session          " );
			println( "                  " + logDF.format(new Date()) + "       " );
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println();
		}
	}

}
