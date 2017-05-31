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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import java.util.WeakHashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * XSL File Cache
 * This class keeps a master copy in memory of each XSL Stylesheet file.<br>
 * When a Transformer object is requested a copy of the master Stylesheet is
 * done. This is faster than re-loading the XSL file from disk.<br>
 * StylesheetCache is a WeakHashMap so cached stylesheets can be automatically
 * garbage collected is memory runs low.
 * @author Sergio Montoro Ten
 * @version 7.0
 */
public class StylesheetCache {

  private StylesheetCache() { }

  // ---------------------------------------------------------------------------

  /**
   * Get Transformer object for XSL file.
   * StylesheetCache automatically checks file last modification date and compares
   * it with loading date for cached objects. If file is more recent than its cached
   * object then the disk copy is reloaded.
   * @param sFilePath File Path
   * @throws IOException
   * @throws TransformerException
   * @throws TransformerConfigurationException
   */
  public static synchronized Transformer newTransformer(String sFilePath)
    throws FileNotFoundException, IOException, TransformerException, TransformerConfigurationException {

    File oFile = new File(sFilePath);

    if (!oFile.exists()) {
      throw new FileNotFoundException(sFilePath);
    }
    long lastMod = oFile.lastModified();

    TransformerFactory oFactory;
    Templates oTemplates;
    StreamSource oStreamSrc;
    SheetEntry oSheet = oCache.get(sFilePath);

    if (null!=oSheet) {
      if (lastMod>oSheet.lastModified) {
        oSheet = null;
        oCache.remove(sFilePath);
      }
    } // fi (oSheet)

    if (null==oSheet) {
      oFactory = TransformerFactory.newInstance();
      File oFlSrc = new File(sFilePath);
      oStreamSrc = new StreamSource(oFlSrc);
      oStreamSrc.setSystemId(oFlSrc);
      oTemplates = oFactory.newTemplates(oStreamSrc);
      oSheet = new SheetEntry(lastMod, oTemplates);
      oCache.put(sFilePath, oSheet);
    } // fi

    Transformer oTransformer = oSheet.templates.newTransformer();

    return oTransformer;
  } // newTransformer()

  // ---------------------------------------------------------------------------

  /**
   * Set parameters for a StyleSheet taken from a properties collection.
   * This method is primarily designed for setting environment parameters.
   * @param oXSL Transformer object.
   * @param oProps Properties to be set as parameters. The substring "param_"
   * will be added as a preffix to each property name passed as parameter.
   * So if you pass a property named "workarea" it must be retrieved from XSL
   * as &lt;xsl:param name="param_workarea"/&gt;
   * @throws NullPointerException if oXSL is <b>null</b> or oProps is <b>null</b>
   */
  public static void setParameters(Transformer oXSL, Properties oProps)
    throws NullPointerException {

    String sKey, sVal;
    Iterator myIterator = oProps.keySet().iterator();

    while (myIterator.hasNext())
    {
      sKey = (String) myIterator.next();
      sVal = oProps.getProperty(sKey);

      try {
       	oXSL.setParameter("param_" + sKey, sVal);
      } catch (Exception ignore) {
      }
    } // wend()

  } // setParameters

  // ---------------------------------------------------------------------------

  /**
   * Perform XSLT transformation
   * @param sStyleSheetPath File Path to XSL style sheet file
   * @param oXMLInputStream Input Stream for XML source data
   * @param oOutputStream Stream where output is to be written
   * @param oProps Parameters for Transformer. The substring "param_"
   * will be added as a prefix to each property name passed as parameter.
   * So if you pass a property named "workarea" it must be retrieved from XSL
   * as &lt;xsl:param name="param_workarea"/&gt;
   * @throws NullPointerException if oProps is <b>null</b>
   * @throws FileNotFoundException if sStyleSheetPath does not exist
   * @throws IOException
   * @throws TransformerException
   * @throws TransformerConfigurationException
   */
  public static void transform (String sStyleSheetPath,
                                InputStream oXMLInputStream,
                                OutputStream oOutputStream, Properties oProps)
    throws IOException, FileNotFoundException,
           NullPointerException, TransformerException, TransformerConfigurationException {

    long lElapsed = 0;

    if (null==sStyleSheetPath)
      	throw new NullPointerException ("StylesheetCache.transform() style sheet path may not be null");

    if (null==oXMLInputStream)
      	throw new NullPointerException ("StylesheetCache.transform() InputStream may not be null");

    if (null==oOutputStream)
      	throw new NullPointerException ("StylesheetCache.transform() OutputStream may not be null");

    Transformer oTransformer = StylesheetCache.newTransformer(sStyleSheetPath);

    if (null!=oProps) setParameters(oTransformer, oProps);

    StreamSource oStreamSrcXML = new StreamSource(oXMLInputStream);

    StreamResult oStreamResult = new StreamResult(oOutputStream);

    oTransformer.transform(oStreamSrcXML, oStreamResult);

  } // transform

