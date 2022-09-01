package com.zoarial.iot.action.dao

import com.zoarial.PrintBaseClass
import com.zoarial.iot.action.model.IoTAction
import com.zoarial.iot.action.model.IoTActionList
import com.zoarial.iot.action.model.JavaIoTAction
import com.zoarial.iot.action.model.ScriptIoTAction
import com.zoarial.iot.dao.DAOHelper
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.RollbackException
import java.util.*

class IoTActionDAO : PrintBaseClass("IoTActionDAO") {
    private val emf: EntityManagerFactory = DAOHelper.getEntityManagerFactory()!!

    fun getActionByUUID(uuid: UUID): IoTAction {
        val em = emf.createEntityManager()
        val action = em.find(IoTAction::class.java, uuid)
        em.close()
        if (action == null) {
            throw EntityNotFoundException()
        }
        return action
    }

    fun findActionByUUID(uuid: UUID): Optional<IoTAction> {
        val em = emf.createEntityManager()
        val action = em.find(IoTAction::class.java, uuid)
        em.close()
        return Optional.ofNullable(action)
    }

    val enabledActions: IoTActionList
        get() {
            val em = emf.createEntityManager()
            val q = em.createNamedQuery("IoTAction.getEnabled", IoTAction::class.java)
            val actions = IoTActionList(q.resultList)
            em.close()
            return actions
        }
    val disabledActions: IoTActionList
        get() {
            val em = emf.createEntityManager()
            val q = em.createNamedQuery("IoTAction.getDisabled", IoTAction::class.java)
            val actions = IoTActionList(q.resultList)
            em.close()
            return actions
        }

    /**
     *
     * @param action An action with a UUID in the database.
     *
     * This function updates the security level, encrypted, and local
     * variables.
     */
    fun update(action: IoTAction) {
        val em = emf.createEntityManager()
        val transaction = em.transaction
        try {
            transaction.begin()
            em.merge(action)
            transaction.commit()
        } catch (ex: EntityExistsException) {
            println("Action already exists in the database.")
        } catch (ex: RollbackException) {
            ex.printStackTrace()
            println("Could not commit action to database.")
        }
        if (transaction.isActive) {
            transaction.rollback()
        }
        if (em.isOpen) {
            em.close()
        }
    }

    fun persist(action: IoTAction?) {
        val em = emf.createEntityManager()
        val transaction = em.transaction
        try {
            transaction.begin()
            em.persist(action)
            transaction.commit()
        } catch (ex: EntityExistsException) {
            println("Action already exists in the database.")
        } catch (ex: RollbackException) {
            ex.printStackTrace()
            println("Could not commit action to database.")
        }
        if (transaction.isActive) {
            transaction.rollback()
        }
        if (em.isOpen) {
            em.close()
        }
    }

    operator fun contains(node: IoTAction): Boolean {
        return findActionByUUID(node.uuid!!).isPresent
    }

    fun persistOrUpdate(action: IoTAction) {
        if (contains(action)) {
            update(action)
        } else {
            persist(action)
        }
    }

    // Criteria stuff
    val allActions: IoTActionList
        get() {
            val em = emf.createEntityManager()
            val transaction = em.transaction

            // Criteria stuff
            val builder = em.criteriaBuilder
            val query = builder.createQuery(IoTAction::class.java)
            val root = query.from(IoTAction::class.java)
            val select = query.select(root)

            // Actual query
            val typedQuery = em.createQuery(select)
            val resultList = typedQuery.resultList
            em.close()
            return IoTActionList(resultList)
        }

    fun getScriptActionByName(name: String): ScriptIoTAction? {
        val em = emf.createEntityManager()
        val transaction = em.transaction

        // Criteria stuff
        val builder = em.criteriaBuilder
        val query = builder.createQuery(ScriptIoTAction::class.java)
        val root = query.from(ScriptIoTAction::class.java)
        val nameParameter = builder.parameter(String::class.java)
        val select = query.select(root).where(builder.equal(root.get<Any>("name"), nameParameter))

        // Actual query
        val typedQuery = em.createQuery(select)
        typedQuery.setParameter(nameParameter, name)
        val resultList = typedQuery.resultList
        em.close()
        return if (resultList.size > 1) {
            println("Found multiple actions for one name... Something isn't right.")
            null
        } else if (resultList.size == 0) {
            null
        } else {
            resultList[0]
        }
    }

    fun getJavaActionByName(name: String): JavaIoTAction? {
        val em = emf.createEntityManager()
        val transaction = em.transaction

        // Criteria stuff
        val builder = em.criteriaBuilder
        val query = builder.createQuery(JavaIoTAction::class.java)
        val root = query.from(JavaIoTAction::class.java)
        val nameParameter = builder.parameter(String::class.java)
        val select = query.select(root).where(builder.equal(root.get<Any>("name"), nameParameter))

        // Actual query
        val typedQuery = em.createQuery(select)
        typedQuery.setParameter(nameParameter, name)
        val resultList = typedQuery.resultList
        em.close()
        return if (resultList.size > 1) {
            println("Found multiple actions for one name... Something isn't right.")
            null
        } else if (resultList.size == 0) {
            null
        } else {
            resultList[0]
        }
    }

    fun getActionByName(name: String): IoTAction? {
        val em = emf.createEntityManager()
        val transaction = em.transaction

        // Criteria stuff
        val builder = em.criteriaBuilder
        val query = builder.createQuery(IoTAction::class.java)
        val root = query.from(IoTAction::class.java)
        val nameParameter = builder.parameter(String::class.java)
        val select = query.select(root).where(builder.equal(root.get<Any>("actionName"), nameParameter))

        // Actual query
        val typedQuery = em.createQuery(select)
        typedQuery.setParameter(nameParameter, name)
        val resultList = typedQuery.resultList
        em.close()
        return if (resultList.size > 1) {
            println("Found multiple actions for one name... Something isn't right.")
            null
        } else {
            resultList[0]
        }
    }
}