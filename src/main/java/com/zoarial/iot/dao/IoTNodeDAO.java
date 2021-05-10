package com.zoarial.iot.dao;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.models.IoTNode;
import com.zoarial.iot.models.actions.IoTAction;
import com.zoarial.iot.models.actions.IoTActionList;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class IoTNodeDAO extends PrintBaseClass {
    private final EntityManagerFactory emf;

    public IoTNodeDAO() {
        super("IoTNodeDAO");
        emf = DAOHelper.getEntityManagerFactory();
    }

    public void persist(IoTNode node) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            em.persist(node);
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
        if(em.isOpen()) {
            em.close();
        }
    }

    public List<IoTNode> getAllNodes() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IoTNode> query = builder.createQuery(IoTNode.class);
        Root<IoTNode> root = query.from(IoTNode.class);
        CriteriaQuery<IoTNode> select = query.select(root);

        // Actual query
        TypedQuery<IoTNode> typedQuery = em.createQuery(select);
        List<IoTNode> resultList = typedQuery.getResultList();

        em.close();

        return resultList;
    }

}
