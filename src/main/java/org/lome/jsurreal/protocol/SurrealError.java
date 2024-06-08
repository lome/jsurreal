package org.lome.jsurreal.protocol;

public class SurrealError {

    private int code;
    private String message;

    public SurrealError() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SurrealError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
