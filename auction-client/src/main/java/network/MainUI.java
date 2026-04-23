package network;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Trỏ đường dẫn tới file fxml trong thư mục resources
        URL fxmlLocation = getClass().getResource("/views/Login.fxml");
        if (fxmlLocation == null) {
            System.out.println("Không tìm thấy file Login.fxml! Hãy kiểm tra lại thư mục resources.");
            return;
        }

        Parent root = FXMLLoader.load(fxmlLocation);
        primaryStage.setTitle("Hệ thống Đấu giá ebid");

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Lệnh bung toàn màn hình chuẩn xác
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}