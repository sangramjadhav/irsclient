package org.sj.irs.client;

import org.sj.irs.client.manifest.ManifestProcessor;
import org.sj.irs.client.status.IrsRequestStatusClient;
import org.sj.irs.client.submit.DataFileInfo;
import org.sj.irs.client.submit.IrsTransmitterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import us.gov.treasury.irs.ext.aca.air._7.ACATransmitterManifestReqDtl;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command Line Runner implementation that will act on arguments provided and call webservice
 */
@Component
@Profile("!TEST")
public class ServiceExecutor implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceExecutor.class);

    private final IrsRequestStatusClient irsRequestStatusClient;

    private final IrsTransmitterClient irsTransmitterClient;

    private final ManifestProcessor manifestProcessor;

    private enum RequestType {TEST, BULK, STATUS}

    private static final String PATH = "data";

    @Autowired
    public ServiceExecutor(IrsTransmitterClient irsTransmitterClient,
                           IrsRequestStatusClient irsRequestStatusClient,
                           ManifestProcessor manifestProcessor) {
        this.irsTransmitterClient = irsTransmitterClient;
        this.irsRequestStatusClient = irsRequestStatusClient;
        this.manifestProcessor = manifestProcessor;
    }

    @Override
    public void run(String[] args) throws Exception {
        RequestType requestType;
        if (args == null || args.length != 2) {
            System.out.println("Insufficient arguments. This program needs 2 arguments");
            System.out.println("request type: 1 or 2. 1 represents Bulk request and 2 represents status request");
            System.out.println("unique request id: unique number or string. It should represent directory in data directory or receipt id");
        } else {
            requestType = determineRequestType(args[0]);
            switch (requestType) {
                case BULK:
                    String directory = args[1];

                    Path requestFilePath = Paths.get(PATH, directory);

                    List<File> manifests = Files.walk(requestFilePath)
                            .filter(Files::isRegularFile)
                            .map(Path::toFile)
                            .filter(x -> x.getName().toLowerCase().contains("manifest"))
                            .collect(Collectors.toList());

                    List<File> dataFiles = Files.walk(requestFilePath)
                            .filter(Files::isRegularFile)
                            .map(Path::toFile)
                            .filter(x -> !x.getName().toLowerCase().contains("manifest"))
                            .collect(Collectors.toList());

                    ACATransmitterManifestReqDtl aCATransmitterManifestReqDtl = manifestProcessor.getObject(manifests.get(0));
                    DataFileInfo dataFileInfo = new DataFileInfo();
                    dataFileInfo.setFileName(dataFiles.get(0).getName());
                    dataFileInfo.setFilePath(dataFiles.get(0).getPath());
                    /*dataFileInfo.setCount1094(1);
                    dataFileInfo.setCount1095(3);
                    dataFileInfo.setSize(14505);
                    dataFileInfo.setChecksum(DigestUtils.md5DigestAsHex(new FileInputStream(dataFile)));*/
                    String receiptId = irsTransmitterClient.execute(aCATransmitterManifestReqDtl, dataFileInfo);
                    writeReceiptIdToFile(receiptId);
                    break;
                case STATUS:
                    irsRequestStatusClient.execute(args[1]);
                    break;
                case TEST:
                    break;
            }
        }
    }

    private void writeReceiptIdToFile(String receiptId){
        File file = new File("resultReceipt.txt");
        try (FileWriter fileWriter = new FileWriter(file)){
            fileWriter.write(receiptId);
            fileWriter.flush();
            fileWriter.close();
        }catch (Exception e){
            LOG.error("Error writing to file",e);
        }
    }

    private RequestType determineRequestType(String requestType) {
        int type = Integer.parseInt(requestType);
        switch (type) {
            case 1:
                return RequestType.BULK;
            case 2:
                return RequestType.STATUS;
            default:
                return RequestType.TEST;
        }
    }
}
