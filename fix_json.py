import json

file_path = 'app/src/main/assets/TKA_SMP_2027_complete.json'
with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

for q in data['questions']:
    if q['id'] == 34:
        q['question'] = "Diketahui fungsi f(x) = 5x - 3. Nilai dari f(4) + f(-2) adalah ...."
        q['images'] = []
        q['options'] = {
            "A": "4",
            "B": "8",
            "C": "17",
            "D": "30"
        }
        q['answer'] = "A"
    
    if q['id'] == 40:
        q['question'] = "Dalam sebuah kantong terdapat 5 kelereng merah, 3 kelereng kuning, dan 2 kelereng hijau. Peluang terambilnya kelereng yang bukan berwarna merah adalah ...."
        q['images'] = []
        q['options'] = {
            "A": "1/2",
            "B": "3/10",
            "C": "1/5",
            "D": "7/10"
        }
        q['answer'] = "A"

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4, ensure_ascii=False)

print("JSON updated successfully.")
