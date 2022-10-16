/* Copyright 2018-2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.file;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.url.URLStreamer;

/**
 * Access a web-based file as a local file.
 * File is lazily downloaded the first time it is accessed only, unless it
 * gets deleted, in which case it will be downloaded again.
 * Use this class for simple scenarios. If more complex ones are required
 * (e.g., authentication, proxy, etc), use a different approach.
 * All this {@link Path} methods are applied on the downloaded file.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class WebFile implements Path {

    private static final Logger LOG = LoggerFactory.getLogger(WebFile.class);

    private final Path localFile;
    private final URL url;

    /**
     * Creates a web file using the OS temp directory + "webfiles" as
     * the local download location, and the URL "file" name as the local
     * file name.
     * @param url URL to download
     */
    public WebFile(String url) {
        this(toURL(url), null);
    }
    /**
     * Creates a web file using a local path to store the dowloaded file.
     * If the local file argument is <code>null</code>, invoking this method
     * is the same as invoking {@link #WebFile(URL)}
     * @param url URL to download
     * @param localFile full file path to local file destination
     */
    public WebFile(String url, Path localFile) {
        this(toURL(url), localFile);
    }

    /**
     * Creates a web file using the OS temp directory + "webfiles" as
     * the local download location, and the URL "file" name as the local
     * file name.
     * @param url URL to download
     */
    public WebFile(URL url) {
        this(url, null);
    }
    /**
     * Creates a web file using a local path to store the dowloaded file.
     * If the local file argument is <code>null</code>, invoking this method
     * is the same as invoking {@link #WebFile(URL)}
     * @param url URL to download
     * @param localFile full file path to local file destination
     */
    public WebFile(URL url, Path localFile) {
        Objects.requireNonNull(url, "'url' must not be null");
        this.url = url;
        if (localFile != null) {
            this.localFile = localFile;
        } else {
            this.localFile = new File(FileUtils.getTempDirectory(),
                    "webfiles").toPath().resolve(extractFileName(url));
        }
    }

    /**
     * Creates a web file using the given directory as the local download
     * location, and the URL "file" name as the local file name.
     * @param url URL to download
     * @param localDir full directory path to local directory destination
     * @return web file
     */
    public static WebFile create(String url, Path localDir) {
        return create(toURL(url), localDir);
    }
    /**
     * Creates a web file using the given directory as the local download
     * location, and the given name as the local file name.
     * @param url URL to download
     * @param localDir full directory path to local directory destination
     * @param localName local file name
     * @return web file
     */
    public static WebFile create(String url, Path localDir, String localName) {
        return create(toURL(url), localDir, localName);
    }
    /**
     * Creates a web file using the OS temp directory + "webfiles" as the local
     * download location, and the given name as the local file name.
     * @param url URL to download
     * @param localName local file name
     * @return web file
     */
    public static WebFile create(String url, String localName) {
        return create(toURL(url), localName);
    }

    /**
     * Creates a web file using the given directory as the local download
     * location, and the URL "file" name as the local file name.
     * @param url URL to download
     * @param localDir full directory path to local directory destination
     * @return web file
     */
    public static WebFile create(URL url, Path localDir) {
        Objects.requireNonNull(localDir, "'localDir' must not be null");
        return new WebFile(url, localDir.resolve(extractFileName(url)));
    }
    /**
     * Creates a web file using the given directory as the local download
     * location, and the given name as the local file name.
     * @param url URL to download
     * @param localDir full directory path to local directory destination
     * @param localName local file name
     * @return web file
     */
    public static WebFile create(URL url, Path localDir, String localName) {
        Objects.requireNonNull(localDir, "'localDir' must not be null");
        Objects.requireNonNull(localName, "'localName' must not be null");
        return new WebFile(url, localDir.resolve(localName));
    }
    /**
     * Creates a web file using the OS temp directory + "webfiles" as the local
     * download location, and the given name as the local file name.
     * @param url URL to download
     * @param localName local file name
     * @return web file
     */
    public static WebFile create(URL url, String localName) {
        return new WebFile(url, new File(FileUtils.getTempDirectory(),
                "webfiles").toPath().resolve(localName));
    }

    public URL getUrl() {
        return url;
    }

    private static String extractFileName(URL url) {
        String localName = StringUtils.trimToNull(url.getPath());
        if (localName == null) {
            throw new IllegalArgumentException(
                    "URL does not contain a path.");
        }
        return localName.replaceFirst("^.*[\\/\\\\](.*)$", "$1");
    }

    synchronized Path getResolvedFile() {
        if (localFile.toFile().exists()) {
            LOG.debug("Web file already downloaded: {}", localFile);
        } else {
            download(url, localFile);
        }
        return localFile;
    }

    private static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Not a valid URL: " + url, e);
        }
    }

    protected void download(URL url, Path localFile) {
        LOG.debug("Downloading web file from \"{}\" to \"{}\"",
                url, localFile);
        URL targetURL = url;
        try {
            if (targetURL.toString().contains(".zip!")) {
                targetURL = new URL("jar:" + url);
            }
            FileUtils.copyInputStreamToFile(
                    URLStreamer.stream(targetURL), localFile.toFile());
            LOG.debug("Web file downloaded from \"{}\" to \"{}\"",
                    url, localFile);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not download web file: " + url, e);
        }
    }

    //--- Decorating methods ---------------------------------------------------

    @Override
    public FileSystem getFileSystem() {
        return getResolvedFile().getFileSystem();
    }
    @Override
    public boolean isAbsolute() {
        return getResolvedFile().isAbsolute();
    }
    @Override
    public Path getRoot() {
        return getResolvedFile().getRoot();
    }
    @Override
    public Path getFileName() {
        return getResolvedFile().getFileName();
    }
    @Override
    public Path getParent() {
        return getResolvedFile().getParent();
    }
    @Override
    public int getNameCount() {
        return getResolvedFile().getNameCount();
    }
    @Override
    public Path getName(int index) {
        return getResolvedFile().getName(index);
    }
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return getResolvedFile().subpath(beginIndex, endIndex);
    }
    @Override
    public boolean startsWith(Path other) {
        return getResolvedFile().startsWith(other);
    }
    @Override
    public boolean startsWith(String other) {
        return getResolvedFile().startsWith(other);
    }
    @Override
    public boolean endsWith(Path other) {
        return getResolvedFile().endsWith(other);
    }
    @Override
    public boolean endsWith(String other) {
        return getResolvedFile().endsWith(other);
    }
    @Override
    public Path normalize() {
        return getResolvedFile().normalize();
    }
    @Override
    public Path resolve(Path other) {
        return getResolvedFile().resolve(other);
    }
    @Override
    public Path resolve(String other) {
        return getResolvedFile().resolve(other);
    }
    @Override
    public Path resolveSibling(Path other) {
        return getResolvedFile().resolveSibling(other);
    }
    @Override
    public Path resolveSibling(String other) {
        return getResolvedFile().resolveSibling(other);
    }
    @Override
    public Path relativize(Path other) {
        return getResolvedFile().relativize(other);
    }
    @Override
    public URI toUri() {
        return getResolvedFile().toUri();
    }
    @Override
    public Path toAbsolutePath() {
        return getResolvedFile().toAbsolutePath();
    }
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return getResolvedFile().toRealPath(options);
    }
    @Override
    public File toFile() {
        return getResolvedFile().toFile();
    }
    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events,
            Modifier... modifiers) throws IOException {
        return getResolvedFile().register(watcher, events, modifiers);
    }
    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events)
            throws IOException {
        return getResolvedFile().register(watcher, events);
    }
    @Override
    public Iterator<Path> iterator() {
        return getResolvedFile().iterator();
    }

    @Override
    public String toString() {
        return localFile.toString();
    }
    @Override
    public int hashCode() {
        return localFile.hashCode();
    }

    // JDK assumes a specific implementation of Path, so for the following,
    // if "other" is a WebFile, use its resolved file to avoid cast exception.
    @Override
    public int compareTo(Path other) {
        return getResolvedFile().compareTo(nativePath(other));
    }

    @Override
    public boolean equals(Object obj) {
        return localFile.equals(nativePath(obj));
    }

    @SuppressWarnings("unchecked")
    private <T> T nativePath(T path) {
        return path instanceof WebFile
                ? (T) ((WebFile) path).getResolvedFile() : path;
    }
}
