package com.ibm.icu.dev.test.util;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

public abstract class UnicodeLabel {
    public abstract String getValue(int codepoint, boolean isShort);
    public String getValue(String s, String separator, boolean withCodePoint) {
        if (s.length() == 1) { // optimize simple case
            return getValue(s.charAt(0), withCodePoint); 
        }
        StringBuffer sb = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i+=UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s,i);
            if (i != 0) sb.append(separator);
            sb.append(getValue(cp, withCodePoint));
        }
        return sb.toString();
    }
    public int getMaxWidth(boolean isShort) {
        return 0;
    }
    private static class Hex extends UnicodeLabel {
        public String getValue(int codepoint, boolean isShort) {
            if (isShort) return Utility.hex(codepoint,4);
            return "U+" + Utility.hex(codepoint,4);
        }       
    }
    private static class Null extends UnicodeLabel {
        public String getValue(int codepoint, boolean isShort) {
            return "";
        }       
    }
    public static final UnicodeLabel NULL = new Null();
    public static final UnicodeLabel HEX = new Hex();
}