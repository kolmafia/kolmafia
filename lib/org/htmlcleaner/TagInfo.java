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
 * It also contains rules for tag balancing. For each tag, list of dependent
 * tags may be defined. There are several kinds of dependencies used to reorder
 * tags:
 * <ul>
 *      <li>
 * 		  fatal tags - required outer tag - the tag will be ignored during
 *        parsing (will be skipped) if this fatal tag is missing. For example, most web
 *        browsers ignore elements TD, TR, TBODY if they are not in the context of TABLE tag.
 *      </li>
 *      <li>
 *        required enclosing tags - if there is no such, it is implicitly
 *        created. For example if TD is out of TR - open TR is created before.
 *      </li>
 *      <li>
 *        forbidden tags - it is not allowed to occur inside - for example
 *        FORM cannot be inside other FORM and it will be ignored during cleanup.
 *      </li>
 *      <li>
 *        allowed children tags - for example TR allows TD and TH. If there
 *        are some dependent allowed tags defined then cleaner ignores other tags, treating
 *        them as not allowed, unless they are in some other relationship with this tag.
 *      </li>
 *      <li>
 *        preferred child tag - where a child tag doesn't match, but we want to by default
 *        insert an intervening tag rather than just move it outside. For example, LI in UL, TD in TR.
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
 * Tag TR for instance (table row) may define the following dependencies:
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
 *          otherwise <code>tbody</code> will be implicitly created in front of it.</li>
 *      <li><code>tr</code> can contain <code>td</code> and <code>th</code>, all other tags and content will be pushed out of current
 *      limiting context, in the case of html tables, in front of enclosing <code>table</code> tag.</li>
 *      <li>if previous open tag is one of <code>tr</code>, <code>caption</code> or <code>colgroup</code>, it will be implicitly closed.</li>
 *   </ul>
 * </p>
 */
public class TagInfo {

    public String getAssumedNamespace() {
		return assumedNamespace;
	}

	public void setAssumedNamespace(String assumedNamespace) {
		this.assumedNamespace = assumedNamespace;
	}

	private String name;
    private ContentType contentType;
    private Set<String> mustCloseTags = new HashSet<String>();
    private Set<String> higherTags = new HashSet<String>();
    private Set<String> childTags = new HashSet<String>();
    private Set<String> permittedTags = new HashSet<String>();
    private Set<String> copyTags = new HashSet<String>();
    private Set<String> continueAfterTags = new HashSet<String>();
    private BelongsTo belongsTo = BelongsTo.BODY;
    private Set<String>requiredParentTags = new HashSet<String>();
    private Set<String>fatalTags = new HashSet<String>();
    private String preferredChildTag = null;
    private String assumedNamespace = null;
    private boolean deprecated;
    private boolean unique;
    private boolean ignorePermitted;
    private CloseTag closeTag;
    private Display display;

    public TagInfo(String name, ContentType contentType, BelongsTo belongsTo, boolean deprecated, boolean unique, boolean ignorePermitted, CloseTag closeTag, Display display) {
        this.name = name;
        this.contentType = contentType;
        this.belongsTo = belongsTo;
        this.deprecated = deprecated;
        this.unique = unique;
        this.ignorePermitted = ignorePermitted;
        this.closeTag = closeTag;
        this.display = display;
    }

