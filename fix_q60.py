import json

file_path = 'app/src/main/assets/TKA_SMP_2027_complete.json'
with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

for q in data['questions']:
    if q['id'] == 60:
        q['images'] = []
        q['options'] = {} # Could not be recovered without fabricating

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4, ensure_ascii=False)

print("JSON updated successfully for Q60.")
