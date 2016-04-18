package org.sj.irs.client.intercept;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.sj.irs.client.config.KeyStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import us.gov.treasury.irs.ext.aca.air._7.ACATransmitterManifestReqDtl;
import us.gov.treasury.irs.msg.irstransmitterstatusrequest.ACABulkRequestTransmitterStatusDetailRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

class InterceptorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorUtils.class);

    private static PrivateKeyEntry getPrivateKeyEntry(KeyStoreConfig config) {
        KeyStore ks;
        PrivateKeyEntry keyEntry = null;
        try {
            ks = KeyStore.getInstance(config.getType());
            ks.load(new FileInputStream(config.getPath()), config.getStorePassword().toCharArray());
            keyEntry = (PrivateKeyEntry) ks.getEntry(
                    config.getKeyAlias(),
                    new KeyStore.PasswordProtection(config.getKeyPassword().toCharArray()));
        } catch (Exception e) {
            LOG.error("Error occurred in retrieving Private Key: ", e);
        }

        return keyEntry;
    }

    static SOAPElement getSOAPElementFromBindingObject(Object obj) {
        SOAPElement soapElem = null;
        JAXBContext jaxbContext;

        try {
            jaxbContext = JAXBContext.newInstance(obj.getClass());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(obj, doc);

            MessageFactory mf = MessageFactory.newInstance();
            SOAPMessage msg = mf.createMessage();
            SOAPBody body = msg.getSOAPBody();
            soapElem = body.addDocument(doc);
        } catch (Exception e) {
            LOG.error("Error: ", e);
        }

        return soapElem;
    }

    static void buildAcaBusinessHeaderForSubmission(SOAPHeader header,
                                                    XMLGregorianCalendar xmlNow,
                                                    String transmitterControlCode) {

        SOAPElement acaBusinessHeader;
        try {
            acaBusinessHeader = header.addChildElement("ACABusinessHeader",
                    "urn2", "urn:us:gov:treasury:irs:msg:acabusinessheader");
            acaBusinessHeader
                    .addAttribute(
                            new QName(
                                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                                    "Id", "wsu"), "id-" + uuid());

            acaBusinessHeader.addNamespaceDeclaration("ss2", "urn:us:gov:treasury:irs:msg:irstransmitterstatusrequest");

            SOAPElement uniqueTransmissionId = acaBusinessHeader
                    .addChildElement("UniqueTransmissionId", "urn");


            uniqueTransmissionId.addTextNode(generateTransmissionId(transmitterControlCode));

            SOAPElement timeStampElement = acaBusinessHeader.addChildElement("Timestamp", "urn1");
            timeStampElement.addTextNode(xmlNow.toString());
        } catch (SOAPException e) {
            LOG.error("Error: ", e);
        }
    }

    static void buildAcaBusinessHeaderForStatus(SOAPHeader header,
                                                XMLGregorianCalendar xmlNow,
                                                String transmitterControlCode) {

        SOAPElement acaBusinessHeader;
        try {
            acaBusinessHeader = header.addChildElement("ACABusinessHeader",
                    "ss2", "urn:us:gov:treasury:irs:msg:irstransmitterstatusrequest");
            acaBusinessHeader
                    .addAttribute(
                            new QName(
                                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                                    "Id", "wsu"),
                            "id-" + uuid()
                    );

            acaBusinessHeader.addNamespaceDeclaration("urn2", "urn:us:gov:treasury:irs:msg:acabusinessheader");
            SOAPElement uniqueTransmissionId = acaBusinessHeader
                    .addChildElement("UniqueTransmissionId", "urn");

            uniqueTransmissionId.addTextNode(generateTransmissionId(transmitterControlCode));

            SOAPElement timeStampElement = acaBusinessHeader.addChildElement("Timestamp", "urn1");
            timeStampElement.addTextNode(xmlNow.toString());
        } catch (SOAPException e) {
            LOG.error("Error: ", e);
        }
    }

    /**
     * Generates UniqueTransmissionId
     */
    private static String generateTransmissionId(String transmitterControlCode) {
        String delimiter = ":";
        String applicationId = "SYS12";
        //T = Transactional ; P= Production
        String requestType = "T";

        StringBuffer uniqueTransmissionId = new StringBuffer();

        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();

        uniqueTransmissionId = uniqueTransmissionId.append(randomUUIDString)
                .append(delimiter).append(applicationId).append(delimiter)
                .append(transmitterControlCode).append(delimiter).append(delimiter).append(requestType);

        return uniqueTransmissionId.toString();
    }


    /**
     * Add Action elements to SOAP envelope per IRS ACA-AIR  document
     */

    static void constructActionHeader(SOAPHeader header, Object o) {
        SOAPElement soapElement;
        try {
            if (o instanceof ACATransmitterManifestReqDtl) {
                soapElement = header.addChildElement(new QName("http://www.w3.org/2005/08/addressing", "Action"));
                soapElement.addTextNode("BulkRequestTransmitter");
            } else {
                soapElement = header.addChildElement(new QName("http://www.w3.org/2005/08/addressing", "Action"));
                soapElement.addTextNode("RequestSubmissionStatusDetail");
            }
        } catch (SOAPException e) {
            LOG.error("Error: ", e);
        }
    }


    /**
     * Create WS Security headers
     */
    static SOAPMessage createWSSecurityHeaders(SOAPMessage message,
                                                      KeyStoreConfig ksConfig, Class<?> clazz) throws WSSecurityException {

        PrivateKeyEntry privateKeyEntry = getPrivateKeyEntry(ksConfig);

        PrivateKey signingKey = privateKeyEntry.getPrivateKey();
        Certificate[] chain = privateKeyEntry.getCertificateChain();
        /* This is important. You must know the index of your certificate in chain */
        X509Certificate signingCert = (X509Certificate) chain[ksConfig.getCertificateIndex()];

        final String alias = ksConfig.getKeyAlias();
        // 10 minutes in seconds as per IRS
        final int signatureValidityTime = 600;

        WSSConfig config = WSSConfig.getNewInstance();

        WsuIdAllocator idAllocator = new WsuIdAllocator() {
            @Override
            public String createSecureId(String prefix, Object o) {
                switch(prefix){
                    case "KI-": return "KI-" + uuid();
                    case "STR-": return "STR-" + uuid();
                    default: return null;
                }
            }

            @Override
            public String createId(String prefix, Object o) {
                return "SIG-" + uuid();
            }

        };
        config.setIdAllocator(idAllocator);

        WSSecSignature wsSecSignature = new WSSecSignature();

        wsSecSignature.setX509Certificate(signingCert);
        wsSecSignature.setUserInfo(alias, ksConfig.getKeyPassword());
        wsSecSignature.setUseSingleCertificate(true);
        wsSecSignature.setKeyIdentifierType(WSConstants.X509_KEY_IDENTIFIER);
        wsSecSignature.setDigestAlgo(WSConstants.SHA1);
        wsSecSignature.setSignatureAlgorithm(WSConstants.RSA_SHA1);
        wsSecSignature.setSigCanonicalization(WSConstants.C14N_EXCL_WITH_COMMENTS);
        try {
            Document document = createDoc(message);
            WSSecHeader secHeader = new WSSecHeader(document);
            secHeader.insertSecurityHeader();

            WSSecTimestamp timestamp = new WSSecTimestamp();
            timestamp.setTimeToLive(signatureValidityTime);
            document = timestamp.build(document, secHeader);

            List<WSEncryptionPart> wsEncryptionParts = new ArrayList<>();

            //Order of the parts is most important
            if (clazz.equals(ACATransmitterManifestReqDtl.class)) {
                WSEncryptionPart timestampPart = new WSEncryptionPart("Timestamp",
                        WSConstants.WSU_NS, "");

                wsEncryptionParts.add(timestampPart);

                WSEncryptionPart aCATransmitterManifestReqDtlPart = new WSEncryptionPart(
                        "ACATransmitterManifestReqDtl",
                        "urn:us:gov:treasury:irs:ext:aca:air:7.0", "");
                wsEncryptionParts.add(aCATransmitterManifestReqDtlPart);

                WSEncryptionPart aCABusinessHeaderPart = new WSEncryptionPart(
                        "ACABusinessHeader",
                        "urn:us:gov:treasury:irs:msg:acabusinessheader", "");
                wsEncryptionParts.add(aCABusinessHeaderPart);
            } else if (clazz.equals(ACABulkRequestTransmitterStatusDetailRequest.class)) {
                WSEncryptionPart timestampPart = new WSEncryptionPart("Timestamp",
                        WSConstants.WSU_NS, "");

                wsEncryptionParts.add(timestampPart);

                WSEncryptionPart aCABusinessHeaderPart = new WSEncryptionPart(
                        "ACABusinessHeader",
                        "urn:us:gov:treasury:irs:msg:irstransmitterstatusrequest", "");
                wsEncryptionParts.add(aCABusinessHeaderPart);

                WSEncryptionPart aCABulkRequestTransmitterStatusDetailRequestPart = new WSEncryptionPart(
                        "ACABulkRequestTransmitterStatusDetailRequest",
                        "urn:us:gov:treasury:irs:msg:irstransmitterstatusrequest", "");
                wsEncryptionParts.add(aCABulkRequestTransmitterStatusDetailRequestPart);
            }

            wsSecSignature.getParts().addAll(wsEncryptionParts);

            Properties properties = new Properties();
            properties.setProperty("org.apache.ws.security.crypto.provider",
                    "org.apache.ws.security.components.crypto.Merlin");
            Crypto crypto = CryptoFactory.getInstance(properties);
            KeyStore keystore = KeyStore.getInstance(ksConfig.getType());

            try (FileInputStream fis = new FileInputStream(ksConfig.getPath())){
                keystore.load(fis, ksConfig.getStorePassword().toCharArray());
            }

            keystore.setKeyEntry(alias, signingKey, ksConfig.getStorePassword().toCharArray(), new Certificate[]{signingCert});
            ((Merlin) crypto).setKeyStore(keystore);
            crypto.loadCertificate(new ByteArrayInputStream(signingCert.getEncoded()));
            document = wsSecSignature.build(document, crypto, secHeader);
            updateMessage(document, message);
        } catch (Exception e) {
            LOG.error("Error: ", e);
        }
        return message;
    }

    private static Document createDoc(SOAPMessage soapMsg) throws TransformerException,
            SOAPException, IOException {
        Source src = soapMsg.getSOAPPart().getContent();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(src, result);
        return (Document) result.getNode();
    }

    private static SOAPMessage updateMessage(Document doc,
                                             SOAPMessage message)
            throws Exception {
        DOMSource domSource = new DOMSource(doc);
        message.getSOAPPart().setContent(domSource);
        return message;
    }

    static XMLGregorianCalendar getTimeNowAsXMLGregorianCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        /*
         Most important. Please check your machine's/workstations/VM's time if WS-Security header error TPE1122 occurs.
         For unix/linux you can use ntp or run below command.
         sudo date -s "$(wget -qSO- --max-redirect=0 google.com 2>&1 | grep Date: | cut -d' ' -f5-8)Z"
        */

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date());

        XMLGregorianCalendar xMLGregorianCalendar = null;
        try {
            xMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException e) {
            LOG.error("Error: ", e);
        }
        return xMLGregorianCalendar;
    }

    /**
     * Generate UUID without '-' and with Uppercase letters
     */
    private static String uuid(){
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    static void logSOAPMessage(SOAPMessage message) throws IOException, SOAPException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        message.writeTo(bout);
        String msg = bout.toString(StandardCharsets.UTF_8.name());
        LOG.info("SOAP Message: {}", msg);
    }
}
