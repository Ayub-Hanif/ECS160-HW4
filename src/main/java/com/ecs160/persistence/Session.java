package com.ecs160.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.PersistenceAnnotations.LazyLoad;
import com.ecs160.persistence.PersistenceAnnotations.Persistable;
import com.ecs160.persistence.PersistenceAnnotations.PersistableField;
import com.ecs160.persistence.PersistenceAnnotations.PersistableId;
import com.ecs160.persistence.PersistenceAnnotations.PersistableListField;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
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

    private static String getObjectId(Object obj) {
        for (Field f : getAllFields(obj)) {
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
     * IMPORTANT: assumes object id is already inside the @PersistableId-annotated
     * field.
     */
    private static String getPersistedObjIdString(Object partialObject) {
        String ret = "";
        for (Field field : getAllFields(partialObject)) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    ret = field.get(partialObject).toString();
                    break; // found the id field
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

    // Only assign a new ID if the current value is -1 (or null)
    public void setObjId(Object obj) {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    Object currentValue = field.get(obj);
                    if (currentValue == null || ((Integer) currentValue) == -1) {
                        field.set(obj, current_id);
                        current_id++;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void persistObject(Object targetObj) throws IllegalArgumentException, IllegalAccessException {
        if (persistedHashCodes.contains(targetObj.hashCode())) {
            return;
        }
        Class<?> clazz = targetObj.getClass();
        if (!clazz.isAnnotationPresent(Persistable.class)) {
            return;
        }
        setObjId(targetObj);

        String objStringId = getObjectId(targetObj);

        // Persist fields annotated with @PersistableField
        for (Field field : getAllFields(targetObj)) {
            if (field.isAnnotationPresent(PersistableField.class)) {
                field.setAccessible(true);
                Object value = field.get(targetObj);
                String valueStr = (value != null) ? value.toString() : "";
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
                // Persist the ID value as well
                field.setAccessible(true);
                jedis.hset(objStringId, field.getName(), field.get(targetObj).toString());
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

    private Object createProxy(Object object) {
        Class<?> clazz = object.getClass();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(clazz);

        MethodHandler methodHandler = new MethodHandler() {
            @Override
            public Object invoke(Object self,
                    Method thisMethod,
                    Method proceed,
                    Object[] args) throws Throwable {
                // System.out.println("accessing method " + thisMethod.getName());
                String calledMethodName = thisMethod.getName();
                if (!calledMethodName.startsWith("get")) { // this is not a getter method
                    return thisMethod.invoke(object, args);
                }
                // remove "get" prefix and lowercase first letter
                String getterFieldName = calledMethodName.substring(3);
                getterFieldName = Character.toLowerCase(getterFieldName.charAt(0)) +
                        getterFieldName.substring(1);
                // Class<?> clazz = self.getClass();
                Field field = null;
                try {
                    field = clazz.getDeclaredField(getterFieldName);
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    return thisMethod.invoke(object, args);
                }
                boolean isLazyGetter = field.isAnnotationPresent(LazyLoad.class);
                if (isLazyGetter) { // check if the called method's name refers to a lazy
                    List<Object> loadedList = new ArrayList<>();
                    for (Object item : (List<?>) field.get(object)) {
                        loadedList.add(load(item));
                    }
                    return loadedList;
                }
                return thisMethod.invoke(object, args);
            }
        };

        Class<?> proxyClass = proxyFactory.createClass();
        Object proxyObject;
        try {
            proxyObject = proxyClass.getDeclaredConstructor().newInstance();

            ((javassist.util.proxy.Proxy) proxyObject).setHandler(methodHandler);
            return proxyObject;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public Object load(Object object) {
        // Load an object from Redis based on its ID field.
        String objId = getPersistedObjIdString(object);
        if (objId == null || objId.isEmpty()) {
            System.err.println("Invalid object id for loading.");
            return null;
        }
        Object ret = null;
        Class<?> clazz = object.getClass();
        boolean isLazyLoadPresent = false;
        try {
            ret = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        // Load fields
        for (Field field : getAllFields(ret)) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                // Load the ID field from Redis.
                field.setAccessible(true);
                String storedValue = jedis.hget(objId, field.getName());
                try {
                    if (field.getType() == Integer.class && storedValue != null && !storedValue.isEmpty()) {
                        field.set(ret, Integer.parseInt(storedValue));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (field.isAnnotationPresent(PersistableField.class)) {
                field.setAccessible(true);
                String storedValue = jedis.hget(objId, field.getName());
                try {
                    if (field.getType() == String.class) {
                        field.set(ret, storedValue);
                    } else if (field.getType() == Integer.class) {
                        field.set(ret, (storedValue != null && !storedValue.isEmpty())
                                ? Integer.parseInt(storedValue)
                                : null);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else if (field.isAnnotationPresent(PersistableListField.class)) {
                field.setAccessible(true);
                // If field is annotated with @LazyLoad, create a dynamic proxy.
                // Otherwise, load the list immediately.
                String storedIds = jedis.hget(objId, field.getName());
                List<Object> loadedList = new ArrayList<>();
                if (storedIds != null && !storedIds.isEmpty()) {
                    String[] ids = storedIds.split(",");
                    for (String idStr : ids) {
                        if (idStr.trim().isEmpty())
                            continue;
                        try {
                            Class<?> listElementClass = Class.forName(
                                    field.getAnnotation(PersistableListField.class).className());
                            Object listElement = listElementClass.getDeclaredConstructor().newInstance();
                            for (Field f : getAllFields(listElement)) {
                                if (f.isAnnotationPresent(PersistableId.class)) {
                                    f.setAccessible(true);
                                    f.set(listElement, Integer.parseInt(idStr));
                                    break;
                                }
                            }
                            // If the list element is not annotated with @LazyLoad, load it now.
                            if (!field.isAnnotationPresent(LazyLoad.class)) {
                                listElement = load(listElement);
                            }
                            loadedList.add(listElement);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        field.set(ret, loadedList);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (field.isAnnotationPresent(LazyLoad.class)) {
                isLazyLoadPresent = true;
            }
        }
        if (isLazyLoadPresent) {
            return createProxy(ret);
        } else {
            return ret;
        }
    }

    // Dynamic proxy handler for lazy loading list fields.
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
                        if (idStr.trim().isEmpty())
                            continue;
                        try {
                            Class<?> listElementClass = Class.forName(
                                    field.getAnnotation(PersistableListField.class).className());
                            Object listElement = listElementClass.getDeclaredConstructor().newInstance();
                            for (Field f : getAllFields(listElement)) {
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

    // Closes the Jedis connection.
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
