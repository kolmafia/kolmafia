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
 *
 */
public class BuffBotHome {
	private BuffBotLog BBLog;
	private DateFormat LogDF;
	private KoLmafia client;
	private PrintStream BBLogFile;
	private JTabbedPane advTabs;
	private String LogFileName;
	
	/** Creates a new instance of BuffBotHome */
	public BuffBotHome(KoLmafia client) {
		this.client = client;
		LogDF = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		LogFileName = "BuffBot??.log";
		
	}
	
	/** create the BuffBotLog (<code>BBLog</code>) and the
	 *      BuffBotLogFile (<code>BBLogFile</code>)
	 *      if they don't already exist
	 */
	public void initializeBBLogs(){
		if (!(BBLog instanceof BuffBotLog)){
			BBLog = new BuffBotLog();
		}
		initializeBBLogFile();
		BBLog.append("Today's Log: " + LogFileName + " \n");
	}
	
	public BuffBotLog getBBLog(){
		return BBLog;
	}
	
	public void setAdventureTabs(JTabbedPane tabs){
		advTabs = tabs;
	}
	
	public JTabbedPane getAdventureTabs(){
		return advTabs;
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
			Date now = new Date();
			String ExtendedLogEntry = LogDF.format(now) + " " + logEntry + "\n";
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
	 * Initializes a stream for logging debugging information.  This
	 * method creates a <code>BuffBot.log</code> file in the default
	 * data directory if one does not exist, or appends to the existing
	 * log. A new file is opened each day.
	 */
	
	public void initializeBBLogFile() {
		// First, ensure that a BBlog stream has not already been
		// initialized - this can be checked by observing what
		// class the current log stream is.
		
		if ( BBLogFile instanceof BBLogFileClass )
			return;
		
		try {
			String DayOfYear = (new SimpleDateFormat("D")).format(new Date());
			String characterName = client.getLoginName();
			String noExtensionName = characterName.replaceAll( "\\p{Punct}", "" ).replaceAll( " ", "_" );
			File f = new File( KoLmafia.DATA_DIRECTORY + noExtensionName + "_BuffBot" + DayOfYear + ".log" );
			
			LogFileName = f.getName();
			if ( !f.exists() )
				f.createNewFile();
			
			BBLogFile = new BBLogFileClass( f );
		} catch ( IOException e ) {
			// This should not happen, unless the user
			// security settings are too high to allow
			// programs to write output; therefore,
			// pretend for now that everything works.
		}
	}
	
	/**
	 * De-initializes the BBlog stream.  This method is not currently
	 * ever called.
	 * TODO Change to call when BuffBotPanel is closed.
	 */
	
	public void deinitializeBBLogFile() {
		if ( BBLogFile != null )
			BBLogFile.close();
		BBLogFile = new NullStream();
	}
	
	
	/**
	 * An extension of {@link java.io.PrintStream} which handles BuffBot
	 * logging message. All logs generated
	 * by this class will be appended to provided files.
	 */
	public class BBLogFileClass extends java.io.PrintStream{
		
		public BBLogFileClass( String fileName ) throws FileNotFoundException{
			this( new File(fileName ) );
		}
		public BBLogFileClass( File file ) throws FileNotFoundException{
			super( new FileOutputStream( file, true ) );
			println();
			println();
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println( "                  Beginning New BuffBot Logging Session          " );
			println( "                          " + LogDF.format(new Date()) + "       " );
			println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			println();
		}
	}
	
}





