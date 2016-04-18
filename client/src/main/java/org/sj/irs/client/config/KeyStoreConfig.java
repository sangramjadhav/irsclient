package org.sj.irs.client.config;

/**
 * KeyStore configuration
 */
public class KeyStoreConfig {

    private String path;
    private String type;
    private String storePassword;
    private String keyPassword;
    private String keyAlias;
    private int certificateIndex;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStorePassword() {
        return storePassword;
    }

    public void setStorePassword(String storePassword) {
        this.storePassword = storePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public int getCertificateIndex() {
        return certificateIndex;
    }

    public void setCertificateIndex(int certificateIndex) {
        this.certificateIndex = certificateIndex;
    }
}
