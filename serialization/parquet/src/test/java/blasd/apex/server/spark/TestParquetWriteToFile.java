/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.server.spark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.parquet.ParquetStreamFactory;
import blasd.apex.serialization.avro.AvroSchemaHelper;
import blasd.apex.serialization.avro.AvroStreamHelper;

/**
 * We demonstrate how to write a subset of rows from a parquet files
 * 
 * @author Benoit Lacelle
 *
 */
public class TestParquetWriteToFile {
	ParquetStreamFactory parquetStreamFactory = new ParquetStreamFactory();

	@Test(expected = IllegalArgumentException.class)
	public void testWriteParquet_FileExist() throws IOException {
		Assume.assumeTrue(ApexHadoopHelper.isHadoopReady());

		Stream<Map<String, Object>> rows = IntStream.range(0, 10)
				.mapToObj(i -> ImmutableMap.of("longField", (long) i, "stringField", "string_" + i));

		Schema avroSchema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("longField", 0L));

		Path tmpPath = ApexFileHelper.createTempPath("testWriteParquet_FromJavaStream", ".parquet", true);
		parquetStreamFactory.writeToPath(tmpPath, rows.map(AvroStreamHelper.toGenericRecord(avroSchema)));
	}

	@Test
	public void testWriteParquet_FromJavaStream() throws IOException {
		Assume.assumeTrue(ApexHadoopHelper.isHadoopReady());

		Stream<Map<String, Object>> rows = IntStream.range(0, 10)
				.mapToObj(i -> ImmutableMap.of("longField", (long) i, "stringField", "string_" + i));

		Schema avroSchema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("longField", 0L));

		Path tmpPath = ApexFileHelper.createTempPath("testWriteParquet_FromJavaStream", ".parquet", true);
		long nbWritten =
				parquetStreamFactory.writeToPath(tmpPath, rows.map(AvroStreamHelper.toGenericRecord(avroSchema)));

		Assert.assertEquals(10, nbWritten);
	}
}
