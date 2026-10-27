import json
import subprocess

data = {
    "messages": [
        {"role": "user", "content": "Explain what hemoglobin does in 2 sentences."}
    ],
    "temperature": 0.2,
    "max_tokens": 64,
    "stream": True
}

with open("test_stream.json", "w") as f:
    json.dump(data, f)

subprocess.run(["adb", "-s", "RZGL325K28P", "push", "test_stream.json", "/data/local/tmp/test_stream.json"])
res = subprocess.run(["adb", "-s", "RZGL325K28P", "shell", "curl -N -s -X POST http://127.0.0.1:8080/v1/chat/completions -H 'Content-Type: application/json' -d @/data/local/tmp/test_stream.json"], capture_output=True, text=True)
print("First 500 chars of stream:\n", res.stdout[:500])
