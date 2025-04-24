package cpsc4620;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/*
 * This file is where you will implement the methods needed to support this application.
 * You will write the code to retrieve and save information to the database and use that
 * information to build the various objects required by the applicaiton.
 *
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 *
 * This class also has static string variables for pickup, delivery and dine-in.
 * DO NOT change these constant values.
 *
 * You can add any helper methods you need, but you must implement all the methods
 * in this class and use them to complete the project.  The autograder will rely on
 * these methods being implemented, so do not delete them or alter their method
 * signatures.
 *
 * Make sure you properly open and close your DB connections in any method that
 * requires access to the DB.
 * Use the connect_to_db below to open your connection in DBConnector.
 * What is opened must be closed!
 */

/*
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// DO NOT change these variables!
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	public enum order_state {
		PREPARED,
		DELIVERED,
		PICKEDUP
	}


	private static boolean connect_to_db() throws SQLException, IOException
	{

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	public static void addOrder(Order o) throws SQLException, IOException
	{
		/*
		 * add code to add the order to the DB. Remember that we're not just
		 * adding the order to the order DB table, but we're also recording
		 * the necessary data for the delivery, dinein, pickup, pizzas, toppings
		 * on pizzas, order discounts and pizza discounts.
		 *
		 * This is a KEY method as it must store all the data in the Order object
		 * in the database and make sure all the tables are correctly linked.
		 *
		 * Remember, if the order is for Dine In, there is no customer...
		 * so the cusomter id coming from the Order object will be -1.
		 *
		 */
		connect_to_db();
		try {
			String orderSQL = "INSERT INTO ordertable (customer_CustID, ordertable_OrderType, " +
					"ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, " +
					"ordertable_IsComplete) VALUES (?, ?, ?, ?, ?, ?)";
			PreparedStatement orderStmt = conn.prepareStatement(orderSQL, Statement.RETURN_GENERATED_KEYS);
			if (o.getOrderType().equals(DBNinja.dine_in)) {
				orderStmt.setNull(1, java.sql.Types.INTEGER);
			} else {
				orderStmt.setInt(1, o.getCustID());
			}
			orderStmt.setString(2, o.getOrderType());
			orderStmt.setString(3, o.getDate());
			orderStmt.setDouble(4, o.getCustPrice());
			orderStmt.setDouble(5, o.getBusPrice());
			orderStmt.setBoolean(6, o.getIsComplete());
			orderStmt.executeUpdate();
			int orderID = -1;
			ResultSet keys = orderStmt.getGeneratedKeys();
			if (keys.next()) {
				orderID = keys.getInt(1);
				o.setOrderID(orderID);
			}
			keys.close();
			orderStmt.close();
            switch (o.getOrderType()) {
				case DBNinja.dine_in:
                    DineinOrder dineIn = (DineinOrder) o;
                    String dineInSQL = "INSERT INTO dinein (ordertable_OrderID, dinein_TableNum) VALUES (?, ?)";
                    PreparedStatement dineInStmt = conn.prepareStatement(dineInSQL);
                    dineInStmt.setInt(1, orderID);
                    dineInStmt.setInt(2, dineIn.getTableNum());
                    dineInStmt.executeUpdate();
                    dineInStmt.close();
                break;
				case DBNinja.delivery:
                    DeliveryOrder delivery = (DeliveryOrder) o;
                    String[] addressParts = delivery.getAddress().split("\t");
                    int houseNum = Integer.parseInt(addressParts[0]);
                    String street = addressParts[1];
                    String city = addressParts[2];
                    String state = addressParts[3];
                    int zip = Integer.parseInt(addressParts[4]);
                    String deliverySQL = "INSERT INTO delivery (ordertable_OrderID, delivery_HouseNum, " +
                            "delivery_Street, delivery_City, delivery_State, delivery_Zip, " +
                            "delivery_IsDelivered) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement deliveryStmt = conn.prepareStatement(deliverySQL);
                    deliveryStmt.setInt(1, orderID);
                    deliveryStmt.setInt(2, houseNum);
                    deliveryStmt.setString(3, street);
                    deliveryStmt.setString(4, city);
                    deliveryStmt.setString(5, state);
                    deliveryStmt.setInt(6, zip);
                    deliveryStmt.setBoolean(7, false);
                    deliveryStmt.executeUpdate();
                    deliveryStmt.close();
                break;
				case DBNinja.pickup:
                    PickupOrder pickup = (PickupOrder) o;
                    String pickupSQL = "INSERT INTO pickup (ordertable_OrderID, pickup_IsPickedUp) VALUES (?, ?)";
                    PreparedStatement pickupStmt = conn.prepareStatement(pickupSQL);
                    pickupStmt.setInt(1, orderID);
                    pickupStmt.setBoolean(2, pickup.getIsPickedUp());
                    pickupStmt.executeUpdate();
                    pickupStmt.close();
                break;
            }
			java.util.Date orderDate = new java.util.Date();
			for (Pizza p : o.getPizzaList()) {
				p.setOrderID(orderID);
				addPizza(orderDate, orderID, p);
			}
			for (Discount d : o.getDiscountList()) {
				String discountSQL = "INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (?, ?)";
				PreparedStatement discountStmt = conn.prepareStatement(discountSQL);
				discountStmt.setInt(1, orderID);
				discountStmt.setInt(2, d.getDiscountID());
				discountStmt.executeUpdate();
				discountStmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}

	public static int addPizza(java.util.Date d, int orderID, Pizza p) throws SQLException, IOException
	{
		/*
		 * Add the code needed to insert the pizza into into the database.
		 * Keep in mind you must also add the pizza discounts and toppings
		 * associated with the pizza.
		 *
		 * NOTE: there is a Date object passed into this method so that the Order
		 * and ALL its Pizzas can be assigned the same DTS.
		 *
		 * This method returns the id of the pizza just added.
		 *
		 */
		connect_to_db();

		int pizzaID = -1;

		try {
			java.sql.Timestamp sqlDate = new java.sql.Timestamp(d.getTime());
			String insertPizzaSQL = "INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, " +
					"pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?)";

			PreparedStatement pizzaStmt = conn.prepareStatement(insertPizzaSQL, Statement.RETURN_GENERATED_KEYS);
			pizzaStmt.setString(1, p.getSize());
			pizzaStmt.setString(2, p.getCrustType());
			pizzaStmt.setString(3, p.getPizzaState());
			pizzaStmt.setTimestamp(4, sqlDate);
			pizzaStmt.setDouble(5, p.getCustPrice());
			pizzaStmt.setDouble(6, p.getBusPrice());
			pizzaStmt.setInt(7, orderID);

			pizzaStmt.executeUpdate();

			ResultSet keys = pizzaStmt.getGeneratedKeys();
			if (keys.next()) {
				pizzaID = keys.getInt(1);
				p.setPizzaID(pizzaID);
			}
			keys.close();
			pizzaStmt.close();
			for (Topping t : p.getToppings()) {
				boolean isDouble = t.getDoubled();
				String insertToppingSQL = "INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble) " +
						"VALUES (?, ?, ?)";
				PreparedStatement toppingStmt = conn.prepareStatement(insertToppingSQL);
				toppingStmt.setInt(1, pizzaID);
				toppingStmt.setInt(2, t.getTopID());
				toppingStmt.setInt(3, isDouble ? 1 : 0);
				toppingStmt.executeUpdate();
				toppingStmt.close();
			}
			for (Discount discount : p.getDiscounts()) {
				String insertDiscountSQL = "INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) " +
						"VALUES (?, ?)";
				PreparedStatement discountStmt = conn.prepareStatement(insertDiscountSQL);
				discountStmt.setInt(1, pizzaID);
				discountStmt.setInt(2, discount.getDiscountID());
				discountStmt.executeUpdate();
				discountStmt.close();
			}
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return pizzaID;
	}

	public static int addCustomer(Customer c) throws SQLException, IOException
	{
		/*
		 * This method adds a new customer to the database.
		 *
		 */

		connect_to_db();
		int newCustomerID = -1;
		String insertCustomer = "INSERT INTO customer " +
				"(customer_FName, customer_LName, customer_PhoneNum) " +
				"VALUES (?, ?, ?)";
		PreparedStatement ps = conn.prepareStatement(insertCustomer, Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, c.getFName());
		ps.setString(2, c.getLName());
		ps.setString(3, c.getPhone());
		int rowsAffected = ps.executeUpdate();
		if (rowsAffected > 0) {
			ResultSet keys = ps.getGeneratedKeys();
			if (keys.next()) {
				newCustomerID = keys.getInt(1);
				c.setCustID(newCustomerID);
			}
			keys.close();
		}
		ps.close();
		conn.close();
		return newCustomerID;
	}

	public static void completeOrder(int OrderID, order_state newState ) throws SQLException, IOException
	{
		/*
		 * Mark that order as complete in the database.
		 * Note: if an order is complete, this means all the pizzas are complete as well.
		 * However, it does not mean that the order has been delivered or picked up!
		 *
		 * For newState = PREPARED: mark the order and all associated pizza's as completed
		 * For newState = DELIVERED: mark the delivery status
		 * FOR newState = PICKEDUP: mark the pickup status
		 *
		 */
		connect_to_db();
		try {
			if (newState == order_state.PREPARED) {
				String completeOrderSQL = "UPDATE ordertable SET ordertable_IsComplete = TRUE WHERE ordertable_OrderID = ?";
				PreparedStatement orderStmt = conn.prepareStatement(completeOrderSQL);
				orderStmt.setInt(1, OrderID);
				orderStmt.executeUpdate();
				orderStmt.close();
				String completePizzaSQL = "UPDATE pizza SET pizza_PizzaState = 'Completed' WHERE ordertable_OrderID = ?";
				PreparedStatement pizzaStmt = conn.prepareStatement(completePizzaSQL);
				pizzaStmt.setInt(1, OrderID);
				pizzaStmt.executeUpdate();
				pizzaStmt.close();

			} else if (newState == order_state.DELIVERED) {
				String deliverySQL = "UPDATE delivery SET delivery_IsDelivered = TRUE WHERE ordertable_OrderID = ?";
				PreparedStatement deliveryStmt = conn.prepareStatement(deliverySQL);
				deliveryStmt.setInt(1, OrderID);
				deliveryStmt.executeUpdate();
				deliveryStmt.close();
			} else if (newState == order_state.PICKEDUP) {
				String pickupSQL = "UPDATE pickup SET pickup_IsPickedUp = TRUE WHERE ordertable_OrderID = ?";
				PreparedStatement pickupStmt = conn.prepareStatement(pickupSQL);
				pickupStmt.setInt(1, OrderID);
				pickupStmt.executeUpdate();
				pickupStmt.close();
			}
		} catch (SQLException e) {
		} finally {
			conn.close();
		}

	}


	public static ArrayList<Order> getOrders(int status) throws SQLException, IOException
	{
		/*
		 * Return an ArrayList of orders.
		 * 	status   == 1 => return a list of open (ie oder is not completed)
		 *           == 2 => return a list of completed orders (ie order is complete)
		 *           == 3 => return a list of all the orders
		 * Remember that in Java, we account for supertypes and subtypes
		 * which means that when we create an arrayList of orders, that really
		 * means we have an arrayList of dineinOrders, deliveryOrders, and pickupOrders.
		 *
		 * You must fully populate the Order object, this includes order discounts,
		 * and pizzas along with the toppings and discounts associated with them.
		 *
		 * Don't forget to order the data coming from the database appropriately.
		 *
		 */
		connect_to_db();

		ArrayList<Order> orders = new ArrayList<>();

		try {
			String sql;
			if (status == 1) {
				sql = "SELECT * FROM ordertable WHERE ordertable_IsComplete = FALSE ORDER BY ordertable_OrderDateTime";
			} else if (status == 2) {
				sql = "SELECT * FROM ordertable WHERE ordertable_IsComplete = TRUE ORDER BY ordertable_OrderDateTime";
			} else {
				sql = "SELECT * FROM ordertable ORDER BY ordertable_OrderDateTime";
			}
			Statement stmt = conn.createStatement();
			ResultSet orderData = stmt.executeQuery(sql);

			while (orderData.next()) {
				int orderID = orderData.getInt("ordertable_OrderID");
				int custID = orderData.getInt("customer_CustID");
				String orderType = orderData.getString("ordertable_OrderType");
				String date = orderData.getString("ordertable_OrderDateTime");
				double custPrice = orderData.getDouble("ordertable_CustPrice");
				double busPrice = orderData.getDouble("ordertable_BusPrice");
				boolean isComplete = orderData.getBoolean("ordertable_IsComplete");
				Order order = null;
                switch (orderType) {
					case DBNinja.dine_in:
                        String dineInSQL = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?";
                        PreparedStatement dineInStmt = conn.prepareStatement(dineInSQL);
                        dineInStmt.setInt(1, orderID);
                        ResultSet dineInData = dineInStmt.executeQuery();
                        if (dineInData.next()) {
                            int tableNum = dineInData.getInt("dinein_TableNum");
                            order = new DineinOrder(orderID, custID, date, custPrice, busPrice, isComplete, tableNum);
                        }
                        dineInData.close();
                        dineInStmt.close();
                    break;
					case DBNinja.delivery:
                        String deliverySQL = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                        PreparedStatement deliveryStmt = conn.prepareStatement(deliverySQL);
                        deliveryStmt.setInt(1, orderID);
                        ResultSet deliveryData = deliveryStmt.executeQuery();
                        if (deliveryData.next()) {
                            int houseNum = deliveryData.getInt("delivery_HouseNum");
                            String street = deliveryData.getString("delivery_Street");
                            String city = deliveryData.getString("delivery_City");
                            String state = deliveryData.getString("delivery_State");
                            int zip = deliveryData.getInt("delivery_Zip");
                            boolean isDelivered = deliveryData.getBoolean("delivery_IsDelivered");
                            String address = houseNum + "\t" + street + "\t" + city + "\t" + state + "\t" + zip;
                            order = new DeliveryOrder(orderID, custID, date, custPrice, busPrice, isComplete, isDelivered, address);
                        }
                        deliveryData.close();
                        deliveryStmt.close();
                    break;
					case DBNinja.pickup:
                        String pickupSQL = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?";
                        PreparedStatement pickupStmt = conn.prepareStatement(pickupSQL);
                        pickupStmt.setInt(1, orderID);
                        ResultSet pickupData = pickupStmt.executeQuery();
                        if (pickupData.next()) {
                            boolean isPickedUp = pickupData.getBoolean("pickup_IsPickedUp");
                            order = new PickupOrder(orderID, custID, date, custPrice, busPrice, isPickedUp, isComplete);
                        }
                        pickupData.close();
                        pickupStmt.close();
                    break;
                }
				if (order != null) {
					order.setPizzaList(getPizzas(order));
					order.setDiscountList(getDiscounts(order));
					orders.add(order);
				}
			}
			orderData.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return orders;
	}

	public static Order getLastOrder() throws SQLException, IOException
	{
		/*
		 * Query the database for the LAST order added
		 * then return an Order object for that order.
		 * NOTE...there will ALWAYS be a "last order"!
		 */
		connect_to_db();

		Order lastOrder = null;
		try {
			String orderSQL = "SELECT * FROM ordertable ORDER BY ordertable_OrderDateTime DESC LIMIT 1";
			Statement stmt = conn.createStatement();
			ResultSet orderData = stmt.executeQuery(orderSQL);
			if (orderData.next()) {
				int orderID = orderData.getInt("ordertable_OrderID");
				int custID = orderData.getInt("customer_CustID");
				String orderType = orderData.getString("ordertable_OrderType");
				String date = orderData.getString("ordertable_OrderDateTime");
				double custPrice = orderData.getDouble("ordertable_CustPrice");
				double busPrice = orderData.getDouble("ordertable_BusPrice");
				boolean isComplete = orderData.getBoolean("ordertable_IsComplete");
                switch (orderType) {
					case DBNinja.dine_in:
                        String dineInSQL = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?";
                        PreparedStatement dineInStmt = conn.prepareStatement(dineInSQL);
                        dineInStmt.setInt(1, orderID);
                        ResultSet dineInData = dineInStmt.executeQuery();
                        if (dineInData.next()) {
                            int tableNum = dineInData.getInt("dinein_TableNum");
                            lastOrder = new DineinOrder(orderID, custID, date, custPrice, busPrice, isComplete, tableNum);
                        }
                        dineInData.close();
                        dineInStmt.close();
                    break;
					case DBNinja.delivery:
                        String deliverySQL = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                        PreparedStatement deliveryStmt = conn.prepareStatement(deliverySQL);
                        deliveryStmt.setInt(1, orderID);
                        ResultSet deliveryData = deliveryStmt.executeQuery();
                        if (deliveryData.next()) {
                            int houseNum = deliveryData.getInt("delivery_HouseNum");
                            String street = deliveryData.getString("delivery_Street");
                            String city = deliveryData.getString("delivery_City");
                            String state = deliveryData.getString("delivery_State");
                            int zip = deliveryData.getInt("delivery_Zip");
                            boolean isDelivered = deliveryData.getBoolean("delivery_IsDelivered");
                            String address = houseNum + "\t" + street + "\t" + city + "\t" + state + "\t" + zip;
                            lastOrder = new DeliveryOrder(orderID, custID, date, custPrice, busPrice, isComplete, isDelivered, address);
                        }
                        deliveryData.close();
                        deliveryStmt.close();
                    break;
					case DBNinja.pickup:
                        String pickupSQL = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?";
                        PreparedStatement pickupStmt = conn.prepareStatement(pickupSQL);
                        pickupStmt.setInt(1, orderID);
                        ResultSet pickupData = pickupStmt.executeQuery();
                        if (pickupData.next()) {
                            boolean isPickedUp = pickupData.getBoolean("pickup_IsPickedUp");
                            lastOrder = new PickupOrder(orderID, custID, date, custPrice, busPrice, isPickedUp, isComplete);
                        }
                        pickupData.close();
                        pickupStmt.close();
                    break;
                }
				if (lastOrder != null) {
					lastOrder.setPizzaList(getPizzas(lastOrder));
					lastOrder.setDiscountList(getDiscounts(lastOrder));
				}
			}
			orderData.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return lastOrder;
	}

	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException
	{
		/*
		 * Query the database for ALL the orders placed on a specific date
		 * and return a list of those orders.
		 *
		 */
		connect_to_db();
		ArrayList<Order> dateOrders = new ArrayList<>();
		try {
			String orderSQL = "SELECT * FROM ordertable WHERE DATE(ordertable_OrderDateTime) = ? " +
					"ORDER BY ordertable_OrderDateTime";
			PreparedStatement stmt = conn.prepareStatement(orderSQL);
			stmt.setString(1, date);
			ResultSet orderData = stmt.executeQuery();
			while (orderData.next()) {
				int orderID = orderData.getInt("ordertable_OrderID");
				int custID = orderData.getInt("customer_CustID");
				String orderType = orderData.getString("ordertable_OrderType");
				String orderDate = orderData.getString("ordertable_OrderDateTime");
				double custPrice = orderData.getDouble("ordertable_CustPrice");
				double busPrice = orderData.getDouble("ordertable_BusPrice");
				boolean isComplete = orderData.getBoolean("ordertable_IsComplete");
				Order order = null;
                switch (orderType) {
					case DBNinja.dine_in:
                        String dineInSQL = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?";
                        PreparedStatement dineInStmt = conn.prepareStatement(dineInSQL);
                        dineInStmt.setInt(1, orderID);
                        ResultSet dineInData = dineInStmt.executeQuery();
                        if (dineInData.next()) {
                            int tableNum = dineInData.getInt("dinein_TableNum");
                            order = new DineinOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, tableNum);
                        }
                        dineInData.close();
                        dineInStmt.close();
                    break;
					case DBNinja.delivery:
                        String deliverySQL = "SELECT * FROM delivery WHERE ordertable_OrderID = ?";
                        PreparedStatement deliveryStmt = conn.prepareStatement(deliverySQL);
                        deliveryStmt.setInt(1, orderID);
                        ResultSet deliveryData = deliveryStmt.executeQuery();
                        if (deliveryData.next()) {
                            int houseNum = deliveryData.getInt("delivery_HouseNum");
                            String street = deliveryData.getString("delivery_Street");
                            String city = deliveryData.getString("delivery_City");
                            String state = deliveryData.getString("delivery_State");
                            int zip = deliveryData.getInt("delivery_Zip");
                            boolean isDelivered = deliveryData.getBoolean("delivery_IsDelivered");
                            String address = houseNum + "\t" + street + "\t" + city + "\t" + state + "\t" + zip;
                            order = new DeliveryOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, isDelivered, address);
                        }
                        deliveryData.close();
                        deliveryStmt.close();
                    break;
					case DBNinja.pickup:
                        String pickupSQL = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?";
                        PreparedStatement pickupStmt = conn.prepareStatement(pickupSQL);
                        pickupStmt.setInt(1, orderID);
                        ResultSet pickupData = pickupStmt.executeQuery();
                        if (pickupData.next()) {
                            boolean isPickedUp = pickupData.getBoolean("pickup_IsPickedUp");
                            order = new PickupOrder(orderID, custID, orderDate, custPrice, busPrice, isPickedUp, isComplete);
                        }
                        pickupData.close();
                        pickupStmt.close();
                    break;
                }
				if (order != null) {
					order.setPizzaList(getPizzas(order));
					order.setDiscountList(getDiscounts(order));
					dateOrders.add(order);
				}
			}
			orderData.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return dateOrders;
	}

	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException
	{
		/*
		 * Query the database for all the available discounts and
		 * return them in an arrayList of discounts ordered by discount name.
		 *
		 */
		connect_to_db();
		ArrayList<Discount> discountList = new ArrayList<>();
		try {
			String sql = "SELECT * FROM discount ORDER BY discount_DiscountName";
			Statement stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery(sql);
			while (results.next()) {
				int id = results.getInt("discount_DiscountID");
				String name = results.getString("discount_DiscountName");
				double amount = results.getDouble("discount_Amount");
				boolean isPercentage = results.getBoolean("discount_IsPercent");
				Discount disc = new Discount(id, name, amount, isPercentage);
				discountList.add(disc);
			}
			results.close();
			stmt.close();
		} catch (Exception e) {
		} finally {
			conn.close();
		}
		return discountList;
	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException
	{
		/*
		 * Query the database for a discount using it's name.
		 * If found, then return an OrderDiscount object for the discount.
		 * If it's not found....then return null
		 *
		 */
		connect_to_db();
		Discount foundDiscount = null;
		String discountQuery = "SELECT * FROM discount WHERE discount_DiscountName = ?";
		PreparedStatement pstmt = conn.prepareStatement(discountQuery);
		pstmt.setString(1, name);
		ResultSet discountData = pstmt.executeQuery();
		if (discountData.next()) {
			int discountID = discountData.getInt("discount_DiscountID");
			String discountName = discountData.getString("discount_DiscountName");
			double discountAmount = discountData.getDouble("discount_Amount");
			boolean isPercent = discountData.getBoolean("discount_IsPercent");
			foundDiscount = new Discount(discountID, discountName, discountAmount, isPercent);
		}
		discountData.close();
		pstmt.close();
		conn.close();
		return foundDiscount;
	}


	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException
	{
		/*
		 * Query the data for all the customers and return an arrayList of all the customers.
		 * Don't forget to order the data coming from the database appropriately.
		 *
		 */
		connect_to_db();
		ArrayList<Customer> customerList = new ArrayList<>();
		String customerSQL = "SELECT * FROM customer " +
				"ORDER BY customer_LName, customer_FName, customer_PhoneNum";
		Statement stmt = conn.createStatement();
		ResultSet customerData = stmt.executeQuery(customerSQL);
		while (customerData.next()) {
			int id = customerData.getInt("customer_CustID");
			String firstName = customerData.getString("customer_FName");
			String lastName = customerData.getString("customer_LName");
			String phoneNumber = customerData.getString("customer_PhoneNum");
			Customer customer = new Customer(id, firstName, lastName, phoneNumber);
			customerList.add(customer);
		}
		customerData.close();
		stmt.close();
		conn.close();
		return customerList;
	}

	public static Customer findCustomerByPhone(String phoneNumber)  throws SQLException, IOException
	{
		/*
		 * Query the database for a customer using a phone number.
		 * If found, then return a Customer object for the customer.
		 * If it's not found....then return null
		 *
		 */
		connect_to_db();
		Customer foundCustomer = null;
		try {
			String query = "SELECT * FROM customer WHERE customer_PhoneNum = ?";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, phoneNumber);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int custID = rs.getInt("customer_CustID");
				String firstName = rs.getString("customer_FName");
				String lastName = rs.getString("customer_LName");
				String phone = rs.getString("customer_PhoneNum");

				foundCustomer = new Customer(custID, firstName, lastName, phone);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return foundCustomer;
	}

	public static String getCustomerName(int CustID) throws SQLException, IOException
	{
		/*
		 * COMPLETED...WORKING Example!
		 *
		 * This is a helper method to fetch and format the name of a customer
		 * based on a customer ID. This is an example of how to interact with
		 * your database from Java.
		 *
		 * Notice how the connection to the DB made at the start of the
		 *
		 */

		connect_to_db();

		/*
		 * an example query using a constructed string...
		 * remember, this style of query construction could be subject to sql injection attacks!
		 *
		 */
		String cname1 = "";
		String cname2 = "";
		String query = "Select customer_FName, customer_LName From customer WHERE customer_CustID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);

		while(rset.next())
		{
			cname1 = rset.getString(1) + " " + rset.getString(2);
		}

		/*
		 * an BETTER example of the same query using a prepared statement...
		 * with exception handling
		 *
		 */
		try {
			PreparedStatement os;
			ResultSet rset2;
			String query2;
			query2 = "Select customer_FName, customer_LName From customer WHERE customer_CustID=?;";
			os = conn.prepareStatement(query2);
			os.setInt(1, CustID);
			rset2 = os.executeQuery();
			while(rset2.next())
			{
				cname2 = rset2.getString("customer_FName") + " " + rset2.getString("customer_LName"); // note the use of field names in the getSting methods
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}

		conn.close();

		return cname1;
		// OR
		// return cname2;

	}


	public static ArrayList<Topping> getToppingList() throws SQLException, IOException
	{
		/*
		 * Query the database for the aviable toppings and
		 * return an arrayList of all the available toppings.
		 * Don't forget to order the data coming from the database appropriately.
		 *
		 */
		connect_to_db();
		ArrayList<Topping> allToppings = new ArrayList<>();
		String toppingQuery = "SELECT * FROM topping ORDER BY topping_TopName";
		PreparedStatement ps = conn.prepareStatement(toppingQuery);
		ResultSet toppingResults = ps.executeQuery();
		while (toppingResults.next()) {
			int id = toppingResults.getInt("topping_TopID");
			String name = toppingResults.getString("topping_TopName");
			double smallAmount = toppingResults.getDouble("topping_SmallAMT");
			double mediumAmount = toppingResults.getDouble("topping_MedAMT");
			double largeAmount = toppingResults.getDouble("topping_LgAMT");
			double xlargeAmount = toppingResults.getDouble("topping_XLAMT");
			double customerPrice = toppingResults.getDouble("topping_CustPrice");
			double businessPrice = toppingResults.getDouble("topping_BusPrice");
			int minInventory = toppingResults.getInt("topping_MinINVT");
			int currentInventory = toppingResults.getInt("topping_CurINVT");
			Topping newTopping = new Topping(id, name, smallAmount, mediumAmount, largeAmount,
					xlargeAmount, customerPrice, businessPrice, minInventory, currentInventory);
			allToppings.add(newTopping);
		}
		toppingResults.close();
		ps.close();
		conn.close();
		return allToppings;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException
	{
		/*
		 * Query the database for the topping using it's name.
		 * If found, then return a Topping object for the topping.
		 * If it's not found....then return null
		 *
		 */
		connect_to_db();
		String findTopping = "SELECT * FROM topping WHERE topping_TopName = ?";
		PreparedStatement statement = conn.prepareStatement(findTopping);
		statement.setString(1, name);
		ResultSet results = statement.executeQuery();
		Topping topping = null;
		if (results.next()) {
			int id = results.getInt("topping_TopID");
			String toppingName = results.getString("topping_TopName");
			double smallAmount = results.getDouble("topping_SmallAMT");
			double medAmount = results.getDouble("topping_MedAMT");
			double lgAmount = results.getDouble("topping_LgAMT");
			double xlAmount = results.getDouble("topping_XLAMT");
			double custPrice = results.getDouble("topping_CustPrice");
			double busPrice = results.getDouble("topping_BusPrice");
			int minInventory = results.getInt("topping_MinINVT");
			int currInventory = results.getInt("topping_CurINVT");
			topping = new Topping(id, toppingName, smallAmount, medAmount, lgAmount,
					xlAmount, custPrice, busPrice, minInventory, currInventory);
		}
		results.close();
		statement.close();
		conn.close();
		return topping;
	}

	public static ArrayList<Topping> getToppingsOnPizza(Pizza p) throws SQLException, IOException
	{
		/*
		 * This method builds an ArrayList of the toppings ON a pizza.
		 * The list can then be added to the Pizza object elsewhere in the
		 */

		connect_to_db();
		ArrayList<Topping> pizzaToppings = new ArrayList<>();
		try {
			String toppingSQL = "SELECT t.*, pt.pizza_topping_IsDouble " +
					"FROM topping t " +
					"JOIN pizza_topping pt ON t.topping_TopID = pt.topping_TopID " +
					"WHERE pt.pizza_PizzaID = ?";
			PreparedStatement stmt = conn.prepareStatement(toppingSQL);
			stmt.setInt(1, p.getPizzaID());
			ResultSet results = stmt.executeQuery();
			while (results.next()) {
				int id = results.getInt("topping_TopID");
				String name = results.getString("topping_TopName");
				double smallAmt = results.getDouble("topping_SmallAMT");
				double medAmt = results.getDouble("topping_MedAMT");
				double lgAmt = results.getDouble("topping_LgAMT");
				double xlAmt = results.getDouble("topping_XLAMT");
				double custPrice = results.getDouble("topping_CustPrice");
				double busPrice = results.getDouble("topping_BusPrice");
				int minInvt = results.getInt("topping_MinINVT");
				int curInvt = results.getInt("topping_CurINVT");
				boolean isDouble = results.getInt("pizza_topping_IsDouble") == 1;
				Topping t = new Topping(id, name, smallAmt, medAmt, lgAmt, xlAmt,
						custPrice, busPrice, minInvt, curInvt);
				t.setDoubled(isDouble);
				pizzaToppings.add(t);
			}
			results.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return pizzaToppings;
	}

	public static void addToInventory(int toppingID, double quantity) throws SQLException, IOException
	{
		/*
		 * Updates the quantity of the topping in the database by the amount specified.
		 *
		 * */
		connect_to_db();
		String updateSQL = "UPDATE topping " +
				"SET topping_CurINVT = topping_CurINVT + ? " +
				"WHERE topping_TopID = ?";
		PreparedStatement updateStatement = conn.prepareStatement(updateSQL);
		updateStatement.setDouble(1, quantity);
		updateStatement.setInt(2, toppingID);
		updateStatement.executeUpdate();
		updateStatement.close();
		conn.close();
	}


	public static ArrayList<Pizza> getPizzas(Order o) throws SQLException, IOException
	{
		/*
		 * Build an ArrayList of all the Pizzas associated with the Order.
		 *
		 */
		connect_to_db();
		ArrayList<Pizza> orderPizzas = new ArrayList<>();
		try {
			String pizzaSQL = "SELECT * FROM pizza WHERE ordertable_OrderID = ?";
			PreparedStatement stmt = conn.prepareStatement(pizzaSQL);
			stmt.setInt(1, o.getOrderID());
			ResultSet results = stmt.executeQuery();
			while (results.next()) {
				int pizzaID = results.getInt("pizza_PizzaID");
				String size = results.getString("pizza_Size");
				String crust = results.getString("pizza_CrustType");
				String state = results.getString("pizza_PizzaState");
				String date = results.getString("pizza_PizzaDate");
				double custPrice = results.getDouble("pizza_CustPrice");
				double busPrice = results.getDouble("pizza_BusPrice");
				Pizza p = new Pizza(pizzaID, size, crust, o.getOrderID(), state, date, custPrice, busPrice);
				p.setToppings(getToppingsOnPizza(p));
				p.setDiscounts(getDiscounts(p));
				orderPizzas.add(p);
			}
			results.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return orderPizzas;
	}

	public static ArrayList<Discount> getDiscounts(Order o) throws SQLException, IOException
	{
		/*
		 * Build an array list of all the Discounts associted with the Order.
		 *
		 */

		connect_to_db();
		ArrayList<Discount> orderDiscounts = new ArrayList<>();
		try {
			String discountSQL = "SELECT d.* FROM discount d " +
					"JOIN order_discount od ON d.discount_DiscountID = od.discount_DiscountID " +
					"WHERE od.ordertable_OrderID = ?";
			PreparedStatement stmt = conn.prepareStatement(discountSQL);
			stmt.setInt(1, o.getOrderID());
			ResultSet results = stmt.executeQuery();
			while (results.next()) {
				int id = results.getInt("discount_DiscountID");
				String name = results.getString("discount_DiscountName");
				double amount = results.getDouble("discount_Amount");
				boolean isPercent = results.getBoolean("discount_IsPercent");
				Discount discount = new Discount(id, name, amount, isPercent);
				orderDiscounts.add(discount);
			}

			results.close();
			stmt.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return orderDiscounts;
	}

	public static ArrayList<Discount> getDiscounts(Pizza p) throws SQLException, IOException
	{
		/*
		 * Build an array list of all the Discounts associted with the Pizza.
		 *
		 */

		connect_to_db();
		ArrayList<Discount> pizzaDiscounts = new ArrayList<>();
		String sql = "SELECT d.* FROM discount d " +
				"JOIN pizza_discount pd ON d.discount_DiscountID = pd.discount_DiscountID " +
				"WHERE pd.pizza_PizzaID = ?";
		PreparedStatement statement = conn.prepareStatement(sql);
		statement.setInt(1, p.getPizzaID());
		ResultSet data = statement.executeQuery();
		while (data.next()) {
			int discountID = data.getInt("discount_DiscountID");
			String discountName = data.getString("discount_DiscountName");
			double amount = data.getDouble("discount_Amount");
			boolean isPercent = data.getBoolean("discount_IsPercent");
			Discount d = new Discount(discountID, discountName, amount, isPercent);
			pizzaDiscounts.add(d);
		}
		data.close();
		statement.close();
		conn.close();
		return pizzaDiscounts;
	}

	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException
	{
		/*
		 * Query the database fro the base customer price for that size and crust pizza.
		 *
		 */
		connect_to_db();
		double price = 0.0;
		try {
			String sql = "SELECT baseprice_CustPrice FROM baseprice " +
					"WHERE baseprice_Size = ? AND basePrice_CrustType = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, size);
			statement.setString(2, crust);
			ResultSet result = statement.executeQuery();
			if (result.next()) {
				price = result.getDouble("baseprice_CustPrice");
			}
			result.close();
			statement.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
		return price;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException
	{
		/*
		 * Query the database fro the base business price for that size and crust pizza.
		 *
		 */
		connect_to_db();
		double businessPrice = 0.0;
		String priceQuery = "SELECT baseprice_BusPrice " +
				"FROM baseprice " +
				"WHERE baseprice_Size = ? " +
				"AND basePrice_CrustType = ?";
		PreparedStatement priceStatement = conn.prepareStatement(priceQuery);
		priceStatement.setString(1, size);
		priceStatement.setString(2, crust);
		ResultSet priceData = priceStatement.executeQuery();
		if (priceData.next()) {
			businessPrice = priceData.getDouble(1); // Get first column
		}
		priceData.close();
		priceStatement.close();
		conn.close();
		return businessPrice;
	}


	public static void printToppingReport() throws SQLException, IOException
	{
		/*
		 * Prints the ToppingPopularity view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 *
		 * The result should be readable and sorted as indicated in the prompt.
		 *
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 *
		 */
		connect_to_db();
		String reportQuery = "SELECT * FROM ToppingPopularity";
		Statement stmt = conn.createStatement();
		ResultSet reportData = stmt.executeQuery(reportQuery);
		System.out.printf("%-6s %-13s\n", "Topping", "Topping Count");
		System.out.printf("%-6s %-13s\n", "-------", "------------");
		while (reportData.next()) {
			String toppingName = reportData.getString("Topping");
			int count = reportData.getInt("ToppingCount");
			System.out.printf("%-6s %-13s\n", toppingName, count);
		}
		reportData.close();
		stmt.close();
		conn.close();
	}

	public static void printProfitByPizzaReport() throws SQLException, IOException
	{
		/*
		 * Prints the ProfitByPizza view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 *
		 * The result should be readable and sorted as indicated in the prompt.
		 *
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 *
		 */
		connect_to_db();
		try {
			String sql = "SELECT * FROM ProfitByPizza";
			PreparedStatement statement = conn.prepareStatement(sql);
			ResultSet results = statement.executeQuery();
			System.out.printf("%-9s %-11s %-6s %-15s\n",
					"Pizza Size", "Pizza Crust", "Profit", "Last Order Date");
			System.out.printf("%-9s %-11s %-6s %-15s\n",
					"----------", "-----------", "------", "--------------");
			while (results.next()) {
				String size = results.getString("Size");
				String crust = results.getString("Crust");
				double profit = results.getDouble("Profit");
				String date = results.getString("OrderMonth");
				System.out.printf("%-9s %-11s %-6.2f %-15s\n", size, crust, profit, date);
			}
			results.close();
			statement.close();
		} catch (SQLException e) {
		} finally {
			conn.close();
		}
	}

	public static void printProfitByOrderTypeReport() throws SQLException, IOException
	{
		/*
		 * Prints the ProfitByOrderType view. Remember that this view
		 * needs to exist in your DB, so be sure you've run your createViews.sql
		 * files on your testing DB if you haven't already.
		 *
		 * The result should be readable and sorted as indicated in the prompt.
		 *
		 * HINT: You need to match the expected output EXACTLY....I would suggest
		 * you look at the printf method (rather that the simple print of println).
		 * It operates the same in Java as it does in C and will make your code
		 * better.
		 *
		 */
		connect_to_db();
		String sql = "SELECT * FROM ProfitByOrderType";
		Statement statement = conn.createStatement();
		ResultSet data = statement.executeQuery(sql);
		System.out.println("Customer Type Order Month Total Order Price Total Order Cost Profit");
		System.out.println("------------- ----------- ----------------- --------------- ------");
		while (data.next()) {
			String type = data.getString("customerType");
			String month = data.getString("OrderMonth");
			double totalPrice = data.getDouble("TotalOrderPrice");
			double totalCost = data.getDouble("TotalOrderCost");
			double profit = data.getDouble("Profit");
			System.out.printf("%-13s %-11s %-17.2f %-17.2f %-6.2f\n",
					type, month, totalPrice, totalCost, profit);
		}
		data.close();
		statement.close();
		conn.close();
	}



	/*
	 * These private methods help get the individual components of an SQL datetime object.
	 * You're welcome to keep them or remove them....but they are usefull!
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0,4));
	}
	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}
	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder)
	{
		if(getYear(dateOfOrder) > year)
			return true;
		else if(getYear(dateOfOrder) < year)
			return false;
		else
		{
			if(getMonth(dateOfOrder) > month)
				return true;
			else if(getMonth(dateOfOrder) < month)
				return false;
			else
			{
				if(getDay(dateOfOrder) >= day)
					return true;
				else
					return false;
			}
		}
	}


}