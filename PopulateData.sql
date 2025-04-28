# Evan Schwartz & Wilson Murphy
USE PizzaDB;
SET @SKIP_INVENTORY_UPDATE = 1;

INSERT INTO topping
    (topping_TopName, topping_CustPrice, topping_BusPrice, topping_CurINVT, topping_MinINVT, topping_SmallAMT,topping_MedAMT,topping_LgAMT,topping_XLAMT)
VALUES
    ('Pepperoni', 1.25, 0.20, 100, 50, 2.00, 2.75, 3.50, 4.50),
    ('Sausage', 1.25, 0.15, 100, 50, 2.50, 3.00, 3.50, 4.25),
    ('Ham', 1.50, 0.15, 78, 25, 2.00, 2.50, 3.25, 4.00),
    ('Chicken', 1.75, 0.25, 56, 25, 1.50, 2.00, 2.25, 3.00),
    ('Green Pepper', 0.50, 0.02, 79, 25, 1.00, 1.50, 2.00, 2.50),
    ('Onion', 0.50, 0.02, 85, 25, 1.00, 1.50, 2.00, 2.75),
    ('Roma Tomato', 0.75, 0.03, 86, 10, 2.00, 3.00, 3.50, 4.50),
    ('Mushrooms', 0.75, 0.10, 52, 50, 1.50, 2.00, 2.50, 3.00),
    ('Black Olives', 0.60, 0.10, 39, 25, 0.75, 1.00, 1.50, 2.00),
    ('Pineapple', 1.00, 0.25, 15, 0, 1.00, 1.25, 1.75, 2.00),
    ('Jalapenos', 0.50, 0.05, 64, 0, 0.50, 0.75, 1.25, 1.75),
    ('Banana Peppers', 0.50, 0.05, 36, 0, 0.60, 1.00, 1.30, 1.75),
    ('Regular Cheese', 0.50, 0.12, 250, 50, 2.00, 3.50, 5.00, 7.00),
    ('Four Cheese Blend', 1.00, 0.15, 150, 25, 2.00, 3.50, 5.00, 7.00),
    ('Feta Cheese', 1.50, 0.18, 75, 0, 1.75, 3.00, 4.00, 5.50),
    ('Goat Cheese', 1.50, 0.20, 54, 0, 1.60, 2.75, 4.00, 5.50),
    ('Bacon', 1.50, 0.25, 89, 0, 1.00, 1.50, 2.00, 3.00);

INSERT INTO discount
    (discount_DiscountName, discount_Amount, discount_IsPercent)
VALUES
    ('Employee', 15.00, TRUE),
    ('Lunch Special Medium', 1.00, FALSE),
    ('Lunch Special Large', 2.00, FALSE),
    ('Specialty Pizza', 1.50, FALSE),
    ('Happy Hour', 10.00, TRUE),
    ('Gameday Special', 20.00, TRUE);

INSERT INTO baseprice
    (baseprice_Size, basePrice_CrustType, baseprice_CustPrice, baseprice_BusPrice)
VALUES
    ('Small', 'Thin', 3.00, 0.50),
    ('Small', 'Original', 3.00, 0.75),
    ('Small', 'Pan', 3.50, 1.00),
    ('Small', 'Gluten-Free', 4.00, 2.00),
    ('Medium', 'Thin', 5.00, 1.00),
    ('Medium', 'Original', 5.00, 1.50),
    ('Medium', 'Pan', 6.00, 2.25),
    ('Medium', 'Gluten-Free', 6.25, 3.00),
    ('Large', 'Thin', 8.00, 1.25),
    ('Large', 'Original', 8.00, 2.00),
    ('Large', 'Pan', 9.00, 3.00),
    ('Large', 'Gluten-Free', 9.50, 4.00),
    ('XLarge', 'Thin', 10.00, 2.00),
    ('XLarge', 'Original', 10.00, 3.00),
    ('XLarge', 'Pan', 11.50, 4.50),
    ('XLarge', 'Gluten-Free', 12.50, 6.00);



