package org.lome.jsurreal.protocol.command;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignIn extends BaseRequest{

    @JsonProperty("user")
    final private String username;
    @JsonProperty("pass")
    final private String password;
    @JsonProperty("NC")
    private String namespace;
    @JsonProperty("DB")
    private String database;
    @JsonProperty("SC")
    private String scope;

    public SignIn(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public SignIn withNamespace(String namespace){
        this.namespace = namespace;
        return this;
    }

    public SignIn withDatabase(String database){
        this.database = database;
        return this;
    }

    public SignIn withScope(String scope){
        this.scope = scope;
        return this;
    }

    public SignIn withSet(String name, Object value){
        this.set(name, value);
        return this;
    }
}
