"""Produce a local pairing record. This is not a signed attestation."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def sha256(data):
    return hashlib.sha256(data).hexdigest()

def create_record(image, inventory_bytes, app_bytes, cdx_bytes, config_bytes):
    inventory = json.loads(inventory_bytes)
    if inventory['source']['metadata']['imageID'] != image['Id']:
        raise ValueError('Scanner source does not identify the selected image')
    if (image['Os'], image['Architecture']) != ('linux', 'amd64'):
        raise ValueError('This measured fixture requires linux/amd64')
    return {
        'imageTag': 'book21-inventory:candidate',
        'imageID': image['Id'],
        'platform': 'linux/amd64',
        'inventorySha256': sha256(inventory_bytes),
        'cycloneDxSha256': sha256(cdx_bytes),
        'appJarSha256': sha256(app_bytes),
        'scanner': 'syft',
        'scannerVersion': inventory['descriptor']['version'],
        'scope': inventory['descriptor']['configuration']['search']['scope'],
        'scannerConfigSha256': sha256(config_bytes),
    }

if __name__ == '__main__':
    for label in ['a', 'b']:
        stage = ROOT/'out'/label
        record = create_record(json.loads((stage/'image.json').read_text()),
                               (stage/'image.syft.json').read_bytes(), (stage/'app.jar').read_bytes(),
                               (stage/'image.cdx.json').read_bytes(), (ROOT/'syft.yaml').read_bytes())
        (stage/'record.json').write_text(json.dumps(record, indent=2)+'\n')
        print(label, record['imageID'], record['inventorySha256'])
