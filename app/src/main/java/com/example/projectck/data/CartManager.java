package com.example.projectck.data;

import com.example.projectck.models.CartItem;
import com.example.projectck.models.Food;

import java.util.ArrayList;

public class CartManager {

    public static ArrayList<CartItem> cartList = new ArrayList<>();

    public static void addToCart(Food food) {

        for (CartItem item : cartList) {
            if (item.getFood().getId().equals(food.getId())) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }

        cartList.add(new CartItem(food, 1));
    }

    public static void removeItem(CartItem item) {
        cartList.remove(item);
    }

    public static int getTotalPrice() {
        int total = 0;

        for (CartItem item : cartList) {
            total += item.getTotalPrice();
        }

        return total;
    }

    public static void clearCart() {
        cartList.clear();
    }
}