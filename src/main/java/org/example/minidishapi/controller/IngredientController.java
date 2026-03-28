package org.example.minidishapi.controller;

import org.example.minidishapi.entity.Ingredient;
import org.example.minidishapi.entity.StockValue;
import org.example.minidishapi.entity.UnitTypeEnum;
import org.example.minidishapi.repository.IngredientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    public IngredientController(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    // GET /ingredients
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        List<Map<String, Object>> response = ingredients.stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(response);
    }

    //GET /ingredients/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getIngredientById(@PathVariable Integer id) {
        try {
            Ingredient ingredient = ingredientRepository.findById(id);
            return ResponseEntity.ok(toMap(ingredient));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body("Ingredient.id=" + id + " is not found");
        }
    }

    // GET /ingredients/{id}/stock?at=...&unit=...
    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStock(
            @PathVariable Integer id,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) String unit) {

        if (at == null || unit == null) {
            return ResponseEntity.status(400)
                    .body("Either mandatory query parameter `at` or `unit` is not provided.");
        }

        // Récupération de l'ingrédient
        Ingredient ingredient;
        try {
            ingredient = ingredientRepository.findById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body("Ingredient.id=" + id + " is not found");
        }

        Instant instant;
        try {
            instant = Instant.parse(at);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(400)
                    .body("Format 'at' invalide. Utilise ISO 8601, ex : 2024-01-06T12:00:00Z");
        }

        UnitTypeEnum unitType;
        try {
            unitType = UnitTypeEnum.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body("Valeur 'unit' invalide. Valeurs acceptées : PCS, KG, L");
        }

        StockValue stockValue = ingredient.getStockValueAt(instant);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("unit", unitType.name());
        response.put("value", stockValue.getQuantity());
        return ResponseEntity.ok(response);
    }

    // Helper : Ingredient → Map JSON
    private Map<String, Object> toMap(Ingredient i) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",       i.getId());
        map.put("name",     i.getName());
        map.put("category", i.getCategory().name());
        map.put("price",    i.getPrice());
        return map;
    }
}
