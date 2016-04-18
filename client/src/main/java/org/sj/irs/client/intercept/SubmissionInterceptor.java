package org.sj.irs.client.intercept;

import org.sj.irs.client.config.KeyStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.gov.treasury.irs.ext.aca.air._7.ACATransmitterManifestReqDtl;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;


/**
 * Submission interceptor for adding WS Security and other headers
 */
public class SubmissionInterceptor implements SOAPHandler<SOAPMessageContext> {
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionInterceptor.class);

    private ACATransmitterManifestReqDtl aCATransmitterManifestReqDtl;
    private KeyStoreConfig keyStoreConfig;
    private String transmitterControlCode;

    public SubmissionInterceptor() {

    }

    public SubmissionInterceptor(ACATransmitterManifestReqDtl aCATransmitterManifestReqDtl,
                                 KeyStoreConfig keyStoreConfig,
                                 String transmitterControlCode) {
        this.aCATransmitterManifestReqDtl = aCATransmitterManifestReqDtl;
        this.keyStoreConfig = keyStoreConfig;
        this.transmitterControlCode = transmitterControlCode;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        Boolean isOutgoing = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOutgoing) {
            // intercept only outgoing message
            SOAPMessage message = smc.getMessage();
            try {
                SOAPEnvelope envelope = smc.getMessage().getSOAPPart().getEnvelope();
                envelope.addNamespaceDeclaration("urn", "urn:us:gov:treasury:irs:ext:aca:air:7.0");
                envelope.addNamespaceDeclaration("urn1", "urn:us:gov:treasury:irs:common");
                envelope.addNamespaceDeclaration("oas1",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

                SOAPHeader header = envelope.getHeader();

                XMLGregorianCalendar xmlNow = InterceptorUtils.getTimeNowAsXMLGregorianCalendar();
                header.addChildElement(InterceptorUtils.getSOAPElementFromBindingObject(aCATransmitterManifestReqDtl));
                InterceptorUtils.buildAcaBusinessHeaderForSubmission(header, xmlNow, transmitterControlCode);
                InterceptorUtils.constructActionHeader(header, aCATransmitterManifestReqDtl);
                InterceptorUtils.createWSSecurityHeaders(message, keyStoreConfig, aCATransmitterManifestReqDtl.getClass());
                InterceptorUtils.logSOAPMessage(message);
            } catch (Exception e) {
                LOG.error("Error: " + e.getMessage(), e);
            }
        } else {
            try {
                SOAPMessage message = smc.getMessage();
                InterceptorUtils.logSOAPMessage(message);
            } catch (Exception ex) {
                LOG.error("Error occurred", ex);
            }
        }
        return isOutgoing;
    }

    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {
    }
}