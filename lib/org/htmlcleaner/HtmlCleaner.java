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
import java.net.URL;
import java.util.*;

/**
 * Main HtmlCleaner class.
 *
 * <p>It represents public interface to the user. It's task is to call tokenizer with
 * specified source HTML, traverse list of produced token list and create internal
 * object model. It also offers a set of methods to write resulting XML to string,
 * file or any output stream.</p>
 * <p>Typical usage is the following:</p>
 *
 * <xmp>
 *      HtmlCleaner cleaner = new HtmlCleaner(...);     // one of few constructors
 *      cleaner.setXXX(...)                             // optionally, set cleaner's behaviour
 *      clener.clean();                                 // calls cleaning process
 *      cleaner.writeXmlXXX(...);                       // writes resulting XML to string, file or any output stream
 *      // cleaner.createDOM();                         // creates DOM of resulting xml
 *      // cleaner.createJDom();                        // creates JDom of resulting xml
 * </xmp>
 *
 * Created by: Vladimir Nikic <br/>
 * Date: November, 2006
 */
public class HtmlCleaner {

    public static final String DEFAULT_CHARSET = System.getProperty("file.encoding");

    /**
     * Contains information about single open tag
     */
    private class TagPos {
		private int position;
		private String name;
		private TagInfo info;

		TagPos(int position, String name) {
			this.position = position;
			this.name = name;
            this.info = tagInfoProvider.getTagInfo(name);
        }
	}

    /**
     * Class that contains information and mathods for managing list of open,
     * but unhandled tags.
     */
    private class OpenTags {
        private List list = new ArrayList();
        private TagPos last = null;
        private Set set = new HashSet();

        private boolean isEmpty() {
            return list.isEmpty();
        }

        private void addTag(String tagName, int position) {
            last = new TagPos(position, tagName);
            list.add(last);
            set.add(tagName);
        }

        private void removeTag(String tagName) {
            ListIterator it = list.listIterator( list.size() );
            while ( it.hasPrevious() ) {
                TagPos currTagPos = (TagPos) it.previous();
                if (tagName.equals(currTagPos.name)) {
                    it.remove();
                    break;
                }
            }

            last =  list.isEmpty() ? null : (TagPos) list.get( list.size() - 1 );
        }

        private TagPos findFirstTagPos() {
            return list.isEmpty() ? null : (TagPos) list.get(0);
        }

        private TagPos getLastTagPos() {
            return last;
        }

        private TagPos findTag(String tagName) {
            if (tagName != null) {
                ListIterator it = list.listIterator(list.size());
                String fatalTag = null;
                TagInfo fatalInfo = tagInfoProvider.getTagInfo(tagName);
                if (fatalInfo != null) {
                    fatalTag = fatalInfo.getFatalTag();
                }

                while (it.hasPrevious()) {
                    TagPos currTagPos = (TagPos) it.previous();
                    if (tagName.equals(currTagPos.name)) {
                        return currTagPos;
                    } else if (fatalTag != null && fatalTag.equals(currTagPos.name)) {
                        // do not search past a fatal tag for this tag
                        return null;
                    }
                }
            }

            return null;
        }

        private boolean tagExists(String tagName) {
            TagPos tagPos = findTag(tagName);
            return tagPos != null;
        }

        private TagPos findTagToPlaceRubbish() {
            TagPos result = null, prev = null;

            if ( !isEmpty() ) {
                ListIterator it = list.listIterator( list.size() );
                while ( it.hasPrevious() ) {
                    result = (TagPos) it.previous();
                    if ( result.info == null || result.info.allowsAnything() ) {
                    	if (prev != null) {
                            return prev;
                        }
                    }
                    prev = result;
                }
            }

            return result;
        }

        private boolean tagEncountered(String tagName) {
        	return set.contains(tagName);
        }

        /**
         * Checks if any of tags specified in the set are already open.
         * @param tags
         */
        private boolean someAlreadyOpen(Set tags) {
        	Iterator it = list.iterator();
            while ( it.hasNext() ) {
            	TagPos curr = (TagPos) it.next();
            	if ( tags.contains(curr.name) ) {
            		return true;
            	}
            }


            return false;
        }
    }

    private CleanerProperties properties;

    private ITagInfoProvider tagInfoProvider;

