package demo.app.dao;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.SynchronizationType;
import vest.doctor.Eager;

@Singleton
@Eager
@PersistenceContext(
        unitName = "default",
        properties = {
                @PersistenceProperty(name = "jakarta.persistence.jdbc.url", value = "${db.url:_missingurl_}"),
                @PersistenceProperty(name = "hibernate.hbm2ddl.auto", value = "create")
        },
        synchronization = SynchronizationType.UNSYNCHRONIZED)
public class DAO {

    private final EntityManager entityManager;

    @Inject
    public DAO(@Named("default") EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void store(User user) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.persist(user);
        } finally {
            transaction.commit();
        }
    }

    public User findUser(Long id) {
        return entityManager.createQuery("select u from User u where id = ?1", User.class)
                .setParameter(1, id)
                .getSingleResult();
    }
}