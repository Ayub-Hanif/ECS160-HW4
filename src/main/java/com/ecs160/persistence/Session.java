package com.ecs160.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.PersistenceAnnotations.Persistable;
import com.ecs160.persistence.PersistenceAnnotations.PersistableField;
import com.ecs160.persistence.PersistenceAnnotations.PersistableId;
import com.ecs160.persistence.PersistenceAnnotations.PersistableListField;

import redis.clients.jedis.Jedis;

public class Session {
    private List<Object> objBuffer = new ArrayList<Object>();
    private static int PORT_NUMBER = 6379;
    private Integer current_id = 1;

    public Session() {
        Jedis jedis = connectToJedis();
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

    // private static String getObjStringIdentifier(Object obj) { // get the
    // identifier for this object
    // Class<?> clazz = obj.getClass();
    // String className = clazz.getName();
    // String id = getObjectId(obj);
    // StringBuilder classWithId = new StringBuilder(className);
    // classWithId.append(":");
    // classWithId.append(id);
    // return classWithId.toString(); // e.g. Post:1000
    // }

    // private static String makeObjStringId(Object obj) {
    // Class<?> clazz = obj.getClass();
    // String className = clazz.getName();
    // StringBuilder classWithId = new StringBuilder(className);
    // classWithId.append(":");
    // classWithId.append(current_id++);
    // return classWithId.toString();
    // }

    // private static String makeObjIdString(String className, int id){
    // return className + ":" + id;
    // }

    /*
     * Gets the id string from a persisted object (i.e. an object that has been
     * persisted
     * with persistAll).
     */
    // IMPORTANT NOTE: assumes object id is already inside the
    // @PersistableId-annotated field
    private static String getPersistedObjIdString(Object partialObject) {
        Class<?> clazz = partialObject.getClass();
        String className = clazz.getName();
        Field[] fields = clazz.getDeclaredFields();
        String ret = "";
        for (Field field : fields) {
            if (field.isAnnotationPresent(PersistableId.class)) {
                field.setAccessible(true);
                try {
                    assert (field.get(partialObject) instanceof Integer); // the get call should return an integer
                    ret = field.get(partialObject).toString();
                    break; // we found the id field, no need to look through any other fields
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    public void persistAll() throws IllegalArgumentException, IllegalAccessException {
        Jedis jedis = connectToJedis();
        for (Object obj : objBuffer) {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            if (!clazz.isAnnotationPresent(Persistable.class)) {
                continue;
            }
            for (Field field : fields) {
                if (field.isAnnotationPresent(PersistableId.class)) {
                    field.setAccessible(true);
                    field.set(obj, current_id);
                    current_id++;
                }
            }
        }
        for (Object targetObj : objBuffer) {
            Class<?> clazz = targetObj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            if (!clazz.isAnnotationPresent(Persistable.class)) {
                continue;
            }

            // set the id equal to the current_id
            String objStringId = getObjectId(targetObj);
            // System.out.println("persisted with id = " + objStringId);
            // current_id++;

            // String id = getObjectId(targetObj);
            // assert (id != "");

            // save all persistable fields under the ID specified by the annotation
            // persistable field is just written as is,
            // persistable list field persists all the IDs in a comma-separated list
            for (Field field : fields) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    field.setAccessible(true);
                    System.out.println("persisted field name = " + field.getName() + " with value = "
                            + field.get(targetObj).toString() + " for id " + objStringId);
                    jedis.hset(objStringId, field.getName(), field.get(targetObj).toString()); // persist
                                                                                               // the
                                                                                               // field
                } else if (field.isAnnotationPresent(PersistableListField.class)) {
                    field.setAccessible(true);

                    StringBuilder persistedIdList = new StringBuilder(); // comma separated list of IDs
                    List<Object> itemList = (List) field.get(targetObj);
                    if (itemList.isEmpty()) {
                        persistedIdList.append(",");
                    } else {
                        for (Object item : itemList) {
                            System.out.println("saving list item with id = " + getObjectId(item));
                            persistedIdList.append(getObjectId(item)); // ID
                            persistedIdList.append(",");
                        }
                        persistedIdList.deleteCharAt(persistedIdList.length() - 1); // remove trailing comma
                    }

                    jedis.hset(objStringId, field.getName(), persistedIdList.toString());
                } else if (field.isAnnotationPresent(PersistableId.class)) {
                    field.setAccessible(true);
                    field.set(targetObj, Integer.parseInt(objStringId)); // set the ID of the object to the current ID
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

        // IMPORTANT NOTE: assumes object id is already inside the
        // @PersistableId-annotated field
        for (Field f : fields) {
            if (f.isAnnotationPresent(PersistableField.class)) {
                f.setAccessible(true);
                System.out.println("load field " + f.getName() + " with id = " + getPersistedObjIdString(object));
                Object fieldValue = jedis.hget(getPersistedObjIdString(object), f.getName());
                System.out.println("the field value was " + fieldValue.toString());
                try {
                    Class<?> type = f.getType();
                    if (type == String.class) {
                        f.set((Object) ret, fieldValue);
                    } else if (type == Integer.class) {
                        System.out.println("setting int field " + f.getName());
                        Integer newValue = Integer.parseInt(fieldValue.toString());
                        f.set((Object) ret, newValue);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (f.isAnnotationPresent(PersistableListField.class)) {
                f.setAccessible(true);
                String objectIds = jedis.hget(getPersistedObjIdString(object), f.getName());
                System.out.println("objectIds = " + objectIds);
                String[] ids = objectIds.split(",");
                List<Object> loadedObjects = new ArrayList<>();
                for (String id : ids) {
                    try {
                        System.out
                                .println("for class name = " + f.getAnnotation(PersistableListField.class).className());
                        Class<?> loadedObjClass = Class
                                .forName(f.getAnnotation(PersistableListField.class).className());
                        Constructor<?> loadedObj = loadedObjClass.getDeclaredConstructor();
                        Object obj = loadedObj.newInstance();
                        for (Field field : obj.getClass().getFields()) {
                            if (field.isAnnotationPresent(PersistableId.class)) {
                                field.setAccessible(true);
                                field.set(obj, Integer.parseInt(id)); // set the id of this object to the id seen in db
                            }
                        }

                        // load the object with load (recursively)
                        System.out.println("loading recusively with id = " + id);
                        obj = load(obj);
                        System.out.println("loaded!");

                        // populate loadedObjects array with result object
                        loadedObjects.add(obj);
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
                try {
                    f.set(ret, loadedObjects);
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
