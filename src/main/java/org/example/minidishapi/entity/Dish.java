package org.example.minidishapi.entity;

import java.util.ArrayList;
import java.util.List;

public class Dish {

    private Integer           id;
    private String            name;
    private DishTypeEnum      dishType;
    private Double            sellingPrice;
    private List<DishIngredient> ingredients = new ArrayList<>();

    public Dish() {

    }

    public Dish(Integer id, String name, DishTypeEnum dishType, Double sellingPrice) {
        this.id           = id;
        this.name         = name;
        this.dishType     = dishType;
        this.sellingPrice = sellingPrice;
    }

    // Coût total du plat = somme(price * quantityRequired) pour chaque ingrédient
    public Double getDishCost() {
        double cost = 0.0;
        for (DishIngredient di : ingredients) {
            cost += di.getIngredient().getPrice() * di.getQuantityRequired();
        }
        return cost;
    }

    // Marge brute = sellingPrice - getDishCost(). Lance une exception si sellingPrice est null.
    public Double getGrossMargin() {
        if (sellingPrice == null) {
            throw new RuntimeException("Le prix de vente est null pour le plat : " + name);
        }
        return sellingPrice - getDishCost();
    }

    public Integer getId(){
        return id;
    }
    public void setId(Integer id){
        this.id = id;
    }
    public String getName(){
        return name;
    }
    public void setName(String n){
        this.name = n;
    }
    public DishTypeEnum getDishType(){
        return dishType;
    }
    public void setDishType(DishTypeEnum dt) {
        this.dishType = dt;
    }
    public Double getSellingPrice(){
        return sellingPrice;
    }
    public void setSellingPrice(Double sp) {
        this.sellingPrice = sp;
    }
    public List<DishIngredient> getIngredients(){
        return ingredients;
    }
    public void setIngredients(List<DishIngredient> list) {
        this.ingredients = list;
    }
}