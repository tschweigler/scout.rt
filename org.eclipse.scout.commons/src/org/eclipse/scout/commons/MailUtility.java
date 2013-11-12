/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.util.ByteArrayDataSource;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

@SuppressWarnings("restriction")
public final class MailUtility {

  public static final IScoutLogger LOG = ScoutLogManager.getLogger(MailUtility.class);

  private static final String CONTENT_TYPE_ID = "Content-Type";
  public static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=\"UTF-8\"";
  public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=\"UTF-8\"";
  public static final String CONTENT_TYPE_MESSAGE_RFC822 = "message/rfc822";
  public static final String CONTENT_TYPE_MULTIPART = "alternative";

  private static MailUtility instance = new MailUtility();

  private MailUtility() {
  }

  /**
   * Container for the mail body in plain text and html.
   * For the correct html representation it contains also a list of files referenced in the html.
   */
  public class MailMessage {

    private String m_plainText;
    private String m_htmlText;
    private List<File> m_htmlAttachmentList;

    public MailMessage(String plainText, String htmlText, List<File> htmlAttachmentList) {
      m_plainText = plainText;
      m_htmlText = htmlText;
      m_htmlAttachmentList = htmlAttachmentList;
    }

    public String getPlainText() {
      return m_plainText;
    }

    public String getHtmlText() {
      return m_htmlText;
    }

    public List<File> getHtmlAttachmentList() {
      return m_htmlAttachmentList;
    }
  }

  public static Part[] getBodyParts(Part message) throws ProcessingException {
    return instance.getBodyPartsImpl(message);
  }

  private Part[] getBodyPartsImpl(Part message) throws ProcessingException {
    List<Part> bodyCollector = new ArrayList<Part>();
    List<Part> attachementCollector = new ArrayList<Part>();
    collectMailPartsReqImpl(message, bodyCollector, attachementCollector);
    return bodyCollector.toArray(new Part[bodyCollector.size()]);
  }

  public static Part[] getAttachmentParts(Part message) throws ProcessingException {
    return instance.getAttachmentPartsImpl(message);
  }

  private Part[] getAttachmentPartsImpl(Part message) throws ProcessingException {
    List<Part> bodyCollector = new ArrayList<Part>();
    List<Part> attachementCollector = new ArrayList<Part>();
    collectMailPartsReqImpl(message, bodyCollector, attachementCollector);
    return attachementCollector.toArray(new Part[attachementCollector.size()]);
  }

  public static void collectMailParts(Part message, List<Part> bodyCollector, List<Part> attachementCollector) throws ProcessingException {
    instance.collectMailPartsReqImpl(message, bodyCollector, attachementCollector);
  }

  private void collectMailPartsReqImpl(Part part, List<Part> bodyCollector, List<Part> attachementCollector) throws ProcessingException {
    if (part == null) {
      return;
    }
    try {
      String disp = part.getDisposition();
      if (disp != null && disp.equalsIgnoreCase(Part.ATTACHMENT)) {
        attachementCollector.add(part);
      }
      else if (part.getContent() instanceof Multipart) {
        Multipart multiPart = (Multipart) part.getContent();
        for (int i = 0; i < multiPart.getCount(); i++) {
          collectMailPartsReqImpl(multiPart.getBodyPart(i), bodyCollector, attachementCollector);
        }
      }
      else {
        if (part.isMimeType(CONTENT_TYPE_TEXT_PLAIN)) {
          bodyCollector.add(part);
        }
        else if (part.isMimeType(CONTENT_TYPE_TEXT_HTML)) {
          bodyCollector.add(part);
        }
        else if (part.isMimeType(CONTENT_TYPE_MESSAGE_RFC822) && part.getContent() instanceof MimeMessage) {
          // its a MIME message in rfc822 format as attachment therefore we have to set the filename for the attachment correctly.
          MimeMessage msg = (MimeMessage) part.getContent();
          String filteredSubjectText = StringUtility.filterText(msg.getSubject(), "a-zA-Z0-9_-", "");
          String fileName = (StringUtility.hasText(filteredSubjectText) ? filteredSubjectText : "originalMessage") + ".eml";
          RFCWrapperPart wrapperPart = new RFCWrapperPart(part, fileName);
          attachementCollector.add(wrapperPart);
        }
      }
    }
    catch (ProcessingException e) {
      throw e;
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected: ", t);
    }
  }

