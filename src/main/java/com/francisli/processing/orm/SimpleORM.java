package com.francisli.processing.orm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import processing.core.PApplet;

interface PrimaryKeySetter {
  void setPrimaryKey(PreparedStatement stmt, int i) throws Exception;
}

public abstract class SimpleORM<T> {
  static HashMap<String, Connection> connections = new HashMap<String, Connection>();
  static HashMap<Class, Field[]> fields = new HashMap<Class, Field[]>();
  static HashMap<Class, PreparedStatement> inserts = new HashMap<Class, PreparedStatement>();
  static HashMap<Class, PreparedStatement> selects = new HashMap<Class, PreparedStatement>();
  Connection conn;

  public SimpleORM(PApplet parent) {
    this(parent, "sketch.db");
  }

  public SimpleORM(PApplet parent, String filename) {
    String connectionURL = "jdbc:sqlite:" + Paths.get(parent.sketchPath(), filename);
    conn = connections.get(connectionURL);
    if (conn == null) {
      try {
        conn = DriverManager.getConnection(connectionURL);
        connections.put(connectionURL, conn);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public T get(String id) {
    return get((stmt, i) -> stmt.setString(i, id));
  }

  public T get(int id) {
    return get((stmt, i) -> stmt.setInt(i, id));
  }

  public T get(Long id) {
    return get((stmt, i) -> stmt.setLong(i, id));
  }

  Field[] getFields(Class clazz) throws Exception {
    Field[] fields = SimpleORM.fields.get(clazz);
    if (fields == null) {
      fields = clazz.getFields();
      SimpleORM.fields.put(clazz, fields);

      //// create an appropriate table if needed
      StringBuffer buffer = new StringBuffer();
      buffer.append("CREATE TABLE IF NOT EXISTS ");
      buffer.append(clazz.getSimpleName());
      buffer.append(" (");
      boolean first = true;
      for (int i = 0, length = fields.length; i < length; i++) {
        Field field = fields[i];
        if ((field.getModifiers() & Modifier.TRANSIENT) > 0) {
          continue;
        }
        Class type = field.getType();
        String columnType;
        if (type == Integer.class || type == Long.class || type == Boolean.class ||
            type == Integer.TYPE || type == Long.TYPE || type == Boolean.TYPE) {
          columnType = " INTEGER";
        } else if (type == Float.class || type == Double.class) {
          columnType = " REAL";
        } else if (type == String.class) {
          columnType = " TEXT";
        } else {
          continue;
        }
        if (!first) {
          buffer.append(", ");
        }
        String name = field.getName();
        buffer.append(name);
        buffer.append(columnType);
        if (name.equals("id")) {
          buffer.append(" PRIMARY KEY");
        }
        first = false;
      }
      buffer.append(")");
      Statement createStmt = conn.createStatement();
      createStmt.execute(buffer.toString());
    }
    return fields;
  }

  T get(PrimaryKeySetter setter) {
    try {
      T result = newInstance();
      Class clazz = result.getClass();
      Field[] fields = getFields(clazz);
      Field primaryKeyField = null;
      for (Field field: fields) {
        if ((field.getModifiers() & Modifier.TRANSIENT) > 0) {
          continue;
        }
        String name = field.getName();
        if (name.equals("id")) {
          primaryKeyField = field;
          break;
        }
      }
      if (primaryKeyField == null) {
        throw new RuntimeException("Primary key field not found.");
      }
      PreparedStatement select = selects.get(clazz);
      if (select == null) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT * FROM ");
        buffer.append(clazz.getSimpleName());
        buffer.append(" WHERE ");
        buffer.append(primaryKeyField.getName());
        buffer.append("=?");
        select = conn.prepareStatement(buffer.toString());
        selects.put(clazz, select);
      }
      setter.setPrimaryKey(select, 1);
      ResultSet resultSet = select.executeQuery();
      Results results = new Results(resultSet, fields);
      if (results.next(result)) {
        return result;
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  boolean setParameter(PreparedStatement stmt, int i, Object obj, Field field) throws Exception {
    if ((field.getModifiers() & Modifier.TRANSIENT) > 0) {
      return false;
    }
    Class type = field.getType();
    if (type == Integer.class || type == Integer.TYPE) {
      stmt.setInt(i, field.getInt(obj));
    } else if (type == Long.class || type == Long.TYPE) {
      stmt.setLong(i, field.getLong(obj));
    } else if (type == Float.class || type == Float.TYPE) {
      stmt.setFloat(i, field.getFloat(obj));
    } else if (type == Double.class || type == Double.TYPE) {
      stmt.setDouble(i, field.getDouble(obj));
    } else if (type == Boolean.class || type == Boolean.TYPE) {
      stmt.setBoolean(i, field.getBoolean(obj));
    } else if (type == String.class) {
      stmt.setString(i, (String) field.get(obj));
    } else {
      return false;
    }
    return true;
  }

  public void put(T obj) {
    try {
      Class clazz = obj.getClass();
      Field[] fields = getFields(clazz);
      PreparedStatement insert = inserts.get(clazz);
      if (insert == null) {
        //// generate the insert statement
        StringBuffer buffer = new StringBuffer();
        buffer.append("INSERT OR REPLACE INTO ");
        buffer.append(clazz.getSimpleName());
        buffer.append(" (");
        int count = 0;
        for (int i = 0, length = fields.length; i < length; i++) {
          Field field = fields[i];
          if ((field.getModifiers() & Modifier.TRANSIENT) > 0) {
            continue;
          }
          Class type = field.getType();
          if (type == Integer.class || type == Long.class || type == Boolean.class ||
              type == Integer.TYPE || type == Long.TYPE || type == Boolean.TYPE ||
              type == Float.class || type == Double.class || type == String.class) {
            if (count > 0) {
              buffer.append(", ");
            }
            String name = field.getName();
            buffer.append(name);
            count += 1;
          }
        }
        buffer.append(" ) VALUES (");
        for (int i = 0; i < count; i++) {
          if (i > 0) {
            buffer.append(", ");
          }
          buffer.append("?");
        }
        buffer.append(")");
        insert = conn.prepareStatement(buffer.toString());
        inserts.put(clazz, insert);
      }
      int i = 1;
      for (Field field: fields) {
        if (setParameter(insert, i, obj, field)) {
          i += 1;
        }
      }
      insert.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public T[] fetch(String query) {
    ArrayList<T> list = new ArrayList<T>();
    Results results = query(query);
    T result = newInstance();
    while (results.next(result)) {
      list.add(result);
      result = newInstance();
    }
    T[] array = (T[]) Array.newInstance(getClassType(), list.size());
    return (T[])list.toArray(array);
  }

  public Results query(String query) {
    try {
      Class<T> clazz = getClassType();
      Field[] fields = getFields(clazz);
      StringBuffer buffer = new StringBuffer();
      buffer.append("SELECT * FROM ");
      buffer.append(clazz.getSimpleName());
      buffer.append(" ");
      buffer.append(query);
      Statement select = conn.createStatement();
      ResultSet resultSet = select.executeQuery(buffer.toString());
      return new Results(resultSet, fields);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  Class<T> getClassType() {
    ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
    Type type = superClass.getActualTypeArguments()[0];
    Class<T> classType;
    if (type instanceof Class) {
        classType = (Class<T>) type;
    } else {
        classType = (Class<T>) ((ParameterizedType) type).getRawType();
    }
    return classType;
  }

  T newInstance() {
      Class<T> classType = getClassType();
      try {
          return classType.newInstance();
      } catch (Exception e) {
          // Oops, no default constructor
          throw new RuntimeException(e);
      }
  }
}
