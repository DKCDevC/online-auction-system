package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SignUpController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label lblMessage;

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblMessage.setTextFill(javafx.scene.paint.Color.GRAY);
        lblMessage.setText("Đang xử lý...");

        new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JsonObject regData = new JsonObject();
                regData.addProperty("command", "REGISTER");
                regData.addProperty("username", username);
                regData.addProperty("password", password);
                regData.addProperty("role", role);

                out.println(new Gson().toJson(regData));

                String response = in.readLine();
                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {
                        lblMessage.setTextFill(javafx.scene.paint.Color.web("#34c759"));
                        lblMessage.setText("Đăng ký thành công! Hãy quay lại đăng nhập.");
                    } else {
                        lblMessage.setTextFill(javafx.scene.paint.Color.RED);
                        lblMessage.setText("Tài khoản đã tồn tại.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(javafx.scene.paint.Color.RED);
                    lblMessage.setText("Lỗi kết nối Server.");
                });
            }
        }).start();
    }

    // Đã sửa lại hàm này để chuyển cảnh mượt mà và giữ nguyên toàn màn hình
    @FXML
    public void handleBackToLogin(ActionEvent event) {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));

            // Dòng lệnh quan trọng vừa được bổ sung
            stage.setScene(new Scene(root));

            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}