  /**
   * @param part
   * @return the plainText part encoded with the encoding given in the MIME header or UTF-8 encoded or null if the
   *         plainText Part is not given
   * @throws ProcessingException
   */
  public static String getPlainText(Part part) throws ProcessingException {
    return instance.getPlainTextImpl(part);
  }

  private String getPlainTextImpl(Part part) throws ProcessingException {
    String text = null;
    try {
      Part[] bodyParts = getBodyPartsImpl(part);
      Part plainTextPart = getPlainTextPart(bodyParts);

      if (plainTextPart instanceof MimePart) {
        MimePart mimePart = (MimePart) plainTextPart;
        byte[] content = IOUtility.getContent(mimePart.getInputStream());
        if (content != null) {
          try {
            text = new String(content, getCharacterEncodingOfMimePart(mimePart));
          }
          catch (UnsupportedEncodingException e) {
            text = new String(content);
          }

        }
      }
    }
    catch (ProcessingException e) {
      throw e;
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected: ", t);
    }
    return text;
  }

  public static Part getHtmlPart(Part[] bodyParts) throws ProcessingException {
    for (Part p : bodyParts) {
      try {
        if (p != null && p.isMimeType(CONTENT_TYPE_TEXT_HTML)) {
          return p;
        }
      }
      catch (Throwable t) {
        throw new ProcessingException("Unexpected: ", t);
      }
    }
    return null;
  }

  public static Part getPlainTextPart(Part[] bodyParts) throws ProcessingException {
    for (Part p : bodyParts) {
      try {
        if (p != null && p.isMimeType(CONTENT_TYPE_TEXT_PLAIN)) {
          return p;
        }
      }
      catch (Throwable t) {
        throw new ProcessingException("Unexpected: ", t);
      }
    }
    return null;
  }

  public static DataSource createDataSource(File file) throws ProcessingException {
    try {
      int indexDot = file.getName().lastIndexOf(".");
      if (indexDot > 0) {
        String fileName = file.getName();
        String ext = fileName.substring(indexDot + 1);
        return instance.createDataSourceImpl(new FileInputStream(file), fileName, ext);
      }
      else {
        return null;
      }
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected: ", t);
    }
  }

  public static DataSource createDataSource(InputStream inStream, String fileName, String fileExtension) throws ProcessingException {
    return instance.createDataSourceImpl(inStream, fileName, fileExtension);
  }

  /**
   * @param inStream
   * @param fileName
   *          e.g. "file.txt"
   * @param fileExtension
   *          e.g. "txt", "jpg"
   * @return
   * @throws ProcessingException
   */
  private DataSource createDataSourceImpl(InputStream inStream, String fileName, String fileExtension) throws ProcessingException {
    try {
      ByteArrayDataSource item = new ByteArrayDataSource(inStream, getContentTypeForExtension(fileExtension));
      item.setName(fileName);
      return item;
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected: ", t);
    }
  }

  public static MailMessage extractMailMessageFromWordArchive(File archiveFile) {
    return instance.extractMailMessageFromWordArchiveInternal(archiveFile);
  }

  private MailMessage extractMailMessageFromWordArchiveInternal(File archiveFile) {
    MailMessage mailMessage = null;
    File tempDir = extractWordArchive(archiveFile);
    String simpleName = extractSimpleNameFromWordArchive(archiveFile);
    String messagePlainText = extractPlainTextFromWordArchiveInternal(tempDir, simpleName);
    String messageHtml = extractHtmlFromWordArchiveInternal(tempDir, simpleName);
    // replace directory entry
    // replace all paths to the 'files directory' with the root directory
    File attachmentFolder = null;
    if (tempDir.isDirectory()) {
      for (File file : tempDir.listFiles()) {
        if (file.isDirectory() && file.getName().startsWith(simpleName)) {
          attachmentFolder = file;
          break;
        }
      }
    }
    String folderName = null;
    if (attachmentFolder != null) {
      folderName = attachmentFolder.getName();
    }
    messageHtml = messageHtml.replaceAll(folderName + "/", "");
    messageHtml = removeWordTags(messageHtml);
    // now loop through the directory and search all the files needed for a correct representation of the html mail
    List<File> attachmentList = new ArrayList<File>();
    if (attachmentFolder != null) {
      for (File attFile : attachmentFolder.listFiles()) {
        // exclude Microsoft Word specific directory file. This is only used to edit HTML in Word.
        if (!attFile.isDirectory() && !isWordSpecificFile(attFile.getName())) {
          attachmentList.add(attFile);
        }
      }
    }
    mailMessage = new MailMessage(messagePlainText, messageHtml, attachmentList);
    return mailMessage;
  }

