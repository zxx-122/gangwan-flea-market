# generate_vhs_image.py
# 用法：在命令行中设置环境变量 OPENAI_API_KEY，然后运行：
#    python generate_vhs_image.py
# 会在当前目录生成 vhs_portrait.png

import os
import sys
import base64
import requests

API_KEY = os.getenv('OPENAI_API_KEY')
if not API_KEY:
    print('请先设置环境变量 OPENAI_API_KEY（例如：export OPENAI_API_KEY=sk-... 或 setx OPENAI_API_KEY "sk-..."）。')
    sys.exit(1)

prompt = (
    "80s VHS-style portrait of a single adult wearing a denim jacket, natural smile, "
    "soft neon rim lighting, warm film color shift, heavy film grain, subtle VHS scanlines and slight "
    "motion blur, analog timestamp overlay (e.g. 01:23:45 10/12/86), 4:5 portrait aspect, photorealistic, high detail"
)

endpoint = 'https://api.openai.com/v1/images/generations'
headers = {
    'Authorization': f'Bearer {API_KEY}',
    'Content-Type': 'application/json',
}

payload = {
    'model': 'gpt-image-1',
    'prompt': prompt,
    'size': '1024x1280',
    'n': 1,
}

print('Requesting image generation...')
resp = requests.post(endpoint, headers=headers, json=payload)
if resp.status_code != 200:
    print(f'Error {resp.status_code}: {resp.text}')
    sys.exit(1)

data = resp.json()
# 解析 base64
b64 = data['data'][0].get('b64_json') if data.get('data') else None
if not b64:
    print('未返回图像数据：', data)
    sys.exit(1)

img_bytes = base64.b64decode(b64)
out_path = os.path.join(os.getcwd(), 'vhs_portrait.png')
with open(out_path, 'wb') as f:
    f.write(img_bytes)

print('Saved:', out_path)

# 备选 curl 命令（Linux / macOS）：
# curl -s -X POST "https://api.openai.com/v1/images/generations" \
#   -H "Authorization: Bearer $OPENAI_API_KEY" \
#   -H "Content-Type: application/json" \
#   -d '{"model":"gpt-image-1","prompt":"REPLACE_PROMPT","size":"1024x1280","n":1}' \
#   | jq -r '.data[0].b64_json' | base64 --decode > vhs_portrait.png

# 若需调整效果：修改 prompt（更多 "film grain", "vhs artifacts", "soft neon rim light" 等），
# 或把 size 改为 1536x1920（更高分辨率，消耗更多）。
