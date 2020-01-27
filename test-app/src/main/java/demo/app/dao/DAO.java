package demo.app.dao;

import vest.doctor.Eager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;

@Singleton
@Eager
@PersistenceContext(name = "default",
        properties = {
                @PersistenceProperty(name = "demo", value = "nothingImportant")
        })
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