  private String extractHtmlFromWordArchiveInternal(File dir, String simpleName) {
    String txt = null;
    try {
      txt = extractTextFromWordArchiveInternal(dir, simpleName, "html");
    }
    catch (Exception e) {
      LOG.error("Error occured while trying to extract plain text file", e);
    }
    return txt;
  }

  private String extractSimpleNameFromWordArchive(File archiveFile) {
    String simpleName = archiveFile.getName();
    if (archiveFile.getName().lastIndexOf('.') != -1) {
      simpleName = archiveFile.getName().substring(0, archiveFile.getName().lastIndexOf('.'));
    }
    return simpleName;
  }

  private File extractWordArchive(File archiveFile) {
    File tempDir = null;
    try {
      tempDir = IOUtility.createTempDirectory("");
      FileUtility.extractArchive(archiveFile, tempDir);
    }
    catch (Exception e) {
      LOG.error("Error occured while trying to extract word archive", e);
    }
    return tempDir;
  }

  public static String extractPlainTextFromWordArchive(File archiveFile) {
    return instance.extractPlainTextFromWordArchiveInternal(archiveFile);
  }

  private String extractPlainTextFromWordArchiveInternal(File archiveFile) {
    File tempDir = extractWordArchive(archiveFile);
    String simpleName = extractSimpleNameFromWordArchive(archiveFile);
    return extractPlainTextFromWordArchiveInternal(tempDir, simpleName);
  }

  private String extractPlainTextFromWordArchiveInternal(File dir, String simpleName) {
    String plainText = null;
    try {
      plainText = extractTextFromWordArchiveInternal(dir, simpleName, "txt");
    }
    catch (Exception e) {
      LOG.error("Error occured while trying to extract plain text file", e);
    }
    return plainText;
  }

  private String extractTextFromWordArchiveInternal(File dir, String simpleName, String fileType) throws ProcessingException, IOException {
    String txt = null;
    File plainTextFile = new File(dir, simpleName + "." + fileType);
    if (plainTextFile.exists() && plainTextFile.canRead()) {
      txt = IOUtility.getContentInEncoding(plainTextFile.getPath(), "UTF-8");
    }
    return txt;
  }

  /**
   * Create {@link MimeMessage} from plain text fields.
   * 
   * @rn aho, 19.01.2009
   */
  public static MimeMessage createMimeMessage(String[] toRecipients, String sender, String subject, String bodyTextPlain, DataSource[] attachements) throws ProcessingException {
    return instance.createMimeMessageInternal(toRecipients, null, null, sender, subject, bodyTextPlain, attachements);
  }

  /**
   * Create {@link MimeMessage} from plain text fields.
   * 
   * @rn aho, 19.01.2009
   */
  public static MimeMessage createMimeMessage(String[] toRecipients, String[] ccRecipients, String[] bccRecipients, String sender, String subject, String bodyTextPlain, DataSource[] attachements) throws ProcessingException {
    return instance.createMimeMessageInternal(toRecipients, ccRecipients, bccRecipients, sender, subject, bodyTextPlain, attachements);
  }

