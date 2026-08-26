package org.vgk.hr.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.vgk.hr.position.Position;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF-шаблон, сохранённый в базе данных вместе с описанием областей подстановки.
 */
@Entity
@Table(name = "pdf_templates")
@Getter
@Setter
@NoArgsConstructor
public class PdfTemplate {

    /** Технический идентификатор записи шаблона. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальный номер шаблона, который передаёт клиент при генерации PDF. */
    @Column(nullable = false, unique = true)
    private Integer templateNumber;

    /** Должность, к которой относится шаблон; null для legacy-шаблонов. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    /** Отображаемое имя шаблона в пакете документов. */
    private String name;

    /** Порядок шаблона внутри должности. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Признак мягкого удаления шаблона в пакете должности. */
    @Column(nullable = false)
    private boolean deleted;

    /** Исходное имя загруженного PDF-файла. */
    @Column(nullable = false)
    private String originalFileName;

    /** Бинарное содержимое PDF-шаблона. */
    @Column(nullable = false)
    @JdbcTypeCode(Types.LONGVARBINARY)
    private byte[] content;

    /** Области PDF, в которые подставляются ФИО, дата рождения и текущая дата. */
    @OneToMany(mappedBy = "template", cascade = jakarta.persistence.CascadeType.ALL)
    private List<TemplateField> fields = new ArrayList<>();

    /**
     * Помечает текущие активные поля как удалённые и добавляет новые настройки.
     * Старые записи в БД сохраняются.
     */
    public void replaceFields(List<TemplateField> fields) {
        this.fields.stream()
                .filter(field -> !field.isDeleted())
                .forEach(field -> field.setDeleted(true));
        fields.forEach(field -> {
            field.setTemplate(this);
            field.setDeleted(false);
            this.fields.add(field);
        });
    }

    /** Активные (не удалённые) области подстановки. */
    public List<TemplateField> getActiveFields() {
        return fields.stream()
                .filter(field -> !field.isDeleted())
                .toList();
    }
}
