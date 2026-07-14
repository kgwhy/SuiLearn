package com.suilearn.api.task.application;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class TransactionalOutboxTest { @Test void submitsTaskAndOutboxEventTogether() { var outbox=new TransactionalOutbox(); var event=outbox.submit("task_1","PARSING"); assertThat(event.taskId()).isEqualTo("task_1"); assertThat(outbox.pending()).containsExactly(event); } }