  private MimeMessage createMimeMessageInternal(String[] toRecipients, String[] ccRecipients, String[] bccRecipients, String sender, String subject, String bodyTextPlain, DataSource[] attachements) throws ProcessingException {
    try {
      MimeMessage msg = MailUtility.createMimeMessage(bodyTextPlain, null, attachements);
      if (sender != null) {
        InternetAddress addrSender = new InternetAddress(sender);
        msg.setFrom(addrSender);
        msg.setSender(addrSender);
      }
      msg.setSentDate(new java.util.Date());
      //
      msg.setSubject(subject, "UTF-8");
      //
      msg.setRecipients(Message.RecipientType.TO, parseAddresses(toRecipients));
      msg.setRecipients(Message.RecipientType.CC, parseAddresses(ccRecipients));
      msg.setRecipients(Message.RecipientType.BCC, parseAddresses(bccRecipients));
      return msg;
    }
    catch (ProcessingException pe) {
      throw pe;
    }
    catch (Exception e) {
      throw new ProcessingException("Failed to create MimeMessage.", e);
    }
  }

  public static MimeMessage createMimeMessage(String messagePlain, String messageHtml, DataSource[] attachements) throws ProcessingException {
    return instance.createMimeMessageInternal(messagePlain, messageHtml, attachements);
  }

  public static MimeMessage createMimeMessageFromWordArchiveDirectory(File archiveDir, String simpleName, File[] attachments, boolean markAsUnsent) throws ProcessingException {
    return instance.createMimeMessageFromWordArchiveInternal(archiveDir, simpleName, attachments, markAsUnsent);
  }

  public static MimeMessage createMimeMessageFromWordArchive(File archiveFile, File[] attachments) throws ProcessingException {
    return createMimeMessageFromWordArchive(archiveFile, attachments, false);
  }

  public static MimeMessage createMimeMessageFromWordArchive(File archiveFile, File[] attachments, boolean markAsUnsent) throws ProcessingException {
    try {
      File tempDir = IOUtility.createTempDirectory("");
      FileUtility.extractArchive(archiveFile, tempDir);

      String simpleName = archiveFile.getName();
      if (archiveFile.getName().lastIndexOf('.') != -1) {
        simpleName = archiveFile.getName().substring(0, archiveFile.getName().lastIndexOf('.'));
      }
      return instance.createMimeMessageFromWordArchiveInternal(tempDir, simpleName, attachments, markAsUnsent);
    }
    catch (ProcessingException pe) {
      throw pe;
    }
    catch (IOException e) {
      throw new ProcessingException("Error occured while accessing files", e);
    }
  }

