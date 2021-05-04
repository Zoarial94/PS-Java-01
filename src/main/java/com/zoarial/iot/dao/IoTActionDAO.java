package com.zoarial.iot.dao;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.models.actions.IoTAction;
import com.zoarial.iot.models.actions.IoTActionList;
import com.zoarial.iot.models.actions.JavaIoTAction;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.List;

public class IoTActionDAO extends PrintBaseClass {
    private final EntityManagerFactory emf;

    public IoTActionDAO() {
        super("IoTActionDAO");
        emf = DOAHelper.getEntityManagerFactory();
    }

    public synchronized void persist(IoTAction action) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            em.persist(action);
            transaction.commit();
        } catch (EntityExistsException ex) {
            println("Action already exists in the database.");
        } catch (RollbackException ex) {
            ex.printStackTrace();
            println("Could not commit action to database.");
        }

        if(em.isOpen()) {
            em.close();
        }
        if(transaction.isActive()) {
            transaction.rollback();
        }

    }

    public IoTActionList getAllActions() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IoTAction> query = builder.createQuery(IoTAction.class);
        Root<IoTAction> root = query.from(IoTAction.class);
        CriteriaQuery<IoTAction> select = query.select(root);

        // Actual query
        TypedQuery<IoTAction> typedQuery = em.createQuery(select);
        List<IoTAction> resultList = typedQuery.getResultList();
        return new IoTActionList(resultList);

    }

    public JavaIoTAction getJavaActionByName(String name) {

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<JavaIoTAction> query = builder.createQuery(JavaIoTAction.class);
        Root<JavaIoTAction> root = query.from(JavaIoTAction.class);
        ParameterExpression<String> nameParameter = builder.parameter(String.class);
        CriteriaQuery<JavaIoTAction> select = query.select(root).where(builder.equal(root.get("actionName"), nameParameter));

        // Actual query
        TypedQuery<JavaIoTAction> typedQuery = em.createQuery(select);
        typedQuery.setParameter(nameParameter, name);
        List<JavaIoTAction> resultList = typedQuery.getResultList();
        if(resultList.size() > 1) {
            println("Found multiple actions for one name... Something isn't right.");
            return null;
        } else {
            return resultList.get(0);
        }
    }

    public IoTAction getActionByName(String name) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<IoTAction> query = builder.createQuery(IoTAction.class);
        Root<IoTAction> root = query.from(IoTAction.class);
        ParameterExpression<String> nameParameter = builder.parameter(String.class);
        CriteriaQuery<IoTAction> select = query.select(root).where(builder.equal(root.get("actionName"), nameParameter));

        // Actual query
        TypedQuery<IoTAction> typedQuery = em.createQuery(select);
        typedQuery.setParameter(nameParameter, name);
        List<IoTAction> resultList = typedQuery.getResultList();
        if(resultList.size() > 1) {
            println("Found multiple actions for one name... Something isn't right.");
            return null;
        } else {
            return resultList.get(0);
        }
    }

}
