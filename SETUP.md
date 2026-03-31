# AI Text Processor - Setup Guide

## Prerequisites
- Java 21 or higher
- Maven 3.8+
- MySQL 8.0+
- Google Cloud Project with Generative Language API enabled

## Environment Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ai-text-processor
```

### 2. Configure Environment Variables

#### Recommended: Using the Run Script (Windows/PowerShell)

1. Copy `run.ps1.example` to `run.ps1`:
   ```powershell
   Copy-Item run.ps1.example run.ps1
   ```

2. Edit `run.ps1` and fill in your credentials:
   ```powershell
   $env:DB_PASSWORD="your_password_here"
   $env:GEMINI_API_KEY="your_gemini_api_key_here"
   ```

3. Run the script from the project root:
   ```powershell
   .\run.ps1
   ```
   
   This will automatically:
   - Set all environment variables
   - Build the project
   - Start the application

#### Alternative: Manual Environment Variables (Any OS)

**PowerShell (Windows):**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/text_summarizer_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password_here"
$env:GEMINI_API_KEY="your_gemini_api_key_here"
mvn spring-boot:run
```

**Bash (Linux/Mac):**
```bash
export DB_URL="jdbc:mysql://localhost:3306/text_summarizer_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_password_here"
export GEMINI_API_KEY="your_gemini_api_key_here"
mvn spring-boot:run
```

### 3. Get Your Gemini API Key
1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Create a new API key
3. Or go to [Google Cloud Console](https://console.cloud.google.com), switch to your Gemini project, and create an API key in Credentials

### 4. Database Setup
Ensure MySQL is running. The application will automatically create the database schema on startup.

### 5. Build and Run

Copy and customize the run script for your OS:
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

#### Using IDE:
1. Import project as Maven project
2. Run `AiTextProcessorApplication.java` as a Java application

### 6. Verify Setup
The application will start on `http://localhost:8080`

Check the logs for:
```
Started AiTextProcessorApplication in X seconds
Tomcat started on port 8080
```

## API Endpoints

### Summarize Text (POST)
```bash
curl -X POST http://localhost:8080/api/summarizer/summarize \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Your text here...",
    "summaryType": "concise"
  }'
```

### Get All Summaries (GET)
```bash
curl http://localhost:8080/api/summarizer/history?page=0&size=10
```

### Get Summaries by Type (GET)
```bash
curl http://localhost:8080/api/summarizer/history/by-type?summaryType=concise&page=0&size=10
```

## Troubleshooting

### Database Connection Error
- Verify MySQL is running
- Check DB_URL, DB_USERNAME, DB_PASSWORD in environment variables
- Ensure the database user has proper permissions

### Gemini API 404 Error
- Verify GEMINI_API_KEY is correct and not expired
- Check that Generative Language API is enabled in Google Cloud Console
- Ensure the model `gemini-2.5-flash` is available in your project

### Port Already in Use
- Change the port in environment variables: `SERVER_PORT=8081`
- Or kill the process using port 8080

## Security Notes
- Never commit `.env` files to version control
- Keep API keys and passwords secure
- Use strong database passwords in production
- Enable SSL/TLS for database connections in production
- Use managed services (Cloud SQL, API Keys in GCP Secret Manager) in production
