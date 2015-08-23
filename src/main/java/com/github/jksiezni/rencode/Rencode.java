/**
 * 
 */
package com.github.jksiezni.rencode;

import java.nio.charset.Charset;

/**
 * Rencode type codes based on Conelly Barnes source code. 
 * 
 * @see {@linkplain http://barnesc.blogspot.com/2006/01/}
 * @author Jakub Księżniak
 *
 */
final class Rencode {
	
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	// Default number of bits for serialized floats, either 32 or 64 (also a parameter for dumps()).
	public static final int LENGTH_DELIMITER = ':';

	// Maximum length of integer when written as base 10 string.
	public static final int MAX_INT_LENGTH = 64;
	
	// The bencode 'typecodes' such as i, d, etc have been extended and
	// relocated on the base-256 character set.
	public static final int CHR_LIST 		= 59;
	public static final int CHR_DICT    = 60;
	public static final int CHR_INT     = 61;
	public static final int CHR_INT1    = 62;
	public static final int CHR_INT2    = 63;
	public static final int CHR_INT4    = 64;
	public static final int CHR_INT8    = 65;
	public static final int CHR_FLOAT32 = 66;
	public static final int CHR_FLOAT64 = 44;
	public static final int CHR_TRUE    = 67;
	public static final int CHR_FALSE   = 68;
	public static final int CHR_NONE    = 69;
	public static final int CHR_TERM    = 127;
	
	// Positive integers with value embedded in typecode.
	public static final int INT_POS_FIXED_START = 0;
	public static final int INT_POS_FIXED_COUNT = 44;

	// Dictionaries with length embedded in typecode.
	public static final int DICT_FIXED_START = 102;
	public static final int DICT_FIXED_COUNT = 25;

	// Negative integers with value embedded in typecode.
	public static final int INT_NEG_FIXED_START = 70;
	public static final int INT_NEG_FIXED_COUNT = 32;

	// Strings with length embedded in typecode.
	public static final int STR_FIXED_START = 128;
	public static final int STR_FIXED_COUNT = 64;

	// Lists with length embedded in typecode.
	public static final int LIST_FIXED_START = STR_FIXED_START + STR_FIXED_COUNT;
	public static final int LIST_FIXED_COUNT = 64;
	
}
