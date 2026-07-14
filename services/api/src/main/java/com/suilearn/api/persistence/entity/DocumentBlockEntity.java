package com.suilearn.api.persistence.entity;
import jakarta.persistence.*;
@Entity @Table(name="document_blocks", uniqueConstraints=@UniqueConstraint(columnNames={"revisionId","blockOrder"}))
public class DocumentBlockEntity { @Id private String id; private String revisionId; private Integer blockOrder; private Integer pageNumber; private String sectionPath; @Column(columnDefinition="text") private String content; protected DocumentBlockEntity(){} }
