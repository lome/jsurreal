package org.lome.jsurreal.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name="persons")
public class SampleEntity {

    @Id
    private String id;

    @Column(name = "name")
    private String fuck;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFuck() {
        return fuck;
    }

    public void setFuck(String fuck) {
        this.fuck = fuck;
    }
}
