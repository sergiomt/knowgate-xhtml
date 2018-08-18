package com.knowgate.mail;

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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.net.ssl.SSLContext;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import javax.mail.BodyPart;
import javax.mail.AuthenticationFailedException;
import javax.mail.NoSuchProviderException;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.URLName;
import javax.mail.Session;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.Folder;
import javax.mail.Flags;
import javax.mail.FetchProfile;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import javax.mail.internet.InternetAddress;

import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.sun.mail.smtp.SMTPMessage;

import com.knowgate.debug.Chronometer;
import com.knowgate.debug.DebugFile;
import com.knowgate.debug.StackTraceUtil;
import com.knowgate.stringutils.Str;
import com.knowgate.xhtml.FastStreamReplacer;

/**
 * <p>A wrapper around javax.mail.Store and javax.mail.Transport</p>
 * @author Sergio Montoro Ten
 * @version 8.0
 */
public class MailSessionHandler {

  protected String sInAccountName;
  protected String sInAuthStr;
  protected String sOutAccountName;
  protected String sOutAuthStr;
  protected String sInHostName;
  protected String sOutHostName;
  protected int iOutPortNum;
  protected String sMBoxDir;
  protected Map<String,String> oProps;
  protected URLName oURLSession;
  protected Session oMailSession;
  protected Session oSmtpSession;
  protected Store oMailStore;
  protected Transport oMailTransport;
  protected boolean bIsStoreConnected;
  protected boolean bIsTransportConnected;
  protected boolean bIncomingSSL;
  protected boolean bOutgoingSSL;

  // ---------------------------------------------------------------------------

  /**
   * Default constructor
   */
  public MailSessionHandler() {
    bIsStoreConnected = bIsTransportConnected = false;
    oMailTransport = null;
    oMailStore = null;
    oProps = null;
    sOutAccountName = sOutAuthStr = sInAccountName = sInAuthStr = sInHostName = sOutHostName = sMBoxDir = null;
    iOutPortNum = 25;
    oMailSession = null;
    bOutgoingSSL = bIncomingSSL=false;
  }

  /**
   * Create session using given Properties
   * @param oMailProperties Properties<br>
   * <table><tr><th>Property</th><th>Description></th><th>Default value</th></tr>
   *        <tr><td>mail.user</td><td>Store and transport user</td><td></td></tr>
   *        <tr><td>mail.password</td><td></td>Store and transport password<td></td></tr>
   *        <tr><td>mail.store.protocol</td><td></td><td>pop3</td></tr>
   *        <tr><td>mail.transport.protocol</td><td></td><td>smtp</td></tr>
   *        <tr><td>mail.<i>storeprotocol</i>.host</td><td>For example: pop.mailserver.com</td><td></td></tr>
   *        <tr><td>mail.<i>storeprotocol</i>.socketFactory.class</td><td>Only if using SSL set this value to javax.net.ssl.SSLSocketFactory</td><td></td></tr>
   *        <tr><td>mail.<i>storeprotocol</i>.socketFactory.port</td><td>Only if using SSL</td><td></td></tr>
   *        <tr><td>mail.<i>transportprotocol</i>.host</td><td>For example: smtp.mailserver.com</td><td></td></tr>
   *        <tr><td>mail.<i>transportprotocol</i>.port</td><td>For example: 587</td><td></td></tr>
   *        <tr><td>mail.<i>transportprotocol</i>.socketFactory.class</td><td>Only if using SSL set this value to javax.net.ssl.SSLSocketFactory</td><td></td></tr>
   *        <tr><td>mail.<i>transportprotocol</i>.socketFactory.port</td><td>Only if using SSL</td><td></td></tr>
   *        <tr><td>proxySet</td><td>Use proxy</td><td>false</td></tr>
   *        <tr><td>socksProxyHost</td><td>Proxy IP address</td><td></td></tr>
   *        <tr><td>socksProxyPort</td><td>Proxy Port</td><td></td></tr>
   *        <tr><td>mail.smtp.debug</td><td>JavaMail Debug Mode</td><td>true or false (default)</td></tr>
   * </table>
   * @throws NullPointerException if oMailProperties is null
   * @since 3.1
   */
  public MailSessionHandler(Map<String,String> oMailProperties)
    throws NullPointerException {
	oProps = oMailProperties;
    sOutAccountName = sInAccountName = oProps.get("mail.user");
    sOutAuthStr = sInAuthStr = oProps.get("mail.password");
    bIsStoreConnected = bIsTransportConnected = false;
	oMailTransport = null;
	oMailStore = null;
	sMBoxDir = null;
	oMailSession = null;
	String sStoreProtocol = oProps.getOrDefault("mail.store.protocol", "pop3");
    sInHostName = oProps.get("mail."+sStoreProtocol+".host");
	bIncomingSSL = oProps.getOrDefault("mail."+sStoreProtocol+".socketFactory.class", "").equals("javax.net.ssl.SSLSocketFactory");
    if (sInAuthStr!=null) {
        oProps.put("mail."+sStoreProtocol+".auth", "true");
      }
    if (bIncomingSSL) {
      oProps.put("mail."+sStoreProtocol+".socketFactory.port", oMailProperties.get("mail."+sStoreProtocol+".port"));
      oProps.put("mail."+sStoreProtocol+".socketFactory.fallback", "false");	
    }
	String sTransportProtocol = oProps.getOrDefault("mail.transport.protocol", "smtp");
    sOutHostName = oProps.get("mail."+sTransportProtocol+".host");
    iOutPortNum = Integer.parseInt(oProps.getOrDefault("mail."+sTransportProtocol+".port","25"));
	bOutgoingSSL = oProps.getOrDefault("mail."+sTransportProtocol+".socketFactory.class", "").equals("javax.net.ssl.SSLSocketFactory");
    if (sOutAuthStr!=null) {
      oProps.put("mail."+sTransportProtocol+".auth", "true");
    }
    if (bOutgoingSSL) {
        oProps.put("mail."+sTransportProtocol+".socketFactory.port", String.valueOf(iOutPortNum));
        oProps.put("mail."+sTransportProtocol+".socketFactory.fallback", "false");	
        oProps.put("mail."+sTransportProtocol+".starttls.enable","true");
    }
    if (bIncomingSSL || bOutgoingSSL) {
      try {
	      Security.addProvider(SSLContext.getDefault().getProvider());
      } catch (NoSuchAlgorithmException neverthrown) { }
    }
  }

