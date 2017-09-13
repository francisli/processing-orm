Simple ORM Library for Processing
===================================

This library can write simple Java objects into a local SQLite database for
your sketch. Given an instance of a Java class, the library will automatically
create a database and a table with columns for all public fields of the common
types- int, long, boolean, float, double, String. It expects at least one
field to be called "id" that will be a unique primary key.

Then, you can simply "get" and "put" instances into the database. If you know
some basic SQL, you can also run queries and get an array of results back.
