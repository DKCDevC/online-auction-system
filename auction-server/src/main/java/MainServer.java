import models.Auction;
import models.Electronics;
import models.Item;
import models.Seller;
import network.AuctionSocketServer;
import services.AuctionManager;
import utils.DBConnection; // --- ĐÃ THÊM: Import file kết nối DB ---

import java.time.LocalDateTime;

public class MainServer {
    public static void main(String[] args) {
        System.out.println("=== KHỞI ĐỘNG HỆ THỐNG ĐẤU GIÁ ===");

        // --- ĐÃ THÊM: Kích hoạt kiểm tra và tạo bảng Database ---
        System.out.println(">>> Đang kết nối và kiểm tra Database...");
        DBConnection.getConnection();
        // -------------------------------------------------------

        Seller seller1 = new Seller("nguoiban_01", "pass123", "seller@gmail.com");

        Item laptop = new Electronics("Laptop Asus ROG", "Gaming", 10000000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(1), 12);

        Auction laptopAuction = new Auction(laptop, seller1);
        laptopAuction.setAuctionId("laptop001");
        AuctionManager.getInstance().addAuction(laptopAuction);
        System.out.println(">>> ID CỦA PHIÊN ĐẤU GIÁ LÀ: " + laptopAuction.getAuctionId());

        System.out.println("=== ĐÃ TẠO PHIÊN ĐẤU GIÁ MẪU: Laptop Asus ROG ===");

        AuctionManager.getInstance().startAuctionTimer();

        int port = 9999;
        AuctionSocketServer server = new AuctionSocketServer(port);
        server.startServer();
    }
}