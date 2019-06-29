/*  Copyright (c) 2006-2017, the HtmlCleaner Project
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
*/

package org.htmlcleaner;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>DOM serializer - creates xml DOM.</p>
 */
public class DomSerializer {
	
    private static final String CSS_COMMENT_START = "/*";

    private static final String CSS_COMMENT_END = "*/";
    
    private static final String SCRIPT_TAG_NAME = "script";
    
    private static final String JS_COMMENT = "//";

    private static final String NEW_LINE = "\n";

    /**
     * The HTML Cleaner properties set by the user to control the HTML cleaning.
     */
    protected CleanerProperties props;
    
    /**
     * Whether XML entities should be escaped or not.
     */
    protected boolean escapeXml = true;
    
    protected boolean deserializeCdataEntities = false;
    
    protected boolean strictErrorChecking = true;
    
    /**
     * @param props the HTML Cleaner properties set by the user to control the HTML cleaning.
     * @param escapeXml if true then escape XML entities
     * @param deserializeCdataEntities if true then deserialize entities in CData sections
     * @param strictErrorChecking if false then Document strict error checking is turned off
     */
    public DomSerializer(CleanerProperties props, boolean escapeXml, boolean deserializeCdataEntities, boolean strictErrorChecking){
        this.props = props;
        this.escapeXml = escapeXml;
        this.deserializeCdataEntities = deserializeCdataEntities;
        this.strictErrorChecking = strictErrorChecking;
    }

    /**
     * @param props the HTML Cleaner properties set by the user to control the HTML cleaning.
     * @param escapeXml if true then escape XML entities
     * @param deserializeCdataEntities if true then deserialize entities in CData sections
     */
    public DomSerializer(CleanerProperties props, boolean escapeXml, boolean deserializeCdataEntities) {
        this.props = props;
        this.escapeXml = escapeXml;
        this.deserializeCdataEntities = deserializeCdataEntities;
    }

    /**
     * @param props the HTML Cleaner properties set by the user to control the HTML cleaning.
     * @param escapeXml if true then escape XML entities
     */
    public DomSerializer(CleanerProperties props, boolean escapeXml) {
        this.props = props;
        this.escapeXml = escapeXml;
    }

    /**
     * @param props the HTML Cleaner properties set by the user to control the HTML cleaning.
     */
    public DomSerializer(CleanerProperties props) {
        this(props, true);
    }
    
    //
    // Allow overriding of serialization for implementations. See bug #167.
    //
    protected Document createDocument(TagNode rootNode) throws ParserConfigurationException{

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation impl = builder.getDOMImplementation();
        
        Document document;
        
        //
        // Where a DOCTYPE is supplied in the input, ensure that this is in the output DOM. See issue #27
        //
        // Note that we may want to fix incorrect DOCTYPEs in future; there are some fairly
        // common patterns for errors with the older HTML4 doctypes.
        //
        if (rootNode.getDocType() != null){
        	String qualifiedName = rootNode.getDocType().getPart1();
        	String publicId = rootNode.getDocType().getPublicId();
        	String systemId = rootNode.getDocType().getSystemId();
        	
        	//
        	// If there is no qualified name, set it to html. See bug #153.
        	//
        	if (qualifiedName == null) qualifiedName = "html";
        	
            DocumentType documentType = impl.createDocumentType(qualifiedName, publicId, systemId);
            
            //
            // While the qualified name is "HTML" for some DocTypes, we want the actual document root name to be "html". See bug #116
            //
            if (qualifiedName.equals("HTML")) qualifiedName = "html";
            document = impl.createDocument(rootNode.getNamespaceURIOnPath(""), qualifiedName, documentType);
        } else {
        	document = builder.newDocument();
        	Element rootElement = document.createElement(rootNode.getName());
        	document.appendChild(rootElement);
        }
        
        //
        // Turn off error checking if we're allowing invalid attribute names, or if we've chosen to turn it off
        //
        if (props.isAllowInvalidAttributeNames() || strictErrorChecking == false){
        	document.setStrictErrorChecking(false);
        }
        
        
        //
        // Copy across root node attributes - see issue 127. Thanks to rasifiel for the patch
        //
        Map<String, String> attributes =  rootNode.getAttributes();
        Iterator<Map.Entry<String, String>> entryIterator = attributes.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, String> entry = entryIterator.next();
            String attrName = entry.getKey();
            String attrValue = entry.getValue();
            
            //
            // Fix any invalid attribute names
            //
            if (!props.isAllowInvalidAttributeNames()){
            	attrName = Utils.sanitizeXmlAttributeName(attrName, props.getInvalidXmlAttributeNamePrefix());
            }
        	
        	if (attrName != null && (Utils.isValidXmlIdentifier(attrName) || props.isAllowInvalidAttributeNames())){

        		if (escapeXml) {
        			attrValue = Utils.escapeXml(attrValue, props, true);
        		}

        		document.getDocumentElement().setAttribute(attrName, attrValue);

        		//
        		// Flag the attribute as an ID attribute if appropriate. Thanks to Chris173
        		//
        		if (attrName.equalsIgnoreCase("id")) {
        			document.getDocumentElement().setIdAttribute(attrName, true);
        		}
        	}

        }
        
