package com.ecs160.persistence;

import java.lang.reflect.Field;
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
    private List<Object> objBuffer = new ArrayList<>();
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

    private static String getPersistedObjIdString(Object partialObject) {
        String ret = "";
        for (Field field : getAllFields(partialObject)) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    ret = field.get(partialObject).toString();
                    break;
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

        if (jedis.exists(objStringId)) {
            return;
        }

        for (Field field : getAllFields(targetObj)) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(PersistableField.class)) {
                Object value = field.get(targetObj);
                String valueStr = (value != null) ? value.toString() : "";
                jedis.hset(objStringId, field.getName(), valueStr);

            } else if (field.isAnnotationPresent(PersistableListField.class)) {
                List<?> itemList = (List<?>) field.get(targetObj);
                StringBuilder persistedIdList = new StringBuilder();

                if (itemList != null && !itemList.isEmpty()) {
                    for (Object item : itemList) {
                        persistObject(item); // recursively persist child items
                        persistedIdList.append(getObjectId(item)).append(",");
                    }
                    // Remove trailing comma
                    persistedIdList.setLength(persistedIdList.length() - 1);
                }
                jedis.hset(objStringId, field.getName(), persistedIdList.toString());
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
            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                String calledMethodName = thisMethod.getName();
                if (!calledMethodName.startsWith("get")) {
                    return proceed.invoke(self, args);
                }
                String getterFieldName = calledMethodName.substring(3);
                getterFieldName = Character.toLowerCase(getterFieldName.charAt(0)) + getterFieldName.substring(1);

                Field field;
                try {
                    field = clazz.getDeclaredField(getterFieldName);
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    return proceed.invoke(self, args);
                }

                boolean isLazyGetter = field.isAnnotationPresent(LazyLoad.class);
                if (isLazyGetter) {
                    // for each item in the list, load it
                    List<Object> loadedList = new ArrayList<>();
                    for (Object item : (List<?>) field.get(self)) {
                        loadedList.add(load(item));
                    }
                    return loadedList;
                }
                return proceed.invoke(self, args);
            }
        };

        Class<?> proxyClass = proxyFactory.createClass();
        try {
            Object proxyObject = proxyClass.getDeclaredConstructor().newInstance();
            ((javassist.util.proxy.Proxy) proxyObject).setHandler(methodHandler);
            return proxyObject;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Object load(Object object) {
        String objId = getPersistedObjIdString(object);
        if (objId == null || objId.isEmpty()) {
            System.err.println("Invalid object id for loading.");
            return null;
        }

        Class<?> clazz = object.getClass();
        boolean isLazyLoadPresent = false;
        for (Field field : getAllFields(object)) {
            if (field.isAnnotationPresent(LazyLoad.class)) {
                isLazyLoadPresent = true;
                break;
            }
        }

        Object ret;
        try {
            if (isLazyLoadPresent) {
                ret = createProxy(object);
            } else {
                ret = clazz.getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        for (Field field : getAllFields(ret)) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(PersistableId.class)) {
                try {
                    if (field.getType() == Integer.class) {
                        field.set(ret, Integer.parseInt(objId));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            } else if (field.isAnnotationPresent(PersistableField.class)) {
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

                            // set the ID of the child
                            for (Field f : getAllFields(listElement)) {
                                if (f.isAnnotationPresent(PersistableId.class)) {
                                    f.setAccessible(true);
                                    f.set(listElement, Integer.parseInt(idStr));
                                    break;
                                }
                            }
                            // If field not lazy, load child right now
                            if (!field.isAnnotationPresent(LazyLoad.class)) {
                                listElement = load(listElement);
                            }
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
        return ret;
    }
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
