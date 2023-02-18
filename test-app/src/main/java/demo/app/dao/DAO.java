package demo.app.dao;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import vest.doctor.Eager;

@Singleton
@Eager
public class DAO {

    private final EntityManager entityManager;

    @Inject
    public DAO(@Named("default") EntityManager entityManager,
               @Named("alternate") EntityManager readOnly) {
        this.entityManager = entityManager;
        readOnly.close();
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