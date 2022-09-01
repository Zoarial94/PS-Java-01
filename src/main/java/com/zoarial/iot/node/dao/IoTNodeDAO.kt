package com.zoarial.iot.node.dao

import com.zoarial.PrintBaseClass
import com.zoarial.iot.dao.DAOHelper
import com.zoarial.iot.node.model.IoTNode
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.RollbackException
import jakarta.persistence.TypedQuery
import java.util.*

class IoTNodeDAO : PrintBaseClass("IoTNodeDAO") {
    private val emf: EntityManagerFactory? = DAOHelper.getEntityManagerFactory()

    val allNodes: List<IoTNode>
        get() {
            val em = emf!!.createEntityManager()
            val q: TypedQuery<IoTNode> = em.createNamedQuery("getAll", IoTNode::class.java)
            val resultList = q.resultList
            em.close()
            return resultList
        }

    fun getNodeByUUID(uuid: UUID): IoTNode? {
        val em = emf!!.createEntityManager()
        return em.find(IoTNode::class.java, uuid)
    }

    fun persist(node: IoTNode?) {
        val em = emf!!.createEntityManager()
        val transaction = em.transaction
        try {
            transaction.begin()
            em.persist(node)
            transaction.commit()
        } catch (ex: EntityExistsException) {
            println("Node already exists in the database.")
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

    fun containsNode(node: IoTNode): Boolean {
        return getNodeByUUID(node.uuid!!) != null
    }

    fun update(node: IoTNode) {
        val em = emf!!.createEntityManager()
        val transaction = em.transaction
        try {
            transaction.begin()
            em.merge(node)
            transaction.commit()
        } catch (ex: EntityExistsException) {
            println("Node already exists in the database.")
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

    fun updateTimestamp(uuid: UUID, timestamp: Long) {
        val em = emf!!.createEntityManager()
        val transaction = em.transaction
        val dbNode = getNodeByUUID(uuid)
        if (dbNode == null) {
            return
        } else if (dbNode.lastHeardFrom!! > timestamp) {
            return
        } else {
            dbNode.lastHeardFrom = timestamp
            try {
                transaction.begin()
                em.merge(dbNode)
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
        }
        if (em.isOpen) {
            em.close()
        }
    }
}