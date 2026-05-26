package com.evbooking.repository;

import java.util.List;

import com.evbooking.model.ChargingStation;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;

public class ChargingStationDAO {

    public List<ChargingStation> findAll() {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.createQuery(
                "SELECT s FROM ChargingStation s", 
                ChargingStation.class)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public ChargingStation findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            return em.find(ChargingStation.class, id);
        } finally {
            em.close();
        }
    }

    public void save(ChargingStation station) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(station);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(ChargingStation station) {
        EntityManager em = HibernateUtil.getEntityManagerFactory()
                                        .createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(station);
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
            ChargingStation station = em.find(ChargingStation.class, id);
            if (station != null) {
                em.remove(station);
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