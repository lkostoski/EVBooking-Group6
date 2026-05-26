package com.evbooking.repository;

import java.time.LocalDate;
import java.util.List;

import com.evbooking.model.AvailableSlot;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;

public class AvailableSlotDAO {

    public List<AvailableSlot> findByConnectorId(Long connectorId) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.createQuery(
                "SELECT s FROM AvailableSlot s WHERE s.connector.connectorId = :connectorId",
                AvailableSlot.class)
                .setParameter("connectorId", connectorId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public List<AvailableSlot> findByConnectorAndDate(Long connectorId, LocalDate date) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.createQuery(
                "SELECT s FROM AvailableSlot s WHERE s.connector.connectorId = :connectorId " +
                "AND s.date = :date",
                AvailableSlot.class)
                .setParameter("connectorId", connectorId)
                .setParameter("date", date)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(AvailableSlot slot) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(slot);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            AvailableSlot slot = em.find(AvailableSlot.class, id);
            if (slot != null) {
                em.remove(slot);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}