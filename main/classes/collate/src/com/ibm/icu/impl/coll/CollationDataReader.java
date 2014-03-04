/*
*******************************************************************************
* Copyright (C) 2013-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* CollationDataReader.java, ported from collationdatareader.h/.cpp
*
* C++ version created on: 2013feb07
* created by: Markus W. Scherer
*/

package com.ibm.icu.impl.coll;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.Trie2_32;
import com.ibm.icu.impl.USerializedSet;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Collation binary data reader.
 */
final class CollationDataReader /* all static */ {
    // The following constants are also copied into source/common/ucol_swp.cpp.
    // Keep them in sync!
    /**
     * Number of int indexes.
     *
     * Can be 2 if there are only options.
     * Can be 7 or 8 if there are only options and a script reordering.
     * The loader treats any index>=indexes[IX_INDEXES_LENGTH] as 0.
     */
    static final int IX_INDEXES_LENGTH = 0;
    /**
     * Bits 31..24: numericPrimary, for numeric collation
     *      23..16: fast Latin format version (0 = no fast Latin table)
     *      15.. 0: options bit set
     */
    static final int IX_OPTIONS = 1;
    static final int IX_RESERVED2 = 2;
    static final int IX_RESERVED3 = 3;

    /** Array offset to Jamo CE32s in ce32s[], or <0 if none. */
    static final int IX_JAMO_CE32S_START = 4;

    // Byte offsets from the start of the data, after the generic header.
    // The indexes[] are at byte offset 0, other data follows.
    // Each data item is aligned properly.
    // The data items should be in descending order of unit size,
    // to minimize the need for padding.
    // Each item's byte length is given by the difference between its offset and
    // the next index/offset value.
    /** Byte offset to int reorderCodes[]. */
    static final int IX_REORDER_CODES_OFFSET = 5;
    /**
     * Byte offset to uint8_t reorderTable[].
     * Empty table if <256 bytes (padding only).
     * Otherwise 256 bytes or more (with padding).
     */
    static final int IX_REORDER_TABLE_OFFSET = 6;
    /** Byte offset to the collation trie. Its length is a multiple of 8 bytes. */
    static final int IX_TRIE_OFFSET = 7;

    static final int IX_RESERVED8_OFFSET = 8;
    /** Byte offset to long ces[]. */
    static final int IX_CES_OFFSET = 9;
    static final int IX_RESERVED10_OFFSET = 10;
    /** Byte offset to int ce32s[]. */
    static final int IX_CE32S_OFFSET = 11;

    /** Byte offset to uint32_t rootElements[]. */
    static final int IX_ROOT_ELEMENTS_OFFSET = 12;
    /** Byte offset to UChar *contexts[]. */
    static final int IX_CONTEXTS_OFFSET = 13;
    /** Byte offset to char [] with serialized unsafeBackwardSet. */
    static final int IX_UNSAFE_BWD_OFFSET = 14;
    /** Byte offset to char fastLatinTable[]. */
    static final int IX_FAST_LATIN_TABLE_OFFSET = 15;

    /** Byte offset to char scripts[]. */
    static final int IX_SCRIPTS_OFFSET = 16;
    /**
     * Byte offset to boolean compressibleBytes[].
     * Empty table if <256 bytes (padding only).
     * Otherwise 256 bytes or more (with padding).
     */
    static final int IX_COMPRESSIBLE_BYTES_OFFSET = 17;
    static final int IX_RESERVED18_OFFSET = 18;
    static final int IX_TOTAL_SIZE = 19;

