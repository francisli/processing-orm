package com.francisli.processing.orm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import processing.core.PApplet;

/**
 * Unit test for simple App.
 */
public class SimpleORMTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SimpleORMTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SimpleORMTest.class );
    }

    static class Data {
      public String id;
      public String name;
      public int count;
      public boolean enabled;
    }

    /* Tests the creation of the sqlite db file on first access.
     */
    public void testCreate()
    {
      PApplet stub = new PApplet();
      stub.sketchPath();

      SimpleORM<Data> dataORM = new SimpleORM<Data>(stub) {};
    }
}
