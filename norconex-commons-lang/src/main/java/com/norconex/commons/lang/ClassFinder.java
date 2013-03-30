package com.norconex.commons.lang;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Utility class for finding names of classes implementing an interface or class
 * in directories or JAR files. In order to find if a class is potential
 * candidate, it is "loaded" first, but into a temporary class loader.  
 * Still, if it is important to you that classes do not get loaded, you can 
 * use other approaches, such as byte-code scanning.   See 
 * <a href="http://commons.apache.org/sandbox/classscan/">Apache ClassScan</a>
 * sandbox project for code that does that. 
 * 
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public class ClassFinder {

    private static final Logger LOG = Logger.getLogger(ClassFinder.class);

    /**
     * Finds the names of all classes implementing the super class.
     * This method is null-safe.  If no classes are found, 
     * an empty list will be returned.
     * @param files directories and JARs to scan for classes
     * @param superClass the class from which to find implementors
     * @return list of class names
     */
    public static List<String> findImplementors(
            List<File> files, Class<?> superClass) {
        List<String> classes = new ArrayList<String>();
        for (File file : files) {
            classes.addAll(ClassFinder.findImplementors(
                            file, superClass));
        }
        return classes;
    }

    /**
     * Finds the names of all classes implementing the super class.
     * This method is null-safe.  If no classes are found, 
     * an empty list will be returned.  
     * If the file is null or does not exists, or if it is not a JAR or 
     * directory, an empty string list will be returned.
     * @param file directory or JAR to scan for classes
     * @param superClass the class from which to find implementors
     * @return list of class names
     */
    public static List<String> findImplementors(
            File file, Class<?> superClass) {
        if (file == null || !file.exists()) {
            LOG.warn("Trying to find implementing classes from a null or "
                   + "non-existant file: " + file);
            return new ArrayList<String>();
        }
        if (file.isDirectory()) {
            return findImplementingDirectoryClasses(
                    new File(file.getAbsolutePath() + "/"), superClass);
        }
        if (file.getName().endsWith(".jar")) {
            return findImplementingJarClasses(file, superClass);
        }
        LOG.warn("File not a JAR and not a directory.");
        return new ArrayList<String>();
    }
    
    
    private static List<String> findImplementingDirectoryClasses(
            File dir, Class<?> superClass) {
        
        List<String> classes = new ArrayList<String>();
        String dirPath = dir.getAbsolutePath();

        Collection<File> classFiles = FileUtils.listFiles(
                dir, new String[] {"class"}, true);
        ClassLoader loader = getClassLoader(dir);
        if (loader == null) {
            return classes;
        }
        
        for (File classFile : classFiles) {
            String filePath = classFile.getAbsolutePath();
            String className = StringUtils.removeStart(filePath, dirPath);
            className = resolveName(loader, className, superClass);
            if (className != null) {
                classes.add(className);
            }
        }
        return classes;
    }
    private static List<String> findImplementingJarClasses(
            File jarFile, Class<?> superClass) {
        
        List<String> classes = new ArrayList<String>();
        
        ClassLoader loader = getClassLoader(jarFile);
        if (loader == null) {
            return classes;
        }
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile); 
        } catch (IOException e) {
            LOG.error("Invalid JAR: " + jarFile, e);
            return classes;
        }
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String className = entry.getName();
            className = resolveName(loader, className, superClass);
            if (className != null) {
                classes.add(className);
            }
        }
        return classes;
    }

    private static ClassLoader getClassLoader(File url) {
        try {
            URL dirURL = url.toURI().toURL();
            return new URLClassLoader(
                    new URL[] {dirURL},
                    ClassFinder.class.getClassLoader());
        } catch (MalformedURLException e) {
            LOG.error("Invalid classpath: " + url, e);
            return null;
        }
    }
    
    private static String resolveName(
            ClassLoader loader, String rawName, Class<?> superClass) {
        String className = rawName;
        if (!rawName.endsWith(".class") || className.contains("$")) {
            return null;
        }
        className = className.replaceAll("[\\\\/]", ".");
        className = StringUtils.removeStart(className, ".");
        className = StringUtils.removeEnd(className, ".class");

        try {
            Class<?> clazz = loader.loadClass(className);
            if (superClass.isAssignableFrom(clazz)) {
                return clazz.getName();
            }
        } catch (ClassNotFoundException e) {
            LOG.error("Invalid class: " + className, e);
        } catch (NoClassDefFoundError e) {
            LOG.debug("Invalid class: " + className, e);
        }
        return null;
    }
    

}
