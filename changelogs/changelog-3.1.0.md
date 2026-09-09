# Version 3.1.0

Release Date: 2026-09-09

## Added

- `ClassFinder` can now read a class index instead of scanning a JAR: a JAR
  shipping `META-INF/norconex/classes.idx` (one fully qualified class name,
  or archive path, per line) has that index read in place of opening and
  walking the archive. New `ClassFinder#INDEX_RESOURCE` constant.
- New `norconex.classfinder.scanMode` system property (`all`, the default,
  or `indexed`) lets `ClassFinder` skip classpath JARs carrying no class
  index entirely. New `ClassFinder#PROPERTY_SCAN_MODE`, `#SCAN_MODE_ALL`,
  `#SCAN_MODE_INDEXED` constants.
- New `norconex.classfinder.includeJars` system property: comma-separated
  wildcard patterns matched against classpath JAR file names. A JAR that
  does not match is never opened, which is what actually saves time —
  deciding whether a JAR carries an index still means opening it, but
  deciding from the file name does not. On a 242-entry classpath, scanning
  only 5 matching JARs dropped the scan from ~190ms to ~63ms. New
  `ClassFinder#PROPERTY_INCLUDE_JARS` constant.
- New `norconex.classfinder.extDir` system property (default `ext`) names a
  directory that `ClassFinder` always scans, whatever the scan mode or
  include patterns say, and whether or not it is on the classpath — the
  place for a JAR whose types must still be discovered by scanning despite
  being excluded by the settings above. New `ClassFinder#PROPERTY_EXT_DIR`,
  `#DEFAULT_EXT_DIR` constants.
- New `ClassFinder#clearCache()` method to force the next lookup to rescan.

## Fixed

- Fixed `ConfigurationLoader#toObject(Path, Class)` and
  `#toObject(Path, Object)` failing with `Illegal processing instruction
  target ("xml")` when a `#parse`d or `#include`d configuration fragment
  carried its own XML declaration or DOCTYPE, which get spliced into the
  middle of the parent document. `#toXml(Path)` already stripped these;
  the `BeanMapper`-based `#toObject` methods did not.
- Fixed `ConfigurationLoader` giving an implicit variables file (matched by
  configuration file name) higher precedence than an explicitly supplied
  one, the reverse of the documented order. An explicit variables file now
  correctly overrides values from the implicit one.
- Fixed `ClassFinder` leaving `jar:` URL connection caching on while
  reading a class index, which held the archive open for the life of the
  process.

## Removed

- Removed `XmlIf.xsd` and `XmlIfNot.xsd`. Unreachable: `XmlIf` and
  `XmlIfNot` are package-private, and `Xml#validate(Class)` — the only
  method that ever loads an XSD — has no call site targeting them anywhere
  in this library or in `crawlers-v4`.
- Removed the `xercesImpl`, `xpath2.processor`, and `java-cup`
  dependencies. They existed solely to support XSD 1.1 validation for the
  two schemas above; the JDK has never implemented XSD 1.1 (still true as
  of JDK 26), so a usable factory always meant carrying these, about
  2.35MB, into every downstream distribution for schemas nothing could
  reach.

## Deprecated

- `XmlUtil#W3C_XML_SCHEMA_NS_URI_1_1` is deprecated. Kept only so code
  that references the constant still compiles.

## Breaking Changes

- `XmlUtil#createSchemaFactory()` now returns the JDK's built-in XSD 1.0
  factory. It previously always requested XSD 1.1. Nothing in this
  library used that capability, but a caller that did should add an XSD
  1.1 implementation to their own classpath and construct a
  `SchemaFactory` directly rather than through this method.
- The `ConfigurationLoader` variables-precedence fix above changes
  resolved values for any configuration that has both an implicit and an
  explicit variables file defining the same key and was relying on the
  old, reversed order.

## Upgrade Notes

- If you referenced `org.exist-db.thirdparty.xerces:xercesImpl`,
  `com.rackspace.eclipse.webtools.sourceediting:org.eclipse.wst.xml.xpath2.processor`,
  or `edu.princeton.cup:java-cup` transitively through this library,
  declare them directly in your own project — they no longer come from
  `norconex-commons-lang`.
