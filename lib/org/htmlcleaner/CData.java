/*  Copyright (c) 2006-2013, the HtmlCleaner Project
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

public class CData extends ContentNode implements HtmlNode {
	
    public static final String BEGIN_CDATA = "<![CDATA[";
    public static final String END_CDATA = "]]>";
    public static final String SAFE_BEGIN_CDATA = "/*<![CDATA[*/";
    public static final String SAFE_END_CDATA = "/*]]>*/";
    public static final String SAFE_BEGIN_CDATA_ALT = "//<![CDATA[";
    public static final String SAFE_END_CDATA_ALT = "//]]>";
	
	public CData(String content){
		super(content);	
	}
	
	public String getContentWithoutStartAndEndTokens(){
		return this.content;
	}

	/* (non-Javadoc)
	 * @see org.htmlcleaner.ContentNode#getContent()
	 */
	@Override
	public String getContent() {
		return getContentWithoutStartAndEndTokens();
	}

	/* (non-Javadoc)
	 * @see org.htmlcleaner.ContentNode#toString()
	 */
	@Override
	public String toString() {
		return getContentWithStartAndEndTokens();
	}
	
	public String getContentWithStartAndEndTokens(){
		return SAFE_BEGIN_CDATA + this.content + SAFE_END_CDATA;
	}
	
	
	
	
}
