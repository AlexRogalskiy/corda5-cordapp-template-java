package net.corda.c5template.service;

import net.corda.v5.application.services.lifecycle.ServiceLifecycleEvent;
import net.corda.v5.application.services.lifecycle.ServiceLifecycleObserver;
import net.corda.v5.application.services.lifecycle.StateMachineStarted;
import net.corda.v5.serialization.SingletonSerializeAsToken;

public interface CordaServiceLifecycle extends ServiceLifecycleObserver, SingletonSerializeAsToken {
}

class CordaServiceLifecycleImpl implements CordaServiceLifecycle {
    @Override
    public void onEvent(ServiceLifecycleEvent event){
        if (event instanceof StateMachineStarted) {
            System.out.println("**************CordaServiceLifecycle**************");
        }
    }
}