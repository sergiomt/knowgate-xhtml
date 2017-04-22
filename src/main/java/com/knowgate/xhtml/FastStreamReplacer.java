package com.knowgate.xhtml;

/**
 * © Copyright 2016 the original author.
 * This file is licensed under the Apache License version 2.0.
 * You may not use this file except in compliance with the license.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.HashMap;

import java.util.Date;

/**
 * Search and Replace a set of substrings with another substrings.
 * <p>This class is a single-pass fast no wildcards replacer for a given set of substrings.</p>
 * <p>It is primarily designed for mail merge document personalization routines, where a small
 * number of substrings have to be replaced at a master document with data retrieved from a list
 * or database.</p>
 * @author Sergio Montoro Ten
 * @version 2.1
 */

public class FastStreamReplacer {
  int BufferSize;
  int iReplacements;
  StringBuffer oOutStream;

  // ----------------------------------------------------------

  public FastStreamReplacer() {
    BufferSize = 32767;
    oOutStream = new StringBuffer(BufferSize);
  }

  // ----------------------------------------------------------

  public FastStreamReplacer(int iBufferSize) {
    BufferSize = iBufferSize;
    oOutStream = new StringBuffer(BufferSize);
  }

  // ----------------------------------------------------------
  /**
   * Replace substrings from a Stream.
   * @param oFileInStream Input Stream containing substrings to be replaced.
   * @param oMap Map with values to be replaced.<br>
   * Each map key will be replaced by its value.<br>
   * Map keys must appear in stream text as {#<i>key</i>}<br>
   * For example: InputStream "Today is {#System.Date}" will be replaced with "Today is 2002-02-21 11:32:44"<br>
   * No wildcards are accepted.<br>
   * Map keys must not contain {#&nbsp;} key markers.
   * @return String Replacements Result
   * @throws IOException
   */

  public String replace(InputStream oFileInStream, HashMap oMap) throws IOException {

    int iChr;
    String sKey;
    Object oValue;

    Date dtToday = new Date();
    String sToday = String.valueOf(dtToday.getYear()+1900) + "-" + String.valueOf(dtToday.getMonth()+1) + "-" + String.valueOf(dtToday.getDate());

    oMap.put("Sistema.Fecha",sToday);
    oMap.put("System.Date",sToday);

    iReplacements = 0;

    BufferedInputStream oInStream = new BufferedInputStream(oFileInStream, BufferSize);

    oOutStream.setLength(0);

    do {
      iChr = oInStream.read();

      if (-1==iChr)
        break;

      else {

        if (123 == iChr) {
          // Se encontro el caracter '{'
          iChr = oInStream.read();
          if (35 == iChr) {
            // Se encontro el caracter '#'

            iReplacements++;

            sKey = "";

            do {

              iChr = oInStream.read();
              if (-1==iChr || 125==iChr)
                break;
              sKey += (char) iChr;

            } while (true);

            if (oMap.containsKey(sKey))
              oValue = oMap.get(sKey);
            else
            	oValue = "{#"+sKey+"}";

            if (null!=oValue)
              oOutStream.append(((String)oValue));
          } // fi ('#')

          else {
            oOutStream.append((char)123);
            oOutStream.append((char)iChr);
          }

        } // fi ('{')

        else
          oOutStream.append((char)iChr);
      } // fi (!eof)

    } while (true);

    oInStream.close();

    return oOutStream.toString();
  } // replace()

  // ----------------------------------------------------------
  /**
   * Replace subtrings from a StringBuffer.
   * @param oStrBuff StringBuffer containing substrings to be replaced.
   * @param oMap Map with values to be replaced.<br>
   * Each map key will be replaced by its value.<br>
   * Map keys must appear in stream text as {#<i>key</i>}<br>
   * For example: InputStream "Today is {#System.Date}" will be replaced with "Today is 2002-02-21 11:32:44"<br>
   * No wildcards are accepted.<br>
   * Map keys must not contain {#&nbsp;} key markers.
   * @return String Replacements Result
   * @throws IOException
   * @throws IndexOutOfBoundsException
   */

