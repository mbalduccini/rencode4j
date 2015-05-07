/**
 * 
 */
package com.github.jksiezni.rencode;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static com.github.jksiezni.rencode.Rencode.*;

/**
 * @author Jakub Księżniak
 *
 */
public class RencodeOutputStream extends FilterOutputStream implements DataOutput {

	public RencodeOutputStream(OutputStream out) {
		super(out);
	}

	public void writeBoolean(boolean v) throws IOException {
		write(v ? CHR_TRUE : CHR_FALSE);
	}

	public void writeByte(int v) throws IOException {
		if (0 <= v && v < INT_POS_FIXED_COUNT) {
			write(INT_POS_FIXED_START + v);
		} else if (-INT_NEG_FIXED_COUNT <= v && v < 0) {
			write(INT_NEG_FIXED_START - 1 - v);
		} else {
			write(CHR_INT1);
			write(v);
		}
	}

	public void writeShort(int v) throws IOException {
		if (Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE) {
			writeByte(v);
		} else {
			write(CHR_INT2);
			write((v >>> 8) & 0xFF);
			write((v >>> 0) & 0xFF);
		}
	}

	public void writeChar(int v) throws IOException {
		writeShort(v);
	}

	public void writeInt(int v) throws IOException {
		if (Short.MIN_VALUE <= v && v <= Short.MAX_VALUE) {
			writeShort(v);
		} else {
			write(CHR_INT4);
			write((v >>> 24) & 0xFF);
			write((v >>> 16) & 0xFF);
			write((v >>> 8) & 0xFF);
			write((v >>> 0) & 0xFF);
		}
	}

	private final byte buffer[] = new byte[8];

	public void writeLong(long v) throws IOException {
		if (Integer.MIN_VALUE <= v && v <= Integer.MAX_VALUE) {
			writeInt((int) v);
		} else {
			write(CHR_INT8);
			buffer[0] = (byte) (v >>> 56);
			buffer[1] = (byte) (v >>> 48);
			buffer[2] = (byte) (v >>> 40);
			buffer[3] = (byte) (v >>> 32);
			buffer[4] = (byte) (v >>> 24);
			buffer[5] = (byte) (v >>> 16);
			buffer[6] = (byte) (v >>> 8);
			buffer[7] = (byte) (v >>> 0);
			write(buffer, 0, 8);
		}
	}

	public void writeFloat(float v) throws IOException {
		write(CHR_FLOAT32);
		final int val = Float.floatToIntBits(v);
		write((val >>> 24) & 0xFF);
		write((val >>> 16) & 0xFF);
		write((val >>> 8) & 0xFF);
		write((val >>> 0) & 0xFF);
	}

	public void writeDouble(double v) throws IOException {
		final long val = Double.doubleToLongBits(v);
		buffer[0] = (byte) (val >>> 56);
		buffer[1] = (byte) (val >>> 48);
		buffer[2] = (byte) (val >>> 40);
		buffer[3] = (byte) (val >>> 32);
		buffer[4] = (byte) (val >>> 24);
		buffer[5] = (byte) (val >>> 16);
		buffer[6] = (byte) (val >>> 8);
		buffer[7] = (byte) (val >>> 0);
		write(CHR_FLOAT64);
		write(buffer, 0, 8);
	}

	public void writeBytes(String s) throws IOException {
		writeUTF(s);
	}

	public void writeChars(String s) throws IOException {
		writeUTF(s);
	}

	public void writeUTF(String s) throws IOException {
		final byte[] bytes = s.getBytes(UTF_8);
		if (bytes.length < STR_FIXED_COUNT) {
			write(STR_FIXED_START + bytes.length);
			write(bytes);
		} else {
			write(String.valueOf(bytes.length).getBytes(UTF_8));
			write(LENGTH_DELIMITER);
			write(bytes);
		}
	}

	public void writeObject(Object value) throws IOException {
		if (value == null) {
			write(CHR_NONE);
		} else if (value instanceof Boolean) {
			writeBoolean((Boolean) value);

		} else if (value instanceof Character) {
			writeChar((Character) value);

		} else if (value instanceof String) {
			writeUTF((String) value);

    } else if (value instanceof Number) {
    	writeNumber((Number)value);

		} else if (value instanceof Collection<?>) {
			writeCollection((Collection<?>) value);

		} else if (value instanceof Map<?, ?>) {
			writeMap((Map<?, ?>) value);

		}
	}

	private void writeNumber(Number value) throws IOException {
		if(value instanceof Float) {
        writeFloat(value.floatValue());
    }
    else if(value instanceof Double) {
        writeDouble(value.doubleValue());
    }
    else if(value instanceof BigDecimal
    		|| value instanceof BigInteger) {
    	final String sval = String.valueOf(value);
			if (sval.length() >= MAX_INT_LENGTH) {
				throw new IllegalArgumentException("A number " + sval + " should not exceed length of " + MAX_INT_LENGTH);
			}
			write(CHR_INT);
			writeChars(sval);
    } else {
    	writeLong(value.longValue());
    }
	}

	public void writeCollection(Collection<?> list) throws IOException {
		if (list.size() < LIST_FIXED_COUNT) {
			write(LIST_FIXED_START + list.size());
			for (Object elem : list) {
				writeObject(elem);
			}
		} else {
			write(CHR_LIST);
			for (Object elem : list) {
				writeObject(elem);
			}
			write(CHR_TERM);
		}
	}

	public <T> void writeMap(Map<T, ?> map) throws IOException {
		if (map.size() < DICT_FIXED_COUNT) {
			write(DICT_FIXED_START + map.size());
			for (Entry<T, ?> elem : map.entrySet()) {
				writeObject(elem.getKey());
				writeObject(elem.getValue());
			}
		} else {
			write(CHR_DICT);
			for (Entry<T, ?> elem : map.entrySet()) {
				writeObject(elem.getKey());
				writeObject(elem.getValue());
			}
			write(CHR_TERM);
		}
	}

}
