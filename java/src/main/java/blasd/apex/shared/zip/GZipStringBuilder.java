/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.shared.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

import com.google.common.base.Charsets;

import blasd.apex.shared.util.IApexMemoryConstants;

/**
 * A StringBuilder-like structure gzipping pending data
 * 
 * @author Benoit Lacelle
 *
 */
// http://support.quartetfs.com/jira/browse/APS-6584
public class GZipStringBuilder implements Appendable {
	protected static final int DEFAULT_BUFFER_LENGTH = IApexMemoryConstants.KB_INT;

	/**
	 * Temporary buffer used to hold writes of strings and single characters
	 */
	protected char[] charBuffer;

	protected ByteArrayOutputStream baos = new ByteArrayOutputStream();
	protected DeflaterOutputStream os;
	protected long count = 0;

	public GZipStringBuilder() {
		charBuffer = new char[DEFAULT_BUFFER_LENGTH];

		// syncFlush to make sure .flush flushes the compressor
		os = new DeflaterOutputStream(baos, true);
	}

	@Override
	public GZipStringBuilder append(CharSequence s) {
		if (s == null) {
			return append("null");
		} else if (s instanceof String) {
			return this.appendChars(s.length(), ((String) s)::getChars);
		} else if (s instanceof StringBuilder) {
			return this.appendChars(s.length(), ((StringBuilder) s)::getChars);
		} else {
			return this.append(s.toString());
		}
	}

	// http://stackoverflow.com/questions/5513144/converting-char-to-byte
	private byte[] toBytes(char[] chars, int length, Charset charset) {
		CharBuffer charBuffer = CharBuffer.wrap(chars, 0, length);
		ByteBuffer byteBuffer = charset.encode(charBuffer);

		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		// Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		// Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	private interface CopyChars {
		void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);
	}

	protected GZipStringBuilder appendChars(int length, CopyChars copyChars) {
		try {
			if (charBuffer.length < length) {
				charBuffer = new char[length];
				// byteBuffer = new byte[length];
			}

			copyChars.getChars(0, length, charBuffer, 0);

			byte[] byteBuffer = toBytes(charBuffer, length, Charsets.UTF_8);

			os.write(byteBuffer);
			count += length;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	protected GZipStringBuilder appendStringBuilder(StringBuilder s) {
		try {
			int length = s.length();

			if (charBuffer.length < length) {
				charBuffer = new char[length];
				// byteBuffer = new byte[length];
			}

			s.getChars(0, length, charBuffer, 0);

			byte[] byteBuffer = toBytes(charBuffer, length, Charsets.UTF_8);

			os.write(byteBuffer);
			count += length;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public void writeTo(OutputStream out, Charset charset) {
		try {
			os.flush();

			// We need to decompress when writing back
			baos.writeTo(new InflaterOutputStream(out));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream toInputStream() {
		try {
			os.flush();

			return new InflaterInputStream(new ByteArrayInputStream(baos.toByteArray()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] copyInflatedByteArray() {
		try {
			os.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) {
		if (csq == null) {
			append("null");
		} else {
			append(csq.subSequence(start, end));
		}

		return this;
	}

	@Override
	public Appendable append(char c) throws IOException {
		os.write(c);

		return this;
	}

	/**
	 * Clear the content of this {@link StringBuilder}
	 */
	public void clear() {
		synchronized (this) {
			charBuffer = new char[DEFAULT_BUFFER_LENGTH];
			count = 0;
			try {
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			baos.reset();
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeTo(baos, Charsets.UTF_8);
		return new String(baos.toByteArray());
	}
}
