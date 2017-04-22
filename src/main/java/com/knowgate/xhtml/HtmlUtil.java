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

public class HtmlUtil {

    final static String[] aEnts = {"amp;", "lt;", "gt;", "quot;", "iexcl;", "curren;", "yen;", "brvbar;", "sect;",
            "uml;", "copy;", "ordf;", "laquo;", "raquo;", "euro;", "pound;", "shy;", "reg;",
            "macr;", "deg;", "plusmn;", "sup1;", "sup2;", "sup3;", "acute;", "micro;", "para;",
            "middot;", "cedil;", "ordm;", "iquest;", "ntilde;", "Ntilde;", "aacute;", "eacute;", "iacute;",
            "oacute;", "uacute;", "uuml;", "Aacute;", "Agrave;", "Auml;", "Acirc;", "Aring;", "Eacute;",
            "Egrave;", "Euml;", "Ecirc;", "Iacute;", "Igrave;", "Iuml;", "Icirc;", "Oacute;", "Ograve;",
            "Ouml;", "Ocirc;", "Uacute;", "Ugrave;", "Uuml;", "Ucirc;", "frac12;", "frac34;", "frac14;",
            "Ccedil;", "ccedil;", "eth;", "cent;", "THORN;",  "thorn;", "ETH;", "times;", "divide;",
            "AElig;", "ordf;", "hellip;", "bull;", "ldquo;", "rdquo;", "ndash;", "mdash;", "oline;",
            "Alpha;", "Beta;", "Gamma;", "Delta;", "Epsilon;", "Lambda;", "Sigma;", "Pi;", "Psi;", "Omega;",
            "alpha;", "beta;", "gamma;", "delta;", "epsilon;", "lambda;", "sigma;", "pi;", "zeta;", "omega;",
            "forall;", "part;", "exist;", "empty;", "isin;", "notin;", "sum;", "infin;", "minus;",
            "loz;", "spades;", "clubs;", "hearts;", "diams;", "nbsp;"
            };

    final static char[] aChars= {'&', '<', '>', '\'', '¡', '¤', '¥', '|', '§',
             '¨', '©', 'ª', '«' , '»', '€', '£', '­', '®',
             '¯', '°', '±', '¹' , '²', '³', '´', 'µ', '¶',
             '·', '¸', 'º', '¿' , 'ñ', 'Ñ', 'á', 'é', 'í',
             'ó', 'ú', 'ü', 'Á' , 'À', 'Ä', 'Â', 'Å', 'É',
             'È', 'Ë', 'Ê', 'Í' , 'Ì', 'Ï', 'Î', 'Ó', 'Ò',
             'Ö', 'Ô', 'Ú', 'Ù' , 'Ü', 'Û', '½', '¾', '¼',
             'Ç', 'ç', 'ð', '¢' , 'Þ', 'þ', 'Ð', '×', '÷',
             'Æ', 'ª', '…', '•' , '“', '”', '–', '—', '‾',
             'Α', 'Β', 'Γ', 'Δ' , 'Ε', 'Λ', 'Σ', 'Π', 'Ψ', 'Ω',
             'α', 'β', 'γ', 'δ' , 'ε', 'λ', 'σ', 'σ', 'ζ', 'ω',
             '∀', '∂', '∃', '∅' , '∈', '∈', '∑', '∞', '−',
             '◊', '♠', '♣', '♥' , '♦', ' '
            };
	
  /**
   * Replace HTML entities with UTF-cters8 characters
   * @param text String
   * @return Input string with HTML entities replaced by UTF-8 characters
   */
  public static String HTMLDencode(String text) {
    if (text == null) return "";

    char c;
    int len = text.length();
    StringBuffer results = new StringBuffer(len);

    final int iEnts = aEnts.length;
    
    for (int i = 0; i < len; ) {
      c = text.charAt(i);
      if (c=='&' && i<len-3) {
        try {
          int semicolon = text.indexOf(59, i+1)+1;
          if (semicolon>0) {
            if (text.charAt(i+1)=='#') {
            	if (text.charAt(i+2)=='x')
                results.append( (char) Integer.parseInt(text.substring(i + 3, semicolon-1), 16));
              else
                results.append( (char) Integer.parseInt(text.substring(i + 2, semicolon-1)));
              i = semicolon;
            } else {
              int e = -1;
              for (int f=0; f<iEnts && e<0; f++)
              	if (aEnts[f].equals(text.substring(i+1, semicolon)))
              	  e = f;
              if (e>=0) {
                results.append(aChars[e]);
                i = semicolon;
              } else {
                results.append(c);
                i++;
              }
            }          
          } else {
            results.append(c);
            i++;        
          }
        } catch (StringIndexOutOfBoundsException siob) {
          return results.toString();
        }
      } else {
        results.append(c);
        i++;
      }
    } // next (i)

    return results.toString();
  } // HTMLDencode

  /**
   * <p>Return text encoded as HTML.</p>
   * @param text String to encode
   * @return HTML-encoded text. If text is <b>null</b> then an empty String "" is returned.
   */
  public static String HTMLEncode(String text) {
    if (text == null) return "";
    char c;
    final int len = text.length();
    final int cln = aChars.length;
    StringBuilder results = new StringBuilder(len*2);
    
    for (int i = 0; i < len; ++i) {
      c = text.charAt(i);
      for (int j=0; j<cln; c++)
    	  if (c==aChars[j])
        	  results.append('&').append(aEnts[j]);    		  
    	  else
    		  results.append('c');
    }
    return results.toString();
  }

}
