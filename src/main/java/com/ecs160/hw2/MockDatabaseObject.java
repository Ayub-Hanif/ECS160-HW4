package com.ecs160.hw2;

import java.util.Map;

public class MockDatabaseObject {
    private Map<String, String> fields;
    public void persistField(String name, Object value) {
        fields.put(name, value.toString());
    }
    public void getField(String name) {
        fields.get(name);
    }
}