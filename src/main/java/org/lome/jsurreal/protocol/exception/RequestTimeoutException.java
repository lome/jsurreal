package org.lome.jsurreal.protocol.exception;

public class RequestTimeoutException extends Exception{

    public RequestTimeoutException() {
    }

    public RequestTimeoutException(String message) {
        super(message);
    }
}
