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
package blasd.apex.core.eventbus;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class TestApexEventBusHelper {

	@Test
	public void test_ctor_coverage() {
		Assert.assertNotNull(new ApexEventBusHelper());
	}

	@Test
	public void testAsConsumer() {
		EventBus eventBus = new EventBus();

		Optional<Consumer<Object>> asConsumer = ApexEventBusHelper.asConsumer(eventBus);

		Assert.assertTrue(asConsumer.isPresent());
	}

	@Test
	public void testAsConsumer_null() {
		Optional<Consumer<Object>> asConsumer = ApexEventBusHelper.asConsumer(null);

		Assert.assertFalse(asConsumer.isPresent());
	}
}
