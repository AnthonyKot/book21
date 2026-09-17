"""Tiny document-approval rules used as the build's test subject."""


def can_approve(role, state):
    return role == 'reviewer' and state == 'submitted'


def next_state(state, decision):
    if state != 'submitted':
        raise ValueError('only submitted documents can be decided')
    return 'approved' if decision == 'approve' else 'rejected'
