# Evan Schwartz & Wilson Murphy

USE PizzaDB;

CREATE VIEW ToppingPopularity AS
SELECT
    t.topping_TopName AS Topping,
    COALESCE(pt.TotalCount, 0) AS ToppingCount
FROM topping t
LEFT JOIN (
    SELECT
        topping_TopID,
        SUM(CASE WHEN pizza_topping_IsDouble = 1 THEN 2 ELSE 1 END) AS TotalCount
    FROM pizza_topping
    GROUP BY topping_TopID
) pt ON t.topping_TopID = pt.topping_TopID
ORDER BY ToppingCount DESC, Topping;


CREATE VIEW ProfitByPizza AS
SELECT
    p.pizza_Size AS Size,
    p.pizza_CrustType AS Crust,
    SUM(p.pizza_CustPrice - p.pizza_BusPrice) AS Profit,
    DATE_FORMAT(p.pizza_PizzaDate, '%c/%Y') AS OrderMonth
FROM pizza p
GROUP BY OrderMonth, p.pizza_Size, p.pizza_CrustType
ORDER BY Profit;

CREATE VIEW ProfitByOrderType AS
SELECT customerType, OrderMonth, TotalOrderPrice, TotalOrderCost, Profit
FROM (
    SELECT
        ordertable_OrderType AS customerType,
        DATE_FORMAT(ordertable_OrderDateTime, '%c/%Y') AS OrderMonth,
        SUM(ordertable_CustPrice) AS TotalOrderPrice,
        SUM(ordertable_BusPrice) AS TotalOrderCost,
        SUM(ordertable_CustPrice - ordertable_BusPrice) AS Profit,
        0 AS sort_order
    FROM ordertable
    GROUP BY ordertable_OrderType, DATE_FORMAT(ordertable_OrderDateTime, '%c/%Y')

    UNION ALL

    SELECT
        '' AS customerType,
        'Grand Total' AS OrderMonth,
        SUM(ordertable_CustPrice) AS TotalOrderPrice,
        SUM(ordertable_BusPrice) AS TotalOrderCost,
        SUM(ordertable_CustPrice - ordertable_BusPrice) AS Profit,
        1 AS sort_order
    FROM ordertable
) AS t
ORDER BY sort_order,
    FIELD(customerType, 'dinein', 'pickup', 'delivery', ''),
    Profit DESC;
