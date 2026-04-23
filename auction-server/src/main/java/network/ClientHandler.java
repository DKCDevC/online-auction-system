package network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.AuctionRequest;
import models.BidTransaction;
import models.Item;
import models.User;
import models.Seller;
import services.AuctionManager;
import services.ItemManager;
import services.UserManager;
import services.ItemFactory;
import utils.GsonConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable, AuctionObserver {

    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private Socket clientSocket;
    private PrintWriter out;
    private Gson gson = GsonConfig.createGson();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            registerObserver(this);
            System.out.println(">>> Số Client đang kết nối: " + observers.size());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    AuctionRequest request = gson.fromJson(inputLine, AuctionRequest.class);

                    if ("LOGIN".equals(request.getCommand())) {
                        JsonObject loginData = gson.fromJson(inputLine, JsonObject.class);

                        String username = loginData.has("username") ? loginData.get("username").getAsString() : "";
                        String password = loginData.has("password") ? loginData.get("password").getAsString() : "";

                        User loggedInUser = UserManager.getInstance().login(username, password);

                        if (loggedInUser != null) {
                            out.println("{\"status\":\"SUCCESS\", \"role\":\"" + loggedInUser.getRole() + "\"}");
                            System.out.println(">>> User [" + loggedInUser.getUsername() + "] đã đăng nhập thành công!");
                        } else {
                            out.println("{\"status\":\"FAILED\", \"message\":\"Sai tai khoan hoac mat khau\"}");
                        }
                    }

                    else if ("REGISTER".equals(request.getCommand())) {
                        JsonObject regData = gson.fromJson(inputLine, JsonObject.class);
                        String username = regData.get("username").getAsString();
                        String password = regData.get("password").getAsString();
                        String role = regData.get("role").getAsString();

                        boolean success = UserManager.getInstance().registerUser(username, password, role);

                        if (success) {
                            out.println("{\"status\":\"SUCCESS\"}");
                            System.out.println(">>> Đã tạo tài khoản mới: " + username + " (" + role + ")");
                        } else {
                            out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản đã tồn tại!\"}");
                        }
                    }

                    // --- PHẦN QUAN TRỌNG: GỠ FIX CỨNG DASHBOARD ---
                    else if ("GET_SELLER_DASHBOARD".equals(request.getCommand())) {
                        JsonObject reqData = gson.fromJson(inputLine, JsonObject.class);
                        String sellerName = reqData.has("username") ? reqData.get("username").getAsString() : "";

                        JsonObject response = new JsonObject();
                        response.addProperty("status", "SUCCESS");

                        List<Item> allItems = ItemManager.getInstance().getAllItems();
                        JsonArray productsArray = new JsonArray();

                        int activeAuctionsCount = 0;
                        double totalRevenue = 0;
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();

                        for (Item item : allItems) {
                            // Lọc sản phẩm theo đúng Seller đang đăng nhập
                            if (item.getSeller() != null && sellerName.equals(item.getSeller().getUsername())) {

                                JsonObject pObj = new JsonObject();
                                String id = item.getId();
                                pObj.addProperty("id", id);
                                pObj.addProperty("name", item.getName());
                                pObj.addProperty("description", item.getDescription() != null ? item.getDescription() : "");

                                // Xác định loại sản phẩm
                                String itemType = "GENERAL";
                                if (item instanceof models.Electronics) itemType = "ELECTRONICS";
                                else if (item instanceof models.Art) itemType = "ART";
                                else if (item instanceof models.Vehicle) itemType = "VEHICLE";
                                pObj.addProperty("type", itemType);

                                double currentVal = item.getStartingPrice();
                                pObj.addProperty("price", String.format("%,.0f", currentVal));

                                // Thêm thời gian kết thúc
                                if (item.getEndTime() != null) {
                                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                                    pObj.addProperty("endTime", item.getEndTime().format(fmt));
                                } else {
                                    pObj.addProperty("endTime", "");
                                }

                                // Kiểm tra trạng thái dựa trên thời gian thật
                                if (now.isAfter(item.getEndTime())) {
                                    pObj.addProperty("status", "Đã kết thúc");
                                    totalRevenue += currentVal;
                                } else {
                                    pObj.addProperty("status", "Đang đấu giá");
                                    activeAuctionsCount++;
                                }

                                productsArray.add(pObj);
                            }
                        }

                        // Thống kê số liệu thật
                        response.addProperty("totalRevenue", String.format("%,.0f", totalRevenue));
                        response.addProperty("activeAuctions", String.valueOf(activeAuctionsCount));

                        // Đếm đơn hàng từ database
                        int pendingOrderCount = 0;
                        JsonArray ordersArray = new JsonArray();
                        try {
                            dao.OrderDAO orderDao = new dao.OrderDAO();
                            java.util.List<java.util.Map<String, String>> orderList = orderDao.getOrdersBySeller(sellerName);
                            pendingOrderCount = orderList.size();
                            for (java.util.Map<String, String> order : orderList) {
                                JsonObject oObj = new JsonObject();
                                oObj.addProperty("orderId", order.get("orderId"));
                                oObj.addProperty("itemName", order.get("itemName"));
                                oObj.addProperty("buyerName", order.get("buyerName"));
                                oObj.addProperty("price", order.get("price"));
                                oObj.addProperty("orderDate", order.get("orderDate"));
                                ordersArray.add(oObj);
                            }
                        } catch (Exception ex) {
                            System.out.println("Lỗi load đơn hàng: " + ex.getMessage());
                        }

                        response.addProperty("pendingOrders", String.valueOf(pendingOrderCount));
                        response.add("products", productsArray);
                        response.add("orders", ordersArray);

                        // Biểu đồ doanh thu 7 ngày từ Database
                        JsonArray chartData = new JsonArray();
                        try {
                            dao.OrderDAO orderDao = new dao.OrderDAO();
                            java.util.Map<String, Double> realStats = orderDao.getRevenueLast7Days(sellerName);

                            for (java.util.Map.Entry<String, Double> entry : realStats.entrySet()) {
                                JsonObject dayObj = new JsonObject();
                                java.time.LocalDate date = java.time.LocalDate.parse(entry.getKey());
                                int dayOfWeek = date.getDayOfWeek().getValue();
                                String label = (dayOfWeek == 7) ? "CN" : "T" + (dayOfWeek + 1);

                                dayObj.addProperty("day", label);
                                dayObj.addProperty("revenue", entry.getValue());
                                chartData.add(dayObj);
                            }
                        } catch (Exception ex) {
                            System.out.println("Lỗi load biểu đồ thật: " + ex.getMessage());
                        }
                        response.add("chartData", chartData);

                        out.println(gson.toJson(response));
                        System.out.println(">>> Dashboard RIÊNG BIỆT đã gửi cho: " + sellerName);
                    }

                    else if ("GET_ITEMS".equals(request.getCommand())) {
                        List<Item> items = ItemManager.getInstance().getAllItems();
                        JsonObject response = new JsonObject();
                        response.addProperty("command", "SET_ITEMS");
                        response.add("data", gson.toJsonTree(items));
                        out.println(gson.toJson(response));
                    }

                    else if ("ADD_ITEM".equals(request.getCommand())) {
                        JsonObject fullJson = gson.fromJson(inputLine, JsonObject.class);
                        JsonObject data = fullJson.getAsJsonObject("data");

                        String type = data.get("type").getAsString();
                        String name = data.get("name").getAsString();
                        String desc = data.get("desc").getAsString();
                        double price = data.get("price").getAsDouble();
                        String extra = data.get("extra").getAsString();
                        String sellerName = data.get("seller").getAsString();
                        
                        int durationDays = 7;
                        if (data.has("duration")) {
                            durationDays = data.get("duration").getAsInt();
                        }

                        java.time.LocalDateTime start = java.time.LocalDateTime.now();
                        java.time.LocalDateTime end = start.plusDays(durationDays);

                        Item newItem = ItemFactory.createItem(type, name, desc, price, start, end, extra);
                        String newId = java.util.UUID.randomUUID().toString();

                        // Quan trọng: Lưu seller vào item để dashboard lọc được
                        ItemManager.getInstance().addItem(newId, newItem, new Seller(sellerName, "", ""));

                        notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
                    }

                    else if ("DELETE_ITEM".equals(request.getCommand())) {
                        JsonObject data = gson.toJsonTree(request.getData()).getAsJsonObject();
                        String productId = data.get("productId").getAsString();
                        boolean success = ItemManager.getInstance().deleteItem(productId);
                        if (success) {
                            out.println("SUCCESS");
                            notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
                        } else {
                            out.println("FAILED");
                        }
                    }

                    else if ("UPDATE_ITEM".equals(request.getCommand())) {
                        JsonObject data = gson.toJsonTree(request.getData()).getAsJsonObject();
                        String productId = data.get("productId").getAsString();
                        String newName = data.get("name").getAsString();
                        String newDesc = data.get("desc").getAsString();
                        double newPrice = data.get("price").getAsDouble();
                        
                        boolean success = ItemManager.getInstance().updateItem(productId, newName, newDesc, newPrice);
                        if (success) {
                            out.println("SUCCESS");
                            notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
                        } else {
                            out.println("FAILED");
                        }
                    }

                    else if ("BID".equals(request.getCommand())) {
                        JsonElement jsonElement = gson.toJsonTree(request.getData());
                        BidTransaction bid = gson.fromJson(jsonElement, BidTransaction.class);
                        String realAuctionId = AuctionManager.getInstance().getFirstRunningAuctionId();

                        boolean success = AuctionManager.getInstance().placeBid(realAuctionId, bid.getBidder(), bid.getAmount());

                        if (success) {
                            out.println("SUCCESS");
                            String broadcastMsg = "{\"command\":\"UPDATE_PRICE\", \"message\":\"Sản phẩm vừa được " +
                                    bid.getBidder().getUsername() + " đặt giá " + bid.getAmount() + "\"}";
                            notifyAllObservers(broadcastMsg);
                        } else {
                            out.println("FAILED: Gia thap hon hoac phien da dong");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("--- LỖI DỮ LIỆU TỪ CLIENT ---");
                    e.printStackTrace();
                    out.println("ERROR: Sai dinh dang du lieu");
                }
            }
        } catch (IOException e) {
        } finally {
            removeObserver(this);
            System.out.println(">>> 1 Client vừa ngắt kết nối. Còn lại: " + observers.size());
        }
    }

    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public static void notifyAllObservers(String message) {
        for (AuctionObserver obs : observers) {
            obs.updateClient(message);
        }
    }

    @Override
    public void updateClient(String message) {
        if (this.out != null) {
            this.out.println(message);
        }
    }
}