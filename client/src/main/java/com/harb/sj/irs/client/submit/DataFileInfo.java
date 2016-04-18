package com.harb.sj.irs.client.submit;

/**
 * Model to represent Data file (1094/1095)
 */
public class DataFileInfo {
    private String fileName;
    private String filePath;
    private String checksum;
    private long size;
    private int count1094;
    private int count1095;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getCount1094() {
        return count1094;
    }

    public void setCount1094(int count1094) {
        this.count1094 = count1094;
    }

    public int getCount1095() {
        return count1095;
    }

    public void setCount1095(int count1095) {
        this.count1095 = count1095;
    }
}
