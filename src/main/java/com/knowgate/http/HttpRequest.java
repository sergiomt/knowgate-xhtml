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

import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

import java.text.SimpleDateFormat;

import java.util.Map;
import java.util.Date;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.knallgrau.utils.textcat.TextCategorizer;

import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.StringBean;

import com.knowgate.stringutils.Str;
import com.knowgate.xhtml.HtmlUtil;
import com.knowgate.io.StreamPipe;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Wrapper for an HTTP request
 * @author Sergio Montoro Ten
 * @version 9.0
 *
 */
public class HttpRequest extends Thread implements Callable<Object> {

	private String sUrl;
	private URL oReferUrl;
	private String sMethod;
	private Entry<String,String>[] aParams;
	private Object oRetVal;
	private String sPageSrc, sEncoding, sUsr, sPwd;
	private int responseCode; 
	private List<Entry<String,String>> aHeaders;
	private List<Entry<String,String>> aCookies;

	private static final SimpleDateFormat oDtFmt = new SimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");

	// ------------------------------------------------------------------------

	/**
	 * Create new request for the given URL with default method GET
	 * @param sUrl String
	 */
	public HttpRequest(String sUrl) {
		this.sUrl = sUrl;
		oReferUrl = null;
		sMethod = "GET";
		aParams = null;
		responseCode=0;
		oRetVal = null;
		sPageSrc = null;
		sEncoding = null;
		sUsr = null;
		sPwd = null;
		aCookies = new ArrayList<Entry<String,String>>();
		aHeaders = new ArrayList<Entry<String,String>>();
	}	

	// ------------------------------------------------------------------------

	/**
	 * Create new request for the given URL
	 * @param sUrl String requested URL
	 * @param oReferUrl String Referrer URL
	 * @param sMethod String Must be "get" or "post"
	 * @param aParams Array with parameters to be sent along with get or post
	 */
	public HttpRequest(String sUrl, URL oReferUrl, String sMethod, Entry<String,String>[] aParams) {
		this.sUrl = sUrl;
		this.oReferUrl = oReferUrl;
		this.sMethod = sMethod;
		this.aParams = aParams;
		responseCode=0;
		oRetVal = null;
		sPageSrc = null;    
		sEncoding = null;  
		sUrl = null;
		sPwd = null;
		aCookies = new ArrayList<Entry<String,String>>();
		aHeaders = new ArrayList<Entry<String,String>>();
	}	

	// ------------------------------------------------------------------------

	/**
	 * Create new request for the given URL with basic authentication
	 * @param sUrl String requested URL
	 * @param oReferUrl String Referer URL
	 * @param sMethod String Must be "get" or "post"
	 * @param aParams Array with parameters to be sent along with get or post
	 * @param sUsr String
	 * @param sPwd String
	 */
	public HttpRequest(String sUrl, URL oReferUrl, String sMethod, Entry<String,String>[] aParams, String sUsr, String sPwd) {
		this.sUrl = sUrl;
		this.oReferUrl = oReferUrl;
		this.sMethod = sMethod;
		this.aParams = aParams;
		this.responseCode=0;
		this.oRetVal = null;
		this.sPageSrc = null;    
		this.sEncoding = null;  
		this.sUsr = sUsr;
		this.sPwd = sPwd;
		this.aCookies = new ArrayList<Entry<String,String>>();
		this.aHeaders = new ArrayList<Entry<String,String>>();
	}	

	// ------------------------------------------------------------------------

	/**
	 * Add a cookie for the next call
	 * @param name String Cookie Name
	 * @param value String Cookie Value
	 */
	public void addCookie(String name, String value) {
		aCookies.add(new SimpleImmutableEntry<String,String>(name,value));
	}

	// ------------------------------------------------------------------------

	/**
	 * Add a header for the next call
	 * @param name String Header Name
	 * @param value String Header Value
	 */
	public void addHeader(String name, String value) {
		aHeaders.add(new SimpleImmutableEntry<String,String>(name,value));
	}

	// ------------------------------------------------------------------------

	/**
	 * Get cookies read from the last get or post call
	 * @return List&lt;Entry&lt;String,String&gt;&gt;
	 */
	public List<Entry<String,String>> getCookies() {
		return Collections.unmodifiableList(aCookies);
	}

	// ------------------------------------------------------------------------

	/**
	 * Get headers read from the last get or post call
	 * @return List&lt;Entry&lt;String,String&gt;&gt;
	 */
	public List<Entry<String,String>> getHeaders() {
		return Collections.unmodifiableList(aHeaders);
	}

