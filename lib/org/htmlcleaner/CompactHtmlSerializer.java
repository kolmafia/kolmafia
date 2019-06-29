/*  Copyright (c) 2006-20013, HtmlCleaner project
    All rights reserved.
	
    Redistribution and use of this software in source and binary forms, 
    with or without modification, are permitted provided that the following 
    conditions are met:
	
    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.
	
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.
	
    * The name of HtmlCleaner may not be used to endorse or promote 
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
	
    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "HtmlCleaner" in the
    subject line.
*/

package org.htmlcleaner;

import java.io.*;
import java.util.*;

/**
 * <p>Compact HTML serializer - creates resulting HTML by stripping whitespaces wherever possible.</p>
 */
public class CompactHtmlSerializer extends HtmlSerializer {

    private int openPreTags = 0;

	public CompactHtmlSerializer(CleanerProperties props) {
		super(props);
	}

    protected void serialize(TagNode tagNode, Writer writer) throws IOException {
        boolean isPreTag = "pre".equalsIgnoreCase(tagNode.getName());
        if (isPreTag) {
            openPreTags++;
        }

        serializeOpenTag(tagNode, writer, false);

        List<? extends BaseToken> tagChildren = tagNode.getAllChildren();
        if ( !isMinimizedTagSyntax(tagNode) ) {
            ListIterator<? extends BaseToken> childrenIt = tagChildren.listIterator();
            while ( childrenIt.hasNext() ) {
                Object item = childrenIt.next();
                if (item instanceof ContentNode) {
                    String content = item.toString();
                    if (openPreTags > 0) {
                        writer.write(content);
                    } else {
                        boolean startsWithSpace = content.length() > 0 && Character.isWhitespace( content.charAt(0) );
                        boolean endsWithSpace = content.length() > 1 && Character.isWhitespace( content.charAt(content.length() - 1) );
                        content = dontEscape(tagNode) ? content.trim() : escapeText(content.trim());

                        if (startsWithSpace) {
                            writer.write(' ');
                        }

                        if (content.length() != 0) {
                            writer.write(content);
                            if (endsWithSpace) {
                                writer.write(' ');
                            }
                        }

                        //Removed due to issue #199
                        //if (childrenIt.hasNext()) {
                        //    if ( !Utils.isWhitespaceString(childrenIt.next()) ) {
                        //        writer.write("\n");
                        //    }
                        //    childrenIt.previous();
                        //}

                    }
                } else if (item instanceof CommentNode) {
                    String content = ((CommentNode) item).getCommentedContent().trim();
                    writer.write(content);
                } else if (item instanceof BaseToken) {
                    ((BaseToken)item).serialize(this, writer);
                }
            }

            serializeEndTag(tagNode, writer, false);
            if (isPreTag) {
                openPreTags--;
            }
        }
	}

}