import json
import re

with open('app/src/main/assets/TKA_SMP_2027_complete.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

results = {}
counts = {
    "OK": 0, "MISSING_OPTIONS": 0, "PLACEHOLDER_OPTIONS": 0, 
    "IMAGE_ONLY_OPTIONS": 0, "DUPLICATED_OPTIONS": 0, 
    "INVALID_CORRECT_ANSWER": 0, "NEEDS_REVIEW": 0
}

for q in data['questions']:
    q_id = q['id']
    status = "OK"
    
    options = q.get('options', {})
    answer = q.get('answer', '')
    question_text = q.get('question', '')
    images = q.get('images', [])
    content_blocks = q.get('content_blocks', [])
    combined_text = question_text + " ".join(content_blocks)
    
    # 1. Check Missing Options
    if not options:
        if images:
            status = "IMAGE_ONLY_OPTIONS"
        else:
            status = "MISSING_OPTIONS"
    else:
        # 2. Check Placeholder Options
        is_placeholder = False
        for k, v in options.items():
            v_lower = str(v).lower()
            if "pilihan " in v_lower or "option " in v_lower or "data tidak tersedia" in v_lower or v_lower.strip() == "":
                is_placeholder = True
        
        if is_placeholder:
            status = "PLACEHOLDER_OPTIONS"
        else:
            # 3. Check Invalid Correct Answer
            if not answer or answer not in options:
                status = "INVALID_CORRECT_ANSWER"
            else:
                # 4. Check Duplicated Options
                has_dup = False
                if re.search(r'\bA\..*?\bB\..*?\bC\.', combined_text):
                    has_dup = True
                if "A.B.C.D." in combined_text or "A. B. C. D." in combined_text:
                    has_dup = True
                if "Teks Pilihan" in combined_text or "Grafik / Diagram" in combined_text:
                    has_dup = True
                
                if has_dup:
                    status = "DUPLICATED_OPTIONS"

    results[q_id] = status
    counts[status] += 1

print("--- COUNTS ---")
for k, v in counts.items():
    print(f"{k}: {v}")

print("--- LIST ---")
for i in range(1, 101):
    if i in results:
        print(f"Question {i}: {results[i]}")
        
print("--- FIX LIST ---")
fix_list = [str(k) for k, v in results.items() if v != "OK"]
print("[" + ", ".join(fix_list) + "]")
