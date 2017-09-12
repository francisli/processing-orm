package com.francisli.processing.orm;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import processing.core.PApplet;

public abstract class SimpleORM<T> {

  public SimpleORM(PApplet parent) {
    this(parent, "sketch.db");
  }

  public SimpleORM(PApplet parent, String filename) {
    
  }

  T newInstance() {
      ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
      Type type = superClass.getActualTypeArguments()[0];
      Class<T> classType;
      if (type instanceof Class) {
          classType = (Class<T>) type;
      } else {
          classType = (Class<T>) ((ParameterizedType) type).getRawType();
      }
      try {
          return classType.newInstance();
      } catch (Exception e) {
          // Oops, no default constructor
          throw new RuntimeException(e);
      }
  }
}
