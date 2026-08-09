package com.smartpark.upload.exception;

public class ExtensionNotAllowedException extends FileUploadException {
    public ExtensionNotAllowedException(String message) {
        super(message);
    }
}
