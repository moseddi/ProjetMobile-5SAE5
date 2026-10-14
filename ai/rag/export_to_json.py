"""
Export ChromaDB restaurant data to a static JSON for use in React (no server needed).
"""
import chromadb
import json

CHROMA_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/rag/chroma_db"
OUTPUT_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/web/src/data/ragRestaurants.json"

client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = client.get_or_create_collection(name="restaurant_reviews")

results = collection.get(include=["metadatas", "documents"])

restaurants = {}
snippets = []

for meta, doc in zip(results["metadatas"], results["documents"]):
    name = meta.get("restaurant", "Unknown")
    food = meta.get("food", 5)
    service = meta.get("service", 5)
    price = meta.get("price", 5)
    ambiance = meta.get("ambiance", 5)
    rating = meta.get("rating", "N/A")

    # Keep best scores per restaurant (aggregate by max)
    if name not in restaurants:
        restaurants[name] = {"restaurant": name, "food": food, "service": service,
                             "price": price, "ambiance": ambiance, "rating": rating}
    else:
        r = restaurants[name]
        r["food"] = max(r["food"], food)
        r["service"] = max(r["service"], service)
        r["price"] = max(r["price"], price)
        r["ambiance"] = max(r["ambiance"], ambiance)

    # Store review snippets for autocomplete
    snippets.append({
        "restaurant": name,
        "text": doc[:200]
    })

output = {
    "restaurants": list(restaurants.values()),
    "snippets": snippets[:30]  # keep top 30 for autocomplete
}

import os
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"✅ Exported {len(restaurants)} restaurants and {len(snippets)} snippets to {OUTPUT_PATH}")
