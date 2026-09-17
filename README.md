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

Базовый путь: `/api/v1/positions`  
Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Настройка должности (админка)

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` / `GET` | `/api/v1/positions` | создать должность / список должностей |
| `GET` / `PUT` / `DELETE` | `/api/v1/positions/{id}` | карточка, изменение, деактивация |
| `GET` / `POST` | `/api/v1/positions/{id}/templates` | список PDF-шаблонов / загрузка (`multipart`: `file` + `metadata`) |
| `PUT` / `DELETE` | `/api/v1/positions/{id}/templates/{templateId}` | замена файла и полей / мягкое удаление |
| `GET` / `PUT` | `/api/v1/positions/{id}/email-template` | шаблон письма с плейсхолдерами `{{fieldCode}}` |

Поле в `metadata.fields` описывается своим `fieldCode` (например `fullName`), `label`, `valueType` (`TEXT` / `DATE`), `valueSource` (`USER` / `SYSTEM`), координатами (`pageNumber`, `x`, `y`, `width`, `height`), оформлением (`fontSize`, `fontName`, `color`, `bold`) и `dateFormat` для дат. Один и тот же `fieldCode` в разных шаблонах должности обязан иметь одинаковые `label`, `valueType` и `valueSource` — иначе сохранение вернёт `400`.

### Форма оператора

`GET /api/v1/positions/{id}/form-schema` — уникальные `USER`-поля всех активных шаблонов должности: то, что должен заполнить оператор. `SYSTEM`-поля (например `currentDate`) в схему не попадают и подставляются сервером.

### Сгенерировать пакет документов

`POST /api/v1/positions/{id}/generate`

```json
{ "fields": { "fullName": "Иванов Иван Иванович", "birthDate": "1990-05-15" } }
```

Ответ — `application/zip` со всеми заполненными PDF (`01-Направление.pdf`, `02-Согласие.pdf`, …). Текст письма приходит в заголовках:

| Заголовок | Значение |
|-----------|----------|
| `X-Email-Subject` | тема письма, Base64 от UTF-8 |
| `X-Email-Body` | тело письма, Base64 от UTF-8 |
| `X-Email-Encoding` | всегда `base64` |

Base64 нужен потому, что HTTP-заголовки не переносят кириллицу как есть. В браузере: `new TextDecoder().decode(Uint8Array.from(atob(header), c => c.charCodeAt(0)))`.

Коды ошибок: `400` — не переданы или некорректны значения полей (`DATE` ожидает `yyyy-MM-dd`), `404` — должность не найдена, `409` — должность неактивна или без активных шаблонов.

### Legacy API

`POST /api/v1/file/templates` и `POST /api/v1/file/generate` работают на одиночном шаблоне без должности. Помечены `@Deprecated`, будут удалены — используйте `/api/v1/positions`.

## Технологии

- Java 21, Spring Boot 4.1
- Spring Data JPA, PostgreSQL, Flyway
- Apache PDFBox
- springdoc-openapi (Swagger)
- Testcontainers, JUnit 5, JaCoCo (порог покрытия ≥ 80%)

## CI

На каждый pull request и push в `main`/`master` GitHub Actions запускает `./mvnw clean verify` (включая Testcontainers и проверку покрытия JaCoCo ≥ 80%).

Чтобы запретить merge без зелёных тестов: **Settings → Branches → Branch protection rules** для `master` → включить **Require status checks to pass** и выбрать check `Maven verify`.


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
