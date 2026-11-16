package com.example.gap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GroceryApp.db";
    // v1: customers + addresses (plain-text passwords)
    // v2: hashed passwords, orders + order_items, no seeded test account
    // v3: payment method + payment reference (UPI transaction id) on orders
    // v4: order status (placed / out for delivery / delivered / cancelled)
    // v5: optional map pin (latitude/longitude) on addresses
    private static final int DATABASE_VERSION = 5;

    // Customer Table
    private static final String TABLE_CUSTOMER = "customers";
    private static final String COL_CUSTOMER_ID = "customer_id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password"; // stores a PasswordHasher hash
    private static final String COL_FULL_NAME = "full_name";
    private static final String COL_PHONE = "phone";
    private static final String COL_EMAIL = "email";

    // Address Table
    private static final String TABLE_ADDRESS = "addresses";
    private static final String COL_ADDRESS_ID = "address_id";
    private static final String COL_ADDRESS_CUSTOMER_ID = "customer_id";
    private static final String COL_ADDRESS_LABEL = "address_label";
    private static final String COL_HOUSE_NO = "house_no";
    private static final String COL_STREET = "street";
    private static final String COL_AREA = "area";
    private static final String COL_CITY = "city";
    private static final String COL_STATE = "state";
    private static final String COL_PINCODE = "pincode";
    private static final String COL_LANDMARK = "landmark";
    private static final String COL_IS_DEFAULT = "is_default";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";

    // Orders Table
    private static final String TABLE_ORDER = "orders";
    private static final String COL_ORDER_ID = "order_id";
    private static final String COL_ORDER_CUSTOMER_ID = "customer_id";
    private static final String COL_ORDER_TOTAL = "total";
    private static final String COL_ORDER_ADDRESS = "delivery_address";
    private static final String COL_ORDER_CREATED_AT = "created_at";
    private static final String COL_ORDER_PAYMENT_METHOD = "payment_method";
    private static final String COL_ORDER_PAYMENT_REF = "payment_ref";
    private static final String COL_ORDER_STATUS = "status";

    // Order Items Table
    private static final String TABLE_ORDER_ITEM = "order_items";
    private static final String COL_ITEM_ID = "item_id";
    private static final String COL_ITEM_ORDER_ID = "order_id";
    private static final String COL_ITEM_NAME = "name";
    private static final String COL_ITEM_PRICE = "price";
    private static final String COL_ITEM_UNIT = "unit";
    private static final String COL_ITEM_QUANTITY = "quantity";

    private static volatile DatabaseHelper instance;

    /** Single shared instance so every screen uses the same connection. */
    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** In-memory database for instrumented tests; never touches the real GroceryApp.db. */
    @VisibleForTesting
    static DatabaseHelper createInMemory(Context context) {
        return new DatabaseHelper(context.getApplicationContext(), null);
    }

    private DatabaseHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    private DatabaseHelper(Context context, @Nullable String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CUSTOMER + " (" +
                COL_CUSTOMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_FULL_NAME + " TEXT, " +
                COL_PHONE + " TEXT, " +
                COL_EMAIL + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ADDRESS + " (" +
                COL_ADDRESS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ADDRESS_CUSTOMER_ID + " INTEGER NOT NULL, " +
                COL_ADDRESS_LABEL + " TEXT NOT NULL, " +
                COL_HOUSE_NO + " TEXT NOT NULL, " +
                COL_STREET + " TEXT, " +
                COL_AREA + " TEXT NOT NULL, " +
                COL_CITY + " TEXT NOT NULL, " +
                COL_STATE + " TEXT NOT NULL, " +
                COL_PINCODE + " TEXT NOT NULL, " +
                COL_LANDMARK + " TEXT, " +
                COL_IS_DEFAULT + " INTEGER DEFAULT 0, " +
                COL_LATITUDE + " REAL, " +
                COL_LONGITUDE + " REAL, " +
                "FOREIGN KEY(" + COL_ADDRESS_CUSTOMER_ID + ") REFERENCES " +
                TABLE_CUSTOMER + "(" + COL_CUSTOMER_ID + ") ON DELETE CASCADE)");

        createOrderTables(db);
    }

    private void createOrderTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ORDER + " (" +
                COL_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ORDER_CUSTOMER_ID + " INTEGER NOT NULL, " +
                COL_ORDER_TOTAL + " REAL NOT NULL, " +
                COL_ORDER_ADDRESS + " TEXT NOT NULL, " +
                COL_ORDER_CREATED_AT + " INTEGER NOT NULL, " +
                COL_ORDER_PAYMENT_METHOD + " TEXT NOT NULL DEFAULT '', " +
                COL_ORDER_PAYMENT_REF + " TEXT, " +
                COL_ORDER_STATUS + " TEXT NOT NULL DEFAULT '" + OrderStatus.PLACED.name() + "', " +
                "FOREIGN KEY(" + COL_ORDER_CUSTOMER_ID + ") REFERENCES " +
                TABLE_CUSTOMER + "(" + COL_CUSTOMER_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_ORDER_ITEM + " (" +
                COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_ORDER_ID + " INTEGER NOT NULL, " +
                COL_ITEM_NAME + " TEXT NOT NULL, " +
                COL_ITEM_PRICE + " REAL NOT NULL, " +
                COL_ITEM_UNIT + " TEXT, " +
                COL_ITEM_QUANTITY + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_ITEM_ORDER_ID + ") REFERENCES " +
                TABLE_ORDER + "(" + COL_ORDER_ID + ") ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migrate step by step instead of dropping tables, so user data survives upgrades.
        if (oldVersion < 2) {
            // Remove the seeded test account ("user" / "password") that v1 shipped with.
            try (Cursor c = db.query(TABLE_CUSTOMER, new String[]{COL_CUSTOMER_ID},
                    COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                    new String[]{"user", "password"}, null, null, null)) {
                while (c.moveToNext()) {
                    String id = String.valueOf(c.getInt(0));
                    db.delete(TABLE_ADDRESS, COL_ADDRESS_CUSTOMER_ID + "=?", new String[]{id});
                    db.delete(TABLE_CUSTOMER, COL_CUSTOMER_ID + "=?", new String[]{id});
                }
            }

            // Hash any remaining plain-text passwords.
            Map<Integer, String> plain = new HashMap<>();
            try (Cursor c = db.query(TABLE_CUSTOMER, new String[]{COL_CUSTOMER_ID, COL_PASSWORD},
                    null, null, null, null, null)) {
                while (c.moveToNext()) {
                    String pw = c.getString(1);
                    if (!PasswordHasher.isHashed(pw)) {
                        plain.put(c.getInt(0), pw);
                    }
                }
            }
            for (Map.Entry<Integer, String> e : plain.entrySet()) {
                ContentValues cv = new ContentValues();
                cv.put(COL_PASSWORD, PasswordHasher.hash(e.getValue()));
                db.update(TABLE_CUSTOMER, cv, COL_CUSTOMER_ID + "=?",
                        new String[]{String.valueOf(e.getKey())});
            }

            createOrderTables(db); // created with the latest order schema, so skip the order ALTERs
        } else {
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_ORDER + " ADD COLUMN " + COL_ORDER_PAYMENT_METHOD +
                        " TEXT NOT NULL DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_ORDER + " ADD COLUMN " + COL_ORDER_PAYMENT_REF + " TEXT");
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE " + TABLE_ORDER + " ADD COLUMN " + COL_ORDER_STATUS +
                        " TEXT NOT NULL DEFAULT '" + OrderStatus.PLACED.name() + "'");
            }
        }
        if (oldVersion < 5) {
            // The addresses table has existed since v1, so every older version needs these columns.
            db.execSQL("ALTER TABLE " + TABLE_ADDRESS + " ADD COLUMN " + COL_LATITUDE + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_ADDRESS + " ADD COLUMN " + COL_LONGITUDE + " REAL");
        }
    }

    // ---------------------------------------------------------------- customers

    /** Registers a customer and returns the new customer, or null if it failed (e.g. duplicate username). */
    @Nullable
    public Customer registerCustomer(String username, String password, String fullName, String phone, String email) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, PasswordHasher.hash(password));
        cv.put(COL_FULL_NAME, fullName);
        cv.put(COL_PHONE, phone);
        cv.put(COL_EMAIL, email);
        long id = db.insert(TABLE_CUSTOMER, null, cv);
        return id > 0 ? new Customer((int) id, username, fullName, phone, email) : null;
    }

    /** Loads a customer by id (used to restore a saved login session), or null if it no longer exists. */
    @Nullable
    public Customer getCustomer(int customerId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_CUSTOMER, null, COL_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(customerId)}, null, null, null)) {
            return cursor.moveToFirst() ? readCustomer(cursor) : null;
        }
    }

    private static Customer readCustomer(Cursor cursor) {
        return new Customer(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_CUSTOMER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_FULL_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)));
    }

    /** Returns the customer if the username/password pair is valid, otherwise null. */
    @Nullable
    public Customer loginCustomer(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_CUSTOMER, null, COL_USERNAME + "=?",
                new String[]{username}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            String stored = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD));
            if (!PasswordHasher.verify(password, stored)) {
                return null;
            }
            return readCustomer(cursor);
        }
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_CUSTOMER, new String[]{COL_CUSTOMER_ID},
                COL_USERNAME + "=?", new String[]{username}, null, null, null)) {
            return cursor.getCount() > 0;
        }
    }

    // ---------------------------------------------------------------- addresses

    /**
     * Adds an address. If the customer has no default address yet, this one becomes the default
     * regardless of {@link Address#isDefault()}, so a customer with addresses always has a default.
     */
    public long addAddress(int customerId, Address address) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            boolean makeDefault = address.isDefault() || getDefaultAddress(customerId) == null;
            if (makeDefault) {
                ContentValues reset = new ContentValues();
                reset.put(COL_IS_DEFAULT, 0);
                db.update(TABLE_ADDRESS, reset, COL_ADDRESS_CUSTOMER_ID + "=?",
                        new String[]{String.valueOf(customerId)});
            }

            ContentValues cv = new ContentValues();
            cv.put(COL_ADDRESS_CUSTOMER_ID, customerId);
            cv.put(COL_ADDRESS_LABEL, address.getLabel());
            cv.put(COL_HOUSE_NO, address.getHouseNo());
            cv.put(COL_STREET, address.getStreet());
            cv.put(COL_AREA, address.getArea());
            cv.put(COL_CITY, address.getCity());
            cv.put(COL_STATE, address.getState());
            cv.put(COL_PINCODE, address.getPincode());
            cv.put(COL_LANDMARK, address.getLandmark());
            putLocation(cv, address);
            cv.put(COL_IS_DEFAULT, makeDefault ? 1 : 0);
            long id = db.insert(TABLE_ADDRESS, null, cv);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public List<Address> getCustomerAddresses(int customerId) {
        List<Address> addresses = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_ADDRESS, null, COL_ADDRESS_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(customerId)}, null, null,
                COL_IS_DEFAULT + " DESC, " + COL_ADDRESS_ID + " ASC")) {
            while (cursor.moveToNext()) {
                addresses.add(readAddress(cursor));
            }
        }
        return addresses;
    }

    public int getAddressCount(int customerId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ADDRESS +
                " WHERE " + COL_ADDRESS_CUSTOMER_ID + "=?", new String[]{String.valueOf(customerId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    @Nullable
    public Address getDefaultAddress(int customerId) {
        return querySingleAddress(COL_ADDRESS_CUSTOMER_ID + "=? AND " + COL_IS_DEFAULT + "=1",
                new String[]{String.valueOf(customerId)});
    }

    @Nullable
    public Address getAddress(int customerId, int addressId) {
        return querySingleAddress(COL_ADDRESS_CUSTOMER_ID + "=? AND " + COL_ADDRESS_ID + "=?",
                new String[]{String.valueOf(customerId), String.valueOf(addressId)});
    }

    public void setDefaultAddress(int customerId, int addressId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues reset = new ContentValues();
            reset.put(COL_IS_DEFAULT, 0);
            db.update(TABLE_ADDRESS, reset, COL_ADDRESS_CUSTOMER_ID + "=?",
                    new String[]{String.valueOf(customerId)});

            ContentValues set = new ContentValues();
            set.put(COL_IS_DEFAULT, 1);
            db.update(TABLE_ADDRESS, set, COL_ADDRESS_ID + "=? AND " + COL_ADDRESS_CUSTOMER_ID + "=?",
                    new String[]{String.valueOf(addressId), String.valueOf(customerId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Updates an existing address's fields. If {@code address.isDefault()} is true it also becomes the
     * default; clearing the flag never leaves the customer without a default (use setDefaultAddress on
     * another address instead).
     */
    public boolean updateAddress(int customerId, Address address) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ADDRESS_LABEL, address.getLabel());
        cv.put(COL_HOUSE_NO, address.getHouseNo());
        cv.put(COL_STREET, address.getStreet());
        cv.put(COL_AREA, address.getArea());
        cv.put(COL_CITY, address.getCity());
        cv.put(COL_STATE, address.getState());
        cv.put(COL_PINCODE, address.getPincode());
        cv.put(COL_LANDMARK, address.getLandmark());
        putLocation(cv, address);
        int rows = db.update(TABLE_ADDRESS, cv, COL_ADDRESS_ID + "=? AND " + COL_ADDRESS_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(address.getAddressId()), String.valueOf(customerId)});
        if (rows > 0 && address.isDefault()) {
            setDefaultAddress(customerId, address.getAddressId());
        }
        return rows > 0;
    }

    public int deleteAddress(int customerId, int addressId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_ADDRESS, COL_ADDRESS_ID + "=? AND " + COL_ADDRESS_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(addressId), String.valueOf(customerId)});
    }

    @Nullable
    private Address querySingleAddress(String selection, String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_ADDRESS, null, selection, args, null, null, null, "1")) {
            return cursor.moveToFirst() ? readAddress(cursor) : null;
        }
    }

    private static void putLocation(ContentValues cv, Address address) {
        if (address.hasLocation()) {
            cv.put(COL_LATITUDE, address.getLatitude());
            cv.put(COL_LONGITUDE, address.getLongitude());
        } else {
            cv.putNull(COL_LATITUDE);
            cv.putNull(COL_LONGITUDE);
        }
    }

    private static Address readAddress(Cursor cursor) {
        int latIdx = cursor.getColumnIndexOrThrow(COL_LATITUDE);
        int lngIdx = cursor.getColumnIndexOrThrow(COL_LONGITUDE);
        Double lat = cursor.isNull(latIdx) ? null : cursor.getDouble(latIdx);
        Double lng = cursor.isNull(lngIdx) ? null : cursor.getDouble(lngIdx);
        return new Address(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ADDRESS_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS_LABEL)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_HOUSE_NO)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_STREET)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_AREA)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_CITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_STATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PINCODE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDMARK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DEFAULT)) == 1,
                lat, lng);
    }

    // ---------------------------------------------------------------- orders

    /** Saves an order and its items atomically. Returns the new order id, or -1 on failure. */
    public long placeOrder(int customerId, Order order) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_ORDER_CUSTOMER_ID, customerId);
            cv.put(COL_ORDER_TOTAL, order.getTotal());
            cv.put(COL_ORDER_ADDRESS, order.getDeliveryAddress());
            cv.put(COL_ORDER_CREATED_AT, order.getCreatedAt());
            cv.put(COL_ORDER_PAYMENT_METHOD, order.getPaymentMethod());
            cv.put(COL_ORDER_PAYMENT_REF, order.getPaymentRef());
            cv.put(COL_ORDER_STATUS, order.getStatus().name());
            long orderId = db.insertOrThrow(TABLE_ORDER, null, cv);

            for (GroceryItem item : order.getItems()) {
                ContentValues icv = new ContentValues();
                icv.put(COL_ITEM_ORDER_ID, orderId);
                icv.put(COL_ITEM_NAME, item.getName());
                icv.put(COL_ITEM_PRICE, item.getPrice());
                icv.put(COL_ITEM_UNIT, item.getUnit());
                icv.put(COL_ITEM_QUANTITY, item.getQuantity());
                db.insertOrThrow(TABLE_ORDER_ITEM, null, icv);
            }
            db.setTransactionSuccessful();
            return orderId;
        } catch (android.database.SQLException e) {
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** Returns the customer's orders, newest first, with their items. */
    public List<Order> getOrders(int customerId) {
        return queryOrders(customerId, null);
    }

    /** Returns one of the customer's orders with its items, or null if it doesn't exist. */
    @Nullable
    public Order getOrder(int customerId, long orderId) {
        List<Order> list = queryOrders(customerId, orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Moves an order to a new status, but only if it is currently in one of {@code allowedFrom}
     * (e.g. cancel only while PLACED). Returns true if the order was updated.
     */
    public boolean updateOrderStatus(int customerId, long orderId, OrderStatus newStatus, OrderStatus... allowedFrom) {
        StringBuilder where = new StringBuilder(COL_ORDER_ID + "=? AND " + COL_ORDER_CUSTOMER_ID + "=?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(orderId));
        args.add(String.valueOf(customerId));
        if (allowedFrom.length > 0) {
            where.append(" AND ").append(COL_ORDER_STATUS).append(" IN (");
            for (int i = 0; i < allowedFrom.length; i++) {
                where.append(i == 0 ? "?" : ",?");
                args.add(allowedFrom[i].name());
            }
            where.append(")");
        }
        ContentValues cv = new ContentValues();
        cv.put(COL_ORDER_STATUS, newStatus.name());
        return getWritableDatabase().update(TABLE_ORDER, cv, where.toString(), args.toArray(new String[0])) > 0;
    }

    private List<Order> queryOrders(int customerId, @Nullable Long onlyOrderId) {
        SQLiteDatabase db = getReadableDatabase();
        String customerArg = String.valueOf(customerId);
        String orderFilter = onlyOrderId != null ? " AND o." + COL_ORDER_ID + "=" + onlyOrderId : "";

        Map<Long, List<GroceryItem>> itemsByOrder = new HashMap<>();
        try (Cursor c = db.rawQuery("SELECT i.* FROM " + TABLE_ORDER_ITEM + " i JOIN " + TABLE_ORDER +
                " o ON i." + COL_ITEM_ORDER_ID + " = o." + COL_ORDER_ID +
                " WHERE o." + COL_ORDER_CUSTOMER_ID + "=?" + orderFilter +
                " ORDER BY i." + COL_ITEM_ID, new String[]{customerArg})) {
            while (c.moveToNext()) {
                long orderId = c.getLong(c.getColumnIndexOrThrow(COL_ITEM_ORDER_ID));
                GroceryItem item = new GroceryItem(
                        c.getString(c.getColumnIndexOrThrow(COL_ITEM_NAME)),
                        c.getDouble(c.getColumnIndexOrThrow(COL_ITEM_PRICE)),
                        c.getString(c.getColumnIndexOrThrow(COL_ITEM_UNIT)),
                        0,
                        c.getInt(c.getColumnIndexOrThrow(COL_ITEM_QUANTITY)));
                List<GroceryItem> list = itemsByOrder.get(orderId);
                if (list == null) {
                    list = new ArrayList<>();
                    itemsByOrder.put(orderId, list);
                }
                list.add(item);
            }
        }

        List<Order> orders = new ArrayList<>();
        String selection = COL_ORDER_CUSTOMER_ID + "=?"
                + (onlyOrderId != null ? " AND " + COL_ORDER_ID + "=" + onlyOrderId : "");
        try (Cursor c = db.query(TABLE_ORDER, null, selection,
                new String[]{customerArg}, null, null, COL_ORDER_CREATED_AT + " DESC")) {
            while (c.moveToNext()) {
                long orderId = c.getLong(c.getColumnIndexOrThrow(COL_ORDER_ID));
                List<GroceryItem> items = itemsByOrder.get(orderId);
                orders.add(new Order(orderId,
                        items != null ? items : new ArrayList<>(),
                        c.getString(c.getColumnIndexOrThrow(COL_ORDER_ADDRESS)),
                        c.getLong(c.getColumnIndexOrThrow(COL_ORDER_CREATED_AT)),
                        c.getString(c.getColumnIndexOrThrow(COL_ORDER_PAYMENT_METHOD)),
                        c.getString(c.getColumnIndexOrThrow(COL_ORDER_PAYMENT_REF)),
                        OrderStatus.fromDb(c.getString(c.getColumnIndexOrThrow(COL_ORDER_STATUS)))));
            }
        }
        return orders;
    }
}
