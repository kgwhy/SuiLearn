package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.MaterialChunkEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialChunkJpaRepository extends JpaRepository<MaterialChunkEntity, String> {
    List<MaterialChunkEntity> findByMaterialId(String materialId);

    @Query(value = """
        select c.*
        from material_chunks c
        join learning_materials m on m.id = c.material_id
        where (:knowledgeBaseId is null or m.knowledge_base_id = :knowledgeBaseId)
          and (:materialId is null or c.material_id = :materialId)
          and m.status <> 'DELETED'
          and c.embedding_status in ('READY', 'TEXT_ONLY')
          and c.search_tsv @@ to_tsquery('simple', :query)
        order by ts_rank_cd(c.search_tsv, to_tsquery('simple', :query)) desc
        limit :limit
        """, nativeQuery = true)
    List<MaterialChunkEntity> searchText(
        @Param("query") String query,
        @Param("knowledgeBaseId") String knowledgeBaseId,
        @Param("materialId") String materialId,
        @Param("limit") int limit
    );

    @Query(value = """
        select c.*
        from material_chunks c
        join learning_materials m on m.id = c.material_id
        where (:knowledgeBaseId is null or m.knowledge_base_id = :knowledgeBaseId)
          and (:materialId is null or c.material_id = :materialId)
          and m.status <> 'DELETED'
          and c.embedding_status in ('READY', 'TEXT_ONLY')
        order by c.material_id, c.chunk_ordinal
        """, nativeQuery = true)
    List<MaterialChunkEntity> findByScope(
        @Param("knowledgeBaseId") String knowledgeBaseId,
        @Param("materialId") String materialId
    );

    void deleteByMaterialId(String materialId);

    void deleteByMaterialIdIn(Collection<String> materialIds);
}
