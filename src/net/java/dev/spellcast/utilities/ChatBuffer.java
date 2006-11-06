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
 *		notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *		notice, this list of conditions and the following disclaimer in
 *		the documentation and/or other materials provided with the
 *		distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *		its contributors may be used to endorse or promote products
 *		derived from this software without specific prior written
 *		permission.
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

import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A multi-purpose message buffer which stores all sorts of the messages
 * that can either be displayed or serialized in HTML form.  In essence,
 * this shifts the functionality of processing a chat message from the
 * <code>ChatPanel</code> to an object external to it, which allows more
 * object-oriented design and less complications.
 */

public class ChatBuffer
{
	private DisplayPaneUpdater UPDATER = new DisplayPaneUpdater();
	private static TreeMap activeLogFiles = new TreeMap();

	protected static final int CONTENT_CHANGE = 0;
	protected static final int DISPLAY_CHANGE = 1;
	protected static final int LOGFILE_CHANGE = 2;

	private static final Pattern TRIPLE_LINE_PATTERN = Pattern.compile( "<br>\\s*<br>\\s*(<br>\\s*)+" );

	private String title;
	private String header;
	private String filename;

	protected JEditorPane displayPane;
	protected Runnable scrollBarResizer;
	protected PrintWriter activeLogWriter;
	protected StringBuffer displayBuffer;

	protected static final String EMPTY_STRING = "";
	protected static final String NEW_LINE = System.getProperty( "line.separator" );
	protected static String BUFFER_STYLE = "body { font-family: sans-serif; }";

	/**
	 * Constructs a new <code>ChatBuffer</code>.  However, note that this
	 * does not automatically translate into the messages being displayed;
	 * until a chat display is set, this buffer merely stores the message
	 * content to be displayed.
	 */

	public ChatBuffer( String title )
	{
		this.displayBuffer = new StringBuffer( "<body>" );
		this.title = title;
		this.header = "<html><head>" + NEW_LINE + "<title>" + title + "</title>" + NEW_LINE;
	}

	/**
	 * Method used for clearing the buffer so it can start fresh.  This
	 * is used whenever you want to clear the editor pane from being too
	 * full (in the event that it ever happens).
	 */

	public void clearBuffer()
	{
		displayBuffer.setLength( 0 );
		fireBufferChanged( DISPLAY_CHANGE, null );
	}

	/**
	 * Method used to get the current content of the buffer as a string
	 */

	public String getBuffer()
	{	return displayBuffer.toString();
	}

	/**
	 * Sets the chat display used to display the chat messages currently
	 * being stored in the buffer.  Note that whenever modifications are
	 * made to the buffer, the display will also be modified to reflect
	 * these changes.
	 *
	 * @param	display	The chat display to be used to display incoming messages.
	 */

