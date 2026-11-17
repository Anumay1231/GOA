package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;

/**
 * Upgrading databases written by older builds of the app. Uses throwaway database files with
 * made-up data; the app's real database is never touched.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseUpgradeTest {

    private static final String DB_NAME = "upgrade_test.db";
    private Context context;
    private DatabaseHelper helper;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB_NAME);
    }

    @After
    public void tearDown() {
        if (helper != null) helper.close();
        context.deleteDatabase(DB_NAME);
    }

    /** Layout used by an earlier release: plain-text passwords, map columns, order_date/item_* columns. */
    @Test
    public void legacyReleaseLayout_isConvertedWithoutDataLoss() {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null)) {
            db.execSQL("CREATE TABLE customers (customer_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, full_name TEXT, phone TEXT, email TEXT)");
            db.execSQL("CREATE TABLE addresses (address_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, address_label TEXT NOT NULL, house_no TEXT NOT NULL, street TEXT, area TEXT NOT NULL, city TEXT NOT NULL, state TEXT NOT NULL, pincode TEXT NOT NULL, landmark TEXT, latitude REAL, longitude REAL, is_default INTEGER DEFAULT 0, FOREIGN KEY(customer_id) REFERENCES customers(customer_id))");
            db.execSQL("CREATE TABLE orders (order_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, order_date TEXT NOT NULL, order_total REAL NOT NULL, delivery_address TEXT NOT NULL, payment_method TEXT NOT NULL, FOREIGN KEY(customer_id) REFERENCES customers(customer_id))");
            db.execSQL("CREATE TABLE order_items (order_item_id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, item_name TEXT NOT NULL, item_price REAL NOT NULL, item_quantity INTEGER NOT NULL, item_image_res_id INTEGER, FOREIGN KEY(order_id) REFERENCES orders(order_id))");

            db.execSQL("INSERT INTO customers VALUES (1, 'alice', 'secret1', 'Alice', '9000000001', NULL)");
            db.execSQL("INSERT INTO customers VALUES (2, 'user', 'password', 'Test User', '9876543210', NULL)");
            db.execSQL("INSERT INTO addresses VALUES (1, 1, 'Home', '1A', 'Main St', 'Bandra', 'Mumbai', 'MH', '400050', '', 19.06, 72.83, 1)");
            db.execSQL("INSERT INTO addresses VALUES (2, 2, 'Home', '2', '', 'Area', 'Mumbai', 'MH', '400001', '', NULL, NULL, 1)");
            db.execSQL("INSERT INTO orders VALUES (7, 1, '2025-11-20 01:43:07', 65.0, 'Home address', 'Cash on Delivery')");
            db.execSQL("INSERT INTO orders VALUES (8, 2, '2025-11-21 10:00:00', 25.0, 'Test address', 'UPI')");
            db.execSQL("INSERT INTO order_items VALUES (1, 7, 'Potato', 25.0, 2, 0)");
            db.execSQL("INSERT INTO order_items VALUES (2, 7, 'Lemon', 5.0, 3, 0)");
            db.execSQL("INSERT INTO order_items VALUES (3, 8, 'Potato', 25.0, 1, 0)");
            db.setVersion(2);
        }

        helper = DatabaseHelper.openForTest(context, DB_NAME);

        // Plain-text password was hashed and still works; the old test account is gone.
        Customer alice = helper.loginCustomer("alice", "secret1");
        assertNotNull(alice);
        assertNull(helper.loginCustomer("user", "password"));
        assertFalse(helper.isUsernameExists("user"));

        // Map pin survived.
        Address home = helper.getDefaultAddress(alice.getCustomerId());
        assertNotNull(home);
        assertTrue(home.hasLocation());
        assertEquals(19.06, home.getLatitude(), 1e-9);

        // Orders converted: same id, total, payment, items with units; old orders count as delivered.
        List<Order> orders = helper.getOrders(alice.getCustomerId());
        assertEquals(1, orders.size());
        Order o = orders.get(0);
        assertEquals(7, o.getOrderId());
        assertEquals(65.0, o.getTotal(), 0.0001);
        assertEquals(Order.PAYMENT_COD, o.getPaymentMethod());
        assertEquals(OrderStatus.DELIVERED, o.getStatus());
        assertEquals(2, o.getItems().size());
        assertEquals("piece", o.getItems().get(1).getUnit()); // Lemon, unit from the catalog
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(o.getCreatedAt());
        assertEquals(2025, cal.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH));

        // New orders keep working after the conversion.
        long newId = helper.placeOrder(alice.getCustomerId(), new Order(o.getItems(), "Home", Order.PAYMENT_UPI, null));
        assertTrue(newId > 7);
    }

    /** This project's own earlier version 2 layout (no payment/status/map columns). */
    @Test
    public void ownVersion2Layout_getsMissingColumns() {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null)) {
            db.execSQL("CREATE TABLE customers (customer_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, full_name TEXT, phone TEXT, email TEXT)");
            db.execSQL("CREATE TABLE addresses (address_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, address_label TEXT NOT NULL, house_no TEXT NOT NULL, street TEXT, area TEXT NOT NULL, city TEXT NOT NULL, state TEXT NOT NULL, pincode TEXT NOT NULL, landmark TEXT, is_default INTEGER DEFAULT 0)");
            db.execSQL("CREATE TABLE orders (order_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, total REAL NOT NULL, delivery_address TEXT NOT NULL, created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE order_items (item_id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, name TEXT NOT NULL, price REAL NOT NULL, unit TEXT, quantity INTEGER NOT NULL)");
            db.execSQL("INSERT INTO customers VALUES (1, 'bob', '" + PasswordHasher.hash("secret2") + "', 'Bob', '9000000002', NULL)");
            db.execSQL("INSERT INTO addresses VALUES (1, 1, 'Home', '1', '', 'Area', 'Mumbai', 'MH', '400001', '', 1)");
            db.execSQL("INSERT INTO orders VALUES (1, 1, 25.0, 'Home', 1700000000000)");
            db.execSQL("INSERT INTO order_items VALUES (1, 1, 'Potato', 25.0, 'kg', 1)");
            db.setVersion(2);
        }

        helper = DatabaseHelper.openForTest(context, DB_NAME);

        Customer bob = helper.loginCustomer("bob", "secret2");
        assertNotNull(bob);
        Order o = helper.getOrder(bob.getCustomerId(), 1);
        assertNotNull(o);
        assertEquals(OrderStatus.PLACED, o.getStatus());
        assertEquals("", o.getPaymentMethod());
        assertFalse(helper.getDefaultAddress(bob.getCustomerId()).hasLocation());
        assertTrue(helper.updateOrderStatus(bob.getCustomerId(), 1, OrderStatus.CANCELLED, OrderStatus.PLACED));
    }
}
