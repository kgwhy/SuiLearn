package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "document_blocks", uniqueConstraints = @UniqueConstraint(columnNames = {"revisionId", "blockOrder"}))
public class DocumentBlockEntity {
    @Id private String id;
    private String revisionId;
    private Integer blockOrder;
    private Integer pageNumber;
    private String sectionPath;
    @Column(columnDefinition = "text") private String content;

    protected DocumentBlockEntity() { }

    public DocumentBlockEntity(String id, String revisionId, Integer blockOrder, Integer pageNumber, String sectionPath, String content) {
        this.id = id;
        this.revisionId = revisionId;
        this.blockOrder = blockOrder;
        this.pageNumber = pageNumber;
        this.sectionPath = sectionPath;
        this.content = content;
    }

    public String getId() { return id; }
    public String getRevisionId() { return revisionId; }
    public Integer getBlockOrder() { return blockOrder; }
    public Integer getPageNumber() { return pageNumber; }
    public String getSectionPath() { return sectionPath; }
    public String getContent() { return content; }
}
