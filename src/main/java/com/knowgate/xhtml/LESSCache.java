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

import java.io.File;
import java.io.IOException;

import java.util.WeakHashMap;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;

/**
 * LESS CSS caching using in-memory WeakHashMap
 * @author Sergio Montoro Ten
 * @see <a href="http://lesscss.org/">lesscss.org</a>
 */
public class LESSCache extends WeakHashMap<String,String> {

  /**
   * Convert a LESS CSS file into plain CSS
   * @param lcssFile File with LESS CSS
   * @return String CSS source code
   * @throws LessException
   * @throws IOException
   */
  public String render(File lcssFile)
	throws LessException, IOException {
    final String sFilePath = lcssFile.getAbsolutePath();
	String sRetCSS = null;    
	if (containsKey(sFilePath)) {
	  sRetCSS = get(sFilePath);
	} else {
      LessCompiler oLssC = null;
      try {
        oLssC = new LessCompiler();
        oLssC.setCompress(true);
	      sRetCSS = oLssC.compile(lcssFile);
		    put(sFilePath, sRetCSS);
      } finally {
    	if (oLssC!=null) oLssC.close();
      }
	}
	return sRetCSS;
  } // 	render

}
