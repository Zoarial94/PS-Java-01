package com.zoarial.iot.dao;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.models.IoTNode;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

public class IoTNodeDAO extends PrintBaseClass {
    private final EntityManagerFactory emf;

    public IoTNodeDAO() {
        super("IoTNodeDAO");
        emf = DAOHelper.getEntityManagerFactory();
    }

    public List<IoTNode> getAllNodes() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<IoTNode> q;

        q = em.createNamedQuery("getAll", IoTNode.class);

        List<IoTNode> resultList = q.getResultList();

        em.close();

        return resultList;
    }

    public IoTNode getNodeByUUID(UUID uuid) {
        EntityManager em = emf.createEntityManager();
        return em.find(IoTNode.class, uuid);
    }

    public void persist(IoTNode node) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            em.persist(node);
            transaction.commit();
        } catch (EntityExistsException ex) {
            println("Node already exists in the database.");
        } catch (RollbackException ex) {
            ex.printStackTrace();
            println("Could not commit action to database.");
        }

        if(transaction.isActive()) {
            transaction.rollback();
        }
        if(em.isOpen()) {
            em.close();
        }
    }

    public boolean containsNode(IoTNode node) {
        return getNodeByUUID(node.getUuid()) != null;
    }

    public void update(IoTNode node) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            em.merge(node);
            transaction.commit();
        } catch (EntityExistsException ex) {
            println("Node already exists in the database.");
        } catch (RollbackException ex) {
            ex.printStackTrace();
            println("Could not commit action to database.");
        }

        if(transaction.isActive()) {
            transaction.rollback();
        }
        if(em.isOpen()) {
            em.close();
        }

    }

    public void updateTimestamp(UUID uuid, long timestamp) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        IoTNode dbNode = getNodeByUUID(uuid);
        if(dbNode == null) {
            return;
        } else if(dbNode.getLastHeardFrom() > timestamp) {
            return;
        } else {

            dbNode.setLastHeardFrom(timestamp);

            try {
                transaction.begin();
                em.merge(dbNode);
                transaction.commit();
            } catch (EntityExistsException ex) {
                println("Action already exists in the database.");
            } catch (RollbackException ex) {
                ex.printStackTrace();
                println("Could not commit action to database.");
            }
            if(transaction.isActive()) {
                transaction.rollback();
            }
        }

        if(em.isOpen()) {
            em.close();
        }
    }


}