  // ---------------------------------------------------------------------------

  /**
   * <p>Get column incoming_account of k_user_mail</p>
   * @return String account name or <b>null</b> if this instance has not been
   * initialized from a MailAccount object
   */
  public String getAccountName() {
    return sInAccountName;
  }

  // ---------------------------------------------------------------------------

  /**
   * Set incoming mail account name
   */
  public void setAccountName(String aAccName) {
    sInAccountName=aAccName;
  }

  // ---------------------------------------------------------------------------

  /**
   * <p>Get column incoming_password of k_user_mail</p>
   * @return String password or <b>null</b> if this instance has not been
   * initialized from a MailAccount object
   */
  public String getAuthStr() {
    return sInAuthStr;
  }

  // ---------------------------------------------------------------------------

  /**
   * Set incoming mail password
   */
  public void setAuthStr(String aAutStr) {
    sInAuthStr=aAutStr;
  }

  // ---------------------------------------------------------------------------

  /**
   * <p>Get column incoming_server of k_user_mail</p>
   * @return String
   */
  public String getHostName() {
    return sInHostName;
  }

  // ---------------------------------------------------------------------------

  /**
   * Set incoming mail host name or IP address
   */
  public void setHostName(String sName) {
    sInHostName=sName;
  }

  // ---------------------------------------------------------------------------

  public String getMBoxDirectory() {
    return sMBoxDir;
  }

  // ---------------------------------------------------------------------------

  public void setMBoxDirectory(String sDir) {
    sMBoxDir=sDir;
  }

  // ---------------------------------------------------------------------------

  public Map<String,String> getProperties() {
    return oProps;
  }

  // ---------------------------------------------------------------------------

  public void setProperties(Map<String,String> oPropties) {
    oProps = oPropties;
  }

  // ---------------------------------------------------------------------------

