package org.vgk.hr.db.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.vgk.hr.db.entity.PdfTemplate;

import java.util.List;
import java.util.Optional;

public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long> {

    @EntityGraph(attributePaths = "fields")
    Optional<PdfTemplate> findByTemplateNumber(Integer templateNumber);

    @EntityGraph(attributePaths = "fields")
    Optional<PdfTemplate> findByIdAndPositionIdAndDeletedFalse(Long id, Long positionId);

    @EntityGraph(attributePaths = "fields")
    List<PdfTemplate> findByPositionIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long positionId);

    @Query("select coalesce(max(t.templateNumber), 0) from PdfTemplate t")
    Integer findMaxTemplateNumber();
}
