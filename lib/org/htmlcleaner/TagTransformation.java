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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * Describes how specified tag is transformed to another one, or is ignored during parsing
 */
public class TagTransformation {
    public static String VAR_START = "${";
    public static String VAR_END = "}";
    private String sourceTag;
    private String destTag;
    private boolean preserveSourceAttributes;
    private Map<String, String> attributeTransformations = new LinkedHashMap<String, String>();
    private List<AttributeTransformation> attributePatternTransformations = new ArrayList<AttributeTransformation>();
    public TagTransformation() {
        this.preserveSourceAttributes = true;
    }
    /**
     * Creates new tag transformation from source tag to target tag specifying whether
     * source tag attributes are preserved.
     * @param sourceTag Name of the tag to be transformed.
     * @param destTag Name of tag to which source tag is to be transformed.
     * @param preserveSourceAttributes Tells whether source tag attributes are preserved in transformation.
     */
    public TagTransformation(String sourceTag, String destTag, boolean preserveSourceAttributes) {
        this.sourceTag = sourceTag.toLowerCase();
        if (destTag == null) {
            this.destTag = null;
        } else {
            this.destTag = Utils.isValidXmlIdentifier(destTag) ? destTag.toLowerCase() : sourceTag;
        }
        this.preserveSourceAttributes = preserveSourceAttributes;
    }

    /**
     * Creates new tag transformation from source tag to target tag preserving
     * all source tag attributes.
     * @param sourceTag Name of the tag to be transformed.
     * @param destTag Name of tag to which source tag is to be transformed.
     */
    public TagTransformation(String sourceTag, String destTag) {
        this(sourceTag, destTag, true);
    }

    /**
     * Creates new tag transformation in which specified tag will be skipped (ignored)
     * during parsing process.
     * @param sourceTag
     */
    public TagTransformation(String sourceTag) {
        this(sourceTag, null);
    }

    /**
     * Adds new attribute transformation to this tag transformation. It tells how destination
     * attribute will look like. Small templating mechanism is used to describe attribute value:
     * all names between ${ and } inside the template are evaluated against source tag attributes.
     * That way one can make attribute values consist of mix of source tag attributes.
     *
     * @param targetAttName Name of the destination attribute 
     * @param transformationDesc Template describing attribute value.
     */
    public void addAttributeTransformation(String targetAttName, String transformationDesc) {
        attributeTransformations.put(targetAttName.toLowerCase(), transformationDesc);
    }
    public void addAttributePatternTransformation(Pattern attNamePattern, String transformationDesc) {
        attributePatternTransformations.add(new AttributeTransformationPatternImpl(attNamePattern, null, transformationDesc));
    }
    public void addAttributePatternTransformation(Pattern attNamePattern, Pattern attValuePattern, String transformationDesc) {
        addAttributePatternTransformation(new AttributeTransformationPatternImpl(attNamePattern, attValuePattern, transformationDesc));
    }
    /**
     * @param attributeTransformation
     */
    public void addAttributePatternTransformation(AttributeTransformation attributeTransformation) {
        if (attributePatternTransformations == null) {
            attributePatternTransformations = new ArrayList<AttributeTransformation>();
        }
        attributePatternTransformations.add(attributeTransformation);
    }
    /**
     * Adds new attribute transformation in which destination attrbute will not exists
     * (simply removes it from list of attributes).
     * @param targetAttName
     */
    public void addAttributeTransformation(String targetAttName) {
        addAttributeTransformation(targetAttName, null);
    }

    boolean hasAttributeTransformations() {
        return attributeTransformations != null || attributePatternTransformations != null;
    }

    String getSourceTag() {
        return sourceTag;
    }

    String getDestTag() {
        return destTag;
    }

    boolean isPreserveSourceAttributes() {
        return preserveSourceAttributes;
    }

    Map<String, String> getAttributeTransformations() {
        return attributeTransformations;
    }
    /**
     * @param attributes
     */
    public Map<String, String> applyTagTransformations(Map<String, String> attributes) {
        boolean isPreserveSourceAtts = isPreserveSourceAttributes();
        boolean hasAttTransforms = hasAttributeTransformations();
        if ( hasAttTransforms || !isPreserveSourceAtts) {
            Map<String, String> newAttributes = isPreserveSourceAtts ? new LinkedHashMap<String, String>(attributes) : new LinkedHashMap<String, String>();
            if (hasAttTransforms) {
                Map<String, String> map = getAttributeTransformations();
                Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    String attName = (String) entry.getKey();
                    String template = (String) entry.getValue();
                    if (template == null) {
                        newAttributes.remove(attName);
                    } else {
                        String attValue = evaluateTemplate(template, attributes);
                        newAttributes.put(attName, attValue);
                    }
                }
                
                for(AttributeTransformation attributeTransformation: this.attributePatternTransformations) {
                    for(Map.Entry<String, String> entry1: attributes.entrySet()) {
                        String attName = entry1.getKey();
                        if (attributeTransformation.satisfy(attName, entry1.getValue())) {
                            String template = attributeTransformation.getTemplate();
                            if (template == null) {
                                newAttributes.remove(attName);
                            } else {
                                String attValue = evaluateTemplate(template, attributes);
                                newAttributes.put(attName, attValue);
                            }
                        }
                    }
                }
            }
            return newAttributes;
        } else {
            return attributes;
        }
    }
    /**
     * Evaluates string template for specified map of variables. Template string can contain
     * dynamic parts in the form of ${VARNAME}. Each such part is replaced with value of the
     * variable if such exists in the map, or with empty string otherwise.
     *
     * @param template Template string
     * @param variables Map of variables (can be null)
     * @return Evaluated string
     */
    public String evaluateTemplate(String template, Map<String, String> variables) {
        if (template == null) {
            return template;
        }

        StringBuffer result = new StringBuffer();

        int startIndex = template.indexOf(VAR_START);
        int endIndex = -1;

        while (startIndex >= 0 && startIndex < template.length()) {
            result.append( template.substring(endIndex + 1, startIndex) );
            endIndex = template.indexOf(VAR_END, startIndex);

            if (endIndex > startIndex) {
                String varName = template.substring(startIndex + VAR_START.length(), endIndex);
                Object resultObj = variables != null ? variables.get(varName.toLowerCase()) : "";
                result.append( resultObj == null ? "" : resultObj.toString() );
            }

            startIndex = template.indexOf( VAR_START, Math.max(endIndex + VAR_END.length(), startIndex + 1) );
        }

        result.append( template.substring(endIndex + 1) );

        return result.toString();
    }
}