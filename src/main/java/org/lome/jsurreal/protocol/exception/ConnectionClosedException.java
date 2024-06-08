package org.lome.jsurreal.protocol.exception;

public class ConnectionClosedException extends Exception{

    public ConnectionClosedException() {
    }

    public ConnectionClosedException(String message) {
        super(message);
    }
}
