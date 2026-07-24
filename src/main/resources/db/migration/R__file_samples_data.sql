-- Repeatable migration: seed/refresh the webapp's 3D model file samples.
truncate file_samples;

insert into file_samples (uuid, label, description, model_uri, thumbnail_uri, applicable_tools)
values ('d2e86ccd-8e41-4237-ad49-d16514f8c8ce',
        'Sample GLB from Zenodo',
        'A small glTF binary sample for testing mesh tools and thumbnail generation.',
        'https://zenodo.org/api/records/20133425/files/Bruss_20260512_Y-up.glb/content',
        null,
        '{CONVERT,COMPRESS,THUMBNAIL,DETAILS,VIEWER}'),
       ('ea20acd1-5d26-43db-bf37-4c8c6f2e53f2',
        'Sample GLB from repox',
        'A small glTF binary sample for testing mesh tools and thumbnail generation.',
        'https://storev2-api.repox.io/p/datastreams/02b702da-829d-4ec3-8209-e8f23832fc1f/files/7ab5783c-128a-4e56-90d2-fd139217b5d6/view?t=demo3d',
        null,
        '{CONVERT,COMPRESS,THUMBNAIL,DETAILS,VIEWER}'),
    /*
       ('5eded9b6-1908-47f3-9cf7-7b311a931be1',
        'Sample GLB with thumbnail',
        'A small glTF binary sample for testing mesh tools and thumbnail generation. Already Draco-compressed',
        'https://zenodo.org/api/records/20244148/files/fb89f5c3a9eff2427f93d42d8a65f139.glb/content',
        'https://zenodo.org/api/records/20244148/files/fb89f5c3a9eff2427f93d42d8a65f139_r000.png/content',
        '{CONVERT,THUMBNAIL,DETAILS}'),
       ('b7c1f3a2-2d54-4c8e-9a1b-6f0e2c9d4a11',
        'Sample USDZ',
        'A textured USDZ model for testing mesh conversion and technical detail extraction.',
        'https://zenodo.org/api/records/18949753/files/a_pyxis.usdz/content',
        'https://zenodo.org/api/records/18949753/files/a_pyxis_r000.png/content',
        '{CONVERT,COMPRESS,DETAILS,THUMBNAIL}'),

     */
       ('f4a9e0d1-5b23-4e77-8c62-3a1d7b8e9c02',
        'Sample Splat from Zenodo',
        'A Gaussian splat capture for testing splat conversion and thumbnail rendering.',
        'https://zenodo.org/api/records/21086572/files/90675_Cleanup_test_v01.ksplat/content',
        null,
        '{CONVERT,COMPRESS,DETAILS,THUMBNAIL,VIEWER}'),
       ('dd5c64f6-0e38-46f5-a212-84ca7c53bc3b',
        'Sample DAE from Repox',
        'A sample file in DAE format.',
        'https://storev2-api.repox.io/p/datastreams/af033f14-3764-4a60-a111-e080c9d9e6f7/files/5b917c4c-1f69-4eb6-b031-381c5cdb6df3/view?t=demo3d',
        null,
        '{CONVERT,COMPRESS,DETAILS,THUMBNAIL,VIEWER}')
    ;