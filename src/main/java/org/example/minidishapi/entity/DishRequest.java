package org.example.minidishapi.entity;


public class DishRequest {

    private String       name;
    private DishTypeEnum dishType;
    private Double       sellingPrice;

    public DishRequest() {

    }

    public DishRequest(String name, DishTypeEnum dishType, Double sellingPrice) {
        this.name         = name;
        this.dishType     = dishType;
        this.sellingPrice = sellingPrice;
    }

    public String  getName()                       {
        return name;
    }
    public void  setName(String name)            {
        this.name = name;
    }
    public DishTypeEnum getDishType()                   {
        return dishType;
    }
    public void  setDishType(DishTypeEnum dt)    {
        this.dishType = dt;
    }
    public Double getSellingPrice()               {
        return sellingPrice;
    }
    public void setSellingPrice(Double sp)      {
        this.sellingPrice = sp;
    }
}