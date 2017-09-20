package com.francisli.processing.orm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;

public class Results {
  ResultSet resultSet;
  Field[] fields;

  Results(ResultSet resultSet, Field[] fields) {
    this.resultSet = resultSet;
    this.fields = fields;
  }

  public boolean next(Object obj) {
    try {
      boolean result = resultSet.next();
      if (result) {
        for (Field field: fields) {
          setValue(resultSet, obj, field);
        }
      }
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void setValue(ResultSet resultSet, Object result, Field field) throws Exception {
    if ((field.getModifiers() & Modifier.TRANSIENT) > 0) {
      return;
    }
    Class type = field.getType();
    String name = field.getName();
    if (type == Integer.class || type == Integer.TYPE) {
      field.setInt(result, resultSet.getInt(name));
    } else if (type == Long.class || type == Long.TYPE) {
      field.setLong(result, resultSet.getLong(name));
    } else if (type == Float.class || type == Float.TYPE) {
      field.setFloat(result, resultSet.getFloat(name));
    } else if (type == Double.class || type == Double.TYPE) {
      field.setDouble(result, resultSet.getDouble(name));
    } else if (type == Boolean.class || type == Boolean.TYPE) {
      field.setBoolean(result, resultSet.getBoolean(name));
    } else if (type == String.class) {
      field.set(result, resultSet.getString(name));
    }
  }
}
