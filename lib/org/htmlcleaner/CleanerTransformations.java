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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains transformation collection.
 */
public class CleanerTransformations { 

    private Map mappings = new HashMap();
    private TagTransformation globalTransformations=new TagTransformation(); 

    public CleanerTransformations() {
        
    }
    /**
     * @param transInfos
     */
    public CleanerTransformations(Map transInfos) {
        updateTagTransformations(transInfos);
    }

    /**
     * Adds specified tag transformation to the collection.
     * @param tagTransformation
     */
    public void addTransformation(TagTransformation tagTransformation) {
        if (tagTransformation != null) {
            mappings.put( tagTransformation.getSourceTag(), tagTransformation );
        }
    }
    
    public void addGlobalTransformation(AttributeTransformation attributeTransformation) {
        globalTransformations.addAttributePatternTransformation(attributeTransformation);
    }

    public boolean hasTransformationForTag(String tagName)  {
        return tagName != null && mappings.containsKey(tagName.toLowerCase());
    }

    public TagTransformation getTransformation(String tagName) {
        return tagName != null ? (TagTransformation) mappings.get(tagName.toLowerCase()) : null; 
    }

    public void updateTagTransformations(String key, String value) {
        int index = key.indexOf('.');
    
        // new tag transformation case (tagname[=destname[,preserveatts]])
        if (index <= 0) {
            String destTag = null;
            boolean preserveSourceAtts = true;
            if (value != null) {
                String[] tokens = Utils.tokenize(value, ",;");
                if (tokens.length > 0) {
                    destTag = tokens[0];
                }
                if (tokens.length > 1) {
                    preserveSourceAtts = "true".equalsIgnoreCase(tokens[1]) ||
                                         "yes".equalsIgnoreCase(tokens[1]) ||
                                         "1".equals(tokens[1]);
                }
            }
            TagTransformation newTagTrans = new TagTransformation(key, destTag, preserveSourceAtts);
            addTransformation(newTagTrans);
        } else {    // attribute transformation description
            String[] parts = Utils.tokenize(key, ".");
            String tagName = parts[0];
            TagTransformation trans = getTransformation(tagName);
            if (trans != null) {
                trans.addAttributeTransformation(parts[1], value);
            }
        }
    }
    public void updateTagTransformations(Map transInfos) {
        Iterator iterator = transInfos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String tag = (String) entry.getKey();
            String value = (String) entry.getValue();
            updateTagTransformations(tag, value);
        }
    }
    public Map<String, String> transformAttributes(String originalTagName, Map<String, String> attributes) {
        TagTransformation tagTrans = getTransformation(originalTagName);
        Map<String, String> results;
        if ( tagTrans != null ) {
            results = tagTrans.applyTagTransformations(attributes);
        } else {
            results = attributes;
        }
        return this.globalTransformations.applyTagTransformations(results);
    }

    public String getTagName(String tagName) {
        TagTransformation tagTransformation = null;
        if (hasTransformationForTag(tagName)) {
            tagTransformation = getTransformation(tagName);
            if (tagTransformation != null) {
                return tagTransformation.getDestTag();
            }
        }
        return tagName;
    }
    /**
     * 
     */
    public void clear() {
        this.mappings.clear();
    }
}