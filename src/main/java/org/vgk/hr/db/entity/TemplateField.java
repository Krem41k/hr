package org.vgk.hr.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Настройки отдельной области PDF-шаблона для подстановки значения.
 */
@Entity
@Table(name = "template_field")
@Getter
@Setter
@NoArgsConstructor
public class TemplateField {

    /** Технический идентификатор области подстановки. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PDF-шаблон, которому принадлежит область подстановки. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PdfTemplate template;

    /** Тип подставляемого значения: текущая дата или ФИО. */
    @Enumerated(EnumType.STRING)
    private TemplateFieldType type;
    /** Номер страницы PDF, начиная с 1. */
    private int pageNumber;
    /** Координата левого нижнего угла области по оси X. */
    private double x;
    /** Координата левого нижнего угла области по оси Y. */
    private double y;
    /** Ширина области, которую требуется очистить и заполнить. */
    private double width;
    /** Высота области, которую требуется очистить и заполнить. */
    private double height;
    /** Размер шрифта в пунктах. */
    private double fontSize;
    /** Имя шрифта, указанное в настройках шаблона. */
    private String fontName;
    /** Цвет текста в формате HEX. */
    private String color;
    /** Нужно ли использовать жирное начертание шрифта. */
    private boolean bold;
    /** Формат даты для CURRENT_DATE и BIRTH_DATE; не используется для FULL_NAME. */
    private String dateFormat;
    /** Признак мягкого удаления: старые настройки помечаются при обновлении шаблона. */
    private boolean deleted;

    public TemplateField(TemplateFieldType type, int pageNumber, double x, double y, double width, double height,
                         double fontSize, String fontName, String color, boolean bold, String dateFormat) {
        this.type = type;
        this.pageNumber = pageNumber;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fontSize = fontSize;
        this.fontName = fontName;
        this.color = color;
        this.bold = bold;
        this.dateFormat = dateFormat;
        this.deleted = false;
    }
}
