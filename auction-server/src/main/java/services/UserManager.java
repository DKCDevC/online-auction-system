package services;

import dao.UserDAO;
import models.User;

public class UserManager {
    private static UserManager instance;
    private UserDAO userDAO;

    private UserManager() {
        userDAO = new UserDAO();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // Hàm Login
    public User login(String username, String password) {
        return userDAO.loginUser(username, password);
    }

    // Hàm Register
    public boolean registerUser(String username, String password, String role) {
        return userDAO.insertUser(username, password, role);
    }
}