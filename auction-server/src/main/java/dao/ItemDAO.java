package dao;

import models.Art;
import models.Electronics;
import models.Item;
import models.Vehicle;
import models.Seller;
import services.ItemFactory;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        // ĐÃ SỬA: Lấy thêm cả cột seller_name
        String query = "SELECT * FROM items";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
                LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
                String extraInfo = rs.getString("extra_info");

                // Đọc seller_name từ DB
                String sellerName = rs.getString("seller_name");

                Item item = ItemFactory.createItem(type, name, description, startingPrice, startTime, endTime, extraInfo);
                item.setId(id); // QUAN TRỌNG: Gán ID thật từ Database

                // QUAN TRỌNG: Gắn lại Seller vào Item để Dashboard lọc được
                if (sellerName != null) {
                    item.setSeller(new Seller(sellerName, "", ""));
                }

                item.setCurrentHighestPrice(startingPrice);
                itemList.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tải danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    // Gộp chung logic vào một hàm duy nhất để tránh nhầm lẫn
    public void addItem(String id, Item item) {
        String sql = "INSERT INTO items(id, name, description, starting_price, start_time, end_time, type, extra_info, seller_name) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setString(5, item.getStartTime().toString());
            pstmt.setString(6, item.getEndTime().toString());

            // Xác định type và extraInfo dựa trên instance của item
            String type = "GENERAL";
            String extraInfo = "";
            if (item instanceof Electronics) {
                type = "ELECTRONICS";
                extraInfo = String.valueOf(((Electronics) item).getWarrantyMonths());
            } else if (item instanceof Art) {
                type = "ART";
                extraInfo = ((Art) item).getArtistName();
            } else if (item instanceof Vehicle) {
                type = "VEHICLE";
                extraInfo = ((Vehicle) item).getBrand();
            }

            pstmt.setString(7, type);
            pstmt.setString(8, extraInfo);

            // Lấy tên người bán từ đối tượng Seller trong Item
            if (item.getSeller() != null) {
                pstmt.setString(9, item.getSeller().getUsername());
            } else {
                pstmt.setString(9, "Unknown");
            }

            pstmt.executeUpdate();
            System.out.println(">>> [Database] Đã lưu sản phẩm mới thành công: " + item.getName());

        } catch (SQLException e) {
            System.out.println("Lỗi khi thêm sản phẩm vào Database (Kiểm tra xem đã xóa file .db cũ chưa): " + e.getMessage());
        }
    }

    public boolean updateItem(String productId, String newName, String newDesc, double newStartPrice) {
        String query = "UPDATE items SET name = ?, description = ?, starting_price = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newName);
            stmt.setString(2, newDesc);
            stmt.setDouble(3, newStartPrice);
            stmt.setString(4, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi cập nhật sản phẩm: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteItem(String productId) {
        String query = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi xóa sản phẩm: " + e.getMessage());
            return false;
        }
    }
}