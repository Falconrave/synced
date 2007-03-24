//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import com.threerings.io.Streamable;

/**
 * Compilation Build Results.
 */
public class BuildResult
    implements Streamable
{
    public BuildResult ()
    {
        _output = new ArrayList<CompilerOutput>();
    }

    /** Set compiler output file path. */
    public void setOutputFile (File path) {
        _outputFile = path;
    }

    public File getOutputFile ()
    {
        return _outputFile;
    }

    public void setBuildResultURL (String url) {
        _buildResultURL = url;
    }

    public String getBuildResultURL ()
    {
        return _buildResultURL;
    }

    /** Return the build compiler's output, in the order it was received. */
    public List<CompilerOutput> getOutput () {
        return _output;
    }

    /** Append a parsed compiler statement to the build output. */
    public void appendOutput (CompilerOutput output) {
        // If we get an error message, the build failed.
        if (output.getLevel() == CompilerOutput.Level.ERROR) {
            _buildSuccess = false;
        }

        _output.add(output);
    }

    /** Returns true if the build succeeded. */
    public boolean buildSuccessful () {
        return _buildSuccess;
    }

    /** All compiler output. */
    private List<CompilerOutput> _output;
    
    /** Did the build succeed. */
    protected boolean _buildSuccess = true;

    /** The build output file. */
    protected transient File _outputFile;

    /** The URL of the built artifact. */
    protected String _buildResultURL;
}