	// ------------------------------------------------------------------------

	/**
	 * Set cookies for next call to get or post method
	 * @param aCookies List&lt;Entry&lt;String,String&gt;&gt;
	 */
	public void setCookies(List<Entry<String,String>> aCookies) {
		this.aCookies = new ArrayList<Entry<String,String>>(aCookies.size());
		for (Entry<String,String> cookie : aCookies)
			this.aCookies.add(new SimpleImmutableEntry<String,String>(cookie.getKey(), cookie.getValue()));
	}

	// ------------------------------------------------------------------------

	/**
	 * Set cookies for next call to get or post method
	 * @param mCookies Map&lt;String,String&gt;
	 */
	public void setCookies(Map<String,String> mCookies) {
		this.aCookies = new ArrayList<Entry<String,String>>(mCookies.size());
		for (Entry<String,String> cookie : mCookies.entrySet())
			this.aCookies.add(new SimpleImmutableEntry<String,String>(cookie.getKey(), cookie.getValue()));
	}

	// ------------------------------------------------------------------------

	/**
	 * Set headers for next call to get or post method
	 * @param aHeaders List&lt;Entry&lt;String,String&gt;&gt;
	 */
	public void setHeaders(List<Entry<String,String>> aHeaders) {
		this.aHeaders = new ArrayList<Entry<String,String>>(aHeaders.size());
		for (Entry<String,String> header : aHeaders)
			this.aHeaders.add(new SimpleImmutableEntry<String,String>(header.getKey(), header.getValue()));
	}

	// ------------------------------------------------------------------------

	/**
	 * Set headers for next call to get or post method
	 * @param mHeaders Map&lt;String,String&gt;
	 */
	public void setHeaders(Map<String,String> mHeaders) {
		this.aHeaders = new ArrayList<Entry<String,String>>(mHeaders.size());
		for (Entry<String,String> header : mHeaders.entrySet())
			this.aHeaders.add(new SimpleImmutableEntry<String,String>(header.getKey(), header.getValue()));
	}

	// ------------------------------------------------------------------------

	@Override
	public void run() {
		call();
	} // run

	// ------------------------------------------------------------------------

	@Override
	public Object call() {
		try {
			if (sMethod.equalsIgnoreCase("POST"))
				return post();
			else if (sMethod.equalsIgnoreCase("GET"))
				return get();
			else if (sMethod.equalsIgnoreCase("HEAD"))
				return head();
			else
				throw new RuntimeException("Unknown HTTP method " + sMethod);
		} catch (Exception mue) {
			throw new RuntimeException(mue.getClass().getName() + " " + mue.getMessage(), mue);
		}

	}

	// ------------------------------------------------------------------------

	private void readResponseCookies(HttpURLConnection oCon) {
		Map<String,List<String>> oHdrs = oCon.getHeaderFields();
		Iterator<String> oNames = oHdrs.keySet().iterator();
		while (oNames.hasNext()) {
			String sName = oNames.next();
			if ("Set-Cookie".equals(sName)) {
				Iterator<String> oValues = oHdrs.get(sName).iterator();
				while (oValues.hasNext()) {
					String[] aCookie = oValues.next().split("; ");
					String[] aCookieNameValue=aCookie[0].split("=");
					boolean isExpired=false;
					try {
						String[] aCookieExpired=aCookie[1].split("=");
						Date dtExpires = oDtFmt.parse(aCookieExpired[1]);
						isExpired=(dtExpires.compareTo(new Date())<=0);
					} catch (Exception ignore) { }
					if (!isExpired) {
						if (aCookieNameValue.length>1)
							aCookies.add(new SimpleImmutableEntry<String,String>(aCookieNameValue[0],aCookieNameValue[1]));
						else
							aCookies.add(new SimpleImmutableEntry<String,String>(aCookieNameValue[0],""));        	  
					}
				} // wend
			} // fi
		} // wend
	}

	// ------------------------------------------------------------------------

	private void writeRequestCookies(HttpURLConnection oCon) {
		StringBuffer oCookies = new StringBuffer();
		for (Entry<String,String> nvp : aCookies)
			oCookies.append(nvp.getKey()+"="+nvp.getValue()+"; ");
		oCon.setRequestProperty("Cookie", oCookies.toString());
	}

	// ------------------------------------------------------------------------

