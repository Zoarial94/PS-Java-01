package com.zoarial.iot.dao;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.concurrent.atomic.AtomicReference;

public class DOAHelper {
    private final static AtomicReference<EntityManagerFactory> entityManagerFactory = new AtomicReference<>();

    private DOAHelper() {

    }

    public static EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory emf = entityManagerFactory.getOpaque();
        if(emf == null) {
            synchronized (entityManagerFactory) {
                if(entityManagerFactory.getPlain() == null) {
                    emf = Persistence.createEntityManagerFactory("ZIoT");
                    entityManagerFactory.setPlain(emf);
                }
            }
        }
        return emf;
    }
}
