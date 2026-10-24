# Version 3.1.2

Release Date: 2026-10-24

## Fixed

- Fixed `BeanMapper.getPolymorphicTypes()` sometimes throwing a
  `ConcurrentModificationException` when one thread iterated the returned
  map while another thread caused a Jackson `ObjectMapper` to be created
  (for example the first read or write in a given format) on the same
  `BeanMapper` instance. This is most likely on a shared instance such as
  `BeanMapper.DEFAULT`, and surfaced as an occasional failure in parallel
  test runs.
- Fixed the same map accumulating duplicate sub-types: each time an
  `ObjectMapper` was created, every polymorphic type's sub-types were added
  to its own list again, so the lists grew with every mapper built.

## Improved

- The polymorphic types of a `BeanMapper` are now resolved once and
  published as an unmodifiable map. Registration through the builder
  (`polymorphicType`, `polymorphicTypeImpl`, ...) and through
  `PolymorphicTypeProvider` service loading is unchanged.

## Upgrade Notes

- `BeanMapper.getPolymorphicTypes()` now returns an unmodifiable map.
  It was never a registration point (types are registered when the mapper is
  built), but code that modified the returned map will now get an
  `UnsupportedOperationException` instead of silently altering the mapper's
  internal state. Register types with the `BeanMapper` builder instead.
