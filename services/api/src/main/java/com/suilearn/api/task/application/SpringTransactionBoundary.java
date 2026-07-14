package com.suilearn.api.task.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class SpringTransactionBoundary implements TransactionBoundary {
    private final TransactionTemplate transactions;

    SpringTransactionBoundary(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Work<T> work) {
        return transactions.execute(status -> work.run());
    }
}
