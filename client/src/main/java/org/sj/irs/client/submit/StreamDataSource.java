package org.sj.irs.client.submit;

import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Data source from input file
 */
public class StreamDataSource implements DataSource {
    private InputStream inputStream;

    public StreamDataSource(InputStream inputStream) {
        this.inputStream = inputStream;

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getContentType() {
        return "application/xml";
    }

    @Override
    public String getName() {
        return "StreamDataSource";
    }
}