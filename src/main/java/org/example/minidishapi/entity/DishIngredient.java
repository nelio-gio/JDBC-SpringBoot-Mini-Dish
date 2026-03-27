package org.example.minidishapi.entity;

public class DishIngredient {

    private Integer      id;
    private Integer      idDish;
    private Ingredient   ingredient;
    private Double       quantityRequired;
    private UnitTypeEnum unit;

    public DishIngredient() {

    }

    public DishIngredient(Integer id, Integer idDish, Ingredient ingredient,
                          Double quantityRequired, UnitTypeEnum unit) {
        this.id               = id;
        this.idDish           = idDish;
        this.ingredient       = ingredient;
        this.quantityRequired = quantityRequired;
        this.unit             = unit;
    }

    public Integer getId(){
        return id;
    }
    public void setId(Integer id){
        this.id = id;
    }
    public Integer getIdDish(){
        return idDish;
    }
    public void setIdDish(Integer i){
        this.idDish = i;
    }
    public Ingredient getIngredient(){
        return ingredient;
    }
    public void setIngredient(Ingredient i) {
        this.ingredient = i;
    }
    public Double getQuantityRequired() {
        return quantityRequired;
    }
    public void setQuantityRequired(Double q) {
        this.quantityRequired = q;
    }
    public UnitTypeEnum getUnit(){
        return unit;
    }
    public void setUnit(UnitTypeEnum u) {
        this.unit = u;
    }
}
