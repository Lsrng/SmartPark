package com.smartpark.upload.exception;

public class FileSizeExceededException extends FileUploadException {
    public FileSizeExceededException(String message) {
        super(message);
    }
}