	/**
	 * Perform HTTP POST request
	 * @return Object A String or a byte array containing the response to the request
	 * @throws IOException if server returned any status different from HTTP_MOVED_PERM (301), HTTP_MOVED_TEMP (302), HTTP_OK (200) or HTTP_ACCEPTED  (202)
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public Object post ()
			throws IOException, URISyntaxException, MalformedURLException {

		oRetVal = null;
		sPageSrc = null;
		sEncoding = null;

		URL oUrl;

		if (null==oReferUrl)
			oUrl = new URL(sUrl);
		else
			oUrl = new URL(oReferUrl, sUrl);

		String sParams = "";
		if (aParams!=null) {
			for (int p=0; p<aParams.length; p++) {
				sParams += aParams[p].getKey()+"="+URLEncoder.encode(aParams[p].getValue(), ISO_8859_1.name());
				if (p<aParams.length-1) sParams += "&";
			} // next
		} // fi

		HttpURLConnection oCon = (HttpURLConnection) oUrl.openConnection();

		oCon.setUseCaches(false);
		oCon.setInstanceFollowRedirects(false);
		oCon.setDoInput (true);
		oCon.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201");

		if (sUsr!=null && sPwd!=null) {
			final String credentials = sUsr + ":" + sPwd;
			oCon.setRequestProperty ("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
		}
		oCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		oCon.setRequestProperty("Content-Length", String.valueOf(sParams.getBytes().length));
		for (Entry<String,String> header : aHeaders)
			oCon.setRequestProperty(header.getKey(), header.getValue());
		oCon.setFixedLengthStreamingMode(sParams.getBytes().length);
		oCon.setDoOutput(true);
		oCon.setRequestMethod("POST"); 
		writeRequestCookies(oCon);
		OutputStreamWriter oWrt = new OutputStreamWriter(oCon.getOutputStream());
		oWrt.write(sParams);
		oWrt.flush();
		oWrt.close();

		responseCode = oCon.getResponseCode();
		String sLocation = oCon.getHeaderField("Location");
		if (sLocation!=null)
			if (sLocation.charAt(0)=='/')
				sLocation = "http://centros.lectiva.com"+sLocation;

		if ((responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
				responseCode == HttpURLConnection.HTTP_MOVED_TEMP) &&
				!sUrl.equals(sLocation)) {
			HttpRequest oMoved = new HttpRequest(sLocation, oUrl, "GET", null);
			readResponseCookies(oCon);
			oMoved.setCookies(getCookies());
			oRetVal = oMoved.post();
			sUrl = oMoved.url();
		} else if (responseCode == HttpURLConnection.HTTP_OK ||
				responseCode == HttpURLConnection.HTTP_ACCEPTED) {
			readResponseCookies(oCon);
			InputStream oStm = oCon.getInputStream();
			String sEnc = oCon.getContentEncoding();
			if (sEnc==null) {
				ByteArrayOutputStream oBya = new ByteArrayOutputStream();
				new StreamPipe().between(oStm, oBya);
				oRetVal = oBya.toByteArray();
			} else {
				int c;
				StringBuffer oDoc = new StringBuffer();
				Reader oRdr = new InputStreamReader(oStm, sEnc);
				while ((c=oRdr.read())!=-1) {
					oDoc.append((char) c);
				} // wend
				oRdr.close();
				oRetVal = oDoc.toString();
			}
			oStm.close();
		} else {
			throw new IOException(String.valueOf(responseCode));
		}
		oCon.disconnect();
		return oRetVal;
	} // post

	// ------------------------------------------------------------------------

	public String url() {
		return sUrl;
	}

	// ------------------------------------------------------------------------

	/**
	 * Perform HTTP GET request
	 * @return Object A String or a byte array containing the response to the request
	 * @throws IOException if server returned any status different from HTTP_MOVED_PERM (301), HTTP_MOVED_TEMP (302), HTTP_OK (200) or HTTP_ACCEPTED  (202)
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public Object get ()
			throws IOException, URISyntaxException, MalformedURLException {

		oRetVal = null;
		sPageSrc = null;
		sEncoding = null;

		URL oUrl;

		String sParams = "";
		if (aParams!=null) {
			if (sUrl.indexOf('?')<0) sParams = "?";
			for (int p=0; p<aParams.length; p++) {
				sParams += aParams[p].getKey()+"="+URLEncoder.encode(aParams[p].getValue(), ISO_8859_1.name());
				if (p<aParams.length-1) sParams += "&";
			} // next
		} // fi

		if (null==oReferUrl)
			oUrl = new URL(sUrl+sParams);
		else
			oUrl = new URL(oReferUrl, sUrl+sParams);

		HttpURLConnection oCon = (HttpURLConnection) oUrl.openConnection();

		if (sUsr!=null && sPwd!=null) {
			final String credentials = sUsr + ":" + sPwd;
			oCon.setRequestProperty ("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
		}
		oCon.setUseCaches(false);
		oCon.setInstanceFollowRedirects(false);
		oCon.setRequestMethod("GET");
		oCon.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201");
		for (Entry<String,String> header : aHeaders)
			oCon.setRequestProperty(header.getKey(), header.getValue());
		writeRequestCookies(oCon);

		responseCode = oCon.getResponseCode();

		if ((responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
				responseCode == HttpURLConnection.HTTP_MOVED_TEMP) &&
				!sUrl.equals(oCon.getHeaderField("Location"))) {
			HttpRequest oMoved = new HttpRequest(oCon.getHeaderField("Location"), oUrl, "GET", null);
			readResponseCookies(oCon);
			oMoved.setCookies(getCookies());	  
			oRetVal = oMoved.get();
			sUrl = oMoved.url();
			oCon.disconnect();
		} else {
			readResponseCookies(oCon);
			InputStream oStm = oCon.getInputStream();
			if (oStm!=null) {
				sEncoding = oCon.getContentEncoding();
				if (sEncoding==null) {
					ByteArrayOutputStream oBya = new ByteArrayOutputStream();
					new StreamPipe().between(oStm, oBya);
					oRetVal = oBya.toByteArray();
				} else {
					int c;
					StringBuffer oDoc = new StringBuffer();
					Reader oRdr = new InputStreamReader(oStm, sEncoding);
					while ((c=oRdr.read())!=-1) {
						oDoc.append((char) c);
					} // wend
					oRdr.close();
					oRetVal = oDoc.toString();
				}
				oStm.close();
			} // fi (oStm!=null)
			if (responseCode!=HttpURLConnection.HTTP_OK &&
					responseCode!=HttpURLConnection.HTTP_ACCEPTED) {
				oCon.disconnect();
				throw new IOException(String.valueOf(responseCode));
			} else {
				oCon.disconnect();
			} // fi (responseCode)
		} // fi

		return oRetVal;
	} // get

	// ------------------------------------------------------------------------

	/**
	 * Perform HTTP HEAD request
	 * @return int HTTP response code
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public int head ()
			throws IOException, URISyntaxException, MalformedURLException {

		oRetVal = null;
		sPageSrc = null;
		sEncoding = null;

		URL oUrl;

		String sParams = "";
		if (aParams!=null) {
			if (sUrl.indexOf('?')<0) sParams = "?";
			for (int p=0; p<aParams.length; p++) {
				sParams += aParams[p].getKey()+"="+URLEncoder.encode(aParams[p].getValue(), ISO_8859_1.name());
				if (p<aParams.length-1) sParams += "&";
			} // next
		} // fi

		if (null==oReferUrl)
			oUrl = new URL(sUrl+sParams);
		else
			oUrl = new URL(oReferUrl, sUrl+sParams);

		HttpURLConnection oCon = (HttpURLConnection) oUrl.openConnection();

		if (sUsr!=null && sPwd!=null) {
			final String credentials = sUsr + ":" + sPwd;
			oCon.setRequestProperty ("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
		}
		oCon.setUseCaches(false);
		oCon.setInstanceFollowRedirects(false);
		oCon.setRequestMethod("HEAD");
		oCon.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201");
		for (Entry<String,String> header : aHeaders)
			oCon.setRequestProperty(header.getKey(), header.getValue());

		responseCode = oCon.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
				responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
			HttpRequest oMoved = new HttpRequest(oCon.getHeaderField("Location"), oUrl, "HEAD", null);
			oMoved.head();
			sUrl = oMoved.url();
		} 

		oCon.disconnect();

		return responseCode;
	} // head

	// ------------------------------------------------------------------------

	/**
	 * Get response code from last GET, POST or HEAD request
	 * @return int
	 */
	public int responseCode() {
		return responseCode;
	}

