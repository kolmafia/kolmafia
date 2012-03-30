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
 * Copyright (c) 2005-2010, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *		without specific prior written permission.
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

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

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

	private final StringBuffer content = new StringBuffer();
	private final LinkedList displayPanes = new LinkedList();

	private final Set stickyPanes = new LinkedHashSet();
	private final LinkedList addStickyPanes = new LinkedList();
	private final LinkedList removeStickyPanes = new LinkedList();

	private volatile int resetSequence = 0;

	// Every queued update for this ChatBuffer carries the then-current value of resetSequence,
	// which is incremented only on updates that completely rewrite the display.  Any update
	// with an outdated sequence number is simply ignored.

	private PrintWriter logWriter;

	protected static final String NEW_LINE = System.getProperty( "line.separator" );
	protected static final HashMap ACTIVE_LOG_FILES = new HashMap();

	private static final int MAXIMUM_LENGTH = 50000;
	private static final int TRIM_TO_LENGTH = 45000;

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

		displayPane.setText( this.getHTMLContent() );

		this.displayPanes.addLast( displayPane );
		this.addStickyPanes.addLast( displayPane );

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
			this.logWriter = (PrintWriter) ChatBuffer.ACTIVE_LOG_FILES.get( filename );
		}
		else
		{
			boolean shouldAppend = f.exists();
			this.logWriter = new PrintWriter( DataUtilities.getOutputStream( f, shouldAppend ), true );

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
		this.displayPanes.clear();
		this.stickyPanes.clear();
		this.addStickyPanes.clear();
		this.removeStickyPanes.clear();

		if ( this.logWriter != null )
		{
			this.logWriter.close();
		}

		this.content.setLength( 0 );
	}

	/**
	 * Clears the current buffer content.
	 */

	public void clear()
	{
		this.content.setLength( 0 );

		SwingUtilities.invokeLater( new ResetHandler( this.getHTMLContent() ) );
	}

	/**
	 * Appends the given contents to the chat buffer.
	 */

	public void append( String newContents )
	{
		synchronized ( this.stickyPanes )
		{
			this.stickyPanes.addAll( this.addStickyPanes );
			this.addStickyPanes.clear();

			this.stickyPanes.removeAll( this.removeStickyPanes );
			this.removeStickyPanes.clear();
		}

		if ( newContents == null )
		{
			SwingUtilities.invokeLater( new ResetHandler( this.getHTMLContent() ) );
			return;
		}

		newContents = newContents.trim();

		if ( newContents.length() == 0 )
		{
			return;
		}

		this.content.append( newContents );

		if ( this.logWriter != null )
		{
			this.logWriter.println( newContents );
		}

		if ( this.content.length() < ChatBuffer.MAXIMUM_LENGTH )
		{
			SwingUtilities.invokeLater( new AppendHandler( newContents ) );
			SwingUtilities.invokeLater( new ScrollHandler() );
			return;
		}

		int lineIndex = this.content.indexOf( "<br", ChatBuffer.MAXIMUM_LENGTH - ChatBuffer.TRIM_TO_LENGTH );

		if ( lineIndex != -1 )
		{
			lineIndex = this.content.indexOf( ">", lineIndex ) + 1;
		}

		if ( lineIndex == -1 )
		{
			this.clear();
			return;
		}

		this.content.delete( 0, lineIndex );

		SwingUtilities.invokeLater( new ResetHandler( this.getHTMLContent() ) );
		SwingUtilities.invokeLater( new ScrollHandler() );
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
		return this.content.toString();
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

		htmlContent.append( this.content.toString() );

		htmlContent.append( "</body></html>" );

		return htmlContent.toString();
	}

	public void setSticky( JEditorPane editor, boolean sticky )
	{
		synchronized ( this.stickyPanes )
		{
			if ( sticky )
			{
				this.addStickyPanes.add( editor );
				this.removeStickyPanes.remove( editor );
			}
			else
			{
				this.addStickyPanes.remove( editor );
				this.removeStickyPanes.add( editor );
			}
		}
	}

	private class ResetHandler
		implements Runnable
	{
		private final String htmlContent;
		private final int resetSequence;

		public ResetHandler( final String htmlContent )
		{
			this.htmlContent = htmlContent;
			this.resetSequence = ++ChatBuffer.this.resetSequence;
		}

		public void run()
		{
			if ( this.resetSequence != ChatBuffer.this.resetSequence )
			{
				return;	// outdated by a subsequent display reset
			}
			
			Iterator paneIterator = ChatBuffer.this.displayPanes.iterator();

			while ( paneIterator.hasNext() )
			{
				JEditorPane displayPane = (JEditorPane) paneIterator.next();

				if ( displayPane == null )
				{
					paneIterator.remove();
					continue;
				}

				displayPane.setText( this.htmlContent );
			}
		}
	}

	private class AppendHandler
		implements Runnable
	{
		private final String newContent;
		private final int resetSequence;

		public AppendHandler( final String newContent )
		{
			// Check for imbalanced HTML here
			
			Stack openTags = new Stack();
			Set skippedTags = new HashSet();
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
							String openTag = (String) openTags.pop();
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
				String openTag = (String) openTags.pop();
				buffer.append( "</" );
				buffer.append( openTag );
				buffer.append( ">" );
			}
			
			this.newContent = buffer.toString();
			
			this.resetSequence = ChatBuffer.this.resetSequence;
		}

		public void run()
		{
			if ( this.resetSequence != ChatBuffer.this.resetSequence )
			{
				return;	// outdated by a subsequent display reset
			}
			
			Iterator paneIterator = ChatBuffer.this.displayPanes.iterator();

			while ( paneIterator.hasNext() )
			{
				JEditorPane displayPane = (JEditorPane) paneIterator.next();

				if ( displayPane == null )
				{
					paneIterator.remove();
					continue;
				}

				HTMLDocument currentHTML = (HTMLDocument) displayPane.getDocument();

				Element contentElement = currentHTML.getDefaultRootElement();

				while ( !contentElement.isLeaf() )
				{
					contentElement = contentElement.getElement( contentElement.getElementCount() - 1 );
				}

				try
				{
					currentHTML.insertAfterEnd( contentElement, this.newContent );
					// If the insertion contained any non-ASCII characters, the "multiByte"
					// property will be set on the document.  This causes the use of
					// an alternate layout algorithm that handles bidirectional text
					// and other Unicode oddities: it's slower, and on some combinations
					// of platform and JRE version, tremendously slower.
					currentHTML.putProperty( "multiByte", Boolean.FALSE );
				}
				catch ( Exception e )
				{
					// If there's an exception, continue onward so that you
					// still have an updated display. But, print the stack
					// trace so you know what's going on.

					e.printStackTrace();
				}
			}
		}
	}

	private class ScrollHandler
		implements Runnable
	{
		private final int resetSequence;

		public ScrollHandler()
		{
			this.resetSequence = ChatBuffer.this.resetSequence;
		}

		public void run()
		{
			if ( this.resetSequence != ChatBuffer.this.resetSequence )
			{
				return;	// outdated by a subsequent display reset
			}

			Iterator paneIterator = ChatBuffer.this.stickyPanes.iterator();

			while ( paneIterator.hasNext() )
			{
				JEditorPane stickyPane = (JEditorPane) paneIterator.next();

				if ( stickyPane == null )
				{
					paneIterator.remove();
					continue;
				}

				int contentLength = stickyPane.getDocument().getLength();

				int caretPosition = Math.max( contentLength - 1, 0 );

				stickyPane.setCaretPosition( caretPosition );
			}
		}
	}

}
