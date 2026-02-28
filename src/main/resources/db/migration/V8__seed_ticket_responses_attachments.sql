INSERT INTO ticket_response (
    id,
    ticket_id,
    admin_id,
    message,
    created_at
) VALUES
    (
        1,
        3,
        'd71ccb57-6bc1-4660-a33c-28ab86998d12',
        'Reset the account and cleared the cache. Please retry login.',
        '2026-02-28 12:05:00'
    ),
    (
        2,
        4,
        'd71ccb57-6bc1-4660-a33c-28ab86998d12',
        'Replacement ID is ready for pickup at the registrar.',
        '2026-02-28 16:10:00'
    );

INSERT INTO ticket_attachment (
    id,
    ticket_id,
    storage_path,
    original_filename,
    mime_type,
    size_bytes,
    uploaded_at
) VALUES
    (
        1,
        1,
        'tickets/2026/02/28/tkt-20260228-0001/request-details.pdf',
        'advising-request.pdf',
        'application/pdf',
        245612,
        '2026-02-28 10:02:00'
    ),
    (
        2,
        3,
        'tickets/2026/02/28/tkt-20260228-0003/screenshot-login.jpg',
        'aims-login-error.jpg',
        'image/jpeg',
        181034,
        '2026-02-28 10:18:00'
    );
