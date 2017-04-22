package com.knowgate.xhtml;

/**
 * This file is licensed under the Apache License version 2.0.
 * You may not use this file except in compliance with the license.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.
 */

import java.io.*;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * SAXValidate Class.
 * XML Document Parser Validator.
 * @author Carlos Enrique Navarro Candil
 * @version 1.0
 */

public class SAXValidate extends DefaultHandler
                        implements ErrorHandler {

   private static final String
           DEFAULT_PARSER = "org.apache.xerces.parsers.SAXParser";
   private boolean schemavalidate = false;
   @SuppressWarnings("unused")
   private boolean valid;

   /**
    * Create instance of handler class
    * @param validateschema  boolean
    */
   public SAXValidate(boolean validateschema) {
     this.schemavalidate = validateschema;
     this.valid = true;
   }

   public void error (SAXParseException exception) throws SAXException {
     this.valid = false;
     System.out.println("ERROR: " + exception.getMessage());
   }

  /**
   * main routine to test validation.
   */
  static public void main(String[] args) {

    if (args.length < 1 || args.length > 2) {
      System.err.println("Usage: java SAXValidate [-s] <xmlfile>");
    } else {
      boolean svalidate = false;
      String filename = "";

      if (args.length > 1) {
        if (args[0].equals("-s")) {
          svalidate = true;
        }
        filename = args[1];
      } else {
        filename = args[0];
      }

      SAXValidate test = new SAXValidate(svalidate);

      try {
        test.runTest(new FileReader(new File(filename).toString()),
                   DEFAULT_PARSER);
      } catch (Exception e) {
        System.err.println("Error running test.");
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
      }
    }
  }

  /**
   * Test XML file
   * @param xml Reader
   * @param parserName String Name of a SAX2 compliant class, for example org.apache.xerces.parsers.SAXParser
   */

  public void runTest(Reader xml, String parserName)
                  throws IOException, ClassNotFoundException {

    try {

      // Get parser instance
      XMLReader parser = XMLReaderFactory.createXMLReader(parserName);

      // Configure parser handlers
      parser.setContentHandler((ContentHandler)this);
      parser.setErrorHandler((ErrorHandler)this);

      parser.setFeature("http://xml.org/sax/features/validation", true);
      if (schemavalidate) {
        parser.setFeature("http://apache.org/xml/features/validation/schema", true);
      }

      // Parse document
      parser.parse(new InputSource(xml));

    } catch (SAXParseException e) {
      System.err.println(e.getMessage());
    } catch (SAXException e) {
      System.err.println(e.getMessage());
    } catch (Exception e) {
      System.err.println(e.toString());
    }
  }
}
