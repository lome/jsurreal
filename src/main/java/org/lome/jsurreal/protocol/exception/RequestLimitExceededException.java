package org.lome.jsurreal.protocol.exception;

public class RequestLimitExceededException extends SurrealCallException{

    public RequestLimitExceededException() {
        super(0, "Request Limit Exceeded");
    }

    public RequestLimitExceededException(String message) {
        super(0, message);
    }
}
