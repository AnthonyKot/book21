"""Deliberately unfinished: a mutable tag is not enough to accept a pairing."""
def accepts(record, requested_tag, selected_image_id, inventory_bytes, cdx_bytes):
    # Exercise: bind both report files to the selected immutable image.
    return record.get('imageTag') == requested_tag
