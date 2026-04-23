package models;

import java.time.LocalDateTime;

public class GeneralItem extends Item {

    public GeneralItem(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(name, description, startingPrice, startTime, endTime);
    }

    @Override
    public void printInfo() {
        System.out.println("[Chung] " + getName() + " - Giá hiện tại: " + getCurrentHighestPrice());
    }
}
