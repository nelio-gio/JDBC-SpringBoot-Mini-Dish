package org.example.minidishapi.repository;

import org.example.minidishapi.entity.*;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // findAll sans pagination
    public List<Ingredient> findAll() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM Ingredient ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ingredients.add(mapIngredient(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des ingredients", e);
        }
        return ingredients;
    }

    //findAll avec pagination
    public List<Ingredient> findAll(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        int offset = (page - 1) * size;
        String sql = "SELECT * FROM Ingredient ORDER BY id LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, size);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(mapIngredient(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des ingredients", e);
        }
        return ingredients;
    }

    // findById
    public Ingredient findById(Integer id) {
        String sql = "SELECT * FROM Ingredient WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Ingredient ingredient = mapIngredient(rs);
                    ingredient.setStockMovementList(findStockMovements(id));
                    return ingredient;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de l'ingredient", e);
        }
        throw new RuntimeException("Ingredient.id=" + id + " is not found");
    }

    // findByIdOrNull (usage interne)
    public Ingredient findByIdOrNull(Integer id) {
        String sql = "SELECT * FROM Ingredient WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Ingredient ingredient = mapIngredient(rs);
                    ingredient.setStockMovementList(findStockMovements(id));
                    return ingredient;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation de l'ingredient", e);
        }
        return null;
    }

    // findByCriteria
    public List<Ingredient> findByCriteria(String ingredientName, CategoryEnum category,
                                           String dishName, int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT i.* FROM Ingredient i " +
                        "LEFT JOIN DishIngredient di ON i.id = di.id_ingredient " +
                        "LEFT JOIN Dish d ON di.id_dish = d.id " +
                        "WHERE 1=1"
        );

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(mapIngredient(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche par criteres", e);
        }
        return ingredients;
    }

    // create (atomique)
    public List<Ingredient> create(List<Ingredient> newIngredients) {
        List<Ingredient> existing = findAll();
        List<String> existingNames = existing.stream()
                .map(i -> i.getName().toLowerCase())
                .toList();

        for (Ingredient ingredient : newIngredients) {
            if (existingNames.contains(ingredient.getName().toLowerCase())) {
                throw new RuntimeException(
                        "L'ingredient '" + ingredient.getName() + "' existe deja dans la base de donnees.");
            }
        }

        List<Ingredient> created = new ArrayList<>();
        String sql = "INSERT INTO Ingredient (name, price, category) " +
                "VALUES (?, ?, CAST(? AS ingredient_category)) RETURNING id";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Ingredient ingredient : newIngredients) {
                    stmt.setString(1, ingredient.getName());
                    stmt.setDouble(2, ingredient.getPrice());
                    stmt.setString(3, ingredient.getCategory().name());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            ingredient.setId(rs.getInt("id"));
                            created.add(ingredient);
                        }
                    }
                }
                conn.commit();


            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la creation des ingredients", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion", e);
        }
        return created;
    }

    // save (insert ou update + mouvements de stock)
    public Ingredient save(Ingredient ingredient) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (ingredient.getId() == null) {
                    String sql = "INSERT INTO Ingredient (name, price, category) " +
                            "VALUES (?, ?, CAST(? AS ingredient_category)) RETURNING id";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, ingredient.getName());
                        stmt.setDouble(2, ingredient.getPrice());
                        stmt.setString(3, ingredient.getCategory().name());

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                ingredient.setId(rs.getInt("id"));
                            }
                        }
                    }
                } else {
                    String sql = "UPDATE Ingredient SET name=?, price=?, " +
                            "category=CAST(? AS ingredient_category) WHERE id=?";

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, ingredient.getName());
                        stmt.setDouble(2, ingredient.getPrice());
                        stmt.setString(3, ingredient.getCategory().name());
                        stmt.setInt(4, ingredient.getId());

                        int updated = stmt.executeUpdate();
                        if (updated == 0) {
                            throw new RuntimeException("Ingredient.id=" + ingredient.getId() + " is not found");
                        }
                    }
                }

                // Sauvegarde des mouvements de stock
                if (ingredient.getStockMovementList() != null) {
                    String sqlMovement =
                            "INSERT INTO StockMovement (id, id_ingredient, quantity, type, unit, creation_datetime) " +
                                    "VALUES (?, ?, ?, CAST(? AS mouvement_type), CAST(? AS unit_type), ?) " +
                                    "ON CONFLICT (id) DO NOTHING";

                    String sqlMovementNoId =
                            "INSERT INTO StockMovement (id_ingredient, quantity, type, unit, creation_datetime) " +
                                    "VALUES (?, ?, CAST(? AS mouvement_type), CAST(? AS unit_type), ?)";

                    for (StockMovement movement : ingredient.getStockMovementList()) {
                        if (movement.getId() != null) {
                            try (PreparedStatement stmt = conn.prepareStatement(sqlMovement)) {
                                stmt.setInt(1, movement.getId());
                                stmt.setInt(2, ingredient.getId());
                                stmt.setDouble(3, movement.getValue().getQuantity());
                                stmt.setString(4, movement.getType().name());
                                stmt.setString(5, movement.getValue().getUnit().name());
                                stmt.setTimestamp(6, Timestamp.from(movement.getCreationDatetime()));
                                stmt.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement stmt = conn.prepareStatement(sqlMovementNoId)) {
                                stmt.setInt(1, ingredient.getId());
                                stmt.setDouble(2, movement.getValue().getQuantity());
                                stmt.setString(3, movement.getType().name());
                                stmt.setString(4, movement.getValue().getUnit().name());
                                stmt.setTimestamp(5, Timestamp.from(movement.getCreationDatetime()));
                                stmt.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la sauvegarde de l'ingredient", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur de connexion", e);
        }
        return ingredient;
    }

    // findStockMovements (usage interne)
    private List<StockMovement> findStockMovements(Integer ingredientId) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT * FROM StockMovement WHERE id_ingredient = ? ORDER BY creation_datetime ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ingredientId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StockValue value = new StockValue(
                            rs.getDouble("quantity"),
                            UnitTypeEnum.valueOf(rs.getString("unit"))
                    );
                    movements.add(new StockMovement(
                            rs.getInt("id"),
                            value,
                            MovementTypeEnum.valueOf(rs.getString("type")),
                            rs.getTimestamp("creation_datetime").toInstant()
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des mouvements de stock", e);
        }
        return movements;
    }

    // mapIngredient (usage interne)
    private Ingredient mapIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                CategoryEnum.valueOf(rs.getString("category"))
        );
    }
}