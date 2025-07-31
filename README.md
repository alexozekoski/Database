# Java UniversalJDBC

## Introduction

This library was developed to work genetically with a database of different types. It works with java model object and json object.

This library was built for [Java 8](https://openjdk.java.net/projects/jdk/16/) Any help to this library is welcomed as long as the coding style of the project is respected. 

#### Dependencies

##### Libs
Json   | Version 
--------- | ------
Gson | 2.8.6

##### JDBC
JDBC   | Version 
--------- | ------
PostgreSQL | 42.2.10
MySQL | 5.1.42
SQLite | 3.27.2.1
Firebird | 4.0.2

### How to use

#### Connecting to database

##### PostgreSQL or MySQL

Creating a simple connection and running as json.
```java

public static void main(String[] args) {

    PostgreSQL postgreSQL = new PostgreSQL("localhost", "5432", "user", "password", "mydatabse");
    
    if (postgreSQL.connect()) {
        
        System.out.println(postgreSQL.executeAsJsonArray("SELECT * FROM table"));

        postgreSQL.disconnect();
        
    } else {
        System.out.println("Not connected");
    }
}

```

Creating a simple connection catching connection errors.

```java
public static void main(String[] args) {

    MySQL mySQL = new MySQL("127.0.0.1", "3306", "user", "password", "mydatabase");

    try {
        mySQL.tryConnect();
        mySQL.tryExecuteAsJsonArray("SELECT * FROM table");
        mySQL.tryDisconnect();
    } catch (Exception e) {
        System.out.println("My connection error is " + e);
    }
}
```

Creating a simple connection catching errors.

```java
public static void main(String[] args) {

    PostgreSQL postgreSQL = new PostgreSQL("127.0.0.1", "5432", "user", "password", "mydatabase");

    try {
        postgreSQL.tryConnect();
        postgreSQL.tryExecuteAsJsonArray("SELECT * FROM table");
        postgreSQL.tryDisconnect();
    } catch (Exception e) {
        System.out.println("My connection error is " + e);
    }
}
```

##### SQLite or Firebird

Creating a simple connection and running as json.

Note: In SQLite if you try to connect to a database that doesn't exist it will create the file automatically
```java

public static void main(String[] args) {
    SQLite sqlite = new SQLite(null, null, null, null, "folder/mydbfile.db");
    //or SQLite sqlite = new SQLite("folder/mydbfile.db");
    if(sqlite.connect())
    {
        JsonObject json = sqlite.executeAsJsonObject("SELECT * FROM table");
        
        System.out.println(json.get("head"));
        System.out.println(json.get("body"));
        
        sqlite.disconnect();
    }
}

```

Creating a simple connection and testing if it was closed correctly.

```java
public static void main(String[] args) {
    FirebirdSQL firebirdSQL = new FirebirdSQL("folder/myfile.db");
    
    if(firebirdSQL.connect())
    {
        System.out.println(firebirdSQL.executeAsJsonArray("SELECT * FROM table"));
        
        if(firebirdSQL.disconnect())
        {
            System.out.println("Disconnected successfully");
        }
    }
}

```

#### Extra resorces

```java
public static void main(String[] args) {
    PostgreSQL database = new PostgreSQL("localhost", "5432", "user", "password", "mydatabse");

    if (database.connect()) {

        JsonArray tables = database.tables();

        JsonArray columns = database.columns("table");
        
        if(database.hasTable("table"))
        {
            System.out.println("OK");
        }

        //Set ready only
        database.setReadOnly(true);
        
        database.disconnect();

    } else {
        System.out.println("Not connected");
    }
}

```
Show internal errors
```java
import github.alexozekoski.database.Log;

public static void main(String[] args) {
    Log.show = true;
}

```

Generic instance
```java
public static void main(String[] args) {
    Database database = Database.connect(PostgreSQL.JDBC, "localhost", "5432", "postgres", "postgres", "teste");
    // or Database database = Database.connect("postgresql", "localhost", "5432", "postgres", "postgres", "teste");
    database.connect();
    
    System.out.println(database.tables());
    
    System.out.println(database.columns("venda"));
    
    database.disconnect();
}

```

Generic instance using a configuration json

```java
public static void main(String[] args) {
    JsonObject json = new JsonObject();
    json.addProperty("jdbc", MySQL.JDBC);
    json.addProperty("host", "127.0.0.1");
    json.addProperty("port", "3306");
    json.addProperty("user", "root");
    json.addProperty("password", "password");
    json.addProperty("database", "mydatabase");
    
    Database database = Database.connect(json);
    if(database.connect())
    {
        System.out.println(database.tables());
        database.disconnect();
    }
}

```

To not connect to a specific database, it works only on PostgreSQL and MySQL

```java
public static void main(String[] args) {
    MySQL mySQL = new MySQL("localhost", "3306", "root", "password", null);
    mySQL.connect();
}

```


