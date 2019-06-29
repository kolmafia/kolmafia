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

import java.io.*;
import java.util.*;

/**
 * <p>Abstract HTML serializer - contains common logic for descendants.</p>
 */
public abstract class HtmlSerializer extends Serializer {

    protected HtmlSerializer(CleanerProperties props) {
        super(props);
    }


    protected boolean isMinimizedTagSyntax(TagNode tagNode) {
        final TagInfo tagInfo = props.getTagInfoProvider().getTagInfo(tagNode.getName());
        return tagInfo != null && !tagNode.hasChildren() && tagInfo.isEmptyTag();
    }

    protected boolean dontEscape(TagNode tagNode) {
        return isScriptOrStyle(tagNode);
    }
    
    protected String escapeText(String content) {
        return Utils.escapeHtml(content, props);
    }

    protected void serializeOpenTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
        String tagName = tagNode.getName();

        if (Utils.isEmptyString(tagName)) {
            return;
        }

        boolean nsAware = props.isNamespacesAware();

        if (!nsAware && Utils.getXmlNSPrefix(tagName) != null ) {
            tagName = Utils.getXmlName(tagName);
        }

        writer.write("<" + tagName);
        for (Map.Entry<String, String> entry: tagNode.getAttributes().entrySet()) {
            String attName = entry.getKey();
            
            //
            // Fix any invalid attribute names
            //
            if (!props.isAllowInvalidAttributeNames()){
            	attName = Utils.sanitizeXmlAttributeName(attName, props.getInvalidXmlAttributeNamePrefix());
            }
            
            if (attName != null && (Utils.isValidXmlIdentifier(attName) || props.isAllowInvalidAttributeNames())){

            if (!nsAware && Utils.getXmlNSPrefix(attName) != null ) {
                attName = Utils.getXmlName(attName);
            }
            if (!(nsAware && attName.equalsIgnoreCase("xmlns")))
            writer.write(" " + attName + "=\"" + escapeText(entry.getValue()) + "\"");
            }
        }

        if (nsAware) {
            Map<String, String> nsDeclarations = tagNode.getNamespaceDeclarations();
            if (nsDeclarations != null) {
                for (Map.Entry<String, String> entry: nsDeclarations.entrySet()) {
                    String prefix = entry.getKey();
                    String att = "xmlns";
                    if (prefix.length() > 0) {
                         att += ":" + prefix;
                    }
                    writer.write(" " + att + "=\"" + escapeText(entry.getValue()) + "\"");
                }
            }
        }

        if ( isMinimizedTagSyntax(tagNode) ) {
            writer.write(" />");
            if (newLine) {
                writer.write("\n");
            }
        } else {
            writer.write(">");
        }
    }

    protected void serializeEndTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
        String tagName = tagNode.getName();

        if (Utils.isEmptyString(tagName)) {
            return;
        }

        if (Utils.getXmlNSPrefix(tagName) != null && !props.isNamespacesAware()) {
            tagName = Utils.getXmlName(tagName);
        }

        writer.write( "</" + tagName + ">" );
        if (newLine) {
            writer.write("\n");
        }
    }

}