package org.example.minidishapi.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Ingredient {

    private Integer              id;
    private String               name;
    private Double               price;
    private CategoryEnum         category;
    private List<StockMovement>  stockMovementList = new ArrayList<>();

    public Ingredient() {

    }

    public Ingredient(Integer id, String name, Double price, CategoryEnum category) {
        this.id       = id;
        this.name     = name;
        this.price    = price;
        this.category = category;
    }

    public StockValue getStockValueAt(Instant t) {
        double total = 0.0;
        UnitTypeEnum unit = UnitTypeEnum.KG;

        for (StockMovement movement : stockMovementList) {
            if (!movement.getCreationDatetime().isAfter(t)) {
                if (movement.getType() == MovementTypeEnum.IN) {
                    total += movement.getValue().getQuantity();
                } else {
                    total -= movement.getValue().getQuantity();
                }
                unit = movement.getValue().getUnit();
            }
        }
        return new StockValue(total, unit);
    }

    public Integer getId(){
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName(){
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Double getPrice(){
        return price;
    }
    public void setPrice(Double price){
        this.price = price;
    }
    public CategoryEnum getCategory(){
        return category;
    }
    public void setCategory(CategoryEnum c){
        this.category = c;
    }
    public List<StockMovement> getStockMovementList(){
        return stockMovementList;
    }
    public void setStockMovementList(List<StockMovement> list){
        this.stockMovementList = list;
    }
}