package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML private CheckBox chkRemember;

    // Khai báo Preferences để lưu thông tin vào hệ thống máy tính
    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    // Hàm initialize tự động chạy khi mở màn hình
    @FXML
    public void initialize() {
        // Lấy thông tin đã lưu (nếu có)
        String savedUser = prefs.get("username", "");
        String savedPass = prefs.get("password", "");
        boolean isRemembered = prefs.getBoolean("remember", false);

        if (isRemembered) {
            txtUsername.setText(savedUser);
            txtPassword.setText(savedPass);
            if (chkRemember != null) {
                chkRemember.setSelected(true);
            }
        }
    }

    // Bắt sự kiện khi bấm nút Đăng nhập
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblError.setText("Đang kết nối...");
        lblError.setTextFill(javafx.scene.paint.Color.web("#8e8e93")); // Màu xám

        new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JsonObject loginData = new JsonObject();
                loginData.addProperty("command", "LOGIN");
                loginData.addProperty("username", username);
                loginData.addProperty("password", password);

                out.println(new Gson().toJson(loginData));

                String response = in.readLine();
                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {

                        // --- LƯU THÔNG TIN NẾU CHỌN DUY TRÌ ĐĂNG NHẬP ---
                        if (chkRemember != null && chkRemember.isSelected()) {
                            prefs.put("username", username);
                            prefs.put("password", password);
                            prefs.putBoolean("remember", true);
                        } else {
                            prefs.remove("username");
                            prefs.remove("password");
                            prefs.putBoolean("remember", false);
                        }
                        // ---------------------------------------------------------

                        // 1. Lấy role từ JSON do Server trả về
                        String role = jsonResponse.get("role").getAsString();

                        lblError.setTextFill(javafx.scene.paint.Color.web("#34c759")); // Xanh lá
                        lblError.setText("Đăng nhập thành công!");

                        // 2. PHÂN LUỒNG CHUYỂN CẢNH DỰA VÀO ROLE
                        try {
                            Stage stage = (Stage) btnLogin.getScene().getWindow();
                            FXMLLoader loader;
                            Parent root;

                            if ("SELLER".equalsIgnoreCase(role)) {
                                // Mở giao diện Quản lý sản phẩm dành riêng cho Seller
                                loader = new FXMLLoader(getClass().getResource("/views/SellerDashboard.fxml"));
                                root = loader.load();

                                SellerDashboardController sellerCtrl = loader.getController();
                                sellerCtrl.setUserInfo(username);

                                stage.setScene(new Scene(root));
                                stage.setMaximized(true); // Đã chỉnh để bung toàn màn hình
                            } else {
                                // Mở giao diện Đấu giá (Dành cho Bidder / Admin)
                                loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
                                root = loader.load();

                                DashboardController dashboard = loader.getController();
                                dashboard.setUserInfo(username, role);

                                stage.setScene(new Scene(root));
                                stage.setMaximized(true); // Đã chỉnh để bung toàn màn hình
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                            lblError.setText("Lỗi khi tải màn hình chính.");
                        }
                    } else {
                        lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30")); // Đỏ
                        lblError.setText("Tài khoản hoặc mật khẩu không đúng.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                    lblError.setText("Không thể kết nối tới máy chủ.");
                });
            }
        }).start();
    }

    // Bắt sự kiện khi bấm nút "Đăng ký ngay"
    @FXML
    public void handleGoToSignUp(ActionEvent event) {
        try {
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/SignUp.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true); // Đã chỉnh để bung toàn màn hình
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}