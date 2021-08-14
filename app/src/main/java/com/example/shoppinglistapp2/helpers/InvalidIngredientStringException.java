package com.example.shoppinglistapp2.helpers;

public class InvalidIngredientStringException extends RuntimeException {
    public InvalidIngredientStringException(String msg) {
        super(msg);
    }

    public InvalidIngredientStringException() {
        super();
    }
}
