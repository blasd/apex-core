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
package org.eclipse.mat.tests.parser;

import org.eclipse.mat.parser.index.longroaring.LongIterator;
import org.eclipse.mat.parser.index.longroaring.MutableTreeRoaringBitmap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestTreeRoaringBitmap {
	@Test
	public void testEmpty() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		Assert.assertFalse(map.iterator().hasNext());

		Assert.assertEquals(0, map.getCardinality());

		Assert.assertEquals(0, map.rankLong(Long.MIN_VALUE));
		Assert.assertEquals(0, map.rankLong(Long.MIN_VALUE + 1));
		Assert.assertEquals(0, map.rankLong(-1));
		Assert.assertEquals(0, map.rankLong(0));
		Assert.assertEquals(0, map.rankLong(1));
		Assert.assertEquals(0, map.rankLong(Long.MAX_VALUE - 1));
		Assert.assertEquals(0, map.rankLong(Long.MAX_VALUE));
	}

	@Test
	public void testZero() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.addLong(0);

		{
			LongIterator iterator = map.iterator();
			Assert.assertTrue(iterator.hasNext());
			Assert.assertEquals(0, iterator.next());
			Assert.assertEquals(0, map.select(0));
			Assert.assertFalse(iterator.hasNext());
		}

		Assert.assertEquals(1, map.getCardinality());

		Assert.assertEquals(0, map.rankLong(Long.MIN_VALUE));
		Assert.assertEquals(0, map.rankLong(Integer.MIN_VALUE - 1L));
		Assert.assertEquals(0, map.rankLong(-1));
		Assert.assertEquals(1, map.rankLong(0));
		Assert.assertEquals(1, map.rankLong(1));
		Assert.assertEquals(1, map.rankLong(Integer.MAX_VALUE + 1L));
		Assert.assertEquals(1, map.rankLong(Long.MAX_VALUE));
	}

	@Test
	public void testIterator_NextWithoutHasNext_Filled() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.addLong(0);

		Assert.assertTrue(map.iterator().hasNext());
		Assert.assertEquals(0, map.iterator().next());
	}

	@Test(expected = IllegalStateException.class)
	public void testIterator_NextWithoutHasNext_Empty() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.iterator().next();
	}

	@Test
	public void testLongMaxValue() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.addLong(Long.MAX_VALUE);

		{
			LongIterator iterator = map.iterator();
			Assert.assertTrue(iterator.hasNext());
			Assert.assertEquals(Long.MAX_VALUE, iterator.next());
			Assert.assertEquals(Long.MAX_VALUE, map.select(0));
			Assert.assertFalse(iterator.hasNext());
		}

		Assert.assertEquals(1, map.getCardinality());

		Assert.assertEquals(0, map.rankLong(Long.MIN_VALUE));
		Assert.assertEquals(0, map.rankLong(Long.MIN_VALUE + 1));
		Assert.assertEquals(0, map.rankLong(-1));
		Assert.assertEquals(0, map.rankLong(0));
		Assert.assertEquals(0, map.rankLong(1));
		Assert.assertEquals(0, map.rankLong(Long.MAX_VALUE - 1));
		Assert.assertEquals(1, map.rankLong(Long.MAX_VALUE));
	}

	@Test
	public void testLongMinValue() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.addLong(Long.MIN_VALUE);

		{
			LongIterator iterator = map.iterator();
			Assert.assertTrue(iterator.hasNext());
			Assert.assertEquals(Long.MIN_VALUE, iterator.next());
			Assert.assertEquals(Long.MIN_VALUE, map.select(0));
			Assert.assertFalse(iterator.hasNext());
		}

		Assert.assertEquals(1, map.getCardinality());

		Assert.assertEquals(1, map.rankLong(Long.MIN_VALUE));
		Assert.assertEquals(1, map.rankLong(Long.MIN_VALUE + 1));
		Assert.assertEquals(1, map.rankLong(-1));
		Assert.assertEquals(1, map.rankLong(0));
		Assert.assertEquals(1, map.rankLong(1));
		Assert.assertEquals(1, map.rankLong(Long.MAX_VALUE - 1));
		Assert.assertEquals(1, map.rankLong(Long.MAX_VALUE));
	}

	@Test
	public void testLongMinValueZeroOneMaxValue() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		map.addLong(Long.MIN_VALUE);
		map.addLong(0);
		map.addLong(1);
		map.addLong(Long.MAX_VALUE);

		{
			LongIterator iterator = map.iterator();
			Assert.assertTrue(iterator.hasNext());
			Assert.assertEquals(Long.MIN_VALUE, iterator.next());
			Assert.assertEquals(Long.MIN_VALUE, map.select(0));
			Assert.assertEquals(0, iterator.next());
			Assert.assertEquals(0, map.select(1));
			Assert.assertEquals(1, iterator.next());
			Assert.assertEquals(1, map.select(2));
			Assert.assertEquals(Long.MAX_VALUE, iterator.next());
			Assert.assertEquals(Long.MAX_VALUE, map.select(3));
			Assert.assertFalse(iterator.hasNext());
		}

		Assert.assertEquals(4, map.getCardinality());

		Assert.assertEquals(1, map.rankLong(Long.MIN_VALUE));
		Assert.assertEquals(1, map.rankLong(Long.MIN_VALUE + 1));
		Assert.assertEquals(1, map.rankLong(-1));
		Assert.assertEquals(2, map.rankLong(0));
		Assert.assertEquals(3, map.rankLong(1));
		Assert.assertEquals(3, map.rankLong(2));
		Assert.assertEquals(3, map.rankLong(Long.MAX_VALUE - 1));
		Assert.assertEquals(4, map.rankLong(Long.MAX_VALUE));
	}

	@Test
	public void testPerfManyDifferentBuckets() {
		MutableTreeRoaringBitmap map = new MutableTreeRoaringBitmap();

		long problemSize = 100 * 1000L;
		for (long i = 1; i <= problemSize; i++) {
			map.addLong(i * Integer.MAX_VALUE + 1L);
		}

		long cardinality = map.getCardinality();
		Assert.assertEquals(problemSize, cardinality);

		long last = map.select(cardinality - 1);
		Assert.assertEquals(problemSize * Integer.MAX_VALUE + 1L, last);
		Assert.assertEquals(cardinality, map.rankLong(last));
	}
}
