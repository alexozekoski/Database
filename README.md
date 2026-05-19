# Java UniversalJDBC

## Documentation

Complete documentation page:
[GitHub Pages](https://alexozekoski.github.io/Database/)

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
import github.alexozk.database.Log;

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

### Model and Query

This library also works with `Model` classes. The fields must be `public` and annotated with `@Table` and `@Column`.

#### Creating a model

```java
import github.alexozk.database.model.Column;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.Table;

@Table("users")
public class User extends Model<User> {

    @Column(value = "id", primary = true, serial = true, insert = false, update = false)
    public Long id;

    @Column("name")
    public String name;

    @Column("email")
    public String email;
}
```

#### Using a model to query

```java
public static void main(String[] args) {
    PostgreSQL database = new PostgreSQL("localhost", "5432", "user", "password", "mydatabase");

    if (database.connect()) {
        User user = new User();
        user.setDatabase(database);

        ModelList<User> users = user.query()
                .where("name", "John")
                .orderByDesc("id")
                .limit(10)
                .get();

        for (User item : users) {
            System.out.println(item.toJsonString(true));
        }

        database.disconnect();
    }
}
```

#### Getting the first record

```java
public static void main(String[] args) {
    PostgreSQL database = new PostgreSQL("localhost", "5432", "user", "password", "mydatabase");

    if (database.connect()) {
        User user = new User();
        user.setDatabase(database);

        User first = user.query()
                .where("email", "john@example.com")
                .first();

        if (first != null) {
            System.out.println(first.toJsonString(true));
        }

        database.disconnect();
    }
}
```

#### Query using JsonObject

```java
public static void main(String[] args) {
    PostgreSQL database = new PostgreSQL("localhost", "5432", "user", "password", "mydatabase");

    if (database.connect()) {
        JsonObject where = new JsonObject();
        where.addProperty("name", "John");
        where.addProperty("email", "john@example.com");

        ModelList<User> users = database.query(User.class)
                .where(where)
                .limit(20)
                .get();

        System.out.println(users);
        database.disconnect();
    }
}
```

#### Useful query methods

```java
public static void main(String[] args) {
    PostgreSQL database = new PostgreSQL("localhost", "5432", "user", "password", "mydatabase");

    if (database.connect()) {
        QueryModel<User> query = database.query(User.class);

        query.select("id", "name");
        query.where("name", "like", "%john%");
        query.orWhere("email", "john@example.com");
        query.orderByAsc("name");
        query.limit(0, 25);

        System.out.println(query.getAsJsonArray());
        System.out.println(query.count());

        database.disconnect();
    }
}
```

### Global listeners

You can register global listeners for a model class and receive lifecycle callbacks for every instance of that class.

```java
import com.google.gson.JsonObject;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.ModelListenerAdapter;

Model.addListener(User.class, new ModelListenerAdapter<User>() {

    @Override
    public void afterInsert(User model) {
        System.out.println("Inserted: " + model.id);
    }

    @Override
    public void afterUpdate(User model) {
        System.out.println("Updated: " + model.id);
    }

    @Override
    public void getErrors(User model, JsonObject errors, String type) {
        System.out.println(errors);
    }
});
```

### Validation

The library validates columns automatically using the annotations declared on the model. You can also add custom rules with `onValidateColumn`.

```java
import github.alexozk.database.model.Column;
import github.alexozk.database.validation.Validator;

@Override
public void onValidateColumn(Column column, Object value, Validator validator) {
    if ("email".equals(column.value()) && value != null && !value.toString().contains("@")) {
        validator.addInvalid(1, "email is invalid");
    }
}
```

```java
User user = new User();
user.name = "John";
user.email = "john.example.com";

JsonObject errors = user.validate();
if (errors != null) {
    System.out.println(user.validateToString());
}
```

### Transactions

Use `executeTransaction` to group multiple operations in a single unit of work. The library creates savepoints and commits only when the block finishes successfully.

```java
try {
    database.executeTransaction(db -> {
        User user = new User();
        user.setDatabase(db);
        user.name = "John";
        user.email = "john@example.com";
        user.insert();

        db.query().table("audit_log")
                .set("message", "user created")
                .update();
    }, ex -> System.out.println(ex.getMessage()));
} catch (Exception ex) {
    ex.printStackTrace();
}
```

The same API is available from `Model`:

```java
user.executeTransaction(db -> {
    user.setDatabase(db);
    user.save();
});
```

### Safe thread access

If the same database instance is shared between threads, wrap it with `SynchronizedDatabase`.

```java
Database database = new SynchronizedDatabase(
        new PostgreSQL("localhost", "5432", "user", "password", "mydatabase"));

database.connect();
database.query(User.class)
        .where("active", true)
        .get();
```

### Connection pools

The project includes two pool strategies.

`PoolDatabase` keeps a fixed number of connections. The pool is created up front and `getNextDatabase()` returns
the next synchronized database in round-robin order.

`PoolDynamicDatabase` grows and shrinks the pool between a minimum and maximum size based on demand.
Use `getAndUseNextDatabase()` to borrow a connection and `releaseDatabase()` when you are done.

```java
JsonObject config = new JsonObject();
config.addProperty("jdbc", MySQL.JDBC);
config.addProperty("host", "127.0.0.1");
config.addProperty("port", "3306");
config.addProperty("user", "root");
config.addProperty("password", "password");
config.addProperty("database", "mydatabase");

PoolDatabase pool = new PoolDatabase("app-pool", 5, config);
SynchronizedDatabase db = pool.getNextDatabase();

PoolDynamicDatabase dynamicPool = new PoolDynamicDatabase("app-dynamic", 2, 10, config);
SynchronizedDatabase dynamicDb = dynamicPool.getAndUseNextDatabase();
dynamicPool.releaseDatabase(dynamicDb);
```

### Migrations and refresh

The migration API can create the table, add new columns, and drop removed columns.

```java
if (database.connect()) {
    JsonArray tables = database.tables();
    JsonArray columns = database.columns("users");

    System.out.println(tables);
    System.out.println(columns);

    Migration.migrate(database, User.class);
}
```

To inspect the current schema update result, use `Table.update(true, true, true)`:

```java
Table table = database.migrate(User.class);
char result = table.update(true, true, true);

if (result == 'A') {
    System.out.println("Schema changed");
}
```

To reload the current record from the database, use `refresh()`:

```java
user.refresh();
user.refresh("name", "email");
```

For a full rebuild, use `Migration.fresh(database, User.class);`