    private CleanerTransformations transformations = null;

    private transient OpenTags _openTags;
    private transient boolean _headOpened = false;
    private transient boolean _bodyOpened = false;
    private transient Set _headTags = new LinkedHashSet();
    private Set allTags = new TreeSet();

    private TagNode htmlNode;
    private TagNode bodyNode;
    private TagNode headNode;
    private TagNode rootNode;

    private Set pruneTagSet = new HashSet();
    private Set pruneNodeSet = new HashSet();

    /**
     * Constructor - creates cleaner instance with default tag info provider and default properties.
     */
    public HtmlCleaner() {
        this(null, null);
    }

    /**
     * Constructor - creates the instance with specified tag info provider and default properties
     * @param tagInfoProvider Provider for tag filtering and balancing
     */
    public HtmlCleaner(ITagInfoProvider tagInfoProvider) {
        this(tagInfoProvider, null);
    }

    /**
     * Constructor - creates the instance with default tag info provider and specified properties
     * @param properties Properties used during parsing and serializing
     */
    public HtmlCleaner(CleanerProperties properties) {
        this(null, properties);
    }

    /**
	 * Constructor - creates the instance with specified tag info provider and specified properties
	 * @param tagInfoProvider Provider for tag filtering and balancing
	 * @param properties Properties used during parsing and serializing
	 */
	public HtmlCleaner(ITagInfoProvider tagInfoProvider, CleanerProperties properties) {
        this.tagInfoProvider = tagInfoProvider == null ? DefaultTagProvider.getInstance() : tagInfoProvider;
        this.properties = properties == null ? new CleanerProperties() : properties;
        this.properties.tagInfoProvider = this.tagInfoProvider;
    }

    public TagNode clean(String htmlContent) throws IOException {
        return clean( new StringReader(htmlContent) );
    }

    public TagNode clean(File file, String charset) throws IOException {
        FileInputStream in = new FileInputStream(file);
        Reader reader = new InputStreamReader(in, charset);
        return clean(reader);
    }

    public TagNode clean(File file) throws IOException {
        return clean(file, DEFAULT_CHARSET);
    }

    public TagNode clean(URL url, String charset) throws IOException {
        StringBuffer content = Utils.readUrl(url, charset);
        Reader reader = new StringReader( content.toString() );
        return clean(reader);
    }

    public TagNode clean(URL url) throws IOException {
        return clean(url, DEFAULT_CHARSET);
    }

    public TagNode clean(InputStream in, String charset) throws IOException {
        return clean( new InputStreamReader(in, charset) );
    }

    public TagNode clean(InputStream in) throws IOException {
        return clean(in, DEFAULT_CHARSET);
    }

    /**
     * Basic version of the cleaning call.
     * @param reader
     * @return An instance of TagNode object which is the root of the XML tree.
     * @throws IOException
     */
    public TagNode clean(Reader reader) throws IOException {
        _openTags = new OpenTags();
        _headOpened = false;
        _bodyOpened = false;
        _headTags.clear();
        allTags.clear();
        setPruneTags(properties.pruneTags);

        htmlNode = new TagNode("html", this);
        bodyNode = new TagNode("body", this);
        headNode = new TagNode("head", this);
        rootNode = null;
        htmlNode.addChild(headNode);
        htmlNode.addChild(bodyNode);

        HtmlTokenizer htmlTokenizer = new HtmlTokenizer(this, reader);

		htmlTokenizer.start();

        List nodeList = htmlTokenizer.getTokenList();
        closeAll(nodeList);
        createDocumentNodes(nodeList);

        calculateRootNode( htmlTokenizer.getNamespacePrefixes() );

        // if there are some nodes to prune from tree
        if ( pruneNodeSet != null && !pruneNodeSet.isEmpty() ) {
            Iterator iterator = pruneNodeSet.iterator();
            while (iterator.hasNext()) {
                TagNode tagNode = (TagNode) iterator.next();
                TagNode parent = tagNode.getParent();
                if (parent != null) {
                    parent.removeChild(tagNode);
                }
            }
        }

        rootNode.setDocType( htmlTokenizer.getDocType() );

        return rootNode;
    }

