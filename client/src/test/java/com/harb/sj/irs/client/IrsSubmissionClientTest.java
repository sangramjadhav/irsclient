package com.harb.sj.irs.client;

import com.harb.sj.irs.client.submit.DataFileInfo;
import com.harb.sj.irs.client.submit.IrsTransmitterClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import us.gov.treasury.irs.common.BinaryFormatCodeType;
import us.gov.treasury.irs.ext.aca.air._7.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IrsclientApplication.class)
@ActiveProfiles("TEST")
public class IrsSubmissionClientTest {

	@Autowired
	private IrsTransmitterClient client;

	@Test
	public void submitRequest() {
		DataFileInfo dataFileInfo = new DataFileInfo();
        // Provide XML file name and path
        // File name format: 1094C_Request_TCCID_TimeStamp.xml
        // TCCID and Timestamp are runtime values that you should make sure are right
		dataFileInfo.setFileName("1.xml");
		dataFileInfo.setFilePath("/tmp/1.xml");
		dataFileInfo.setCount1094(1);
		dataFileInfo.setCount1095(3);
		dataFileInfo.setSize(14505);
        // Calculate and put MD5 checksum
		dataFileInfo.setChecksum("00000");
		ACATransmitterManifestReqDtl dtl = generateAcaManifestHeader(dataFileInfo);
		client.execute(dtl, dataFileInfo);
	}

	/**
	 * Populates fields in the ACATransmitterManifestReqDtl.
	 */
	public ACATransmitterManifestReqDtl generateAcaManifestHeader(DataFileInfo dataFileInfo) {
		ACATransmitterManifestReqDtl aCATransmitterManifestReqDtl = new ACATransmitterManifestReqDtl();

		BigInteger count1095 = new BigInteger(Long.toString(dataFileInfo.getCount1095()));
		BigInteger count1094 = new BigInteger(Long.toString(dataFileInfo.getCount1094()));
		BigInteger fileSize = new BigInteger(Long.toString(dataFileInfo.getSize()));

		CompanyInformationGrp companyInformationGrp = new CompanyInformationGrp();
		VendorInformationGrp vendorInformationGrp = new VendorInformationGrp();
		BusinessNameType businessNameType = new BusinessNameType();
		OtherCompletePersonNameType contactNameGrp = new OtherCompletePersonNameType();

		BusinessAddressGrpType busAddrGrp = new BusinessAddressGrpType();
		USAddressGrp addrGrp = new USAddressGrp();

		DatatypeFactory dataTypeFactory = null;
		try {
			dataTypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}

		XMLGregorianCalendar paymentYear = dataTypeFactory.newXMLGregorianCalendar();
		paymentYear.setYear(2015);
		businessNameType.setBusinessNameLine1Txt("TEMP");

		addrGrp.setAddressLine1Txt("Address 1");
		addrGrp.setAddressLine2Txt("Address 2");
		addrGrp.setCityNm("Test");
		addrGrp.setUSStateCd(StateType.fromValue("CA"));
		addrGrp.setUSZIPCd("95104");
		addrGrp.setUSZIPExtensionCd("0000");
		busAddrGrp.setUSAddressGrp(addrGrp);

		contactNameGrp.setPersonFirstNm("Test");
		contactNameGrp.setPersonMiddleNm("X");
		contactNameGrp.setPersonLastNm("Test");
		contactNameGrp.setSuffixNm("X");

		companyInformationGrp.setCompanyNm("TEMP");

		companyInformationGrp.setContactPhoneNum("0000000000");
		companyInformationGrp.setMailingAddressGrp(busAddrGrp);
		companyInformationGrp.setContactNameGrp(contactNameGrp);

		vendorInformationGrp.setVendorCd("V");
		vendorInformationGrp.setContactPhoneNum("0000000000");
		vendorInformationGrp.setContactNameGrp(contactNameGrp);

		aCATransmitterManifestReqDtl.setPaymentYr(paymentYear);
		aCATransmitterManifestReqDtl.setPriorYearDataInd("0");
		aCATransmitterManifestReqDtl.setEIN("000000001");
		aCATransmitterManifestReqDtl.setTestFileCd("T");
		aCATransmitterManifestReqDtl.setTransmitterNameGrp(businessNameType);
		aCATransmitterManifestReqDtl.setCompanyInformationGrp(companyInformationGrp);
		aCATransmitterManifestReqDtl.setVendorInformationGrp(vendorInformationGrp);
		aCATransmitterManifestReqDtl.setTotalPayeeRecordCnt(count1095);
		aCATransmitterManifestReqDtl.setTotalPayerRecordCnt(count1094);
		/** Put your software ID */
		aCATransmitterManifestReqDtl.setSoftwareId("0000000");
		aCATransmitterManifestReqDtl.setFormTypeCd("1094/1095C");
		aCATransmitterManifestReqDtl.setBinaryFormatCd(BinaryFormatCodeType.APPLICATION_XML);
		aCATransmitterManifestReqDtl.setChecksumAugmentationNum(dataFileInfo.getChecksum());
		aCATransmitterManifestReqDtl.setAttachmentByteSizeNum(fileSize);
		aCATransmitterManifestReqDtl.setDocumentSystemFileNm(dataFileInfo.getFileName());
		aCATransmitterManifestReqDtl.setId("id-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
		aCATransmitterManifestReqDtl.setTransmissionTypeCd(TransmissionTypeCdType.O);
		return aCATransmitterManifestReqDtl;
	}

}
