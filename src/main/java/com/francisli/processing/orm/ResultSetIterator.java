package com.francisli.processing.orm;

public interface ResultSetIterator<T> {
  void next(T obj);
}
