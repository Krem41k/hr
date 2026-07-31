# HR — генерация PDF-документов по шаблонам

Сервис для HR-процессов: хранит PDF-шаблоны (например, направления на медосмотр) и автоматически заполняет в них фиксированные поля — текущую дату, дату рождения и ФИО.

## Зачем нужен проект

В кадровых документах одни и те же бланки заполняются многократно: меняются ФИО сотрудника, дата рождения и дата выдачи, а расположение полей в PDF остаётся неизменным.

Проект позволяет:

1. Один раз загрузить PDF-шаблон и описать координаты областей подстановки.
2. При каждом запросе передавать только номер шаблона, ФИО и дату рождения.
3. Получить готовый PDF с подставленными значениями.

## Что делает приложение

- Сохраняет PDF-шаблон и метаданные полей в PostgreSQL.
- При обновлении шаблона старые настройки полей не удаляются, а помечаются как `deleted`.
- По номеру шаблона находит сохранённый файл.
- Подставляет:
  - **CURRENT_DATE** — текущую дату;
  - **BIRTH_DATE** — дату рождения из запроса;
  - **FULL_NAME** — ФИО из запроса.
- Поддерживает несколько вхождений одного типа поля на разных страницах.
- Рисует текст через Apache PDFBox (шрифт DejaVu Sans, опционально жирный).
- Отдаёт API-документацию через Swagger / OpenAPI.

## Структура проекта

```text
hr/
├── compose.yaml                          # PostgreSQL для локального запуска
├── pom.xml                               # Maven, зависимости, JaCoCo
└── src/
    ├── main/
    │   ├── java/org/vgk/hr/
    │   │   ├── HrApplication.java        # Точка входа Spring Boot
    │   │   ├── config/
    │   │   │   └── OpenApiConfig.java    # Настройка Swagger
    │   │   ├── controller/
    │   │   │   └── FileController.java   # REST API шаблонов и генерации
    │   │   ├── db/
    │   │   │   ├── entity/               # JPA-сущности
    │   │   │   │   ├── PdfTemplate.java
    │   │   │   │   ├── TemplateField.java
    │   │   │   │   └── TemplateFieldType.java
    │   │   │   └── repository/
    │   │   │       └── PdfTemplateRepository.java
    │   │   ├── domain/request/           # DTO запросов
    │   │   │   ├── TemplateUploadRequest.java
    │   │   │   ├── TemplateFieldRequest.java
    │   │   │   └── TextEditRequest.java
    │   │   └── service/
    │   │       ├── PdfTemplateService.java   # Сохранение/чтение шаблонов
    │   │       ├── PdfEditorService.java     # Правка PDF через PDFBox
    │   │       └── TemplateNotFoundException.java
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/             # Flyway-миграции
    │       └── fonts/                    # DejaVu Sans / Bold для кириллицы
    └── test/java/org/vgk/hr/             # Unit- и интеграционные тесты
```

### Слои

| Слой | Назначение |
|------|------------|
| `controller` | HTTP-эндпоинты, логирование запросов/ответов |
| `service` | Бизнес-логика шаблонов и редактирования PDF |
| `db.entity` / `db.repository` | Модель БД и доступ к данным |
| `domain.request` | Контракты API (metadata шаблона, параметры правок) |
| `config` | OpenAPI / Swagger |

## API

Базовый путь: `/api/v1/file`  
Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Загрузить или обновить шаблон

`POST /api/v1/file/templates` — `multipart/form-data`

| Часть | Описание |
|-------|----------|
| `file` | PDF-файл шаблона |
| `metadata` | JSON с номером шаблона и списком полей |

Типы полей в `metadata.fields`:

- `CURRENT_DATE` — текущая дата (`dateFormat`, например `dd.MM.yyyy`)
- `BIRTH_DATE` — дата рождения (`dateFormat`)
- `FULL_NAME` — ФИО
- `bold` — жирное начертание (`true` / `false`)

В шаблоне должны быть хотя бы по одному полю каждого обязательного типа: `CURRENT_DATE`, `BIRTH_DATE`, `FULL_NAME`.

### Сгенерировать PDF

`POST /api/v1/file/generate`

| Параметр | Описание |
|----------|----------|
| `templateNumber` | Номер сохранённого шаблона |
| `fullName` | ФИО сотрудника |
| `birthDate` | Дата рождения в формате `yyyy-MM-dd` |

Ответ: файл `application/pdf`.

## Технологии

- Java 21, Spring Boot 4.1
- Spring Data JPA, PostgreSQL, Flyway
- Apache PDFBox
- springdoc-openapi (Swagger)
- Testcontainers, JUnit 5, JaCoCo (порог покрытия ≥ 80%)

## Локальный запуск

1. Поднять PostgreSQL:

```bash
docker compose up -d
```

По умолчанию в `compose.yaml`: БД `hr`, пользователь `hr`, пароль `hr`, порт `5432`.

2. Проверить `spring.datasource.*` в `src/main/resources/application.yml`.

3. Запустить приложение:

```bash
./mvnw spring-boot:run
```

4. Прогнать тесты:

```bash
./mvnw clean verify
```

Для интеграционных тестов нужен запущенный Docker (Testcontainers).
