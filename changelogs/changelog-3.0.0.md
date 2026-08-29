# Version 3.0.0

Release Date: 2026-08-29

First release of the 3.x line. Requires **Java 21** or higher.

The headline change is a move from Jackson 2 to Jackson 3, together with a
new `BeanMapper` that reads and writes objects to XML, JSON, and YAML from a
single configuration model. Configuration files written for 2.x are not
read as-is by 3.x.

## Added

- New "flow" package to have conditions in XML, JSON, or Yaml configuration files. A replacement to XMLFlow.
- New BeanMapper class to write/read objects to/from XML, JSON, and Yaml, with support for polymorphism.
- New JSON and Yaml (de)serializers for types covered by GenericConverter.
- New DateModel class for easy transition from one date type to another.
- New ZonedDateTimeParser for epoch and relative dates parsing in addition to parsing from date formats.
- New Timer class.
- New SemanticVersion and SemanticVersionParser classes.
- New @JsonCollection annotation.
- New PackageManifest class.
- New #runAndCaptureOutput and #callAndCaptureOutput methods on SystemUtil.
- New JarDuplicates#get(File) method and collection based constructor.
- New StringUtil #ifBlank and #ifNotBlank methods.
- New FluentPropertyDescriptor methods: #isReadable, #isWritable, #readValue, and #writeValue.
- FluentPropertyDescriptor now has added support for builder-style accessor methods.
- New BeanUtil methods: #getPropertyDescriptors(Class), #getWriteMethod, #getReadMethod, #isWritable, #isReadable (the last two replaces the now deprecated #isGettable and #isSettable).
- New SystemCommand methods: #getErrorListeners and #getOutputListeners.
- New FileUtil#isFile methods providing a null-safe variant to File#isFile or Files#isRegularFile.
- New FileUtil#isFile methods providing a null-safe variant to File#isDirectory or Files#isDirectory.
- New FileUtil#toPath methods providing a null-safe variant to File#toPath.
- New FileUtil#toPaths methods.
- New JarDuplicates#getAllButGreatest method.
- New JarFile#toJarFiles methods.
- New #getMapChangeListeners and #clearMapChangeListeners methods on ObservableMap and MapChangeSupport.
- New TextMatcher #trim and #matchEmpty methods.
- New Regex #trim and #matchEmpty methods.
- New Credentials constructors accepting username, password, and password key.
- New DateUtil date conversion methods using UTC.
- New YearMonthDay LocalDate constructor plus new #toLocalDate and #toLocalDateTime methods.
- New XML methods: isEmpty, hasChildElements, hasAttributes, hasTextContent, isElementPresent, computeIfElementAbsent, getTextContent, removeTextContent.
- New XPathUtil class.
- New CollectionUtil#unionList and CollectionUtil#unionSet methods.
- CachedInputStream#nullInputStream() now returns a CachedInputStream instance.
- New MutableImage#fromBase64String method.
- New PatternConverter class along with XML#getPattern method.
- New ProxySettings#toProxy method (java.net.Proxy).
- New ClassUtil#newInstance(Object...) method.
- New XMLAdapters class to add JAXB support for the same type offered by GenericConverter.
- New Configurable interface.
- ConfigurationLoader can now load XML, JSON, and Yaml into objects.
- New PredicatedConsumer #isTrue and #isFalse and #elseConsumer methods.
- New Predicates #anyOf and #allOf methods.
- New Consumers #of method.
- New IOUtil #toNonNullWriter and #toNonNullOutputStream methods.
- New TextMatcher#isSet method.

## Improved

