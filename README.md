# Expense Tracker Telegram Bot

Telegram-бот для учета личных расходов. Пользователь вводит сумму и описание, выбирает категорию inline-кнопкой, а бот сохраняет расход в PostgreSQL.

## Стек

- Kotlin 2.1.10, JVM 21
- Spring Boot 3.4.4
- TelegramBots long polling 9.5.0
- PostgreSQL
- jOOQ 3.19.x
- Liquibase
- Gradle Kotlin DSL
- JUnit 5, MockK, Testcontainers

## Что уже работает

- `/start` создает базовые категории для нового пользователя и показывает главное меню.
- Главное меню: `➕ Добавить расход`, `📋 Расходы`, `📂 Категории`, `📊 Статистика`.
- Добавление расхода:
  1. нажать `➕ Добавить расход`;
  2. отправить `500 такси` или `такси 500`;
  3. выбрать категорию;
  4. получить подтверждение сохранения.
- Парсер поддерживает сумму в начале или конце строки, десятичную точку и запятую.
- Кнопка отмены в inline-выборе категории отменяет добавление расхода.

Разделы `Расходы`, `Категории` и `Статистика` пока отвечают, что функциональность в работе.

## Структура

```text
src/main/kotlin/me/kmozze/expensetracker/
  adapter/       Telegram boundary and UI formatting/keyboards
  handler/       Dialogue routing and state handlers
  model/         Domain messages, states, actions, entities
  repository/    jOOQ repositories
  service/       Business services and input parser
  exception/     Business/system errors

src/main/resources/db/changelog/
  db.changelog-master.yaml
  changesets/

src/test/kotlin/me/kmozze/expensetracker/
  unit/
  integration/
```

Дополнительная карта проекта лежит в `docs/PROJECT_MAP.md`.
План будущего UX и поведения бота лежит в `UI.md`.

## Локальный запуск с нуля

Нужны JDK 21, Docker и Telegram bot token от BotFather.

1. Поднять PostgreSQL:

```bash
docker compose up -d
```

2. Экспортировать переменные окружения:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/expense_db
export DB_USER=user
export DB_PASSWORD=password
export BOT_TOKEN=<bot_token>
```

3. Сгенерировать jOOQ-код:

```bash
./gradlew jooqCodegen
```

Сейчас генерация настроена через jOOQ `DDLDatabase` и читает SQL из `src/main/resources/db/changelog/changesets/001-init-schema.sql`. Живая БД для самой генерации не нужна, но Postgres нужен для запуска приложения и интеграционных тестов.

4. Проверить компиляцию:

```bash
./gradlew compileKotlin
```

5. Запустить приложение:

```bash
./gradlew bootRun
```

Если этот же бот уже запущен в другом месте, long polling может конфликтовать. Для ручной проверки нужен один активный экземпляр.

## jOOQ

jOOQ-код генерируется в:

```text
build/generated-sources/jooq
```

Команда:

```bash
./gradlew jooqCodegen
```

Генерация настроена через `DDLDatabase` и читает SQL из `src/main/resources/db/changelog/changesets/001-init-schema.sql`. При изменении схемы обновляйте миграции и перегенерируйте jOOQ-классы.

## Проверки

```bash
./gradlew ktlintCheck
./gradlew compileKotlin
./gradlew test
```

`./gradlew test` запускает unit- и integration-тесты. Интеграционные тесты используют Testcontainers PostgreSQL, поэтому Docker должен быть запущен.

Перед полным прогоном тестов стоит поднять Docker/Postgres:

```bash
docker compose up -d
./gradlew test
```

Последний локальный прогон на 2026-05-19: `./gradlew test` прошел успешно при доступном Docker.

Для быстрой проверки unit-тестов:

```bash
./gradlew test --tests "me.kmozze.expensetracker.unit.*"
```

Перед PR также стоит проверить, что приложение стартует:

```bash
docker compose up -d
./gradlew jooqCodegen
./gradlew bootRun
```

`jooqCodegen` нужен при первом локальном запуске, после очистки `build/` и после изменений схемы. Для этой проверки должны быть выставлены `DB_URL`, `DB_USER`, `DB_PASSWORD` и `BOT_TOKEN`. Успешный smoke-check - приложение стартовало без ошибок Spring context / Liquibase / Telegram long polling. Если этот же Telegram bot token уже используется другим запущенным экземпляром, long polling может конфликтовать.

## Текущие ограничения

- Сессии пользователей хранятся in-memory и сбрасываются при рестарте приложения.
- Просмотр расходов, управление категориями, статистика, редактирование и удаление расходов пока не реализованы в UI.
