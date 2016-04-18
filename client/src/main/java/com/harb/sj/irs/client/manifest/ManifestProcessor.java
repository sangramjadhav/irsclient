package com.harb.sj.irs.client.manifest;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import us.gov.treasury.irs.ext.aca.air._7.ACATransmitterManifestReqDtl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Iterator;

/**
 * Generate ACA Manifest from manifest file.
 */
@Component
public class ManifestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ManifestProcessor.class);
	private static final String ACA_TAG = "ACATransmitterManifestReqDtl xmlns=\"URN:us:gov:treasury:irs:ext:aca:air:7.0\" xmlns:URN1=\"URN:us:gov:treasury:irs:common\" xmlns:ns3=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" ns3:Id=\"id-E68EBBF1696C5DD4AA143353323390577\"";
	private static final String ACA_END_TAG = "ACATransmitterManifestReqDtl";
	private static final String URN1 = "URN1";
	private static final String URN = "URN";
	private static final String EIN = "EIN";
	private static final String REQUIRED_EIN_TAG = URN1 + ":" + EIN;

	public ACATransmitterManifestReqDtl getObject(File file) {
		ACATransmitterManifestReqDtl dtl = null;
		try {
			String fileContents = FileUtils.readFileToString(file, "UTF-8");
			SOAPMessage message = MessageFactory.newInstance().createMessage(null,
					new ByteArrayInputStream(fileContents.getBytes()));
			Unmarshaller unmarshaller = JAXBContext.newInstance(ACATransmitterManifestReqDtl.class)
					.createUnmarshaller();

			Iterator iterator = message.getSOAPHeader().examineAllHeaderElements();
			while (iterator.hasNext()) {
				SOAPHeaderElement element = (SOAPHeaderElement) iterator.next();
				QName name = new QName("urn:us:gov:treasury:irs:ext:aca:air:7.0", "ACATransmitterManifestReqDtl",
						"urn");
				if (element.getElementQName().equals(name)) {
					dtl = (ACATransmitterManifestReqDtl) unmarshaller.unmarshal(processElement(element));
				}

			}
		} catch (Exception e) {
			LOG.error("Error", e);
		}
		return dtl;

	}

	private Document processElement(SOAPHeaderElement element)
			throws TransformerException, ParserConfigurationException, SAXException, IOException, SOAPException {
		NamedNodeMap attrs = element.getAttributes();
		element.setPrefix("");

		while (attrs.getLength() > 0) {
			attrs.removeNamedItem(attrs.item(0).getNodeName());
		}

		String xmlString = "";
		Iterator list = element.getChildElements();
		while (list.hasNext()) {
			Node node = (Node) list.next();
			xmlString += nodeToString(node);
		}

		xmlString = xmlString.replace(URN, URN1);

		xmlString = xmlString.replace(EIN, REQUIRED_EIN_TAG);
		xmlString = xmlString.replace("xmlns:urn1=\"urn1:us:gov:treasury:irs:ext:aca:air:7.0\"", "");
		xmlString = xmlString.replace(ACA_END_TAG, URN + ":" + ACA_END_TAG);
		xmlString = ("<" + ACA_TAG + ">" + xmlString + "</" + ACA_END_TAG + ">");
		return stringParser(xmlString);
	}

	private Document stringParser(String str) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		return dBuilder.parse(new InputSource(new StringReader(str)));
	}

	private static String nodeToString(Node node) throws TransformerException {
		StringWriter buf = new StringWriter();
		Transformer xform = TransformerFactory.newInstance().newTransformer();
		xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xform.transform(new DOMSource(node), new StreamResult(buf));
		return (buf.toString());
	}
}
