package org.htmlcleaner;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;

/**
 * <p>
 * JDom serializer - creates xml JDom instance out of the TagNode.
 * </p>
 */
public class JDomSerializer {
	
    private static final String CSS_COMMENT_START = "/*";

    private static final String CSS_COMMENT_END = "*/";
    
    private static final String SCRIPT_TAG_NAME = "script";
    
    private static final String JS_COMMENT = "//";

    private static final String NEW_LINE = "\n";

    private DefaultJDOMFactory factory;

    protected CleanerProperties props;
    protected boolean escapeXml = true;

    public JDomSerializer(CleanerProperties props, boolean escapeXml) {
        this.props = props;
        this.escapeXml = escapeXml;
    }

    public JDomSerializer(CleanerProperties props) {
        this(props, true);
    }

    public Document createJDom(TagNode rootNode) {
        this.factory = new DefaultJDOMFactory();
        
        //
        // If there is no actual root node then return nothing
        //
        if (rootNode.getName() == null) return null;
        
        Element rootElement = createElement(rootNode);
        Document document = this.factory.document(rootElement);

        setAttributes(rootNode, rootElement);

        createSubnodes(rootElement, rootNode.getAllChildren());

        return document;
    }

    private Element createElement(TagNode node) {
        String name = node.getName();
        boolean nsAware = props.isNamespacesAware();
        String prefix = Utils.getXmlNSPrefix(name);
        Map<String, String> nsDeclarations = node.getNamespaceDeclarations();
        String nsURI = null;
        if (prefix != null) {
            name = Utils.getXmlName(name);
            if (nsAware) {
                if (nsDeclarations != null) {
                    nsURI = nsDeclarations.get(prefix);
                }
                if (nsURI == null) {
                    nsURI = node.getNamespaceURIOnPath(prefix);
                }
                if (nsURI == null) {
                    nsURI = prefix;
                }
            }
        } else {
            if (nsAware) {
                if (nsDeclarations != null) {
                    nsURI = nsDeclarations.get("");
                }
                if (nsURI == null) {
                    nsURI = node.getNamespaceURIOnPath(prefix);
                }
            }
        }

        Element element;
        if (nsAware && nsURI != null) {
            Namespace ns = prefix == null ? Namespace.getNamespace(nsURI) : Namespace.getNamespace(prefix, nsURI);
            element = factory.element(name, ns);
        } else {
            element = factory.element(name);
        }

        if (nsAware) {
            defineNamespaceDeclarations(node, element);
        }
        return element;
    }

    private void defineNamespaceDeclarations(TagNode node, Element element) {
        Map<String, String> nsDeclarations = node.getNamespaceDeclarations();
        if (nsDeclarations != null) {
            for (Map.Entry<String, String> nsEntry : nsDeclarations.entrySet()) {
                String nsPrefix = nsEntry.getKey();
                String nsURI = nsEntry.getValue();
                Namespace ns = nsPrefix == null || "".equals(nsPrefix) ? Namespace.getNamespace(nsURI) : Namespace
                        .getNamespace(nsPrefix, nsURI);
                element.addNamespaceDeclaration(ns);
            }
        }
    }

    private void setAttributes(TagNode node, Element element) {
    	for (Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
    		String attrName = entry.getKey();
    		String attrValue = entry.getValue();
    		if (escapeXml) {
    			attrValue = Utils.escapeXml(attrValue, props, true);
    		}

            //
            // Fix any invalid attribute names
            //
            if (!props.isAllowInvalidAttributeNames()){
            	attrName = Utils.sanitizeXmlAttributeName(attrName, props.getInvalidXmlAttributeNamePrefix());
            }

            //
            // Note that even if we did want to allow invalid attribute names, JDom won't allow it
            //
    		if (attrName != null && Utils.isValidXmlIdentifier(attrName)){
    			String attPrefix = Utils.getXmlNSPrefix(attrName);
    			Namespace ns = null;
    			if (attPrefix != null) {
    				attrName = Utils.getXmlName(attrName);
    				if (props.isNamespacesAware()) {
    					String nsURI = node.getNamespaceURIOnPath(attPrefix);
    					if (nsURI == null) {
    						nsURI = attPrefix;
    					}
    					if (!attPrefix.startsWith("xml")) {
    						ns = Namespace.getNamespace(attPrefix, nsURI);
    					}
    				}
    			}

    			//
    			// Don't manually add xmlns attributes as these should be 
    			// handled automatically by JDOM through the namespace
    			// mechanism
    			//
    			if (!attrName.equals("xmlns")){
    				if (ns == null) {
    					element.setAttribute(attrName, attrValue);
    				} else {
    					element.setAttribute(attrName, attrValue, ns);
    				}
    			}
    		}
    	}
    }

    private void createSubnodes(Element element, List<? extends BaseToken> tagChildren) {
        if (tagChildren != null) {
        	
        	CDATA cdata = null;
        	//
        	// For script and style nodes, check if we're set to use CDATA
        	//
        	if (props.isUseCdataFor(element.getName())){
        		cdata = factory.cdata("");
    			element.addContent(factory.text(CSS_COMMENT_START));
        		element.addContent(cdata); 
        	}
        	
        	
            Iterator<? extends BaseToken> it = tagChildren.iterator();
            while (it.hasNext()) {
            	
                Object item = it.next();
                
                if (item instanceof CommentNode) {
                    CommentNode commentNode = (CommentNode) item;
                    Comment comment = factory.comment(commentNode.getContent().toString());
                    element.addContent(comment);
                    
                } else if (item instanceof ContentNode) {
                	String nodeName = element.getName();
                	String content = item.toString();
                	boolean specialCase = props.isUseCdataFor(nodeName);

                	if (escapeXml && !specialCase) {
                		content = Utils.escapeXml(content, props, true);
                	}
                	if (specialCase && item instanceof CData){
                		//
                		// For CDATA sections we don't want to return the start and
                		// end tokens. See issue #106.
                		//
                		content = ((CData)item).getContentWithoutStartAndEndTokens();
                	}
                	if (cdata != null){
                		cdata.append(content);
                	} else {
                		Text text = factory.text(content);
                		element.addContent(text);
                	}

                } else if (item instanceof TagNode) {
                    TagNode subTagNode = (TagNode) item;
                    Element subelement = createElement(subTagNode);

                    setAttributes(subTagNode, subelement);

                    // recursively create subnodes
                    createSubnodes(subelement, subTagNode.getAllChildren());

                    element.addContent(subelement);
                } else if (item instanceof List) {
                    List sublist = (List) item;
                    createSubnodes(element, sublist);
                }
                
            }
            if (cdata != null){
        		if (!cdata.getText().startsWith(NEW_LINE)){
        			cdata.setText(CSS_COMMENT_END + NEW_LINE + cdata.getText());
        		} else {
        			cdata.setText(CSS_COMMENT_END + cdata.getText());
        		}
        		if (!cdata.getText().endsWith(NEW_LINE)){

        			cdata.append(NEW_LINE);
        		}
            	cdata.append(CSS_COMMENT_START); 
    			element.addContent(factory.text(CSS_COMMENT_END));
            }
        }
    }

}