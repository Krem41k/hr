package org.vgk.hr.position;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vgk.hr.db.entity.PdfTemplate;
import org.vgk.hr.emailtemplate.EmailTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Должность: пакет PDF-шаблонов и один шаблон письма.
 */
@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "position")
    private List<PdfTemplate> templates = new ArrayList<>();

    @OneToOne(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmailTemplate emailTemplate;

    public Position(String name, String code) {
        this.name = name;
        this.code = code;
        this.active = true;
    }

    public List<PdfTemplate> getActiveTemplates() {
        return templates.stream()
                .filter(template -> !template.isDeleted())
                .toList();
    }
}
