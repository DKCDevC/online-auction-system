package services;

import models.Art;
import models.Electronics;
import models.GeneralItem;
import models.Item;
import models.Vehicle;

import java.time.LocalDateTime;

public class ItemFactory {

    public static Item createItem(String type, String name, String description, double startingPrice,
                                  LocalDateTime startTime, LocalDateTime endTime, String extraInfo) {

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = 0;
                try { warranty = Integer.parseInt(extraInfo); } catch (NumberFormatException e) { /* mặc định 0 */ }
                return new Electronics(name, description, startingPrice, startTime, endTime, warranty);

            case "ART":
                return new Art(name, description, startingPrice, startTime, endTime, extraInfo);

            case "VEHICLE":
                return new Vehicle(name, description, startingPrice, startTime, endTime, extraInfo);

            case "GENERAL":
                return new GeneralItem(name, description, startingPrice, startTime, endTime);

            default:
                throw new IllegalArgumentException("Lỗi: Loại sản phẩm không được hỗ trợ (" + type + ")");
        }
    }
}