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

import java.util.*;

/**
 * <p>
 * Class contains information about single HTML tag.<br/>
 * It also contains rules for tag balancing. For each tag, list of dependant
 * tags may be defined. There are several kinds of dependancies used to reorder
 * tags:
 * <ul>
 *      <li>
 * 		  fatal tags - required outer tag - the tag will be ignored during
 *        parsing (will be skipped) if this fatal tag is missing. For example, most web
 *        browsers ignore elements TD, TR, TBODY if they are not in the context of TABLE tag.
 *      </li>
 *      <li>
 *        required enclosing tags - if there is no such, it is implicitely
 *        created. For example if TD is out of TR - open TR is created before.
 *      </li>
 *      <li>
 *        forbidden tags - it is not allowed to occure inside - for example
 *        FORM cannot be inside other FORM and it will be ignored during cleanup.
 *      </li>
 *      <li>
 *        allowed children tags - for example TR allowes TD and TH. If there
 *        are some dependant allowed tags defined then cleaner ignores other tags, treating
 *        them as unallowed, unless they are in some other relationship with this tag.
 *      </li>
 *      <li>
 *        higher level tags - for example for TR higher tags are THEAD, TBODY, TFOOT.
 *      </li>
 *      <li>
 *        tags that must be closed and copied - for example, in
 *        <code>&lt;a href="#"&gt;&lt;div&gt;....</code> tag A must be closed before DIV but
 *        copied again inside DIV.
 *      </li>
 *      <li>
 *        tags that must be closed before closing this tag and copied again after -
 *        for example, in <code>&lt;i&gt;&lt;b&gt;at&lt;/i&gt; first&lt;/b&gt; text </code>
 *        tag B must be closed before closing I, but it must be copied again after resulting
 *        finally in sequence: <code>&lt;i&gt;&lt;b&gt;at&lt;/b&gt;&lt;/i&gt;&lt;b&gt; first&lt;/b&gt; text </code>.
 *      </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Tag TR for instance (table row) may define the following dependancies:
 *      <ul>
 *          <li>fatal tag is <code>table</code></li>
 *          <li>required enclosing tag is <code>tbody</code></li>
 *          <li>allowed children tags are <code>td,th</code></li>
 *          <li>higher level tags are <code>thead,tfoot</code></li>
 *          <li>tags that muste be closed before are <code>tr,td,th,caption,colgroup</code></li>
 *      </ul>
 * meaning the following: <br>
 *   <ul>
 *      <li><code>tr</code> must be in context of <code>table</code>, otherwise it will be ignored,</li>
 *      <li><code>tr</code> may can be directly inside <code>tbody</code>, <code>tfoot</code> and <code>thead</code>,
 *          otherwise <code>tbody</code> will be implicitely created in front of it.</li>
 *      <li><code>tr</code> can contain <code>td</code> and <code>th</code>, all other tags and content will be pushed out of current
 *      limiting context, in the case of html tables, in front of enclosing <code>table</code> tag.</li>
 *      <li>if previous open tag is one of <code>tr</code>, <code>caption</code> or <code>colgroup</code>, it will be implicitely closed.</li>
 *   </ul>
 * </p>
 * <br>
 * Created by Vladimir Nikic.<br/>
 * Date: November, 2006
 */
public class TagInfo {

    protected static final int HEAD_AND_BODY = 0;
	protected static final int HEAD = 1;
	protected static final int BODY = 2;
	
	protected static final int CONTENT_ALL = 0;
	protected static final int CONTENT_NONE = 1;
	protected static final int CONTENT_TEXT = 2;

    private String name;
    private int contentType;
    private Set mustCloseTags = new HashSet();
    private Set higherTags = new HashSet();
    private Set childTags = new HashSet();
    private Set permittedTags = new HashSet();
    private Set copyTags = new HashSet();
    private Set continueAfterTags = new HashSet();
    private int belongsTo = BODY;
    private String requiredParent = null;
    private String fatalTag = null; 
    private boolean deprecated = false; 
    private boolean unique = false; 
    private boolean ignorePermitted = false;


    public TagInfo(String name, int contentType, int belongsTo, boolean depricated, boolean unique, boolean ignorePermitted) {
        this.name = name;
        this.contentType = contentType;
        this.belongsTo = belongsTo;
        this.deprecated = depricated;
        this.unique = unique;
        this.ignorePermitted = ignorePermitted;
    }

