# Evan Schwartz & Wilson Murphy

USE PizzaDB;

DELIMITER $$


DROP PROCEDURE IF EXISTS sp_InsertOrder$$
CREATE PROCEDURE sp_InsertOrder(
    IN p_customerID    INT,
    IN p_orderType     VARCHAR(30),
    IN p_orderDateTime DATETIME,
    IN p_custPrice     DECIMAL(10,2),
    IN p_busPrice      DECIMAL(10,2),
    IN p_tableNum      INT,
    IN p_houseNum      INT,
    IN p_street        VARCHAR(50),
    IN p_city          VARCHAR(50),
    IN p_state         VARCHAR(2),
    IN p_zip           INT,
    IN p_isPickedUp    BOOLEAN
)
BEGIN
    DECLARE newOrderID INT;

INSERT INTO ordertable
(customer_CustID, ordertable_OrderType, ordertable_OrderDateTime,
 ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES (p_customerID, p_orderType, p_orderDateTime, 0, 0, TRUE);

SET newOrderID = LAST_INSERT_ID();

    IF p_orderType = 'dinein' THEN
        INSERT INTO dinein(ordertable_OrderID, dinein_TableNum)
        VALUES(newOrderID, p_tableNum);

    ELSEIF p_orderType = 'delivery' THEN
        INSERT INTO delivery(ordertable_OrderID, delivery_HouseNum,
                             delivery_Street,  delivery_City,
                             delivery_State,   delivery_Zip,
                             delivery_IsDelivered)
        VALUES(newOrderID, p_houseNum, p_street, p_city, p_state, p_zip, TRUE);

    ELSEIF p_orderType = 'pickup' THEN
        INSERT INTO pickup(ordertable_OrderID, pickup_IsPickedUp)
        VALUES(newOrderID, p_isPickedUp);
END IF;
END$$


DROP PROCEDURE IF EXISTS sp_InsertPizza$$
CREATE PROCEDURE sp_InsertPizza(
    IN p_size      VARCHAR(30),
    IN p_crustType VARCHAR(30),
    IN p_state     VARCHAR(30),
    IN p_date      DATETIME,
    IN p_custPrice DECIMAL(5,2),
    IN p_busPrice  DECIMAL(5,2),
    IN p_orderID   INT
)
BEGIN
INSERT INTO pizza(pizza_Size, pizza_CrustType, pizza_PizzaState,
                  pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice,
                  ordertable_OrderID)
VALUES(p_size, p_crustType, p_state, p_date, p_custPrice, p_busPrice, p_orderID);
END$$



DROP FUNCTION IF EXISTS fn_GetToppingUsage$$
CREATE FUNCTION fn_GetToppingUsage(p_toppingID INT, p_size VARCHAR(30))
    RETURNS DECIMAL(5,2)
    DETERMINISTIC
BEGIN
    DECLARE tusage DECIMAL(5,2);

SELECT CASE
           WHEN p_size = 'Small'  THEN topping_SmallAMT
           WHEN p_size = 'Medium' THEN topping_MedAMT
           WHEN p_size = 'Large'  THEN topping_LgAMT
           WHEN p_size = 'XLarge' THEN topping_XLAMT
           ELSE 0
           END
INTO tusage
FROM topping
WHERE topping_TopID = p_toppingID;

RETURN tusage;
END$$


DROP FUNCTION IF EXISTS fn_CalcProfit$$
CREATE FUNCTION fn_CalcProfit(p_custPrice DECIMAL(10,2),
                              p_busPrice  DECIMAL(10,2))
    RETURNS DECIMAL(10,2)
    DETERMINISTIC
BEGIN
RETURN p_custPrice - p_busPrice;
END$$



-- Make sure the delimiter is still $$ when you run this section
DROP TRIGGER IF EXISTS trg_BeforeInsertPizzaTopping$$
CREATE TRIGGER trg_BeforeInsertPizzaTopping          -- <-- this line was missing
    BEFORE INSERT ON pizza_topping
    FOR EACH ROW
BEGIN
    DECLARE pizzaSize VARCHAR(30);
    DECLARE baseUsage DECIMAL(5,2);

    /* allow Java to bypass the trigger by setting a session variable */
    IF @SKIP_INVENTORY_UPDATE IS NULL OR @SKIP_INVENTORY_UPDATE = 0 THEN

    SELECT pizza_Size
    INTO pizzaSize
    FROM pizza
    WHERE pizza_PizzaID = NEW.pizza_PizzaID;

    SET baseUsage = fn_GetToppingUsage(NEW.topping_TopID, pizzaSize);

        IF NEW.pizza_topping_IsDouble = 2 THEN
            SET baseUsage = baseUsage * 2;
END IF;

UPDATE topping
SET topping_CurINVT = topping_CurINVT - baseUsage
WHERE topping_TopID   = NEW.topping_TopID;
END IF;
END$$



DROP TRIGGER IF EXISTS trg_AfterInsertPizza$$
CREATE TRIGGER trg_AfterInsertPizza
    AFTER INSERT ON pizza
    FOR EACH ROW
BEGIN
    UPDATE ordertable
    SET ordertable_CustPrice = ordertable_CustPrice + NEW.pizza_CustPrice,
        ordertable_BusPrice  = ordertable_BusPrice  + NEW.pizza_BusPrice
    WHERE ordertable_OrderID   = NEW.ordertable_OrderID;
    END$$


    DROP TRIGGER IF EXISTS trg_AfterUpdatePizza$$
    CREATE TRIGGER trg_AfterUpdatePizza
        AFTER UPDATE ON pizza
        FOR EACH ROW
    BEGIN
        UPDATE ordertable
        SET ordertable_CustPrice = ordertable_CustPrice - OLD.pizza_CustPrice + NEW.pizza_CustPrice,
            ordertable_BusPrice  = ordertable_BusPrice  - OLD.pizza_BusPrice  + NEW.pizza_BusPrice
        WHERE ordertable_OrderID   = NEW.ordertable_OrderID;
        END$$


        DROP TRIGGER IF EXISTS trg_BeforeUpdateTopping$$
        CREATE TRIGGER trg_BeforeUpdateTopping
            BEFORE UPDATE ON topping
            FOR EACH ROW
        BEGIN
            IF NEW.topping_CurINVT < NEW.topping_MinINVT THEN
        SET NEW.topping_CurINVT = NEW.topping_MinINVT;
        END IF;
        END$$


        DROP TRIGGER IF EXISTS trg_AfterInsertOrderDiscount$$
        CREATE TRIGGER trg_AfterInsertOrderDiscount
            AFTER INSERT ON order_discount
            FOR EACH ROW
        BEGIN
            DECLARE disc_amount   DECIMAL(10,2);
    DECLARE is_a_percent  BOOLEAN;

            SELECT discount_Amount, discount_IsPercent
            INTO disc_amount, is_a_percent
            FROM discount
            WHERE discount_DiscountID = NEW.discount_DiscountID;

            IF is_a_percent THEN
            UPDATE ordertable
            SET ordertable_CustPrice = ordertable_CustPrice * (1 - (disc_amount / 100))
            WHERE ordertable_OrderID   = NEW.ordertable_OrderID;
            ELSE
            UPDATE ordertable
            SET ordertable_CustPrice = ordertable_CustPrice - disc_amount
            WHERE ordertable_OrderID   = NEW.ordertable_OrderID;
        END IF;
        END$$


        DELIMITER ;
