const token = process.env.GITHUB_MODELS_TOKEN || process.env.GITHUB_TOKEN;
const apiUrl =
  (process.env.GITHUB_MODELS_API_URL || "https://models.github.ai/inference").replace(/\/$/, "");

if (!token) {
  throw new Error("Set GITHUB_MODELS_TOKEN first");
}

const response = await fetch(`${apiUrl}/chat/completions`, {
  method: "POST",
  headers: {
    "Accept": "application/vnd.github+json",
    "Authorization": `Bearer ${token}`,
    "Content-Type": "application/json",
    "X-GitHub-Api-Version": "2022-11-28"
  },
  body: JSON.stringify({
    model: "openai/gpt-4.1-mini",
    messages: [
      { role: "system", content: "You are a helpful assistant." },
      { role: "user", content: "Reply with: GitHub Models test successful." }
    ],
    max_tokens: 200,
    temperature: 0
  })
});

if (!response.ok) {
  const text = await response.text();
  throw new Error(`HTTP ${response.status}: ${text}`);
}

const data = await response.json();
console.log("Response:\n", data.choices?.[0]?.message?.content);
console.log("\nUsage:\n", data.usage);



curl -X POST "https://models.github.ai/inference/chat/completions" \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_MODELS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  -d '{
    "model": "openai/gpt-4.1-mini",
    "messages": [
      { "role": "system", "content": "You are a helpful assistant." },
      { "role": "user", "content": "Reply with: GitHub Models test successful." }
    ],
    "max_tokens": 200,
    "temperature": 0
  }'
