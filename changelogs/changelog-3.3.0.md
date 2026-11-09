# Version 3.3.0

Release Date: 2026-11-09

## Added

- None.

## Improved

- `Operator` is now also read from the name of its constants, in addition to the
  symbols and abbreviations it already understood. `GREATER_THAN`,
  `GreaterThan`, `greater-than` and `greater than` are all the same operator
  (`>`, `gt`), as are the equivalent names of `GREATER_EQUAL`, `EQUALS`,
  `LOWER_EQUAL` and `LOWER_THAN`. Letter case and any character that is not a
  letter or a digit are ignored when matching a name.

  Until now, a constant name in a configuration was not recognized and the
  operator was read as nothing, which conditions then treated as "equals"
  without warning. Text that is still not recognized (such as `greater`) is
  unchanged: `Operator.of(...)` returns `null` for it.

## Fixed

- None.

## Deprecated

- None.

## Removed

- None.

## Breaking Changes

- None.

## Upgrade Notes

- Nothing is required to keep existing behaviour. A configuration that spelled an
  operator with a constant name and was silently behaving as "equals" will now
  behave as written.