	// ------------------------------------------------------------------------

	/**
	 * Get response as String
	 * @return String
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	public String src() throws IOException,UnsupportedEncodingException,URISyntaxException {


		if (sPageSrc==null) {
			if (oRetVal==null) get();

			sPageSrc = null;

			if (oRetVal!=null) {
				String sRcl = oRetVal.getClass().getName();
				if (sRcl.equals("[B")) {
					Pattern content = Pattern.compile("content=[\"']text/\\w+;\\s*charset=((_|-|\\d|\\w)+)[\"']", Pattern.CASE_INSENSITIVE);
					Pattern xml = Pattern.compile("<\\?xml version=\"1\\.0\" encoding=\"((_|-|\\d|\\w)+)\"\\?>", Pattern.CASE_INSENSITIVE);
					sPageSrc = new String((byte[]) oRetVal, sEncoding==null ? US_ASCII.name() : sEncoding);
					Matcher mtchr = content.matcher(sPageSrc);
					if (mtchr.find()) {
						sEncoding = mtchr.group(1);
						sPageSrc = new String((byte[]) oRetVal, sEncoding);
					} else {
						mtchr = xml.matcher(sPageSrc);
						if (mtchr.find()) {
							sEncoding = mtchr.group(1);
							sPageSrc = new String((byte[]) oRetVal, sEncoding);
						} else {
							if (null==sEncoding) sEncoding = US_ASCII.name();
						}
					}
				} else if (sRcl.equals("java.lang.String")) {
					sPageSrc = (String) oRetVal;
					sEncoding = "UTF8";
				}
			} // fi
		} // fi

		return sPageSrc;
	} // src

	// ------------------------------------------------------------------------

	/**
	 * Get response encoding
	 * @return String
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws URISyntaxException
	 */
	public String encoding() throws IOException,UnsupportedEncodingException,URISyntaxException {
		src();
		return sEncoding;
	} // encoding

