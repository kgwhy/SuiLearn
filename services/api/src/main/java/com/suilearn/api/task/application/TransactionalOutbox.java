package com.suilearn.api.task.application;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class TransactionalOutbox { private final List<OutboxEvent> events=new ArrayList<>(); public OutboxEvent submit(String taskId,String stage){var event=new OutboxEvent("outbox_"+UUID.randomUUID().toString().replace("-",""),taskId,stage,Instant.now());events.add(event);return event;} public List<OutboxEvent> pending(){return List.copyOf(events);} public record OutboxEvent(String id,String taskId,String stage,Instant createdAt){} }
