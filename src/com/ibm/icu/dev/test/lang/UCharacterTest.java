/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/UCharacterTest.java,v $ 
* $Date: 2000/12/26 20:01:08 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu.test.text;

import java.io.BufferedReader;
import java.io.FileReader;
import com.ibm.test.TestFmwk;
import com.ibm.icu.text.UCharacter;
import com.ibm.icu.text.UCharacterCategoryEnum;
import com.ibm.icu.text.UCharacterDirectionEnum;
import com.ibm.icu.text.UTF16;

/**
* Testing class for UCharacter
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since nov 04 2000
*/
public final class UCharacterTest extends TestFmwk
{ 
  // private variables =============================================
  
  /**
  * ICU4J data version number
  */
  private final String VERSION_ = "3.0.0.0";
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public UCharacterTest()
  {
  }
  
  // public methods ================================================
  
  /**
  * Testing the uppercase and lowercase function of UCharacter
  */
  public void TestUpperLower()
  {
    // variables to test the uppercase and lowercase characters
    int upper[] = {0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0xb1, 0xb2, 
                   0xb3, 0x48, 0x49, 0x4a, 0x2e, 0x3f, 0x3a, 0x4b, 0x4c,
                   0x4d, 0x4e, 0x4f, 0x01c4, 0x01c8, 0x000c, 0x0000};
    int lower[] = {0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0xb1, 0x00b2, 
                   0xb3, 0x68, 0x69, 0x6a, 0x2e, 0x3f, 0x3a, 0x6b, 0x6c,
                   0x6d, 0x6e, 0x6f, 0x01c6, 0x01c9, 0x000c, 0x0000};
    
    int size = upper.length;
    
    for (int i = 0; i < size; i ++) 
    {
      if (UCharacter.isLetter(lower[i]) && !UCharacter.isLowerCase(lower[i]))
      {
        errln("FAIL isLowerCase test for 0x" + 
              Integer.toHexString(lower[i]));
        break;
      }
      if (UCharacter.isLetter(upper[i]) && !(UCharacter.isUpperCase(upper[i]) 
                                       || UCharacter.isTitleCase(upper[i])))     
      {
        errln("FAIL isUpperCase test for 0x" + 
              Integer.toHexString(upper[i]));
        break;
      }
      if (lower[i] != UCharacter.toLowerCase(upper[i]) || 
          (upper[i] != UCharacter.toUpperCase(lower[i]) &&
            upper[i] != UCharacter.toTitleCase(lower[i])))
      {
        errln("FAIL case conversion test for 0x" + 
              Integer.toHexString(upper[i]) + " to 0x" + 
              Integer.toHexString(lower[i]));
        break;
      }
      if (lower[i] != UCharacter.toLowerCase(lower[i]))
      {
        errln("FAIL lower case conversion test for 0x" + 
        Integer.toHexString(lower[i]));
        break;
      }
      if (upper[i] != UCharacter.toUpperCase(upper[i]) && 
          upper[i] != UCharacter.toTitleCase(upper[i]))
      {
        errln("FAIL upper case conversion test for 0x" + 
              Integer.toHexString(upper[i]));
        break;
      }
      logln("Ok    0x" + Integer.toHexString(upper[i]) + " and 0x" +
            Integer.toHexString(lower[i]));
    }
  }
  
  /**
  * Testing the letter and number determination in UCharacter
  */
  public void TestLetterNumber()
  {
    for (int i = 0x0041; i < 0x005B; i ++) 
      if (!UCharacter.isLetter(i))
        errln("FAIL 0x" + Integer.toHexString(i) + " expected to be a letter");
        
    for (int i = 0x0660; i < 0x066A; i ++) 
      if (UCharacter.isLetter(i))
        errln("FAIL 0x" + Integer.toHexString(i) + 
              " expected not to be a letter");
    
    for (int i = 0x0660; i < 0x066A; i ++) 
      if (!UCharacter.isDigit(i))
        errln("FAIL 0x" + Integer.toHexString(i) + " expected to be a digit");
    
    for (int i = 0x0041; i < 0x005B; i ++) 
      if (!UCharacter.isLetterOrDigit(i))
        errln("FAIL 0x" + Integer.toHexString(i) + 
              " expected not to be a digit");
        
    for (int i = 0x0660; i < 0x066A; i ++) 
      if (!UCharacter.isLetterOrDigit(i))
        errln("FAIL 0x" + Integer.toHexString(i) + 
              "expected to be either a letter or a digit");
  }

