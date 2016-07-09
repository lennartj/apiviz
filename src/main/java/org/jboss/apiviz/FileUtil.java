/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.apiviz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author The APIviz Project (apiviz-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
public class FileUtil {

    public static final String NEWLINE = System.getProperty("line.separator", "\n");
    public static final String NORMAL_FONT = "Arial";
    public static final String ITALIC_FONT = "Arial Italic";

    /**
     * The default charset used unless another was supplied.
     */
    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    /**
     * Reads and returns all content within the supplied file.
     *
     * @param file a non-null File.
     * @return the string
     * @throws IOException
     */
    public static String readFile(final File file) throws IOException {

        byte[] byteContent;
        RandomAccessFile in = new RandomAccessFile(file, "r");
        try {
            byteContent = new byte[(int) in.length()];
            in.readFully(byteContent);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        return new String(byteContent, DEFAULT_CHARSET);
    }

    public static void writeFile(File file, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes(DEFAULT_CHARSET));
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    private FileUtil() {
        // Unused
    }
}
