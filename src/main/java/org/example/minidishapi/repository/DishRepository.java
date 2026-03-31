package org.example.minidishapi.repository;

import org.example.minidishapi.entity.*;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {

    private final DataSource           dataSource;
    private final IngredientRepository ingredientRepository;

    public DishRepository(DataSource dataSource, IngredientRepository ingredientRepository) {
        this.dataSource           = dataSource;
        this.ingredientRepository = ingredientRepository;
    }

    //findAll AVEC filtres optionnels
    public List<Dish> findAllWithFilters(Double priceUnder, Double priceOver, String name) {
        List<Dish> dishes = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM Dish WHERE 1=1");

        if (priceUnder != null) {
            sql.append(" AND selling_price < ?");
            params.add(priceUnder);
        }
        if (priceOver != null) {
            sql.append(" AND selling_price > ?");
            params.add(priceOver);
        }
        if (name != null && !name.isBlank()) {
            sql.append(" AND name ILIKE ?");
            params.add("%" + name + "%");
        }

        sql.append(" ORDER BY id");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Dish dish = mapDish(rs);
                    dish.setIngredients(findIngredientsByDishId(dish.getId()));
                    dishes.add(dish);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des plats", e);
        }
        return dishes;
    }

    //findAll sans filtres (appelle findAllWithFilters avec null)
    public List<Dish> findAll() {
        return findAllWithFilters(null, null, null);
    }

    // findById
    public Dish findById(Integer id) {
        String sql = "SELECT * FROM Dish WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Dish dish = mapDish(rs);
                    dish.setIngredients(findIngredientsByDishId(id));
                    return dish;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du plat", e);
        }
        throw new RuntimeException("Dish.id=" + id + " is not found");
    }

    //createDishes (POST /dishes)
    public List<Dish> createDishes(List<DishRequest> requests) {
        // Vérifie les doublons de noms AVANT d'insérer
        for (DishRequest request : requests) {
            if (dishNameExists(request.getName())) {
                throw new IllegalArgumentException(
                        "Dish.name=" + request.getName() + " already exists");
            }
        }

        List<Dish> createdDishes = new ArrayList<>();
        String sql = "INSERT INTO Dish (name, dish_type, selling_price) " +
                "VALUES (?, CAST(? AS dish_type), ?) RETURNING id";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (DishRequest request : requests) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, request.getName());
                        stmt.setString(2, request.getDishType().name());

                        if (request.getSellingPrice() != null) {
                            stmt.setDouble(3, request.getSellingPrice());
                        } else {
                            stmt.setNull(3, Types.NUMERIC);
                        }

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int newId = rs.getInt("id");
                                Dish created = new Dish(
                                        newId,
                                        request.getName(),
                                        request.getDishType(),
                                        request.getSellingPrice()
                                );
                                created.setIngredients(new ArrayList<>());
                                createdDishes.add(created);
                            }
                        }
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la creation des plats", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion", e);
        }
        return createdDishes;
    }

    //findByIngredientName
    public List<Dish> findByIngredientName(String ingredientName) {
        List<Dish> dishes = new ArrayList<>();
        String sql =
                "SELECT DISTINCT d.* FROM Dish d " +
                        "JOIN DishIngredient di ON d.id = di.id_dish " +
                        "JOIN Ingredient i ON di.id_ingredient = i.id " +
                        "WHERE i.name ILIKE ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + ingredientName + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Dish dish = mapDish(rs);
                    dish.setIngredients(findIngredientsByDishId(dish.getId()));
                    dishes.add(dish);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par ingredient", e);
        }
        return dishes;
    }

    // save
    public Dish save(Dish dish) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (dish.getId() == null) {
                    String sql = "INSERT INTO Dish (name, dish_type, selling_price) " +
                            "VALUES (?, CAST(? AS dish_type), ?) RETURNING id";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, dish.getName());
                        stmt.setString(2, dish.getDishType().name());
                        if (dish.getSellingPrice() != null) {
                            stmt.setDouble(3, dish.getSellingPrice());
                        } else {
                            stmt.setNull(3, Types.NUMERIC);
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                dish.setId(rs.getInt("id"));
                            }
                        }
                    }
                } else {
                    String sql = "UPDATE Dish SET name=?, dish_type=CAST(? AS dish_type), " +
                            "selling_price=? WHERE id=?";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, dish.getName());
                        stmt.setString(2, dish.getDishType().name());
                        if (dish.getSellingPrice() != null) {
                            stmt.setDouble(3, dish.getSellingPrice());
                        } else {
                            stmt.setNull(3, Types.NUMERIC);
                        }
                        stmt.setInt(4, dish.getId());

                        int updated = stmt.executeUpdate();
                        if (updated == 0) {
                            throw new RuntimeException("Dish.id=" + dish.getId() + " is not found");
                        }
                    }
                }

                String deleteSql = "DELETE FROM DishIngredient WHERE id_dish = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, dish.getId());
                    stmt.executeUpdate();
                }

                String insertSql = "INSERT INTO DishIngredient (id_dish, id_ingredient, quantity_required, unit) " +
                        "VALUES (?, ?, ?, CAST(? AS unit_type))";
                for (DishIngredient di : dish.getIngredients()) {
                    try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        stmt.setInt(1, dish.getId());
                        stmt.setInt(2, di.getIngredient().getId());
                        stmt.setDouble(3, di.getQuantityRequired());
                        stmt.setString(4, di.getUnit().name());
                        stmt.executeUpdate();
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la sauvegarde du plat", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion", e);
        }
        return findById(dish.getId());
    }

    // updateIngredients
    public Dish updateIngredients(Integer dishId, List<Integer> ingredientIds) {
        findById(dishId);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String deleteSql = "DELETE FROM DishIngredient WHERE id_dish = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, dishId);
                    stmt.executeUpdate();
                }

                String insertSql = "INSERT INTO DishIngredient (id_dish, id_ingredient, quantity_required, unit) " +
                        "VALUES (?, ?, ?, CAST(? AS unit_type))";
                for (Integer ingId : ingredientIds) {
                    if (ingredientRepository.findByIdOrNull(ingId) != null) {
                        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                            stmt.setInt(1, dishId);
                            stmt.setInt(2, ingId);
                            stmt.setDouble(3, 1.0);
                            stmt.setString(4, "KG");
                            stmt.executeUpdate();
                        }
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la mise a jour des ingredients", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion", e);
        }
        return findById(dishId);
    }

    //dishNameExists (usage interne)
    private boolean dishNameExists(String name) {
        String sql = "SELECT COUNT(*) FROM Dish WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la verification du nom", e);
        }
        return false;
    }

    //findIngredientsByDishId (usage interne)
    private List<DishIngredient> findIngredientsByDishId(Integer dishId) {
        List<DishIngredient> list = new ArrayList<>();
        String sql =
                "SELECT di.id, di.id_dish, di.quantity_required, di.unit, " +
                        "       i.id AS i_id, i.name AS i_name, i.price AS i_price, i.category AS i_category " +
                        "FROM DishIngredient di " +
                        "JOIN Ingredient i ON di.id_ingredient = i.id " +
                        "WHERE di.id_dish = ? ORDER BY di.id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, dishId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Ingredient ingredient = new Ingredient(
                            rs.getInt("i_id"),
                            rs.getString("i_name"),
                            rs.getDouble("i_price"),
                            CategoryEnum.valueOf(rs.getString("i_category"))
                    );
                    list.add(new DishIngredient(
                            rs.getInt("id"),
                            rs.getInt("id_dish"),
                            ingredient,
                            rs.getDouble("quantity_required"),
                            UnitTypeEnum.valueOf(rs.getString("unit"))
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des ingredients du plat", e);
        }
        return list;
    }

    // mapDish (usage interne)
    private Dish mapDish(ResultSet rs) throws SQLException {
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
}