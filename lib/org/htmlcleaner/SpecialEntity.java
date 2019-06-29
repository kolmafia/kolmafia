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

public class SpecialEntity{
    private final String key;
    private final int intCode;
    // escaped value outputed when generating html
    private final String htmlString;
    private boolean htmlSpecialEntity;
    // escaped value when outputting html
    private final String escapedXmlString;

    /**
     *
     * @param key value between & and the ';' example 'amp' for '&amp;'
     * @param intCode
     * @param htmlString
     * @param htmlSpecialEntity entity is affected by translateSpecialEntities property setting.
     */
    public SpecialEntity(String key, int intCode, String htmlString, boolean htmlSpecialEntity) {
        this.key = key;
        this.intCode = intCode;
        String str = "&" + key +";";
        if ( htmlString != null) {
            this.htmlString = htmlString;
        } else {
            this.htmlString = str;
        }
        if ( htmlSpecialEntity ) {
            this.escapedXmlString = String.valueOf((char)this.intCode);
        } else {
            this.escapedXmlString = str;
        }
        this.htmlSpecialEntity = htmlSpecialEntity;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the intCode
     */
    public int intValue() {
        return intCode;
    }

    /**
     * @return the domString
     */
    public String getHtmlString() {
        return htmlString;
    }

    public String getEscapedXmlString() {
        return this.escapedXmlString;
    }

    public String getEscaped(boolean htmlEscaped) {
        return htmlEscaped?this.getHtmlString():this.getEscapedXmlString();
    }

    /**
     * @return the translateSpecialEntities
     */
    public boolean isHtmlSpecialEntity() {
        return htmlSpecialEntity;
    }

    /**
     * @return {@link #intValue()} cast to an char
     */
    public char charValue() {
        return (char) intValue();
    }
    /**
     * @return Numeric Character Reference in decimal format
     */
    public String getDecimalNCR() {
        return "&#" + intCode + ";";
    }

    /**
     * @return Numeric Character Reference in hex format
     */
    public String getHexNCR() {
        return "&#x" + Integer.toHexString(intCode) + ";";
    }

    /**
     * @return Escaped value of the entity
     */
    public String getEscapedValue() {
        return "&" + key + ";";
    }
}