    public void defineFatalTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.fatalTags.add(currTag);
            this.higherTags.add(currTag);
        }
    }

    public void defineRequiredEnclosingTags(String commaSeparatedListOfTags) {
        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
        while (tokenizer.hasMoreTokens()) {
            String currTag = tokenizer.nextToken();
            this.requiredParentTags.add(currTag);
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

    public Display getDisplay() {
    	return display;
    }

    public void setDisplay(Display display) {
    	this.display = display;
    }

    public String getName() {
        return name;
    }


	public void setName(String name) {
        this.name = name;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public Set<String> getMustCloseTags() {
        return mustCloseTags;
    }

    public void setMustCloseTags(Set<String> mustCloseTags) {
        this.mustCloseTags = mustCloseTags;
    }

    public Set<String> getHigherTags() {
        return higherTags;
    }

    public void setHigherTags(Set<String> higherTags) {
        this.higherTags = higherTags;
    }

    public Set<String> getChildTags() {
        return childTags;
    }

    public void setChildTags(Set<String> childTags) {
        this.childTags = childTags;
    }

    public Set<String> getPermittedTags() {
        return permittedTags;
    }

    public void setPermittedTags(Set<String> permittedTags) {
        this.permittedTags = permittedTags;
    }

    public Set<String> getCopyTags() {
        return copyTags;
    }

    public void setCopyTags(Set<String> copyTags) {
        this.copyTags = copyTags;
    }

    public Set<String> getContinueAfterTags() {
        return continueAfterTags;
    }

    public void setContinueAfterTags(Set<String> continueAfterTags) {
        this.continueAfterTags = continueAfterTags;
    }

   public Set<String> getRequiredParentTags() {
        return requiredParentTags;
   }

    public void setRequiredParent(String requiredParent) {
        this.requiredParentTags.add(requiredParent);
    }

    public BelongsTo getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(BelongsTo belongsTo) {
        this.belongsTo = belongsTo;
    }
    
    public Set<String> getFatalTags(){
    	return this.fatalTags;
    }
    
    public boolean isFatalTag(String tag){
    	for (String fatalTag:this.fatalTags){
    		if (tag.equals(fatalTag)) return true;
    	}
    	return false;
    }

    public void setFatalTag(String fatalTag) {
        this.fatalTags.add(fatalTag);
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
        return ContentType.none == contentType;
    }

    public void setIgnorePermitted(boolean ignorePermitted) {
        this.ignorePermitted = ignorePermitted;
    }

    // other functionality

    boolean allowsBody() {
    	return ContentType.none != contentType;
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
    	return belongsTo == BelongsTo.HEAD;
    }

    boolean isHeadAndBodyTag() {
    	return belongsTo == BelongsTo.HEAD || belongsTo == BelongsTo.HEAD_AND_BODY;
    }

    boolean isMustCloseTag(TagInfo tagInfo) {
        if (tagInfo != null) {
            return mustCloseTags.contains( tagInfo.getName() ) || tagInfo.contentType == ContentType.text;
        }

        return false;
    }

    /**
     *
     * @param token
     * @return true if the passed token is allowed to be nested in a Tag with this TagInfo.
     */
    boolean allowsItem(BaseToken token) {
        if ( contentType != ContentType.none && token instanceof TagToken ) {
            TagToken tagToken = (TagToken) token;
            String tagName = tagToken.getName();
            if ( "script".equals(tagName) ) {
                return true;
            }
        }

        switch (contentType) {
        case all:
            if ( !childTags.isEmpty() ) {
                if ( token instanceof TagToken) {
                    return childTags.contains( ((TagToken)token).getName() );
                }
    		} else if ( !permittedTags.isEmpty() ) {
                if ( token instanceof TagToken) {
                    return !permittedTags.contains( ((TagToken)token).getName() );
                }
    		}
           return true;    			
        case text:
    		return !(token instanceof TagToken);
        case none:
            if ( token instanceof ContentNode ) {
                // allow white space in outputed html
                return ( (ContentNode)token).isBlank();
            } else if (!(token instanceof TagToken)) {
                // allow directives.
                return true;
            }
        default:
            return false;
        }
    }

    boolean allowsAnything() {
    	return ContentType.all == contentType && childTags.isEmpty();
    }

    /**
     * @return
     */
    public boolean isMinimizedTagPermitted() {
        return this.closeTag.isMinimizedTagPermitted();
    }

	public String getPreferredChildTag() {
		return preferredChildTag;
	}

	public void setPreferredChildTag(String preferredChildTag) {
		this.preferredChildTag = preferredChildTag;
	}

}