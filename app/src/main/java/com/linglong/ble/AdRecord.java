package com.linglong.ble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author : kui
 * date   : 2018/12/4  14:07
 * desc   :
 * version: 1.0
 */
public class AdRecord {
    private final int mLength;
    private final int mType;
    private final byte[] mData;

    public AdRecord(int length, int type, byte[] data) {
        mLength = length;
        mType = type;
        mData = data;
    }

    public int getLength() {
        return mLength;
    }

    public int getType() {
        return mType;
    }

    public byte[] getData() {
        return mData;
    }

    public static List<AdRecord> getAdRecord(byte[] scanRecord) {
        List<AdRecord> records = new ArrayList<AdRecord>();
        int index = 0;
        // scanRecord数据解析
        while (index < scanRecord.length) {
            final int length = scanRecord[index++];
            if (length == 0) {
                break;
            }
            final int type = getIntFromByte(scanRecord[index]);
            if (type == 0) {
                break;
            }
            final byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
            records.add(new AdRecord(length, type, data));
            index += length;
        }
        return records;
    }

    /**
     * Converts a byte to an int, preserving the sign.
     * <p>
     * For example, FF will be converted to 255 and not -1.
     *
     * @param bite the bite
     * @return the int from byte
     */
    public static int getIntFromByte(final byte bite) {
        return Integer.valueOf(bite & 0xFF);
    }

}