- Now require Java 17+.
- Taglets rewritten to use the new jdk.javadoc.doclet.* API.
- EncryptionUtil now uses more secure cypher "AES/GCM/NoPadding" with EncryptionKey default key size now being 256.
- SystemUtil method invoking Callable now throws an UncheckedCallableException.
- Event name now included in generated string from Event#toString().
- Introduced Lombok.
- Huge jump in unit test code coverage.
- CollectionUtil#toArray now returns an empty array instead of null when a null collection is supplied.
- Using a variables file with ConfigurationLoader now assumes the ".variables" format when its extension is neither ".variables" nor ".properties".
- Converter#toString(Object, String) will now return the default value when the object is null.
- EncryptionUtil#main(String[]) no longer issue a System#exit.
- EventManager#removeListener not remove entries by identity to be consistent with add methods.
- EqualUtil now considers supplying an empty vargars as always non equal.
- FileUtil#head now returns lines in order they are read.
- FileUtil#deleteEmptyDirs now deletes directories with empty directories, recursively.
- FileUtil#deleteEmptyDirs now throws IOException.
- FileUtil#deleteEmptyDirs methods were overloaded to support Path.
- FileUtil#dirEmpty method was overloaded to support Path.
- FileUtil#moveFile was overloaded to support Path and now creates missing directories in destination path. I also waits half a second between retries (was 1 second).
- FileUtil#moveFileToDir was overloaded to support Path.
- FileUtil#dirHasFile was overloaded to support Path.
- Dependency "java.xml.bind" replaced by "jakarta.xml.bind".
- ByteArrayOutputStream#toString method was overloaded to support Charset.
- IInputStreamFilter is now deprecated in favor of a string Predicate.
- JarDuplicates #hasVersionConflict and #getLatestVersion methods have been deprecated in favor of #areEquivalent and #getGreatest respectively.
- JarFile #getPath has been deprecated in favor of #toFile.
- JarDuplicateFinder#findJarDuplicates method was overloaded to also support collection of files.
- JarCopier now distinguish between source action and target action when resolving jar conflicts. Deprecated integer-based on-conflict strategies in favor of OnJarConflict.
- PropertyMatcher now support null field and value matchers.
- PropertyMatchers#addAll(varargs) now returns a boolean with a true value if the list has changed. Null values are now ignored.
- Regex#setFlags now returns itself.
- YearMonthDay methods affected by time-zone now always use UTC.
- YearMonthDayInterval now supports null values to represent infinity.
- XML now implements #equals and #hashCode.
- XML#setTextContent now only sets the direct text, and no longer removes other child elements.
- XML#populate and XML#validate methods no longer return a list of errors. Relies on ErrorHandler instead.
- XML#join has been deprecated.
- XML can now be saved and loaded mixing both XMLConfigurable and JAXB on the same class.
- XML#assertWriteRead now supports JAXB.
- XML now recognizes schemas for inner classes.
- Added extra safety checks in XMLUtil to prevent XXE attacks.
- Converter class renamed to GenericConverter.
- IConverter interface renamed to Converter.
- These interfaces were rename to the same name minus the "I": IEventListener, IExceptionFilter, IRetriable, IFileChangeListener, IFileVisitor, ICachedStream, IInputStreamFilter, IInputStreamListener, IMapChangeListener, IDurationUnitFormatter, IXMLFlowConsumerAdapter, IXMLFlowPredicateAdapter, IXMLConfigurable.
- XML#newXPath and XML#newXPathExpression are now deprecated in favor of XPathUtil#newXPath and XPathUtil#newXPathExpression.
- TextMatcher now considers null values as non-matching by defualt.
- ContentFamily and ContentType are now thread-safe.
- BeanUtil#diff now display hashCode for any non-null values.
- BeanUtil#visit... will no process passed collections and maps.
- EnumConverter will now match values regardless of non-alphanumeric characters they may contain.
- DateUtil now deprecated in favor of DateModel.
- Several classes previously implementing XMLConfigurable are no longer doing so, in favor of Jackson based serialization.
- CachedOutputStream now has a dispose() method and close() has no effect.

## Fixed

- Properties#loadFromXML is now null-safe.
- Fixed CollectionUtil#testRemoveNulls having opposite effect.
- Fixed ConfigurationLoader not resolving fragment ".variables" file properly when including or parsing fragments.
- Fixed CircularRange#is(...) always throwing IllegalArgumentException.
- Fixed FileUtil#tail now always reading lines properly.
- Fixed FileUtil#visitEmpty dir not always returning only empty dirs.
- Fixed WebFile#compareTo throwing a ClassCastException when comparing to another WebFile instance.
- Fixed some InputStreamConsumer#consume methods not registering listeners.
- Fixed IOUtil#consumeUntil(Reader, IntPredicate) behaving like "consumeWhile".
- Fixed DateUtil#toDate[...](Instant) not converting non UTC dates properly.

## Removed

- Removed a number of classes and methods deprecated in previous major release: *.lang.config.IXMLConfigurable, *.lang.encrypt.EncryptionXMLUtil, *.lang.StringUtil, *.lang.map.Properties#set[...], *.lang.map.PropertyMatcher#[...], ProxySettings get|setProxy[...], DurationUtil, DataUnit#convert, DataUnit@to[...]., DataUnitFormatter constructors.
- Removed #finalize method from CachedInputStream and CachedOutputStream (finalize is deprecated in the Java API since Java 9).

