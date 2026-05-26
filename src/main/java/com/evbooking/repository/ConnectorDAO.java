package com.evbooking.repository;

import java.util.List;

import com.evbooking.model.Connector;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;

public class ConnectorDAO {

    public List<Connector> findByStationId(Long stationId) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.createQuery(
                "SELECT c FROM Connector c WHERE c.chargingStation.stationId = :stationId",
                Connector.class)
                .setParameter("stationId", stationId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public Connector findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.find(Connector.class, id);
        } finally {
            em.close();
        }
    }

    public void save(Connector connector) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(connector);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Connector connector) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            Connector managed = em.find(Connector.class, connector.getConnectorId());
            if (managed != null) {
                managed.setConnectorType(connector.getConnectorType());
            }
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
            Connector connector = em.find(Connector.class, id);
            if (connector != null) {
                em.remove(connector);
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
