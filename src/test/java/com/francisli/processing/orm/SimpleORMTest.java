package com.francisli.processing.orm;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

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

    /* Tests all the standard operations
     */
    public void testAll()
    {
      PApplet stub = new PApplet();
      stub.sketchPath();
      //// delete the db file if it exists
      File dbFile = new File(stub.sketchPath(), "sketch.db");
      if (dbFile.exists()) {
        dbFile.delete();
      }
      assertFalse(dbFile.exists());

      //// instantiate ORM, which should create the db
      SimpleORM<Data> dataORM = new SimpleORM<Data>(stub) {};
      assert(dbFile.exists());

      //// now try to put in the first object, which should create the table and insert
      Data data = new Data();
      data.id = "1";
      data.name = "Foo bar";
      data.count = 123;
      data.enabled = true;
      dataORM.put(data);

      //// now to retreive the object by id
      data = dataORM.get("1");
      assertNotNull(data);
      assertEquals("1", data.id);
      assertEquals("Foo bar", data.name);
      assertEquals(123, data.count);
      assertEquals(true, data.enabled);

      //// now let's put a second object
      data = new Data();
      data.id = "2";
      data.name = "Baz";
      data.count = 456;
      data.enabled = false;
      dataORM.put(data);

      //// now let's query and iterate over them
      Data[] results = dataORM.fetch("ORDER BY id DESC");
      assertEquals(2, results.length);
      assertEquals("2", results[0].id);
      assertEquals("1", results[1].id);
    }
}
