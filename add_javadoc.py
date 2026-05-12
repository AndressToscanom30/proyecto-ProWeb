import os
import re

directory = r"c:\Users\Keiver\Documents\GitHub\proyecto-ProWeb\src\main\java\com\cronos\gestiontributaria"

def add_javadoc_to_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Check if file has class-level javadoc before public class|interface|enum|record
    pattern = re.compile(r'(?s)/\*\*.*?\*/\s*(?:@[\w\(\)\s",=]*\s*)*public\s+(class|interface|enum|record)\s+\w+')
    if pattern.search(content):
        return # Already has javadoc

    # Regex to find the declaration
    decl_pattern = re.compile(r'^(\s*)((?:@[\w\(\)\s",=]*\s*)*)(public\s+(class|interface|enum|record)\s+(\w+))', re.MULTILINE)
    
    def repl(m):
        indent = m.group(1)
        annotations = m.group(2)
        declaration = m.group(3)
        class_name = m.group(5)
        javadoc = f"{indent}/**\n{indent} * Documentación de la entidad {class_name}.\n{indent} */\n"
        return f"{javadoc}{indent}{annotations}{declaration}"

    new_content = decl_pattern.sub(repl, content, count=1)
    
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Added javadoc to {filepath}")

for root, _, files in os.walk(directory):
    for f in files:
        if f.endswith('.java'):
            add_javadoc_to_file(os.path.join(root, f))
