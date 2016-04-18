package org.sj.irs.client.status;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.sj.irs.client.config.ApplicationConfig;
import org.sj.irs.client.config.KeyStoreConfig;
import org.sj.irs.client.intercept.StatusInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.gov.treasury.irs.common.ErrorMessageDetail;
import us.gov.treasury.irs.ext.aca.air._7.ACABulkReqTrnsmtStsReqGrpDtl;
import us.gov.treasury.irs.msg.irstransmitterstatusrequest.ACABulkRequestTransmitterStatusDetailRequest;
import us.gov.treasury.irs.msg.irstransmitterstatusrequest.ACABulkRequestTransmitterStatusDetailResponse;
import us.gov.treasury.irs.srv.acabulkrequesttransmitterstatus.ACATransmitterStatusReqPortType;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IRS Submission Request status client
 */

@Component("irsRequestStatusClient")
public class IrsRequestStatusClient {
    private static final Logger LOG = LoggerFactory.getLogger(IrsRequestStatusClient.class);

    private final ACATransmitterStatusReqPortType acaTransmitterStatusService;

    private final KeyStoreConfig keyStoreConfig;

    private final ApplicationConfig applicationConfig;

    @Autowired
    public IrsRequestStatusClient(ACATransmitterStatusReqPortType acaTransmitterStatusService,
                                  ApplicationConfig applicationConfig,
                                  KeyStoreConfig keyStoreConfig) {
        this.acaTransmitterStatusService = acaTransmitterStatusService;
        this.keyStoreConfig = keyStoreConfig;
        this.applicationConfig = applicationConfig;
    }

    public void execute(String receiptId) {
        LOG.info("Executing Status query");

        BindingProvider bindingProvider = (BindingProvider) acaTransmitterStatusService;
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding();
        Client client = ClientProxy.getClient(acaTransmitterStatusService);
        GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor(0);
        gzipOutInterceptor.setForce(Boolean.TRUE);
        client.getOutInterceptors().add(gzipOutInterceptor);

        ACABulkRequestTransmitterStatusDetailRequest request = generateStatusRequest(receiptId);

        List<Handler> handlerList = new ArrayList<>();

        handlerList.add(new StatusInterceptor(keyStoreConfig, applicationConfig.getTccId(), request));
        binding.setHandlerChain(handlerList);
        ACABulkRequestTransmitterStatusDetailResponse statusDetailResponse;

        String errorCode;
        String errorMessage;

        try {
            statusDetailResponse = this.acaTransmitterStatusService.getACATransmitterStatusReqOperation(request);
            String statusCode = statusDetailResponse.getACABulkRequestTransmitterResponse().getTransmissionStatusCd().value();
            LOG.info("Receipt ID: {}, Status:{}", receiptId, statusCode);
            System.out.println("Receipt ID: " + receiptId + ", Status:" + statusCode);
        } catch (SOAPFaultException e) {
            LOG.error("Error: ", e);
            SOAPFault s = e.getFault();
            Detail d = s.getDetail();
            errorCode = d.getFirstChild().getNextSibling().getFirstChild().getTextContent();
            errorMessage = e.getMessage();
            LOG.error("Error details: code: {}, message: {}", errorCode, errorMessage);
            System.out.println("ERROR");
        } catch (us.gov.treasury.irs.srv.acabulkrequesttransmitterstatus.Fault e) {
            LOG.error("Error: ", e);
            ErrorMessageDetail errorMessageDetailType = e.getFaultInfo();
            errorCode = errorMessageDetailType.getErrorMessageCd();
            errorMessage = errorMessageDetailType.getErrorMessageTxt();
            LOG.error("Error details: code:{}, message:{}", errorCode, errorMessage);
            System.out.println("ERROR");
        }
        LOG.info("Executed Status query");
    }

    private ACABulkRequestTransmitterStatusDetailRequest generateStatusRequest(String receiptId) {
        ACABulkRequestTransmitterStatusDetailRequest request = new ACABulkRequestTransmitterStatusDetailRequest();
        request.setId("id-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        ACABulkReqTrnsmtStsReqGrpDtl grpDtl = new ACABulkReqTrnsmtStsReqGrpDtl();
        grpDtl.setReceiptId(receiptId);
        request.setACABulkReqTrnsmtStsReqGrpDtl(grpDtl);
        return request;
    }
}