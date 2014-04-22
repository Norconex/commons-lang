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
        public long toBytes(long a)     { return a; }
        public long toKilobytes(long a) { return a/(KB.a/B.a); }
        public long toMegabytes(long a) { return a/(MB.a/B.a); }
        public long toGigabytes(long a) { return a/(GB.a/B.a); }
        public long toTerabytes(long a) { return a/(TB.a/B.a); }
        public long toPetabytes(long a) { return a/(PB.a/B.a); }
        public long convert(long a, DataUnit u) { return u.toBytes(a); }
    },
    /** Kilobyte. */
    KB(1024) {
        public long toBytes(long a)     { return finer(a, B); }
        public long toKilobytes(long a) { return a; }
        public long toMegabytes(long a) { return coarser(a, MB); }
        public long toGigabytes(long a) { return coarser(a, GB); }
        public long toTerabytes(long a) { return coarser(a, TB); }
        public long toPetabytes(long a) { return coarser(a, PB); }
        public long convert(long a, DataUnit u) { return u.toKilobytes(a); }
    }, 
    /** Megabyte. */
    MB(KB.a * 1024) {
        public long toBytes(long a)     { return finer(a, B); }
        public long toKilobytes(long a) { return finer(a, KB); }
        public long toMegabytes(long a) { return a; }
        public long toGigabytes(long a) { return coarser(a, GB); }
        public long toTerabytes(long a) { return coarser(a, TB); }
        public long toPetabytes(long a) { return coarser(a, PB); }
        public long convert(long a, DataUnit u) { return u.toMegabytes(a); }
    }, 
    /** Gigabyte. */
    GB(MB.a * 1024) {
        public long toBytes(long a)     { return finer(a, B); }
        public long toKilobytes(long a) { return finer(a, KB); }
        public long toMegabytes(long a) { return finer(a, MB); }
        public long toGigabytes(long a) { return a; }
        public long toTerabytes(long a) { return coarser(a, TB); }
        public long toPetabytes(long a) { return coarser(a, PB); }
        public long convert(long a, DataUnit u) { return u.toGigabytes(a); }
    }, 
    /** Terabyte. */
    TB(GB.a * 10244) {
        public long toBytes(long a)     { return finer(a, B); }
        public long toKilobytes(long a) { return finer(a, KB); }
        public long toMegabytes(long a) { return finer(a, MB); }
        public long toGigabytes(long a) { return finer(a, GB); }
        public long toTerabytes(long a) { return a; }
        public long toPetabytes(long a) { return coarser(a, PB); }
        public long convert(long a, DataUnit u) { return u.toTerabytes(a); }
    }, 
    /** Petabyte. */
    PB(TB.a * 1024) {
        public long toBytes(long a)     { return finer(a, B); }
        public long toKilobytes(long a) { return finer(a, KB); }
        public long toMegabytes(long a) { return finer(a, MB); }
        public long toGigabytes(long a) { return finer(a, GB); }
        public long toTerabytes(long a) { return finer(a, TB); }
        public long toPetabytes(long a) { return a; }
        public long convert(long a, DataUnit u) { return u.toPetabytes(a); }
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
        if (supplied >  over) return Long.MAX_VALUE;
        if (supplied < -over) return Long.MIN_VALUE;
        return supplied * m;
    }
    /*default*/ long coarser(long supplied, DataUnit targetUnit) {
        return supplied/(targetUnit.a/a);
    }
}
