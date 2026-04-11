# 💰 Expense Tracker Telegram Bot

## 🛠 Технологический стек

* **Language:** Kotlin 2.1 (JVM 21)
* **Framework:** Spring Boot 3.4.4
* **Database:** PostgreSQL
* **Persistence:** jOOQ (Type-safe SQL)
* **Migrations:** Liquibase
* **Infrastructure:** Docker & Docker Compose
* **Build System:** Gradle (Kotlin DSL)

---

## 📋 Функциональные возможности
Пока реализованно только команда /start
(п.с. для проверяющего не делаю лишние миграции так как база пустая и не хочется плодить мелкие миграции в проекте на этом уровне потому просто перевыкатывала пустую базу)
---

## 🚀 Инструкция по развертыванию

### 1. Подготовка инфраструктуры
Запустите контейнер базы данных (убедитесь, что Docker запущен):
```bash
docker-compose up -d
```

### 2. Конфигурация

```yaml
bot:
  name: ${BOT_NAME}
  token: ${BOT_TOKEN}
```

Настройте параметры доступа к Telegram API (ваш токен и имя бота, полученные от @BotFather)
через переменные окружения или локальный конфиг

### 3. Генерация метамодели jOOQ

Для синхронизации программного кода со схемой БД выполните генерацию классов (убедитесь, что база данных в Docker запущена):

```bash
./gradlew jooqCodegen
```
Сгенерированные классы будут доступны в директории build/generated-sources/jooq.

### 4. Запуск приложения

Соберите и запустите проект через Gradle или IntelliJ IDEA:

```bash
./gradlew bootRun
```
