from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import chromadb
import requests
import uvicorn
import uuid

app = FastAPI(title="Restaurant Review RAG & Analysis API")

# Activer CORS pour permettre les requêtes depuis React (localhost:5173)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

OLLAMA_EMB_URL = "http://localhost:11434/api/embeddings"
EMBED_MODEL = "nomic-embed-text"
CHROMA_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/rag/chroma_db"

client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = client.get_or_create_collection(name="restaurant_reviews")

class QueryRequest(BaseModel):
    query: str
    n_results: int = 3

class AddReviewRequest(BaseModel):
    restaurant: str
    review: str
    food: int
    service: int
    price: int
    ambiance: int
    rating: str = "5"

@app.post("/recommend")
def recommend_restaurants(req: QueryRequest):
    try:
        r = requests.post(OLLAMA_EMB_URL, json={
            "model": EMBED_MODEL,
            "prompt": req.query
        }, timeout=30)
        query_emb = r.json().get("embedding")

        # Déterminer le filtre adéquat selon le besoin exprimé
        where_filter = None
        q_lower = req.query.lower()
        if any(w in q_lower for w in ["price", "cost", "cher", "affordable", "value"]):
            where_filter = {"price": {"$gte": 7}}
        elif any(w in q_lower for w in ["service", "accueil", "waiter", "staff"]):
            where_filter = {"service": {"$gte": 7}}
        elif any(w in q_lower for w in ["ambiance", "atmosphere", "cadre", "cozy", "warm", "decor", "décor"]):
            where_filter = {"ambiance": {"$gte": 7}}

        try:
            if where_filter:
                results = collection.query(
                    query_embeddings=[query_emb],
                    n_results=req.n_results,
                    where=where_filter
                )
            else:
                results = collection.query(
                    query_embeddings=[query_emb],
                    n_results=req.n_results
                )
        except Exception:
            results = collection.query(
                query_embeddings=[query_emb],
                n_results=req.n_results
            )

        matches = []
        if results and "metadatas" in results and results["metadatas"]:
            for metadata, doc in zip(results["metadatas"][0], results["documents"][0]):
                matches.append({
                    "restaurant": metadata.get("restaurant", "Inconnu"),
                    "food": metadata.get("food", 5),
                    "service": metadata.get("service", 5),
                    "price": metadata.get("price", 5),
                    "ambiance": metadata.get("ambiance", 5),
                    "rating": metadata.get("rating", "N/A"),
                    "review_snippet": doc[:160] + "..."
                })

        return {
            "query": req.query,
            "results": matches
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/add-review")
def add_review(req: AddReviewRequest):
    try:
        r = requests.post(OLLAMA_EMB_URL, json={
            "model": EMBED_MODEL,
            "prompt": req.review
        }, timeout=30)
        emb = r.json().get("embedding")

        review_id = f"rev_{uuid.uuid4().hex[:8]}"
        collection.add(
            ids=[review_id],
            embeddings=[emb],
            documents=[req.review],
            metadatas=[{
                "restaurant": req.restaurant,
                "food": req.food,
                "service": req.service,
                "price": req.price,
                "ambiance": req.ambiance,
                "rating": req.rating
            }]
        )
        return {
            "status": "success",
            "review_id": review_id,
            "total_reviews": collection.count()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
