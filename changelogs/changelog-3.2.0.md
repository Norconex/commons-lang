# Version 3.2.0

Release Date: 2026-10-31

## Added

- `@ClassIndex`, a package annotation. Put it on a package's `package-info.java`
  and the compiler writes `META-INF/norconex/classes.idx` into that module's
  output, listing its concrete classes. `ClassFinder` reads that listing instead
  of walking the archive, so classes are found in nested JARs (for example in a
  Spring Boot fat JAR), where a classpath scan cannot see them.
- `ClassIndexProcessor`, the annotation processor that writes the index. It is
  registered for the compiler through `META-INF/services`, so it is found on the
  compile classpath of any module that depends on this library.

## Improved

- `ClassFinder` no longer resolves a canonical path for each classpath entry when
  it checks for an index, which removes a file-system call per entry on long
  classpaths. The classes found are unchanged.

## Fixed

- None.

## Deprecated

- None.

## Removed

- None.

## Breaking Changes

- None.

## Upgrade Notes

- Nothing is required to keep existing behaviour.
- To publish an index for your own classes, add `@ClassIndex` to the package
  declaration in `package-info.java`. Modules that do not use the annotation are
  not affected.
- If your build sets `annotationProcessorPaths` explicitly (as Maven does when
  it is configured), the processor is not picked up from the classpath. Add
  `norconex-commons-lang` to that list to have the index written.
