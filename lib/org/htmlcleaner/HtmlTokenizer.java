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
import java.util.*;

/**
 * Main HTML tokenizer.
 * <p>It's task is to parse HTML and produce list of valid tokens:
 * open tag tokens, end tag tokens, contents (text) and comments.
 * As soon as new item is added to token list, cleaner is invoked
 * to clean current list at the end.</p>
 *
 * Created by: Vladimir Nikic.<br>
 * Date: November, 2006

 */
public class HtmlTokenizer {
	
	private final static int WORKING_BUFFER_SIZE = 1024;

    private BufferedReader _reader;
    private char[] _working = new char[WORKING_BUFFER_SIZE];
    
    private transient int _pos = 0;
    private transient int _len = -1;

    private transient StringBuffer _saved = new StringBuffer(512);

    private transient boolean _isLateForDoctype = false;
    private transient DoctypeToken _docType = null;
    private transient TagToken _currentTagToken = null;
    private transient List _tokenList = new ArrayList();
    private transient Set _namespacePrefixes = new HashSet();
    
    private boolean _asExpected = true;

    private boolean _isScriptContext = false;

    private HtmlCleaner cleaner;
    private CleanerProperties props;
    private CleanerTransformations transformations;

    /**
     * Constructor - cretes instance of the parser with specified content.
     * @param cleaner
     * @throws IOException
     */
    public HtmlTokenizer(HtmlCleaner cleaner, Reader reader) throws IOException {
        this._reader = new BufferedReader(reader);
        this.cleaner = cleaner;
        this.props = cleaner.getProperties();
        this.transformations = cleaner.getTransformations();
    }

    private void addToken(BaseToken token) {
        _tokenList.add(token);
        cleaner.makeTree( _tokenList, _tokenList.listIterator(_tokenList.size() - 1) );
    }

    private void readIfNeeded(int neededChars) throws IOException {
        if (_len == -1 && _pos + neededChars >= WORKING_BUFFER_SIZE) {
            int numToCopy = WORKING_BUFFER_SIZE - _pos;
            System.arraycopy(_working, _pos, _working, 0, numToCopy);
    		_pos = 0;

            int expected = WORKING_BUFFER_SIZE - numToCopy;
            int size = 0;
            int charsRead = 0;
            int offset = numToCopy;
            do {
                charsRead = _reader.read(_working, offset, expected);
                if (charsRead >= 0) {
                    size += charsRead;
                    offset += charsRead;
                    expected -= charsRead;
                }
            } while (charsRead >= 0 && expected > 0);

            if (expected > 0) {
    			_len = size + numToCopy;
            }

            // convert invalid XML characters to spaces
            for (int i = 0; i < (_len >= 0 ? _len : WORKING_BUFFER_SIZE); i++) {
                int ch = _working[i];
                if (ch >= 1 && ch <= 32 && ch != 10 && ch != 13) {
                    _working[i] = ' ';
                }
            }
        }
    }

    List getTokenList() {
    	return this._tokenList;
    }

    Set getNamespacePrefixes() {
        return _namespacePrefixes;
    }

    private void go() throws IOException {
    	_pos++;
    	readIfNeeded(0);
    }

    private void go(int step) throws IOException {
    	_pos += step;
    	readIfNeeded(step - 1);
    }

    /**
     * Checks if content starts with specified value at the current position.
     * @param value
     * @return true if starts with specified value, false otherwise.
     * @throws IOException
     */
    private boolean startsWith(String value) throws IOException {
        int valueLen = value.length();
        readIfNeeded(valueLen);
        if (_len >= 0 && _pos + valueLen  > _len) {
            return false;
        }

        for (int i = 0; i < valueLen; i++) {
        	char ch1 = Character.toLowerCase( value.charAt(i) );
        	char ch2 = Character.toLowerCase( _working[_pos + i] );
        	if (ch1 != ch2) {
        		return false;
        	}
        }

        return true;
    }

