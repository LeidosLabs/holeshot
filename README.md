# HOLESHOT
Image Tileserver with multi-tiered caching.   

## What is HOLESHOT

- HOLESHOT* is optimized around the idea that users don’t look at all images and they’re frequently only utilizing a very small portion of the ones they do analyze (e.g. - an Airfield, a port, or a facility).
- Instead of moving entire full-framed images, which average 2-50 GB in size) around the enterprise, HOLESHOT only moves the portions of the image that a user is likely to look at. This usually results in an order of magnitude decrease in data transferred.
- Every night, HOLESHOT runs analytics to generate multi-resolution heatmaps that show the AOIs (Areas of Interest) for each user in the enterprise. The results of this analysis are used to move those AOIs in new images downstream and closer to the analyst through the multiple tiers of the HOLESHOT caching system.  (NOTE: Analytic-driven, usage-based caching isn't currently part of the opensource baseline, but is expected to be released in 2Q2020)

## How is HOLESHOT different than other Tileservers?

- Traditional Tileservers read full-framed raw imagery from a local filesystem and feed out finished tiles at display time.

  - Roaming performance is usually very blocky, as areas are blacked out prior to the tile being retrieved from the server.
  - Whenever adjustments to imagery are made, it requires a fetch of all image tiles from the server-side. Adjusting TTC, Gamma, Brightness, and Contrast black out the screen as new tiles are requested.
  - By warping pixels to WGS-84, many “Tileservers” are designed for situational awareness, not for image exploitation.
  - Most tileservers are designed to utilize a single tier of storage, discounting the need for balancing cost and performance tradeoffs.

- ##### HOLESHOT is different

  - HOLESHOT optimizes tile storage across Cloud storage tiers of S3, DynamoDB(TBD), Elasticache, and Cloudfront(TBD) as well as Edge based tiers(TBD) residing on site hosted commodity caches such as Akamai or the user’s own workstation.  Pixels that the user and/or site are interested in are moved to the appropriate storage tier to ensure optimal performance.
  - HOLESHOT was designed to serve our raw image tiles at scale. Tests have shown it capable of running at over 1500 Tiles-Per-Second, for months at a time, without downtime.
  - By serving out raw image tiles, the imagery can be precached and manipulated by the local GPU, ensuring optimal roam and image adjustment performance.
  - HOLESHOT is also capable of pushing out SIPS-Compliant display-ready image tiles at scale, service-enabling web-based mashups, WMS/WMTS/MRF compliant clients, and analytics across the enterprise. These can be served in image space or warped to WGS-84.

## Value Proposition

- Service enablement of imagery access, both raw and SIPS processed, for ELTs, web applications, and analytics across the enterprise.
- Centralized/Cloud-based management of all image holdings, increasing security and reliability, while decreasing time and personnel needed for release deployments.
- 80% decrease in image network traffic across the enterprise
- 80% decrease in remote-site image storage requirements
- Elimination of full-framed imagery staging and its associated timeline (User access to imagery in seconds rather than hours)
- Dynamic adherence to FGAC (Fine Grained Access Controls) on imagery across the enterprise.

## HOLESHOT Architecture

![Architecture](F:\dev\leidoslabs\holeshot\images\Architecture.png)

## HOLESHOT Components

- Tileserver - Serves out single-banded, full-resolution, raw image tiles
- Chipserver - Serves out Raw or Finished chips of images.   Image chips can be in image space or warped to WGS-84.  Also has the ability to serve out WMS or WMTS compliant feeds for specific images.
- ELT - Java OpenGL Electronic Light Table.  Reads from the Tileserver and applies a SIPS image chain to the image at frame rate speeds.   Performs:
  - DRA (Dynamic Range Adjustment) and TTC Lookup
  - Smooth and continuous Roaming, Zooming, and Rotating of images.
  - Manual Brightness, Contrast, and Gamma adjustments.
  - Simple Vector overlays
- Ingest Server - Listens to S3 bucket and prepares full-framed imagery for serving.
  - Tiles out full-framed images to a full-resolution image pyramid of 512x512 raw image tiles.
  - Normalizes Metadata to a common metadata.json format.
  - Uses given RPCs, or calculates RPCs if they're not available.
  - Handles GeoTiff, Uncompressed NITF, and JPEG2K Compressed NITF (with the addition of an externally provided Kakadu library, not included).