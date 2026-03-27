package org.example.minidishapi.entity;

import java.time.Instant;

public class StockMovement {

    private Integer          id;
    private StockValue       value;
    private MovementTypeEnum type;
    private Instant          creationDatetime;

    public StockMovement() {

    }

    public StockMovement(Integer id, StockValue value,
                         MovementTypeEnum type, Instant creationDatetime) {
        this.id               = id;
        this.value            = value;
        this.type             = type;
        this.creationDatetime = creationDatetime;
    }

    public Integer getId() {
        return id; }
    public void setId(Integer id) {
        this.id = id;
    }
    public StockValue getValue() {
        return value;
    }
    public void setValue(StockValue v){
        this.value = v;
    }
    public MovementTypeEnum getType() {
        return type;
    }
    public void setType(MovementTypeEnum t) {
        this.type = t;
    }
    public Instant getCreationDatetime() {
        return creationDatetime;
    }
    public void setCreationDatetime(Instant i) {
        this.creationDatetime = i;
    }
}