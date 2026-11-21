package com.inventory.dto;

import java.util.List;

public record DashboardResponse(long totalProducts, long totalCustomers, long totalOrders, long lowStock,
                                List<ChartPoint> stockByCategory, List<LogResponse> recentActivities,
                                List<ProductResponse> recentProducts, List<OrderResponse> recentOrders) {
}
