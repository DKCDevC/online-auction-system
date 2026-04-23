package dao;

import utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

public class OrderDAO {

    public Map<String, Double> getRevenueLast7Days(String sellerName) {
        // Sử dụng LinkedHashMap để giữ đúng thứ tự từ cũ đến mới
        Map<String, Double> revenueData = new LinkedHashMap<>();

        // Khởi tạo 7 ngày gần nhất với giá trị 0
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            revenueData.put(date, 0.0);
        }

        String query = "SELECT order_date, SUM(final_price) as daily_sum " +
                "FROM orders WHERE seller_name = ? " +
                "AND order_date >= DATE('now', '-7 days') " +
                "GROUP BY order_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sellerName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                revenueData.put(rs.getString("order_date"), rs.getDouble("daily_sum"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return revenueData;
    }

    public List<Map<String, String>> getOrdersBySeller(String sellerName) {
        List<Map<String, String>> orders = new ArrayList<>();

        String query = "SELECT o.order_id, o.item_id, o.bidder_name, o.final_price, o.order_date, " +
                "COALESCE(i.name, 'Sản phẩm đã xóa') as item_name " +
                "FROM orders o LEFT JOIN items i ON o.item_id = i.id " +
                "WHERE o.seller_name = ? ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sellerName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> order = new HashMap<>();
                order.put("orderId", String.valueOf(rs.getInt("order_id")));
                order.put("itemName", rs.getString("item_name"));
                order.put("buyerName", rs.getString("bidder_name") != null ? rs.getString("bidder_name") : "N/A");
                order.put("price", String.format("%,.0f", rs.getDouble("final_price")));
                order.put("orderDate", rs.getString("order_date") != null ? rs.getString("order_date") : "N/A");
                orders.add(order);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi lấy danh sách đơn hàng: " + e.getMessage());
        }
        return orders;
    }
}