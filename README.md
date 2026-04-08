# AI Text Processor

A Spring Boot application that uses Google's Gemini AI to summarize text in multiple formats.

## ✨ Features

- Summarize text in 4 different formats: **concise**, **detailed**, **bullet_points**, **executive**
- View summary history with pagination
- Filter summaries by type
- Intelligent caching — duplicate requests served from memory, no redundant API calls
- Per-IP rate limiting to prevent abuse

## 🚀 Quick Start

### Prerequisites
- Java 21+
- MySQL 8.0+
- Google Gemini API Key (Get one free: https://aistudio.google.com/app/apikey)

### Setup

Create a `.env` file in the project root:

```
GEMINI_API_KEY=your_api_key_here
DB_URL=jdbc:mysql://localhost:3306/text_summarizer_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password_here
```

Then run:

```bash
mvn spring-boot:run
```

App runs on `http://localhost:8080`

## 📄 API Documentation (Swagger UI)

http://localhost:8080/swagger-ui/index.html

- Test APIs directly from the browser (no Postman required)
- View request/response formats and schemas
- Explore endpoints with detailed descriptions and error responses

## 📡 API Endpoints

### Summarize Text

```bash
POST /api/summarizer/summarize
```

```json
{
  "text": "Your text here...",
  "summaryType": "concise"
}
```

**Summary Types:** `concise`, `detailed`, `bullet_points`, `executive`

### Get Summary History

```bash
GET /api/summarizer/history?page=0&size=10
GET /api/summarizer/history/by-type?summaryType=concise&page=0&size=10
```

### Cache Statistics

```bash
GET /api/cache/stats
GET /api/cache/info
```

## 💾 Caching

Duplicate summarization requests are served from cache (90% faster). Cache keys include text hash and summary type.

- 1st request: Gemini API called (~1200ms)
- 2nd request (same text/type): Served from cache (<10ms)

## 🛡️ Rate Limiting

Powered by Bucket4j, limits are applied per IP address:

| Endpoint | Limit |
|---|---|
| POST /api/summarizer/summarize | 10 requests / 60s |
| All other /api/** routes | 60 requests / 60s |

Requests exceeding the limit receive a `429 Too Many Requests` response.

## ⚙️ Tech Stack

- Java 21, Spring Boot
- MySQL 8.0+
- Google Gemini API
- Caffeine Cache + Bucket4j
- Maven

---

## 🧪 Testing

This project includes a comprehensive testing strategy covering unit, controller, repository, and full integration layers.

### 🔁 Full Regression Suite

```bash
mvn test -Dtest=AiTextProcessorTestSuite
```

### ▶️ Test Execution Commands

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=SummaryTypeTest
mvn test -Dtest=CacheKeyGeneratorTest
mvn test -Dtest=ExceptionTest
mvn test -Dtest=TextSummarizerServiceTest
mvn test -Dtest=TextSummarizerControllerTest
mvn test -Dtest=CacheStatsControllerTest
mvn test -Dtest=TextSummaryRepositoryTest
mvn test -Dtest=FullIntegrationTest
mvn test -Dtest=RateLimitIntegrationTest

# Run only unit tests
mvn test -Dtest="SummaryTypeTest+CacheKeyGeneratorTest+ExceptionTest+TextSummarizerServiceTest"

# Run only integration tests
mvn test -Dtest="FullIntegrationTest+RateLimitIntegrationTest"
```

### 🏗️ Test Suite Breakdown

#### ⚡ Unit Tests (Fast — No Spring Context)

- SummaryTypeTest — enum parsing, type mapping, prompt generation
- CacheKeyGeneratorTest — SHA-256 hashing, key format, collision safety
- ExceptionTest — custom exception constructors and behavior
- TextSummarizerServiceTest — service logic with mocked dependencies

#### 🌐 Controller Slice Tests (@WebMvcTest)

- TextSummarizerControllerTest — API endpoints, validation, status codes
- CacheStatsControllerTest — cache stats and info endpoints

#### 🗄️ Repository Tests (@DataJpaTest)

- TextSummaryRepositoryTest — CRUD operations, filtering, pagination (H2 database)

#### 🔗 Integration Tests (Full Spring Context)

- FullIntegrationTest — end-to-end flow, caching behavior, actuator, Swagger
- RateLimitIntegrationTest — rate limiting, headers, 429 handling, filter isolation

### ✅ Highlights

- Clear separation between unit, slice, and integration tests
- Fast feedback loop with targeted test execution
- Full regression suite for end-to-end validation
- High test coverage across all layers