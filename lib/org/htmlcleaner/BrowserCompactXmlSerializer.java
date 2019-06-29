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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

/**
 * <p>
 *  Browser compact XML serializer - creates resulting XML by stripping whitespaces wherever possible,
 *  but preserving single whitespace where at least one exists. This behaviour is well suited
 *  for web-browsers, which usually treat multiple whitespaces as single one, but make difference
 *  between single whitespace and empty text.
 * </p>
 */
public class BrowserCompactXmlSerializer extends XmlSerializer {

    private static final String PRE_TAG = "pre";
    private static final String BR_TAG = "<br />";
    private static final String LINE_BREAK = "\n";

    public BrowserCompactXmlSerializer(CleanerProperties props) {
        super(props);
    }

    @Override
    protected void serialize(TagNode tagNode, Writer writer) throws IOException {
        serializeOpenTag(tagNode, writer, false);
        TagInfo tagInfo = props.getTagInfoProvider().getTagInfo(tagNode.getName());
        String tagName = tagInfo!=null? tagInfo.getName() : null;
        List<? extends BaseToken> tagChildren = new ArrayList<BaseToken>(tagNode.getAllChildren());
        if (!isMinimizedTagSyntax(tagNode)) {
            ListIterator<? extends BaseToken> childrenIt = tagChildren.listIterator();
            while (childrenIt.hasNext()) {
                Object item = childrenIt.next();
                if (item != null) {
                    if (item instanceof ContentNode && !PRE_TAG.equals(tagName)) {
                        String content = ((ContentNode) item).getContent();
                        content = dontEscape(tagNode) ? content.replaceAll("]]>", "]]&gt;") : escapeXml(content);
                        content = content.replaceAll("^"+SpecialEntities.NON_BREAKABLE_SPACE+"+", " ");
                        content = content.replaceAll(SpecialEntities.NON_BREAKABLE_SPACE+"+$", " ");
                        boolean whitespaceAllowed = tagInfo != null && tagInfo.getDisplay().isLeadingAndEndWhitespacesAllowed();
                        boolean writeLeadingSpace = content.length() > 0 && (Character.isWhitespace(content.charAt(0)));
                        boolean writeEndingSpace = content.length() > 1 && Character.isWhitespace(content.charAt(content.length() - 1));
                        content = content.trim();
                        if (content.length() != 0) {
                            boolean hasPrevContent = false;
                            int order = tagChildren.indexOf(item);
                            if (order >= 2) {
                                Object prev = tagChildren.get(order-1);
                                hasPrevContent = isContentOrInline(prev);
                            }

                            if (writeLeadingSpace && (whitespaceAllowed || hasPrevContent)) {
                                writer.write(' ');
                            }

                            StringTokenizer tokenizer = new StringTokenizer(content, LINE_BREAK, true);
                            String prevToken = "";
                            while (tokenizer.hasMoreTokens()) {
                                String token = tokenizer.nextToken();
                                if (prevToken.equals(token) && prevToken.equals(LINE_BREAK)) {
                                    writer.write(BR_TAG);
                                    prevToken = "";
                                } else if (LINE_BREAK.equals(token)) {
                                    writer.write(' ');
                                } else {
                                    writer.write(token.trim());
                                }
                                prevToken = token;
                            }

                            boolean hasFollowingContent = false;
                            if (childrenIt.hasNext()) {
                                Object next = childrenIt.next();
                                hasFollowingContent = isContentOrInline(next);
                                childrenIt.previous();
                            }

                            if (writeEndingSpace && (whitespaceAllowed || hasFollowingContent)) {
                                writer.write(' ');
                            }
                        } else{
                            childrenIt.remove();
                        }
                    } else if(item instanceof ContentNode){
                        String content = ((ContentNode) item).getContent();
                        writer.write(content);
                    } else if (item instanceof CommentNode) {
                    	String content = ((CommentNode) item).getCommentedContent().trim();
                    	writer.write(content);
                    } else {
                    	((BaseToken)item).serialize(this, writer);
                    }
                }
            }

            serializeEndTag(tagNode, writer, tagInfo != null && tagInfo.getDisplay().isAfterTagLineBreakNeeded());
        }
    }

    private boolean isContentOrInline(Object node) {
        boolean result = false;
        if (node instanceof ContentNode) {
            result = true;
        } else if (node instanceof TagNode) {
            TagInfo nextInfo = props.getTagInfoProvider().getTagInfo(((TagNode) node).getName());
            result = nextInfo != null && nextInfo.getDisplay() == Display.inline;
        }
        return result;
    }

}