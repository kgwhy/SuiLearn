package com.suilearn.api.task.application;

interface TransactionBoundary {
    <T> T execute(Work<T> work);

    @FunctionalInterface
    interface Work<T> { T run(); }
}
