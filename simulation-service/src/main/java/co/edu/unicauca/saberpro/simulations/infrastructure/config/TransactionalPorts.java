package co.edu.unicauca.saberpro.simulations.infrastructure.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Envuelve un puerto de entrada en una transacción (cada método = una transacción, rollback ante
 * RuntimeException) sin poner anotaciones de Spring en la capa de aplicación.
 */
public final class TransactionalPorts {

    private TransactionalPorts() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<T> port, T target, PlatformTransactionManager transactionManager) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(port);
        factory.addAdvice(new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
}
