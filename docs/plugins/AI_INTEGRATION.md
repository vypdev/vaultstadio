# VaultStadio AI Integration

## Overview

VaultStadio includes a comprehensive AI integration layer that supports multiple AI providers for intelligent file analysis, tagging, and classification. The system supports both local models (for privacy-focused deployments) and cloud providers (for maximum capability).

## Supported Providers

### Local Providers

#### Ollama

[Ollama](https://ollama.ai/) runs AI models locally on your machine.

**Setup:**
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Download a vision model
ollama pull llava

# Ollama runs at http://localhost:11434
```

**Configuration:**
```json
{
  "type": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "model": "llava",
  "timeout": 120000
}
```

**Recommended Models:**
| Model | Description | Use Case |
|-------|-------------|----------|
| `llava` | Vision-capable LLaMA model | Image analysis |
| `llava:34b` | Larger LLaVA model | Better accuracy |
| `bakllava` | BakLLaVA vision model | Image classification |
| `llama3` | General LLaMA 3 | Text tasks |

---

#### LM Studio

[LM Studio](https://lmstudio.ai/) provides a GUI for running local models with an OpenAI-compatible API.

**Setup:**
1. Download and install LM Studio
2. Download a model (e.g., LLaVA, Mistral)
3. Start the local server (default: `http://localhost:1234`)

**Configuration:**
```json
{
  "type": "LM_STUDIO",
  "baseUrl": "http://localhost:1234/v1",
  "model": "loaded-model-name",
  "timeout": 120000
}
```

---

### Cloud Providers

#### OpenRouter

[OpenRouter](https://openrouter.ai/) provides unified access to multiple AI models.

**Supported Models:**
| Model ID | Provider | Vision | Description |
|----------|----------|--------|-------------|
| `anthropic/claude-3-opus` | Anthropic | Yes | Most capable |
| `anthropic/claude-3-sonnet` | Anthropic | Yes | Balanced |
| `anthropic/claude-3-haiku` | Anthropic | Yes | Fast & cheap |
| `openai/gpt-4o` | OpenAI | Yes | Latest GPT-4 |
| `google/gemini-1.5-pro` | Google | Yes | Long context |
| `meta-llama/llama-3.2-90b-vision-instruct` | Meta | Yes | Open source |

**Configuration:**
```json
{
  "type": "OPENROUTER",
  "baseUrl": "https://openrouter.ai/api/v1",
  "apiKey": "sk-or-v1-xxx",
  "model": "anthropic/claude-3-haiku",
  "timeout": 120000
}
```

**Getting an API Key:**
1. Sign up at [openrouter.ai](https://openrouter.ai/)
2. Go to Settings > API Keys
3. Create a new key

---

## API Endpoints

### Provider Management

#### List Providers

```http
GET /api/v1/ai/providers
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "type": "OLLAMA",
      "baseUrl": "http://localhost:11434",
      "model": "llava",
      "hasApiKey": false,
      "timeout": 120000,
      "enabled": true,
      "isActive": true
    }
  ]
}
```

#### Configure Provider

```http
POST /api/v1/ai/providers
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "type": "OPENROUTER",
  "baseUrl": "https://openrouter.ai/api/v1",
  "apiKey": "sk-or-v1-xxx",
  "model": "anthropic/claude-3-haiku",
  "timeout": 120000,
  "maxTokens": 1024,
  "temperature": 0.7,
  "enabled": true
}
```

#### Set Active Provider

```http
POST /api/v1/ai/providers/active
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "type": "OPENROUTER"
}
```

#### Check Provider Status

```http
GET /api/v1/ai/providers/OLLAMA/status
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": { "available": true }
}
```

---

### Model Listing

#### List Models from Active Provider

```http
GET /api/v1/ai/models
Authorization: Bearer <token>
```

#### List Models from Specific Provider

```http
GET /api/v1/ai/providers/OPENROUTER/models
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "anthropic/claude-3-haiku",
      "name": "Claude 3 Haiku",
      "provider": "openrouter",
      "supportsVision": true,
      "contextLength": 200000,
      "description": "Fast and efficient"
    }
  ]
}
```

---

### AI Operations

#### Chat Completion

```http
POST /api/v1/ai/chat
Authorization: Bearer <token>
Content-Type: application/json

{
  "messages": [
    { "role": "user", "content": "Hello!" }
  ],
  "model": "anthropic/claude-3-haiku",
  "maxTokens": 500,
  "temperature": 0.7
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": "Hello! How can I help you today?",
    "model": "anthropic/claude-3-haiku",
    "promptTokens": 10,
    "completionTokens": 8,
    "totalTokens": 18
  }
}
```

#### Vision (Image Analysis)

```http
POST /api/v1/ai/vision
Authorization: Bearer <token>
Content-Type: application/json

{
  "prompt": "Describe this image in detail",
  "imageBase64": "/9j/4AAQSkZJRg...",
  "mimeType": "image/jpeg"
}
```

#### Describe Image

```http
POST /api/v1/ai/describe
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "/9j/4AAQSkZJRg..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "description": "A sunset over the ocean with vibrant orange and purple colors..."
  }
}
```

#### Tag Image

```http
POST /api/v1/ai/tag
Authorization: Bearer <token>
Content-Type: application/json

{
  "imageBase64": "/9j/4AAQSkZJRg..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "tags": ["sunset", "ocean", "beach", "orange", "peaceful", "nature"]
  }
}
```

#### Classify Content

```http
POST /api/v1/ai/classify
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Annual financial report for Q4 2024...",
  "categories": ["financial", "technical", "marketing", "legal", "hr"]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "category": "financial"
  }
}
```

#### Summarize Text

```http
POST /api/v1/ai/summarize
Authorization: Bearer <token>
Content-Type: application/json

{
  "text": "Long document text here...",
  "maxLength": 200
}
```

---

## Architecture

### Provider Abstraction

```
┌─────────────────────────────────────────────────┐
│                   AIService                      │
│  - Provider management                           │
│  - High-level operations (describe, tag, etc.)  │
└─────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ OllamaProvider│ │ LMStudioProv. │ │OpenRouterProv.│
│               │ │               │ │               │
│ Local Model   │ │ Local Model   │ │ Cloud Models  │
│ http://11434  │ │ http://1234   │ │ Multiple APIs │
└───────────────┘ └───────────────┘ └───────────────┘
```

### AI Provider Interface

```kotlin
interface AIProvider {
    val type: AIProviderType
    val config: AIProviderConfig
    
    suspend fun isAvailable(): Boolean
    suspend fun listModels(): Either<AIError, List<AIModel>>
    suspend fun chat(request: AIRequest): Either<AIError, AIResponse>
    suspend fun vision(prompt: String, imageBase64: String, mimeType: String): Either<AIError, AIResponse>
}
```

---

## Plugin Integration

The AI Classification Plugin uses the AI Service to automatically analyze uploaded images:

```kotlin
// On file upload, the plugin:
1. Retrieves the file
2. Encodes as base64
3. Calls AI vision endpoint
4. Parses response for tags/labels
5. Saves metadata to the file
```

### Metadata Keys

| Key | Description |
|-----|-------------|
| `aiTags` | Comma-separated list of AI-generated tags |
| `classification` | Primary classification label |
| `confidence` | Confidence score (0-100) |
| `classifiedAt` | ISO timestamp of classification |

---

## Configuration Examples

### Privacy-First (Local Only)

```json
{
  "type": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "model": "llava",
  "enabled": true
}
```

### Best Quality (Cloud)

```json
{
  "type": "OPENROUTER",
  "baseUrl": "https://openrouter.ai/api/v1",
  "apiKey": "sk-or-v1-xxx",
  "model": "anthropic/claude-3-opus",
  "maxTokens": 2048,
  "temperature": 0.3
}
```

### Cost-Optimized (Cloud)

```json
{
  "type": "OPENROUTER",
  "baseUrl": "https://openrouter.ai/api/v1",
  "apiKey": "sk-or-v1-xxx",
  "model": "anthropic/claude-3-haiku",
  "maxTokens": 512
}
```

---

## Error Handling

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `ConnectionError` | 503 | Cannot connect to provider |
| `AuthenticationError` | 401 | Invalid API key |
| `RateLimitError` | 429 | Too many requests |
| `ModelNotFoundError` | 404 | Model not available |
| `ProviderError` | 400 | General provider error |

---

## Best Practices

1. **Use local models for sensitive data** - Ollama/LM Studio keep data on-premises
2. **Configure timeouts appropriately** - AI processing can be slow
3. **Set reasonable maxTokens** - Reduces costs and latency
4. **Use vision-capable models for images** - Check `supportsVision` flag
5. **Cache results** - Store AI metadata to avoid re-processing
6. **Handle errors gracefully** - AI is not always available

---

## Troubleshooting

### Ollama Not Available

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Restart Ollama
ollama serve
```

### OpenRouter Rate Limits

- Check your usage at openrouter.ai/dashboard
- Consider using a cheaper model
- Implement request queuing

### Slow Response Times

- Use a smaller model (e.g., `llama3` instead of `llama3:70b`)
- Reduce `maxTokens`
- For local: ensure GPU acceleration is enabled
