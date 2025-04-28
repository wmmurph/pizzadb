# Evan Schwartz & Wilson Murphy

DROP SCHEMA IF EXISTS PizzaDB;
CREATE SCHEMA PizzaDB;
USE PizzaDB;

DROP USER IF EXISTS 'dbtester';
CREATE USER 'dbtester' IDENTIFIED BY 'CPSC4620';
GRANT ALL ON PizzaDB.* TO 'dbtester';

CREATE TABLE baseprice (
    baseprice_Size VARCHAR(30),
    basePrice_CrustType VARCHAR(30),
    baseprice_CustPrice DECIMAL(5,2) NOT NULL ,
    baseprice_BusPrice DECIMAL(5,2) NOT NULL,
    PRIMARY KEY(baseprice_Size,basePrice_CrustType)
);

CREATE TABLE topping (
    topping_TopID INT AUTO_INCREMENT PRIMARY KEY,
    topping_TopName VARCHAR(30) NOT NULL ,
    topping_SmallAMT DECIMAL(5,2) NOT NULL,
    topping_MedAMT DECIMAL(5,2) NOT NULL,
    topping_LgAMT DECIMAL(5,2) NOT NULL,
    topping_XLAMT DECIMAL(5,2) NOT NULL,
    topping_CustPrice DECIMAL(5,2) NOT NULL,
    topping_BusPrice DECIMAL(5,2) NOT NULL,
    topping_MinINVT INT NOT NULL,
    topping_CurINVT INT NOT NULL
);

CREATE TABLE discount (
    discount_DiscountID INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    discount_DiscountName VARCHAR(30) NOT NULL,
    discount_Amount DECIMAL(5,2) NOT NULL,
    discount_IsPercent Boolean NOT NULL
);

CREATE TABLE customer (
    customer_CustID INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    customer_FName VARCHAR(30) NOT NULL,
    customer_LName VARCHAR(30) NOT NULL,
    customer_PhoneNum VARCHAR(30) NOT NULL
);

CREATE TABLE ordertable (
    ordertable_OrderID INT AUTO_INCREMENT PRIMARY KEY,
    customer_CustID INT NULL,
    ordertable_OrderType VARCHAR(30) NOT NULL,
    ordertable_OrderDateTime DATETIME NOT NULL,
    ordertable_CustPrice DECIMAL(5,2) NOT NULL,
    ordertable_BusPrice DECIMAL(5,2) NOT NULL,
    ordertable_IsComplete TINYINT(1) NULL DEFAULT 0,
    FOREIGN KEY(customer_CustID) REFERENCES customer(customer_CustID)
);


CREATE TABLE pizza (
    pizza_PizzaID INT AUTO_INCREMENT PRIMARY KEY,
    pizza_Size VARCHAR(30) NOT NULL,
    pizza_CrustType VARCHAR(30) NOT NULL,
    pizza_PizzaState VARCHAR(30) NOT NULL,
    pizza_PizzaDate DATETIME NOT NULL,
    pizza_CustPrice DECIMAL(5,2) NOT NULL,
    pizza_BusPrice DECIMAL(5,2) NOT NULL,
    ordertable_OrderID INT NOT NULL,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID),
    FOREIGN KEY(pizza_Size, pizza_CrustType) REFERENCES baseprice(baseprice_Size, basePrice_CrustType)
);

CREATE TABLE pizza_topping (
    pizza_PizzaID INT,
    topping_TopID INT,
    pizza_topping_IsDouble INT NOT NULL,
    PRIMARY KEY(pizza_PizzaID, topping_TopID),
    FOREIGN KEY(pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY(topping_TopID) REFERENCES topping(topping_TopID)
);

CREATE TABLE pizza_discount (
    pizza_PizzaID INT,
    discount_DiscountID INT NOT NULL,
    PRIMARY KEY(pizza_PizzaID,discount_DiscountID),
    FOREIGN KEY(pizza_PizzaID) REFERENCES pizza(pizza_PizzaID),
    FOREIGN KEY(discount_DiscountID) REFERENCES discount(discount_DiscountID)
);

CREATE TABLE order_discount (
    ordertable_OrderID INT,
    discount_DiscountID INT,
    PRIMARY KEY(ordertable_OrderID,discount_DiscountID),
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID),
    FOREIGN KEY(discount_DiscountID) REFERENCES discount(discount_DiscountID)
);

CREATE TABLE pickup (
    ordertable_OrderID INT PRIMARY KEY,
    pickup_IsPickedUp BOOLEAN DEFAULT 0 NOT NULL,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE delivery (
    ordertable_OrderID INT PRIMARY KEY,
    delivery_HouseNum INT NOT NULL,
    delivery_Street VARCHAR(30) NOT NULL,
    delivery_City VARCHAR(30) NOT NULL,
    delivery_State VARCHAR(2) NOT NULL,
    delivery_Zip INT NOT NULL,
    delivery_IsDelivered BOOLEAN DEFAULT 0 NOT NULL,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);

CREATE TABLE dinein (
    ordertable_OrderID INT PRIMARY KEY,
    dinein_TableNum INT NOT NULL,
    FOREIGN KEY(ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID)
);
