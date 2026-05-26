package com.evbooking.repository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import com.evbooking.model.User;
import com.evbooking.model.UserSession;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;

public class UserSessionDAO {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    public UserSession create(User user, int ttlSeconds) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            User managedUser = em.find(User.class, user.getUsername());
            UserSession session = new UserSession(
                newSessionId(),
                managedUser,
                LocalDateTime.now().plusSeconds(ttlSeconds)
            );
            em.persist(session);
            em.getTransaction().commit();
            return session;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public UserSession findValid(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            UserSession session = em.find(UserSession.class, sessionId);
            if (session == null) return null;
            if (session.isExpired()) {
                em.getTransaction().begin();
                em.remove(session);
                em.getTransaction().commit();
                return null;
            }
            session.getUser().getRole();
            return session;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public void delete(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            UserSession session = em.find(UserSession.class, sessionId);
            if (session != null) em.remove(session);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    private static String newSessionId() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
