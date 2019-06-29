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
 * <p>Basic abstract serializer - contains common logic for descendants (methods <code>writeXXX()</code>.</p>
 */
public abstract class Serializer {

    /**
     * Used to implement serialization with missing envelope - omiting open and close tags, just
     * serialize children.
     */
    private class HeadlessTagNode extends TagNode {
        private HeadlessTagNode(TagNode wrappedNode) {
            super("");
            getAttributes().putAll(wrappedNode.getAttributes());
            addChildren(wrappedNode.getAllChildren());
            setDocType(wrappedNode.getDocType());
            Map<String, String> nsDecls = getNamespaceDeclarations();
            if (nsDecls != null) {
                Map<String, String> wrappedNSDecls = wrappedNode.getNamespaceDeclarations();
                if (wrappedNSDecls != null) {
                    nsDecls.putAll(wrappedNSDecls);
                }
            }

        }
    }

	protected CleanerProperties props;

	protected Serializer(CleanerProperties props) {
		this.props = props;
    }

    /**
     * Writes specified TagNode to the output stream, using specified charset and optionally omits node envelope
     * (skips open and close tags of the node).
     * @param tagNode Node to be written
     * @param out Output stream
     * @param charset Charset of the output
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @throws IOException
     */
    public void writeToStream(TagNode tagNode, OutputStream out, String charset, boolean omitEnvelope) throws IOException {
         write( tagNode, new OutputStreamWriter(out, charset), charset, omitEnvelope );
    }

    /**
     * Writes specified TagNode to the output stream, using specified charset.
     * @param tagNode Node to be written
     * @param out Output stream
     * @param charset Charset of the output
     * @throws IOException
     */
    public void writeToStream(TagNode tagNode, OutputStream out, String charset) throws IOException {
         writeToStream(tagNode, out, charset, false);
    }

    /**
     * Writes specified TagNode to the output stream, using system default charset and optionally omits node envelope
     * (skips open and close tags of the node).
     * @param tagNode Node to be written
     * @param out Output stream
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @throws IOException
     */
    public void writeToStream(TagNode tagNode, OutputStream out, boolean omitEnvelope) throws IOException {
         writeToStream( tagNode, out, props.getCharset(), omitEnvelope );
    }

    /**
     * Writes specified TagNode to the output stream, using system default charset.
     * @param tagNode Node to be written
     * @param out Output stream
     * @throws IOException
     */
    public void writeToStream(TagNode tagNode, OutputStream out) throws IOException {
         writeToStream(tagNode, out, false);
    }

    /**
     * Writes specified TagNode to the file, using specified charset and optionally omits node envelope
     * (skips open and close tags of the node).
     * @param tagNode Node to be written
     * @param fileName Output file name
     * @param charset Charset of the output
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @throws IOException
     */
    public void writeToFile(TagNode tagNode, String fileName, String charset, boolean omitEnvelope) throws IOException {
        writeToStream(tagNode, new FileOutputStream(fileName), charset, omitEnvelope );
    }

    /**
     * Writes specified TagNode to the file, using specified charset.
     * @param tagNode Node to be written
     * @param fileName Output file name
     * @param charset Charset of the output
     * @throws IOException
     */
    public void writeToFile(TagNode tagNode, String fileName, String charset) throws IOException {
        writeToFile(tagNode, fileName, charset, false);
    }

    /**
     * Writes specified TagNode to the file, using specified charset and optionally omits node envelope
     * (skips open and close tags of the node).
     * @param tagNode Node to be written
     * @param fileName Output file name
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @throws IOException
     */
    public void writeToFile(TagNode tagNode, String fileName, boolean omitEnvelope) throws IOException {
        writeToFile(tagNode,fileName, props.getCharset(), omitEnvelope);
    }

    /**
     * Writes specified TagNode to the file, using system default charset.
     * @param tagNode Node to be written
     * @param fileName Output file name
     * @throws IOException
     */
    public void writeToFile(TagNode tagNode, String fileName) throws IOException {
        writeToFile(tagNode, fileName, false);
    }

    /**
     * @param tagNode Node to serialize to string
     * @param charset Charset of the output - stands in xml declaration part
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @return Output as string
     */
    public String getAsString(TagNode tagNode, String charset, boolean omitEnvelope) {
        StringWriter writer = new StringWriter();
        try {
            write(tagNode, writer, charset, omitEnvelope);
        } catch (IOException e) {
            // not writing to the file system so any io errors should be really rare ( and bad)
            throw new HtmlCleanerException(e);
        }
        return writer.getBuffer().toString();
    }

    /**
     * @param tagNode Node to serialize to string
     * @param charset Charset of the output - stands in xml declaration part
     * @return Output as string
     */
    public String getAsString(TagNode tagNode, String charset) {
        return getAsString(tagNode, charset, false);
    }

    /**
     * @param tagNode Node to serialize to string
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @return Output as string
     * @throws IOException
     */
    public String getAsString(TagNode tagNode, boolean omitEnvelope) {
        return getAsString(tagNode, props.getCharset(), omitEnvelope);
    }

    /**
     * @param tagNode Node to serialize to string
     * @return Output as string
     * @throws IOException
     */
    public String getAsString(TagNode tagNode) {
        return getAsString(tagNode, false);
    }

    public String getAsString(String htmlContent) {
        HtmlCleaner htmlCleaner = new HtmlCleaner(this.props);
        TagNode tagNode = htmlCleaner.clean(htmlContent);
        return getAsString(tagNode, props.getCharset());
    }


    /**
     * Writes specified node using specified writer.
     * @param tagNode Node to serialize.
     * @param writer Writer instance
     * @param charset Charset of the output
     * @throws IOException
     */
    public void write(TagNode tagNode, Writer writer, String charset) throws IOException {
        write(tagNode, writer, charset, false);
    }

    /**
     * Writes specified node using specified writer.
     * @param tagNode Node to serialize.
     * @param writer Writer instance
     * @param charset Charset of the output
     * @param omitEnvelope Tells whether to skip open and close tag of the node.
     * @throws IOException
     */
    public void write(TagNode tagNode, Writer writer, String charset, boolean omitEnvelope) throws IOException {
        if (omitEnvelope) {
            tagNode = new HeadlessTagNode(tagNode);
        }
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


    protected boolean isScriptOrStyle(TagNode tagNode) {
        String tagName = tagNode.getName();
        return "script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName);
    }
    
    protected abstract void serialize(TagNode tagNode, Writer writer) throws IOException;
	
}