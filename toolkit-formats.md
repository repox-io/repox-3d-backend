# 3D Toolkit — Conversion & Compression Formats

Reference for the file types the toolkit can **convert** and **compress**, and the engine behind
each path. Every endpoint takes the source model as **either** a multipart `file` upload **or** a
`url` parameter (exactly one — supplying both or neither is a `400`). All endpoints are
asynchronous: `POST` returns `202 Accepted` with a job, then poll `GET /jobs/{id}` and download
from `GET /jobs/{id}/output`.

## Conversion

### Mesh conversion — `POST /tools/convert/mesh`

Engine: **Assimp first, Blender fallback** (USD targets are Blender-only). The Blender fallback is
what lets it read inputs Assimp can't — notably **Draco-compressed glTF/GLB** and **USDZ**.

| | Formats |
|---|---|
| **Inputs** (readable) | GLB, glTF, OBJ, FBX, DAE/Collada, STL, PLY, USD, USDA, USDC, USDZ |
| **Outputs** (`MeshExportFormat`) | GLB, GLTF, OBJ, STL, PLY, FBX, DAE, USD, USDA, USDC, USDZ |

- GLB / GLTF / OBJ / STL / PLY / FBX / DAE → Assimp (`GLB2`/`GLTF2`/…) with automatic Blender fallback.
- **USD family (USD / USDA / USDC / USDZ) is Blender-only** — Assimp cannot export USD.
- Assimp's niche/debug export tokens (assbin, x, 3ds, 3mf, x3d, pbrt, stp, …) are intentionally not exposed.

### Splat conversion — `POST /tools/convert/splat`

Engine: **GaussForge (`gfconvert`)**. For Gaussian splats, **conversion *is* compression** — there
is no separate compress step or quality knob; you compress by choosing a compact target format.

| | Formats (`SplatFormat`) |
|---|---|
| **Inputs / Outputs** | PLY, COMPRESSED_PLY, SPLAT, KSPLAT, SPZ, SOG |

Compact (compressing) targets: **SPZ, SOG, KSPLAT, COMPRESSED_PLY**.

## Compression

### Mesh compression — `POST /tools/compress/mesh`

Engine: **gltfpack (meshoptimizer)**.

| | |
|---|---|
| **Input** | glTF / GLB only — **plain or meshopt-encoded**. A **Draco-compressed input is rejected with `400`** (re-encoding to meshopt would only inflate it). When a file is uploaded, this is checked at submit time, so the request fails *before* a job is created. |
| **Output** | GLB with **meshopt** geometry (`EXT_meshopt_compression`). |
| **Options** | `compressTextures` — KTX2/Basis texture supercompression (`-tc`, default `true`)<br>`aggressive` — stronger meshopt (`-cc` vs `-c`, default `false`)<br>`simplifyRatio` — mesh simplification target fraction (`-si`, optional) |

### USDZ compression — `POST /tools/compress/usdz`

Engine: **Blender** (mesh decimation + texture downscaling — the only self-hostable "compression"
for USD).

| | |
|---|---|
| **Input / Output** | USDZ → USDZ |
| **Options** | `decimateRatio` — mesh decimation ratio (optional)<br>`textureMaxSize` — cap on the longest texture edge; oversized textures are downscaled (optional) |

### Splat compression — `POST /tools/compress/splat`

Engine: **GaussForge (`gfconvert`)** — the *same* tool and job as splat conversion. GaussForge has
no separate compression step, so this endpoint is convert to a **compact target**, constrained and
defaulted to make "compress" meaningful.

| | |
|---|---|
| **Input** | any splat format (PLY, COMPRESSED_PLY, SPLAT, KSPLAT, SPZ, SOG) |
| **Output / target** | a **compact** `SplatFormat` only — **SPZ, SOG, KSPLAT, COMPRESSED_PLY**. `targetFormat` is optional and **defaults to `SPZ`**; requesting `PLY`/`SPLAT` is rejected with `400` (use `/tools/convert/splat` for those). |

Payload is otherwise identical to `/tools/convert/splat` — GaussForge exposes no compression-level
or quality knob to add. Reuses the `SPLAT_CONVERT` job type/processor under the hood.

## Mental model

- **Mesh geometry codecs:** the API **produces meshopt** (via gltfpack) and **rejects Draco input**
  on the compress endpoint; it can still **read** Draco via the Blender-backed *convert* path.
- **Splats:** convert = compress (GaussForge); pick a compact container.
- **USDZ:** "compression" = Blender decimate / texture-downscale; output stays USDZ.

## Related (not convert/compress)

- **Image → 3D** — `POST /tools/image-to-3d` (TripoSR) *generates* a GLB from a single photo. It is
  a generation step, not a format conversion or compression, and is listed here only for orientation.
  It currently answers **501** while TripoSR moves to `repox-3dtools-triposr`
  (see `repox-3dtools-triposr-handover.md`).

---

*Source of truth: `MeshExportFormat`, `SplatFormat`, `MeshCompressionOptions`, `UsdzOptimizeOptions`
(package `…toolkit.domain`) and `ModelConversionController` (`…toolkit.adapters.in`). Update this doc
when those change.*
