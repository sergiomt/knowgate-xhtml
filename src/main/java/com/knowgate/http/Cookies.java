package com.knowgate.http;

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

import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

/**
 * <p>Cookies</p>
 * <p>Company: KnowGate</p>
 * @version 5.0
 */
public class Cookies {

	public static String getCookie (Object httpServletRequest, String sName, String sDefault, String sEncoding) {
		Object[] aCookies = null;
		String sValue = null;
		try {
			aCookies = (Object[]) httpServletRequest.getClass().getMethod("getCookies").invoke(httpServletRequest);
			if (aCookies!=null) {
				for (int c=0; c<aCookies.length; c++) {
					Object cookie = aCookies[c];
					String name = (String) cookie.getClass().getMethod("getName").invoke(cookie);
					if (name.equals(sName)) {
						sValue = URLDecoder.decode((String) cookie.getClass().getMethod("getValue").invoke(cookie),sEncoding);
						break;
					} // fi(aCookies[c]==sName)
				} // next(c)
			}
		} catch (IllegalAccessException iace) {
		} catch (IllegalArgumentException iare) {
		} catch (InvocationTargetException itae) {
		} catch (NoSuchMethodException nsme) {
		} catch (SecurityException sece) {
		} catch (UnsupportedEncodingException ence) {
		}
		return sValue!=null ? sValue : sDefault;
	} // getCookie()

} // Cookies