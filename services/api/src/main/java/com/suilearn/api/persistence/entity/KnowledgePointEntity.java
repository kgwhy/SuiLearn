package com.suilearn.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_points")
public class KnowledgePointEntity {
    @Id
    private String id;
    private String knowledgeBaseId;
    private String name;
    private String title;
    private String description;
    private String shortSummary;
    @Column(columnDefinition = "text")
    private String definition;
    @Column(columnDefinition = "text")
    private String principlesJson;
    @Column(columnDefinition = "text")
    private String applicationScenariosJson;
    @Column(columnDefinition = "text")
    private String pitfallsJson;
    private String reviewStatus;
    private String revisionId;
    private Boolean sourceOutdated;
    private Boolean legacy;
    private String sourceMaterialId;
    @Column(columnDefinition = "text")
    private String sourceRefsJson;

    protected KnowledgePointEntity() {
    }

    public KnowledgePointEntity(
        String id,
        String knowledgeBaseId,
        String name,
        String description,
        String sourceMaterialId,
        String sourceRefsJson
    ) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = name;
        this.description = description;
        this.sourceMaterialId = sourceMaterialId;
        this.sourceRefsJson = sourceRefsJson;
    }
    public KnowledgePointEntity(String id, String kb, String name, String description, String shortSummary, String definition,
        String principlesJson, String scenariosJson, String pitfallsJson, String reviewStatus, Boolean sourceOutdated, Boolean legacy,
        String sourceMaterialId, String sourceRefsJson) {
        this(id, kb, name, description, sourceMaterialId, sourceRefsJson);
        this.shortSummary=shortSummary; this.definition=definition; this.principlesJson=principlesJson;
        this.applicationScenariosJson=scenariosJson; this.pitfallsJson=pitfallsJson; this.reviewStatus=reviewStatus;
        this.sourceOutdated=sourceOutdated; this.legacy=legacy;
    }

    public String getId() {
        return id;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceMaterialId() {
        return sourceMaterialId;
    }

    public String getSourceRefsJson() {
        return sourceRefsJson;
    }
    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }
    public String getShortSummary(){return shortSummary;} public String getDefinition(){return definition;}
    public String getPrinciplesJson(){return principlesJson;} public String getApplicationScenariosJson(){return applicationScenariosJson;}
    public String getPitfallsJson(){return pitfallsJson;} public String getReviewStatus(){return reviewStatus;}
    public Boolean getSourceOutdated(){return sourceOutdated;} public Boolean getLegacy(){return legacy;}
}
