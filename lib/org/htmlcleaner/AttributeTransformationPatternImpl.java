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
 
    Additional work by Amplafi. -- All rights released.
 */
package org.htmlcleaner;

import java.util.regex.Pattern;

public class AttributeTransformationPatternImpl implements AttributeTransformation {
    private final Pattern attNamePattern;
    private final Pattern attValuePattern;
    private final String template;
    public AttributeTransformationPatternImpl(Pattern attNamePattern, Pattern attValuePattern, String template) {
        this.attNamePattern = attNamePattern;
        this.attValuePattern = attValuePattern;
        this.template = template;
    }
    public AttributeTransformationPatternImpl(String attNamePattern, String attValuePattern, String template) {
        this.attNamePattern = attNamePattern ==null?null:Pattern.compile(attNamePattern);
        this.attValuePattern = attValuePattern == null? null: Pattern.compile(attValuePattern);
        this.template = template;
    }

    public boolean satisfy(String attName, String attValue) {
        if ( (attNamePattern == null || attNamePattern.matcher(attName).find()) && (attValuePattern ==null || attValuePattern.matcher(attValue).find())){
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the template
     */
    public String getTemplate() {
        return template;
    }
}