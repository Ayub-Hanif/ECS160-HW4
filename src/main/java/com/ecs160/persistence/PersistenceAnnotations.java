package com.ecs160.persistence;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PersistenceAnnotations {
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
    // Extra credit part for our assignment#2: lazy loading annotation.
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LazyLoad { }
}