  /**
  * Tests for space determination in UCharacter
  */
  public void TestSpaces()
  {
    int spaces[] = {0x0020, 0x00a0, 0x2000, 0x2001, 0x2005};
    int nonspaces[] = {0x61, 0x62, 0x63, 0x64, 0x74};
    int whitespaces[] = {0x2008, 0x2009, 0x200a, 0x001c, 0x000c};
    int nonwhitespaces[] = {0x61, 0x62, 0x3c, 0x28, 0x3f};
                       
    int size = spaces.length;
    for (int i = 0; i < size; i ++)
    {
      if (!UCharacter.isSpaceChar(spaces[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(spaces[i]) + 
              " expected to be a space character");
        break;
      }
      
      if (UCharacter.isSpaceChar(nonspaces[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonspaces[i]) + 
              " expected not to be space character");
        break;
      }
 
      if (!UCharacter.isWhitespace(whitespaces[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(whitespaces[i]) + 
              " expected to be a white space character");
        break;
      }
      if (UCharacter.isWhitespace(nonwhitespaces[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonwhitespaces[i]) + 
              " expected not to be a space character");
        break;
      }
      logln("Ok    0x" + Integer.toHexString(spaces[i]) + " and 0x" +
            Integer.toHexString(nonspaces[i]) + " and 0x" +
            Integer.toHexString(whitespaces[i]) + " and 0x" +
            Integer.toHexString(nonwhitespaces[i]));
    }
  }
  
  /**
  * Tests for defined and undefined characters
  */
  public void TestDefined()
  {
    int undefined[] = {0xfff1, 0xfff7, 0xfa30};
    int defined[] = {0x523E, 0x4f88, 0xfffd};
    
    int size = undefined.length;
    for (int i = 0; i < size; i ++) 
    {
      if (UCharacter.isDefined(undefined[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(undefined[i]) + 
              " expected not to be defined");
        break;
      }
      if (!UCharacter.isDefined(defined[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(defined[i]) + 
              " expected defined");
        break;
      }
    }
  }
  
  /**
  * Tests for base characters and their cellwidth
  */
  public void TestBase()
  {
    int base[] = {0x0061, 0x0031, 0x03d2};
    int nonbase[] = {0x002B, 0x0020, 0x203B};    
    int size = base.length;
    for (int i = 0; i < size; i ++) 
    {
      if (UCharacter.isBaseForm(nonbase[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonbase[i]) + 
              " expected not to be a base character");
        break;
      }
      if (!UCharacter.isBaseForm(base[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(base[i]) + 
              " expected to be a base character");
        break;
      }
    }
  }
    
  /**
  * Tests for digit characters 
  */
  public void TestDigits()
  {
    int digits[] = {0x0030, 0x0662, 0x0F23, 0x0ED5, 0x2160};
    
    //special characters not in the properties table
    int digits2[] = {0x3007, 0x4e00, 0x4e8c, 0x4e09, 0x56d8, 0x4e94, 0x516d, 
                     0x4e03, 0x516b, 0x4e5d}; 
    int nondigits[] = {0x0010, 0x0041, 0x0122, 0x68FE};
    
    int digitvalues[] = {0, 2, 3, 5, 1};
    int digitvalues2[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};     
    
    int size  = digits.length;
    for (int i = 0; i < size; i ++) 
      if (UCharacter.isDigit(digits[i]) && 
          UCharacter.digit(digits[i]) != digitvalues[i]) 
      {
        errln("FAIL 0x" + Integer.toHexString(digits[i]) + 
              " expected digit with value " + digitvalues[i]);
        break;
      }
    
    size = nondigits.length;
    for (int i = 0; i < size; i ++)
      if (UCharacter.isDigit(nondigits[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nondigits[i]) + 
              " expected nondigit");
        break;
      }
      
    size = digits2.length;
    for (int i = 0; i < 10; i ++) 
      if (UCharacter.isDigit(digits2[i]) &&
          UCharacter.digit(digits2[i]) != digitvalues2[i]) 
      {
        errln("FAIL 0x" + Integer.toHexString(digits2[i]) + 
              " expected digit with value " + digitvalues2[i]);
        break;
      }
  }

  /**
  * Tests for version 
  */
  public void TestVersion()
  {
    String version = UCharacter.getUnicodeVersion();
    if (!version.equals(VERSION_))
      errln("FAIL expected " + VERSION_);
  }
  
  /**
  * Tests for control characters
  */
  public void TestControl()
  {
    int control[] = {0x001b, 0x0097, 0x0082};
    int noncontrol[] = {0x61, 0x0031, 0x00e2};
    
    int size = control.length;
    for (int i = 0; i < size; i ++) 
    {
      if (!UCharacter.isControl(control[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(control[i]) + 
              " expected to be a control character");
        break;
      }  
      if (UCharacter.isControl(noncontrol[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(noncontrol[i]) + 
              " expected to be not a control character");
        break;
      }
      
      logln("Ok    0x" + Integer.toHexString(control[i]) + " and 0x" +
            Integer.toHexString(noncontrol[i]));
    }
  }
  
  /**
  * Tests for printable characters
  */
  public void TestPrint()
  {
    int printable[] = {0x0042, 0x005f, 0x2014};
    int nonprintable[] = {0x200c, 0x009f, 0x001b};
    
    int size = printable.length;
    for (int i = 0; i < size; i ++)
    {
      if (!UCharacter.isPrintable(printable[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(printable[i]) + 
              " expected to be a printable character");
        break;
      }
      if (UCharacter.isPrintable(nonprintable[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonprintable[i]) +
              " expected not to be a printable character");
        break;
      }
      logln("Ok    0x" + Integer.toHexString(printable[i]) + " and 0x" +
            Integer.toHexString(nonprintable[i]));
    }
  }
  
  /** 
  * Testing for identifier characters
  */
  public void TestIdentifier()
  {
    int unicodeidstart[] = {0x0250, 0x00e2, 0x0061};
    int nonunicodeidstart[] = {0x2000, 0x000a, 0x2019};
    int unicodeidpart[] = {0x005f, 0x0032, 0x0045};
    int nonunicodeidpart[] = {0x2030, 0x00a3, 0x0020};
    int idignore[] = {0x070F, 0x180B, 0x180C};
    int nonidignore[] = {0x0075, 0x00a3, 0x0061};

    int size = unicodeidstart.length;
    for (int i = 0; i < size; i ++) 
    {
      if (!UCharacter.isUnicodeIdentifierStart(unicodeidstart[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(unicodeidstart[i]) + 
              " expected to be a unicode identifier start character");
        break;
      }
      if (UCharacter.isUnicodeIdentifierStart(nonunicodeidstart[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonunicodeidstart[i]) + 
              " expected not to be a unicode identifier start character");
        break;
      }
      if (!UCharacter.isUnicodeIdentifierPart(unicodeidpart[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(unicodeidpart[i]) + 
              " expected to be a unicode identifier part character");
        break;
      }
      if (UCharacter.isUnicodeIdentifierPart(nonunicodeidpart[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonunicodeidpart[i]) + 
              " expected not to be a unicode identifier part character");
        break;
      }
      if (!UCharacter.isIdentifierIgnorable(idignore[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(idignore[i]) + 
              " expected to be a ignorable unicode character");
        break;
      }
      if (UCharacter.isIdentifierIgnorable(nonidignore[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(nonidignore[i]) + 
              " expected not to be a ignorable unicode character");
        break;
      }
      logln("Ok    0x" + Integer.toHexString(unicodeidstart[i]) + " and 0x" +
            Integer.toHexString(nonunicodeidstart[i]) + " and 0x" +
            Integer.toHexString(unicodeidpart[i]) + " and 0x" +
            Integer.toHexString(nonunicodeidpart[i]) + " and 0x" +
            Integer.toHexString(idignore[i]) + " and 0x" +
            Integer.toHexString(nonidignore[i]));
    }
  }
  
  /**
  * Tests for the character types, direction
  */
  public void TestCatDir()
  {
    // this is the 2 char category types used in the UnicodeData file
    final String TYPE = 
      "LuLlLtLmLoMnMeMcNdNlNoZsZlZpCcCfCoCsPdPsPePcPoSmScSkSoPiPf";
    
    // directory types used in the UnicodeData file
    // padded by spaces to make each type size 4
    final String DIR = 
      "L   R   EN  ES  ET  AN  CS  B   S   WS  ON  LRE LRO AL  RLE RLO PDF NSM BN  ";
      
    StringBuffer file = new StringBuffer("UnicodeData-");
    file.append(UCharacter.getUnicodeVersion());
    file.append(".txt");
    String s;
    
    final int LASTUNICODECHAR = 0xFFFD;
    int ch = 0,
        index = 0,
        type = 0,
        dir = 0;
	
	  try
	  {
	    // reading in the UnicodeData file
	    FileReader fr = new FileReader(file.toString());
	    BufferedReader input = new BufferedReader(fr);
	    
      while (ch != LASTUNICODECHAR)
      {
        s= input.readLine();
        
        // geting the unicode character, its type and its direction
        ch = Integer.parseInt(s.substring(0, 4), 16);
        index = s.indexOf(';', 5);
        String t = s.substring(index + 1, index + 3);
        index = s.indexOf(';', index + 4);
        String d = s.substring(index + 1, s.indexOf(';', index + 1));
        
        // testing the category
        // we override the general category of some control characters
        if (ch == 9 || ch == 0xb || ch == 0x1f)
          type = UCharacterCategoryEnum.SPACE_SEPARATOR;
        else
          if (ch == 0xc)
            type = UCharacterCategoryEnum.LINE_SEPARATOR;
          else
            if (ch == 0xa || ch == 0xd || ch == 0x1c || ch == 0x1d || 
                ch == 0x1e || ch == 0x85)
               type = UCharacterCategoryEnum.PARAGRAPH_SEPARATOR;
            else
            {
              type = TYPE.indexOf(t);
              if (type < 0)
                type = 0;
              else 
                type = (type >> 1) + 1;  
            }
            
        if (UCharacter.getType(ch) != type)
        {
          errln("FAIL 0x" + Integer.toHexString(ch) + " expected type " + 
                type);
          break;
        }
        
        // testing the direction
        if (d.length() == 1)
          d = d + "   ";  
          
        dir = DIR.indexOf(d) >> 2;
        if (UCharacter.getDirection(ch) != dir) 
        {
          errln("FAIL 0x" + Integer.toHexString(ch) + 
                " expected wrong direction " + dir);
          break;
        }
      }
      input.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    if (UCharacter.getDirection(0x10001) != 
                                         UCharacterDirectionEnum.LEFT_TO_RIGHT) 
      errln("FAIL 0x10001 expected direction " + 
      UCharacterDirectionEnum.toString(UCharacterDirectionEnum.LEFT_TO_RIGHT));
  }
  
  /**
  * Test for the character names
  */
  public void TestNames()
  {
    int c[] = {0x0061, 0x0284, 0x3401, 0x7fed, 0xac00, 0xd7a3, 0xff08, 0xffe5};
    String name[] = {"LATIN SMALL LETTER A", 
                     "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK", 
                     "CJK UNIFIED IDEOGRAPH-3401", 
                     "CJK UNIFIED IDEOGRAPH-7FED", "HANGUL SYLLABLE GA", 
                     "HANGUL SYLLABLE HIH", "FULLWIDTH LEFT PARENTHESIS",
                     "FULLWIDTH YEN SIGN"};
    String oldname[] = {"", "LATIN SMALL LETTER DOTLESS J BAR HOOK", "", "",
                        "", "", "FULLWIDTH OPENING PARENTHESIS", ""};
    int size = c.length;
    String str;
    int uc;
    
    for (int i = 0; i < size; i ++) 
    {
      // modern Unicode character name
      str = UCharacter.getName(c[i]);
      if (!str.equalsIgnoreCase(name[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(c[i]) + " expected name " +
              name[i]);
        break;
      }
     
      // 1.0 Unicode character name
      str = UCharacter.getName1_0(c[i]);
      if ((str == null && oldname[i].length() > 0) || 
          (str != null && !str.equalsIgnoreCase(oldname[i])))
      {
        errln("FAIL 0x" + Integer.toHexString(c[i]) + " expected 1.0 name " +
              oldname[i]);
        break;
      }
      
      // retrieving unicode character from modern name
      uc = UCharacter.getCharFromName(name[i]);
      if (uc != c[i])
      {
        errln("FAIL " + name[i] + " expected character 0x" + 
              Integer.toHexString(c[i]));
        break;
      }
      
      //retrieving unicode character from 1.0 name
      uc = UCharacter.getCharFromName1_0(oldname[i]);
      if (uc != c[i] && i != 0 && (i == 1 || i == 6))
      {
        errln("FAIL " + name[i] + " expected 1.0 character " + 
              Integer.toHexString(c[i]));
        break;
      }
    }
    
    // extra testing different from icu
    for (int i = UCharacter.MIN_VALUE; i < UCharacter.MAX_VALUE; i ++)
    {
      str = UCharacter.getName(i);
      if (str != null && UCharacter.getCharFromName(str) != i)
      {
        errln("FAIL 0x" + Integer.toHexString(i) + " " + str  + 
              " retrieval of name and vice versa" );
        break;
      }
    }
  }
  
  /**
  * Testing UTF16 class methods append, getCharCount and bounds
  */
  public void TestUTF16AppendBoundCount()
  {
    StringBuffer str = new StringBuffer("this is a string ");
    int length;
    
    for (int i = UCharacter.MIN_VALUE; i < UCharacter.MAX_VALUE; i ++)
    {
      length = str.length();
      UTF16.append(str, i);
      if (!UCharacter.isSupplementary(i))
      {
        if (UTF16.getCharCount(i) != 1)
        {
          errln("FAIL Counting BMP character size error" );
          break;
        }  
        if (str.length() != length + 1)
        {
          errln("FAIL Adding a BMP character error" );
          break;
        }
        if (!UTF16.isSurrogate((char)i) && 
            UTF16.bounds(str.toString(), str.length() - 1) != 
                                                    UTF16.SINGLE_CHAR_BOUNDARY) 
        {
          errln("FAIL Finding BMP character bounds error" );
          break;
        }
      }
      else 
      {
        if (UTF16.getCharCount(i) != 2)
        {
          errln("FAIL Counting Supplementary character size error" );
          break;
        }
        if (str.length() != length + 2)
        {
          errln("FAIL Adding a Supplementary character error" );
          break;
        }
        length = str.length();
        if (UTF16.bounds(str.toString(), str.length() - 2) != 
            UTF16.LEAD_SURROGATE_BOUNDARY || 
            UTF16.bounds(str.toString(), str.length() - 1) != 
            UTF16.TRAIL_SURROGATE_BOUNDARY)
        {
          errln("FAIL Finding Supplementary character bounds error" );
          break;
        }
      }
    } 
  }
  
  /**
  * Testing UTF16 class methods findCPOffset, findOffsetFromCP and charAt
  */
  public void TestUTF16OffsetCharAt()
  {
    StringBuffer str = new StringBuffer("12345");
    UTF16.append(str, 0x10001);
    str.append("67890");
    UTF16.append(str, 0x10002);
    String s = str.toString();
    if (UTF16.charAt(s, 0) != '1' || UTF16.charAt(s, 2) != '3' || 
        UTF16.charAt(s, 5) != 0x10001 || UTF16.charAt(s, 6) != 0x10001 || 
        UTF16.charAt(s, 12) != 0x10002 || UTF16.charAt(s, 13) != 0x10002)
      errln("FAIL Getting character from string error" );

    if (UTF16.findCPOffset(s, 3) != 3 || UTF16.findCPOffset(s, 5) != 5 || 
        UTF16.findCPOffset(s, 6) != 6)
      errln("FAIL Getting codepoint offset from string error" );
    if (UTF16.findOffsetFromCP(s, 3) != 3 || 
        UTF16.findOffsetFromCP(s, 5) != 5 || 
        UTF16.findOffsetFromCP(s, 6) != 7)
      errln("FAIL Getting UTF16 offset from codepoint in string error" );
  }
 
  public static void main(String[] arg)
  {
    try
    {
      UCharacterTest test = new UCharacterTest();
      test.run(arg);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

