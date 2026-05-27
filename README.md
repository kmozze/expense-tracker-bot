# Expense Tracker Telegram Bot

Telegram-бот для учета личных расходов. Пользователь вводит сумму, при желании добавляет описание, выбирает категорию inline-кнопкой, выбирает дату траты, а бот сохраняет расход в PostgreSQL.

## Стек

- Kotlin 2.1.10, JVM 21
- Spring Boot 3.4.4
- TelegramBots long polling 9.5.0
- PostgreSQL
- jOOQ 3.19.x
- Liquibase
- Gradle Kotlin DSL
- JUnit 5, MockK, Database Rider, Testcontainers

## Что уже работает

- `/start` создает базовые категории для нового пользователя и показывает главное меню.
- Главное меню: `➕ Добавить расход`, `📋 Расходы`, `📂 Категории`, `📊 Статистика`.
- Добавление расхода:
  1. нажать `➕ Добавить расход`;
  2. отправить `500`, `500 такси` или `такси 500`;
  3. выбрать категорию;
  4. выбрать дату: сегодня, вчера или ручной ввод в формате `24.05.2026`;
  5. получить подтверждение сохранения с выбранной датой расхода и inline-кнопкой удаления.
- Inline-карточка расхода редактируется на шагах выбора категории, выбора даты, ручного ввода даты, финального подтверждения и подтверждения удаления, чтобы не создавать несколько сообщений одного сценария.
- Сохраненный расход можно удалить из карточки: бот добавляет под карточкой вопрос `Точно хотите удалить расход?` и inline-подтверждение, а после подтверждения удаляет запись и заменяет карточку текстом `Расход удален`.
- Парсер поддерживает ввод только суммы или суммы с описанием в начале/конце строки, десятичную точку и запятую.
- Кнопка отмены во время выбора категории или даты отменяет добавление расхода.

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
    flow/
    repository/
```

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

Сейчас генерация настроена через jOOQ `DDLDatabase` и читает SQL из `src/main/resources/db/changelog/changesets/*.sql`, кроме trigger changeset. Живая БД для самой генерации не нужна, но Postgres нужен для запуска приложения и интеграционных тестов.

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

Генерация настроена через `DDLDatabase` и читает SQL из `src/main/resources/db/changelog/changesets/*.sql`, кроме trigger changeset. При изменении схемы обновляйте миграции и перегенерируйте jOOQ-классы.

## Проверки

```bash
./gradlew ktlintCheck
./gradlew compileKotlin
./gradlew test
```

`./gradlew test` запускает unit- и integration-тесты. Интеграционные тесты используют Testcontainers PostgreSQL, а repository-тесты задают fixture'ы через Database Rider, поэтому Docker должен быть запущен.

Перед полным прогоном тестов стоит поднять Docker/Postgres:

```bash
docker compose up -d
./gradlew test
```

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

`jooqCodegen` нужен при первом локальном запуске, после очистки `build/` и после изменений схемы. Для этой проверки должны быть выставлены `DB_URL`, `DB_USER`, `DB_PASSWORD` и `BOT_TOKEN`. Успешный smoke-check - приложение стартовало без ошибок Spring context / Liquibase / Telegram long polling.

## Текущие ограничения

- Сессии пользователей хранятся in-memory и сбрасываются при рестарте приложения.
- Просмотр расходов, управление категориями, статистика и редактирование расходов пока не реализованы в UI; удаление доступно из карточки только что сохраненного расхода.
