import json
import re

file_path = 'app/src/main/assets/TKA_SMP_2027_complete.json'
with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

options_map = {
    41: ["3 orang", "5 orang", "18 orang", "25 orang"],
    42: ["7", "8", "12", "19"],
    43: ["Rp20.000", "Rp25.000", "Rp35.000", "Rp55.000"],
    44: ["f(x) = 3x - 1", "f(x) = 3x + 1", "f(x) = 2x + 3", "f(x) = 2x - 3"],
    45: ["52 kursi", "60 kursi", "200 kursi", "300 kursi"],
    46: ["3 cm", "6 cm", "9 cm", "12 cm"],
    47: ["(2, 4)", "(2, -4)", "(5, 3)", "(5, -5)"],
    48: ["15 cm", "20 cm", "24 cm", "25 cm"],
    49: ["75", "80", "82,5", "85"],
    50: ["6 kemungkinan", "12 kemungkinan", "18 kemungkinan", "36 kemungkinan"],
    51: ["Rp20.000", "Rp30.000", "Rp40.000", "Rp48.000"],
    52: ["3x + 2 \u2264 34", "3x + 2 \u2264 17", "6x + 4 \u2264 34", "6x + 2 \u2264 34"],
    53: ["Rp5.000", "Rp10.000", "Rp20.000", "Rp25.000"],
    54: ["-3", "1", "3", "11"],
    55: ["3 meja", "25 meja", "28 meja", "31 meja"],
    56: ["9 meter", "10 meter", "14 meter", "16 meter"],
    57: ["(2, 5)", "(2, -8)", "(2, -2)", "(6, 2)"],
    58: ["440 cm³", "513 cm³", "1.026 cm³", "1.540 cm³"],
    59: ["77,5", "80", "82,5", "85"],
}

for q in data['questions']:
    if 41 <= q['id'] <= 59:
        opts = options_map.get(q['id'])
        if opts:
            q['options'] = {
                "A": opts[0],
                "B": opts[1],
                "C": opts[2],
                "D": opts[3]
            }
        
        # Remove any image references since they are not diagrams (except if we need to keep diagrams)
        # But wait, does Q41-59 have diagrams? Let's check their images list.
        # "Only keep bitmap images when they are actual: diagrams, charts, tables, posters, geometric figures..."
        # I should print their current images.

with open(file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4, ensure_ascii=False)

print("JSON updated successfully.")