    public void defineFatalTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.fatalTag = currTag;
            this.higherTags.add(currTag);
        }
    }

    public void defineRequiredEnclosingTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.requiredParent = currTag;
            this.higherTags.add(currTag);
        }
    }

    public void defineForbiddenTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.permittedTags.add(currTag);
        }
    }

    public void defineAllowedChildrenTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.childTags.add(currTag);
        }
    }

    public void defineHigherLevelTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.higherTags.add(currTag);
        }
    }

    public void defineCloseBeforeCopyInsideTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.copyTags.add(currTag);
            this.mustCloseTags.add(currTag);
        }
    }

    public void defineCloseInsideCopyAfterTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.continueAfterTags.add(currTag);
        }
    }

    public void defineCloseBeforeTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.mustCloseTags.add(currTag);
        }
    }

    // getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getContentType() {
        return contentType;
    }

    public Set getMustCloseTags() {
        return mustCloseTags;
    }

    public void setMustCloseTags(Set mustCloseTags) {
        this.mustCloseTags = mustCloseTags;
    }

    public Set getHigherTags() {
        return higherTags;
    }

    public void setHigherTags(Set higherTags) {
        this.higherTags = higherTags;
    }

    public Set getChildTags() {
        return childTags;
    }

    public void setChildTags(Set childTags) {
        this.childTags = childTags;
    }

    public Set getPermittedTags() {
        return permittedTags;
    }

    public void setPermittedTags(Set permittedTags) {
        this.permittedTags = permittedTags;
    }

    public Set getCopyTags() {
        return copyTags;
    }

    public void setCopyTags(Set copyTags) {
        this.copyTags = copyTags;
    }

    public Set getContinueAfterTags() {
        return continueAfterTags;
    }

    public void setContinueAfterTags(Set continueAfterTags) {
        this.continueAfterTags = continueAfterTags;
    }

    public String getRequiredParent() {
        return requiredParent;
    }

    public void setRequiredParent(String requiredParent) {
        this.requiredParent = requiredParent;
    }

    public int getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(int belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getFatalTag() {
        return fatalTag;
    }

    public void setFatalTag(String fatalTag) {
        this.fatalTag = fatalTag;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isIgnorePermitted() {
        return ignorePermitted;
    }

    public boolean isEmptyTag() {
        return CONTENT_NONE == contentType;
    }

    public void setIgnorePermitted(boolean ignorePermitted) {
        this.ignorePermitted = ignorePermitted;
    }

    // other functionality

    boolean allowsBody() {
    	return CONTENT_NONE != contentType; 
    }
    
    boolean isHigher(String tagName) {
    	return higherTags.contains(tagName);
    }
    
    boolean isCopy(String tagName) {
    	return copyTags.contains(tagName);
    }

    boolean hasCopyTags() {
    	return !copyTags.isEmpty(); 
    }

    boolean isContinueAfter(String tagName) {
    	return continueAfterTags.contains(tagName);
    }
    
    boolean hasPermittedTags() {
    	return !permittedTags.isEmpty(); 
    }

    boolean isHeadTag() {
    	return belongsTo == HEAD;
    }
    
    boolean isHeadAndBodyTag() {
    	return belongsTo == HEAD || belongsTo == HEAD_AND_BODY;
    }

    boolean isMustCloseTag(TagInfo tagInfo) {
        if (tagInfo != null) {
            return mustCloseTags.contains( tagInfo.getName() ) || tagInfo.contentType == CONTENT_TEXT;
        }

        return false;
    }

    boolean allowsItem(BaseToken token) {
        if ( contentType != CONTENT_NONE && token instanceof TagToken ) {
            TagToken tagToken = (TagToken) token;
            String tagName = tagToken.getName();
            if ( "script".equals(tagName) ) {
                return true;
            }
        }

        if (CONTENT_ALL == contentType) {
            if ( !childTags.isEmpty() ) {
            	return token instanceof TagToken ? childTags.contains( ((TagToken)token).getName() ) : false;
    		} else if ( !permittedTags.isEmpty() ) {
    			return token instanceof TagToken ? !permittedTags.contains( ((TagToken)token).getName() ) : true;
    		}
            return true;
        } else if ( CONTENT_TEXT == contentType ) {
    		return !(token instanceof TagToken);
    	}
    	
    	return false;
    }
    
    boolean allowsAnything() {
    	return CONTENT_ALL == contentType && childTags.size() == 0;
    }

}