  private MimeMessage createMimeMessageFromWordArchiveInternal(File archiveDir, String simpleName, File[] attachments, boolean markAsUnsent) throws ProcessingException {
    try {
      File plainTextFile = new File(archiveDir, simpleName + ".txt");
      String plainTextMessage = null;
      boolean hasPlainText = false;
      if (plainTextFile.exists()) {
        plainTextMessage = IOUtility.getContentInEncoding(plainTextFile.getPath(), "UTF-8");
        hasPlainText = StringUtility.hasText(plainTextMessage);
      }

      String folderName = null;
      List<DataSource> htmlDataSourceList = new ArrayList<DataSource>();
      for (File filesFolder : archiveDir.listFiles()) {
        // in this archive file, exactly one directory should exist
        // word names this directory differently depending on the language
        if (filesFolder.isDirectory() && filesFolder.getName().startsWith(simpleName)) {
          // we accept the first directory that meets the constraint above
          // add all auxiliary files as attachment
          folderName = filesFolder.getName();
          for (File file : filesFolder.listFiles()) {
            // exclude Microsoft Word specific directory file. This is only used to edit HTML in Word.
            String filename = file.getName();
            if (!isWordSpecificFile(filename)) {
              FileDataSource fds = new FileDataSource(file);
              htmlDataSourceList.add(fds);
            }
          }
          break;
        }
      }

      File htmlFile = new File(archiveDir, simpleName + ".html");
      String htmlMessage = null;
      boolean hasHtml = false;
      if (htmlFile.exists()) {
        htmlMessage = IOUtility.getContentInEncoding(htmlFile.getPath(), "UTF-8");
        // replace directory entry
        // replace all paths to the 'files directory' with the root directory
        htmlMessage = htmlMessage.replaceAll("\"" + folderName + "/", "\"cid:");

        htmlMessage = removeWordTags(htmlMessage);
        // remove any VML elements
        htmlMessage = htmlMessage.replaceAll("<!--\\[if gte vml 1(.*\\r?\\n)*?.*?endif\\]-->", "");
        // remove any VML elements part2
        htmlMessage = Pattern.compile("<!\\[if !vml\\]>(.*?)<!\\[endif\\]>", Pattern.DOTALL).matcher(htmlMessage).replaceAll("$1");
        // remove not referenced attachments
        for (Iterator<DataSource> it = htmlDataSourceList.iterator(); it.hasNext();) {
          DataSource ds = it.next();
          if (!htmlMessage.contains("cid:" + ds.getName())) {
            it.remove();
          }
        }
        hasHtml = StringUtility.hasText(htmlMessage);
      }

      if (!hasPlainText && !hasHtml) {
        throw new ProcessingException("message has no body");
      }

      MimeMessage mimeMessage = new CharsetSafeMimeMessage();
      MimePart bodyPart = null;
      if (attachments != null && attachments.length > 0) {
        MimeMultipart multiPart = new MimeMultipart(); //mixed
        mimeMessage.setContent(multiPart);
        //add a holder for the body text
        MimeBodyPart multiPartBody = new MimeBodyPart();
        multiPart.addBodyPart(multiPartBody);
        bodyPart = multiPartBody;
        //add the attachments
        for (File attachment : attachments) {
          MimeBodyPart part = new MimeBodyPart();
          FileDataSource fds = new FileDataSource(attachment);
          DataHandler handler = new DataHandler(fds);
          part.setDataHandler(handler);
          part.setFileName(attachment.getName());
          multiPart.addBodyPart(part);
        }
      }
      else {
        //no attachments -> no need for multipart/mixed element
        bodyPart = mimeMessage;
      }

      if (hasPlainText && hasHtml) {
        MimeMultipart alternativePart = new MimeMultipart("alternative");
        bodyPart.setContent(alternativePart);
        MimeBodyPart plainBodyPart = new MimeBodyPart();
        alternativePart.addBodyPart(plainBodyPart);
        writePlainBody(plainBodyPart, plainTextMessage);
        MimeBodyPart htmlBodyPart = new MimeBodyPart();
        alternativePart.addBodyPart(htmlBodyPart);
        writeHtmlBody(htmlBodyPart, htmlMessage, htmlDataSourceList);
      }
      else if (hasPlainText) { //plain text only
        writePlainBody(bodyPart, plainTextMessage);
      }
      else { //html only
        writeHtmlBody(bodyPart, htmlMessage, htmlDataSourceList);
      }

      if (markAsUnsent) {
        mimeMessage.setHeader("X-Unsent", "1"); // only supported in Outlook 2010
      }
      return mimeMessage;
    }
    catch (MessagingException e) {
      throw new ProcessingException("Error occured while creating MIME-message", e);
    }
  }

  private String removeWordTags(String htmlMessage) {
    // remove special/unused files
    htmlMessage = htmlMessage.replaceAll("<link rel=File-List href=\"cid:filelist.xml\">", "");
    htmlMessage = htmlMessage.replaceAll("<link rel=colorSchemeMapping href=\"cid:colorschememapping.xml\">", "");
    htmlMessage = htmlMessage.replaceAll("<link rel=themeData href=\"cid:themedata.thmx\">", "");
    htmlMessage = htmlMessage.replaceAll("<link rel=Edit-Time-Data href=\"cid:editdata.mso\">", "");

    // remove Microsoft Word tags
    htmlMessage = htmlMessage.replaceAll("<!--\\[if gte mso(.*\\r?\\n)*?.*?endif\\]-->", "");

    return htmlMessage;
  }

  /**
   * Checks if file is a Microsoft Word specific directory file. They are only used to edit HTML in Word.
   */
  private boolean isWordSpecificFile(String filename) {
    return filename.equalsIgnoreCase("filelist.xml") ||
        filename.equalsIgnoreCase("colorschememapping.xml") ||
        filename.equalsIgnoreCase("themedata.thmx") ||
        filename.equalsIgnoreCase("header.html") ||
        filename.equalsIgnoreCase("editdata.mso") ||
        filename.matches("item\\d{4}\\.xml") ||
        filename.matches("props\\d{4}\\.xml");
  }

