package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AddProductController {

    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory;
    @FXML private ComboBox<String> cboDuration;
    @FXML private Label lblStatus;
    @FXML private FlowPane photoContainer;

    private String sellerName;
    private List<String> imagePaths = new ArrayList<>();

    private String getCategoryType() {
        if (cboCategory.getValue() == null) return "GENERAL";
        switch (cboCategory.getValue()) {
            case "Đồ điện tử": return "ELECTRONICS";
            case "Nghệ thuật": return "ART";
            case "Xe cộ": return "VEHICLE";
            default: return "GENERAL";
        }
    }

    @FXML
    public void initialize() {
        // Khởi tạo danh mục
        cboCategory.setItems(FXCollections.observableArrayList(
                "Khác (Chung)",
                "Đồ điện tử",
                "Nghệ thuật",
                "Xe cộ"
        ));
        cboCategory.setValue("Khác (Chung)");

        // Khởi tạo thời gian
        cboDuration.setItems(FXCollections.observableArrayList(
                "1 Ngày",
                "3 Ngày",
                "5 Ngày",
                "7 Ngày",
                "14 Ngày"
        ));
        cboDuration.setValue("7 Ngày");
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    @FXML
    public void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = (Stage) txtProductName.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (imagePaths.size() >= 5) {
                    lblStatus.setText("Tối đa 5 ảnh!");
                    break;
                }
                String path = file.toURI().toString();
                imagePaths.add(path);

                ImageView imageView = new ImageView(new Image(path));
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                
                photoContainer.getChildren().add(imageView);
            }
        }
    }

    @FXML
    public void handleSaveProduct(ActionEvent event) {
        String name = txtProductName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        String desc = txtDescription.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            lblStatus.setText("Vui lòng nhập tên và giá khởi điểm!");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            lblStatus.setStyle("-fx-text-fill: gray;");
            lblStatus.setText("Đang đẩy lên Server...");

            String type = getCategoryType();

            // Convert danh sách ảnh thành chuỗi cách nhau bởi dấu phẩy
            String extraImages = String.join(",", imagePaths);

            // Lấy thời gian (số ngày)
            int durationDays = 7;
            try {
                String durStr = cboDuration.getValue().replace(" Ngày", "").trim();
                durationDays = Integer.parseInt(durStr);
            } catch (Exception ex) {}
            
            final int finalDuration = durationDays;

            new Thread(() -> {
                try {
                    Socket socket = new Socket("127.0.0.1", 9999);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    JsonObject data = new JsonObject();
                    data.addProperty("type", type);
                    data.addProperty("name", name);
                    data.addProperty("desc", desc);
                    data.addProperty("price", price);
                    data.addProperty("extra", extraImages);
                    data.addProperty("seller", this.sellerName);
                    data.addProperty("duration", finalDuration);

                    JsonObject request = new JsonObject();
                    request.addProperty("command", "ADD_ITEM");
                    request.add("data", data);

                    out.println(new Gson().toJson(request));
                    socket.close();

                    Platform.runLater(() -> {
                        lblStatus.setStyle("-fx-text-fill: green;");
                        lblStatus.setText("Đăng bán thành công!");
                        Stage stage = (Stage) txtProductName.getScene().getWindow();
                        stage.close();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        lblStatus.setStyle("-fx-text-fill: red;");
                        lblStatus.setText("Lỗi kết nối Server!");
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            lblStatus.setText("Giá khởi điểm phải là số!");
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtProductName.getScene().getWindow();
        stage.close();
    }
}