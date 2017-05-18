package eu.domibus.util;

import eu.domibus.ebms3.sender.MSHDispatcher;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.internal.util.io.IOUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import java.io.*;

/**
 * @author idragusa
 * @since 3.2.5
 */
public class SoapUtilTest {

    @Test
    public void getRawXMLMessageTest() throws Exception {

        final String expectedRawMessage = readFile(new File("target/test-classes/dataset/as4/RawXMLMessage.xml"));

        SOAPMessage soapMessage = SoapUtilTest.createSOAPMessage("SOAPMessage.xml");
        String rawXMLMessage = SoapUtil.getRawXMLMessage(soapMessage);
        Assert.assertEquals(expectedRawMessage, rawXMLMessage.replaceAll("\\s+",""));

    }

    public static SOAPMessage createSOAPMessage(String dataset) throws SOAPException, IOException, ParserConfigurationException, SAXException {

        MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage message = factory.createMessage();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(new File("target/test-classes/dataset/as4/" + dataset).getAbsolutePath());
        DOMSource domSource = new DOMSource(document);
        SOAPPart soapPart = message.getSOAPPart();
        soapPart.setContent(domSource);

        AttachmentPart attachment = message.createAttachmentPart();
        attachment.setContent(Base64.decodeBase64("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=".getBytes()), "text/xml");
        attachment.setContentId("sbdh-order");
        message.addAttachmentPart(attachment);

        message.setProperty(MSHDispatcher.PMODE_KEY_CONTEXT_PROPERTY, "blue_gw:red_gw:testService1:tc1Action::pushTestcase1tc1Action");
        try {
            SOAPHeader soapHeader = message.getSOAPHeader();
        } catch (Exception e) {

        }
        return message;
    }

    public String readFile(File file) throws IOException {
        Reader input = new FileReader(file);
        StringWriter output = new StringWriter();
        try {
            IOUtils.copy(input, output);
        } finally {
            input.close();
        }
        return output.toString();
    }

}
