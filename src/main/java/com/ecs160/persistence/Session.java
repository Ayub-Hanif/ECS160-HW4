package com.ecs160.persistence;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;

public class Session {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Persistable { }
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PersistableField { }
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PersistableId { }
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PersistableListField { 
        String className();
    }
    private List<Object> objBuffer = new ArrayList<Object>();
    private static int PORT_NUMBER = 6379;

    public void add(Object obj) {
        objBuffer.add(obj);
    }

    private static Jedis connectToJedis() {
        try (Jedis jedis = new Jedis("localhost", PORT_NUMBER)) {
            // Optional: Authenticate if Redis requires a password
            // jedis.auth("password");

            // Test the connection
            System.out.println("Connection Successful");
            System.out.println("Ping: " + jedis.ping());
            return jedis;
        }
    }

    private static String getObjectId(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            if (f.isAnnotationPresent(PersistableId.class)) {
                f.setAccessible(true);
                try {
                    return f.get(obj).toString();
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        throw new RuntimeException("findIdField couldn't find PersistableId annotation on given object");
    }

    private static String getObjStringIdentifier(Object obj) {
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        String id = getObjectId(obj);
        StringBuilder classWithId = new StringBuilder(className);
        classWithId.append(":");
        classWithId.append(id);
        return classWithId.toString();
    }

    public void persistAll() throws IllegalArgumentException, IllegalAccessException {
        Jedis jedis = connectToJedis();
        for (Object targetObj : objBuffer) {
            Class<?> clazz = targetObj.getClass();
            String className = clazz.getName();
            Field[] fields = clazz.getDeclaredFields();
            if (!clazz.isAnnotationPresent(Persistable.class)) {
               continue; 
            }
            // get the id specified by the annotation
            String id = getObjectId(targetObj);
            assert(id != "");

            // save all persistable fields under the ID specified by the annotation 
                // persistable field is just written as is,
                // persistable list field persists all the IDs in a comma-separated list
            for (Field field : fields) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    field.setAccessible(true);
                    jedis.hset(getObjStringIdentifier(targetObj), field.getName(), field.get(targetObj).toString()); // persist the field
                } else if (field.isAnnotationPresent(PersistableListField.class)) {
                    field.setAccessible(true);

                    StringBuilder persistedIdList = new StringBuilder(); // comma separated list of IDs
                    List<Object> itemList = (List)field.get(targetObj);
                    if (itemList.isEmpty()) {
                        persistedIdList.append(",");    
                    } else {
                        for (Object item : itemList) {
                            persistedIdList.append(getObjectId(item)); // ID
                            persistedIdList.append(",");   
                        }
                    }
                    persistedIdList.deleteCharAt(persistedIdList.length()-1); // remove trailing comma

                    jedis.hset(getObjStringIdentifier(targetObj), field.getName(), persistedIdList.toString());
                }
            }
        }
        objBuffer.clear();
    }

    public Object load(Object object) {
        // get the id from the field of object with annotation
        Object ret = object;
        Jedis jedis = connectToJedis();

        // hit the database for all persistable object fields, for the id from above
        Class<?> clazz = ret.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field f : fields) {
            if (f.isAnnotationPresent(PersistableField.class)) {
                f.setAccessible(true);
                System.out.println("Persisting field value");
                Object fieldValue = jedis.hget(getObjStringIdentifier(object), f.getName());
                System.out.println(fieldValue);
                try {
                    Class<?> type = f.getType();
                    Integer testIntValue = 0;
                    if (type.isInstance(new String())) {
                        f.set((Object)ret, fieldValue);
                    } else if (type == int.class) {
                        f.setInt((Object)ret, Integer.parseInt(fieldValue.toString()));
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        // return packaged object up
        return ret;
    }
}