    static void read(CollationTailoring base, InputStream inBytes,
                     CollationTailoring tailoring) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inBytes);
        tailoring.version = ICUBinary.readHeaderAndDataVersion(bis, DATA_FORMAT, IS_ACCEPTABLE);
        if(base != null && base.getUCAVersion() != tailoring.getUCAVersion()) {
            throw new RuntimeException("Tailoring UCA version differs from base data UCA version");
        }

        DataInputStream ds = new DataInputStream(bis);
        int indexesLength = ds.readInt();  // inIndexes[IX_INDEXES_LENGTH]
        if(indexesLength < 2) {
            throw new RuntimeException("not enough indexes");
        }
        int[] inIndexes = new int[IX_TOTAL_SIZE + 1];
        inIndexes[0] = indexesLength;
        for(int i = 1; i < indexesLength && i < inIndexes.length; ++i) {
            inIndexes[i] = ds.readInt();
        }
        for(int i = indexesLength; i < inIndexes.length; ++i) {
            inIndexes[i] = -1;
        }
        if(indexesLength > inIndexes.length) {
            ds.skipBytes((indexesLength - inIndexes.length) * 4);
        }

        // Assume that the tailoring data is in initial state,
        // with null pointers and 0 lengths.

        // Set pointers to non-empty data parts.
        // Do this in order of their byte offsets. (Should help porting to Java.)

        int index;  // one of the indexes[] slots
        int offset;  // byte offset for the index part
        int length;  // number of bytes in the index part

        CollationData baseData = base == null ? null : base.data;
        int[] reorderCodes;
        index = IX_REORDER_CODES_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 4) {
            if(baseData == null) {
                // We assume for collation settings that
                // the base data does not have a reordering.
                throw new RuntimeException("Collation base data must not reorder scripts");
            }
            reorderCodes = new int[length / 4];
            for(int i = 0; i < length / 4; ++i) {
                reorderCodes[i] = ds.readInt();
            }
            length &= 3;
        } else {
            reorderCodes = new int[0];
        }
        ds.skipBytes(length);

        // There should be a reorder table only if there are reorder codes.
        // However, when there are reorder codes the reorder table may be omitted to reduce
        // the data size.
        byte[] reorderTable = null;
        index = IX_REORDER_TABLE_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 256) {
            if(reorderCodes.length == 0) {
                throw new RuntimeException("Reordering table without reordering codes");
            }
            reorderTable = new byte[256];
            ds.readFully(reorderTable);
            length -= 256;
        } else {
            // If we have reorder codes, then build the reorderTable at the end,
            // when the CollationData is otherwise complete.
        }
        ds.skipBytes(length);

        if(baseData != null && baseData.numericPrimary != (inIndexes[IX_OPTIONS] & 0xff000000L)) {
            throw new RuntimeException("Tailoring numeric primary weight differs from base data");
        }
        CollationData data = null;  // Remains null if there are no mappings.

        index = IX_TRIE_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 8) {
            tailoring.ensureOwnedData();
            data = tailoring.ownedData;
            data.base = baseData;
            data.numericPrimary = inIndexes[IX_OPTIONS] & 0xff000000L;
            data.trie = tailoring.trie = Trie2_32.createFromSerialized(ds);
            int trieLength = data.trie.getSerializedLength();
            if(trieLength > length) {
                throw new RuntimeException("Not enough bytes for the mappings trie");  // No mappings.
            }
            length -= trieLength;
        } else if(baseData != null) {
            // Use the base data. Only the settings are tailored.
            tailoring.data = baseData;
        } else {
            throw new RuntimeException("Missing collation data mappings");  // No mappings.
        }
        ds.skipBytes(length);

        index = IX_RESERVED8_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        ds.skipBytes(length);

        index = IX_CES_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 8) {
            if(data == null) {
                throw new RuntimeException("Tailored ces without tailored trie");
            }
            data.ces = new long[length / 8];
            for(int i = 0; i < length / 8; ++i) {
                data.ces[i] = ds.readLong();
            }
            length &= 7;
        }
        ds.skipBytes(length);

        index = IX_RESERVED10_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        ds.skipBytes(length);

        index = IX_CE32S_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 4) {
            if(data == null) {
                throw new RuntimeException("Tailored ce32s without tailored trie");
            }
            data.ce32s = new int[length / 4];
            for(int i = 0; i < length / 4; ++i) {
                data.ce32s[i] = ds.readInt();
            }
            length &= 3;
        }
        ds.skipBytes(length);

        int jamoCE32sStart = inIndexes[IX_JAMO_CE32S_START];
        if(jamoCE32sStart >= 0) {
            if(data == null || data.ce32s == null) {
                throw new RuntimeException("JamoCE32sStart index into non-existent ce32s[]");
            }
            data.jamoCE32s = new int[CollationData.JAMO_CE32S_LENGTH];
            System.arraycopy(data.ce32s, jamoCE32sStart, data.jamoCE32s, 0, CollationData.JAMO_CE32S_LENGTH);
        } else if(data == null) {
            // Nothing to do.
        } else if(baseData != null) {
            data.jamoCE32s = baseData.jamoCE32s;
        } else {
            throw new RuntimeException("Missing Jamo CE32s for Hangul processing");
        }

        index = IX_ROOT_ELEMENTS_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 4) {
            int rootElementsLength = length / 4;
            if(data == null) {
                throw new RuntimeException("Root elements but no mappings");
            }
            if(rootElementsLength <= CollationRootElements.IX_SEC_TER_BOUNDARIES) {
                throw new RuntimeException("Root elements array too short");
            }
            data.rootElements = new long[rootElementsLength];
            for(int i = 0; i < rootElementsLength; ++i) {
                data.rootElements[i] = ds.readInt() & 0xffffffffL;  // unsigned int -> long
            }
            long commonSecTer = data.rootElements[CollationRootElements.IX_COMMON_SEC_AND_TER_CE];
            if(commonSecTer != Collation.COMMON_SEC_AND_TER_CE) {
                throw new RuntimeException("Common sec/ter weights in base data differ from the hardcoded value");
            }
            long secTerBoundaries = data.rootElements[CollationRootElements.IX_SEC_TER_BOUNDARIES];
            if((secTerBoundaries >>> 24) < CollationKeys.SEC_COMMON_HIGH) {
                // [fixed last secondary common byte] is too low,
                // and secondary weights would collide with compressed common secondaries.
                throw new RuntimeException("[fixed last secondary common byte] is too low");
            }
            length &= 3;
        }
        ds.skipBytes(length);

        index = IX_CONTEXTS_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 2) {
            if(data == null) {
                throw new RuntimeException("Tailored contexts without tailored trie");
            }
            StringBuilder sb = new StringBuilder(length / 2);
            for(int i = 0; i < length / 2; ++i) {
                sb.append(ds.readChar());
            }
            data.contexts = sb.toString();
            length &= 1;
        }
        ds.skipBytes(length);

        index = IX_UNSAFE_BWD_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 2) {
            if(data == null) {
                throw new RuntimeException("Unsafe-backward-set but no mappings");
            }
            if(baseData == null) {
                // Create the unsafe-backward set for the root collator.
                // Include all non-zero combining marks and trail surrogates.
                // We do this at load time, rather than at build time,
                // to simplify Unicode version bootstrapping:
                // The root data builder only needs the new FractionalUCA.txt data,
                // but it need not be built with a version of ICU already updated to
                // the corresponding new Unicode Character Database.
                //
                // The following is an optimized version of
                // new UnicodeSet("[[:^lccc=0:][\\udc00-\\udfff]]").
                // It is faster and requires fewer code dependencies.
                tailoring.unsafeBackwardSet = new UnicodeSet(0xdc00, 0xdfff);  // trail surrogates
                data.nfcImpl.addLcccChars(tailoring.unsafeBackwardSet);
            } else {
                // Clone the root collator's set contents.
                tailoring.unsafeBackwardSet = baseData.unsafeBackwardSet.cloneAsThawed();
            }
            // Add the ranges from the data file to the unsafe-backward set.
            USerializedSet sset = new USerializedSet();
            char[] unsafeData = new char[length / 2];
            for(int i = 0; i < length / 2; ++i) {
                unsafeData[i] = ds.readChar();
            }
            length &= 1;
            sset.getSet(unsafeData, 0);
            int count = sset.countRanges();
            int[] range = new int[2];
            for(int i = 0; i < count; ++i) {
                sset.getRange(i, range);
                tailoring.unsafeBackwardSet.add(range[0], range[1]);
            }
            // Mark each lead surrogate as "unsafe"
            // if any of its 1024 associated supplementary code points is "unsafe".
            int c = 0x10000;
            for(int lead = 0xd800; lead < 0xdc00; ++lead, c += 0x400) {
                if(!tailoring.unsafeBackwardSet.containsNone(c, c + 0x3ff)) {
                    tailoring.unsafeBackwardSet.add(lead);
                }
            }
            tailoring.unsafeBackwardSet.freeze();
            data.unsafeBackwardSet = tailoring.unsafeBackwardSet;
        } else if(data == null) {
            // Nothing to do.
        } else if(baseData != null) {
            // No tailoring-specific data: Alias the root collator's set.
            data.unsafeBackwardSet = baseData.unsafeBackwardSet;
        } else {
            throw new RuntimeException("Missing unsafe-backward-set");
        }
        ds.skipBytes(length);

        // If the fast Latin format version is different,
        // or the version is set to 0 for "no fast Latin table",
        // then just always use the normal string comparison path.
        index = IX_FAST_LATIN_TABLE_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(data != null) {
            data.fastLatinTable = null;
            data.fastLatinTableHeader = null;
            if(((inIndexes[IX_OPTIONS] >> 16) & 0xff) == CollationFastLatin.VERSION) {
                if(length >= 2) {
                    char header0 = ds.readChar();
                    int headerLength = header0 & 0xff;
                    data.fastLatinTableHeader = new char[headerLength];
                    data.fastLatinTableHeader[0] = header0;
                    for(int i = 1; i < headerLength; ++i) {
                        data.fastLatinTableHeader[i] = ds.readChar();
                    }
                    int tableLength = length / 2 - headerLength;
                    data.fastLatinTable = new char[tableLength];
                    for(int i = 0; i < tableLength; ++i) {
                        data.fastLatinTable[i] = ds.readChar();
                    }
                    length &= 1;
                    if((header0 >> 8) != CollationFastLatin.VERSION) {
                        throw new RuntimeException("Fast-Latin table version differs from version in data header");
                    }
                } else if(baseData != null) {
                    data.fastLatinTable = baseData.fastLatinTable;
                    data.fastLatinTableHeader = baseData.fastLatinTableHeader;
                }
            }
        }
        ds.skipBytes(length);

        index = IX_SCRIPTS_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 2) {
            if(data == null) {
                throw new RuntimeException("Script order data but no mappings");
            }
            data.scripts = new char[length / 2];
            for(int i = 0; i < length / 2; ++i) {
                data.scripts[i] = ds.readChar();
            }
            length &= 1;
        } else if(data == null) {
            // Nothing to do.
        } else if(baseData != null) {
            data.scripts = baseData.scripts;
        }
        ds.skipBytes(length);

        index = IX_COMPRESSIBLE_BYTES_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        if(length >= 256) {
            if(data == null) {
                throw new RuntimeException("Data for compressible primary lead bytes but no mappings");
            }
            data.compressibleBytes = new boolean[256];
            for(int i = 0; i < 256; ++i) {
                data.compressibleBytes[i] = ds.readBoolean();
            }
            length -= 256;
        } else if(data == null) {
            // Nothing to do.
        } else if(baseData != null) {
            data.compressibleBytes = baseData.compressibleBytes;
        } else {
            throw new RuntimeException("Missing data for compressible primary lead bytes");
        }
        ds.skipBytes(length);

        index = IX_RESERVED18_OFFSET;
        offset = inIndexes[index];
        length = inIndexes[index + 1] - offset;
        ds.skipBytes(length);

        ds.close();

        CollationSettings ts = tailoring.settings.readOnly();
        int options = inIndexes[IX_OPTIONS] & 0xffff;
        char[] fastLatinPrimaries = new char[CollationFastLatin.LATIN_LIMIT];
        int fastLatinOptions = CollationFastLatin.getOptions(
                tailoring.data, ts, fastLatinPrimaries);
        if(options == ts.options && ts.variableTop != 0 &&
                Arrays.equals(reorderCodes, ts.reorderCodes) &&
                fastLatinOptions == ts.fastLatinOptions &&
                (fastLatinOptions < 0 ||
                        Arrays.equals(fastLatinPrimaries, ts.fastLatinPrimaries))) {
            return;
        }

        CollationSettings settings = tailoring.settings.copyOnWrite();
        settings.options = options;
        // Set variableTop from options and scripts data.
        settings.variableTop = tailoring.data.getLastPrimaryForGroup(
                Collator.ReorderCodes.FIRST + settings.getMaxVariable());
        if(settings.variableTop == 0) {
            throw new RuntimeException("The maxVariable could not be mapped to a variableTop");
        }

        if(reorderCodes.length == 0 || reorderTable != null) {
            settings.setReordering(reorderCodes, reorderTable);
        } else {
            byte[] table = new byte[256];
            baseData.makeReorderTable(reorderCodes, table);
            settings.setReordering(reorderCodes, table);
        }

        settings.fastLatinOptions = CollationFastLatin.getOptions(
            tailoring.data, settings,
            settings.fastLatinPrimaries);
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        // @Override when we switch to Java 6
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0] == 4;
        }
    }
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    private static final byte DATA_FORMAT[] = { 0x55, 0x43, 0x6f, 0x6c  };  // "UCol"

    private CollationDataReader() {}  // no constructor
}

/*
 * Format of collation data (ucadata.icu, binary data in coll/ *.res files):
 * See ICU4C source/common/collationdatareader.h.
 */
