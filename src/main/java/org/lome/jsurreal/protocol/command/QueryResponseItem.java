package org.lome.jsurreal.protocol.command;

public class QueryResponseItem {

    public enum Status {
        OK,
        ERR
    }

    private Object result;
    private Status status;
    private String time;

    public QueryResponseItem() {
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
