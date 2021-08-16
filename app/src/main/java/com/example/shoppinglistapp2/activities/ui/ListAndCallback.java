package com.example.shoppinglistapp2.activities.ui;

import java.util.List;

public class ListAndCallback<T> {
    public List<T> list;
    public Runnable callback;

    public ListAndCallback(List<T> list, Runnable callback) {
        this.list = list;
        this.callback = callback;
    }
}
