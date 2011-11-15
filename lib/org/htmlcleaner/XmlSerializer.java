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
import java.util.Iterator;
import java.util.Map;

/**
 * <p>Abstract XML serializer - contains common logic for descendants.</p>
 *
 * Created by: Vladimir Nikic<br/>
 * Date: November, 2006.
 */
public abstract class XmlSerializer {
	
	protected CleanerProperties props;

	protected XmlSerializer(CleanerProperties props) {
		this.props = props;
    }

    public void writeXmlToStream(TagNode tagNode, OutputStream out, String charset) throws IOException {
         writeXml( tagNode, new OutputStreamWriter(out, charset), charset );
    }

    public void writeXmlToStream(TagNode tagNode, OutputStream out) throws IOException {
         writeXmlToStream( tagNode, out, HtmlCleaner.DEFAULT_CHARSET );
    }

    public void writeXmlToFile(TagNode tagNode, String fileName, String charset) throws IOException {
        writeXmlToStream(tagNode, new FileOutputStream(fileName), charset );
    }

    public void writeXmlToFile(TagNode tagNode, String fileName) throws IOException {
        writeXmlToFile(tagNode,fileName, HtmlCleaner.DEFAULT_CHARSET);
    }

    public String getXmlAsString(TagNode tagNode, String charset) throws IOException {
        StringWriter writer = new StringWriter();
        writeXml(tagNode, writer, charset);
        return writer.getBuffer().toString();
    }

    public String getXmlAsString(TagNode tagNode) throws IOException {
        return getXmlAsString(tagNode, HtmlCleaner.DEFAULT_CHARSET);
    }
	
    public void writeXml(TagNode tagNode, Writer writer, String charset) throws IOException {
        writer = new BufferedWriter(writer);
        if ( !props.isOmitXmlDeclaration() ) {
            String declaration = "<?xml version=\"1.0\"";
            if (charset != null) {
                declaration += " encoding=\"" + charset + "\"";
            }
            declaration += "?>";
            writer.write(declaration + "\n");
		}
		
		if ( !props.isOmitDoctypeDeclaration() ) {
			DoctypeToken doctypeToken = tagNode.getDocType();
			if ( doctypeToken != null ) {
				doctypeToken.serialize(this, writer);
			}
		}
		
		serialize(tagNode, writer);

        writer.flush();
        writer.close();
    }
	
	protected String escapeXml(String xmlContent) {
		return Utils.escapeXml(xmlContent, props, false);
	}
	
	protected boolean dontEscape(TagNode tagNode) {
		String tagName = tagNode.getName();
		return props.isUseCdataForScriptAndStyle() && ("script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName));
	}
	
	protected boolean isScriptOrStyle(TagNode tagNode) {
		String tagName = tagNode.getName();
		return "script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName);
	}

    protected boolean isMinimizedTagSyntax(TagNode tagNode) {
        final TagInfo tagInfo = props.getTagInfoProvider().getTagInfo(tagNode.getName());
        return tagNode.getChildren().size() == 0 &&
               ( props.isUseEmptyElementTags() || (tagInfo != null && tagInfo.isEmptyTag()) );
    }
	
    protected void serializeOpenTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
        String tagName = tagNode.getName();
        Map tagAtttributes = tagNode.getAttributes();
        
        writer.write("<" + tagName);
        Iterator it = tagAtttributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String attName = (String) entry.getKey();
            String attValue = (String) entry.getValue();
            
            if ( !props.isNamespacesAware() && ("xmlns".equals(attName) || attName.startsWith("xmlns:")) ) {
            	continue;
            }
            
            writer.write(" " + attName + "=\"" + escapeXml(attValue) + "\"");
        }
        
        if ( isMinimizedTagSyntax(tagNode) ) {
        	writer.write(" />");
        	if (newLine) {
        		writer.write("\n");
        	}
        } else if (dontEscape(tagNode)) {
        	writer.write("><![CDATA[");
        } else {
        	writer.write(">");
        }
    }
    
    protected void serializeOpenTag(TagNode tagNode, Writer writer) throws IOException {
    	serializeOpenTag(tagNode, writer, true);
    }
    
    protected void serializeEndTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
    	String tagName = tagNode.getName();
    	
    	if (dontEscape(tagNode)) {
    		writer.write("]]>");
    	}
    	
    	writer.write( "</" + tagName + ">" );

        if (newLine) {
    		writer.write("\n");
    	}
    }
    
    protected void serializeEndTag(TagNode tagNode, Writer writer) throws IOException {
    	serializeEndTag(tagNode, writer, true);
    }


    protected abstract void serialize(TagNode tagNode, Writer writer) throws IOException;
	
}