  // ---------------------------------------------------------------------------

  /**
   * Perform XSLT transformation
   * @param sStyleSheetPath File Path to XSL style sheet file
   * @param sXMLInput Input String with XML source data
   * @param oProps Parameters for Transformer. The substring "param_"
   * will be added as a preffix to each property name passed as parameter.
   * So if you pass a property named "workarea" it must be retrieved from XSL
   * as &lt;xsl:param name="param_workarea"/&gt;
   * @return String Transformed document
   * @throws NullPointerException if sXMLInput or oProps are <b>null</b>
   * @throws FileNotFoundException if sStyleSheetPath does not exist
   * @throws IOException
   * @throws UnsupportedEncodingException
   * @throws TransformerException
   * @throws TransformerConfigurationException
   * @since 3.0
   */
  public static String transform (String sStyleSheetPath, String sXMLInput, Properties oProps)
    throws IOException, FileNotFoundException, UnsupportedEncodingException,
           NullPointerException, TransformerException, TransformerConfigurationException {

    if (null==sXMLInput) {
      throw new NullPointerException("StylesheetCache.transform() XML input String may not be null");
    }

    // ****************************************
    // Get character encoding of input XML data
    String sEncoding;
    int iEnc = sXMLInput.toLowerCase().indexOf("encoding");
    if (iEnc<0) {
      sEncoding = "ISO8859_1";
    } else {
      int iBeg = iEnc+8;
      int iEnd;
      while (sXMLInput.charAt(iBeg)==' ' || sXMLInput.charAt(iBeg)=='=') iBeg++;
      while (sXMLInput.charAt(iBeg)==' ') iBeg++;
      if (sXMLInput.charAt(iBeg)=='"') {
        iEnd = ++iBeg;
        while (sXMLInput.charAt(iEnd)!='"') iEnd++;
      } else {
        iEnd = iBeg;
        while (sXMLInput.charAt(iEnd)!=' ' && sXMLInput.charAt(iEnd)!='?') iEnd++;
      } // fi
      sEncoding = sXMLInput.substring(iBeg, iEnd);
    } // fi
    // ****************************************

    ByteArrayOutputStream oOutputStream = new ByteArrayOutputStream();
    ByteArrayInputStream oXMLInputStream = new ByteArrayInputStream(sXMLInput.getBytes(sEncoding));
    Transformer oTransformer = StylesheetCache.newTransformer(sStyleSheetPath);
    if (null!=oProps) setParameters(oTransformer, oProps);
    StreamSource oStreamSrcXML = new StreamSource(oXMLInputStream);
    StreamResult oStreamResult = new StreamResult(oOutputStream);
    oTransformer.transform(oStreamSrcXML, oStreamResult);
    oStreamSrcXML = null;
    oXMLInputStream.close();
    String sRetVal = oOutputStream.toString(sEncoding);
    oStreamResult = null;
    oOutputStream.close();

    return sRetVal;
  } // transform

  // ---------------------------------------------------------------------------

  /**
   * Perform XSLT transformation
   * @param oStyleSheetStream Stream to XSL style sheet
   * @param oXMLInputStream Input Stream with XML source data
   * @param sEncoding Input Stream data encoding
   * @param oProps Parameters for Transformer. The substring "param_"
   * will be added as a preffix to each property name passed as parameter.
   * So if you pass a property named "workarea" it must be retrieved from XSL
   * as &lt;xsl:param name="param_workarea"/&gt;
   * @return String Transformed document
   * @throws NullPointerException if sXMLInput or oProps are <b>null</b>
   * @throws FileNotFoundException if sStyleSheetPath does not exist
   * @throws IOException
   * @throws UnsupportedEncodingException
   * @throws TransformerException
   * @throws TransformerConfigurationException
   * @since 6.0
   */
  public static String transform (InputStream oStyleSheetStream, InputStream oXMLInputStream,
  								  String sEncoding, Properties oProps)
    throws IOException, FileNotFoundException, UnsupportedEncodingException,
           NullPointerException, TransformerException, TransformerConfigurationException {

    if (null==oStyleSheetStream) {
      throw new NullPointerException("StylesheetCache.transform() XSL input stream may not be null");
    }

    if (null==oXMLInputStream) {
      throw new NullPointerException("StylesheetCache.transform() XML input stream may not be null");
    }

    ByteArrayOutputStream oOutputStream = new ByteArrayOutputStream();

    Transformer oTransformer;
    final String sXSLSystemId = oProps.getProperty("XSLSystemId");

    if (sXSLSystemId==null) {
      TransformerFactory oFactory = TransformerFactory.newInstance();
      StreamSource oStreamSrc = new StreamSource(oStyleSheetStream);
      Templates oTemplates = oFactory.newTemplates(oStreamSrc);
      oTransformer = oTemplates.newTransformer();
    } else {
      if (oCache.containsKey(sXSLSystemId)) {
    	oTransformer = StylesheetCache.newTransformer(sXSLSystemId);
      } else {
        TransformerFactory oFactory = TransformerFactory.newInstance();
        StreamSource oStreamSrc = new StreamSource(oStyleSheetStream);
        oStreamSrc.setSystemId(oProps.getProperty("XSLSystemId"));
        Templates oTemplates = oFactory.newTemplates(oStreamSrc);
        oTransformer = oTemplates.newTransformer();    	  
      }
    }

    if (null!=oProps) setParameters(oTransformer, oProps);
    StreamSource oStreamSrcXML = new StreamSource(oXMLInputStream);
    if (oProps.getProperty("XMLSystemId")!=null) oStreamSrcXML.setSystemId(oProps.getProperty("XMLSystemId"));
    StreamResult oStreamResult = new StreamResult(oOutputStream);
    oTransformer.transform(oStreamSrcXML, oStreamResult);
    oStreamSrcXML = null;
    oXMLInputStream.close();
    String sRetVal = oOutputStream.toString(sEncoding);
    oStreamResult = null;
    oOutputStream.close();

    return sRetVal;
  } // transform

