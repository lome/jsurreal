package org.lome.jsurreal.protocol.exception;

import org.lome.jsurreal.protocol.SurrealError;

public class SurrealCallException extends Exception{

    private final int code;

    public SurrealCallException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SurrealCallException(SurrealError error){
        this(error.getCode(), error.getMessage());
    }

    public int getCode() {
        return code;
    }
}
