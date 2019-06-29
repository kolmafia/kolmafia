/*  Copyright (c) 2006-2013, HtmlCleaner project
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
 * <p>Pretty HTML serializer - creates resulting HTML with indenting lines.</p>
 */
public class PrettyHtmlSerializer extends HtmlSerializer {

	private static final String DEFAULT_INDENTATION_STRING = "\t";

    private String indentString = DEFAULT_INDENTATION_STRING;
    private List<String> indents = new ArrayList<String>();

	public PrettyHtmlSerializer(CleanerProperties props) {
		this(props, DEFAULT_INDENTATION_STRING);
	}

	public PrettyHtmlSerializer(CleanerProperties props, String indentString) {
		super(props);
        this.indentString = indentString;
	}

	protected void serialize(TagNode tagNode, Writer writer) throws IOException {
		serializePrettyHtml(tagNode, writer, 0, false, true);
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

                //
                // Removed the trim function as this has the potential
                // to cause issues with actual content without adding
                // any value
                //
                
                /*
                // if first item trims it from left
                if (isFirst) {
                	content = Utils.ltrim(content);
                }

                // if last item trims it from right
                if (!childrenIt.hasNext()) {
                	content = Utils.rtrim(content);
                }
                */

                if ( content.indexOf("\n") >= 0 || content.indexOf("\r") >= 0 ) {
                    return null;
                }
                result.append(content);
            }

            isFirst = false;
        }

        return result.toString();
    }

    protected void serializePrettyHtml(TagNode tagNode, Writer writer, int level, boolean isPreserveWhitespaces, boolean isLastNewLine) throws IOException {
        List<? extends BaseToken> tagChildren = tagNode.getAllChildren();
        String tagName = tagNode.getName();
        boolean isHeadlessNode = Utils.isEmptyString(tagName);
        String indent = isHeadlessNode ? "" : getIndent(level);

        if (!isPreserveWhitespaces) {
            if (!isLastNewLine) {
                writer.write("\n");
            }
            writer.write(indent);
        }
        serializeOpenTag(tagNode, writer, true);

        boolean preserveWhitespaces = isPreserveWhitespaces || "pre".equalsIgnoreCase(tagName);

        boolean lastWasNewLine = false;

        if ( !isMinimizedTagSyntax(tagNode) ) {
            String singleLine = getSingleLineOfChildren(tagChildren);
            boolean dontEscape = dontEscape(tagNode);
            if (!preserveWhitespaces && singleLine != null) {
                writer.write( !dontEscape(tagNode) ? escapeText(singleLine) : singleLine );
            } else {
                Iterator<? extends BaseToken> childIterator = tagChildren.iterator();
                while (childIterator.hasNext()) {
                    Object child = childIterator.next();
                    if (child instanceof TagNode) {
                        serializePrettyHtml((TagNode)child, writer, isHeadlessNode ? level : level + 1, preserveWhitespaces, lastWasNewLine);
                        lastWasNewLine = false;
                    } else if (child instanceof ContentNode) {
                        String content = dontEscape ? child.toString() : escapeText(child.toString());
                        if (content.length() > 0) {
                            if (dontEscape || preserveWhitespaces) {
                                writer.write(content);
                            } else if (Character.isWhitespace(content.charAt(0))) {
                                if (!lastWasNewLine) {
                                    writer.write("\n");
                                    lastWasNewLine = false;
                                }
                                if (content.trim().length() > 0) {
                                    writer.write( getIndentedText(Utils.rtrim(content), isHeadlessNode ? level : level + 1) );
                                } else {
                                    lastWasNewLine = true;
                                }
                            } else {
                                if (content.trim().length() > 0) {
                                    writer.write(Utils.rtrim(content));
                                }
                                if (!childIterator.hasNext()) {
                                    writer.write("\n");
                                    lastWasNewLine = true;
                                }
                            }
                        }
                    } else if (child instanceof CommentNode) {

                        if (!lastWasNewLine && !preserveWhitespaces) {
                            writer.write("\n");
                            lastWasNewLine = false;
                        }
                        CommentNode commentNode = (CommentNode) child;
                        String content = commentNode.getCommentedContent();
                        writer.write( dontEscape ? content : getIndentedText(content, isHeadlessNode ? level : level + 1) );
                    }
                }
            }

            if (singleLine == null && !preserveWhitespaces) {
                if (!lastWasNewLine) {
                    writer.write("\n");
                }
            	writer.write(indent);
            }

            serializeEndTag(tagNode, writer, false);
        }
    }

}