  private static void writeHtmlBody(MimePart htmlBodyPart, String htmlMessage, List<DataSource> htmlDataSourceList) throws MessagingException {
    Multipart multiPartHtml = new MimeMultipart("related");
    htmlBodyPart.setContent(multiPartHtml);
    MimeBodyPart part = new MimeBodyPart();
    part.setContent(htmlMessage, CONTENT_TYPE_TEXT_HTML);
    part.setHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_HTML);
    part.setHeader("Content-Transfer-Encoding", "quoted-printable");
    multiPartHtml.addBodyPart(part);
    for (DataSource source : htmlDataSourceList) {
      part = new MimeBodyPart();
      DataHandler handler = new DataHandler(source);
      part.setDataHandler(handler);
      part.setFileName(source.getName());
      part.addHeader("Content-ID", "<" + source.getName() + ">");
      multiPartHtml.addBodyPart(part);
    }
  }

  private static void writePlainBody(MimePart plainBodyPart, String plainTextMessage) throws MessagingException {
    plainBodyPart.setText(plainTextMessage, "UTF-8");
    plainBodyPart.setHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_PLAIN);
    plainBodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
  }

  private MimeMessage createMimeMessageInternal(String bodyTextPlain, String bodyTextHtml, DataSource[] attachements) throws ProcessingException {
    try {
      CharsetSafeMimeMessage m = new CharsetSafeMimeMessage();
      MimeMultipart multiPart = new MimeMultipart();
      BodyPart bodyPart = createBodyPart(bodyTextPlain, bodyTextHtml);
      if (bodyPart == null) {
        return null;
      }
      multiPart.addBodyPart(bodyPart);
      // attachements
      if (attachements != null) {
        for (DataSource source : attachements) {
          MimeBodyPart part = new MimeBodyPart();
          DataHandler handler = new DataHandler(source);
          part.setDataHandler(handler);
          part.setFileName(source.getName());
          multiPart.addBodyPart(part);
        }
      }
      m.setContent(multiPart);
      return m;
    }
    catch (Throwable t) {
      throw new ProcessingException("Failed to create MimeMessage.", t);
    }
  }

  private BodyPart createBodyPart(String bodyTextPlain, String bodyTextHtml) throws MessagingException {
    if (!StringUtility.isNullOrEmpty(bodyTextPlain) && !StringUtility.isNullOrEmpty(bodyTextHtml)) {
      // multipart
      MimeBodyPart plainPart = new MimeBodyPart();
      plainPart.setText(bodyTextPlain, "UTF-8");
      plainPart.addHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_PLAIN);
      MimeBodyPart htmlPart = new MimeBodyPart();
      htmlPart.setText(bodyTextHtml, "UTF-8");
      htmlPart.addHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_HTML);

      Multipart multiPart = new MimeMultipart("alternative");
      multiPart.addBodyPart(plainPart);
      multiPart.addBodyPart(htmlPart);
      MimeBodyPart multiBodyPart = new MimeBodyPart();
      multiBodyPart.setContent(multiPart);
      return multiBodyPart;
    }
    else if (!StringUtility.isNullOrEmpty(bodyTextPlain)) {
      MimeBodyPart part = new MimeBodyPart();
      part.setText(bodyTextPlain, "UTF-8");
      part.addHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_PLAIN);
      return part;
    }
    else if (!StringUtility.isNullOrEmpty(bodyTextHtml)) {
      MimeBodyPart part = new MimeBodyPart();
      part.setText(bodyTextHtml, "UTF-8");
      part.addHeader(CONTENT_TYPE_ID, CONTENT_TYPE_TEXT_HTML);
      return part;
    }
    return null;
  }

  public static MimeMessage createMessageFromBytes(byte[] bytes) throws ProcessingException {
    return instance.createMessageFromBytesImpl(bytes, null);
  }

  public static MimeMessage createMessageFromBytes(byte[] bytes, Session session) throws ProcessingException {
    return instance.createMessageFromBytesImpl(bytes, session);
  }

  private MimeMessage createMessageFromBytesImpl(byte[] bytes, Session session) throws ProcessingException {
    try {
      ByteArrayInputStream st = new ByteArrayInputStream(bytes);
      return new MimeMessage(session, st);
    }
    catch (Throwable t) {
      throw new ProcessingException("Unexpected: ", t);
    }
  }

  /**
   * @since 2.7
   */
  public static String getContentTypeForExtension(String ext) {
    if (ext == null) {
      return null;
    }
    if (ext.startsWith(".")) {
      ext = ext.substring(1);
    }
    ext = ext.toLowerCase();
    String type = FileUtility.getContentTypeForExtension(ext);
    if (type == null) {
      type = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType("tmp." + ext);
    }
    return type;
  }

  /**
   * Careful: this method returns null when the list of addresses is empty! This is a (stupid) default by
   * javax.mime.Message
   */
  private InternetAddress[] parseAddresses(String[] addresses) throws AddressException {
    if (addresses == null) {
      return null;
    }
    ArrayList<InternetAddress> addrList = new ArrayList<InternetAddress>();
    for (int i = 0; i < Array.getLength(addresses); i++) {
      addrList.add(new InternetAddress(addresses[i]));
    }
    if (addrList.size() == 0) {
      return null;
    }
    else {
      return addrList.toArray(new InternetAddress[addrList.size()]);
    }
  }

  private String getCharacterEncodingOfMimePart(MimePart part) throws MessagingException {
    Pattern pattern = Pattern.compile("charset=\".*\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(part.getContentType());
    String characterEncoding = "UTF-8"; // default, a good guess in Europe
    if (matcher.find()) {
      if (matcher.group(0).split("\"").length >= 2) {
        characterEncoding = matcher.group(0).split("\"")[1];
      }
    }
    else {
      if (part.getContentType().contains("charset=")) {
        if (part.getContentType().split("charset=").length == 2) {
          characterEncoding = part.getContentType().split("charset=")[1];
        }
      }
    }
    return characterEncoding;
  }

  static {
    fixMailcapCommandMap();
  }

  /**
   * jax-ws in jre 1.6.0 and priopr to 1.2.7 breaks support for "Umlaute" ä, ö, ü due to a bug in
   * StringDataContentHandler.writeTo
   * <p>
   * This patch uses reflection to eliminate this buggy mapping from the command map and adds the default text_plain
   * mapping (if available, e.g. sun jre)
   */
  @SuppressWarnings("unchecked")
  private static void fixMailcapCommandMap() {
    try {
      //set the com.sun.mail.handlers.text_plain to level 0 (programmatic) to prevent others from overriding in level 0
      Class textPlainClass;
      try {
        textPlainClass = Class.forName("com.sun.mail.handlers.text_plain");
      }
      catch (Throwable t) {
        //class not found, cancel
        return;
      }
      CommandMap cmap = MailcapCommandMap.getDefaultCommandMap();
      if (!(cmap instanceof MailcapCommandMap)) {
        return;
      }
      ((MailcapCommandMap) cmap).addMailcap("text/plain;;x-java-content-handler=" + textPlainClass.getName());
      //use reflection to clear out all other mappings of text/plain in level 0
      Field f = MailcapCommandMap.class.getDeclaredField("DB");
      f.setAccessible(true);
      Object[] dbArray = (Object[]) f.get(cmap);
      f = Class.forName("com.sun.activation.registries.MailcapFile").getDeclaredField("type_hash");
      f.setAccessible(true);
      Map<Object, Object> db0 = (Map<Object, Object>) f.get(dbArray[0]);
      Map<Object, Object> typeMap = (Map<Object, Object>) db0.get("text/plain");
      List<String> handlerList = (List<String>) typeMap.get("content-handler");
      //put text_plain in front
      handlerList.remove("com.sun.mail.handlers.text_plain");
      handlerList.add(0, "com.sun.mail.handlers.text_plain");
    }
    catch (Throwable t) {
      ScoutLogManager.getLogger(MailUtility.class).warn("Failed fixing MailcapComandMap string handling: " + t);
    }
  }
}
