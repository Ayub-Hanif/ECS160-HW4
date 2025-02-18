package com.ecs160.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.PersistenceAnnotations.Persistable;
import com.ecs160.persistence.PersistenceAnnotations.PersistableField;
import com.ecs160.persistence.PersistenceAnnotations.PersistableId;
import com.ecs160.persistence.PersistenceAnnotations.PersistableListField;
import com.ecs160.persistence.PersistenceAnnotations.LazyLoad;

import redis.clients.jedis.Jedis;

public class Session {
    private List<Object> objBuffer = new ArrayList<Object>();
    private static final int PORT_NUMBER = 6379;
    private Integer current_id = 1;
    private List<Integer> persistedHashCodes = new ArrayList<>();
    private Jedis jedis;

    public Session() {
        jedis = new Jedis("localhost", PORT_NUMBER);
        jedis.flushAll();
    }

    public void add(Object obj) {
        objBuffer.add(obj);
    }

    private static Jedis connectToJedis() {
        try (Jedis jedis = new Jedis("localhost", PORT_NUMBER)) {
            // jedis.auth("password");
            // System.out.println("Connection Successful");
            // System.out.println("Ping: " + jedis.ping());
            // jedis.flushAll();
            return jedis;
        }
    }

    private static String getObjectId(Object obj) {
        Class<?> clazz = obj.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(PersistableId.class)) {
                f.setAccessible(true);
                try {
                    return f.get(obj).toString();
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("No field annotated with @PersistableId found.");
    }

    /*
     * Gets the id string from a persisted object (i.e. an object that has been
     * persisted with persistAll).
     * IMPORTANT NOTE: assumes object id is already inside the @PersistableId-annotated field
     */
    private static String getPersistedObjIdString(Object partialObject) {
        Class<?> clazz = partialObject.getClass();
        String ret = "";
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    ret = field.get(partialObject).toString();
                    break; // we found the id field, no need to look through any other fields
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    private static List<Field> getAllFields(Object obj) {
        Class<?> clazz = obj.getClass();
        List<Field> ret = new ArrayList<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                ret.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return ret;
    }

    public void setObjId(Object obj) {
        System.out.println("setting obj id for " + obj.getClass().getName() + " with id " + current_id);
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    field.set(obj, current_id);
                    current_id++;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void persistObject(Object targetObj) throws IllegalArgumentException, IllegalAccessException {
        if (persistedHashCodes.contains(targetObj.hashCode())) {
            System.out.println("already persisted " + targetObj.getClass().getName());
            return;
        }
        Class<?> clazz = targetObj.getClass();
        if (!clazz.isAnnotationPresent(Persistable.class)) {
            return;
        }
        setObjId(targetObj);

        // set the id equal to the current_id
        String objStringId = getObjectId(targetObj);

        // persistable field is just written as is,
        // persistable list field persists all the IDs in a comma-separated list
        for (Field field : getAllFields(targetObj)) {
            if (field.isAnnotationPresent(PersistableField.class)) {
                field.setAccessible(true);
                Object value = field.get(targetObj);
                String valueStr = (value != null) ? value.toString() : "";
                System.out.println("Persisting field: " + field.getName() + " with value: " + valueStr + " for id " + objStringId);
                jedis.hset(objStringId, field.getName(), valueStr);
            } else if (field.isAnnotationPresent(PersistableListField.class)) {
                field.setAccessible(true);
                StringBuilder persistedIdList = new StringBuilder(); // comma separated list of IDs
                List<?> itemList = (List<?>) field.get(targetObj);
                if (itemList != null && !itemList.isEmpty()) {
                    for (Object item : itemList) {
                        persistObject(item);
                        persistedIdList.append(getObjectId(item)).append(",");
                    }
                    // Remove trailing comma
                    persistedIdList.setLength(persistedIdList.length() - 1);
                }
                jedis.hset(objStringId, field.getName(), persistedIdList.toString());
            } else if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                field.set(targetObj, Integer.parseInt(objStringId)); // set the ID of the object to the current ID
            }
        }
        persistedHashCodes.add(targetObj.hashCode());
    }

    public void persistAll() throws IllegalArgumentException, IllegalAccessException {
        for (Object obj : objBuffer) {
            persistObject(obj);
        }
        objBuffer.clear();
    }

    public Object load(Object object) {
        // get the id from the field of object with annotation
        // Loads an object from Redis based on its ID field
        String objId = getPersistedObjIdString(object);
        if (objId == null || objId.isEmpty()) {
            System.err.println("Invalid object id for loading.");
            return null;
        }
        Object ret = null;
        Class<?> clazz = object.getClass();
        try {
            ret = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        // IMPORTANT NOTE: assumes object id is already inside the @PersistableId-annotated field
        for (Field field : getAllFields(ret)) {
            if (field.isAnnotationPresent(PersistableField.class)) {
                field.setAccessible(true);
                String storedValue = jedis.hget(objId, field.getName());
                try {
                    if (field.getType() == String.class) {
                        field.set(ret, storedValue);
                    } else if (field.getType() == Integer.class) {
                        field.set(ret, (storedValue != null && !storedValue.isEmpty())
                                ? Integer.parseInt(storedValue) : null);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (field.isAnnotationPresent(PersistableListField.class)) {
                field.setAccessible(true);
                // If the field is annotated with @LazyLoad, create a dynamic proxy
                if (field.isAnnotationPresent(LazyLoad.class)) {
                    @SuppressWarnings("unchecked")
                    List<Object> lazyProxy = (List<Object>) Proxy.newProxyInstance(
                            List.class.getClassLoader(),
                            new Class[] { List.class },
                            new LazyListHandler(objId, field)
                    );
                    try {
                        field.set(ret, lazyProxy);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Otherwise, load the list immediately
                    String storedIds = jedis.hget(objId, field.getName());
                    List<Object> loadedList = new ArrayList<>();
                    if (storedIds != null && !storedIds.isEmpty()) {
                        String[] ids = storedIds.split(",");
                        for (String idStr : ids) {
                            if (idStr.trim().isEmpty()) continue;
                            try {
                                Class<?> listElementClass = Class.forName(
                                        field.getAnnotation(PersistableListField.class).className());
                                Object listElement = listElementClass.getDeclaredConstructor().newInstance();
                                for (Field f : listElementClass.getDeclaredFields()) {
                                    if (f.isAnnotationPresent(PersistableId.class)) {
                                        f.setAccessible(true);
                                        f.set(listElement, Integer.parseInt(idStr));
                                        break;
                                    }
                                }
                                listElement = load(listElement);
                                loadedList.add(listElement);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        field.set(ret, loadedList);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ret;
    }

    //lazy loading fields.
    //I made it Dynamic proxy handler, loading until a method is invoked on the list.
    private class LazyListHandler implements InvocationHandler {
        private String parentId;
        private Field field;
        private List<Object> loadedList; // Cached delegate list

        public LazyListHandler(String parentId, Field field) {
            this.parentId = parentId;
            this.field = field;
        }

        private void loadIfNeeded() {
            if (loadedList == null) {
                loadedList = new ArrayList<>();
                String storedIds = jedis.hget(parentId, field.getName());
                if (storedIds != null && !storedIds.isEmpty()) {
                    String[] ids = storedIds.split(",");
                    for (String idStr : ids) {
                        if (idStr.trim().isEmpty()) continue;
                        try {
                            Class<?> listElementClass = Class.forName(
                                    field.getAnnotation(PersistableListField.class).className());
                            Object listElement = listElementClass.getDeclaredConstructor().newInstance();
                            for (Field f : listElementClass.getDeclaredFields()) {
                                if (f.isAnnotationPresent(PersistableId.class)) {
                                    f.setAccessible(true);
                                    f.set(listElement, Integer.parseInt(idStr));
                                    break;
                                }
                            }
                            listElement = load(listElement);
                            loadedList.add(listElement);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            loadIfNeeded();
            return method.invoke(loadedList, args);
        }
    }

    // Closes the Jedis connection
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
