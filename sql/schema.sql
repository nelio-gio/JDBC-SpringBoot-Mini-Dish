GRANT ALL ON SCHEMA public TO mini_dish_db_manager;

-- TYPES ENUM
CREATE TYPE dish_type AS ENUM ('START', 'MAIN', 'DESSERT');
CREATE TYPE ingredient_category AS ENUM ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');
CREATE TYPE unit_type AS ENUM ('PCS', 'KG', 'L');
CREATE TYPE mouvement_type AS ENUM ('IN', 'OUT');

-- TABLES
CREATE TABLE Dish (
                      id            SERIAL PRIMARY KEY,
                      name          VARCHAR(255)  NOT NULL,
                      dish_type     dish_type     NOT NULL,
                      selling_price NUMERIC
);

CREATE TABLE Ingredient (
                            id       SERIAL PRIMARY KEY,
                            name     VARCHAR(255)         NOT NULL,
                            price    NUMERIC              NOT NULL,
                            category ingredient_category  NOT NULL
);

CREATE TABLE DishIngredient (
                                id                SERIAL PRIMARY KEY,
                                id_dish           INT       NOT NULL REFERENCES Dish(id),
                                id_ingredient     INT       NOT NULL REFERENCES Ingredient(id),
                                quantity_required NUMERIC   NOT NULL,
                                unit              unit_type NOT NULL
);

CREATE TABLE StockMovement (
                               id                SERIAL PRIMARY KEY,
                               id_ingredient     INT            NOT NULL REFERENCES Ingredient(id),
                               quantity          NUMERIC        NOT NULL,
                               type              mouvement_type NOT NULL,
                               unit              unit_type      NOT NULL,
                               creation_datetime TIMESTAMP      NOT NULL
);

-- Droits sur toutes les tables et séquences
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO mini_dish_db_manager;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mini_dish_db_manager;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO mini_dish_db_manager;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO mini_dish_db_manager;