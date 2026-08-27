import tomllib

with open('/Users/ahmed/Work/trackflow/mobile/gradle/libs.versions.toml', 'rb') as f:
    t_toml = tomllib.load(f)

with open('/Users/ahmed/Work/mobile-sdks/payment/gradle/libs.versions.toml', 'rb') as f:
    p_toml = tomllib.load(f)

merged = {'versions': {}, 'libraries': {}, 'bundles': {}, 'plugins': {}}

for s in merged.keys():
    for k, v in t_toml.get(s, {}).items():
        merged[s][k] = v
    for k, v in p_toml.get(s, {}).items():
        if k not in merged[s]:
            merged[s][k] = v

def format_val(v):
    if isinstance(v, str):
        return f'"{v}"'
    elif isinstance(v, dict):
        items = []
        for dk, dv in v.items():
            if isinstance(dv, str):
                items.append(f'{dk} = "{dv}"')
            else:
                items.append(f'{dk} = {dv}')
        return '{ ' + ', '.join(items) + ' }'
    elif isinstance(v, list):
        items = []
        for i in v:
            items.append(f'"{i}"')
        return '[\n    ' + ',\n    '.join(items) + '\n]'
    return str(v)

with open('/Users/ahmed/Work/mobile-sdks/gradle/libs.versions.toml', 'w') as f:
    for s in merged.keys():
        f.write(f'[{s}]\n')
        for k, v in merged[s].items():
            f.write(f'{k} = {format_val(v)}\n')
        f.write('\n')
