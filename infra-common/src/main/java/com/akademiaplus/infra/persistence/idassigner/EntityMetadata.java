package com.akademiaplus.infra.persistence.idassigner;

import lombok.Getter;

import java.lang.reflect.Method;

/**
 * Immutable metadata holder for entity reflection data
 */
@Getter
public class EntityMetadata {
    private final String tableName;
    private final String idFieldName;
    private final Method getter;
    private final Method setter;
    private final boolean skip;

    private EntityMetadata(String tableName, String idFieldName, Method getter, Method setter) {
        this.tableName = tableName;
        this.idFieldName = idFieldName;
        this.getter = getter;
        this.setter = setter;
        this.skip = false;
    }

    private EntityMetadata(boolean skip) {
        this.tableName = null;
        this.idFieldName = null;
        this.getter = null;
        this.setter = null;
        this.skip = skip;
    }

    public static EntityMetadata of(String tableName, String idFieldName, Method getter, Method setter) {
        return new EntityMetadata(tableName, idFieldName, getter, setter);
    }

    public static EntityMetadata skip() {
        return new EntityMetadata(true);
    }
}