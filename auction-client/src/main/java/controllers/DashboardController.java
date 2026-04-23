package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import models.Item;
import network.AuctionChartGUI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    // --- Header & Sidebar ---
    @FXML private Label lblGreeting;
    @FXML private TextField txtSearch;
    @FXML private Label sideBrowse;
    @FXML private Label sideCart;

    // --- Layout ---
    @FXML private StackPane contentArea;
    @FXML private VBox pageBrowse;
    @FXML private VBox pageCart;
    @FXML private VBox pageCheckout;

    // --- Page: Browse ---
    @FXML private Label lblCategoryTitle;
    @FXML private FlowPane gridItems;
    @FXML private ComboBox<String> cboFilterCondition;
    @FXML private ComboBox<String> cboFilterPrice;
    @FXML private ComboBox<String> cboFilterRating;

    // --- Page: Cart ---
    //@FXML private TableView<CartItem> tableCart;
    //@FXML private TableColumn<CartItem, String> colCartName;
    //@FXML private TableColumn<CartItem, String> colCartPrice;
    @FXML private Label lblCartTotal;
    @FXML private VBox cartListContainer;
    @FXML private Label lblCartItemsSubtotal;

    // --- Mini Cart & Overlay ---
    @FXML private Label btnMiniCart;
    @FXML private Label lblCartBadge;
    @FXML private VBox miniCartPreview;
    @FXML private VBox miniCartList;
    @FXML private Label lblMiniCartTotal;
    @FXML private StackPane cartOverlay;
    @FXML private Label lblOverlayName;
    @FXML private Label lblOverlayPrice;

    // --- Page: Product Detail ---
    @FXML private VBox pageProductDetail;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailCondition;
    
    private ItemUI selectedProductForDetail;

    // --- Page: Checkout ---
    @FXML private ToggleGroup paymentGroup;
    @FXML private RadioButton radCOD;
    @FXML private RadioButton radCard;
    @FXML private VBox boxCardInfo;
    @FXML private TextField txtAddress;
    @FXML private TextField txtPhone;
    @FXML private Label lblCheckoutTotal;
    @FXML private Label lblCheckoutFinal;

    // --- Category Labels ---
    @FXML private Label catAll;
    @FXML private Label catElectronics;
    @FXML private Label catArt;
    @FXML private Label catVehicle;
    @FXML private Label catGeneral;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String currentUsername;
    private String userRole;

    private List<ItemUI> allItems = new ArrayList<>();
    private ObservableList<ItemUI> filteredItems = FXCollections.observableArrayList();
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    
    private double cartTotalValue = 0;
    private String currentCategoryFilter = "";

    public void setUserInfo(String username, String role) {
        this.currentUsername = username;
        this.userRole = role;
        lblGreeting.setText("Xin chào, " + username + "!");
    }

    @FXML
    public void initialize() {
        // Cấu hình bảng Giỏ hàng
        //colCartName.setCellValueFactory(new PropertyValueFactory<>("name"));
        //colCartPrice.setCellValueFactory(new PropertyValueFactory<>("priceStr"));

        // Setup filter comboboxes
        cboFilterCondition.setItems(FXCollections.observableArrayList("Tất cả", "Mới (New)", "Đã sử dụng (Used)"));
        cboFilterCondition.setValue("Tất cả");
        cboFilterCondition.setOnAction(e -> applySearchFilter());

        cboFilterPrice.setItems(FXCollections.observableArrayList("Tất cả", "Dưới 1,000,000 ₫", "1,000,000 ₫ - 5,000,000 ₫", "Trên 5,000,000 ₫"));
        cboFilterPrice.setValue("Tất cả");
        cboFilterPrice.setOnAction(e -> applySearchFilter());

        cboFilterRating.setItems(FXCollections.observableArrayList("Tất cả", "4 Sao trở lên", "5 Sao"));
        cboFilterRating.setValue("Tất cả");
        cboFilterRating.setOnAction(e -> applySearchFilter());

        // Mini Cart Hover Logic
        btnMiniCart.setOnMouseEntered(e -> {
            miniCartPreview.setVisible(true);
            miniCartPreview.setManaged(true);
            renderMiniCart();
        });
        
        // Hide mini cart when mouse leaves the container area
        miniCartPreview.setOnMouseExited(e -> {
            miniCartPreview.setVisible(false);
            miniCartPreview.setManaged(false);
        });

        // Toggle card info based on payment method
        paymentGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean newVal = (newV == radCard);
            boxCardInfo.setVisible(newVal);
            boxCardInfo.setManaged(newVal);
        });

        //tableCart.setItems(cartItems);

        // Xử lý tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter();
        });

        startBackgroundListener();
    }

    // ==========================================
    // MENU NAVIGATION
    // ==========================================

    @FXML
    public void handleSideBrowse(MouseEvent event) {
        showPage(pageBrowse);
        setActiveSidebar(sideBrowse);
    }

    @FXML
    public void handleSideCart(MouseEvent event) {
        showPage(pageCart);
        setActiveSidebar(sideCart);
        renderFullCart();
    }

    private void setActiveSidebar(Label label) {
        sideBrowse.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideBrowse.setStyle("-fx-cursor: hand;");
        sideCart.setTextFill(javafx.scene.paint.Color.web("#707070"));
        sideCart.setStyle("-fx-cursor: hand;");
        
        label.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        label.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
    }

    private void showPage(VBox page) {
        pageBrowse.setVisible(false);
        pageBrowse.setManaged(false);
        pageCart.setVisible(false);
        pageCart.setManaged(false);
        pageCheckout.setVisible(false);
        pageCheckout.setManaged(false);
        pageProductDetail.setVisible(false);
        pageProductDetail.setManaged(false);

        page.setVisible(true);
        page.setManaged(true);
    }

    // ==========================================
    // CATEGORY FILTERS
    // ==========================================

    private void resetCategoryStyles() {
        Label[] cats = {catAll, catElectronics, catArt, catVehicle, catGeneral};
        for (Label cat : cats) {
            cat.setTextFill(javafx.scene.paint.Color.web("#707070"));
            cat.setStyle("-fx-cursor: hand;");
        }
    }

    private void setActiveCategory(Label cat, String type, String title) {
        resetCategoryStyles();
        cat.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
        cat.setStyle("-fx-cursor: hand; -fx-font-weight: bold;");
        currentCategoryFilter = type;
        lblCategoryTitle.setText(title);
        applySearchFilter();
        
        // Đảm bảo đang ở trang Browse
        handleSideBrowse(null);
    }

    @FXML public void handleCategoryAll(MouseEvent event) { setActiveCategory(catAll, "", "Tất cả sản phẩm"); }
    @FXML public void handleCategoryElectronics(MouseEvent event) { setActiveCategory(catElectronics, "ELECTRONICS", "Đồ điện tử"); }
    @FXML public void handleCategoryArt(MouseEvent event) { setActiveCategory(catArt, "ART", "Nghệ thuật"); }
    @FXML public void handleCategoryVehicle(MouseEvent event) { setActiveCategory(catVehicle, "VEHICLE", "Xe cộ"); }
    @FXML public void handleCategoryGeneral(MouseEvent event) { setActiveCategory(catGeneral, "GENERAL", "Sản phẩm khác"); }

    // ==========================================
    // NETWORK & DATA
    // ==========================================

    private void startBackgroundListener() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 9999);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Xin list ban đầu
                out.println("{\"command\":\"GET_ITEMS\"}");

                String response;
                while ((response = in.readLine()) != null) {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        String cmd = json.get("command").getAsString();

                        if ("SET_ITEMS".equals(cmd)) {
                            JsonArray dataArray = json.getAsJsonArray("data");
                            List<ItemUI> newItems = new ArrayList<>();
                            
                            for (com.google.gson.JsonElement element : dataArray) {
                                JsonObject obj = element.getAsJsonObject();
                                String name = obj.has("name") ? obj.get("name").getAsString() : "Sản phẩm";
                                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                                double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0.0;
                                double currentPrice = obj.has("currentHighestPrice") ? obj.get("currentHighestPrice").getAsDouble() : startPrice;
                                if (currentPrice <= 0) currentPrice = startPrice;
                                
                                String type = obj.has("type") ? obj.get("type").getAsString() : "GENERAL";
                                // Fallback type checking if server doesn't provide type field
                                if ("GENERAL".equals(type)) {
                                    if (obj.has("manufacturer") || obj.has("model")) type = "ELECTRONICS";
                                    else if (obj.has("artist") || obj.has("medium")) type = "ART";
                                    else if (obj.has("make") || obj.has("mileage")) type = "VEHICLE";
                                }
                                
                                String endTime = "N/A";
                                if (obj.has("endTime")) {
                                    try {
                                        JsonObject endObj = obj.getAsJsonObject("endTime");
                                        if (endObj.has("date") && endObj.has("time")) {
                                            JsonObject d = endObj.getAsJsonObject("date");
                                            endTime = String.format("%04d-%02d-%02d", d.get("year").getAsInt(), d.get("month").getAsInt(), d.get("day").getAsInt());
                                        }
                                    } catch (Exception e) {
                                        endTime = obj.get("endTime").getAsString();
                                    }
                                }
                                
                                String sellerName = "Unknown";
                                if (obj.has("seller")) {
                                    try {
                                        JsonObject sellerObj = obj.getAsJsonObject("seller");
                                        sellerName = sellerObj.has("username") ? sellerObj.get("username").getAsString() : "Unknown";
                                    } catch (Exception e) {}
                                }

                                newItems.add(new ItemUI(
                                    name, desc, currentPrice, "Đang đấu giá", startPrice, endTime, type, sellerName
                                ));
                            }

                            Platform.runLater(() -> {
                                allItems = newItems;
                                applySearchFilter();
                            });
                        }
                        else if ("UPDATE_PRICE".equals(cmd)) {
                            out.println("{\"command\":\"GET_ITEMS\"}");
                        }
                    } catch (Exception ex) {
                        System.out.println("Lỗi xử lý JSON: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi kết nối Socket tại Dashboard.");
            }
        }).start();
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        applySearchFilter();
    }

    private void applySearchFilter() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String filterCond = cboFilterCondition.getValue();
        String filterPrice = cboFilterPrice.getValue();
        
        List<ItemUI> result = new ArrayList<>();
        
        for (ItemUI item : allItems) {
            // Lọc theo category
            if (!currentCategoryFilter.isEmpty() && !currentCategoryFilter.equalsIgnoreCase(item.getType())) {
                continue;
            }
            // Lọc theo từ khóa
            if (!keyword.isEmpty()) {
                boolean match = (item.getName() != null && item.getName().toLowerCase().contains(keyword)) ||
                                (item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword));
                if (!match) continue;
            }
            
            // Lọc theo Giá (Price)
            if (filterPrice != null && !filterPrice.equals("Tất cả")) {
                double price = item.getRawPrice();
                if (filterPrice.equals("Dưới 1,000,000 ₫") && price >= 1000000) continue;
                if (filterPrice.equals("1,000,000 ₫ - 5,000,000 ₫") && (price < 1000000 || price > 5000000)) continue;
                if (filterPrice.equals("Trên 5,000,000 ₫") && price <= 5000000) continue;
            }
            
            result.add(item);
        }
        
        filteredItems.setAll(result);
        renderProductCards();
    }

    // ==========================================
    // RENDER PRODUCT CARDS
    // ==========================================

    private void renderProductCards() {
        gridItems.getChildren().clear();
        for (ItemUI item : filteredItems) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 8; -fx-padding: 0 0 15 0; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");
            card.setPrefWidth(260);
            
            // eBay Hover Effect: Scale up 5%
            card.setOnMouseEntered(e -> {
                card.setScaleX(1.05);
                card.setScaleY(1.05);
                card.toFront(); 
            });
            card.setOnMouseExited(e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
            });

            // Click to view details
            card.setOnMouseClicked(e -> {
                showProductDetail(item);
            });

            // Image Placeholder
            Label lblImg = new Label("Image Not Available");
            lblImg.setStyle("-fx-background-color: #eaeaea; -fx-text-fill: #999999; -fx-alignment: center; -fx-font-size: 14; -fx-background-radius: 8 8 0 0;");
            lblImg.setPrefSize(260, 200);

            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-padding: 10 15;");

            // Tên sản phẩm
            Label lblName = new Label(item.getName());
            lblName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            lblName.setTextFill(javafx.scene.paint.Color.web("#0654ba"));
            lblName.setWrapText(true);
            lblName.setMaxHeight(40);
            
            // Tình trạng & Rating (Mô phỏng)
            javafx.scene.layout.HBox ratingBox = new javafx.scene.layout.HBox(5);
            Label lblCondition = new Label("New");
            lblCondition.setTextFill(javafx.scene.paint.Color.web("#707070"));
            lblCondition.setStyle("-fx-font-size: 12px;");
            Label lblStars = new Label("⭐⭐⭐⭐⭐ (12)");
            lblStars.setStyle("-fx-font-size: 11px;");
            ratingBox.getChildren().addAll(lblCondition, new Label("•"), lblStars);

            // Giá
            Label lblPrice = new Label(item.getPriceStr());
            lblPrice.setTextFill(javafx.scene.paint.Color.web("#191919"));
            lblPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));

            // Trạng thái / Free Shipping
            Label lblStatus = new Label("Free international shipping");
            lblStatus.setTextFill(javafx.scene.paint.Color.web("#707070"));
            lblStatus.setStyle("-fx-font-size: 12px;");
            
            // Số lượng đã bán
            Label lblSold = new Label("150+ sold");
            lblSold.setTextFill(javafx.scene.paint.Color.web("#dd1e31"));
            lblSold.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

            infoBox.getChildren().addAll(lblName, ratingBox, lblPrice, lblStatus, lblSold);
            card.getChildren().addAll(lblImg, infoBox);
            
            gridItems.getChildren().add(card);
        }
    }

    private void doPlaceBid(ItemUI item, String amountStr) {
        if (amountStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(amountStr);
            if (bidAmount <= item.getRawPrice()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giá đấu phải cao hơn giá hiện tại!");
                return;
            }

            JsonObject bidRequest = new JsonObject();
            bidRequest.addProperty("command", "BID");

            JsonObject data = new JsonObject();
            JsonObject bidder = new JsonObject();
            bidder.addProperty("username", currentUsername);
            data.add("bidder", bidder);
            data.addProperty("amount", bidAmount);

            bidRequest.add("data", data);
            out.println(new Gson().toJson(bidRequest));

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi yêu cầu đặt giá: " + String.format("%,.0f", bidAmount) + " ₫");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng nhập số hợp lệ!");
        }
    }

    // ==========================================
    // CART & CHECKOUT
    // ==========================================

    /*@FXML
    public void handleRemoveFromCart(ActionEvent event) {
        CartItem selected = tableCart.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartItems.remove(selected);
            updateCartTotal();
        } else {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn sản phẩm để xóa!");
        }
    }*/

    private void renderMiniCart() {
        miniCartList.getChildren().clear();
        double total = 0;
        for (CartItem item : cartItems) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label name = new Label(item.getName());
            name.setPrefWidth(150);
            Label price = new Label(item.getPriceStr());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(name, spacer, price);
            miniCartList.getChildren().add(row);
            total += item.getRawPrice();
        }
        lblMiniCartTotal.setText(String.format("%,.0f ₫", total));
    }

    private void renderFullCart() {
        cartListContainer.getChildren().clear();
        double total = 0;
        for (CartItem item : cartItems) {
            HBox card = new HBox(20);
            card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #E0E0E0; -fx-border-radius: 12;");
            card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label imgPlaceholder = new Label("IMG");
            imgPlaceholder.setPrefSize(120, 120);
            imgPlaceholder.setStyle("-fx-background-color: #f0f0f0; -fx-alignment: center; -fx-background-radius: 8;");
            
            VBox info = new VBox(8);
            Label name = new Label(item.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 18;");
            name.setWrapText(true);
            name.setPrefWidth(400);
            
            Label condition = new Label("Used");
            condition.setTextFill(javafx.scene.paint.Color.web("#707070"));
            
            HBox controls = new HBox(15);
            controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label qty = new Label("Qty: 1");
            Button btnRemove = new Button("Remove");
            btnRemove.setStyle("-fx-text-fill: #0654ba; -fx-background-color: transparent; -fx-underline: true; -fx-cursor: hand;");
            btnRemove.setOnAction(e -> {
                cartItems.remove(item);
                renderFullCart();
                updateCartTotal();
            });
            controls.getChildren().addAll(qty, new Separator(javafx.geometry.Orientation.VERTICAL), btnRemove);
            
            info.getChildren().addAll(name, condition, controls);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label price = new Label(item.getPriceStr());
            price.setStyle("-fx-font-weight: bold; -fx-font-size: 20;");
            
            card.getChildren().addAll(imgPlaceholder, info, spacer, price);
            cartListContainer.getChildren().add(card);
            total += item.getRawPrice();
        }
        lblCartTotal.setText(String.format("%,.0f ₫", total));
        lblCartItemsSubtotal.setText(String.format("%,.0f ₫", total));
    }

    private void updateCartTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getRawPrice();
        }
        lblCartBadge.setText(String.valueOf(cartItems.size()));
        lblCartBadge.setVisible(cartItems.size() > 0);
    }

    private void showProductDetail(ItemUI item) {
        selectedProductForDetail = item;
        lblDetailName.setText(item.getName());
        lblDetailPrice.setText(item.getPriceStr());
        lblDetailCondition.setText("New"); 
        showPage(pageProductDetail);
    }

    @FXML
    public void handleAddToCartFromDetail(ActionEvent event) {
        if (selectedProductForDetail != null) {
            cartItems.add(new CartItem(selectedProductForDetail.getName(), selectedProductForDetail.getRawPrice()));
            updateCartTotal();
            
            lblOverlayName.setText(selectedProductForDetail.getName());
            lblOverlayPrice.setText(selectedProductForDetail.getPriceStr());
            cartOverlay.setVisible(true);
        }
    }

    @FXML
    public void handleCloseCartOverlay(ActionEvent event) {
        cartOverlay.setVisible(false);
    }

    @FXML
    public void handleProceedToCheckout(ActionEvent event) {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giỏ hàng trống!");
            return;
        }
        
        lblCheckoutTotal.setText(String.format("%,.0f ₫", cartTotalValue));
        lblCheckoutFinal.setText(String.format("%,.0f ₫", cartTotalValue));
        
        showPage(pageCheckout);
    }

    @FXML
    public void handleBackToCart(ActionEvent event) {
        showPage(pageCart);
    }

    @FXML
    public void handlePlaceOrder(ActionEvent event) {
        String address = txtAddress.getText().trim();
        String phone = txtPhone.getText().trim();

        if (address.isEmpty() || phone.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ địa chỉ và số điện thoại!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán");
        confirm.setHeaderText("Bạn có chắc chắn muốn đặt hàng?");
        
        String method = radCOD.isSelected() ? "Thanh toán trực tiếp (COD)" : "Thanh toán qua thẻ tín dụng";
        confirm.setContentText("Tổng số tiền: " + String.format("%,.0f ₫", cartTotalValue) + "\nPhương thức: " + method);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Đặt hàng thành công
            cartItems.clear();
            updateCartTotal();
            txtAddress.clear();
            txtPhone.clear();
            
            showAlert(Alert.AlertType.INFORMATION, "Đặt hàng thành công", "Đơn hàng của bạn đã được ghi nhận hệ thống!");
            showPage(pageBrowse);
        }
    }

    // ==========================================
    // UTILS
    // ==========================================

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            if (socket != null) socket.close();
            Stage stage = (Stage) lblGreeting.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá eBid");
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDetailPopup(ItemUI item) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chi tiết sản phẩm & Thông tin Người bán");
            alert.setHeaderText(null);
            
            VBox container = new VBox(20);
            container.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 20; -fx-pref-width: 600;");

            // 1. PRODUCT SECTION
            VBox productBox = new VBox(10);
            productBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
            Label lblName = new Label(item.getName());
            lblName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));
            
            Label lblPrice = new Label("Giá hiện tại: " + item.getPriceStr());
            lblPrice.setTextFill(javafx.scene.paint.Color.web("#ee4d2d"));
            lblPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
            
            Label lblDesc = new Label("Mô tả: " + item.getDescription());
            lblDesc.setWrapText(true);
            Label lblTime = new Label("Kết thúc đấu giá: " + item.getEndTime());
            productBox.getChildren().addAll(lblName, lblPrice, lblDesc, lblTime);

            // 2. SELLER PROFILE SECTION (SHOPEE STYLE)
            javafx.scene.layout.HBox shopBox = new javafx.scene.layout.HBox(20);
            shopBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
            
            // Avatar
            Label lblAvatar = new Label(item.getSellerName().substring(0, 1).toUpperCase());
            lblAvatar.setStyle("-fx-background-color: #ee4d2d; -fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 30; -fx-min-width: 60; -fx-min-height: 60;");
            
            VBox shopInfo = new VBox(5);
            Label lblShopName = new Label("Shop: " + item.getSellerName());
            lblShopName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            
            javafx.scene.layout.HBox shopButtons = new javafx.scene.layout.HBox(10);
            Button btnChat = new Button("Chat Ngay");
            btnChat.setStyle("-fx-background-color: #ee4d2d; -fx-text-fill: white;");
            Button btnViewShop = new Button("Xem Shop");
            shopButtons.getChildren().addAll(btnChat, btnViewShop);
            shopInfo.getChildren().addAll(lblShopName, shopButtons);

            // Thống kê
            javafx.scene.layout.GridPane statsPane = new javafx.scene.layout.GridPane();
            statsPane.setHgap(30);
            statsPane.setVgap(5);
            statsPane.add(new Label("Đánh giá:"), 0, 0);
            Label l1 = new Label("219,4k"); l1.setTextFill(javafx.scene.paint.Color.web("#ee4d2d"));
            statsPane.add(l1, 1, 0);
            
            statsPane.add(new Label("Sản phẩm:"), 0, 1);
            Label l2 = new Label("9,8k"); l2.setTextFill(javafx.scene.paint.Color.web("#ee4d2d"));
            statsPane.add(l2, 1, 1);
            
            statsPane.add(new Label("Tỉ lệ Phản hồi:"), 2, 0);
            Label l3 = new Label("94%"); l3.setTextFill(javafx.scene.paint.Color.web("#ee4d2d"));
            statsPane.add(l3, 3, 0);
            
            shopBox.getChildren().addAll(lblAvatar, shopInfo, new Separator(javafx.geometry.Orientation.VERTICAL), statsPane);

            // 3. REVIEWS SECTION
            VBox reviewsBox = new VBox(10);
            reviewsBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
            Label lblReviewTitle = new Label("ĐÁNH GIÁ SẢN PHẨM");
            lblReviewTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            
            Label lblStars = new Label("⭐⭐⭐⭐⭐ 4.9 trên 5");
            lblStars.setTextFill(javafx.scene.paint.Color.web("#ee4d2d"));
            lblStars.setFont(javafx.scene.text.Font.font("System", 16));
            
            VBox singleReview = new VBox(5);
            Label lblUser = new Label("nguyenvana123  ⭐⭐⭐⭐⭐");
            Label lblComment = new Label("Shop đóng gói siêu cẩn thận, hàng chất lượng chuẩn như mô tả. Sẽ ủng hộ tiếp!");
            lblComment.setWrapText(true);
            singleReview.getChildren().addAll(lblUser, lblComment);
            
            reviewsBox.getChildren().addAll(lblReviewTitle, lblStars, new Separator(), singleReview);

            container.getChildren().addAll(productBox, shopBox, reviewsBox);
            
            alert.getDialogPane().setContent(container);
            alert.showAndWait();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    // ==========================================
    // DATA CLASSES
    // ==========================================

    public static class ItemUI {
        private String name;
        private String description;
        private double rawPrice;
        private String priceStr;
        private String status;
        private double startingPrice;
        private String endTime;
        private String type;
        private String sellerName;

        public ItemUI(String name, String description, double rawPrice, String status, double startingPrice, String endTime, String type, String sellerName) {
            this.name = name;
            this.description = description;
            this.rawPrice = rawPrice;
            this.priceStr = String.format("%,.0f ₫", rawPrice);
            this.status = status;
            this.startingPrice = startingPrice;
            this.endTime = endTime;
            this.type = type;
            this.sellerName = sellerName;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPriceStr() { return priceStr; }
        public String getStatus() { return status; }
        public double getRawPrice() { return rawPrice; }
        public double getStartingPrice() { return startingPrice; }
        public String getEndTime() { return endTime; }
        public String getType() { return type; }
        public String getSellerName() { return sellerName; }
    }

    public static class CartItem {
        private String name;
        private double rawPrice;
        private String priceStr;

        public CartItem(String name, double rawPrice) {
            this.name = name;
            this.rawPrice = rawPrice;
            this.priceStr = String.format("%,.0f ₫", rawPrice);
        }

        public String getName() { return name; }
        public String getPriceStr() { return priceStr; }
        public double getRawPrice() { return rawPrice; }
    }
}