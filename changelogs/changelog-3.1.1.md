# Version 3.1.1

Release Date: 2026-10-13

## Fixed

- Fixed `BeanMapper` silently dropping a property explicitly set to `null`
  when that property has a non-null default, in applications where
  `jackson-databind` comes before this library on the class path. Such a
  `null` is how a configuration switches a default off (for example a
  crawler's `sitemapResolver: null`); when it was not written, reading the
  result back brought the default back, with no error. The null-keeping
  behavior depended on this library shipping a copy of Jackson's
  `PropertyBuilder` under Jackson's own package and class name, which only
  took effect when this library came first on the class path. A single
  executable jar whose class path index is sorted by file name, such as a
  Spring Boot application, has `jackson-databind` first, so the nulls were
  lost there while every test on an ordinary Maven class path passed.
  Behavior is now independent of class path order.
  Written for a property, as before:
  - left at its non-null default: not written (the reader uses the default);
  - `null` over a non-null default: written as `null`;
  - `null` over a `null` default: not written;
  - an explicit per-property inclusion such as `@JsonInclude(NON_NULL)`
    still applies.

## Removed

- This library no longer ships its own copy of
  `tools.jackson.databind.ser.PropertyBuilder`. It was an implementation
  detail, never meant to be used directly.

## Upgrade Notes

- No code change is needed. If you worked around the missing nulls (for
  example by adding them to written configurations yourself), that
  workaround can be removed once you are on 3.1.1.
- Two public classes were added to support the fix:
  `com.norconex.commons.lang.bean.jackson.NullKeepingPropertyBuilder` and
  `NullKeepingSerializerFactory`. `BeanMapper` installs them itself; they are
  not meant to be used directly.
