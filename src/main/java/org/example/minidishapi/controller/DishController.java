package org.example.minidishapi.controller;

import org.example.minidishapi.entity.Dish;
import org.example.minidishapi.entity.DishIngredient;
import org.example.minidishapi.entity.Ingredient;
import org.example.minidishapi.repository.DishRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    // GET /dishes
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();
        List<Map<String, Object>> response = dishes.stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(response);
    }

    // PUT /dishes/{id}/ingredients
    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateIngredients(
            @PathVariable Integer id,
            @RequestBody(required = false) List<Map<String, Object>> body) {

        if (body == null || body.isEmpty()) {
            return ResponseEntity.status(400)
                    .body("Le corps de la requête est obligatoire et ne doit pas être vide.");
        }

        // Extraction des ids depuis le body JSON
        List<Integer> ingredientIds;
        try {
            ingredientIds = body.stream()
                    .map(m -> {
                        Object rawId = m.get("id");
                        if (rawId instanceof Number) return ((Number) rawId).intValue();
                        throw new RuntimeException("Champ 'id' manquant ou invalide dans le body.");
                    })
                    .toList();
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }

        try {
            Dish updatedDish = dishRepository.updateIngredients(id, ingredientIds);
            return ResponseEntity.ok(toMap(updatedDish));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("is not found")) {
                return ResponseEntity.status(404).body("Dish.id=" + id + " is not found");
            }
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    //Helper : Dish → Map JSON
    private Map<String, Object> toMap(Dish dish) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",           dish.getId());
        map.put("name",         dish.getName());
        map.put("sellingPrice", dish.getSellingPrice());

        List<Map<String, Object>> ingredientList = new ArrayList<>();
        for (DishIngredient di : dish.getIngredients()) {
            Ingredient ing = di.getIngredient();
            Map<String, Object> ingMap = new LinkedHashMap<>();
            ingMap.put("id",       ing.getId());
            ingMap.put("name",     ing.getName());
            ingMap.put("category", ing.getCategory().name());
            ingMap.put("price",    ing.getPrice());
            ingredientList.add(ingMap);
        }
        map.put("ingredients", ingredientList);
        return map;
    }
}