  /**
   * <p>Get incoming mail server Session</p>
   * This method calls JavaMail Session.getInstance() method if neccesary,
   * using properties currently set at this instance and SilentAuthenticator as
   * Authenticator subclass
   * @return javax.mail.Session
   * @throws IllegalStateException
   * @throws NullPointerException
   */
  public Session getSession() throws IllegalStateException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.getSession()");
      DebugFile.incIdent();
    }
    if (null==oMailSession) {
      if (null==oProps) {
        if (DebugFile.trace) DebugFile.decIdent();
        throw new IllegalStateException("SessionHandler properties not set");
      }
      if (null==sInAccountName) {
        if (DebugFile.trace) DebugFile.decIdent();
        throw new NullPointerException("SessionHandler account name not set");
      }
      if (DebugFile.trace) DebugFile.writeln("new SilentAuthenticator("+sInAccountName+", ...)");
      SilentAuthenticator oAuth = new SilentAuthenticator(sInAccountName, sInAuthStr);
      if (DebugFile.trace) DebugFile.writeln("Session.getInstance([Properties],[SilentAuthenticator])");
      Properties oSessionProperties = new Properties();
      for (Map.Entry<String,String> oEntry : oProps.entrySet())
    	  oSessionProperties.setProperty(oEntry.getKey(), oEntry.getValue());
      oMailSession = Session.getInstance(oSessionProperties, oAuth);
    }
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.getSession() : " + oMailSession);
    }
    return oMailSession;
  } // getSession

  // ---------------------------------------------------------------------------

  /**
   * <p>Get outgoing mail server Session</p>
   * This method calls JavaMail Session.getInstance() method if neccesary,
   * using properties currently set at this instance and SilentAuthenticator as
   * Authenticator subclass
   * @return javax.mail.Session
   * @throws IllegalStateException
   * @throws NullPointerException
   */
  public Session getSmtpSession() throws IllegalStateException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.getSmtpSession()");
      DebugFile.incIdent();
    }
    if (null==oSmtpSession) {
      if (null==oProps) {
        if (DebugFile.trace) DebugFile.decIdent();
        throw new IllegalStateException("SessionHandler.getSmtpSession() properties not set");
      }
      if (null==sOutAccountName) {
        sOutAccountName = "";
        if (DebugFile.trace) DebugFile.writeln("SessionHandler.getSmtpSession() smtp account name not set");
      }
      Properties oSessionProperties = new Properties();
      for (Map.Entry<String,String> oEntry : oProps.entrySet())
    	  oSessionProperties.setProperty(oEntry.getKey(), oEntry.getValue());
      if (sOutAccountName.trim().length()==0) {
        if (DebugFile.trace) DebugFile.writeln("Session.getInstance([Properties])");
        oSmtpSession = Session.getInstance(oSessionProperties);
      } else {
        if (DebugFile.trace) DebugFile.writeln("new SilentAuthenticator("+sOutAccountName+",...)");
        SilentAuthenticator oAuth = new SilentAuthenticator(sOutAccountName, sOutAuthStr);
        if (DebugFile.trace) DebugFile.writeln("Session.getInstance("+oProps+","+oAuth+")");
        oSmtpSession = Session.getInstance(oSessionProperties, oAuth);
      } // fi
    }
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.getSmtpSession() : " + oSmtpSession);
    }
    return oSmtpSession;
  } // getSmtpSession

  // ---------------------------------------------------------------------------

  /**
   * <p>Get Store</p>
   * This method calls Session.getStore() and Store.connect() if neccesary.
   * @return javax.mail.Store
   * @throws NoSuchProviderException
   * @throws MessagingException
   */
  public Store getStore() throws NoSuchProviderException, MessagingException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.getStore()");
      DebugFile.incIdent();
    }
    if (null==oMailStore) {
      if (null==sInHostName) {
        if (DebugFile.trace) DebugFile.decIdent();
        throw new NullPointerException("SessionHandler host name not set");
      }
      if (DebugFile.trace) DebugFile.writeln("Session.getStore()");
      oMailStore = getSession().getStore();
      if (DebugFile.trace) DebugFile.writeln("Store.connect("+sInHostName+","+sInAccountName+", ...)");
      getStore().connect(sInHostName, sInAccountName, sInAuthStr);
      bIsStoreConnected = true;
    }
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.getStore() : " + oMailStore);
    }
    return oMailStore;
  } // getStore()

  // ---------------------------------------------------------------------------

  /**
   * <p>Get Transport</p>
   * This method calls Session.getTransport() and Transport.connect() if neccesary
   * @return javax.mail.Transport
   * @throws NoSuchProviderException
   * @throws MessagingException
   */
  public Transport getTransport()
    throws NoSuchProviderException,MessagingException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.getTransport()");
      DebugFile.incIdent();
    }
    if (null==oMailTransport) {
      if (DebugFile.trace) DebugFile.writeln("Session.getTransport()");
      oMailTransport = getSmtpSession().getTransport();
      oMailTransport.connect();
    }
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.getTransport() : " + oMailTransport);
    }
    return oMailTransport;
  } // getTransport

  // ---------------------------------------------------------------------------

  /**
   * Get folder from current mail store
   * @return javax.mail.Folder
   * @throws NoSuchProviderException
   * @throws MessagingException
   */
  public Folder getFolder(String sFolderName)
      throws NoSuchProviderException,MessagingException {
    getStore();
    if (null==oMailStore)
      return null;
    else {
      return oMailStore.getFolder(sFolderName);
    }
  } // getFolder

  // ---------------------------------------------------------------------------

  public boolean isStoreConnected() {
    return bIsStoreConnected;
  }

  // ---------------------------------------------------------------------------

  public boolean isTransportConnected() {
    return bIsTransportConnected;
  }

  // ---------------------------------------------------------------------------

  public void sendMessage(Message oMsg)
    throws NoSuchProviderException,SendFailedException,ParseException,
           MessagingException,NullPointerException,IllegalStateException {

    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.sendMessage([Message])");
      DebugFile.incIdent();
    }
    
    oMsg.setSentDate(new java.util.Date());
    
    Transport.send(oMsg);
    
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.sendMessage()");
    }
  } // sendMessage

  // ---------------------------------------------------------------------------

  /**
   * <p>Get a list of all folder messages which are not deleted</p>
   * Messages are returned in ascending date order, oldest messages are returned first
   * @param sFolderName Folder Name, for example: "INBOX"
   * @return An array of strings with format
   * &lt;msg&gt;
   * &lt;num&gt;[1..n]&lt;/num&gt;
   * &lt;id&gt;message unique identifier&lt;/id&gt;
   * &lt;type&gt;message content-type&lt;/type&gt;
   * &lt;disposition&gt;message content-disposition&lt;/disposition&gt;
   * &lt;len&gt;message length in bytes&lt;/len&gt;
   * &lt;priority&gt;X-Priority header&lt;/priority&gt;
   * &lt;spam&gt;X-Spam-Flag header&lt;/spam&gt;
   * &lt;subject&gt;&lt;![CDATA[message subject]]&gt;&lt;/subject&gt;
   * &lt;sent&gt;yyy-mm-dd hh:mi:ss&lt;/sent&gt;
   * &lt;received&gt;yyy-mm-dd hh:mi:ss&lt;/received&gt;
   * &lt;from&gt;&lt;![CDATA[personal name of sender]]&gt;&lt;/from&gt;
   * &lt;to&gt;&lt;![CDATA[personal name or e-mail of receiver]]&gt;&lt;/to&gt;
   * &lt;size&gt;integer size in kilobytes&lt;/size&gt;
   * &lt;err&gt;error description (if any)&lt;/err&gt;
   * &lt;/msg&gt;
   * @throws AuthenticationFailedException
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @since 4.0
   */

  public String[] listFolderMessages(String sFolderName)
  	throws AuthenticationFailedException,NoSuchProviderException,MessagingException {

	Chronometer oChMeter = null;

    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.listFolderMessages("+sFolderName+")");
      DebugFile.incIdent();
      oChMeter = new Chronometer();
    }

    MailHeadersHelper oHlpr = new MailHeadersHelper();
    String [] aMsgsXml = null;

    if (DebugFile.trace) DebugFile.writeln("getting "+sFolderName+" folder");

    Folder oFldr = getFolder(sFolderName);

    if (DebugFile.trace) DebugFile.writeln("opening "+sFolderName+" folder");
    
	oFldr.open (Folder.READ_ONLY);

    if (DebugFile.trace) DebugFile.writeln(sFolderName+" opened in read only mode");
	
	Message[] aMsgsObj = oFldr.getMessages();

	int iTotalCount = 0;
	if (null!=aMsgsObj) iTotalCount = aMsgsObj.length;

	int iDeleted = 0;

	if (iTotalCount>0) {

      if (DebugFile.trace) DebugFile.writeln("Folder.getMessages("+String.valueOf(iTotalCount)+")");

      FetchProfile oFtchPrfl = new FetchProfile();
      oFtchPrfl.add(FetchProfile.Item.ENVELOPE);
      oFtchPrfl.add(FetchProfile.Item.CONTENT_INFO);
      oFtchPrfl.add(FetchProfile.Item.FLAGS);
      oFtchPrfl.add("X-Priority");
      oFtchPrfl.add("X-Spam-Flag");

      if (DebugFile.trace) {
      	DebugFile.writeln("Folder.fetch(Message[], ENVELOPE & CONTENT_INFO & FLAGS)");
        oChMeter.start();
      }

      oFldr.fetch(aMsgsObj, oFtchPrfl);
      
      if (DebugFile.trace) {
      	DebugFile.writeln(String.valueOf(iTotalCount)+" headers fetched in "+String.valueOf(oChMeter.stop()/1000l)+" seconds");
        oChMeter.start();
      }
      	
      aMsgsXml = new String[iTotalCount];
      for (int m=0; m<iTotalCount; m++) {
        if (aMsgsObj[m].isSet(Flags.Flag.DELETED)) {
          iDeleted++;
        } else {
          oHlpr.setMessage((MimeMessage) aMsgsObj[m]);
          aMsgsXml[m-iDeleted] = oHlpr.toXML();
        } // fi
      } // next (m)
      
      aMsgsObj = null;

	  if (iDeleted>0) aMsgsXml = Arrays.copyOfRange(aMsgsXml, 0, iTotalCount-iDeleted);

      if (DebugFile.trace) {
      	DebugFile.writeln(String.valueOf(iTotalCount-iDeleted)+" messages to XML in "+String.valueOf(oChMeter.stop())+" ms");
      }

	} else {
	  if (DebugFile.trace)
        DebugFile.writeln("No messages found at folder "+sFolderName);		
	}// fi (iTotalCount>0)

	oFldr.close(false);

    if (DebugFile.trace) {
      DebugFile.writeln(String.valueOf(iTotalCount)+" messages fetched in "+String.valueOf(oChMeter.stop()/1000l)+" seconds");
      DebugFile.decIdent();
      if (null==aMsgsXml)
        DebugFile.writeln("End SessionHandler.listFolderMessages() : 0");
      else
        DebugFile.writeln("End SessionHandler.listFolderMessages() : " + String.valueOf(aMsgsXml.length));
    }

	
	return aMsgsXml;
  } // listFolderMessages

  // ---------------------------------------------------------------------------

  /**
   * <p>Get a list of headers for all folder messages which are not deleted</p>
   * Messages are returned in ascending date order, oldest messages are returned first
   * @param sFolderName Folder Name, for example: "INBOX"
   * @return An array of HeadersHelper objetcs
   * @throws AuthenticationFailedException
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @since 7.0
   */

  public MailHeadersHelper[] listFolderMailMessagesHeaders(String sFolderName)
  	throws AuthenticationFailedException,NoSuchProviderException,MessagingException {

	Chronometer oChMeter = null;

    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.listFolderMessagesHeaders("+sFolderName+")");
      DebugFile.incIdent();
      oChMeter = new Chronometer();
    }

    MailHeadersHelper[] aMsgsHdr = null;

    Folder oFldr = getFolder(sFolderName);

	oFldr.open (Folder.READ_ONLY);

	int iDeleted = 0;
	int iTotalCount = oFldr.getMessageCount();

	if (iTotalCount>0) {

      if (DebugFile.trace) DebugFile.writeln("Folder.getMessages("+String.valueOf(iTotalCount)+")");

      Message[] aMsgsObj = oFldr.getMessages();

      FetchProfile oFtchPrfl = new FetchProfile();
      oFtchPrfl.add(FetchProfile.Item.ENVELOPE);
      oFtchPrfl.add(FetchProfile.Item.CONTENT_INFO);
      oFtchPrfl.add(FetchProfile.Item.FLAGS);
      oFtchPrfl.add("X-Priority");
      oFtchPrfl.add("X-Spam-Flag");

      if (DebugFile.trace) {
      	DebugFile.writeln("Folder.fetch(Message[], ENVELOPE & CONTENT_INFO & FLAGS)");
        oChMeter.start();
      }

      oFldr.fetch(aMsgsObj, oFtchPrfl);
      
      if (DebugFile.trace) {
      	DebugFile.writeln(String.valueOf(iTotalCount)+" headers fetched in "+String.valueOf(oChMeter.stop()/1000l)+" seconds");
        oChMeter.start();
      }
      	
      aMsgsHdr = new MailHeadersHelper[iTotalCount];
      for (int m=0; m<iTotalCount; m++) {
        if (aMsgsObj[m].isSet(Flags.Flag.DELETED)) {
          iDeleted++;
        } else {
          aMsgsHdr[m-iDeleted] = new MailHeadersHelper((MimeMessage) aMsgsObj[m]);
        } // fi
      } // next (m)
      
      aMsgsObj = null;

	  if (iDeleted>0) aMsgsHdr = Arrays.copyOfRange(aMsgsHdr, 0, iTotalCount-iDeleted);

	} else {
	  if (DebugFile.trace) DebugFile.writeln("No message headers found at folder "+sFolderName);
	}// fi (iTotalCount>0)

	oFldr.close(false);

    if (DebugFile.trace) {
      DebugFile.decIdent();
      if (null==aMsgsHdr)
        DebugFile.writeln("End SessionHandler.listFolderMessagesHeaders() : 0");
      else
        DebugFile.writeln("End SessionHandler.listFolderMessagesHeaders() : " + String.valueOf(aMsgsHdr.length));
    }

	return aMsgsHdr;
  } // listFolderMessagesHeaders

  // ---------------------------------------------------------------------------

  /**
   * <p>Get a list of most recent folder messages which are not deleted, answered or marked as spam</p>
   * Messages are returned in descending date order, most recent messages are returned first
   * @param sFolderName Folder Name, for example: "INBOX"
   * @param iMaxMsgs Maximum number of messages to get [1..2^31-1]
   * @return An array of strings with format
   * &lt;msg&gt;
   * &lt;num&gt;[1..n]&lt;/num&gt;
   * &lt;id&gt;message unique identifier&lt;/id&gt;
   * &lt;type&gt;message content type&lt;/type&gt;
   * &lt;disposition&gt;message content-disposition&lt;/disposition&gt;
   * &lt;len&gt;message length in bytes&lt;/len&gt;
   * &lt;priority&gt;X-Priority header&lt;/priority&gt;
   * &lt;spam&gt;&lt;/spam&gt;
   * &lt;subject&gt;&lt;![CDATA[message subject]]&gt;&lt;/subject&gt;
   * &lt;sent&gt;yyy-mm-dd hh:mi:ss&lt;/sent&gt;
   * &lt;received&gt;yyy-mm-dd hh:mi:ss&lt;/received&gt;
   * &lt;from&gt;&lt;![CDATA[personal name of sender]]&gt;&lt;/from&gt;
   * &lt;to&gt;&lt;![CDATA[personal name or e-mail of receiver]]&gt;&lt;/to&gt;
   * &lt;size&gt;integer size in kilobytes&lt;/size&gt;
   * &lt;err&gt;error description (if any)&lt;/err&gt;
   * &lt;/msg&gt;
   * @throws AuthenticationFailedException
   * @throws NoSuchProviderException
   * @throws MessagingException
   * @since 4.0
   */

  public String[] listRecentMessages(String sFolderName, int iMaxMsgs)
  	throws AuthenticationFailedException,NoSuchProviderException,MessagingException,
  	       IllegalArgumentException {

	if (iMaxMsgs<0 || iMaxMsgs>2147483647) throw new IllegalArgumentException("SessionHandler.listRecentMessages() Max messages must be between 0 and 2147483647");
		
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.listFolderMessages("+sFolderName+","+String.valueOf(iMaxMsgs)+")");
      DebugFile.incIdent();
    }

    MailHeadersHelper oHlpr = new MailHeadersHelper();
    String [] aMsgsXml = null;

    Folder oFldr = getFolder(sFolderName);

	oFldr.open (Folder.READ_ONLY);

	int iTotalCount = oFldr.getMessageCount();

    if (DebugFile.trace) DebugFile.writeln(String.valueOf(iTotalCount)+" messages found at "+sFolderName);

	if (iTotalCount>0) {
	  LinkedList<String> oList = new LinkedList<String>();
	  int iLowerBound = iTotalCount-iMaxMsgs;
	  if (iLowerBound<0) iLowerBound = 0;
	  for (int m=iTotalCount-1; m>=iLowerBound && oList.size()<iMaxMsgs; m--) {
	    if (DebugFile.trace) DebugFile.writeln("getting message "+String.valueOf(m));
	    try {
	      Message oMsgObj = oFldr.getMessage(m);

          if (!oMsgObj.isSet(Flags.Flag.DELETED) && !oMsgObj.isSet(Flags.Flag.ANSWERED)) {
            String sSpamFlag = ((MimeMessage) oMsgObj).getHeader("X-Spam-Flag","");
            if (sSpamFlag==null) sSpamFlag = "";
            if (!sSpamFlag.equalsIgnoreCase("YES")) {
              oHlpr.setMessage((MimeMessage) oMsgObj);
              String sMsgXML = oHlpr.toXML();
		      oList.add(sMsgXML);
            }
          } // fi
	    } catch (ArrayIndexOutOfBoundsException aiob) {
		  if (DebugFile.trace) DebugFile.writeln("Folder.getMessage("+String.valueOf(m)+") ArrayIndexOutOfBoundsException");
	    }
	  } // next (m)
	  if (oList.size()>0) {
	    aMsgsXml = new String [oList.size()];
	    aMsgsXml = oList.toArray(aMsgsXml);
	  }
	} // fi (iTotalCount>0)
    oFldr.close(false);

    if (DebugFile.trace) {
      DebugFile.decIdent();
      if (null==aMsgsXml)
        DebugFile.writeln("End SessionHandler.listFolderMessages() : 0");
      else
        DebugFile.writeln("End SessionHandler.listFolderMessages() : " + String.valueOf(aMsgsXml.length));
    }
	return aMsgsXml;
  } // listFolderMessages

  // ---------------------------------------------------------------------------

  public void sendMessage(Message oMsg, Address[] aAddrs)
    throws NoSuchProviderException,SendFailedException,ParseException,
           MessagingException,NullPointerException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.sendMessage([Message],Address[])");
      DebugFile.incIdent();
    }
    oMsg.setSentDate(new java.util.Date());
    if (DebugFile.trace) DebugFile.writeln("Transport.send(Message,Address[])");
    Transport.send(oMsg,aAddrs);
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.sendMessage()");
    }
  } // sendMessage

  // ---------------------------------------------------------------------------

  public void sendMessage (Message oMsg,
                           Address[] aAdrFrom, Address[] aAdrReply,
                           Address[] aAdrTo, Address[] aAdrCc, Address[] aAdrBcc)
    throws NoSuchProviderException,SendFailedException,ParseException,
           MessagingException,NullPointerException {
       	
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.sendMessage([Message],Address[],Address[],Address[],Address[],Address[])");
      DebugFile.incIdent();
      if (aAdrFrom!=null)
      	for (int f=0; f<aAdrFrom.length; f++) DebugFile.writeln("from "+aAdrFrom[f].toString());
      if (aAdrReply!=null)
      	for (int r=0; r<aAdrReply.length; r++) DebugFile.writeln("reply "+aAdrReply[r].toString());
      if (aAdrTo!=null)
      	for (int t=0; t<aAdrTo.length; t++) DebugFile.writeln("to "+aAdrTo[t].toString());
      if (aAdrCc!=null)
      	for (int c=0; c<aAdrCc.length; c++) DebugFile.writeln("cc "+aAdrCc[c].toString());
      if (aAdrBcc!=null)
      	for (int b=0; b<aAdrBcc.length; b++) DebugFile.writeln("bcc "+aAdrBcc[b].toString());
    }
    oMsg.addFrom(aAdrFrom);
    if (null==aAdrReply)
      oMsg.setReplyTo(aAdrReply);
    else
      oMsg.setReplyTo(aAdrFrom);
    if (aAdrTo!=null) oMsg.addRecipients(javax.mail.Message.RecipientType.TO, aAdrTo);
    if (aAdrCc!=null) oMsg.addRecipients(javax.mail.Message.RecipientType.CC, aAdrCc);
    if (aAdrBcc!=null) oMsg.addRecipients(javax.mail.Message.RecipientType.BCC, aAdrBcc);
    oMsg.setSentDate(new java.util.Date());
    if (DebugFile.trace) DebugFile.writeln("Transport.send(Message)");
    Transport.send(oMsg);
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.sendMessage()");
    }
  } // sendMessage

  // ---------------------------------------------------------------------------

  public void close()
    throws MessagingException {
    if (DebugFile.trace) {
      DebugFile.writeln("Begin SessionHandler.close()");
      DebugFile.incIdent();
    }
    if (null!=oMailStore) {
      if (isStoreConnected()) {
        if (DebugFile.trace) DebugFile.writeln("Store.close()");
        oMailStore.close();
      }
      oMailStore = null;
    }
    if (null!=oMailTransport) {
      if (isTransportConnected()) {
        if (DebugFile.trace) DebugFile.writeln("Transport.close()");
        oMailTransport.close();
      }
      oMailTransport=null;
    }
    oMailSession=null;
    oSmtpSession=null;
    if (DebugFile.trace) {
      DebugFile.decIdent();
      DebugFile.writeln("End SessionHandler.close()");
    }
  } // close

  // ---------------------------------------------------------------------------
  
  private SMTPMessage composeMessage(String sSubject, String sEncoding,
		                             String sTextBody, String sHtmlBody,
		                             String sId, String [] aAttachmentsPath,
		                             String sBasePath, boolean bInlineImages)
  throws IOException,MessagingException,IllegalArgumentException,SecurityException {
  
  String sContentType = (sHtmlBody==null ? "plain" : "html");
  
  if (DebugFile.trace) {
    DebugFile.writeln("Begin SessionHandler.composeMessage("+sSubject+","+sEncoding+",...,"+sId+","+sContentType+","+sBasePath+","+String.valueOf(bInlineImages)+")");
    DebugFile.incIdent();
  }

  if (sEncoding==null) sEncoding = "ASCII";
  String sCharEnc = Charset.forName(sEncoding).name();

  Session oSes = getSmtpSession();
  
  if (DebugFile.trace) DebugFile.writeln("new SMTPMessage(Session)");
  
  SMTPMessage oSentMessage = new SMTPMessage(oSes);

  MimeBodyPart oMsgPlainText = new MimeBodyPart();
  MimeMultipart oSentMsgParts = new MimeMultipart("mixed");

  if (sContentType.equalsIgnoreCase("html")) {

    MimeMultipart oHtmlRelated  = new MimeMultipart("related");
    MimeMultipart oTextHtmlAlt  = new MimeMultipart("alternative");

    // ************************************************************************
    // Replace image CIDs

    HashMap<String,String> oDocumentImages = new HashMap<String,String>(23);

    Parser oPrsr = Parser.createParser(sHtmlBody, sEncoding);

    String sCid, sSrc;

    try {

      if (sTextBody==null) {
          // ****************************
          // Extract plain text from HTML
          if (DebugFile.trace) DebugFile.writeln("new StringBean()");

          StringBean oStrBn = new StringBean();

          try {
            oPrsr.visitAllNodesWith (oStrBn);
          } catch (ParserException pe) {
          throw new MessagingException(pe.getMessage(), pe);
          }

          sTextBody = oStrBn.getStrings();

          oStrBn = null;
      } // fi (sTextBody==null)

      // *******************************
      // Set plain text alternative part

      oMsgPlainText.setDisposition("inline");
      oMsgPlainText.setText(sTextBody, sCharEnc, "plain");
      if (DebugFile.trace) DebugFile.writeln("MimeBodyPart(multipart/alternative).addBodyPart(text/plain)");
      oTextHtmlAlt.addBodyPart(oMsgPlainText);

      // *****************************************
      // Iterate images from HTML and replace CIDs

      if (bInlineImages) {
        NodeList oCollectionList = new NodeList();
        TagNameFilter oImgFilter = new TagNameFilter ("IMG");
        for (NodeIterator e = oPrsr.elements(); e.hasMoreNodes();)
          e.nextNode().collectInto(oCollectionList, oImgFilter);

        final int nImgs = oCollectionList.size();

        if (DebugFile.trace) DebugFile.writeln("NodeList.size() = " + String.valueOf(nImgs));

        for (int i=0; i<nImgs; i++) {

          sSrc = ((ImageTag) oCollectionList.elementAt(i)).extractImageLocn();

          // Keep a reference to every related image name so that the same image is not included twice in the message
          if (!oDocumentImages.containsKey(sSrc)) {

            // Find last slash from image url
            int iSlash = sSrc.lastIndexOf('/');

            // Take image name
            if (iSlash>=0) {
              while (sSrc.charAt(iSlash)=='/') { if (++iSlash==sSrc.length()) break; }
              sCid = sSrc.substring(iSlash);
            }
            else {
              sCid = sSrc;
            }

            if (DebugFile.trace) DebugFile.writeln("HashMap.put("+sSrc+","+sCid+")");

            oDocumentImages.put(sSrc, sCid);
          } // fi (!oDocumentImages.containsKey(sSrc))

          if (DebugFile.trace) DebugFile.writeln("Util.substitute([PatternMatcher],"+ sSrc + ",cid:"+oDocumentImages.get(sSrc)+",...)");
          sHtmlBody = sHtmlBody.replaceAll(sSrc, "cid:"+oDocumentImages.get(sSrc));

        } // next      	
      } // fi (bInlineImages)
    }
    catch (ParserException pe) {
      if (DebugFile.trace) {
        DebugFile.writeln("org.htmlparser.util.ParserException " + pe.getMessage());
      }
    }
    // End replace image CIDs
    // ************************************************************************

    // ************************************************************************
    // Add HTML related images

    if (oDocumentImages.isEmpty()) {
      
    	// Set HTML part
      MimeBodyPart oMsgHtml = new MimeBodyPart();
      oMsgHtml.setDisposition("inline");
      oMsgHtml.setText(sHtmlBody, sCharEnc, "html");
      oTextHtmlAlt.addBodyPart(oMsgHtml);

    } else {

      // Set HTML text related part

      MimeBodyPart oMsgHtmlText = new MimeBodyPart();
      oMsgHtmlText.setDisposition("inline");
      oMsgHtmlText.setText(sHtmlBody, sCharEnc, "html");
      if (DebugFile.trace) DebugFile.writeln("MimeBodyPart(multipart/related).addBodyPart(text/html)");
      oHtmlRelated.addBodyPart(oMsgHtmlText);

      // Set HTML text related inline images

      Iterator oImgs = oDocumentImages.keySet().iterator();

      while (oImgs.hasNext()) {
        BodyPart oImgBodyPart = new MimeBodyPart();

        sSrc = (String) oImgs.next();
        sCid = (String) oDocumentImages.get(sSrc);

        if (sSrc.startsWith("www."))
          sSrc = "http://" + sSrc;

        if (sSrc.startsWith("http://") || sSrc.startsWith("https://")) {
          oImgBodyPart.setDataHandler(new DataHandler(new URL(sSrc)));
        }
        else {
          oImgBodyPart.setDataHandler(new DataHandler(new FileDataSource((sBasePath==null ? "" : sBasePath)+sSrc)));
        }

        oImgBodyPart.setDisposition("inline");
        oImgBodyPart.setHeader("Content-ID", sCid);
        oImgBodyPart.setFileName(sCid);

        // Add image to multi-part
        if (DebugFile.trace) DebugFile.writeln("MimeBodyPart(multipart/related).addBodyPart("+sCid+")");
        oHtmlRelated.addBodyPart(oImgBodyPart);
      } // wend

      // Set html text alternative part (html text + inline images)
      MimeBodyPart oTextHtmlRelated = new MimeBodyPart();
      oTextHtmlRelated.setContent(oHtmlRelated);
      if (DebugFile.trace) DebugFile.writeln("MimeBodyPart(multipart/alternative).addBodyPart(multipart/related)");
      oTextHtmlAlt.addBodyPart(oTextHtmlRelated);
    }

    // ************************************************************************
    // Create message to be sent and add main text body to it

    if (aAttachmentsPath==null) {
      oSentMessage.setContent(oTextHtmlAlt);
    } else {
      MimeBodyPart oMixedPart = new MimeBodyPart();
      oMixedPart.setContent(oTextHtmlAlt);
      oSentMsgParts.addBodyPart(oMixedPart);
    }

  } else { // (sContentType=="plain")

    // *************************************************
    // If this is a plain text message just add the text

    if (aAttachmentsPath==null) {
      oSentMessage.setText(sTextBody, sCharEnc);
    } else {
      oMsgPlainText.setDisposition("inline");
      oMsgPlainText.setText(sTextBody, sCharEnc, "plain");
      //oMsgPlainText.setContent(sTextBody, "text/plain; charset="+sCharEnc);
      if (DebugFile.trace) DebugFile.writeln("MimeBodyPart(multipart/mixed).addBodyPart(text/plain)");
      oSentMsgParts.addBodyPart(oMsgPlainText);
    }
  }
  // fi (sContentType=="html")

  // ************************************************************************
  // Add attachments to message to be sent

  if (aAttachmentsPath!=null) {
    final int nAttachments = aAttachmentsPath.length;

    for (int p=0; p<nAttachments; p++) {
      String sFilePath = aAttachmentsPath[p];
      if (sBasePath!=null) {
        if (!sFilePath.startsWith(sBasePath))
          sFilePath = sBasePath + sFilePath;
      }
      File oFile = new File(sFilePath);
      
      MimeBodyPart oAttachment = new MimeBodyPart();
      oAttachment.setDisposition("attachment");
      oAttachment.setFileName(oFile.getName());
      oAttachment.setHeader("Content-Transfer-Encoding", "base64");

      ByteArrayDataSource oDataSrc;
      oDataSrc = new ByteArrayDataSource(Files.readAllBytes(Paths.get(sFilePath)), "application/octet-stream");
      oAttachment.setDataHandler(new DataHandler(oDataSrc));
      oSentMsgParts.addBodyPart(oAttachment);
    } // next
    oSentMessage.setContent(oSentMsgParts);
  } // fi (iDraftParts>0)

  if (null!=sSubject) oSentMessage.setSubject(sSubject);

  if (sId!=null)
    if (sId.trim().length()>0)
      oSentMessage.setContentID(sId);

  if (DebugFile.trace) {
    DebugFile.decIdent();
    DebugFile.writeln("End SessionHandler.composeMessage()");
  }

  return oSentMessage;
  } // composeMessage
     
  // ---------------------------------------------------------------------------

  /**
   * <p>Send e-mail message</p>
   * @param sSubject String e-mail Subject
   * @param sFromPersonal String Sender display name
   * @param sFromAddr String Sender e-mail address
   * @param sReplyAddr String Reply-To e-mail address
   * @param aRecipients Array of recipients e-mail addresses
   * @param aRecType Array of types for each recipient {to, cc, bcc}
   * @param sTextBody String Plain Text Message Body
   * @param sHtmlBody String HTML Text Message Body
   * @param sEncoding Character Encoding to be used
   * @param sId String Message Unique Id. Optional, may be null.
   * @param aAttachmentsPath Array of relative paths to files to be attached
   * @param sUserDir Base path for attached files
   * @param bInlineImages <b>true</b> is images will be inlined as attachments, false if they will be downloaded upon message opening
   * @param oOut PrintStream Output stream for messages verbose
   * @throws NullPointerException
   * @throws IOException
   * @throws MessagingException
   * @throws IllegalArgumentException
   * @throws SecurityException
   * @since 8.0
   */
  public int sendMessage(String sSubject, String sFromPersonal, String sFromAddr, String sReplyAddr,
		                 String[] aRecipients, RecipientType[] aRecType,
		                 String sTextBody, String sHtmlBody, String sEncoding, String sId,
                         String [] aAttachmentsPath, String sUserDir, boolean bInlineImages, PrintStream oOut )
    throws NullPointerException,IOException,MessagingException,IllegalArgumentException,SecurityException {

	  if (sFromAddr==null) throw new NullPointerException("SessionHandler.sendMessage sender address cannot be null");
	  if (aRecipients==null) throw new NullPointerException("SessionHandler.sendMessage repients list cannot be null");

	  if (DebugFile.trace) {
	    String sRecipientsList = "{"+String.join(";", aRecipients)+"}";
	    String sAttachementsList;
	    if (aAttachmentsPath==null)
        sAttachementsList = "";
	    else
		  sAttachementsList = "{"+String.join(";", aAttachmentsPath)+"}";
	    DebugFile.writeln("SessionHandler.sendMessage("+sSubject+","+sFromPersonal+","+sFromAddr+","+sReplyAddr+","+sRecipientsList+",...,...,...,"+sEncoding+","+sId+","+sAttachementsList+","+sUserDir+",[PrintStream])");
	    DebugFile.incIdent();
	  }

    boolean bHasReplacements = false;
    
    if (null!=sTextBody) bHasReplacements |= (Str.indexOfIgnoreCase(sTextBody, "{#Message.id}")>=0);
    if (null!=sHtmlBody) bHasReplacements |= (Str.indexOfIgnoreCase(sHtmlBody, "{#Message.id}")>=0);

    final int nRecipients = aRecipients.length;
	  if (DebugFile.trace) DebugFile.writeln("recipients count is "+String.valueOf(nRecipients));
    
    int nSend = 0;
    if (bHasReplacements) {
    	HashMap<String,String> oMap = new HashMap<String,String>(13);
    	oMap.put("Message.id", sId);
    	StringBuffer oTextBody = new StringBuffer(sTextBody);
    	StringBuffer oHtmlBody = new StringBuffer(sHtmlBody);
    	FastStreamReplacer oRpl = new FastStreamReplacer();
        for (int r=0; r<nRecipients; r++) {
          String sRecipientAddr = Str.removeChars(aRecipients[r], " \t\r\n");
          if (sRecipientAddr.length()>0) {
            try {
              String sUniqueId = sId+"."+String.valueOf(r+1);
              oMap.remove("Message.id");
          	  oMap.put("Message.id", sUniqueId);          	  
          	  MimeMessage oCurrentMsg = composeMessage(sSubject, sEncoding, oRpl.replace(oTextBody, oMap), oRpl.replace(oHtmlBody, oMap), sUniqueId, aAttachmentsPath, sUserDir, bInlineImages);
              oCurrentMsg.setFrom(new InternetAddress(sFromAddr, null==sFromPersonal ? sFromAddr : sFromPersonal));
              if (null!=sReplyAddr) oCurrentMsg.setReplyTo(new Address[]{new InternetAddress(sReplyAddr)});
              oCurrentMsg.setRecipient(aRecType[r], new InternetAddress(sRecipientAddr));      
              sendMessage(oCurrentMsg);
              oOut.println("OK "+sRecipientAddr);
              nSend++;
            } catch (Exception xcpt) {
          	  if (oOut==null) {
          	    if (DebugFile.trace) {
          	      DebugFile.writeln("ERROR "+aRecipients[r]+" "+xcpt.getClass().getName()+" "+xcpt.getMessage());
          	      try {
          	    	DebugFile.writeln(StackTraceUtil.getStackTrace(xcpt));
          	      } catch (IOException ignore) { }
          	    }
          	  } else {
          	    oOut.println("ERROR at SessionHandler.sendMessage() "+aRecipients[r]+" "+xcpt.getClass().getName()+" "+xcpt.getMessage());
              } // fi (oOut)
          	}
          } // fi (sRecipientAddr!="")
        } // next    	
    } else {
        for (int r=0; r<nRecipients; r++) {
          String sRecipientAddr = Str.removeChars(aRecipients[r], " \t\r\n");
          if (sRecipientAddr.length()>0) {
          	MimeMessage oMasterMsg = composeMessage(sSubject, sEncoding, sTextBody, sHtmlBody, null, aAttachmentsPath, sUserDir, bInlineImages);
            oMasterMsg.setFrom(new InternetAddress(sFromAddr, null==sFromPersonal ? sFromAddr : sFromPersonal));
            if (null!=sReplyAddr) oMasterMsg.setReplyTo(new Address[]{new InternetAddress(sReplyAddr)});
            try {
              SMTPMessage oCurrentMsg = new SMTPMessage (oMasterMsg);
              oCurrentMsg.setContentID(sId+"."+String.valueOf(r+1));
              oCurrentMsg.setRecipient(aRecType[r], new InternetAddress(sRecipientAddr));      
              sendMessage(oCurrentMsg);
              if (oOut!=null) oOut.println("OK "+sRecipientAddr);
              nSend++;
            }
            catch (Exception xcpt) {
              String sCause = "";
              if (xcpt.getCause()!=null)
                sCause = " cause "+xcpt.getCause().getClass().getName()+" "+xcpt.getCause().getMessage();
          	  if (oOut==null) {
          	    if (DebugFile.trace) {
          	    	DebugFile.writeln("ERROR "+aRecipients[r]+" "+xcpt.getClass().getName()+" "+xcpt.getMessage()+sCause);
            	    try {
              	      DebugFile.writeln(StackTraceUtil.getStackTrace(xcpt));
              	    } catch (IOException ignore) { }
          	    }
          	  } else {
          	  	oOut.println("ERROR "+aRecipients[r]+" "+xcpt.getClass().getName()+" "+xcpt.getMessage()+sCause);
          	  }
            }
          } // fi (sRecipientAddr!="")
        } // next    	
    } // fi

    if (nSend==nRecipients) {
    	if (oOut!=null) oOut.println("Process successfully completed. "+String.valueOf(nSend)+" messages sent");
    } else {
    	if (oOut!=null) oOut.println("Process finished with errors. "+String.valueOf(nSend)+" messages successfully sent, "+String.valueOf(nRecipients-nSend)+" messages failed");
    }

	if (DebugFile.trace) {
	  DebugFile.decIdent();
	  DebugFile.writeln("End SessionHandler.sendMessage() : "+String.valueOf(nSend));
	}
    return nSend;
  } // sendMessage

  // ---------------------------------------------------------------------------

  /**
   * <p>Send e-mail message</p>
   * @param sSubject String e-mail Subject
   * @param sFromPersonal String Sender display name
   * @param sFromAddr String Sender e-mail address
   * @param sReplyAddr String Reply-To e-mail address
   * @param aRecipients Array of recipients e-mail addresses
   * @param aRecType Array of types for each recipient {to, cc, bcc}
   * @param sTextBody String Plain Text Message Body
   * @param sHtmlBody String HTML Text Message Body
   * @param sEncoding Character Encoding to be used
   * @param sId String Message Unique Id. Optional, may be null.
   * @param aAttachmentsPath Array of relative paths to files to be attached
   * @param sUserDir Base path for attached files
   * @param oOut PrintStream Output stream for messages verbose
   * @throws NullPointerException
   * @throws IOException
   * @throws MessagingException
   * @throws IllegalArgumentException
   * @throws SecurityException
   */
  public int sendMessage(String sSubject, String sFromPersonal, String sFromAddr, String sReplyAddr,
		                 String[] aRecipients, RecipientType[] aRecType,
		                 String sTextBody, String sHtmlBody, String sEncoding, String sId,
                         String [] aAttachmentsPath, String sUserDir, PrintStream oOut )
    throws NullPointerException,IOException,MessagingException,IllegalArgumentException,SecurityException {
    return sendMessage(sSubject, sFromPersonal, sFromAddr, sReplyAddr,
        							 aRecipients, aRecType, sTextBody, sHtmlBody, sEncoding, sId,
        							 aAttachmentsPath, sUserDir, true, oOut);
  }
  
  // -------------------------------------------------------------------

  public int sendMessage(String sSubject, String sFromPersonal, String sFromAddr, String sReplyAddr,
		                 String[] aRecipients, RecipientType oRecType,
		                 String sTextBody, String sHtmlBody, String sEncoding, String sId,
                         String [] aAttachmentsPath, String sUserDir, PrintStream oOut )
    throws NullPointerException,IOException,MessagingException,IllegalArgumentException,SecurityException {
	if (oRecType==null) oRecType = RecipientType.TO;
	RecipientType[] aRecTypes = new RecipientType[aRecipients.length];
	Arrays.fill(aRecTypes,oRecType);
    return sendMessage(sSubject, sFromPersonal, sFromAddr, sReplyAddr,
		               aRecipients, aRecTypes,
		               sTextBody, sHtmlBody, sEncoding, sId,
                       aAttachmentsPath, sUserDir, oOut);
  }

  /**
   * Checks to see if ports are available.
   */
  public boolean checkPorts() {

	  Socket sc = null;
      try {
          sc = new Socket();
          sc.connect(new InetSocketAddress(sOutHostName, iOutPortNum), 2000);
          return sc.isConnected();
      } catch (IOException e) {
    	if (DebugFile.trace) {
    	  DebugFile.writeln("MailSessionHandler.checkPorts() IOException "+e.getMessage());
    	}
      } finally {
          if (sc != null) {
        	  try {
                  sc.close();
              } catch (IOException neverthrown) { }
          }
      }
      return false;
  }  
} // SessionHandler
