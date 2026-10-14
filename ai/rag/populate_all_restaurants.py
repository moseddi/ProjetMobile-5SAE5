"""
Fast batch processor to extract multi-criteria scores for ALL 100 restaurants
from the 26,000-row Kaggle CSV dataset into ragRestaurants.json.
"""
import pandas as pd
import re
import json

CSV_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/ai/data/Restaurant reviews.csv"
OUTPUT_PATH = "C:/Users/seddi/Downloads/Flutter/Flutter/web/src/data/ragRestaurants.json"

df = pd.read_csv(CSV_PATH)

# Keyword dictionaries for criteria scoring
FOOD_POS = ['delicious','good food','tasty','flavorful','fresh','yummy','amazing food','excellent food','spicy','biryani','authentic','delicious food']
FOOD_NEG = ['bad food','tasteless','cold','bland','spoiled','stale','inedible','horrible food','worst food','disappointing food']

SERV_POS = ['great service','polite','courteous','attentive','quick service','fast service','friendly staff','hospitable','excellent service','good service','helpful']
SERV_NEG = ['slow service','rude','bad service','poor service','unfriendly','ignored us','waited','terrible service','worst service','arrogant']

PRICE_POS = ['affordable','reasonable','cheap','good value','worth it','cost effective','pocket friendly','decent price','inexpensive']
PRICE_NEG = ['expensive','overpriced','too pricey','extortionate','costly','high price','not worth']

AMB_POS = ['great ambience','beautiful','cozy','decor','music','atmosphere','pleasant','nice view','romantic','clean','great ambiance','lovely ambiance']
AMB_NEG = ['bad ambiance','noisy','loud','dirty','uncomfortable','dark','poor atmosphere','no ambiance','crowded']

def calc_score(reviews, pos_words, neg_words):
    pos_count = 0
    neg_count = 0
    total = 0
    for rev in reviews:
        if not isinstance(rev, str): continue
        rev_lower = rev.lower()
        has_pos = any(w in rev_lower for w in pos_words)
        has_neg = any(w in rev_lower for w in neg_words)
        if has_pos:
            pos_count += 1
            total += 1
        if has_neg:
            neg_count += 1
            total += 1
    
    if total == 0:
        return 7  # default neutral-good baseline (7/10)
    
    ratio = pos_count / total
    score = int(round(ratio * 6 + 4)) # scale from 4 to 10
    return max(3, min(10, score))

restaurants = []
snippets = []

grouped = df.groupby('Restaurant')

for name, group in grouped:
    if len(name.strip()) < 2: continue
    
    rev_list = group['Review'].dropna().tolist()
    
    food_score = calc_score(rev_list, FOOD_POS, FOOD_NEG)
    serv_score = calc_score(rev_list, SERV_POS, SERV_NEG)
    price_score = calc_score(rev_list, PRICE_POS, PRICE_NEG)
    amb_score = calc_score(rev_list, AMB_POS, AMB_NEG)
    
    # Calculate overall average rating out of 5
    ratings = []
    for r in group['Rating']:
        try:
            val = float(str(r).replace('LIKE', '').strip())
            if 1 <= val <= 5:
                ratings.append(val)
        except:
            pass
    
    avg_rating = round(sum(ratings)/len(ratings), 1) if ratings else 4.2
    
    restaurants.append({
        "restaurant": str(name).strip(),
        "food": food_score,
        "service": serv_score,
        "price": price_score,
        "ambiance": amb_score,
        "rating": str(avg_rating)
    })
    
    # Collect top 2 review snippets per restaurant for autocomplete
    for rev in rev_list[:2]:
        if len(str(rev)) > 20:
            snippets.append({
                "restaurant": str(name).strip(),
                "text": str(rev)[:200].replace('\n', ' ')
            })

output = {
    "restaurants": restaurants,
    "snippets": snippets
}

with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"SUCCESS: Populated ALL {len(restaurants)} restaurants and {len(snippets)} review snippets into {OUTPUT_PATH}!")
