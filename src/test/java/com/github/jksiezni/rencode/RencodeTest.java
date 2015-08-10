/**
 * 
 */
package com.github.jksiezni.rencode;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Jakub Księżniak
 *
 */
public class RencodeTest {

	private ByteArrayOutputStream byteStream;
	private RencodeOutputStream rEncode;
	
	private RencodeInputStream decoder() {
		final byte[] bytes = byteStream.toByteArray();
		System.out.println(Arrays.toString(bytes));
		return new RencodeInputStream(new ByteArrayInputStream(bytes), true);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		byteStream = new ByteArrayOutputStream();
		rEncode = new RencodeOutputStream(byteStream);
	}
	
	@Test
	public void testBoolean() throws Exception {
		rEncode.writeBoolean(true);
		rEncode.writeBoolean(false);
		assertEquals(true, decoder().readBoolean());
		assertEquals(true, decoder().readObject());
	}

	@Test
	public void testByte() throws Exception {
		rEncode.writeByte(Byte.MAX_VALUE);
		rEncode.writeByte(1);
		rEncode.writeByte(-1);
		{ // test decoding of pure byte values
			final RencodeInputStream decoder = decoder();
			assertEquals(Byte.MAX_VALUE, decoder.readByte());
			assertEquals(1, decoder.readByte());
			assertEquals(-1, decoder.readByte());
		}
		{ // test decoding of byte values as objects
			final RencodeInputStream decoder = decoder();
			assertEquals(Byte.MAX_VALUE, decoder.readObject());
			assertEquals((byte)1, decoder.readObject());
			assertEquals((byte)-1, decoder.readObject());
		}
		assertEquals(Byte.MAX_VALUE, decoder().readShort());
		assertEquals(Byte.MAX_VALUE, decoder().readInt());
		assertEquals(Byte.MAX_VALUE, decoder().readLong());
	}
	
	@Test
	public void testShort() throws Exception {
		rEncode.writeShort(Short.MAX_VALUE);
		assertEquals(-1, decoder().readByte());
		assertEquals(Short.MAX_VALUE, decoder().readShort());
		assertEquals(Short.MAX_VALUE, decoder().readInt());
		assertEquals(Short.MAX_VALUE, decoder().readLong());
		assertEquals(Short.MAX_VALUE, decoder().readObject());
	}
	
	@Test
	public void testInt() throws Exception {
		rEncode.writeInt(Integer.MAX_VALUE);
		assertEquals(-1, decoder().readByte());
		assertEquals(-1, decoder().readShort());
		assertEquals(Integer.MAX_VALUE, decoder().readInt());
		assertEquals(Integer.MAX_VALUE, decoder().readLong());
		assertEquals(Integer.MAX_VALUE, decoder().readObject());
	}
	
	@Test
	public void testLong() throws Exception {
		rEncode.writeLong(Long.MAX_VALUE);
		assertEquals(-1, decoder().readByte());
		assertEquals(-1, decoder().readShort());
		assertEquals(-1, decoder().readInt());
		assertEquals(Long.MAX_VALUE, decoder().readLong());
		assertEquals(Long.MAX_VALUE, decoder().readObject());
	}
	
	@Test
	public void testFloat() throws Exception {
		rEncode.writeFloat(Float.MAX_VALUE);
		assertEquals(Float.MAX_VALUE, decoder().readFloat(), 0);
		assertEquals(Float.MAX_VALUE, decoder().readObject());
	}
	
	@Test
	public void testDouble() throws Exception {
		rEncode.writeDouble(Double.MAX_VALUE);
		assertEquals(Double.MAX_VALUE, decoder().readDouble(), 0);
		assertEquals(Double.MAX_VALUE, decoder().readObject());
	}
	
	@Test
	public void testString() throws Exception {
		rEncode.writeUTF("test0");
		assertEquals("test0", decoder().readUTF());
		assertEquals("test0", decoder().readObject());
	}
	
	@Test
	public void testList() throws Exception {
		ArrayList<String> list = new ArrayList<>();
		list.add("testA");
		list.add("testB");
		rEncode.writeCollection(list);
		assertEquals(list, decoder().readList());
		assertEquals(list, decoder().readObject());
	}
	
	@Test
	public void testMap() throws Exception {
		Map<Object, Object> map = new HashMap<>();
		map.put("testA", Integer.MAX_VALUE);
		map.put(1, "xyz");
		rEncode.writeMap(map);
		assertEquals(map, decoder().readMap());
		assertEquals(map, decoder().readObject());
	}

}
