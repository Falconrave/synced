//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.threerings.msoy.swiftly.client.SwiftlyDocumentEditor;

/**
 * Represents an image file in a project and contains the data of the image.
 * Each document retains an unmodified copy of its initial data to allow for delta generation.
 */
public class SwiftlyImageDocument extends SwiftlyBinaryDocument
{
    public static class DocumentFactory
        implements SwiftlyDocument.DocumentFactory
    {
        public boolean handlesMimeType (String mimeType) {
            for (String type : _mimeTypes) {
                if (mimeType.startsWith(type)) {
                    return true;
                }
            }
            return false;
        }

        public SwiftlyDocument createDocument (PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyImageDocument(path);
        }

        public SwiftlyDocument createDocument (InputStream data, PathElement path, String encoding)
            throws IOException
        {
            return new SwiftlyImageDocument(data, path);
        }

        /** Mime types supported by this document type. */
        private String[] _mimeTypes = {"image/"};
    }

    /** Required for the dobj system. Do not use. */
    public SwiftlyImageDocument ()
    {
    }

    /** Instantiate a new, blank image document with the given path. */
    public SwiftlyImageDocument (PathElement path)
        throws IOException
    {
        this(null, path);
    }

    /** Instantiate an image document with the given data and path. */
    public SwiftlyImageDocument (InputStream data, PathElement path)
        throws IOException
    {
        super(data, path);
        // load up the image data into memory
        _image = IOUtils.toByteArray(getOriginalData());
    }

    /** Returns the image data as a byte array */
    public byte[] getImage ()
    {
        return _image;
    }

    @Override // from SwiftlyBinaryDocument
    public void setData (InputStream data, String encoding)
        throws IOException
    {
        super.setData(data, encoding);
        // load up the image data into memory
        _image = IOUtils.toByteArray(getModifiedData());
    }

    @Override // from SwiftlyDocument
    public void loadInEditor (SwiftlyDocumentEditor editor, int row, int column, boolean highlight)
    {
        editor.editImageDocument(this);
    }

    /** Image contents, inefficiently stored entirely in memory. */
    protected byte[] _image;
}
