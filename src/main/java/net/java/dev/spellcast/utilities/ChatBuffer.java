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
 * Modified by the KoLmafia development team
 */

package net.java.dev.spellcast.utilities;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * A multi-purpose message buffer which stores all sorts of the messages that can either be displayed or serialized in
 * HTML form. In essence, this shifts the functionality of processing a chat message from the <code>ChatPanel</code> to
 * an object external to it, which allows more object-oriented design and less complications.
 */

public class ChatBuffer
{
	private static final Pattern TAG_PATTERN = Pattern.compile( "<\\s*([^\\s>]+)(.*?)>" );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--(.*?)-->" );

	private final String title;

	private final Deque<String> entries = new ArrayDeque<>();
	private int contentLength = 0;

	private final Set<JEditorPane> displayPanes = ChatBuffer.weakSet();
	private final Set<JEditorPane> stickyPanes = ChatBuffer.weakSet();
	private final Set<JEditorPane> stalePanes = ChatBuffer.weakSet();

	private int pendingCount = 0;
	private int pendingRemovals = 0;
	private boolean flushScheduled = false;

	private File logFile;
	private PrintWriter logWriter;

	protected static final HashMap<String, PrintWriter> ACTIVE_LOG_FILES = new HashMap<>();

	private static final int MAXIMUM_LENGTH = 50000;
	private static final int TRIM_TO_LENGTH = 45000;
	private static final int MINIMUM_ENTRIES = 100;
	private static final int MAXIMUM_RETAINED_LENGTH = 200000;

	/**
	 * Constructs a new <code>ChatBuffer</code>. However, note that this does not automatically translate into the
	 * messages being displayed; until a chat display is set, this buffer merely stores the message content to be
	 * displayed.
	 */

	public ChatBuffer( final String title )
	{
		this.title = title;
	}

	/**
	 * Adds a chat display used to display the chat messages currently being stored in the buffer.
	 */

	public JScrollPane addDisplay( final JEditorPane displayPane )
	{
		if ( displayPane == null )
		{
			return null;
		}

		displayPane.setContentType( "text/html" );
		displayPane.setEditable( false );
		displayPane.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// In java 21, a change was made to always render the caret's position despite text being uneditable
				// This isn't as useful for us as we automatically scroll to bottom
				// https://bugs.openjdk.org/browse/JDK-4512626
				displayPane.getCaret().setVisible(false);
			}

