package org.lome.jsurreal.protocol.command;

public enum Command {
    USE("use"),
    INFO("info"),
    SIGNUP("signup"),
    SIGNIN("signin"),
    AUTHENTICATE("authenticate"),
    INVALIDATE("invalidate"),
    LET("let"),
    UNSET("unset"),
    LIVE("live"),
    KILL("kill"),
    QUERY("query"),
    SELECT("select"),
    CREATE("create"),
    INSERT("insert"),
    UPDATE("update"),
    MERGE("merge"),
    PATCH("patch"),
    DELETE("delete");

    private final String symbol;
    Command(String symbol){
        this.symbol = symbol;
    }

    public String getSymbol(){
        return this.symbol;
    }

}
