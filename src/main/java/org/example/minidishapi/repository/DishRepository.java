package org.example.minidishapi.repository;

import org.example.minidishapi.entity.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {

    private final JdbcTemplate          jdbcTemplate;
    private final IngredientRepository  ingredientRepository;

    public DishRepository(JdbcTemplate jdbcTemplate, IngredientRepository ingredientRepository) {
        this.jdbcTemplate         = jdbcTemplate;
        this.ingredientRepository = ingredientRepository;
    }

    // Mapper de base
    private Dish mapDish(ResultSet rs, int rowNum) throws SQLException {
        Double sellingPrice = rs.getObject("selling_price") != null
                ? rs.getDouble("selling_price")
                : null;
        return new Dish(
                rs.getInt("id"),
                rs.getString("name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                sellingPrice
        );
    }

    // Récupère les DishIngredient d'un plat
    private List<DishIngredient> findIngredientsByDishId(Integer dishId) {
        String sql =
                "SELECT di.id, di.id_dish, di.quantity_required, di.unit, " +
                        "       i.id AS i_id, i.name AS i_name, i.price AS i_price, i.category AS i_category " +
                        "FROM DishIngredient di " +
                        "JOIN Ingredient i ON di.id_ingredient = i.id " +
                        "WHERE di.id_dish = ? ORDER BY di.id";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Ingredient ingredient = new Ingredient(
                    rs.getInt("i_id"),
                    rs.getString("i_name"),
                    rs.getDouble("i_price"),
                    CategoryEnum.valueOf(rs.getString("i_category"))
            );
            return new DishIngredient(
                    rs.getInt("id"),
                    rs.getInt("id_dish"),
                    ingredient,
                    rs.getDouble("quantity_required"),
                    UnitTypeEnum.valueOf(rs.getString("unit"))
            );
        }, dishId);
    }

    //findAll
    public List<Dish> findAll() {
        List<Dish> dishes = jdbcTemplate.query("SELECT * FROM Dish ORDER BY id", this::mapDish);
        for (Dish dish : dishes) {
            dish.setIngredients(findIngredientsByDishId(dish.getId()));
        }
        return dishes;
    }

    // findById
    public Dish findById(Integer id) {
        List<Dish> results = jdbcTemplate.query(
                "SELECT * FROM Dish WHERE id = ?", this::mapDish, id);
        if (results.isEmpty()) {
            throw new RuntimeException("Dish.id=" + id + " is not found");
        }
        Dish dish = results.get(0);
        dish.setIngredients(findIngredientsByDishId(id));
        return dish;
    }

    // findByIngredientName
    public List<Dish> findByIngredientName(String ingredientName) {
        String sql =
                "SELECT DISTINCT d.* FROM Dish d " +
                        "JOIN DishIngredient di ON d.id = di.id_dish " +
                        "JOIN Ingredient i ON di.id_ingredient = i.id " +
                        "WHERE i.name ILIKE ?";
        List<Dish> dishes = jdbcTemplate.query(sql, this::mapDish, "%" + ingredientName + "%");
        for (Dish dish : dishes) {
            dish.setIngredients(findIngredientsByDishId(dish.getId()));
        }
        return dishes;
    }

    //save (insert ou update)
    @Transactional
    public Dish save(Dish dish) {
        if (dish.getId() == null) {
            String sql = "INSERT INTO Dish (name, dish_type, selling_price) " +
                    "VALUES (?, CAST(? AS dish_type), ?) RETURNING id";
            Integer newId = jdbcTemplate.queryForObject(sql, Integer.class,
                    dish.getName(), dish.getDishType().name(), dish.getSellingPrice());
            dish.setId(newId);
        } else {
            String sql = "UPDATE Dish SET name=?, dish_type=CAST(? AS dish_type), selling_price=? WHERE id=?";
            int updated = jdbcTemplate.update(sql,
                    dish.getName(), dish.getDishType().name(), dish.getSellingPrice(), dish.getId());
            if (updated == 0) {
                throw new RuntimeException("Dish.id=" + dish.getId() + " is not found");
            }
        }

        // Remplace toutes les associations d'ingrédients
        jdbcTemplate.update("DELETE FROM DishIngredient WHERE id_dish = ?", dish.getId());
        for (DishIngredient di : dish.getIngredients()) {
            jdbcTemplate.update(
                    "INSERT INTO DishIngredient (id_dish, id_ingredient, quantity_required, unit) " +
                            "VALUES (?, ?, ?, CAST(? AS unit_type))",
                    dish.getId(),
                    di.getIngredient().getId(),
                    di.getQuantityRequired(),
                    di.getUnit().name()
            );
        }

        return findById(dish.getId());
    }

    //  updateIngredients
    @Transactional
    public Dish updateIngredients(Integer dishId, List<Integer> ingredientIds) {
        findById(dishId);

        // Filtre : garde uniquement les ingrédients qui existent en base
        List<Integer> validIds = new ArrayList<>();
        for (Integer ingId : ingredientIds) {
            if (ingredientRepository.findByIdOrNull(ingId) != null) {
                validIds.add(ingId);
            }
        }

        // Supprime les associations actuelles, insère les nouvelles
        jdbcTemplate.update("DELETE FROM DishIngredient WHERE id_dish = ?", dishId);
        for (Integer ingId : validIds) {
            jdbcTemplate.update(
                    "INSERT INTO DishIngredient (id_dish, id_ingredient, quantity_required, unit) " +
                            "VALUES (?, ?, ?, CAST(? AS unit_type))",
                    dishId, ingId, 1.0, "KG"
            );
        }

        return findById(dishId);
    }
}
