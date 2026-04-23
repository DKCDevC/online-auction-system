package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardController {

    @FXML private Label lblGreeting; // Đã kiểm tra không có lỗi cú pháp

    // --- Tìm kiếm và danh mục ---
    @FXML private TextField txtSearch;
    @FXML private Label catAll;
    @FXML private Label catElectronics;
    @FXML private Label catArt;
    @FXML private Label catVehicle;
    @FXML private Label catGeneral;

    // --- Thống kê tổng quan ---
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblPendingOrders;

    // --- Biểu đồ ---
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private BarChart<String, Number> revenueChartDetail;

    // --- Bảng sản phẩm tổng quan ---
    @FXML private TableView<ProductUI> tableProducts;
    @FXML private TableColumn<ProductUI, String> colId;
    @FXML private TableColumn<ProductUI, String> colName;
    @FXML private TableColumn<ProductUI, String> colPrice;
    @FXML private TableColumn<ProductUI, String> colStatus;

    // --- Bảng sản phẩm đang bán ---
    @FXML private TableView<ProductUI> tableActiveProducts;
    @FXML private TableColumn<ProductUI, String> colActiveId;
    @FXML private TableColumn<ProductUI, String> colActiveName;
    @FXML private TableColumn<ProductUI, String> colActivePrice;
    @FXML private TableColumn<ProductUI, String> colActiveEndTime;
    @FXML private TableColumn<ProductUI, String> colActiveStatus;

    // --- Bảng sản phẩm đã bán ---
    @FXML private TableView<ProductUI> tableSoldProducts;
    @FXML private TableColumn<ProductUI, String> colSoldId;
    @FXML private TableColumn<ProductUI, String> colSoldName;
    @FXML private TableColumn<ProductUI, String> colSoldPrice;
    @FXML private TableColumn<ProductUI, String> colSoldEndTime;
    @FXML private TableColumn<ProductUI, String> colSoldStatus;

    // --- Bảng đơn hàng ---
    @FXML private TableView<OrderUI> tableOrders;
    @FXML private TableColumn<OrderUI, String> colOrderId;
    @FXML private TableColumn<OrderUI, String> colOrderItem;
    @FXML private TableColumn<OrderUI, String> colOrderBuyer;
    @FXML private TableColumn<OrderUI, String> colOrderPrice;
    @FXML private TableColumn<OrderUI, String> colOrderDate;

    // --- Bảng tất cả sản phẩm (Listings) ---
    @FXML private TableView<ProductUI> tableListings;
    @FXML private TableColumn<ProductUI, String> colListId;
    @FXML private TableColumn<ProductUI, String> colListName;
    @FXML private TableColumn<ProductUI, String> colListDesc;
    @FXML private TableColumn<ProductUI, String> colListPrice;
    @FXML private TableColumn<ProductUI, String> colListStatus;

    // --- Trang doanh thu ---
    @FXML private Label lblRevenueTotalDetail;
    @FXML private Label lblSoldCount;

    // --- Các trang nội dung (StackPane) ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageOverview;
    @FXML private VBox pageActiveProducts;
    @FXML private VBox pageSoldProducts;
    @FXML private VBox pageRevenue;
    @FXML private VBox pageOrders;
    @FXML private VBox pageListings;

    // --- Tab trên cùng ---
    @FXML private Label tabOverview;
    @FXML private Label tabOrders;
    @FXML private Label tabListings;

    // --- Sidebar links ---
    @FXML private Label sideActive;
    @FXML private Label sideSold;
    @FXML private Label sideRevenue;

    private String username;

    // Lưu trữ toàn bộ dữ liệu gốc để tìm kiếm/lọc
    private List<ProductUI> allProducts = new ArrayList<>();
    private List<ProductUI> activeProducts = new ArrayList<>();
    private List<ProductUI> soldProducts = new ArrayList<>();
    private List<OrderUI> allOrders = new ArrayList<>();

    // Bộ lọc hiện tại
    private String currentSearchKeyword = "";
    private String currentCategoryFilter = ""; // "" = Tất cả

    public void setUserInfo(String username) {
        this.username = username;
        lblGreeting.setText("Xin chào, " + username + "!");

        // Ngay khi biết user là ai, gọi Server lấy dữ liệu ngay lập tức!
        loadRealDataFromServer();
    }

    @FXML
    public void initialize() {
        // Cấu hình cột bảng tổng quan
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cấu hình cột bảng sản phẩm đang bán
        colActiveId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colActiveName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colActivePrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colActiveEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colActiveStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cấu hình cột bảng sản phẩm đã bán
        colSoldId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSoldName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSoldPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSoldEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colSoldStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cấu hình cột bảng đơn hàng
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colOrderItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colOrderBuyer.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        colOrderPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        // Cấu hình cột bảng tất cả sản phẩm (Listings)
        colListId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colListName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colListDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colListPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colListStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Lắng nghe thay đổi text trong ô tìm kiếm để tìm kiếm realtime
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchKeyword = newValue != null ? newValue.trim().toLowerCase() : "";
            applyFilters();
        });
    }

    // =============================================
    // TÌM KIẾM VÀ LỌC DANH MỤC
    // =============================================

    @FXML
    public void handleSearch(ActionEvent event) {
        currentSearchKeyword = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        applyFilters();
    }

    // --- Category filter handlers ---
    private void resetCategoryStyles() {
        Label[] cats = {catAll, catElectronics, catArt, catVehicle, catGeneral};
        for (Label cat : cats) {
            cat.setTextFill(javafx.scene.paint.Color.web("#707070"));
            cat.setStyle("-fx-cursor: hand;");
        }
    }

    private void setActiveCategory(Label cat, String type) {
        resetCategoryStyles();
        cat.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        cat.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
        currentCategoryFilter = type;
        applyFilters();
    }

    @FXML public void handleCategoryAll(MouseEvent event) { setActiveCategory(catAll, ""); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { setActiveCategory(catElectronics, "ELECTRONICS"); }
    @FXML public void handleCategoryArt(MouseEvent event) { setActiveCategory(catArt, "ART"); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { setActiveCategory(catVehicle, "VEHICLE"); }
    @FXML public void handleCategoryGeneral(MouseEvent event) { setActiveCategory(catGeneral, "GENERAL"); }

    private void applyFilters() {
        // Lọc sản phẩm theo từ khóa tìm kiếm + danh mục
        List<ProductUI> filteredAll = filterProducts(allProducts);
        List<ProductUI> filteredActive = filterProducts(activeProducts);
        List<ProductUI> filteredSold = filterProducts(soldProducts);
        List<OrderUI> filteredOrders = filterOrders(allOrders);

        // Cập nhật bảng tổng quan
        tableProducts.setItems(FXCollections.observableArrayList(filteredAll));
        tableActiveProducts.setItems(FXCollections.observableArrayList(filteredActive));
        tableSoldProducts.setItems(FXCollections.observableArrayList(filteredSold));
        tableListings.setItems(FXCollections.observableArrayList(filteredAll));
        tableOrders.setItems(FXCollections.observableArrayList(filteredOrders));
    }

    private List<ProductUI> filterProducts(List<ProductUI> source) {
        List<ProductUI> result = new ArrayList<>();
        for (ProductUI p : source) {
            // Lọc theo danh mục
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(p.getType())) {
                continue;
            }
            // Lọc theo từ khóa
            if (!currentSearchKeyword.isEmpty()) {
                boolean match = (p.getName() != null && p.getName().toLowerCase().contains(currentSearchKeyword))
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(currentSearchKeyword))
                        || (p.getId() != null && p.getId().toLowerCase().contains(currentSearchKeyword));
                if (!match) continue;
            }
            result.add(p);
        }
        return result;
    }

    private List<OrderUI> filterOrders(List<OrderUI> source) {
        if (currentSearchKeyword.isEmpty()) return new ArrayList<>(source);
        List<OrderUI> result = new ArrayList<>();
        for (OrderUI o : source) {
            boolean match = (o.getItemName() != null && o.getItemName().toLowerCase().contains(currentSearchKeyword))
                    || (o.getBuyerName() != null && o.getBuyerName().toLowerCase().contains(currentSearchKeyword))
                    || (o.getOrderId() != null && o.getOrderId().toLowerCase().contains(currentSearchKeyword));
            if (match) result.add(o);
        }
        return result;
    }

    // =============================================
    // XỬ LÝ CHUYỂN TRANG
    // =============================================

    private void showPage(VBox page) {
        pageOverview.setVisible(false);
        pageActiveProducts.setVisible(false);
        pageSoldProducts.setVisible(false);
        pageRevenue.setVisible(false);
        pageOrders.setVisible(false);
        pageListings.setVisible(false);
        page.setVisible(true);
    }

    private void resetTabStyles() {
        String normalStyle = "-fx-cursor: hand; -fx-padding: 10 0 5 0;";

        tabOverview.setStyle(normalStyle);
        tabOverview.setTextFill(javafx.scene.paint.Color.web("#707070"));
        tabOrders.setStyle(normalStyle);
        tabOrders.setTextFill(javafx.scene.paint.Color.web("#707070"));
        tabListings.setStyle(normalStyle);
        tabListings.setTextFill(javafx.scene.paint.Color.web("#707070"));
    }

    private void setActiveTab(Label tab) {
        resetTabStyles();
        tab.setStyle("-fx-border-color: transparent transparent #0654ba transparent; -fx-border-width: 3; -fx-padding: 10 0 5 0; -fx-cursor: hand;");
        tab.setTextFill(javafx.scene.paint.Color.web("#191919"));
    }

    private void resetSidebarStyles() {
        sideActive.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideActive.setStyle("-fx-cursor: hand;");
        sideSold.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideSold.setStyle("-fx-cursor: hand;");
        sideRevenue.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideRevenue.setStyle("-fx-cursor: hand;");
    }

    private void setActiveSidebar(Label side) {
        resetSidebarStyles();
        side.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        side.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
    }

    // --- Tab handlers ---
    @FXML
    public void handleTabOverview(MouseEvent event) {
        showPage(pageOverview);
        setActiveTab(tabOverview);
        setActiveSidebar(sideActive);
    }

    @FXML
    public void handleTabOrders(MouseEvent event) {
        showPage(pageOrders);
        setActiveTab(tabOrders);
        resetSidebarStyles();
    }

    @FXML
    public void handleTabListings(MouseEvent event) {
        showPage(pageListings);
        setActiveTab(tabListings);
        resetSidebarStyles();
    }

    // --- Sidebar handlers ---
    @FXML
    public void handleSideActive(MouseEvent event) {
        showPage(pageActiveProducts);
        setActiveTab(tabOverview);
        setActiveSidebar(sideActive);
    }

    @FXML
    public void handleSideSold(MouseEvent event) {
        showPage(pageSoldProducts);
        setActiveTab(tabOverview);
        setActiveSidebar(sideSold);
    }

    @FXML
    public void handleSideRevenue(MouseEvent event) {
        showPage(pageRevenue);
        setActiveTab(tabOverview);
        setActiveSidebar(sideRevenue);
    }

    // =============================================
    // GỌI SERVER LẤY DỮ LIỆU
    // =============================================

    private void loadRealDataFromServer() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 1. GỬI LỆNH YÊU CẦU DATA TỚI SERVER
                JsonObject request = new JsonObject();
                request.addProperty("command", "GET_SELLER_DASHBOARD");
                request.addProperty("username", this.username);
                out.println(new Gson().toJson(request));

                // 2. NHẬN DỮ LIỆU JSON TỪ SERVER
                String response = in.readLine();

                // --- XỬ LÝ LỖI NẾU SERVER CHƯA PHẢN HỒI ---
                if (response == null || response.trim().isEmpty()) {
                    System.out.println("Lỗi: Server không trả về dữ liệu GET_SELLER_DASHBOARD");
                    return;
                }

                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                if ("SUCCESS".equals(status)) {
                    // Cập nhật Giao diện bắt buộc phải nằm trong Platform.runLater
                    Platform.runLater(() -> {
                        // Xóa dữ liệu cũ
                        allProducts.clear();
                        activeProducts.clear();
                        soldProducts.clear();
                        allOrders.clear();
                        tableProducts.getItems().clear();
                        tableActiveProducts.getItems().clear();
                        tableSoldProducts.getItems().clear();
                        tableListings.getItems().clear();
                        tableOrders.getItems().clear();
                        revenueChart.getData().clear();
                        revenueChartDetail.getData().clear();

                        // --- CẬP NHẬT 3 CON SỐ THỐNG KÊ ---
                        if (jsonResponse.has("totalRevenue")) {
                            String rev = jsonResponse.get("totalRevenue").getAsString() + " ₫";
                            lblTotalRevenue.setText(rev);
                            lblRevenueTotalDetail.setText(rev);
                        }
                        if (jsonResponse.has("activeAuctions")) {
                            lblActiveAuctions.setText(jsonResponse.get("activeAuctions").getAsString() + " Sản phẩm");
                        }
                        if (jsonResponse.has("pendingOrders")) {
                            lblPendingOrders.setText(jsonResponse.get("pendingOrders").getAsString() + " Đơn");
                        }

                        int soldCount = 0;

                        // A. ĐỔ DỮ LIỆU VÀO CÁC BẢNG
                        if (jsonResponse.has("products")) {
                            JsonArray products = jsonResponse.getAsJsonArray("products");
                            for (JsonElement element : products) {
                                JsonObject obj = element.getAsJsonObject();
                                String id = obj.get("id").getAsString();
                                String name = obj.get("name").getAsString();
                                String price = obj.get("price").getAsString() + " ₫";
                                String itemStatus = obj.get("status").getAsString();
                                String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                                String type = obj.has("type") ? obj.get("type").getAsString() : "GENERAL";

                                ProductUI product = new ProductUI(id, name, price, itemStatus, endTime, desc, type);

                                // Lưu vào danh sách gốc
                                allProducts.add(product);

                                // Phân loại: đang bán hoặc đã bán
                                if ("Đang đấu giá".equals(itemStatus)) {
                                    activeProducts.add(product);
                                } else {
                                    soldProducts.add(product);
                                    soldCount++;
                                }
                            }
                        }

                        lblSoldCount.setText(String.valueOf(soldCount));

                        // B. ĐỔ DỮ LIỆU VÀO ĐƠN HÀNG
                        if (jsonResponse.has("orders")) {
                            JsonArray orders = jsonResponse.getAsJsonArray("orders");
                            for (JsonElement element : orders) {
                                JsonObject obj = element.getAsJsonObject();
                                allOrders.add(new OrderUI(
                                        obj.get("orderId").getAsString(),
                                        obj.get("itemName").getAsString(),
                                        obj.get("buyerName").getAsString(),
                                        obj.get("price").getAsString() + " ₫",
                                        obj.get("orderDate").getAsString()
                                ));
                            }
                        }

                        // Áp dụng bộ lọc (hiển thị tất cả nếu chưa lọc gì)
                        applyFilters();

                        // C. ĐỔ DỮ LIỆU VÀO BIỂU ĐỒ
                        if (jsonResponse.has("chartData")) {
                            XYChart.Series<String, Number> series = new XYChart.Series<>();
                            series.setName("Doanh thu");
                            XYChart.Series<String, Number> series2 = new XYChart.Series<>();
                            series2.setName("Doanh thu");

                            JsonArray chartData = jsonResponse.getAsJsonArray("chartData");
                            for (JsonElement element : chartData) {
                                JsonObject dayData = element.getAsJsonObject();
                                String day = dayData.get("day").getAsString();
                                int revenue = dayData.get("revenue").getAsInt();
                                series.getData().add(new XYChart.Data<>(day, revenue));
                                series2.getData().add(new XYChart.Data<>(day, revenue));
                            }
                            revenueChart.getData().add(series);
                            revenueChartDetail.getData().add(series2);
                        }
                    });
                }

                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Lỗi kết nối tới Server khi tải Dashboard!");
            }
        }).start();
    }

    // =============================================
    // ĐĂNG XUẤT - VỀ MÀN HÌNH ĐĂNG NHẬP
    // =============================================

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá eBid");
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi chuyển về màn hình đăng nhập!");
        }
    }

    // --- LOGIC MỞ POPUP THÊM SẢN PHẨM ---
    @FXML
    public void handleAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AddProduct.fxml"));
            Parent root = loader.load();

            AddProductController addCtrl = loader.getController();

            // KIỂM TRA: Đảm bảo username không null
            if (this.username == null) {
                System.out.println("Lỗi: Username của Seller đang bị null!");
            }
            addCtrl.setSellerName(this.username);

            Stage stage = new Stage();
            stage.setTitle("Đăng bán sản phẩm mới");
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            // CHỈ DÙNG showAndWait để dừng code dashboard lại, đợi popup đóng
            stage.showAndWait();

            // SAU KHI ĐÓNG POPUP -> Tự động load lại data từ server
            loadRealDataFromServer();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi không mở được form AddProduct.fxml!");
        }
    }

    // --- XÓA VÀ SỬA SẢN PHẨM ---
    @FXML
    public void handleDeleteProduct(ActionEvent event) {
        ProductUI selected = tableActiveProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng chọn một sản phẩm để xóa!");
            alert.showAndWait();
            return;
        }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Xóa sản phẩm");
        confirm.setContentText("Bạn có chắc chắn muốn xóa sản phẩm '" + selected.getName() + "' không?");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            new Thread(() -> {
                try {
                    java.net.Socket socket = new java.net.Socket("127.0.0.1", 9999);
                    java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);

                    JsonObject data = new JsonObject();
                    data.addProperty("productId", selected.getId());
                    JsonObject req = new JsonObject();
                    req.addProperty("command", "DELETE_ITEM");
                    req.add("data", data);

                    out.println(new com.google.gson.Gson().toJson(req));
                    
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                    String response = in.readLine();
                    socket.close();

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response)) {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setContentText("Xóa thành công!");
                            a.showAndWait();
                            loadRealDataFromServer();
                        } else {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            a.setContentText("Xóa thất bại!");
                            a.showAndWait();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    public void handleEditProduct(ActionEvent event) {
        ProductUI selected = tableActiveProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setContentText("Vui lòng chọn một sản phẩm để sửa!");
            alert.showAndWait();
            return;
        }

        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(selected.getName());
        dialog.setTitle("Sửa sản phẩm");
        dialog.setHeaderText("Cập nhật thông tin cho: " + selected.getName());
        dialog.setContentText("Tên mới:");

        java.util.Optional<String> newNameOpt = dialog.showAndWait();
        if (newNameOpt.isPresent() && !newNameOpt.get().trim().isEmpty()) {
            
            javafx.scene.control.TextInputDialog priceDialog = new javafx.scene.control.TextInputDialog(selected.getPrice().replace(" ₫", "").replace(",", ""));
            priceDialog.setTitle("Sửa sản phẩm");
            priceDialog.setHeaderText("Cập nhật giá cho: " + newNameOpt.get());
            priceDialog.setContentText("Giá mới (VND):");
            
            java.util.Optional<String> newPriceOpt = priceDialog.showAndWait();
            if (newPriceOpt.isPresent()) {
                try {
                    double newPrice = Double.parseDouble(newPriceOpt.get().trim());
                    
                    new Thread(() -> {
                        try {
                            java.net.Socket socket = new java.net.Socket("127.0.0.1", 9999);
                            java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);

                            JsonObject data = new JsonObject();
                            data.addProperty("productId", selected.getId());
                            data.addProperty("name", newNameOpt.get().trim());
                            data.addProperty("desc", selected.getDescription()); // Giữ nguyên mô tả cũ
                            data.addProperty("price", newPrice);
                            
                            JsonObject req = new JsonObject();
                            req.addProperty("command", "UPDATE_ITEM");
                            req.add("data", data);

                            out.println(new com.google.gson.Gson().toJson(req));
                            
                            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                            String response = in.readLine();
                            socket.close();

                            Platform.runLater(() -> {
                                if ("SUCCESS".equals(response)) {
                                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                                    a.setContentText("Sửa thành công!");
                                    a.showAndWait();
                                    loadRealDataFromServer();
                                } else {
                                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                    a.setContentText("Sửa thất bại!");
                                    a.showAndWait();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                    
                } catch (NumberFormatException ex) {
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    a.setContentText("Giá không hợp lệ!");
                    a.showAndWait();
                }
            }
        }
    }

    // =============================================
    // CÁC CLASS DỮ LIỆU CHO BẢNG
    // =============================================

    // Class chứa dữ liệu sản phẩm để map vào TableView
    public static class ProductUI {
        private String id;
        private String name;
        private String price;
        private String status;
        private String endTime;
        private String description;
        private String type;

        public ProductUI(String id, String name, String price, String status, String endTime, String description, String type) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.status = status;
            this.endTime = endTime;
            this.description = description;
            this.type = type;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
        public String getEndTime() { return endTime; }
        public String getDescription() { return description; }
        public String getType() { return type; }
    }

    // Class chứa dữ liệu đơn hàng
    public static class OrderUI {
        private String orderId;
        private String itemName;
        private String buyerName;
        private String price;
        private String orderDate;

        public OrderUI(String orderId, String itemName, String buyerName, String price, String orderDate) {
            this.orderId = orderId;
            this.itemName = itemName;
            this.buyerName = buyerName;
            this.price = price;
            this.orderDate = orderDate;
        }

        public String getOrderId() { return orderId; }
        public String getItemName() { return itemName; }
        public String getBuyerName() { return buyerName; }
        public String getPrice() { return price; }
        public String getOrderDate() { return orderDate; }
    }
}