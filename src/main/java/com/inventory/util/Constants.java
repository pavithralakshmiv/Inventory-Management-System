package com.inventory.util;

import java.util.List;

public final class Constants {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";

    public static final String PENDING = "Pending";
    public static final String CANCELLED = "Cancelled";
    public static final String COMPLETED = "Completed";

    public static final List<String> ORDER_STATUSES = List.of("Pending", "Processing", "Shipped", "Delivered", "Cancelled");
    public static final List<String> PAYMENT_STATUSES = List.of("Paid", "Unpaid", "Partial");
    public static final List<String> PAYMENT_METHODS = List.of("Cash", "Card", "UPI", "Bank Transfer");

    public static final String MOVE_OPENING = "OPENING";
    public static final String MOVE_PURCHASE = "PURCHASE";
    public static final String MOVE_SALE = "SALE";
    public static final String MOVE_ORDER = "ORDER";
    public static final String MOVE_REVERSAL = "REVERSAL";
    public static final String MOVE_ADJUSTMENT = "ADJUSTMENT";

    public static final String STOCK_AVAILABLE = "Available";
    public static final String STOCK_LOW = "Low Stock";
    public static final String STOCK_OUT = "Out of Stock";

    private Constants() {
    }
}
