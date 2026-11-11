# Forge 61 Resource Metadata

This applies to the exact Forge 1.21.11-61.1.0 target, not newer loaders.

## Mod Metadata

Keep pack.mcmeta at the mod root, declaring server data format 94.1:

```json
{"pack":{"description":"baseElectric","min_format":[94,1],"max_format":[94,1]}}
```

Keep showAsResourcePack=false at the top level of META-INF/mods.toml.
It defaults to false, but the explicit value protects this contract.

ResourcePackLoader uses PathPackResources for each mod. On the client it
combines hidden mods into DelegatingPackResources named mod_resources,
with native client metadata for 75.x. On the server it exposes the mod's
PathPackResources and reads the root's 94.1 metadata. A mod with
showAsResourcePack=true cannot use this hidden-client metadata strategy.

Do not decode the server-format root as if it were the effective client
pack. The negative test still proves that a raw 94.1 resource pack does not
accept client format 75.0. Both effective native packs must be verified.

A single raw [75,0] through [94,1] declaration fails server decoding because
the server's pre-minor threshold is 81 and requires legacy fields there.
The client's threshold is 64, so it rejects those legacy fields at 75.
Neither fallback metadata nor ignored diagnostic messages fixes that.

Omitting pack.mcmeta is not a solution. The native mod PathResourcesSupplier
returns null metadata for both PackTypes. Pack.readPackMetadata then reports
missing metadata, and ResourcePackLoader enters its broken-resources warning
path. ForgeDevLocator's development-only fallback writes obsolete format 18;
it is not correct per-PackType metadata for a packaged 1.21.11 mod.

## Native APIs

MetadataSectionSerializer is replaced by MetadataSectionType<T>. A native
PackResources implementation overrides:

```java
public <T> T getMetadataSection(MetadataSectionType<T> section) throws IOException
```

PackMetadataSection takes a Component and InclusiveRange<PackFormat>, not
an integer pack version. Use PackMetadataSection.forPackType(type) for the
correct codec, SharedConstants.getCurrentVersion().packVersion(type) for the
current format, and Pack.readMetaAndCreate(..., type, ...) when creating a
built-in pack. A server-only MaterialPolicyPack should use SERVER_DATA.
Native DelegatingPackResources is an example of directly supplied metadata.

## Separate Loader Defect

Forge 61.1.0 ResourcePackLoader.findPacks always gets its comparison version
from DetectedVersion.BUILT_IN.packVersion(CLIENT_RESOURCES), even for server
data. A correctly declared 94.1 server mod pack is therefore labelled
TOO_NEW by that discovery method. Re-reading the same native supplier with
the actual SERVER_DATA version returns COMPATIBLE, and every server resource
is accessible. The test receipt records the discrepancy explicitly.

This is distinct from codec failure. It is not fixed or hidden here, and
server compatibility-label acceptance remains a parent/platform decision.
Do not change data to format 75, suppress warnings, or fake compatibility.

## Proof

NativePackResourceProof runs a separate JVM, builds a resource-only fixture
jar from Gradle's processed Electric resources, parses it with real SecureJar
and ModFile, and calls ResourcePackLoader through PackRepository. It then
opens the actual client/server packs and MultiPackResourceManager instances.
All 306 client and 160 server files are compared byte for byte with the input.
The effective metadata round-trips through the appropriate native codec,
rejects the opposite PackType's version and rejects out-of-range versions.
The Log4j appender observes WARN/ERROR messages without filtering existing
appenders, and the parent also checks the complete child log. Any diagnostic
fails the proof. The test child has its own working directory and leaves
its jar, logs and receipt under ignored build/native-pack-resources/.

This proves resource/metadata behavior, not a full gameplay client/server
launch, basePower compatibility or the Electric release artifact.
