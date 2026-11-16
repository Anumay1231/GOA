package com.example.gap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/** Runs on a device/emulator against an in-memory database (the real app data is untouched). */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private DatabaseHelper db;
    private Customer customer;

    @Before
    public void setUp() {
        db = DatabaseHelper.createInMemory(InstrumentationRegistry.getInstrumentation().getTargetContext());
        customer = db.registerCustomer("tester", "secret123", "Test User", "9876543210", null);
        assertNotNull(customer);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void login_checksHashedPassword() {
        assertNotNull(db.loginCustomer("tester", "secret123"));
        assertNull(db.loginCustomer("tester", "wrong"));
        assertNull(db.loginCustomer("nobody", "secret123"));
        assertTrue(db.isUsernameExists("tester"));
        assertNotNull(db.getCustomer(customer.getCustomerId()));
    }

    @Test
    public void firstAddress_alwaysBecomesDefault() {
        int id = customer.getCustomerId();
        db.addAddress(id, new Address("Home", "1", "", "Area", "Mumbai", "MH", "400001", "", false));
        Address def = db.getDefaultAddress(id);
        assertNotNull(def);
        assertEquals("Home", def.getLabel());

        long officeId = db.addAddress(id, new Address("Office", "2", "", "Area", "Mumbai", "MH", "400002", "", false));
        assertEquals("Home", db.getDefaultAddress(id).getLabel());

        db.setDefaultAddress(id, (int) officeId);
        assertEquals("Office", db.getDefaultAddress(id).getLabel());
        assertEquals(2, db.getAddressCount(id));
    }

    @Test
    public void updateAddress_changesFields() {
        int id = customer.getCustomerId();
        int addrId = (int) db.addAddress(id, new Address("Home", "1", "", "Area", "Mumbai", "MH", "400001", "", true));
        boolean ok = db.updateAddress(id, new Address(addrId, "Other", "9B", "New St", "New Area",
                "Pune", "MH", "411001", "Park", true));
        assertTrue(ok);
        Address a = db.getAddress(id, addrId);
        assertNotNull(a);
        assertEquals("Other", a.getLabel());
        assertEquals("Pune", a.getCity());
        assertTrue(a.isDefault());
    }

    @Test
    public void mapPin_isSavedUpdatedAndCleared() {
        int id = customer.getCustomerId();
        int addrId = (int) db.addAddress(id, new Address(0, "Home", "1", "", "Area", "Mumbai", "MH",
                "400001", "", true, 19.0760, 72.8777));
        Address saved = db.getAddress(id, addrId);
        assertNotNull(saved);
        assertTrue(saved.hasLocation());
        assertEquals(19.0760, saved.getLatitude(), 1e-9);
        assertTrue(saved.toOrderText().contains("https://maps.google.com/?q=19.076000,72.877700"));

        db.updateAddress(id, new Address(addrId, "Home", "1", "", "Area", "Mumbai", "MH", "400001", "", true));
        assertFalse(db.getAddress(id, addrId).hasLocation());
    }

    @Test
    public void orders_roundTripAndStatusRules() {
        int id = customer.getCustomerId();
        Order order = new Order(Arrays.asList(
                new GroceryItem("Potato", 25, "kg", 0, 2),
                new GroceryItem("Lemon", 5, "piece", 0, 3)),
                "Home address", Order.PAYMENT_UPI, "123456789012");
        long orderId = db.placeOrder(id, order);
        assertTrue(orderId > 0);

        List<Order> orders = db.getOrders(id);
        assertEquals(1, orders.size());
        Order saved = db.getOrder(id, orderId);
        assertNotNull(saved);
        assertEquals(2, saved.getItems().size());
        assertEquals(65.0, saved.getTotal(), 0.0001);
        assertEquals(OrderStatus.PLACED, saved.getStatus());
        assertEquals("123456789012", saved.getPaymentRef());

        // Can't mark an order "delivered" via the cancel path, and can't cancel twice.
        assertTrue(db.updateOrderStatus(id, orderId, OrderStatus.CANCELLED, OrderStatus.PLACED));
        assertFalse(db.updateOrderStatus(id, orderId, OrderStatus.CANCELLED, OrderStatus.PLACED));
        assertEquals(OrderStatus.CANCELLED, db.getOrder(id, orderId).getStatus());
    }

    @Test
    public void otherCustomersCannotSeeOrModifyOrders() {
        Customer other = db.registerCustomer("other", "secret123", "Other", "9876543211", null);
        assertNotNull(other);
        long orderId = db.placeOrder(customer.getCustomerId(), new Order(
                Arrays.asList(new GroceryItem("Potato", 25, "kg", 0, 1)), "Home", Order.PAYMENT_COD, null));

        assertNull(db.getOrder(other.getCustomerId(), orderId));
        assertFalse(db.updateOrderStatus(other.getCustomerId(), orderId, OrderStatus.CANCELLED, OrderStatus.PLACED));
    }
}
