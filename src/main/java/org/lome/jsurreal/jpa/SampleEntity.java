package org.lome.jsurreal.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.lome.jsurreal.annotation.EntityReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(name="person")
public class SampleEntity {

    @Id
    private String id;

    @Column(name = "name")
    private String fuck;

    @EntityReference
    @Column(name = "concubin")
    private SampleEntity wife;

    @Column(name = "children")
    private List<@EntityReference SampleEntity> childrens;

    @Column(name = "cousins")
    private Map<String, @EntityReference SampleEntity> cousins = new HashMap<>();

    @Column(name = "rev_cousins")
    private Map<@EntityReference SampleEntity, String> reverseCousins = new HashMap<>();

    public SampleEntity(){}

    public SampleEntity(String id){
        this.id = id;
    }

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

    public SampleEntity getWife() {
        return wife;
    }

    public void setWife(SampleEntity wife) {
        this.wife = wife;
    }

    public List<SampleEntity> getChildrens() {
        return childrens;
    }

    public void setChildrens(List<SampleEntity> childrens) {
        this.childrens = childrens;
    }

    public Map<String, @EntityReference SampleEntity> getCousins() {
        return cousins;
    }

    public void setCousins(Map<String, @EntityReference SampleEntity> cousins) {
        this.cousins = cousins;
    }

    public Map<@EntityReference SampleEntity, String> getReverseCousins() {
        return reverseCousins;
    }

    public void setReverseCousins(Map<@EntityReference SampleEntity, String> reverseCousins) {
        this.reverseCousins = reverseCousins;
    }

    public void addCousin(SampleEntity entity){
        this.cousins.put(entity.getFuck(), entity);
        this.reverseCousins.put(entity, entity.getFuck());
    }

    @Override
    public String toString() {
        return "SampleEntity{" +
                "id='" + id + '\'' +
                ", fuck='" + fuck + '\'' +
                ", wife=" + wife +
                ", childrens=" + childrens +
                ", cousins=" + cousins +
                ", reverseCousins=" + reverseCousins +
                '}';
    }
}
