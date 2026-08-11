import json
path = r'C:\Users\ASUS\.cursor\projects\c-Spherical-Insights-Projects-sample-2-sample-refactored-sample\agent-transcripts\f6e00df6-3adf-4e41-9463-fcfe2b6651c4\f6e00df6-3adf-4e41-9463-fcfe2b6651c4.jsonl'
with open(path, encoding='utf-8') as f:
    for i, line in enumerate(f, 1):
        if 'CreatePlan' not in line:
            continue
        obj = json.loads(line)
        content = obj.get('message', {}).get('content', [])
        if not isinstance(content, list):
            continue
        for c in content:
            if isinstance(c, dict) and c.get('name') == 'CreatePlan':
                inp = c.get('input', {})
                print('=== PLAN ===')
                print(inp.get('plan', ''))
                print('=== TODOS ===')
                print(json.dumps(inp.get('todos', []), indent=2))
