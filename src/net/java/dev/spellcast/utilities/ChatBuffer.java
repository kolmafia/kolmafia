/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

import java.awt.Color;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * A multi-purpose message buffer which stores all sorts of the messages
 * that can either be displayed or serialized in HTML form.  In essence,
 * this shifts the functionality of processing a chat message from the
 * <code>ChatPanel</code> to an object external to it, which allows more
 * object-oriented design and less complications.
 */

public class ChatBuffer
{
	private static final int CONTENT_CHANGE = 0;
	private static final int DISPLAY_CHANGE = 1;
	private static final int LOGFILE_CHANGE = 2;

	private StringBuffer displayBuffer;
	private JEditorPane displayPane;
	private PrintWriter activeLogWriter;

	private static final String EMPTY_STRING = "";
	private static final String NEW_LINE = System.getProperty( "line.separator" );
	private static final String BUFFER_INIT = "<body style=\"font-family: sans-serif; font-size:12pt;\">";
	private static final String BUFFER_STOP = "</body>";

	/**
	 * Constructs a new <code>ChatBuffer</code>.  However, note that this
	 * does not automatically translate into the messages being displayed;
	 * until a chat display is set, this buffer merely stores the message
	 * content to be displayed.
	 */

	public ChatBuffer( String title )
	{
		displayBuffer = new StringBuffer();

		displayBuffer.append( "<html><head>" + NEW_LINE );
		displayBuffer.append( "<title>" + title + "</title>" + NEW_LINE );
		displayBuffer.append( "</head>" + NEW_LINE + NEW_LINE );
		displayBuffer.append( BUFFER_INIT + NEW_LINE );
	}

	/**
	 * Sets the chat display used to display the chat messages currently
	 * being stored in the buffer.  Note that whenever modifications are
	 * made to the buffer, the display will also be modified to reflect
	 * these changes.
	 *
	 * @param	display	The chat display to be used to display incoming messages.
	 */

	public void setChatDisplay( JEditorPane display )
	{
		displayPane = display;
		fireBufferChanged( DISPLAY_CHANGE, null );
	}

	/**
	 * Sets the log file used to actively record messages that are being
	 * stored in the buffer.  Note that whenever modifications are made to
	 * the buffer, the file will also be modified to reflect these changes.
	 */

	public void setActiveLogFile( String filename, String title )
	{
		try
		{
			activeLogWriter = new PrintWriter( new FileOutputStream( filename, false ), true );
			fireBufferChanged( LOGFILE_CHANGE, null );
		}
		catch ( java.io.FileNotFoundException e )
		{	throw new RuntimeException( "The file <" + filename + "> could not be opened for writing" );
		}
		catch ( SecurityException e )
		{	throw new RuntimeException( "The file <" + filename + "> could not be opened for writing" );
		}
	}

	/**
	 * Closes the log file used to actively record messages that are
	 * being stored in the buffer.  This formally closes the file and
	 * sets the log file currently being used to null so that no future
	 * updates are attempted.
	 */

	public void closeActiveLogFile()
	{
		if ( activeLogWriter != null )
		{
			updateLogFile( NEW_LINE + NEW_LINE );
			updateLogFile( BUFFER_STOP + "</html>" );

			activeLogWriter.close();
			activeLogWriter = null;
		}
	}

	/**
	 * Appends the given <code>SpellcastMessage</code> to the chat buffer.  Note
	 * that though the parameter allows for <i>any</i> <code>SpellcastMessage</code>
	 * to be appended to the buffer, the truth is, only pre-specified messages will
	 * actually be displayed while others will simply be ignored.
	 *
	 * @param	message	The message to be appended to this <code>ChatBuffer</code>
	 */

	public void append( String message )
	{
		if ( message == null || message.equals( EMPTY_STRING ) )
			return;
		fireBufferChanged( CONTENT_CHANGE, message );
	}

	/**
	 * An internal function used to indicate that something has changed with
	 * regards to the <code>ChatBuffer</code>.  This includes any addition
	 * to the chat buffer itself and a change in state, such as changing the
	 * display window or the file to which the data is being logged.
	 *
	 * @param	message	The HTML message which needs to be finalized
	 */

	private void fireBufferChanged( int changeType, String newContents )
	{
		if ( newContents != null )
			displayBuffer.append( newContents );

		if ( changeType != LOGFILE_CHANGE && displayPane != null )
			(new DisplayUpdate()).run();

		if ( changeType == CONTENT_CHANGE && activeLogWriter != null && !newContents.equals( null ) )
			updateLogFile( newContents );

		if ( changeType == LOGFILE_CHANGE && activeLogWriter != null )
			updateLogFile( displayBuffer.toString() );
	}

	/**
	 * An internal module used to update the log file.  This method basically
	 * appends the given string to the current logfile.  However, because many
	 * things may wish to update a log file, it is useful to have an extra
	 * module whose job is merely to write data to the current log file.
	 *
	 * @param	chatContent	The content to be written to the file
	 */

	private void updateLogFile( String chatContent )
	{
		if ( activeLogWriter != null )
			activeLogWriter.println( chatContent );
	}

	/**
	 * An internal class used to update the display.  In order to run the display
	 * update, all that is needed is to create a new instance of this class and
	 * call the <code>run()</code> method, which ensures that all of the modifications
	 * to the display occur in the event dispatch thread.
	 */

	private class DisplayUpdate implements Runnable
	{
		public void run()
		{
			// ensure that this is the correct thread
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			// update the display
			displayPane.setContentType( "text/html" );
			displayPane.setText( BUFFER_INIT + displayBuffer.toString() + BUFFER_STOP );
		}
	}
}