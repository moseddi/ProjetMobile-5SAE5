import chromadb
import requests

# ─────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────
OLLAMA_URL = "http://localhost:11434/api/embeddings"
EMBED_MODEL = "nomic-embed-text"
CHROMA_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/rag/chroma_db"

# ─────────────────────────────────────────
# Connect to ChromaDB
# ─────────────────────────────────────────
client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = client.get_collection(name="restaurant_reviews")

def get_embedding(text):
    response = requests.post(OLLAMA_URL, json={
        "model": EMBED_MODEL,
        "prompt": text
    })
    return response.json()["embedding"]

def find_similar_restaurants(query_text, n_results=3):
    """Find restaurants similar to the query"""
    query_embedding = get_embedding(query_text)
    
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=n_results
    )
    
    print(f"\n🔍 Query: {query_text}")
    print("=" * 50)
    print(f"🏆 Top {n_results} similar restaurants:\n")
    
    for i, metadata in enumerate(results["metadatas"][0]):
        resto_name = metadata.get('restaurant', metadata.get('restaurant_name', 'Inconnu'))
        print(f"{i+1}. {resto_name}")
        print(f"   🍔 Food: {metadata.get('food', 'N/A')}/10")
        print(f"   🤝 Service: {metadata.get('service', 'N/A')}/10")
        print(f"   💶 Price: {metadata.get('price', 'N/A')}/10")
        print(f"   🏠 Ambiance: {metadata.get('ambiance', 'N/A')}/10")
        print(f"   ⭐ Rating: {metadata.get('rating', 'N/A')}")
        print()

# ─────────────────────────────────────────
# TEST QUERIES
# ─────────────────────────────────────────
if __name__ == "__main__":
    
    # Test 1: Client wants good food + good service
    find_similar_restaurants(
        "Je cherche un restaurant avec excellente nourriture et bon service"
    )

    # Test 2: Client wants cheap + fast
    find_similar_restaurants(
        "Restaurant pas cher avec service rapide"
    )

    # Test 3: Client disappointed by service
    find_similar_restaurants(
        "Bonne nourriture mais service nul, cherche mieux"
    )
