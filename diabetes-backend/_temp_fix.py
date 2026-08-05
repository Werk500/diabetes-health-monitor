import re  
  
path = r'src\main\java\com\diabetes\monitor\service\impl\AiServiceImpl.java'  
with open(path, 'r', encoding='utf-8') as f:  
    lines = f.readlines()  
for i, l in enumerate(lines, 1):  
    if 'systemPrompt' in l and 'aiChatStream' in l:  
        print(f'LINE {i}: {l}')  
    if 'private DashScopeRequest' in l:  
        print(f'LINE {i}: {l}') 
