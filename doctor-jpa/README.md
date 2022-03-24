# doctor-jpa

Doctor plugin supporting injection of jpa EntityManager instances.

### Basics

First, create the META-INF/persistence.xml in your project, example using hibernate and hikari:

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
             http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd"
             version="2.2">

    <persistence-unit name="default">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <properties>
            <property name="jakarta.persistence.jdbc.driver" value="org.hsqldb.jdbc.JDBCDriver"/>
            <property name="jakarta.persistence.jdbc.user" value="SA"/>
            <property name="jakarta.persistence.jdbc.password" value=""/>
            <property name="hibernate.show_sql" value="false"/>
            <property name="hibernate.hikari.connectionTimeout" value="20000"/>
            <property name="hibernate.hikari.minimumIdle" value="1"/>
            <property name="hibernate.hikari.maximumPoolSize" value="10"/>
            <property name="hibernate.hikari.idleTimeout" value="30000"/>
        </properties>
    </persistence-unit>

</persistence>
```

Then add the persistence context annotation to any provided class:

```java
@Singleton
@Eager
// this annotation creates a provider for the JPA EntityManager with qualifier @Named("default")
// EntityManagers provided in this fashion will be singleton scoped
@PersistenceContext(name = "default", // < matches the name from persistence.xml
        properties = {
                // additional properties can be added to the persistence configuration
                @PersistenceProperty(name = "jakarta.persistence.jdbc.url", value = "${db.url}"),
                @PersistenceProperty(name = "hibernate.hbm2ddl.auto", value = "create")
        })
public class DAO {

    private final EntityManager entityManager;

    @Inject
    // the EntityManager is injectable as a parameter
    public DAO(@Named("default") EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
```