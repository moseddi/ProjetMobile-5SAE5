import chromadb
import json
import requests

# ─────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────
OLLAMA_URL = "http://localhost:11434/api/embeddings"
EMBED_MODEL = "nomic-embed-text"
DATA_FILE = "../data/reviews.json"
CHROMA_PATH = "./chroma_db"

# ─────────────────────────────────────────
# STEP 1 - Connect to ChromaDB
# ─────────────────────────────────────────
client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = client.get_or_create_collection(name="restaurant_reviews")
print("✅ ChromaDB connected")

# ─────────────────────────────────────────
# STEP 2 - Load fake reviews
# ─────────────────────────────────────────
with open(DATA_FILE, "r", encoding="utf-8") as f:
    reviews = json.load(f)
print(f"✅ Loaded {len(reviews)} reviews")

# ─────────────────────────────────────────
# STEP 3 - Embed and store each review
# ─────────────────────────────────────────
def get_embedding(text):
    """Get embedding from Ollama nomic-embed-text"""
    response = requests.post(OLLAMA_URL, json={
        "model": EMBED_MODEL,
        "prompt": text
    })
    return response.json()["embedding"]

print("⏳ Embedding reviews and storing in ChromaDB...")

for review in reviews:
    # Text we embed = comment + scores context
    text_to_embed = f"""
    Restaurant: {review['restaurant_name']}
    Avis: {review['comment']}
    Note nourriture: {review['food_score']}/10
    Note service: {review['service_score']}/10
    Note prix: {review['price_score']}/10
    Note ambiance: {review['ambiance_score']}/10
    Note globale: {review['global_rating']}/5
    """

    embedding = get_embedding(text_to_embed)

    collection.add(
        ids=[review["id"]],
        embeddings=[embedding],
        documents=[text_to_embed],
        metadatas=[{
            "restaurant_id": review["restaurant_id"],
            "restaurant_name": review["restaurant_name"],
            "food_score": review["food_score"],
            "service_score": review["service_score"],
            "price_score": review["price_score"],
            "ambiance_score": review["ambiance_score"],
            "global_rating": review["global_rating"],
            "comment": review["comment"]
        }]
    )
    print(f"  ✔ Stored: {review['restaurant_name']}")

print(f"\n🎉 Done! {len(reviews)} reviews stored in ChromaDB")
print(f"📁 Database saved at: {CHROMA_PATH}")
