package org.example.minidishapi.repository;

import org.example.minidishapi.entity.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final JdbcTemplate jdbcTemplate;

    public IngredientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //Mapper de base
    private RowMapper<Ingredient> ingredientMapper() {
        return (rs, rowNum) -> new Ingredient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                CategoryEnum.valueOf(rs.getString("category"))
        );
    }

    //Lecture des mouvements de stock
    private List<StockMovement> findStockMovements(Integer ingredientId) {
        String sql = "SELECT * FROM StockMovement WHERE id_ingredient = ? ORDER BY creation_datetime ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockValue value = new StockValue(
                    rs.getDouble("quantity"),
                    UnitTypeEnum.valueOf(rs.getString("unit"))
            );
            return new StockMovement(
                    rs.getInt("id"),
                    value,
                    MovementTypeEnum.valueOf(rs.getString("type")),
                    rs.getTimestamp("creation_datetime").toInstant()
            );
        }, ingredientId);
    }

    //findAll (sans pagination)
    public List<Ingredient> findAll() {
        return jdbcTemplate.query("SELECT * FROM Ingredient ORDER BY id", ingredientMapper());
    }

    //findAll (avec pagination)
    public List<Ingredient> findAll(int page, int size) {
        int offset = (page - 1) * size;
        return jdbcTemplate.query(
                "SELECT * FROM Ingredient ORDER BY id LIMIT ? OFFSET ?",
                ingredientMapper(), size, offset);
    }

    //findById
    public Ingredient findById(Integer id) {
        List<Ingredient> results = jdbcTemplate.query(
                "SELECT * FROM Ingredient WHERE id = ?", ingredientMapper(), id);

        if (results.isEmpty()) {
            throw new RuntimeException("Ingredient.id=" + id + " is not found");
        }
        Ingredient ingredient = results.get(0);
        ingredient.setStockMovementList(findStockMovements(id));
        return ingredient;
    }

    // findById
    public Ingredient findByIdOrNull(Integer id) {
        List<Ingredient> results = jdbcTemplate.query(
                "SELECT * FROM Ingredient WHERE id = ?", ingredientMapper(), id);
        if (results.isEmpty()) return null;
        Ingredient ingredient = results.get(0);
        ingredient.setStockMovementList(findStockMovements(id));
        return ingredient;
    }

    //findByCriteria (pagination + filtres)
    public List<Ingredient> findByCriteria(String ingredientName, CategoryEnum category,
                                           String dishName, int page, int size) {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT i.* FROM Ingredient i " +
                        "LEFT JOIN DishIngredient di ON i.id = di.id_ingredient " +
                        "LEFT JOIN Dish d ON di.id_dish = d.id " +
                        "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (ingredientName != null) {
            sql.append(" AND i.name ILIKE ?");
            params.add("%" + ingredientName + "%");
        }
        if (category != null) {
            sql.append(" AND i.category = CAST(? AS ingredient_category)");
            params.add(category.name());
        }
        if (dishName != null) {
            sql.append(" AND d.name ILIKE ?");
            params.add("%" + dishName + "%");
        }

        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        return jdbcTemplate.query(sql.toString(), ingredientMapper(), params.toArray());
    }

    //create (atomique — rollback si doublon)
    @Transactional
    public List<Ingredient> create(List<Ingredient> newIngredients) {
        List<Ingredient> existing = findAll();
        List<String> existingNames = existing.stream()
                .map(i -> i.getName().toLowerCase())
                .toList();

        for (Ingredient ingredient : newIngredients) {
            if (existingNames.contains(ingredient.getName().toLowerCase())) {
                throw new RuntimeException(
                        "L'ingrédient '" + ingredient.getName() + "' existe déjà dans la base de données.");
            }
        }

        List<Ingredient> created = new ArrayList<>();
        for (Ingredient ingredient : newIngredients) {
            String sql = "INSERT INTO Ingredient (name, price, category) " +
                    "VALUES (?, ?, CAST(? AS ingredient_category)) RETURNING id";
            Integer newId = jdbcTemplate.queryForObject(sql, Integer.class,
                    ingredient.getName(), ingredient.getPrice(), ingredient.getCategory().name());
            ingredient.setId(newId);
            created.add(ingredient);
        }
        return created;
    }

    //save (insert ou update + mouvements de stock)
    @Transactional
    public Ingredient save(Ingredient ingredient) {
        if (ingredient.getId() == null) {
            String sql = "INSERT INTO Ingredient (name, price, category) " +
                    "VALUES (?, ?, CAST(? AS ingredient_category)) RETURNING id";
            Integer newId = jdbcTemplate.queryForObject(sql, Integer.class,
                    ingredient.getName(), ingredient.getPrice(), ingredient.getCategory().name());
            ingredient.setId(newId);
        } else {
            String sql = "UPDATE Ingredient SET name=?, price=?, category=CAST(? AS ingredient_category) WHERE id=?";
            int updated = jdbcTemplate.update(sql,
                    ingredient.getName(), ingredient.getPrice(),
                    ingredient.getCategory().name(), ingredient.getId());
            if (updated == 0) {
                throw new RuntimeException("Ingredient.id=" + ingredient.getId() + " is not found");
            }
        }

        // Sauvegarde des mouvements de stock
        if (ingredient.getStockMovementList() != null) {
            for (StockMovement movement : ingredient.getStockMovementList()) {
                if (movement.getId() != null) {
                    // ON CONFLICT DO NOTHING si l'id existe déjà
                    String sql = "INSERT INTO StockMovement (id, id_ingredient, quantity, type, unit, creation_datetime) " +
                            "VALUES (?, ?, ?, CAST(? AS mouvement_type), CAST(? AS unit_type), ?) " +
                            "ON CONFLICT (id) DO NOTHING";
                    jdbcTemplate.update(sql,
                            movement.getId(), ingredient.getId(),
                            movement.getValue().getQuantity(),
                            movement.getType().name(),
                            movement.getValue().getUnit().name(),
                            Timestamp.from(movement.getCreationDatetime()));
                } else {
                    String sql = "INSERT INTO StockMovement (id_ingredient, quantity, type, unit, creation_datetime) " +
                            "VALUES (?, ?, CAST(? AS mouvement_type), CAST(? AS unit_type), ?)";
                    jdbcTemplate.update(sql,
                            ingredient.getId(),
                            movement.getValue().getQuantity(),
                            movement.getType().name(),
                            movement.getValue().getUnit().name(),
                            Timestamp.from(movement.getCreationDatetime()));
                }
            }
        }
        return ingredient;
    }
}