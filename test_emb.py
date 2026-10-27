import json
import subprocess

data = {
    "input": "Fasting blood sugar 92 mg/dL",
    "model": "LFM2.5-Embedding-350M"
}

with open("test_emb.json", "w") as f:
    json.dump(data, f)

subprocess.run(["adb", "-s", "RZGL325K28P", "push", "test_emb.json", "/data/local/tmp/test_emb.json"])
res = subprocess.run(["adb", "-s", "RZGL325K28P", "shell", "curl -s -X POST http://127.0.0.1:8081/v1/embeddings -H 'Content-Type: application/json' -d @/data/local/tmp/test_emb.json"], capture_output=True, text=True)
data = json.loads(res.stdout)
emb = data['data'][0]['embedding']
print("Embedding dimension:", len(emb), "Sample values:", emb[:5])