    /**
     * Assigns root node to internal variable and adds neccessery xmlns
     * attributes if cleaner if namespaces aware.
     * Root node of the result depends on parameter "omitHtmlEnvelope".
     * If it is set, then first child of the body will be root node,
     * or html will be root node otherwise.
     *
     * @param namespacePrefixes
     */
    private void calculateRootNode(Set namespacePrefixes) {
        this.rootNode =  this.htmlNode;

        if (properties.omitHtmlEnvelope) {
            List bodyChildren = this.bodyNode.getChildren();
            if (bodyChildren != null) {
                Iterator iterator = bodyChildren.iterator();
                while (iterator.hasNext()) {
                    Object currChild = iterator.next();
                    // if found child that is tag itself, then return it
                    if (currChild instanceof TagNode) {
                        this.rootNode = (TagNode)currChild;
                    }
                }
            }
        }

        Map atts = this.rootNode.getAttributes();

        if (properties.namespacesAware && namespacePrefixes != null) {
            Iterator iterator = namespacePrefixes.iterator();
            while (iterator.hasNext()) {
                String prefix = (String) iterator.next();
                String xmlnsAtt = "xmlns:" + prefix;
                if ( !atts.containsKey(xmlnsAtt) ) {
                    this.rootNode.addAttribute(xmlnsAtt, prefix);
                }
            }
        }
    }

