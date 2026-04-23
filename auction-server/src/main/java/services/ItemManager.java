package services;

import dao.ItemDAO;
import models.Item;
import models.Seller;
import java.util.List;

public class ItemManager {
    private static ItemManager instance;
    private ItemDAO itemDAO;

    private ItemManager() {
        itemDAO = new ItemDAO();
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    public void addItem(String id, Item item, Seller seller) {
        // 1. Gắn seller vào item
        item.setSeller(seller);

        // 2. ĐÃ SỬA: Gọi DAO để lưu sản phẩm vào Database thay vì dùng biến 'items'
        // Bạn cần đảm bảo trong ItemDAO đã có hàm addItem(String id, Item item)
        itemDAO.addItem(id, item);

        System.out.println(">>> Đã lưu sản phẩm " + item.getName() + " vào Database.");
    }

    public boolean updateItem(String productId, String newName, String newDesc, double newStartPrice) {
        return itemDAO.updateItem(productId, newName, newDesc, newStartPrice);
    }

    public boolean deleteItem(String productId) {
        return itemDAO.deleteItem(productId);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }
}