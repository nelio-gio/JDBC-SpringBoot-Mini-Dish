package org.example.minidishapi.entity;

public class StockValue {

    private Double   quantity;
    private UnitTypeEnum unit;

    public StockValue() {

    }

    public StockValue(Double quantity, UnitTypeEnum unit) {
        this.quantity = quantity;
        this.unit     = unit;
    }

    public Double getQuantity() {
        return quantity;
    }
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    public UnitTypeEnum getUnit()    {
        return unit;
    }
    public void setUnit(UnitTypeEnum unit)   {
        this.unit = unit;
    }
}
