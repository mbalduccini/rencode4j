/**
 * 
 */
package com.github.jksiezni.rencode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.jksiezni.rencode.Rencode.*;

/**
 * @author Jakub Księżniak
 * 
 */
public class RencodeInputStream extends PushbackInputStream implements ObjectInput {
	
	private interface Decoder<T> {
		T decode(int token) throws IOException;
	}

	private final Decoder<?>[] decoders = new Decoder[256];
	

	public RencodeInputStream(InputStream in) {
		this(in, false);
	}
	
	public RencodeInputStream(InputStream in, final boolean decodeAsString) {
		super(in);
		decoders[CHR_TRUE] = new Decoder<Boolean>() {
			@Override
			public Boolean decode(int token) throws IOException {
				return Boolean.TRUE;
			}
		};
		decoders[CHR_FALSE] = new Decoder<Boolean>() {
			@Override
			public Boolean decode(int token) throws IOException {
				return Boolean.FALSE;
			}
		};
		decoders[CHR_NONE] = new Decoder<Object>() {
			@Override
			public Object decode(int token) throws IOException {
				return null;
			}
		};
		decoders['0'] = decoders['1'] = decoders['2'] = decoders['3'] = decoders['4'] = decoders['5'] = decoders['6'] = decoders['7'] = decoders['8'] = decoders['9'] = new Decoder<Object>() {
			@Override
			public Object decode(int token) throws IOException {
				final int length = fetchLength(token);
				if(decodeAsString) {
					return fetchString(length);
				} else {
					final byte[] bytes = new byte[length];
					readFully(bytes);
					return bytes;
				}
			}
		};
		for (int i = STR_FIXED_START; i < STR_FIXED_START+STR_FIXED_COUNT; ++i) {
			decoders[i] = new Decoder<Object>() {
				@Override
				public Object decode(int token) throws IOException {
					final int length = token - STR_FIXED_START;
					if(decodeAsString) {
						return fetchString(length);
					} else {
						final byte[] bytes = new byte[length];
						readFully(bytes);
						return bytes;
					}
				}
			};
		}
		for (int i = INT_POS_FIXED_START; i < INT_POS_FIXED_COUNT; ++i) {
			decoders[i] = new Decoder<Byte>() {
				@Override
				public Byte decode(int token) throws IOException {
					return (byte) token;
				}
			};
		}
		for (int i = INT_NEG_FIXED_START; i < INT_NEG_FIXED_START + INT_NEG_FIXED_COUNT; ++i) {
			decoders[i] = new Decoder<Byte>() {
				@Override
				public Byte decode(int token) throws IOException {
					return (byte) (INT_NEG_FIXED_START - 1 - token);
				}
			};
		}
		decoders[CHR_INT1] = new Decoder<Byte>() {
			@Override
			public Byte decode(int token) throws IOException {
				return (byte) read();
			}
		};
		decoders[CHR_INT2] = new Decoder<Short>() {
			@Override
			public Short decode(int token) throws IOException {
				return (short) (read() << 8 | read());
			}
		};
		decoders[CHR_INT4] = new Decoder<Integer>() {
			@Override
			public Integer decode(int token) throws IOException {
				return fetchInt();
			}
		};
		decoders[CHR_INT8] = new Decoder<Long>() {
			@Override
			public Long decode(int token) throws IOException {
				return fetchLong();
			}
		};
		decoders[CHR_FLOAT32] = new Decoder<Float>() {
			@Override
			public Float decode(int token) throws IOException {
				return Float.intBitsToFloat(fetchInt());
			}
		};
		decoders[CHR_FLOAT64] = new Decoder<Double>() {
			@Override
			public Double decode(int token) throws IOException {
				return Double.longBitsToDouble(fetchLong());
			}
		};
		for(int i = LIST_FIXED_START; i < LIST_FIXED_START+LIST_FIXED_COUNT; ++i) {
  		decoders[i] = new Decoder<List<?>>() {
  			@Override
  			public List<?> decode(int token) throws IOException {
  				int count = token - LIST_FIXED_START;
  				final List<Object> list = new ArrayList<>(count);
  				while(count-- > 0) {
  					list.add(readObject());
  				}
  				return list;
  			}
  		};
		}
		decoders[CHR_LIST] = new Decoder<List<?>>() {
			@Override
			public List<?> decode(int token) throws IOException {
				final List<Object> list = new ArrayList<>(2*LIST_FIXED_COUNT);
				do {
					list.add(readObject());
				} while(peek() != CHR_TERM);
				read(); // consume EOF character
				return list;
			}
		};
		for(int i = DICT_FIXED_START; i < DICT_FIXED_START+DICT_FIXED_COUNT; ++i) {
			decoders[i] = new Decoder<Map<?,?>>() {
				@Override
				public Map<?,?> decode(int token) throws IOException {
					int count = token - DICT_FIXED_START;
					final Map<Object, Object> map = new HashMap<>(count, 1);
					while(count-- > 0) {
						map.put(readKey(), readObject());
					}
					return map;
				}
			};
		}
		decoders[CHR_DICT] = new Decoder<Map<?,?>>() {
			@Override
			public Map<?,?> decode(int token) throws IOException {
				final Map<Object, Object> map = new HashMap<>(2*DICT_FIXED_COUNT);
				do {
					map.put(readKey(), readObject());
				} while(peek() != CHR_TERM);
				read(); // consume EOF character
				return map;
			}
		};
	}
	
	@Override
	public int read() throws IOException {
		final int value = super.read();
		if(value < 0) {
			throw new EOFException("end of stream");
		}
		return value;
	}