	public JScrollPane setChatDisplay( JEditorPane display )
	{
		displayPane = display;
		displayPane.setContentType( "text/html" );
		displayPane.setEditable( false );

		displayPane.setText( header + "<style>" + BUFFER_STYLE + "</style></head>" + displayBuffer.toString() + "</body></html>" );
		fireBufferChanged( DISPLAY_CHANGE, null );

		return new JScrollPane( display, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	}

	/**
	 * Sets the log file used to actively record messages that are being
	 * stored in the buffer.  Note that whenever modifications are made to
	 * the buffer, the file will also be modified to reflect these changes.
	 */

	public void setActiveLogFile( String filename )
	{
		if ( filename == null || title == null )
			return;

		if ( this.filename != null && this.filename.equals( filename ) )
			return;

		try
		{
			if ( activeLogFiles.containsKey( filename ) )
			{
				this.filename = filename;
				activeLogWriter = (PrintWriter) activeLogFiles.get( filename );
			}
			else
			{
				File file = new File( filename );
				if ( file.getParentFile() != null )
					file.getParentFile().mkdirs();

				boolean shouldAppend = file.exists();
				if ( !shouldAppend )
					file.createNewFile();

				activeLogWriter = new PrintWriter( new FileOutputStream( file, shouldAppend ), true );

				this.filename = filename;
				activeLogFiles.put( filename, activeLogWriter );

				if ( !shouldAppend )
				{
					updateLogFile( header );
					updateLogFile( "<style>" );
					updateLogFile( BUFFER_STYLE );
					updateLogFile( "</style>" );
				}
			}

			fireBufferChanged( LOGFILE_CHANGE, null );
		}
		catch ( Exception e )
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
			updateLogFile( "</body></html>" );

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
		if ( message == null || message.trim().length() == 0 )
			return;

		fireBufferChanged( CONTENT_CHANGE, message );
	}

	/**
	 * An internal function used to indicate that something has changed with
	 * regards to the <code>ChatBuffer</code>.  This includes any addition
	 * to the chat buffer itself and a change in state, such as changing the
	 * display window or the file to which the data is being logged.
	 */

	protected void fireBufferChanged( int changeType, String newContents )
	{
		if ( changeType != LOGFILE_CHANGE )
		{
			UPDATER.queueUpdate( newContents );

			if ( displayPane != null && !UPDATER.isQueued() )
			{
				UPDATER.markQueued();

				if ( SwingUtilities.isEventDispatchThread() )
					UPDATER.run();
				else
					SwingUtilities.invokeLater( UPDATER );
			}

			if ( changeType == CONTENT_CHANGE && activeLogWriter != null && newContents != null )
				updateLogFile( newContents );
		}

		else if ( activeLogWriter != null )
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
		{
			activeLogWriter.println( chatContent );
			activeLogWriter.flush();
		}
	}

	private static final Pattern OPENBODY_PATTERN = Pattern.compile( "<body", Pattern.CASE_INSENSITIVE );
	private static final Pattern CLOSEBODY_PATTERN = Pattern.compile( "</body>", Pattern.CASE_INSENSITIVE );

	private class DisplayPaneUpdater implements Runnable
	{
		private boolean isQueued = false;
		private boolean shouldReset = false;
		private boolean shouldScroll = true;

		private ArrayList contentQueue = new ArrayList();

		public void queueUpdate( String newContents )
		{
			if ( newContents == null )
			{
				this.shouldReset = true;
				return;
			}

			if ( OPENBODY_PATTERN.matcher( newContents ).find() )
			{
				shouldScroll = false;
				displayBuffer.setLength(0);
				newContents = newContents.substring( newContents.indexOf( ">" ) + 1 );
			}

			if ( displayBuffer.length() == 0 )
			{
				shouldReset = true;
				shouldScroll = false;
			}

			if ( newContents != null )
			{
				newContents = newContents.trim();
				displayBuffer.append( newContents );
			}

			contentQueue.add( newContents );
		}

		public boolean isQueued()
		{	return isQueued;
		}

		public void markQueued()
		{	this.isQueued = true;
		}

		public void run()
		{
			if ( shouldReset )
			{
				contentQueue.clear();
				displayPane.setText( header + "<style>" + BUFFER_STYLE + "</style></head><body>" + displayBuffer.toString() + "</body></html>" );
			}

			while ( !contentQueue.isEmpty() )
				runOnce( (String) contentQueue.remove(0) );

			int length = displayPane.getDocument().getLength();
			displayPane.setCaretPosition( shouldScroll && length > 0 ? length - 1 : 0 );

			this.shouldReset = false;
			this.shouldScroll = true;
			this.isQueued = false;
		}

		private void runOnce( String newContents )
		{
			// This is really the only way to ensure that the
			// screen does not flicker in later versions of Java.

			HTMLDocument currentHTML = (HTMLDocument) displayPane.getDocument();
			Element parentElement = currentHTML.getDefaultRootElement();

			while ( !parentElement.isLeaf() )
				parentElement = parentElement.getElement( parentElement.getElementCount() - 1 );

			try
			{
				currentHTML.insertAfterEnd( parentElement, newContents );
			}
			catch ( Exception e )
			{
				// If there's an exception, continue onward so that you
				// still have an updated display.  But, print the stack
				// trace so you know what's going on.

				e.printStackTrace();
			}
		}
	}
}
