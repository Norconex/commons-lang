/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.commons.lang.unit;

/**
 * A <tt>DataUnit</tt> represents data amounts at a given unit of
 * granularity and provides utility methods to convert across units.
 * A <tt>DataUnit</tt> does not maintain data amount information, but only
 * helps organize and use data representations that may be maintained
 * separately across various contexts. Unit values are defined as follow:
 * <ul>
 *   <li>1 B (byte) = 1 B (the smallest supported unit).
 *   <li>1 KB (kilobyte)  = 1024 B</li>  
 *   <li>1 MB (megabyte)  = 1024 KB</li>  
 *   <li>1 GB (gigabyte)  = 1024 MB</li>  
 *   <li>1 TB (terabyte)  = 1024 GB</li>  
 *   <li>1 PB (petabyte)  = 1024 TB</li>  
  
 * </ul>
 *
 * <p>A <tt>DataUnit</tt> is mainly used to inform data-based methods
 * how a given data parameter should be interpreted. For example:
 *
 * <pre>
 *   // how many kilobytes in a gigabyte amount
 *   long kb = DataUnit.GB.toKilobytes(3); // results = 3072
 *   
 *   // how many megabyte in a kilobyte amount
 *   long mb = DataUnit.KB.toMegabytes(2500); // results = 2
 *   
 *   // convert with dynamic units
 *   DataUnit targetUnit = // any unit
 *   long value = 1024;
 *   DataUnit kb = DataUnit.KB;
 *   long convertedValue = targetUnit.convert(1024, kb);
 *   
 * </pre>
 *
 * @since 1.4.0
 * @author Pascal Essiembre
 * @see DataUnitFormatter
 */
public enum DataUnit {

    /** Byte. */
    B(1) {
        @Override public long toBytes(long a)     { return a; }
        @Override public long toKilobytes(long a) { return a/(KB.a/B.a); }
        @Override public long toMegabytes(long a) { return a/(MB.a/B.a); }
        @Override public long toGigabytes(long a) { return a/(GB.a/B.a); }
        @Override public long toTerabytes(long a) { return a/(TB.a/B.a); }
        @Override public long toPetabytes(long a) { return a/(PB.a/B.a); }
        @Override public long convert(long a, DataUnit u) { return u.toBytes(a); }
    },
    /** Kilobyte. */
    KB(1024) {
        @Override public long toBytes(long a)     { return finer(a, B); }
        @Override public long toKilobytes(long a) { return a; }
        @Override public long toMegabytes(long a) { return coarser(a, MB); }
        @Override public long toGigabytes(long a) { return coarser(a, GB); }
        @Override public long toTerabytes(long a) { return coarser(a, TB); }
        @Override public long toPetabytes(long a) { return coarser(a, PB); }
        @Override public long convert(long a, DataUnit u) { return u.toKilobytes(a); }
    }, 
    /** Megabyte. */
    MB(KB.a * 1024) {
        @Override public long toBytes(long a)     { return finer(a, B); }
        @Override public long toKilobytes(long a) { return finer(a, KB); }
        @Override public long toMegabytes(long a) { return a; }
        @Override public long toGigabytes(long a) { return coarser(a, GB); }
        @Override public long toTerabytes(long a) { return coarser(a, TB); }
        @Override public long toPetabytes(long a) { return coarser(a, PB); }
        @Override public long convert(long a, DataUnit u) { return u.toMegabytes(a); }
    }, 
    /** Gigabyte. */
    GB(MB.a * 1024) {
        @Override public long toBytes(long a)     { return finer(a, B); }
        @Override public long toKilobytes(long a) { return finer(a, KB); }
        @Override public long toMegabytes(long a) { return finer(a, MB); }
        @Override public long toGigabytes(long a) { return a; }
        @Override public long toTerabytes(long a) { return coarser(a, TB); }
        @Override public long toPetabytes(long a) { return coarser(a, PB); }
        @Override public long convert(long a, DataUnit u) { return u.toGigabytes(a); }
    }, 
    /** Terabyte. */
    TB(GB.a * 10244) {
        @Override public long toBytes(long a)     { return finer(a, B); }
        @Override public long toKilobytes(long a) { return finer(a, KB); }
        @Override public long toMegabytes(long a) { return finer(a, MB); }
        @Override public long toGigabytes(long a) { return finer(a, GB); }
        @Override public long toTerabytes(long a) { return a; }
        @Override public long toPetabytes(long a) { return coarser(a, PB); }
        @Override public long convert(long a, DataUnit u) { return u.toTerabytes(a); }
    }, 
    /** Petabyte. */
    PB(TB.a * 1024) {
        @Override public long toBytes(long a)     { return finer(a, B); }
        @Override public long toKilobytes(long a) { return finer(a, KB); }
        @Override public long toMegabytes(long a) { return finer(a, MB); }
        @Override public long toGigabytes(long a) { return finer(a, GB); }
        @Override public long toTerabytes(long a) { return finer(a, TB); }
        @Override public long toPetabytes(long a) { return a; }
        @Override public long convert(long a, DataUnit u) { return u.toPetabytes(a); }
    };
    
    private static final long MAX = Long.MAX_VALUE;
    private long a;
    DataUnit(long byteAmount) {
        this.a = byteAmount;
    }
    public long toBytes(long amount) {
        throw new AbstractMethodError();
    }
    public long toKilobytes(long amount) {
        throw new AbstractMethodError();
    }
    public long toMegabytes(long amount) {
        throw new AbstractMethodError();
    }
    public long toGigabytes(long amount) {
        throw new AbstractMethodError();
    }
    public long toTerabytes(long amount) {
        throw new AbstractMethodError();
    }
    public long toPetabytes(long amount) {
        throw new AbstractMethodError();
    }
    /**
     * Converts a given source data amount and type to this type.
     * @param sourceAmount source data amount
     * @param sourceUnit source data unit
     * @return converted value
     */
    public long convert(long sourceAmount, DataUnit sourceUnit) {
        throw new AbstractMethodError();
    }
    
    /*default*/ long finer(long supplied, DataUnit targetUnit) {
        long m = a / targetUnit.a;
        long over = MAX/m;
        if (supplied >  over) {
            return Long.MAX_VALUE;
        }
        if (supplied < -over) {
            return Long.MIN_VALUE;
        }
        return supplied * m;
    }
    /*default*/ long coarser(long supplied, DataUnit targetUnit) {
        return supplied/(targetUnit.a/a);
    }
}
