import os
import json

cur_path = os.path.dirname(__file__)
sam_json_path = os.path.join(cur_path, '..', 'api', 'staging', 'sam.json')

with open(sam_json_path, 'r+') as samJsonFile:
    samJson = json.load(samJsonFile)
    defaultRoleProperties = samJson['Resources']['DefaultRole']['Properties']
    defaultRoleProperties['PermissionsBoundary'] = 'arn:aws:iam::555555555555:policy/DeveloperPolicy'
    defaultRoleProperties['RoleName'] = 'AADev-DefaultRole'

os.remove(sam_json_path)
with open(sam_json_path, 'w') as samJsonFile:
    json.dump(samJson, samJsonFile, indent=2)