        return document;
    }

    /**
     * @param rootNode the HTML Cleaner root node to serialize
     * @return the W3C Document object
     * @throws ParserConfigurationException if there's an error during serialization
     */
    public Document createDOM(TagNode rootNode) throws ParserConfigurationException {

    	Document document = createDocument(rootNode);

        createSubnodes(document, (Element)document.getDocumentElement(), rootNode.getAllChildren());

        return document;
    }

    /**
     * @param element the element to check
     * @return true if the passed element is a script or style element
     */
    protected boolean isScriptOrStyle(Element element) {
        String tagName = element.getNodeName();
        return "script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName);
    }
    /**
     * encapsulate content with <[CDATA[ ]]> for things like script and style elements
     * @param element
     * @return true if <[CDATA[ ]]> should be used.
     */
    protected boolean dontEscape(Element element) {
        // make sure <script src=..></script> doesn't get turned into <script src=..><[CDATA[]]></script>
        return props.isUseCdataFor(element.getNodeName()) && (!element.hasChildNodes() || element.getTextContent() == null || element.getTextContent().trim().length() == 0);
    }
    
    protected String outputCData(CData cdata){
    	return cdata.getContentWithoutStartAndEndTokens();
    }
    
    protected String deserializeCdataEntities(String input){
    	return Utils.deserializeEntities(input, props.isRecognizeUnicodeChars());
    }
    
    /**
     * Serialize a given HTML Cleaner node.
     * 
     * @param document the W3C Document to use for creating new DOM elements
     * @param element the W3C element to which we'll add the subnodes to
     * @param tagChildren the HTML Cleaner nodes to serialize for that node
     */
    protected void createSubnodes(Document document, Element element, List<? extends BaseToken> tagChildren) {

    	if (tagChildren != null) {
    		
        	CDATASection cdata = null;
        	
        	//
        	// For script and style nodes, check if we're set to use CDATA
        	//
        	if (props.isUseCdataFor(element.getTagName())){
        		cdata = document.createCDATASection("");
    			element.appendChild(document.createTextNode(CSS_COMMENT_START));
        		element.appendChild(cdata); 
        	}
        	
            Iterator<? extends BaseToken> it = tagChildren.iterator();
            while (it.hasNext()) {
            	
                Object item = it.next();
    			if (item instanceof CommentNode) {
    				CommentNode commentNode = (CommentNode) item;
    				Comment comment = document.createComment( commentNode.getContent() );
    				element.appendChild(comment);

    			} else if (item instanceof ContentNode) {
    				ContentNode contentNode = (ContentNode) item;
    				String content = contentNode.getContent();
					boolean specialCase = props.isUseCdataFor(element.getTagName());

					if (shouldEscapeOrTranslateEntities() && !specialCase) {
						content = Utils.escapeXml(content, props, true);
					}

    				if (specialCase && item instanceof CData){
    					//
    					// For CDATA sections we don't want to return the start and
    					// end tokens. See issue #106.
    					//
    					content = ((CData)item).getContentWithoutStartAndEndTokens();
    				}
    				
    				if (specialCase && deserializeCdataEntities){
    					content = this.deserializeCdataEntities(content);
    				}

                	if (cdata != null){
                		cdata.appendData(content);
                	} else {
    					element.appendChild(document.createTextNode(content) ); 
                	}

    			} else if (item instanceof TagNode) {
    				TagNode subTagNode = (TagNode) item;
    				Element subelement = document.createElement( subTagNode.getName() );
    				Map<String, String> attributes =  subTagNode.getAttributes();
    				Iterator<Map.Entry<String, String>> entryIterator = attributes.entrySet().iterator();
    				while (entryIterator.hasNext()) {
    					Map.Entry<String, String> entry = entryIterator.next();
    					String attrName = entry.getKey();
    					String attrValue = entry.getValue();
    					if (escapeXml) {
    						attrValue = Utils.escapeXml(attrValue, props, true);
    					}
    					
    		            //
    		            // Fix any invalid attribute names by adding a prefix
    		            //
    		            if (!props.isAllowInvalidAttributeNames()){
    		            	attrName = Utils.sanitizeXmlAttributeName(attrName, props.getInvalidXmlAttributeNamePrefix());
    		            }
    		        	
    		        	if (attrName != null && (Utils.isValidXmlIdentifier(attrName) || props.isAllowInvalidAttributeNames())){
    		        		subelement.setAttribute(attrName, attrValue);

    		        		//
    		        		// Flag the attribute as an ID attribute if appropriate. Thanks to Chris173
    		        		//
    		        		if (attrName.equalsIgnoreCase("id")) {
    		        			subelement.setIdAttribute(attrName, true);
    		        		}
    		        	}

    				}

    				// recursively create subnodes
    				createSubnodes(document, subelement, subTagNode.getAllChildren());

    				element.appendChild(subelement);
    			} else if (item instanceof List) {
    				List<? extends BaseToken> sublist = (List<? extends BaseToken>) item;
    				createSubnodes(document, element, sublist);
    			}
    		}
            if (cdata != null){
        		if (!cdata.getData().startsWith(NEW_LINE)){
        			cdata.setData(CSS_COMMENT_END + NEW_LINE + cdata.getData());
        		} else {
        			cdata.setData(CSS_COMMENT_END + cdata.getData());
        		}
        		if (!cdata.getData().endsWith(NEW_LINE)){

        			cdata.appendData(NEW_LINE);
        		}
            	cdata.appendData(CSS_COMMENT_START); 
    			element.appendChild(document.createTextNode(CSS_COMMENT_END));
            }
    	}
    }

	private boolean shouldEscapeOrTranslateEntities() {
		return escapeXml || props.isRecognizeUnicodeChars() || props.isTranslateSpecialEntities();
	}

}