  // ---------------------------------------------------------------------------

  /**
   * Perform XSLT transformation
   * @param oStyleSheetStream Stream to XSL style sheet
   * @param sXMLInput Input String with XML source data
   * @param oProps Parameters for Transformer. The substring "param_"
   * will be added as a preffix to each property name passed as parameter.
   * So if you pass a property named "workarea" it must be retrieved from XSL
   * as &lt;xsl:param name="param_workarea"/&gt;
   * @return String Transformed document
   * @throws NullPointerException if sXMLInput or oProps are <b>null</b>
   * @throws FileNotFoundException if sStyleSheetPath does not exist
   * @throws IOException
   * @throws UnsupportedEncodingException
   * @throws TransformerException
   * @throws TransformerConfigurationException
   * @since 5.0
   */
  public static String transform (InputStream oStyleSheetStream, String sXMLInput, Properties oProps)
    throws IOException, FileNotFoundException, UnsupportedEncodingException,
           NullPointerException, TransformerException, TransformerConfigurationException {

    if (null==oStyleSheetStream) {
      throw new NullPointerException("StylesheetCache.transform() XSL input stream may not be null");
    }

    if (null==sXMLInput) {
      throw new NullPointerException("StylesheetCache.transform() XML input String may not be null");
    }

    // ****************************************
    // Get character encoding of input XML data
    String sEncoding;
    int iEnc = sXMLInput.toLowerCase().indexOf("encoding");
    if (iEnc<0) {
      sEncoding = "ISO8859_1";
    } else {
      int iBeg = iEnc+8;
      int iEnd;
      while (sXMLInput.charAt(iBeg)==' ' || sXMLInput.charAt(iBeg)=='=') iBeg++;
      while (sXMLInput.charAt(iBeg)==' ') iBeg++;
      if (sXMLInput.charAt(iBeg)=='"') {
        iEnd = ++iBeg;
        while (sXMLInput.charAt(iEnd)!='"') iEnd++;
      } else {
        iEnd = iBeg;
        while (sXMLInput.charAt(iEnd)!=' ' && sXMLInput.charAt(iEnd)!='?') iEnd++;
      } // fi
      sEncoding = sXMLInput.substring(iBeg, iEnd);
    } // fi
    // ****************************************

    ByteArrayInputStream oXMLInputStream = new ByteArrayInputStream(sXMLInput.getBytes(sEncoding));
    String sRetVal = transform(oStyleSheetStream, oXMLInputStream, sEncoding, oProps);    
	oXMLInputStream.close();
	
    return sRetVal;
  } // transform

  // ---------------------------------------------------------------------------

  /**
   * Validate an XML document using an XSD schema
   * @param oXsd InputStream to XSD schema
   * @param oXml InputStream to XML document
   * @return String An empty string if validation was successful or text describing the error found
   * @since 7.0
   */
  
  public String validate(InputStream oXsd, InputStream oXml) {
    String sRetVal;
	try {
	  SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
	  Schema schema = factory.newSchema(new StreamSource(oXsd));
	  Validator validator = schema.newValidator();
	  validator.validate(new StreamSource(oXml));
	  sRetVal = "";
	} catch (Exception xcpt) {
      sRetVal = xcpt.getMessage();
	}	
	return sRetVal;
  }
  
  // ---------------------------------------------------------------------------
  
  /**
   * Clear XLS Stylesheets cache
   * @since 7.0
   */
  public static void clearCache () {
    oCache.clear();
  }
  
  // ---------------------------------------------------------------------------

  static class SheetEntry {
    long lastModified;
    Templates templates;

    SheetEntry (long lLastModified, Templates oTemplats) {
      lastModified = lLastModified;
      templates = oTemplats;
    }
  } // SheetEntry

  private static WeakHashMap<String,SheetEntry> oCache = new WeakHashMap<String,SheetEntry>();
} // StylesheetCache