    /**
     * Add attributes from specified map to the specified tag.
     * If some attribute already exist it is preserved.
     * @param tag
     * @param attributes
     */
	private void addAttributesToTag(TagNode tag, Map attributes) {
		if (attributes != null) {
			Map tagAttributes = tag.getAttributes();
			Iterator it = attributes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry currEntry = (Map.Entry) it.next();
				String attName = (String) currEntry.getKey();
				if ( !tagAttributes.containsKey(attName) ) {
					String attValue = (String) currEntry.getValue();
					tag.addAttribute(attName, attValue);
				}
			}
		}
	}

    /**
     * Checks if open fatal tag is missing if there is a fatal tag for
     * the specified tag.
     * @param tag
     */
    private boolean isFatalTagSatisfied(TagInfo tag) {
    	if (tag != null) {
            String fatalTagName = tag.getFatalTag();
            return fatalTagName == null ? true : _openTags.tagExists(fatalTagName);
    	}

    	return true;
    }

    /**
     * Check if specified tag requires parent tag, but that parent
     * tag is missing in the appropriate context.
     * @param tag
     */
    private boolean mustAddRequiredParent(TagInfo tag) {
    	if (tag != null) {
    		String requiredParent = tag.getRequiredParent();
    		if (requiredParent != null) {
	    		String fatalTag = tag.getFatalTag();
                int fatalTagPositon = -1;
                if (fatalTag != null) {
                    TagPos tagPos = _openTags.findTag(fatalTag);
                    if (tagPos != null) {
                        fatalTagPositon = tagPos.position;
                    }
                }

	    		// iterates through the list of open tags from the end and check if there is some higher
	    		ListIterator it = _openTags.list.listIterator( _openTags.list.size() );
	            while ( it.hasPrevious() ) {
	            	TagPos currTagPos = (TagPos) it.previous();
	            	if (tag.isHigher(currTagPos.name)) {
	            		return currTagPos.position <= fatalTagPositon;
	            	}
	            }

	            return true;
    		}
    	}

    	return false;
    }

    private TagNode createTagNode(TagNode startTagToken) {
    	startTagToken.setFormed();
    	return startTagToken;
    }

    private boolean isAllowedInLastOpenTag(BaseToken token) {
        TagPos last = _openTags.getLastTagPos();
        if (last != null) {
			 if (last.info != null) {
                 return last.info.allowsItem(token);
			 }
		}

		return true;
    }

    private void saveToLastOpenTag(List nodeList, Object tokenToAdd) {
        TagPos last = _openTags.getLastTagPos();
        if ( last != null && last.info != null && last.info.isIgnorePermitted() ) {
            return;
        }

        TagPos rubbishPos = _openTags.findTagToPlaceRubbish();
        if (rubbishPos != null) {
    		TagNode startTagToken = (TagNode) nodeList.get(rubbishPos.position);
            startTagToken.addItemForMoving(tokenToAdd);
        }
    }

    private boolean isStartToken(Object o) {
    	return (o instanceof TagNode) && !((TagNode)o).isFormed();
    }

	void makeTree(List nodeList, ListIterator nodeIterator) {
		// process while not reach the end of the list
		while ( nodeIterator.hasNext() ) {
			BaseToken token = (BaseToken) nodeIterator.next();

            if (token instanceof EndTagToken) {
				EndTagToken endTagToken = (EndTagToken) token;
				String tagName = endTagToken.getName();
				TagInfo tag = tagInfoProvider.getTagInfo(tagName);

				if ( (tag == null && properties.omitUnknownTags) || (tag != null && tag.isDeprecated() && properties.omitDeprecatedTags) ) {
					nodeIterator.set(null);
				} else if ( tag != null && !tag.allowsBody() ) {
					nodeIterator.set(null);
				} else {
					TagPos matchingPosition = _openTags.findTag(tagName);

                    if (matchingPosition != null) {
                        List closed = closeSnippet(nodeList, matchingPosition, endTagToken);
                        nodeIterator.set(null);
                        for (int i = closed.size() - 1; i >= 1; i--) {
                            TagNode closedTag = (TagNode) closed.get(i);
                            if ( tag != null && tag.isContinueAfter(closedTag.getName()) ) {
                                nodeIterator.add( closedTag.makeCopy() );
                                nodeIterator.previous();
                            }
                        }
                    } else if ( !isAllowedInLastOpenTag(token) ) {
                        saveToLastOpenTag(nodeList, token);
                        nodeIterator.set(null);
                    }
                }
			} else if ( isStartToken(token) ) {
                TagNode startTagToken = (TagNode) token;
				String tagName = startTagToken.getName();
				TagInfo tag = tagInfoProvider.getTagInfo(tagName);

                TagPos lastTagPos = _openTags.isEmpty() ? null : _openTags.getLastTagPos();
                TagInfo lastTagInfo = lastTagPos == null ? null : tagInfoProvider.getTagInfo(lastTagPos.name);

                // add tag to set of all tags
				allTags.add(tagName);

                // HTML open tag
                if ( "html".equals(tagName) ) {
					addAttributesToTag(htmlNode, startTagToken.getAttributes());
					nodeIterator.set(null);
                // BODY open tag
                } else if ( "body".equals(tagName) ) {
                    _bodyOpened = true;
                    addAttributesToTag(bodyNode, startTagToken.getAttributes());
					nodeIterator.set(null);
                // HEAD open tag
                } else if ( "head".equals(tagName) ) {
                    _headOpened = true;
                    addAttributesToTag(headNode, startTagToken.getAttributes());
					nodeIterator.set(null);
                // unknows HTML tag and unknown tags are not allowed
                } else if ( (tag == null && properties.omitUnknownTags) || (tag != null && tag.isDeprecated() && properties.omitDeprecatedTags) ) {
                    nodeIterator.set(null);
                // if current tag is unknown and last open tag doesn't allow any other tags in its body
                } else if ( tag == null && lastTagInfo != null && !lastTagInfo.allowsAnything() ) {
                    closeSnippet(nodeList, lastTagPos, startTagToken);
                    nodeIterator.previous();
                } else if ( tag != null && tag.hasPermittedTags() && _openTags.someAlreadyOpen(tag.getPermittedTags()) ) {
                	nodeIterator.set(null);
                // if tag that must be unique, ignore this occurence
                } else if ( tag != null && tag.isUnique() && _openTags.tagEncountered(tagName) ) {
                	nodeIterator.set(null);
                // if there is no required outer tag without that this open tag is ignored
                } else if ( !isFatalTagSatisfied(tag) ) {
					nodeIterator.set(null);
                // if there is no required parent tag - it must be added before this open tag
                } else if ( mustAddRequiredParent(tag) ) {
					String requiredParent = tag.getRequiredParent();
					TagNode requiredParentStartToken = new TagNode(requiredParent, this);
					nodeIterator.previous();
					nodeIterator.add(requiredParentStartToken);
					nodeIterator.previous();
                // if last open tag has lower presidence then this, it must be closed
                } else if ( tag != null && lastTagPos != null && tag.isMustCloseTag(lastTagInfo) ) {
					List closed = closeSnippet(nodeList, lastTagPos, startTagToken);
					int closedCount = closed.size();

					// it is needed to copy some tags again in front of current, if there are any
					if ( tag.hasCopyTags() && closedCount > 0 ) {
						// first iterates over list from the back and collects all start tokens
						// in sequence that must be copied
						ListIterator closedIt = closed.listIterator(closedCount);
						List toBeCopied = new ArrayList();
						while (closedIt.hasPrevious()) {
							TagNode currStartToken = (TagNode) closedIt.previous();
							if ( tag.isCopy(currStartToken.getName()) ) {
								toBeCopied.add(0, currStartToken);
							} else {
								break;
							}
						}

						if (toBeCopied.size() > 0) {
							Iterator copyIt = toBeCopied.iterator();
							while (copyIt.hasNext()) {
								TagNode currStartToken = (TagNode) copyIt.next();
								nodeIterator.add( currStartToken.makeCopy() );
							}

                            // back to the previous place, before adding new start tokens
							for (int i = 0; i < toBeCopied.size(); i++) {
								nodeIterator.previous();
							}
                        }
					}

                    nodeIterator.previous();
				// if this open tag is not allowed inside last open tag, then it must be moved to the place where it can be
                } else if ( !isAllowedInLastOpenTag(token) ) {
                    saveToLastOpenTag(nodeList, token);
                    nodeIterator.set(null);
				// if it is known HTML tag but doesn't allow body, it is immediately closed
                } else if ( tag != null && !tag.allowsBody() ) {
					TagNode newTagNode = createTagNode(startTagToken);
                    addPossibleHeadCandidate(tag, newTagNode);
                    nodeIterator.set(newTagNode);
				// default case - just remember this open tag and go further
                } else {
                    _openTags.addTag( tagName, nodeIterator.previousIndex() );
                }
			} else {
				if ( !isAllowedInLastOpenTag(token) ) {
                    saveToLastOpenTag(nodeList, token);
                    nodeIterator.set(null);
				}
			}
		}
    }

	private void createDocumentNodes(List listNodes) {
		Iterator it = listNodes.iterator();
        while (it.hasNext()) {
            Object child = it.next();

            if (child == null) {
            	continue;
            }

			boolean toAdd = true;

            if (child instanceof TagNode) {
                TagNode node = (TagNode) child;
                TagInfo tag = tagInfoProvider.getTagInfo( node.getName() );
                addPossibleHeadCandidate(tag, node);
			} else {
				if (child instanceof ContentToken) {
					toAdd = !"".equals(child.toString());
				}
			}

			if (toAdd) {
				bodyNode.addChild(child);
			}
        }

        // move all viable head candidates to head section of the tree
        Iterator headIterator = _headTags.iterator();
        while (headIterator.hasNext()) {
            TagNode headCandidateNode = (TagNode) headIterator.next();

            // check if this node is already inside a candidate for moving to head
            TagNode parent = headCandidateNode.getParent();
            boolean toMove = true;
            while (parent != null) {
                if ( _headTags.contains(parent) ) {
                    toMove = false;
                    break;
                }
                parent = parent.getParent();
            }

            if (toMove) {
                headCandidateNode.removeFromTree();
                headNode.addChild(headCandidateNode);
            }
        }
    }

	private List closeSnippet(List nodeList, TagPos tagPos, Object toNode) {
		List closed = new ArrayList();
		ListIterator it = nodeList.listIterator(tagPos.position);

		TagNode tagNode = null;
		Object item = it.next();
		boolean isListEnd = false;

		while ( (toNode == null && !isListEnd) || (toNode != null && item != toNode) ) {
			if ( isStartToken(item) ) {
                TagNode startTagToken = (TagNode) item;
                closed.add(startTagToken);
                List itemsToMove = startTagToken.getItemsToMove();
                if (itemsToMove != null) {
            		OpenTags prevOpenTags = _openTags;
            		_openTags = new OpenTags();
            		makeTree(itemsToMove, itemsToMove.listIterator(0));
                    closeAll(itemsToMove);
                    startTagToken.setItemsToMove(null);
                    _openTags = prevOpenTags;
                }

                TagNode newTagNode = createTagNode(startTagToken);
                TagInfo tag = tagInfoProvider.getTagInfo( newTagNode.getName() );
                addPossibleHeadCandidate(tag, newTagNode);
                if (tagNode != null) {
					tagNode.addChildren(itemsToMove);
                    tagNode.addChild(newTagNode);
                    it.set(null);
                } else {
                	if (itemsToMove != null) {
                		itemsToMove.add(newTagNode);
                		it.set(itemsToMove);
                	} else {
                		it.set(newTagNode);
                	}
                }

                _openTags.removeTag( newTagNode.getName() );
                tagNode = newTagNode;
            } else {
            	if (tagNode != null) {
            		it.set(null);
            		if (item != null) {
            			tagNode.addChild(item);
                    }
                }
            }

			if ( it.hasNext() ) {
				item = it.next();
			} else {
				isListEnd = true;
			}
		}

		return closed;
    }

    /**
     * Close all unclosed tags if there are any.
     */
    private void closeAll(List nodeList) {
        TagPos firstTagPos = _openTags.findFirstTagPos();
        if (firstTagPos != null) {
            closeSnippet(nodeList, firstTagPos, null);
        }
    }

    /**
     * Checks if specified tag with specified info is candidate for moving to head section.
     * @param tagInfo
     * @param tagNode
     */
    private void addPossibleHeadCandidate(TagInfo tagInfo, TagNode tagNode) {
        if (tagInfo != null && tagNode != null) {
            if ( tagInfo.isHeadTag() || (tagInfo.isHeadAndBodyTag() && _headOpened && !_bodyOpened) ) {
                _headTags.add(tagNode);
            }
        }
    }

    public CleanerProperties getProperties() {
        return properties;
    }

    public Set getPruneTagSet() {
        return pruneTagSet;
    }

    private void setPruneTags(String pruneTags) {
        pruneTagSet.clear();
        pruneNodeSet.clear();
        if (pruneTags != null) {
            StringTokenizer tokenizer = new StringTokenizer(pruneTags, ",");
            while ( tokenizer.hasMoreTokens() ) {
                pruneTagSet.add( tokenizer.nextToken().trim().toLowerCase() );
            }
        }
    }

    void addPruneNode(TagNode node) {
        this.pruneNodeSet.add(node);
    }

    public Set getAllTags() {
		return allTags;
	}

    /**
     * @return ITagInfoProvider instance for this HtmlCleaner
     */
    public ITagInfoProvider getTagInfoProvider() {
        return tagInfoProvider;
    }

    /**
     * @return Transormations defined for this instance of cleaner
     */
    public CleanerTransformations getTransformations() {
        return transformations;
    }

    /**
     * Sets tranformations for this cleaner instance.
     * @param transformations
     */
    public void setTransformations(CleanerTransformations transformations) {
        this.transformations = transformations;
    }

    /**
     * For the specified node, returns it's content as string.
     * @param node
     */
    public String getInnerHtml(TagNode node) {
        if (node != null) {
            try {
                String content = new SimpleXmlSerializer(properties).getXmlAsString(node);
                int index1 = content.indexOf("<" + node.getName());
                index1 = content.indexOf('>', index1 + 1);
                int index2 = content.lastIndexOf('<');
                return index1 >= 0 && index1 <= index2 ? content.substring(index1 + 1, index2) : null;
            } catch (IOException e) {
                throw new HtmlCleanerException(e);
            }
        } else {
            throw new HtmlCleanerException("Cannot return inner html of the null node!");
        }
    }

    /**
     * For the specified tag node, defines it's html content. This causes cleaner to
     * reclean given html portion and insert it inside the node instead of previous content.
     * @param node
     * @param content
     */
    public void setInnerHtml(TagNode node, String content) {
        if (node != null) {
            String nodeName = node.getName();
            StringBuffer html = new StringBuffer();
            html.append("<" + nodeName + " marker=''>");
            html.append(content);
            html.append("</" + nodeName + ">");
            TagNode parent = node.getParent();
            while (parent != null) {
                String parentName = parent.getName();
                html.insert(0, "<" + parentName + ">");
                html.append("</" + parentName + ">");
                parent = parent.getParent();
            }

            try {
                TagNode rootNode = clean( html.toString() );
                TagNode cleanedNode = rootNode.findElementHavingAttribute("marker", true);
                if (cleanedNode != null) {
                    node.setChildren( cleanedNode.getChildren() );
                }
            } catch (IOException e) {
                throw new HtmlCleanerException(e);
            }
        }
    }

}