			@Override
			public void focusLost(FocusEvent e) {
			}
		});

		displayPane.addHierarchyListener( e ->
		{
			if ( ( e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED ) != 0 && displayPane.isDisplayable() )
			{
				SwingUtilities.invokeLater( () -> this.markStale( displayPane ) );
			}
		} );

		SwingUtilities.invokeLater( () ->
		{
			this.displayPanes.add( displayPane );
			this.stickyPanes.add( displayPane );
			this.markStale( displayPane );
		} );

		JScrollPane scroller =
			new JScrollPane(
				displayPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		return scroller;
	}

	/**
	 * Sets the log file used to actively record messages that are being stored in the buffer.
	 */

	public void setLogFile( final File f )
	{
		if ( f == null || this.title == null )
		{
			return;
		}

		String filename = f.getPath();

		if ( filename == null || this.title == null )
		{
			return;
		}

		if ( ChatBuffer.ACTIVE_LOG_FILES.containsKey( filename ) )
		{
			this.logFile = f;
			this.logWriter = ChatBuffer.ACTIVE_LOG_FILES.get( filename );
		}
		else
		{
			boolean shouldAppend = f.exists();
			this.logFile = f;
			this.logWriter = new PrintWriter( DataUtilities.getOutputStream( f, shouldAppend ), true , StandardCharsets.UTF_8 );

			ChatBuffer.ACTIVE_LOG_FILES.put( filename, this.logWriter );

			if ( !shouldAppend )
			{
				this.logWriter.println( "<html><head>" );
				this.logWriter.println( "<title>" );
				this.logWriter.println( this.title );
				this.logWriter.println( "</title>" );
				this.logWriter.println( "<style>" );
				this.logWriter.println( this.getStyle() );
				this.logWriter.println( "</style>" );
				this.logWriter.println( "<body>" );
			}
		}
	}

	/**
	 * Closes the buffer.
	 */

	public void dispose()
	{
		SwingUtilities.invokeLater( () ->
		{
			this.displayPanes.clear();
			this.stickyPanes.clear();
			this.stalePanes.clear();
		} );

		if ( this.logWriter != null )
		{
			this.logWriter.close();
		}

		this.clear();
	}

	private static void printHTML( final HTMLDocument doc )
	{
		HTMLEditorKit kit = new HTMLEditorKit();
		StringWriter writer = new StringWriter();
		try
		{
			kit.write(writer, doc, 0, doc.getLength());
		}
		catch ( Exception e )
		{
		}
		String s = writer.toString();
		System.out.println( "HTML = \"" + s + "\"" );
	}

	/**
	 * Clears the current buffer content.
	 */

	public void clear()
	{
		SwingUtilities.invokeLater( () ->
		{
			this.entries.clear();
			this.contentLength = 0;

			this.requestReset();
		} );
	}

	public File getLogFile() {
		return this.logFile;
	}

	/**
	 * Appends the given contents to the chat buffer.
	 */

	public void append( String newContents )
	{
		if ( newContents == null )
		{
			SwingUtilities.invokeLater( this::requestReset );
			return;
		}

		String entry = newContents.trim();

		if ( entry.length() == 0 )
		{
			return;
		}

		if ( this.logWriter != null )
		{
			this.logWriter.println( entry );
		}

		SwingUtilities.invokeLater( () -> this.addEntry( entry ) );
	}

	private void addEntry( final String entry )
	{
		this.entries.addLast( entry );
		this.contentLength += entry.length();

		this.pendingCount++;

		if ( this.contentLength >= ChatBuffer.MAXIMUM_LENGTH )
		{
			this.trim();
		}

		this.scheduleFlush();
	}

	/**
	 * Returns the styling used by this buffer.
	 */

	public String getStyle()
	{
		return "body { font-family: sans-serif; }";
	}

	/**
	 * Returns all the content stored within this chat buffer.
	 */

	public String getContent()
	{
		return String.join( "", this.entries );
	}

	/**
	 * Returns all the styled content stored within this chat buffer.
	 */

	public String getHTMLContent()
	{
		StringBuffer htmlContent = new StringBuffer();

		htmlContent.append( "<html><head><style>" );
		htmlContent.append( this.getStyle() );
		htmlContent.append( "</style></head><body>" );

		htmlContent.append( ChatBuffer.wrapEntries( this.entries ) );

		htmlContent.append( "</body></html>" );

		return htmlContent.toString();
	}

	private static String wrapEntries( final Iterable<String> entries )
	{
		StringBuilder html = new StringBuilder();

		for ( String entry : entries )
		{
			html.append( "<div>" ).append( ChatBuffer.balanceTags( entry ) ).append( "</div>" );
		}

		return html.toString();
	}

	public void setSticky( JEditorPane editor, boolean sticky )
	{
		SwingUtilities.invokeLater( () ->
		{
			if ( sticky )
			{
				this.stickyPanes.add( editor );
			}
			else
			{
				this.stickyPanes.remove( editor );
			}
		} );
	}

	private static Set<JEditorPane> weakSet()
	{
		return Collections.newSetFromMap( new WeakHashMap<>() );
	}

	private void markStale( final JEditorPane displayPane )
	{
		this.stalePanes.add( displayPane );
		this.scheduleFlush();
	}

	private void requestReset()
	{
		this.pendingCount = 0;
		this.pendingRemovals = 0;
		this.stalePanes.addAll( this.displayPanes );
		this.scheduleFlush();
	}

	private void trim()
	{
		while ( this.entries.size() > 1 && this.contentLength > ChatBuffer.TRIM_TO_LENGTH
			&& ( this.entries.size() > ChatBuffer.MINIMUM_ENTRIES || this.contentLength > ChatBuffer.MAXIMUM_RETAINED_LENGTH ) )
		{
			if ( this.pendingCount == this.entries.size() )
			{
				this.pendingCount--;
			}
			else
			{
				this.pendingRemovals++;
			}

			this.contentLength -= this.entries.removeFirst().length();
		}
	}

	private void scheduleFlush()
	{
		if ( this.flushScheduled )
		{
			return;
		}

		this.flushScheduled = true;
		SwingUtilities.invokeLater( this::flush );
	}

	private void flush()
	{
		int removals = this.pendingRemovals;
		String added = ChatBuffer.wrapEntries( this.entries.stream().skip( this.entries.size() - this.pendingCount ).toList() );
		String htmlContent = null;

		this.pendingRemovals = 0;
		this.pendingCount = 0;
		this.flushScheduled = false;

		for ( JEditorPane displayPane : this.displayPanes )
		{
			if ( !displayPane.isDisplayable() )
			{
				continue;
			}

			if ( this.stalePanes.remove( displayPane ) || !ChatBuffer.update( displayPane, removals, added ) )
			{
				if ( htmlContent == null )
				{
					htmlContent = this.getHTMLContent();
				}

				displayPane.setText( htmlContent );
			}

			// Non-ASCII text sets "multiByte", which switches to a much slower bidi-aware layout.
			displayPane.getDocument().putProperty( "multiByte", Boolean.FALSE );
		}

		for ( JEditorPane stickyPane : this.stickyPanes )
		{
			if ( !stickyPane.isDisplayable() )
			{
				continue;
			}

			int contentLength = stickyPane.getDocument().getLength();

			int caretPosition = Math.max( contentLength - 1, 0 );

			stickyPane.setCaretPosition( caretPosition );
		}
	}

	private static boolean update( final JEditorPane displayPane, final int removals, final String added )
	{
		HTMLDocument currentHTML = (HTMLDocument) displayPane.getDocument();
		Element body = currentHTML.getElement( currentHTML.getDefaultRootElement(), StyleConstants.NameAttribute, HTML.Tag.BODY );

		if ( body == null )
		{
			return false;
		}

		List<Element> entryElements = new ArrayList<>();

		for ( int i = 0; i < body.getElementCount() && entryElements.size() <= removals; i++ )
		{
			Element child = body.getElement( i );

			if ( child.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.DIV )
			{
				entryElements.add( child );
			}
		}

		if ( entryElements.size() <= removals )
		{
			return false;
		}

		try
		{
			for ( int i = 0; i < removals; i++ )
			{
				currentHTML.removeElement( entryElements.get( i ) );
			}

			if ( !added.isEmpty() )
			{
				currentHTML.insertBeforeEnd( body, added );
			}
		}
		catch ( BadLocationException | IOException e )
		{
			return false;
		}

		// ChatBuffer.printHTML( currentHTML );

		return true;
	}

	static String balanceTags( final String newContent )
	{
		// Check for imbalanced HTML here

		Stack<String> openTags = new Stack<>();
		Set<String> skippedTags = new HashSet<>();
		StringBuffer buffer = new StringBuffer();

		String noCommentsContent = COMMENT_PATTERN.matcher( newContent ).replaceAll( "" );

		Matcher tagMatcher = TAG_PATTERN.matcher( noCommentsContent );

		while ( tagMatcher.find() )
		{
			String tagName = tagMatcher.group( 1 );
			StringBuffer replacement = new StringBuffer();

			if ( tagName.startsWith( "/" ) )
			{
				String closeTag = tagName.substring( 1 );

				if ( skippedTags.contains( closeTag ) )
				{
					skippedTags.remove( closeTag );
				}
				else
				{
					while ( !openTags.isEmpty() )
					{
						String openTag = openTags.pop();
						replacement.append( "</" );
						replacement.append( openTag );
						replacement.append( ">" );

						if ( openTag.equalsIgnoreCase( closeTag ) )
						{
							break;
						}
						else if ( skippedTags.contains( closeTag ) )
						{
							skippedTags.remove( closeTag );
							break;
						}
						else
						{
							skippedTags.add( closeTag );
						}
					}
				}
			}
			else
			{
				if ( !tagName.equalsIgnoreCase( "br" ) )
				{
					openTags.push( tagName );
				}

				replacement.append( "<$1$2>" );
			}

			tagMatcher.appendReplacement( buffer, replacement.toString() );
		}

		tagMatcher.appendTail( buffer );

		while ( !openTags.isEmpty() )
		{
			String openTag = openTags.pop();
			buffer.append( "</" );
			buffer.append( openTag );
			buffer.append( ">" );
		}

		return buffer.toString();
	}
}