    /**
     * Checks if character at specified position is whitespace.
     * @param position
     * @return true is whitespace, false otherwise.
     */
    private boolean isWhitespace(int position) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        return Character.isWhitespace( _working[position] );
    }

    /**
     * Checks if character at current runtime position is whitespace.
     * @return true is whitespace, false otherwise.
     */
    private boolean isWhitespace() {
        return isWhitespace(_pos);
    }

    /**
     * Checks if character at specified position is equal to specified char.
     * @param position
     * @param ch
     * @return true is equals, false otherwise.
     */
    private boolean isChar(int position, char ch) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        return Character.toLowerCase(ch) == Character.toLowerCase(_working[position]);
    }

    /**
     * Checks if character at current runtime position is equal to specified char.
     * @param ch
     * @return true is equal, false otherwise.
     */
    private boolean isChar(char ch) {
        return isChar(_pos, ch);
    }

    /**
     * Checks if character at specified position can be identifier start.
     * @param position
     * @return true is may be identifier start, false otherwise.
     */
    private boolean isIdentifierStartChar(int position) {
    	if (_len >= 0 && position >= _len) {
            return false;
        }

        char ch = _working[position];
        return Character.isUnicodeIdentifierStart(ch);
    }

    /**
     * Checks if character at current runtime position can be identifier start.
     * @return true is may be identifier start, false otherwise.
     */
    private boolean isIdentifierStartChar() {
        return isIdentifierStartChar(_pos);
    }

    /**
     * Checks if character at current runtime position can be identifier part.
     * @return true is may be identifier part, false otherwise.
     */
    private boolean isIdentifierChar() {
    	if (_len >= 0 && _pos >= _len) {
            return false;
        }

        char ch = _working[_pos];
        return Character.isUnicodeIdentifierStart(ch) || Character.isDigit(ch) || Utils.isIdentifierHelperChar(ch);
    }

    /**
     * Checks if end of the content is reached.
     */
    private boolean isAllRead() {
        return _len >= 0 && _pos >= _len;
    }

    /**
     * Saves specified character to the temporary buffer.
     * @param ch
     */
    private void save(char ch) {
        _saved.append(ch);
    }

    /**
     * Saves character at current runtime position to the temporary buffer.
     */
    private void saveCurrent() {
        if (!isAllRead()) {
            save( _working[_pos] );
        }
    }

    /**
     * Saves specified number of characters at current runtime position to the temporary buffer.
     * @throws IOException
     */
    private void saveCurrent(int size) throws IOException {
    	readIfNeeded(size);
        int pos = _pos;
        while ( !isAllRead() && (size > 0) ) {
            save( _working[pos] );
            pos++;
            size--;
        }
    }

    /**
     * Skips whitespaces at current position and moves foreward until
     * non-whitespace character is found or the end of content is reached.
     * @throws IOException
     */
    private void skipWhitespaces() throws IOException {
        while ( !isAllRead() && isWhitespace() ) {
            saveCurrent();
            go();
        }
    }

    private boolean addSavedAsContent() {
        if (_saved.length() > 0) {
            addToken( new ContentToken(_saved.toString()) );
            _saved.delete(0, _saved.length());
            return true;
        }

        return false;
    }

    /**
     * Starts parsing HTML.
     * @throws IOException
     */
    void start() throws IOException {
    	// initialize runtime values
        _currentTagToken = null;
        _tokenList.clear();
        _asExpected = true;
        _isScriptContext = false;
        _isLateForDoctype = false;
        _namespacePrefixes.clear();

        this._pos = WORKING_BUFFER_SIZE;
        readIfNeeded(0);

        boolean isScriptEmpty = true;

        while ( !isAllRead() ) {
            // resets all the runtime values
            _saved.delete(0, _saved.length());
            _currentTagToken = null;
            _asExpected = true;

            // this is enough for making decision
            readIfNeeded(10);

            if (_isScriptContext) {
                if ( startsWith("</script") && (isWhitespace(_pos + 8) || isChar(_pos + 8, '>')) ) {
                    tagEnd();
                } else if ( isScriptEmpty && startsWith("<!--") ) {
                    comment();
                } else {
                    boolean isTokenAdded = content();
                    if (isScriptEmpty && isTokenAdded) {
                        final BaseToken lastToken = (BaseToken) _tokenList.get(_tokenList.size() - 1);
                        if (lastToken != null) {
                            final String lastTokenAsString = lastToken.toString();
                            if (lastTokenAsString != null && lastTokenAsString.trim().length() > 0) {
                                isScriptEmpty = false;
                            }
                        }
                    }
                }
                if (!_isScriptContext) {
                    isScriptEmpty = true;
                }
            } else {
                if ( startsWith("<!doctype") ) {
                	if ( !_isLateForDoctype ) {
                		doctype();
                		_isLateForDoctype = true;
                	} else {
                		ignoreUntil('<');
                	}
                } else if ( startsWith("</") && isIdentifierStartChar(_pos + 2) ) {
                	_isLateForDoctype = true;
                    tagEnd();
                } else if ( startsWith("<!--") ) {
                    comment();
                } else if ( startsWith("<") && isIdentifierStartChar(_pos + 1) ) {
                	_isLateForDoctype = true;
                    tagStart();
                } else if ( props.isIgnoreQuestAndExclam() && (startsWith("<!") || startsWith("<?")) ) {
                    ignoreUntil('>');
                    if (isChar('>')) {
                        go();
                    }
                } else {
                    content();
                }
            }
        }

        _reader.close();
    }

    /**
     * Checks if specified tag name is one of the reserved tags: HTML, HEAD or BODY
     * @param tagName
     * @return
     */
    private boolean isReservedTag(String tagName) {
        return "html".equalsIgnoreCase(tagName) || "head".equalsIgnoreCase(tagName) || "body".equalsIgnoreCase(tagName);
    }

    /**
     * Parses start of the tag.
     * It expects that current position is at the "<" after which
     * the tag's name follows.
     * @throws IOException
     */
    private void tagStart() throws IOException {
        saveCurrent();
        go();

        if ( isAllRead() ) {
            return;
        }

        String tagName = identifier();

        TagTransformation tagTransformation = null;
        if (transformations != null && transformations.hasTransformationForTag(tagName)) {
            tagTransformation = transformations.getTransformation(tagName);
            if (tagTransformation != null) {
                tagName = tagTransformation.getDestTag();
            }
        }

        if (tagName != null) {
            ITagInfoProvider tagInfoProvider = cleaner.getTagInfoProvider();
            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
            if ( (tagInfo == null && !props.isOmitUnknownTags() && props.isTreatUnknownTagsAsContent() && !isReservedTag(tagName)) ||
                 (tagInfo != null && tagInfo.isDeprecated() && !props.isOmitDeprecatedTags() && props.isTreatDeprecatedTagsAsContent()) ) {
                content();
                return;
            }
        }

        TagNode tagNode = new TagNode(tagName, cleaner);
        _currentTagToken = tagNode;

        if (_asExpected) {
            skipWhitespaces();
            tagAttributes();

            if (tagName != null) {
                if (tagTransformation != null) {
                    tagNode.transformAttributes(tagTransformation);
                }
                addToken(_currentTagToken);
            }
            
            if ( isChar('>') ) {
            	go();
                if ( "script".equalsIgnoreCase(tagName) ) {
                    _isScriptContext = true;
                }
            } else if ( startsWith("/>") ) {
            	go(2);
            }

            _currentTagToken = null;
        } else {
        	addSavedAsContent();
        }
    }


    /**
     * Parses end of the tag.
     * It expects that current position is at the "<" after which
     * "/" and the tag's name follows.
     * @throws IOException
     */
    private void tagEnd() throws IOException {
        saveCurrent(2);
        go(2);

        if ( isAllRead() ) {
            return;
        }

        String tagName = identifier();
        if (transformations != null && transformations.hasTransformationForTag(tagName)) {
            TagTransformation tagTransformation = transformations.getTransformation(tagName);
            if (tagTransformation != null) {
                tagName = tagTransformation.getDestTag();
            }
        }

        if (tagName != null) {
            ITagInfoProvider tagInfoProvider = cleaner.getTagInfoProvider();
            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
            if ( (tagInfo == null && !props.isOmitUnknownTags() && props.isTreatUnknownTagsAsContent() && !isReservedTag(tagName)) ||
                 (tagInfo != null && tagInfo.isDeprecated() && !props.isOmitDeprecatedTags() && props.isTreatDeprecatedTagsAsContent()) ) {
                content();
                return;
            }
        }

        _currentTagToken = new EndTagToken(tagName);

        if (_asExpected) {
            skipWhitespaces();
            tagAttributes();

            if (tagName != null) {
                addToken(_currentTagToken);
            }

            if ( isChar('>') ) {
            	go();
            }

            if ( "script".equalsIgnoreCase(tagName) ) {
                _isScriptContext = false;
            }

            _currentTagToken = null;
        } else {
            addSavedAsContent();
        }
    }

    /**
     * Parses an identifier from the current position.
     * @throws IOException
     */
    private String identifier() throws IOException {
        _asExpected = true;

        if ( !isIdentifierStartChar() ) {
            _asExpected = false;
            return null;
        }

        StringBuffer identifierValue = new StringBuffer();

        while ( !isAllRead() && isIdentifierChar() ) {
            saveCurrent();
            identifierValue.append( _working[_pos] );
            go();
        }

        // strip invalid characters from the end
        while ( identifierValue.length() > 0 && Utils.isIdentifierHelperChar(identifierValue.charAt(identifierValue.length() - 1)) ) {
            identifierValue.deleteCharAt( identifierValue.length() - 1 );
        }

        if ( identifierValue.length() == 0 ) {
            return null;
        }

        String id = identifierValue.toString();

        int columnIndex = id.indexOf(':');
        if (columnIndex >= 0) {
            String prefix = id.substring(0, columnIndex);
            String suffix = id.substring(columnIndex + 1);
            int nextColumnIndex = suffix.indexOf(':');
            if (nextColumnIndex >= 0) {
                suffix = suffix.substring(0, nextColumnIndex);
            }
            if (props.isNamespacesAware()) {
                id = prefix + ":" + suffix;
                if ( !"xmlns".equalsIgnoreCase(prefix) ) {
                    _namespacePrefixes.add( prefix.toLowerCase() );
                }
            } else {
                id = suffix;
            }
        }

        return id;
    }

    /**
     * Parses list tag attributes from the current position.
     * @throws IOException
     */
    private void tagAttributes() throws IOException {
        while( !isAllRead() && _asExpected && !isChar('>') && !startsWith("/>") ) {
            skipWhitespaces();
            String attName = identifier();

            if (!_asExpected) {
                if ( !isChar('<') && !isChar('>') && !startsWith("/>") ) {
                    saveCurrent();
                    go();
                }

                if (!isChar('<')) {
                    _asExpected = true;
                }

                continue;
            }

            String attValue;

            skipWhitespaces();
            if ( isChar('=') ) {
                saveCurrent();
                go();
                attValue = attributeValue();
            } else if (CleanerProperties.BOOL_ATT_EMPTY.equals(props.booleanAttributeValues)) {
                attValue = "";
            } else if (CleanerProperties.BOOL_ATT_TRUE.equals(props.booleanAttributeValues)) {
                attValue = "true";
            } else {
                attValue = attName;
            }

            if (_asExpected) {
                _currentTagToken.addAttribute(attName, attValue);
            }
        }
    }

    /**
     * Parses a single tag attribute - it is expected to be in one of the forms:
     * 		name=value
     * 		name="value"
     * 		name='value'
     * 		name
     * @throws IOException
     */
    private String attributeValue() throws IOException {
        skipWhitespaces();
        
        if ( isChar('<') || isChar('>') || startsWith("/>") ) {
        	return "";
        }

        boolean isQuoteMode = false;
        boolean isAposMode = false;

        StringBuffer result = new StringBuffer();

        if ( isChar('\'') ) {
            isAposMode = true;
            saveCurrent();
            go();
        } else if ( isChar('\"') ) {
            isQuoteMode = true;
            saveCurrent();
            go();
        }

        boolean isMultiWord = props.isAllowMultiWordAttributes();

        boolean allowHtml = props.isAllowHtmlInsideAttributes();

        while ( !isAllRead() &&
                ( (isAposMode && !isChar('\'') && (allowHtml || !isChar('>') && !isChar('<')) && (isMultiWord || !isWhitespace())) ||
                  (isQuoteMode && !isChar('\"') && (allowHtml || !isChar('>') && !isChar('<')) && (isMultiWord || !isWhitespace())) ||
                  (!isAposMode && !isQuoteMode && !isWhitespace() && !isChar('>') && !isChar('<'))
                )
              ) {
            result.append( _working[_pos] );
            saveCurrent();
            go();
        }

        if ( isChar('\'') && isAposMode ) {
            saveCurrent();
            go();
        } else if ( isChar('\"') && isQuoteMode ) {
            saveCurrent();
            go();
        }


        return result.toString();
    }

    private boolean content() throws IOException {
        while ( !isAllRead() ) {
            saveCurrent();
            go();

            if ( isChar('<') ) {
                break;
            }
        }

        return addSavedAsContent();
    }

    private void ignoreUntil(char ch) throws IOException {
        while ( !isAllRead() ) {
        	go();
            if ( isChar(ch) ) {
                break;
            }
        }
    }

    private void comment() throws IOException {
    	go(4);
        while ( !isAllRead() && !startsWith("-->") ) {
            saveCurrent();
            go();
        }

        if (startsWith("-->")) {
        	go(3);
        }

        if (_saved.length() > 0) {
            if ( !props.isOmitComments() ) {
                String hyphenRepl = props.getHyphenReplacementInComment();
                String comment = _saved.toString().replaceAll("--", hyphenRepl + hyphenRepl);

        		if ( comment.length() > 0 && comment.charAt(0) == '-' ) {
        			comment = hyphenRepl + comment.substring(1);
        		}
        		int len = comment.length();
        		if ( len > 0 && comment.charAt(len - 1) == '-' ) {
        			comment = comment.substring(0, len - 1) + hyphenRepl;
        		}

        		addToken( new CommentToken(comment) );
        	}
            _saved.delete(0, _saved.length());
        }
    }
    
    private void doctype() throws IOException {
    	go(9);

    	skipWhitespaces();
    	String part1 = identifier();
	    skipWhitespaces();
	    String part2 = identifier();
	    skipWhitespaces();
	    String part3 = attributeValue();
	    skipWhitespaces();
	    String part4 = attributeValue();
	    
	    ignoreUntil('<');
	    
	    _docType = new DoctypeToken(part1, part2, part3, part4);
    }

    public DoctypeToken getDocType() {
        return _docType;
    }
    
}