  public String replace(StringBuffer oStrBuff, HashMap oMap)
    throws IOException, IndexOutOfBoundsException {

    int iChr;
    String sKey;
    Object oValue;

    Date dtToday = new Date();
    String sToday = String.valueOf(dtToday.getYear()+1900) + "-" + String.valueOf(dtToday.getMonth()+1) + "-" + String.valueOf(dtToday.getDate());

    oMap.put("Sistema.Fecha",sToday);
    oMap.put("System.Date",sToday);

    iReplacements = 0;

    oOutStream.setLength(0);

    int iAt = 0;
    final int iLen = oStrBuff.length();

    while (iAt<iLen) {
      iChr = oStrBuff.charAt(iAt++);

        if (123 == iChr) {
          // Se encontro el caracter '{'
          iChr = oStrBuff.charAt(iAt++);
          if (35 == iChr) {
            // Se encontro el caracter '#'
            iReplacements++;

            sKey = "";

            while (iAt<iLen) {
              iChr = oStrBuff.charAt(iAt++);
              if (125==iChr) break;
              sKey += (char) iChr;
            } // wend

            if (oMap.containsKey(sKey))
              oValue = oMap.get(sKey);
            else
            	oValue = "{#"+sKey+"}";

            if (null!=oValue)
              oOutStream.append(((String)oValue));
          } // fi ('#')

          else {
            oOutStream.append((char)123);
            oOutStream.append((char)iChr);
          }

        } // fi ('{')

        else
          oOutStream.append((char)iChr);

    } // wend

    return oOutStream.toString();
  } // replace()

  // ----------------------------------------------------------


  /**
   * Replace substrings from a Text File.
   * @param sFilePath File containing text to be replaced.
   * @param oMap Map with values to be replaced.<br>
   * Each map key will be replaced by its value.<br>
   * Map keys must appear in stream text as {#<i>key</i>}<br>
   * For example: InputStream "Today is {#System.Date}" will be replaced with "Today is 2002-02-21 11:32:44"<br>
   * No wildcards are accepted.<br>
   * Map keys must not contain {#&nbsp;} key markers.
   * @return String Replacements Result.
   * @throws IOException
   */

  public String replace(String sFilePath, HashMap oMap) throws IOException {

    FileInputStream oStrm = new FileInputStream(sFilePath);
    String sRetVal = replace(oStrm, oMap);
    oStrm.close();

    return sRetVal;
  }

  // ----------------------------------------------------------

  /**
   * Number of replacements done in last call to replace() method.
   * @return int
   */
  public int lastReplacements() {
    return iReplacements;
  }

  // ----------------------------------------------------------

  /**
   * <p>Create a HashMap for a couple of String Arrays</p>
   * This method is just a convenient shortcut for creating input HashMap for
   * replace methods from this class
   * @param aKeys An array of Strings to be used as keys
   * @param aValues An array of Strings that will be the actual values for the keys
   * @return A HashMap with the given keys and values
   * @throws ArrayIndexOutOfBoundsException
   */
  public static HashMap createMap(String[] aKeys, String[] aValues) throws ArrayIndexOutOfBoundsException {

    if (aKeys.length!=aValues.length)
    	throw new ArrayIndexOutOfBoundsException("FastStreamReplacer.createMap() ArrayIndexOutOfBoundsException supplied "+String.valueOf(aKeys.length)+" keys but "+String.valueOf(aValues.length)+" values");
    
    HashMap oRetVal  = new HashMap(5+((aKeys.length*100)/60));

    for (int k=0; k<aKeys.length; k++)
      oRetVal.put(aKeys[k], aValues[k]);

    return oRetVal;
  } // createMap

  // ----------------------------------------------------------

} // FastStreamReplacer
