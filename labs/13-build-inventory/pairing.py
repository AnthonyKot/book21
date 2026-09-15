"""Check that an inventory pairing record describes the image selected for release.

Guided tooling for essay 13. Matching bytes and identifiers shows association with recorded
evidence; it does not establish who produced the record or that the scanner was complete.
"""
import hashlib
import json


def accepts(record, requested_tag, selected_image_id, inventory_bytes, cdx_bytes):
    """Accept only when the selection, the native scan's subject and both report hashes agree."""
    try:
        inventory = json.loads(inventory_bytes)
        if not isinstance(inventory, dict):
            return False
        return (
            record['imageTag'] == requested_tag
            and record['imageID'] == selected_image_id
            and inventory['source']['type'] == 'image'
            and inventory['source']['metadata']['imageID'] == selected_image_id
            and record['inventorySha256'] == hashlib.sha256(inventory_bytes).hexdigest()
            and record['cycloneDxSha256'] == hashlib.sha256(cdx_bytes).hexdigest()
        )
    except (KeyError, TypeError, ValueError):
        return False