	@Override
	public Object readObject() throws IOException {
		final int token = read();
		if (token==null) throw new IOException("readObject(): null token");
		return decoders[token].decode(token);
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		int total = 0;
		while (total < len) {
			final int r = read(b, off + total, len - total);
			if (r < 0) {
				throw new EOFException();
			}
			total += r;
		}
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return (int) skip(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return read() == CHR_TRUE;
	}

	private long readNumber() throws IOException {
		final int token = read();
		if (0 <= token && token < INT_POS_FIXED_COUNT) {
			return token;
		} else if (INT_NEG_FIXED_START <= token && token < INT_NEG_FIXED_START + INT_NEG_FIXED_COUNT) {
			return INT_NEG_FIXED_START - 1 - token;
		}
		switch (token) {
		case CHR_INT1:
			return read();
		case CHR_INT2:
			return read() << 8 | read();
		case CHR_INT4:
			return fetchInt();
		case CHR_INT8:
			return fetchLong();
		default:
			throw new IOException("Unable to decode the number.");
		}
	}

	@Override
	public byte readByte() throws IOException {
		return (byte) readNumber();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return (int) readNumber();
	}

	@Override
	public short readShort() throws IOException {
		return (short) readNumber();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return (int) readNumber();
	}

	@Override
	public char readChar() throws IOException {
		return (char) readShort();
	}

	@Override
	public int readInt() throws IOException {
		return (int) readNumber();
	}

	@Override
	public long readLong() throws IOException {
		return readNumber();
	}

	private void validate(final int expected) throws IOException {
		final int token = read();
		if (token != expected) {
			throw new IOException("Invalid code: expected=" + expected + ", actual=" + token);
		}
	}

	@Override
	public float readFloat() throws IOException {
		validate(CHR_FLOAT32);
		return Float.intBitsToFloat(fetchInt());
	}

	@Override
	public double readDouble() throws IOException {
		validate(CHR_FLOAT64);
		return Double.longBitsToDouble(fetchLong());
	}

	@Override
	public String readLine() throws IOException {
		return readUTF();
	}

	@Override
	public String readUTF() throws IOException {
		final int token = read();
		if (STR_FIXED_START <= token && token < STR_FIXED_START + STR_FIXED_COUNT) {
			return fetchString(token - STR_FIXED_START);
		} else if ('0' <= token && token <= '9') {
			final int size = fetchLength(token);
			return fetchString(size);
		}
		throw new IOException("Unable to read a String from stream.");
	}

	public List<Object> readList() throws IOException {
		final int token = read();
		if (LIST_FIXED_START <= token && token < LIST_FIXED_START+LIST_FIXED_COUNT) {
			int count = token - LIST_FIXED_START;
			final List<Object> list = new ArrayList<>(count);
			while(count-- > 0) {
				list.add(readObject());
			}
			return list;
		} else if (token == CHR_LIST) {
			final List<Object> list = new ArrayList<>(2*LIST_FIXED_COUNT);
			do {
				list.add(readObject());
			} while(peek() != CHR_TERM);
			read(); // consume EOF character
			return list;
		}
		throw new IOException("Unable to read a List from stream.");
	}

	public Map<Object, Object> readMap() throws IOException {
		final int token = read();
		if (DICT_FIXED_START <= token && token < DICT_FIXED_START+DICT_FIXED_COUNT) {
			int count = token - DICT_FIXED_START;
			final Map<Object, Object> map = new HashMap<>(count, 1);
			while(count-- > 0) {
				map.put(readKey(), readObject());
			}
			return map;
		} else if (token == CHR_DICT) {
			final Map<Object, Object> map = new HashMap<>(2*DICT_FIXED_COUNT);
			do {
				map.put(readKey(), readObject());
			} while(peek() != CHR_TERM);
			read(); // consume EOF character
			return map;
		}
		throw new IOException("Unable to read a List from stream.");
	}

	private Object readKey() throws IOException {
		final Object key = readObject();
		if(key instanceof Number) {
			return ((Number)key).intValue();
		} else if(key instanceof byte[]) {
			return new String((byte[]) key, UTF_8);
		}
		return key;
	}

	private int peek() throws IOException {
		final int token = read();
		unread(token);
		return token;
	}

	private final byte[] buffer = new byte[8];

	private int fetchInt() throws IOException {
		readFully(buffer, 0, 4);
		return (buffer[0] & 0xFF) << 24 | (buffer[1] & 0xFF) << 16 | (buffer[2] & 0xFF) << 8 | (buffer[3] & 0xFF);
	}

	private long fetchLong() throws IOException {
		readFully(buffer, 0, 8);
		return (buffer[0] & 0xFFl) << 56 | (buffer[1] & 0xFFl) << 48 | (buffer[2] & 0xFFl) << 40
				| (buffer[3] & 0xFFl) << 32 | (buffer[4] & 0xFFl) << 24 | (buffer[5] & 0xFF) << 16 | (buffer[6] & 0xFF) << 8
				| (buffer[7] & 0xFF);
	}

	private int fetchLength(int token) throws IOException {
		final StringBuilder buffer = new StringBuilder(MAX_INT_LENGTH);
		buffer.append((char) token);
		while ((token = read()) != LENGTH_DELIMITER) {
			buffer.append((char) token);
		}
		return Integer.parseInt(buffer.toString());
	}

	private String fetchString(int size) throws IOException {
		final byte[] bytes = size <= buffer.length ? buffer : new byte[size];
		readFully(bytes, 0, size);
		return new String(bytes, 0, size, UTF_8);
	}
}
