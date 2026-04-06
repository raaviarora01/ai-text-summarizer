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
- Google Gemini API Key ([Get one free](https://aistudio.google.com/app/apikey))

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

## 📡 API Endpoints

### Summarize Text
```bash
POST /api/summarizer/summarize

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

Powered by [Bucket4j](https://github.com/bucket4j/bucket4j), limits are applied per IP address:

| Endpoint | Limit |
|---|---|
| `POST /api/summarizer/summarize` | 10 requests / 60s |
| All other `/api/**` routes | 60 requests / 60s |

Requests exceeding the limit receive a `429 Too Many Requests` response. Limits are configurable in `application.properties`.

## ⚙️ Tech Stack

- Java 21, Spring Boot 4.0.5
- MySQL 8.0+
- Google Gemini 2.5 Flash API
- Caffeine Cache + Bucket4j
- Maven