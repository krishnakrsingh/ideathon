import json
import subprocess

data = {
    "messages": [
        {"role": "system", "content": "You are SwasthAI, an on-device personal healthcare companion. Be concise."},
        {"role": "user", "content": "What is normal blood pressure? Answer in 1 sentence."},
        {"role": "assistant", "content": "Normal blood pressure is typically defined as less than 120/80 mmHg."},
        {"role": "user", "content": "And what is considered stage 1 hypertension? 1 sentence."}
    ],
    "temperature": 0.2,
    "max_tokens": 64
}

with open("test_req.json", "w") as f:
    json.dump(data, f)

subprocess.run(["adb", "-s", "RZGL325K28P", "push", "test_req.json", "/data/local/tmp/test_req.json"])
res = subprocess.run(["adb", "-s", "RZGL325K28P", "shell", "curl -s -X POST http://127.0.0.1:8080/v1/chat/completions -H 'Content-Type: application/json' -d @/data/local/tmp/test_req.json"], capture_output=True, text=True)
print("Response:", res.stdout)