-- For dine-in orders we use a dummy customer record.
INSERT INTO customer
    (customer_FName, customer_LName, customer_PhoneNum)
VALUES
    ('Andrew', 'Wilkes-Krier', '8642545861'),
    -- CustomerID 1
    ('Matt', 'Engers', '8644749953'),
    -- CustomerID 2
    ('Frank', 'Turner', '8642328944'),
    -- CustomerID 3
    ('Milo', 'Auckerman', '8648785679');
-- CustomerID 4

# Order #1 – Dine-in Order
INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (NULL, 'dinein', '2025-03-05 12:03:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 1.
INSERT INTO dinein
    (ordertable_OrderID, dinein_TableNum)
VALUES
    (1, 21);
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Large', 'Thin', 'Completed', '2025-03-05 12:03:00', 19.75, 3.68, 1);
-- PizzaID assumed to be 1.
INSERT INTO pizza_discount
    (pizza_PizzaID, discount_DiscountID)
VALUES
    (1, 3);
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (1, 13, 1),
    -- Regular Cheese (extra) [ToppingID 13]
    (1, 1, 0),
    -- Pepperoni [ToppingID 1]
    (1, 2, 0);
-- Sausage [ToppingID 2]

# Order #2 – Dine-in Order

INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (NULL, 'dinein', '2025-04-03 12:05:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 2.
INSERT INTO dinein
    (ordertable_OrderID, dinein_TableNum)
VALUES
    (2, 4);
-- Pizza 2A:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Medium', 'Pan', 'Completed', '2025-04-03 12:05:00', 12.85, 3.23, 2);
-- PizzaID assumed to be 2.
INSERT INTO pizza_discount
    (pizza_PizzaID, discount_DiscountID)
VALUES
    (2, 2),
    (2, 4);
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (2, 15, 0),
    -- Feta Cheese [ToppingID 15]
    (2, 9, 0),
    -- Black Olives [ToppingID 9]
    (2, 7, 0),
    -- Roma Tomato [ToppingID 7]
    (2, 8, 0),
    -- Mushrooms [ToppingID 8]
    (2, 12, 0);
-- Banana Peppers [ToppingID 12]
-- Pizza 2B:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Small', 'Original', 'Completed', '2025-04-03 12:05:00', 6.93, 1.40, 2);
-- PizzaID assumed to be 3.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (3, 13, 0),
    -- Regular Cheese [ToppingID 13]
    (3, 4, 0),
    -- Chicken [ToppingID 4]
    (3, 12, 0);
-- Banana Peppers [ToppingID 12]

# Order #3 – Pickup Order
INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (2, 'pickup', '2025-03-03 21:30:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 3.
INSERT INTO pickup
    (ordertable_OrderID, pickup_IsPickedUp)
VALUES
    (3, TRUE);
-- Insert 6 pizzas for order #3.
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3),
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3),
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3),
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3),
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3),
    ('Large', 'Original', 'Completed', '2025-03-03 21:30:00', 14.88, 3.30, 3);
-- Assume these pizzas receive PizzaIDs 4 through 9.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (4, 13, 0),
    (4, 1, 0),
    (5, 13, 0),
    (5, 1, 0),
    (6, 13, 0),
    (6, 1, 0),
    (7, 13, 0),
    (7, 1, 0),
    (8, 13, 0),
    (8, 1, 0),
    (9, 13, 0),
    (9, 1, 0);



# Order #4 – Delivery Order

INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (2, 'delivery', '2025-04-20 19:11:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 4.
INSERT INTO delivery
    (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered)
VALUES
    (4, 115, 'Party Blvd', 'Anderson', 'SC', 29621, TRUE);
-- Pizza 4A:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('XLarge', 'Original', 'Completed', '2025-04-20 19:11:00', 27.94, 9.19, 4);
-- Assume PizzaID = 10.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (10, 1, 0),
    (10, 2, 0),
    (10, 14, 0);
-- Pepperoni, Sausage, Four Cheese Blend
-- Pizza 4B:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('XLarge', 'Original', 'Completed', '2025-04-20 19:11:00', 31.50, 6.25, 4);
-- Assume PizzaID = 11.
INSERT INTO pizza_discount
    (pizza_PizzaID, discount_DiscountID)
VALUES
    (11, 4);
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (11, 3, 1),
    (11, 10, 1),
    (11, 14, 0);
-- Ham (extra), Pineapple (extra), Four Cheese Blend
-- Pizza 4C:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('XLarge', 'Original', 'Completed', '2025-04-20 19:11:00', 26.75, 5.55, 4);
-- Assume PizzaID = 12.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (12, 4, 0),
    (12, 17, 0),
    (12, 14, 0);
-- Chicken, Bacon, Four Cheese Blend
-- Order-level discount:
INSERT INTO order_discount
    (ordertable_OrderID, discount_DiscountID)
VALUES
    (4, 6);


# Order #5 – Pickup Order
INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (3, 'pickup', '2025-03-02 17:30:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 5.
INSERT INTO pickup
    (ordertable_OrderID, pickup_IsPickedUp)
VALUES
    (5, TRUE);
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('XLarge', 'Gluten-Free', 'Completed', '2025-03-02 17:30:00', 28.70, 7.84, 5);
-- Assume PizzaID = 13.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (13, 5, 0),
    -- Green Pepper
    (13, 6, 0),
    -- Onion
    (13, 7, 0),
    -- Roma Tomato
    (13, 8, 0),
    -- Mushrooms
    (13, 9, 0),
    -- Black Olives
    (13, 16, 0);
-- Goat Cheese
INSERT INTO pizza_discount
    (pizza_PizzaID, discount_DiscountID)
VALUES
    (13, 4);



# Order #6 – Delivery Order
INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (4, 'delivery', '2025-03-02 18:17:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 6.
INSERT INTO delivery
    (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered)
VALUES
    (6, 6745, 'Wessex St', 'Anderson', 'SC', 29621, TRUE);
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Large', 'Thin', 'Completed', '2025-03-02 18:17:00', 25.81, 3.64, 6);
-- Assume PizzaID = 14.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (14, 4, 0),
    (14, 5, 0),
    (14, 6, 0),
    (14, 8, 0),
    (14, 14, 1);
-- Toppings: Chicken, Green Pepper, Onion, Mushrooms, Four Cheese Blend (extra)


# Order #7 – Delivery Order

INSERT INTO ordertable
    (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_IsComplete)
VALUES
    (4, 'delivery', '2025-04-13 20:32:00', 0.00, 0.00, TRUE);
-- OrderTableID assumed to be 7.
INSERT INTO delivery
    (ordertable_OrderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered)
VALUES
    (7, 8879, 'Suburban', 'Anderson', 'SC', 29621, TRUE);
-- Pizza 7A:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Large', 'Thin', 'Completed', '2025-04-13 20:32:00', 18.00, 2.75, 7);
-- Assume PizzaID = 15.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (15, 14, 1);
-- Four Cheese Blend (extra)
-- Pizza 7B:
INSERT INTO pizza
    (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID)
VALUES
    ('Large', 'Thin', 'Completed', '2025-04-13 20:32:00', 19.25, 3.25, 7);
-- Assume PizzaID = 16.
INSERT INTO pizza_topping
    (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble)
VALUES
    (16, 13, 0),
    -- Regular Cheese
    (16, 1, 1);
-- Pepperoni (extra)
INSERT INTO order_discount
    (ordertable_OrderID, discount_DiscountID)
VALUES
    (7, 1);

SET @SKIP_INVENTORY_UPDATE = 0;