/*******************************************************************************
 * Copyright (c) 2008, 2010 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.parser.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.mat.hprof.HprofHeapObjectReader;
import org.eclipse.mat.hprof.HprofIndexBuilder;
import org.eclipse.mat.parser.IIndexBuilder;
import org.eclipse.mat.parser.IObjectReader;
import org.eclipse.mat.snapshot.SnapshotFormat;

public class ParserRegistry
{
    private static final String ID = "id";//$NON-NLS-1$
    private static final String FILE_EXTENSION = "fileExtension";//$NON-NLS-1$
    private static final String NAME = "name";//$NON-NLS-1$
    private static final String DYNAMIC = "dynamic";//$NON-NLS-1$
    public static final String INDEX_BUILDER = "indexBuilder";//$NON-NLS-1$
    public static final String OBJECT_READER = "objectReader";//$NON-NLS-1$
	
	private static final List<Parser> parsers = Arrays.asList(new Parser("hprof",
			new SnapshotFormat("hprof", new String[] { "hprof", "bin" }),
			new org.eclipse.mat.hprof.HprofHeapObjectReader(),
			new org.eclipse.mat.hprof.HprofIndexBuilder()));

    public static class Parser
    {
        private String id;
        private SnapshotFormat snapshotFormat;

	    	private IObjectReader objectReader;
	    	private IIndexBuilder indexBuilder;

        public Parser(String id,
				SnapshotFormat snapshotFormat,
				HprofHeapObjectReader hprofHeapObjectReader,
				HprofIndexBuilder hprofIndexBuilder) {
            this.id = id;
            this.snapshotFormat = snapshotFormat;

            this.objectReader = objectReader;
            this.indexBuilder = indexBuilder;
		}

		public String getId()
        {
            return id;
        }

        public String getUniqueIdentifier()
        {
            return "mat" + "." + id;//$NON-NLS-1$
        }

        public SnapshotFormat getSnapshotFormat()
        {
            return snapshotFormat;
        }

        public IObjectReader createObjectReader()
        {
            return objectReader;
        }

		public IIndexBuilder createIndexBuider() {
			return indexBuilder;
		}
    }
    
    public static Parser lookupParser(String uniqueIdentifier)
    {
        for (Parser p : parsers)
            if (uniqueIdentifier.equals(p.getUniqueIdentifier()))
                return p;
        return null;
    }

	public static List<Parser> matchParser(String name2) {
		// TODO Auto-generated method stub
		return null;
	}

	public static List<SnapshotFormat> getSupportedFormats() {
		// TODO Auto-generated method stub
		return null;
	}

}