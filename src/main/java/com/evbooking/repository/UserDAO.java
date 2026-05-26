package com.evbooking.repository;

import java.util.List;

import com.evbooking.model.User;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;

public class UserDAO {

    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.find(User.class, username);
        } finally {
            em.close();
        }
    }

    public void save(User user) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(User user) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public User findByGoogleId(String googleId) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            List<User> results = em.createQuery(
                "SELECT u FROM User u WHERE u.googleId = :gid", User.class)
                .setParameter("gid", googleId)
                .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    public boolean existsByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username",
                Long.class)
                .setParameter("username", username)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
}