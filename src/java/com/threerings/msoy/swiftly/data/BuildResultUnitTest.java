//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.io.File;

import junit.framework.TestCase;

public class BuildResultUnitTest extends TestCase
{
    public BuildResultUnitTest (String name)
    {
        super(name);
    }

    public void testAppendOutput ()
    {
        CompilerOutput output = new FlexCompilerOutput("Awesome",
            CompilerOutput.Level.INFO, "file.as", 27, 5);
        BuildResult result = new BuildResult();

        result.appendOutput(output);
        
        for (CompilerOutput testOutput : result.getOutput()) {
            assertEquals(testOutput, output);
        }
    }

    public void testErrorDetection ()
    {
        CompilerOutput output = new FlexCompilerOutput("Awesome",
            CompilerOutput.Level.ERROR, "file.as", 27, 5);
        BuildResult result = new BuildResult();

        assertEquals(true, result.buildSuccessful());
        result.appendOutput(output);
        assertEquals(false, result.buildSuccessful());
    }

    public void testSetBuildOutputFile ()
    {
        BuildResult result = new BuildResult();
        File outputFile = new File("/tmp/nonexistent");

        result.setOutputFile(outputFile);
    }
}
