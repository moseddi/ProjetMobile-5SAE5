import pandas as pd
import requests
import json
import chromadb
import time

CSV_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/data/Restaurant reviews.csv"
CHROMA_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/rag/chroma_db"
OLLAMA_GEN_URL = "http://localhost:11434/api/generate"
OLLAMA_EMB_URL = "http://localhost:11434/api/embeddings"

print("📖 Chargement des données...")
df = pd.read_csv(CSV_PATH, encoding_errors="ignore")
# Prendre 1 avis par restaurant pour avoir 15 restaurants différents !
df = df.dropna(subset=["Review", "Restaurant"]).drop_duplicates(subset=["Restaurant"]).head(5)

client = chromadb.PersistentClient(path=CHROMA_PATH)
try:
    client.delete_collection(name="restaurant_reviews")
    print("🧹 Ancienne base nettoyée.")
except Exception:
    pass
collection = client.create_collection(name="restaurant_reviews")

print(f"🚀 Analyse de {len(df)} restaurants DIFFÉRENTS avec Mistral...\n")

for idx, row in df.iterrows():
    resto = str(row["Restaurant"]).strip()
    text = str(row["Review"]).strip()
    rating = str(row["Rating"]).strip()

    prompt = f"""Read this restaurant review and evaluate each aspect with a score from 1 to 10 (1=terrible, 5=average, 10=exceptional).
Review: "{text}"

Rate these 4 items:
FOOD: [score 1-10]
SERVICE: [score 1-10]
PRICE: [score 1-10]
AMBIANCE: [score 1-10]"""

    try:
        # 1. Extraction des scores via Mistral
        r = requests.post(OLLAMA_GEN_URL, json={
            "model": "mistral",
            "prompt": prompt,
            "stream": False
        }, timeout=180)
        
        raw_res = r.json().get("response", "")
        print(f"\n--- Réponse brute Mistral pour {resto} ---\n{raw_res.strip()}\n--------------------------------------")
        
        import re
        def parse_score(pattern, text_resp, default=7):
            m = re.search(pattern, text_resp, re.IGNORECASE)
            if m:
                val = int(m.group(1))
                return max(1, min(10, val))
            return default

        food_score = parse_score(r"FOOD[:\s\-]+(\d+)", raw_res, 8)
        service_score = parse_score(r"SERVICE[:\s\-]+(\d+)", raw_res, 7)
        price_score = parse_score(r"PRICE[:\s\-]+(\d+)", raw_res, 6)
        ambiance_score = parse_score(r"AMBI[EA]NCE[:\s\-]+(\d+)", raw_res, 8)

        # 2. Vectorisation (Embedding) via nomic-embed-text
        emb_r = requests.post(OLLAMA_EMB_URL, json={
            "model": "nomic-embed-text",
            "prompt": text
        }, timeout=180)
        emb = emb_r.json().get("embedding")

        # 3. Sauvegarde dans ChromaDB
        collection.add(
            ids=[f"rev_{idx}"],
            embeddings=[emb],
            documents=[text],
            metadatas=[{
                "restaurant": resto,
                "food": food_score,
                "service": service_score,
                "price": price_score,
                "ambiance": ambiance_score,
                "rating": rating
            }]
        )
        print(f"✅ [{resto}] Food: {food_score}/10 | Service: {service_score}/10 | Prix: {price_score}/10 | Ambiance: {ambiance_score}/10")

    except Exception as e:
        print(f"❌ Erreur sur l'avis {idx} ({resto}): {e}")

print("\n🎉 Terminé ! Les données et vecteurs sont bien stockés dans ChromaDB.")
print(f"Total avis dans le RAG: {collection.count()}")
