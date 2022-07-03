package com.zoarial.iot.action.dao;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.action.model.IoTAction;
import com.zoarial.iot.dao.DAOHelper;
import com.zoarial.iot.action.model.IoTActionList;
import com.zoarial.iot.action.model.JavaIoTAction;
import com.zoarial.iot.action.model.ScriptIoTAction;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class IoTActionDAO extends PrintBaseClass {
    private final EntityManagerFactory emf;

    public IoTActionDAO() {
        super("IoTActionDAO");
        emf = DAOHelper.getEntityManagerFactory();
    }

    public IoTAction getActionByUUID(UUID uuid) {
        EntityManager em = emf.createEntityManager();
        var action = em.find(IoTAction.class, uuid);
        em.close();
        if(action == null) {
            throw new EntityNotFoundException();
        }
        return action;
    }

    public Optional<IoTAction> findActionByUUID(UUID uuid) {
        EntityManager em = emf.createEntityManager();
        var action = em.find(IoTAction.class, uuid);
        em.close();
        return Optional.ofNullable(action);
    }

    public IoTActionList getEnabledActions() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<IoTAction> q = em.createNamedQuery("IoTAction.getEnabled", IoTAction.class);

        IoTActionList actions = new IoTActionList(q.getResultList());
        em.close();

        return actions;
    }

    public IoTActionList getDisabledActions() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<IoTAction> q = em.createNamedQuery("IoTAction.getDisabled", IoTAction.class);

        IoTActionList actions = new IoTActionList(q.getResultList());
        em.close();

        return actions;
    }

    /**
     *
     * @param action An action with a UUID in the database.
     *
     *               This function updates the security level, encrypted, and local
     *               variables.
     *
     */
    public void update(IoTAction action) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            em.merge(action);
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

    public void persist(IoTAction action) {
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

        if(transaction.isActive()) {
            transaction.rollback();
        }
        if(em.isOpen()) {
            em.close();
        }

    }

    public boolean contains(IoTAction node) {
        return findActionByUUID(node.getUuid()).isPresent();
    }

    public void persistOrUpdate(IoTAction action) {
        if(contains(action)) {
            update(action);
        } else {
            persist(action);
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

        em.close();

        return new IoTActionList(resultList);

    }


    public ScriptIoTAction getScriptActionByName(String name) {

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<ScriptIoTAction> query = builder.createQuery(ScriptIoTAction.class);
        Root<ScriptIoTAction> root = query.from(ScriptIoTAction.class);
        ParameterExpression<String> nameParameter = builder.parameter(String.class);
        CriteriaQuery<ScriptIoTAction> select = query.select(root).where(builder.equal(root.get("name"), nameParameter));

        // Actual query
        TypedQuery<ScriptIoTAction> typedQuery = em.createQuery(select);
        typedQuery.setParameter(nameParameter, name);
        List<ScriptIoTAction> resultList = typedQuery.getResultList();

        em.close();

        if(resultList.size() > 1) {
            println("Found multiple actions for one name... Something isn't right.");
            return null;
        } else if (resultList.size() == 0){
            return null;
        } else {
            return resultList.get(0);
        }
    }

    public JavaIoTAction getJavaActionByName(String name) {

        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        // Criteria stuff
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<JavaIoTAction> query = builder.createQuery(JavaIoTAction.class);
        Root<JavaIoTAction> root = query.from(JavaIoTAction.class);
        ParameterExpression<String> nameParameter = builder.parameter(String.class);
        CriteriaQuery<JavaIoTAction> select = query.select(root).where(builder.equal(root.get("name"), nameParameter));

        // Actual query
        TypedQuery<JavaIoTAction> typedQuery = em.createQuery(select);
        typedQuery.setParameter(nameParameter, name);
        List<JavaIoTAction> resultList = typedQuery.getResultList();

        em.close();

        if(resultList.size() > 1) {
            println("Found multiple actions for one name... Something isn't right.");
            return null;
        } else if (resultList.size() == 0){
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

        em.close();

        if(resultList.size() > 1) {
            println("Found multiple actions for one name... Something isn't right.");
            return null;
        } else {
            return resultList.get(0);
        }
    }

}
