//
// $Id$

package com.threerings.msoy.mchooser.sources;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.threerings.msoy.mchooser.MediaChooser;
import com.threerings.msoy.mchooser.MediaSource;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Allows the user to select files from their local file system.
 */
public class LocalFileSource
    implements MediaSource
{
    public static enum Type { IMAGE, AUDIO };

    public LocalFileSource (Type type)
    {
        _type = type;
    }

    // from interface MediaSource
    public String getName ()
    {
        return "Your Computer";
    }

    // from interface MediaSource
    public JComponent createChooser (final ResultReceiver receiver)
    {
        // set up our starting directory
        File startDir = new File(MediaChooser.prefs.get(START_DIR_KEY,
                                                        System.getProperty("user.home")));

        final JFileChooser chooser = new JFileChooser(startDir);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // set up our filters
        switch (_type) {
        case IMAGE:
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Image files", "jpg", "jpeg", "gif", "png", "bmp"));
            break;
        }

        // set up a listener that will report the selection back to the chooser
        chooser.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    File selection = chooser.getSelectedFile();
                    try {
                        // make a note of our current directory
                        MediaChooser.prefs.put(START_DIR_KEY, selection.getParent());
                        receiver.mediaSelected(selection.toURI().toURL());
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to generate URL for " +
                                chooser.getSelectedFile() + ".", e);
                    }
                }
            }
        });
        return chooser;
    }

    protected Type _type;

    protected static final String START_DIR_KEY = "localFileStart";
}
