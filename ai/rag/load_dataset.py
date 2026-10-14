import chromadb
import pandas as pd
import requests
import json
import time

# ─────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────
OLLAMA_URL_EMBED = "http://localhost:11434/api/embeddings"
OLLAMA_URL_CHAT  = "http://localhost:11434/api/generate"
EMBED_MODEL      = "nomic-embed-text"
LLM_MODEL        = "mistral"
TSV_FILE         = "../data/Restaurant_Reviews.tsv"
CHROMA_PATH      = "./chroma_db"
MAX_REVIEWS      = 50  # Start with 50 to test (change to 1000 for full)

# ─────────────────────────────────────────
# CONNECT TO CHROMADB
# ─────────────────────────────────────────
client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = client.get_or_create_collection(name="restaurant_reviews")
print("✅ ChromaDB connected")

# ─────────────────────────────────────────
# LOAD DATASET
# ─────────────────────────────────────────
df = pd.read_csv(TSV_FILE, sep='\t')
df = df.head(MAX_REVIEWS)
print(f"✅ Loaded {len(df)} reviews from TSV")

# ─────────────────────────────────────────
# FUNCTIONS
# ─────────────────────────────────────────

def extract_scores_with_mistral(review_text):
    """Send review to Mistral and extract multi-criteria scores"""
    prompt = f"""Tu es un expert en analyse d'avis clients de restaurants.

Analyse cet avis et extrais un score de 0 à 10 pour chaque critère.
Si un critère n'est pas mentionné, mets 5 (neutre).

Avis: "{review_text}"

Réponds UNIQUEMENT en JSON valide, rien d'autre:
{{
  "food": <nombre 0-10>,
  "service": <nombre 0-10>,
  "price": <nombre 0-10>,
  "ambiance": <nombre 0-10>,
  "sentiment": "<positif ou negatif>"
}}"""

    response = requests.post(OLLAMA_URL_CHAT, json={
        "model": LLM_MODEL,
        "prompt": prompt,
        "stream": False
    })

    raw = response.json()["response"].strip()

    # Extract JSON from response
    start = raw.find("{")
    end = raw.rfind("}") + 1
    json_str = raw[start:end]

    scores = json.loads(json_str)
    return scores


def get_embedding(text):
    """Get embedding vector from Ollama"""
    response = requests.post(OLLAMA_URL_EMBED, json={
        "model": EMBED_MODEL,
        "prompt": text
    })
    return response.json()["embedding"]


def calculate_final_score(scores):
    """Weighted average of all criteria"""
    return round(
        scores["food"]    * 0.35 +
        scores["service"] * 0.25 +
        scores["price"]   * 0.20 +
        scores["ambiance"]* 0.20,
        2
    )


# ─────────────────────────────────────────
# MAIN PIPELINE
# ─────────────────────────────────────────
print("\n⏳ Processing reviews with Mistral + ChromaDB...\n")

success = 0
errors  = 0

for index, row in df.iterrows():
    review_text = row["Review"]
    liked       = row["Liked"]

    try:
        # Step 1: Extract multi-criteria scores with Mistral
        scores = extract_scores_with_mistral(review_text)

        # Step 2: Calculate final score
        final_score = calculate_final_score(scores)

        # Step 3: Build text for embedding
        text_to_embed = f"""
Avis: {review_text}
Nourriture: {scores['food']}/10
Service: {scores['service']}/10
Prix: {scores['price']}/10
Ambiance: {scores['ambiance']}/10
Sentiment: {scores['sentiment']}
"""

        # Step 4: Get embedding
        embedding = get_embedding(text_to_embed)

        # Step 5: Store in ChromaDB
        collection.add(
            ids=[f"tsv_{index}"],
            embeddings=[embedding],
            documents=[text_to_embed],
            metadatas=[{
                "review_text":  review_text,
                "liked":        int(liked),
                "food_score":   scores["food"],
                "service_score":scores["service"],
                "price_score":  scores["price"],
                "ambiance_score":scores["ambiance"],
                "final_score":  final_score,
                "sentiment":    scores["sentiment"]
            }]
        )

        success += 1
        print(f"  ✔ [{index+1}/{MAX_REVIEWS}] food:{scores['food']} service:{scores['service']} price:{scores['price']} | '{review_text[:50]}...'")

        # Small pause to not overload Ollama
        time.sleep(0.5)

    except Exception as e:
        errors += 1
        print(f"  ✗ [{index+1}] Error: {e} | '{review_text[:40]}'")

print(f"\n🎉 Done! {success} stored, {errors} errors")
print(f"📁 ChromaDB saved at: {CHROMA_PATH}")
print(f"📊 Total in collection: {collection.count()}")
