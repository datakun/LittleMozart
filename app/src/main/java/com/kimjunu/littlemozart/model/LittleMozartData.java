package com.kimjunu.littlemozart.model;

public class LittleMozartData {
    private String filename;
    private String binaryData;
    private int error;
    private String errorMessage;

    public LittleMozartData() {
        this.filename = "";
        this.binaryData = "";
        this.error = -1;
        this.errorMessage = "";
    }

    public LittleMozartData(String filename, String binaryData, int error, String errorMessage) {
        this.filename = filename;
        this.binaryData = binaryData;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getBinaryData() {
        return binaryData;
    }

    public void setBinaryData(String binaryData) {
        this.binaryData = binaryData;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

