package org.vgk.hr.db.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.vgk.hr.db.entity.PdfTemplate;

import java.util.Optional;

public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long> {

    @EntityGraph(attributePaths = "fields")
    Optional<PdfTemplate> findByTemplateNumber(Integer templateNumber);
}
