package org.vgk.hr.emailtemplate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vgk.hr.position.Position;

/**
 * Шаблон письма для должности. Плейсхолдеры: {{fieldCode}}.
 */
@Entity
@Table(name = "email_templates")
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false, unique = true)
    private Position position;

    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    public EmailTemplate(Position position, String subjectTemplate, String bodyTemplate) {
        this.position = position;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
    }
}
