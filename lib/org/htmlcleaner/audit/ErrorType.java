/*  
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
package org.htmlcleaner.audit;

/**
 * Possible error codes (read messages) that cleaner uses to inform clients about reasons/actions that modification
 * involves.
 * @author Konstantin Burov (aectann@gmail.com)
 */
public enum ErrorType {

    /**
     * Tag which existence is <i>critical</i> for the current is missing. Most likely, current tag was pruned. Unlike
     * the {@link #RequiredParentMissing} this reports the problem when cleaner removed the tag instead of creating as
     * parent. See {@link org.htmlcleaner.TagInfo} for more detailed description of fatal and required tags.
     * <p>
     * <b>Example:</b>
     * <ul>
     * <li>&lt;option> tag without parent select
     * <li>&lt;tr> tag without parent &lt;table>
     * <li>...
     * </ul>
     */
    FatalTagMissing,
    /**
     * The tag wasn't found on list of allowed tags, thus it was removed.
     */
    NotAllowedTag,
    /**
     * Missing parent tag was added for current (i.e. tbody for tr).
     */
    RequiredParentMissing,
    /**
     * No matching close token was found for the open tag. Tag was closed automatically.
     * <p>
     * <b>Example:</b>
     * <p>
     * &lt;p>Some text..
     * <p>
     * Unclosed &lt;p> tag.
     */
    UnclosedTag,
    /**
     * Second instance of an unique tag was found, most likely it was removed.
     * <p>
     * <b>Example:</b>
     * <p>
     * 
     * <pre>
     * &lt;head>
     *    &lt;title>Some text&lt;/title>
     *    &lt;title>Some more text&lt;/title>
     * &lt;/head>
     * <p>
     * </pre>
     */
    UniqueTagDuplicated,
    /**
     * The tag was deprecated and current cleaner mode doesn't allows this. The tag was removed.
     * <p>
     * <b>Example:</b>
     * <ul>
     * <li>&lt;u>
     * <li>&lt;s>
     * <li>&lt;srtike>
     * <li>....
     * </ul>
     */
    Deprecated,
    /**
     * This tag have bad child that shouldn't be here. Thus the tag is closed automatically to avoid such inclusion.
     * <p>
     * <b>Example:</b>
     * <p>
     * &lt;p>Some text &lt;table>...&lt;/table>&lt;p>
     * <p>
     * &lt;table> is not allowed to be child of &lt;p>, thus &lt;p> is closed before the &lt;table>
     */
    UnpermittedChild,

    /**
     * The tag is unknown and current cleaner mode doesn't allows this. The tag was removed.
     * <p>
     * <b>Example:</b>
     * <ul>
     * <li>&lt;any>
     * <li>&lt;tag>
     * <li>....
     * </ul>
     */
    Unknown
}
