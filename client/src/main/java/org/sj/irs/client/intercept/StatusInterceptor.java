package org.sj.irs.client.intercept;

import org.sj.irs.client.config.KeyStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.gov.treasury.irs.msg.irstransmitterstatusrequest.ACABulkRequestTransmitterStatusDetailRequest;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;

public class StatusInterceptor implements SOAPHandler<SOAPMessageContext> {
    private static final Logger LOG = LoggerFactory.getLogger(StatusInterceptor.class);

    private KeyStoreConfig keyStoreConfig;
    private ACABulkRequestTransmitterStatusDetailRequest aCABulkRequestTransmitterStatusDetailRequest;
    private String transmitterControlCode;

    public StatusInterceptor() {}

    public StatusInterceptor(KeyStoreConfig keyStoreConfig, String transmitterControlCode,
                             ACABulkRequestTransmitterStatusDetailRequest aCABulkRequestTransmitterStatusDetailRequest) {
        this.keyStoreConfig = keyStoreConfig;
        this.transmitterControlCode = transmitterControlCode;
        this.aCABulkRequestTransmitterStatusDetailRequest = aCABulkRequestTransmitterStatusDetailRequest;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outboundProperty) {
            // intercept only outbound message
            SOAPMessage message = smc.getMessage();
            try {
                SOAPEnvelope envelope = smc.getMessage().getSOAPPart().getEnvelope();
                envelope.addNamespaceDeclaration("urn", "urn:us:gov:treasury:irs:ext:aca:air:7.0");
                envelope.addNamespaceDeclaration("urn1", "urn:us:gov:treasury:irs:common");
                envelope.addNamespaceDeclaration("oas1",
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
                SOAPHeader header = envelope.getHeader();

                XMLGregorianCalendar xmlNow = InterceptorUtils.getTimeNowAsXMLGregorianCalendar();
                InterceptorUtils.buildAcaBusinessHeaderForStatus(header, xmlNow, transmitterControlCode);
                InterceptorUtils.constructActionHeader(header, aCABulkRequestTransmitterStatusDetailRequest);
                InterceptorUtils.createWSSecurityHeaders(message, keyStoreConfig, aCABulkRequestTransmitterStatusDetailRequest.getClass());
                InterceptorUtils.logSOAPMessage(message);
            } catch (Exception e) {
                LOG.error("Error occurred: ", e);
            }
        } else {
            try {
                SOAPMessage message = smc.getMessage();
                InterceptorUtils.logSOAPMessage(message);
            } catch (Exception ex) {
                LOG.error("Error:", ex);
            }
        }

        return outboundProperty;
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