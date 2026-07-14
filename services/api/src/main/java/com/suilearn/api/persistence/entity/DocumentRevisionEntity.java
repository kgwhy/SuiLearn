package com.suilearn.api.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="document_revisions", uniqueConstraints=@UniqueConstraint(columnNames={"materialId","revisionNumber"}))
public class DocumentRevisionEntity { @Id private String id; private String materialId; private Integer revisionNumber; private String sourceChecksum; private String processingVersion; private Instant createdAt; protected DocumentRevisionEntity(){} public DocumentRevisionEntity(String id,String materialId,Integer revisionNumber,String sourceChecksum,String processingVersion,Instant createdAt){this.id=id;this.materialId=materialId;this.revisionNumber=revisionNumber;this.sourceChecksum=sourceChecksum;this.processingVersion=processingVersion;this.createdAt=createdAt;} }
