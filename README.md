# AI Text Processor

A Spring Boot application that uses Google's Gemini AI to summarize text in multiple formats.

## ✨ Features

- Summarize text in 4 different formats: **concise**, **detailed**, **bullet_points**, **executive**
- View summary history with pagination
- Filter summaries by type
- REST API

## 🚀 Quick Start

### Prerequisites
- Java 21+
- MySQL 8.0+
- Google Gemini API Key ([Get one free](https://aistudio.google.com/app/apikey))

### Setup

**For detailed setup instructions, see [SETUP.md](SETUP.md)**

Quick setup:
```powershell
Copy-Item run.ps1.example run.ps1
# Edit run.ps1 with your credentials
.\run.ps1
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

## ⚙️ Tech Stack

- Java 21, Spring Boot 4.0.5
- MySQL 8.0+
- Google Gemini 2.5 Flash API
- Maven