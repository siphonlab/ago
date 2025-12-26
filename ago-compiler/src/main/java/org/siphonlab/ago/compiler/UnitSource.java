package org.siphonlab.ago.compiler;

import java.io.Reader;

public class UnitSource {
    private String fileName;
    private Reader reader;

    public UnitSource(String fileName, Reader reader) {
        this.fileName = fileName;
        this.reader = reader;
    }

    public String getFileName() {
        return fileName;
    }

    public Reader getReader() {
        return reader;
    }

}
