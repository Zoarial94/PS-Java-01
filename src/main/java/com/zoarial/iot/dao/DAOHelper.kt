package com.zoarial.iot.dao

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import java.util.concurrent.atomic.AtomicReference

object DAOHelper {
    private val entityManagerFactory = AtomicReference<EntityManagerFactory>()
    fun getEntityManagerFactory(): EntityManagerFactory {
        var emf = entityManagerFactory.opaque
        if (emf == null) {
            synchronized(entityManagerFactory) {
                if (entityManagerFactory.plain == null) {
                    emf = Persistence.createEntityManagerFactory("ZIoT")
                    entityManagerFactory.plain = emf
                }
            }
        }
        return emf
    }

    fun setEntityManagerFactory(name: String?) {
        synchronized(entityManagerFactory) { entityManagerFactory.setPlain(Persistence.createEntityManagerFactory(name)) }
    }
}