	// ------------------------------------------------------------------------

	/**
	 * Get response HTML document title
	 * @return String
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	public String getTitle()
			throws IOException, URISyntaxException, MalformedURLException, UnsupportedEncodingException {

		src();

		String sTxTitle = null;

		if (sPageSrc!=null) {
			int t = Str.indexOfIgnoreCase(sPageSrc,"<title>",0);
			if (t>0) {
				int u = Str.indexOfIgnoreCase(sPageSrc,"</title>",t+7);
				if (u>0) {
					sTxTitle = Str.removeChars(sPageSrc.substring(t+7,u).trim(),"\t\n\r").trim();
					if (sTxTitle.length()>2000)
						sTxTitle = sTxTitle.substring(0,2000);
					sTxTitle = HtmlUtil.HTMLDencode(sTxTitle);
				}
			}         
		} // fi

		return sTxTitle;
	} // getTitle

	// ------------------------------------------------------------------------

	/**
	 * Get response HTML document content language
	 * @return String
	 * @throws IOException
	 * @throws ParserException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	public String getLanguage()
			throws IOException, URISyntaxException, MalformedURLException, UnsupportedEncodingException, ParserException {

		src();

		String sLanguage = null;

		if (sPageSrc!=null) {
			Pattern htmlLang = Pattern.compile("<html\\s+lang=[\"']?(\\w\\w)[\"']?>", Pattern.CASE_INSENSITIVE);
			Pattern htmlXmlns = Pattern.compile("<html\\s+xmlns=\"http://www.w3.org/1999/xhtml\"(?:\\s+xml:lang=\"\\w\\w-\\w\\w\")?\\s+lang=\"(\\w\\w)-\\w\\w\">", Pattern.CASE_INSENSITIVE);
			Pattern htmlMeta = Pattern.compile("<meta\\s+http-equiv=[\"']?Content-Language[\"']?\\s+content=[\"']?(\\w\\w)[\"']?\\s?/?>", Pattern.CASE_INSENSITIVE);
			Matcher mtchr = htmlLang.matcher(sPageSrc);
			if (mtchr.find()) {
				sLanguage = mtchr.group(1);
			} else {
				mtchr = htmlXmlns.matcher(sPageSrc);
				if (mtchr.find()) {
					sLanguage = mtchr.group(1);
				} else {
					mtchr = htmlMeta.matcher(sPageSrc);
					if (mtchr.find())
						sLanguage = mtchr.group(1);
				}
			}

			if (null==sLanguage) {
				TextCategorizer oTxtc = new TextCategorizer();
				if (Str.indexOfIgnoreCase(sPageSrc,"<html")>=0) {
					Parser oPrsr = Parser.createParser(sPageSrc, sEncoding);
					StringBean oStrBn = new StringBean();
					oPrsr.visitAllNodesWith (oStrBn);
					sLanguage = oTxtc.categorize(oStrBn.getStrings());	  
				} else {
					sLanguage = oTxtc.categorize(sPageSrc);
				}
			} // fi
		} // fi

		return sLanguage;
	} // getLanguage()

}