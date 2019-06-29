/*  Copyright (c) 2006-2007, Vladimir Nikic
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

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * <p>Pretty XML serializer - creates resulting XML with indenting lines.</p>
 */
public class PrettyXmlSerializer extends XmlSerializer {

	private static final String DEFAULT_INDENTATION_STRING = "\t";

    private String indentString = DEFAULT_INDENTATION_STRING;
    private List<String> indents = new ArrayList<String>();

	public PrettyXmlSerializer(CleanerProperties props) {
		this(props, DEFAULT_INDENTATION_STRING);
	}

	public PrettyXmlSerializer(CleanerProperties props, String indentString) {
		super(props);
        this.indentString = indentString;
	}

	@Override
    protected void serialize(TagNode tagNode, Writer writer) throws IOException {
		serializePrettyXml(tagNode, writer, 0);
	}

	/**
	 * @param level
	 * @return Appropriate indentation for the specified depth.
	 */
    private synchronized String getIndent(int level) {
        int size = indents.size();
        if (size <= level) {
            String prevIndent = size == 0 ? null : indents.get(size - 1);
            for (int i = size; i <= level; i++) {
                String currIndent = prevIndent == null ? "" : prevIndent + indentString;
                indents.add(currIndent);
                prevIndent = currIndent;
            }
        }

        return indents.get(level);
    }

    private String getIndentedText(String content, int level) {
        String indent = getIndent(level);
        StringBuilder result = new StringBuilder( content.length() );
        StringTokenizer tokenizer = new StringTokenizer(content, "\n\r");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken().trim();
            if (!"".equals(line)) {
                result.append(indent).append(line).append("\n");
            }
        }

        return result.toString();
    }

    private String getSingleLineOfChildren(List<? extends BaseToken> children) {
        StringBuilder result = new StringBuilder();
        Iterator<? extends BaseToken> childrenIt = children.iterator();
        boolean isFirst = true;

        while (childrenIt.hasNext()) {
            Object child = childrenIt.next();

            if ( !(child instanceof ContentNode) ) {
                return null;
            } else {
                String content = child.toString();

                // if first item trims it from left
                if (isFirst) {
                	content = ltrim(content);
                }

                // if last item trims it from right
                if (!childrenIt.hasNext()) {
                	content = rtrim(content);
                }

                if ( content.indexOf("\n") >= 0 || content.indexOf("\r") >= 0 ) {
                    return null;
                }
                result.append(content);
            }

            isFirst = false;
        }

        return result.toString();
    }

    protected void serializePrettyXml(TagNode tagNode, Writer writer, int level) throws IOException {
        List<? extends BaseToken> tagChildren = tagNode.getAllChildren();
        boolean isHeadlessNode = Utils.isEmptyString(tagNode.getName());
        String indent = isHeadlessNode ? "" : getIndent(level);

        writer.write(indent);
        serializeOpenTag(tagNode, writer, true);

        if ( !isMinimizedTagSyntax(tagNode) ) {
            String singleLine = getSingleLineOfChildren(tagChildren);
            boolean dontEscape = dontEscape(tagNode);
            if (singleLine != null) {
            	if ( !dontEscape(tagNode) ) {
            		writer.write( escapeXml(singleLine) );
            	} else {
            		writer.write( singleLine.replaceAll("]]>", "]]&gt;") );
            	}
            } else {
                if (!isHeadlessNode) {
            	    writer.write("\n");
                }
                for (Object child: tagChildren) {
                    if (child instanceof TagNode) {
                        serializePrettyXml( (TagNode)child, writer, isHeadlessNode ? level : level + 1 );
                    } else if (child instanceof CData){
                    	serializeCData((CData)child, tagNode, writer);
                    } else if (child instanceof ContentNode) {
                        String content = dontEscape ? child.toString().replaceAll("]]>", "]]&gt;") : escapeXml(child.toString());
                        writer.write( getIndentedText(content, isHeadlessNode ? level : level + 1) );
                    } else if (child instanceof CommentNode) {
                        CommentNode commentNode = (CommentNode) child;
                        String content = commentNode.getCommentedContent();
                        writer.write( getIndentedText(content, isHeadlessNode ? level : level + 1) );
                    }
                }
            }

            if (singleLine == null) {
            	writer.write(indent);
            }

            serializeEndTag(tagNode, writer, true);
        }
    }
    /**
     * Trims specified string from left.
     * @param s
     */
    private String ltrim(String s) {
        if (s == null) {
            return null;
        }

        int index = 0;
        int len = s.length();

        while ( index < len && Character.isWhitespace(s.charAt(index)) ) {
            index++;
        }

        return (index >= len) ? "" : s.substring(index);
    }

    /**
     * Trims specified string from right.
     * @param s
     */
    private String rtrim(String s) {
        if (s == null) {
            return null;
        }

        int len = s.length();
        int index = len;

        while ( index > 0 && Character.isWhitespace(s.charAt(index-1)) ) {
            index--;
        }

        return (index <= 0) ? "" : s.substring(0, index);
    }
}