# Expense Tracker Telegram Bot

Telegram-бот для учета личных расходов. Пользователь вводит сумму, при желании добавляет описание, выбирает категорию и дату траты в диалоге, а бот сохраняет расход в PostgreSQL.

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

- `/start` создает базовые категории для нового пользователя и показывает inline-меню.
- `/menu` сбрасывает текущий диалог и показывает inline-меню без повторной инициализации.
- Главное меню: `➕ Добавить расход`, `📋 Расходы`, `📂 Категории`, `📊 Статистика`.
- Добавление расхода:
  1. нажать `➕ Добавить расход`;
  2. отправить `500`, `500 такси` или `такси 500`;
  3. выбрать категорию через reply-клавиатуру;
  4. выбрать дату через reply-клавиатуру: сегодня, вчера или ручной ввод в формате `24.05.2026`;
  5. получить отдельную карточку сохраненного расхода с inline-кнопками `Изменить` и `Удалить`;
  6. удалить расход из карточки после inline-подтверждения.
- Во время активного диалога inline-кнопки из других сообщений не меняют состояние и отвечают коротким уведомлением.
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

План будущего UX и поведения бота лежит в `UX.md`.

## Локальный запуск с нуля

Нужны JDK 21, Docker и Telegram bot token от BotFather.

1. Поднять локальный PostgreSQL:

```bash
docker compose up -d postgres
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

## Docker-запуск приложения

Для запуска приложения в контейнере нужен реальный `BOT_TOKEN`. Локальные значения можно задать через shell или через `.env`; сам `.env` игнорируется git.

Минимальный пример `.env`:

```env
BOT_TOKEN=<bot_token>
POSTGRES_DB=expense_db
DB_USER=user
DB_PASSWORD=password
```

Запуск приложения вместе с PostgreSQL:

```bash
docker compose up --build
```

Если нужен только локальный PostgreSQL для запуска приложения через Gradle или для тестов:

```bash
docker compose up -d postgres
```

Health endpoint доступен на порту приложения:

```bash
curl http://localhost:8080/actuator/health
```

Ожидаемый успешный ответ:

```json
{"status":"UP"}
```

Для остановки контейнеров:

```bash
docker compose down
```

Если этот же Telegram-бот уже запущен в другом месте, long polling может конфликтовать. Для smoke-check нужен один активный экземпляр с этим токеном.

## Конфигурация запуска

Приложение читает настройки из переменных окружения:

| Переменная | Назначение | Локальное значение по умолчанию |
| --- | --- | --- |
| `BOT_TOKEN` | Telegram bot token; секрет, обязателен для запуска приложения | нет |
| `DB_URL` | JDBC URL PostgreSQL без логина/пароля для обычного запуска через Gradle | нет |
| `DB_USER` | Пользователь PostgreSQL | `user` |
| `DB_PASSWORD` | Пароль PostgreSQL; секрет вне локального окружения | `password` |
| `POSTGRES_DB` | Имя локальной БД в Docker Compose | `expense_db` |

Docker Compose задает app-контейнеру внутренний `DB_URL` вида `jdbc:postgresql://postgres:5432/<POSTGRES_DB>`. Значения `user/password` предназначены только для локального Docker Compose. Для серверного окружения с уже развернутой БД нужен отдельный deploy-конфиг без локального `postgres` service.

## Docker image

Опубликованный image живет в GHCR:

```text
ghcr.io/kmozze/expense-tracker-bot
```

Публикация выполняется вручную через GitHub Actions workflow `Publish Docker Image`. Workflow публикует два тега:

- `sha-<commit_sha>` - воспроизводимый тег конкретного commit;
- `manual` - последний вручную опубликованный image.

`latest` пока не используется: у проекта еще нет release/deploy-семантики для этого тега.

Запуск опубликованного image предполагает уже доступную PostgreSQL БД:

```bash
export COMMIT_SHA=replace_with_commit_sha

docker run --rm \
  -e BOT_TOKEN="$BOT_TOKEN" \
  -e DB_URL="$DB_URL" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -p 8080:8080 \
  ghcr.io/kmozze/expense-tracker-bot:sha-${COMMIT_SHA}
```

Реальный deploy на сервер, restart policy и отдельный compose для внешней БД будут описаны отдельным шагом.

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
docker compose up -d postgres
./gradlew test
```

Для быстрой проверки unit-тестов:

```bash
./gradlew test --tests "me.kmozze.expensetracker.unit.*"
```

Перед PR также стоит проверить, что приложение стартует:

```bash
docker compose up -d postgres
./gradlew jooqCodegen
./gradlew bootRun
```

`jooqCodegen` нужен при первом локальном запуске, после очистки `build/` и после изменений схемы. Для этой проверки должны быть выставлены `DB_URL`, `DB_USER`, `DB_PASSWORD` и `BOT_TOKEN`. Успешный smoke-check - приложение стартовало без ошибок Spring context / Liquibase / Telegram long polling.

Для проверки Docker runtime без запуска Telegram long polling можно собрать image:

```bash
docker compose build app
```

## Текущие ограничения

- Сессии пользователей хранятся in-memory и сбрасываются при рестарте приложения.
- Просмотр расходов, управление категориями, статистика и редактирование расходов пока не реализованы в UI.
- Inline-кнопка `Изменить` на карточке сохраненного расхода пока отвечает, что раздел в работе.
