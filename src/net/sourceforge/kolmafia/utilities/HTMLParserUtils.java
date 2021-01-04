/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.utilities;

import net.sourceforge.kolmafia.RequestLogger;

import java.util.Iterator;
import java.util.Map;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentNode;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class HTMLParserUtils
{
	public static final HtmlCleaner configureDefaultParser()
	{
		HtmlCleaner cleaner = new HtmlCleaner();

		CleanerProperties props = cleaner.getProperties();
		props.setTranslateSpecialEntities( false );
		props.setRecognizeUnicodeChars( false );
		props.setOmitXmlDeclaration( true );

		return cleaner;
	}

	// Log cleaned HTML

	public static final void logHTML( final TagNode node )
	{
		if ( node != null )
		{
			StringBuffer buffer = new StringBuffer();
			HTMLParserUtils.logHTML( node, buffer, 0 );
		}
	}

	private static final void logHTML( final TagNode node, final StringBuffer buffer, int level )
	{
		String name = node.getName();

 		// Skip scripts
 		if ( name.equals( "script" ) )
 		{
 			return;
		}

		HTMLParserUtils.indent( buffer, level );
		HTMLParserUtils.printTag( buffer, node );
		RequestLogger.updateDebugLog( buffer.toString() );

		Iterator it = node.getAllChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentNode )
			{
				CommentNode object = (CommentNode) child;
				String content = object.getContent();
				HTMLParserUtils.indent( buffer, level + 1 );
				buffer.append( "<!--" );
				buffer.append( content );
				buffer.append( "-->" );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof ContentNode )
			{
				ContentNode object = (ContentNode) child;
				String content = object.getContent().trim();
				if ( content.equals( "" ) )
				{
					continue;
				}

				HTMLParserUtils.indent( buffer, level + 1 );
				buffer.append( content );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof TagNode )
			{
				TagNode object = (TagNode) child;
				HTMLParserUtils.logHTML( object, buffer, level + 1 );
				continue;
			}
		}
	}

	private static final void indent( final StringBuffer buffer, int level )
	{
		buffer.setLength( 0 );
		for ( int i = 0; i < level; ++i )
		{
			buffer.append( " " );
			buffer.append( " " );
		}
	}

	private static final void printTag( final StringBuffer buffer, TagNode node )
	{
		String name = node.getName();
		Map attributes = node.getAttributes();

		buffer.append( "<" );
		buffer.append( name );

		if ( !attributes.isEmpty() )
		{
			Iterator it = attributes.keySet().iterator();
			while ( it.hasNext() )
			{
				String key = (String) it.next();
				buffer.append( " " );
				buffer.append( key );
				buffer.append( "=\"" );
				buffer.append( (String) attributes.get( key ) );
				buffer.append( "\"" );
			}
		}
		buffer.append( ">" );
	}
}
