package org.sj.irs.client.submit;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.sj.irs.client.config.ApplicationConfig;
import org.sj.irs.client.config.KeyStoreConfig;
import org.sj.irs.client.intercept.SubmissionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.gov.treasury.irs.common.ErrorMessageDetail;
import us.gov.treasury.irs.ext.aca.air._7.ACATransmitterManifestReqDtl;
import us.gov.treasury.irs.msg.irsacabulkrequesttransmitter.ACABulkRequestTransmitter;
import us.gov.treasury.irs.msg.irsacabulkrequesttransmitter.ACABulkRequestTransmitterResponse;
import us.gov.treasury.irs.srv.gettransmitterbulkrequest.BulkRequestTransmitterPortType;

import javax.activation.DataHandler;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * IRS Submission Client
 */

@Component("irsTransmitterClient")
public class IrsTransmitterClient {
    private static final Logger LOG = LoggerFactory.getLogger(IrsTransmitterClient.class);

    private BulkRequestTransmitterPortType bulkRequestTransmitterPortType;

    private KeyStoreConfig keyStoreConfig;

    private ApplicationConfig applicationConfig;

    @Autowired
    public IrsTransmitterClient(BulkRequestTransmitterPortType bulkRequestTransmitterPortType,
                                ApplicationConfig applicationConfig,
                                KeyStoreConfig keyStoreConfig) {
        this.bulkRequestTransmitterPortType = bulkRequestTransmitterPortType;
        this.keyStoreConfig = keyStoreConfig;
        this.applicationConfig = applicationConfig;
    }


    public String execute(ACATransmitterManifestReqDtl aCATransmitterManifestReqDtl, DataFileInfo dataFileInfo) {
        LOG.info("Executing bulk transmission web service");
        File dataFile = new File(dataFileInfo.getFilePath());
        BindingProvider bindingProvider = (BindingProvider) bulkRequestTransmitterPortType;
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding();
        Client client = ClientProxy.getClient(bulkRequestTransmitterPortType);
        GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor(0);
        gzipOutInterceptor.setForce(Boolean.TRUE);
        client.getOutInterceptors().add(gzipOutInterceptor);

        List<Handler> handlerList = new ArrayList<>();
        handlerList.add(new SubmissionInterceptor(aCATransmitterManifestReqDtl,
                keyStoreConfig, applicationConfig.getTccId()));
        binding.setHandlerChain(handlerList);

        String errorCode;
        String errorText;
        try {
            String txml = FileUtils.readFileToString(dataFile);
            InputStream inputStream = new ByteArrayInputStream(txml.getBytes(StandardCharsets.UTF_8));
            DataHandler dataHandler = new DataHandler(new StreamDataSource(inputStream));
            ACABulkRequestTransmitter acaBulkRequestTransmitter =
                    new us.gov.treasury.irs.msg.irsacabulkrequesttransmitter.ObjectFactory().createACABulkRequestTransmitter();

            acaBulkRequestTransmitter.setBulkExchangeFile(dataHandler);

            ACABulkRequestTransmitterResponse aCABulkRequestTransmitterResponse;
            LOG.info("invoking web service acaBulkRequestTransmitter");
            aCABulkRequestTransmitterResponse = bulkRequestTransmitterPortType.bulkRequestTransmitter(acaBulkRequestTransmitter);
            String receiptId = aCABulkRequestTransmitterResponse.getReceiptId();
            LOG.info("Receipt ID: {}", receiptId);
            System.out.println("Receipt ID: " + receiptId);
            return receiptId;
        } catch (SOAPFaultException e) {
            LOG.error("Error: ", e);
            SOAPFault s = e.getFault();
            Detail d = s.getDetail();
            errorCode = d.getFirstChild().getNextSibling().getFirstChild().getTextContent();
            errorText = e.getMessage();
            LOG.error("Error details: code: {}, message: {}", errorCode, errorText);
            System.out.println("Receipt ID: ERROR");
            return "ERROR";
        } catch (us.gov.treasury.irs.srv.gettransmitterbulkrequest.Fault e) {
            LOG.error("Error: ", e);
            ErrorMessageDetail errorMessageDetailType = e.getFaultInfo();
            errorCode = errorMessageDetailType.getErrorMessageCd();
            errorText = errorMessageDetailType.getErrorMessageTxt();
            LOG.error("Error details: code:{}, message:{}", errorCode, errorText);
            System.out.println("Receipt ID: ERROR");
            return "ERROR";
        } catch (IOException ioe) {
            LOG.error("Can not attach MTOM attachment", ioe);
            return "ERROR";
        } finally {
            LOG.info("Executed bulk transmission web service");
        }
    }
}