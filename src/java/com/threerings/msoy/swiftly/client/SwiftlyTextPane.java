package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor, SwiftlyDocument document)
    {
        _editor = editor;
        _document = document;

        createActionTable();
        addKeyBindings();
        addPopupMenu();

        // TODO SwiftlyDocument should provide a reader
        StringReader reader = new StringReader(document.getText());
        ActionScriptEditorKit kit = new ActionScriptEditorKit();
        setEditorKit(kit);
        ActionScriptStyledDocument styledDoc = new ActionScriptStyledDocument();
        setDocument(styledDoc);
        // setContentType("text/actionscript");

        // load the document text
        try {
            // styledDoc.insertString(0, document.getText(), null);
            kit.read(reader, styledDoc, 0);
        } catch (IOException io) {
            return;
        } catch (BadLocationException be) {
            return;
        }

        // add listeners
        styledDoc.addUndoableEditListener(new UndoHandler());
        styledDoc.addDocumentListener(new SwiftlyDocumentListener());

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.black);
        setBackground(Color.white);
    }

    public SwiftlyDocument getSwiftlyDocument ()
    {
        return _document;
    }

    // Save the document if needed. Return true if save happened/worked
    public boolean saveDocument ()
    {
        if (_documentChanged) {
            // TODO save the document into the internets
            _documentChanged = false;
            return true;
        }
        return false;
    }

    public boolean hasUnsavedChanges ()
    {
        return _documentChanged;
    }

    // Throws up a close tab dialog internal frame
    public void closeTabDialog()
    {
        // TODO only ever have one of these dialogs in a pane
        // TODO figure out how to select the dialog when it pops up
        add(new CloseTabDialog());
    }

    protected void addKeyBindings ()
    {
        // ctrl-n opens a new tab
        addKeyAction(_editor.createNewTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));

        // ctrl-s saves the current document
        addKeyAction(_editor.createSaveCurrentTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));

        // ctrl-w closes the tab
        addKeyAction(_editor.createCloseCurrentTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));

        // ctrl-z undos the action
        addKeyAction(_undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));

        // ctrl-y redoes the action
        addKeyAction(_redoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
    }

    protected void addPopupMenu ()
    {
        _popup = new JPopupMenu();

        // TODO is there a cross platform way to show what the keybindings are for these actions?
        // Cut
        JMenuItem menuItem = new JMenuItem("Cut");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.cutAction));
        _popup.add(menuItem);
        // Copy
        menuItem = new JMenuItem("Copy");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.copyAction));
        _popup.add(menuItem);
        // Paste
        menuItem = new JMenuItem("Paste");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.pasteAction));
        _popup.add(menuItem);
        // Seperator
        _popup.addSeparator();
        // Select All
        menuItem = new JMenuItem("Select All");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.selectAllAction));
        _popup.add(menuItem);
        // Seperator
        _popup.addSeparator();
        // Undo
        menuItem = new JMenuItem("Undo");
        menuItem.addActionListener(_undoAction);
        _popup.add(menuItem);
        // Redo
        menuItem = new JMenuItem("Redo");
        menuItem.addActionListener(_redoAction);
        _popup.add(menuItem);

        MouseListener popupListener = new PopupListener();
        // add popupListener to the textpane
        addMouseListener(popupListener);
    }

    protected void addKeyAction (Action action, KeyStroke key)
    {
        // key bindings work even if the textpane doesn't have focus
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, action);
        getActionMap().put(action, action);
    }

    // Create the action lookup table
    protected void createActionTable() {
        for (Action action : getStyledEditorKit().getActions()) {
            _actions.put((String)action.getValue(Action.NAME), action);
        }
    }

    // Lookup an action by name
    protected Action getActionByName(String name) {
        return (Action)(_actions.get(name));
    }

    protected class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    protected class UndoHandler implements UndoableEditListener
    {
        public void undoableEditHappened (UndoableEditEvent e)
        {
            _undo.addEdit(e.getEdit());
            _undoAction.update();
            _redoAction.update();
        }
    }

    protected class UndoAction extends AbstractAction
    {
        public UndoAction () {
            super("Undo");
            setEnabled(false);
        }

        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.undo();
            } catch (CannotUndoException ex) {
                _editor.getApplet().setStatus("Unable to undo.");
            }
            update();
            _redoAction.update();
        }

        protected void update ()
        {
            if(_undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, _undo.getUndoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    protected class RedoAction extends AbstractAction
    {
        public RedoAction ()
        {
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.redo();
            } catch (CannotRedoException ex) {
                _editor.getApplet().setStatus("Unable to redo.");
            }
            update();
            _undoAction.update();
        }

        protected void update ()
        {
            if(_undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, _undo.getRedoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    class SwiftlyDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            if (!_documentChanged) {
                _editor.setTabTitleChanged(true);
                _documentChanged = true;
            }
        }

        public void removeUpdate(DocumentEvent e) {
            // nada
        }

        public void changedUpdate(DocumentEvent e) {
            // nada
        }
    }

    protected class CloseTabDialog extends JInternalFrame
    {
        public CloseTabDialog ()
        {
            super("Unsaved changes.", 
                  false,  //resizable
                  false,  //closable
                  false,  //maximizable
                  false); //iconifiable
            setVisible(true);
            // TODO determine this location relatively
            setLocation(60,60);
            add(new JLabel("Document has unsaved changes."), BorderLayout.PAGE_START);
            add(new JButton(_editor.createSaveAndCloseCurrentTabAction()), BorderLayout.WEST);
            add(new JButton(_editor.createForceCloseCurrentTabAction()), BorderLayout.EAST);
            pack();
        }
    }
    
    protected JPopupMenu _popup;
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected UndoAction _undoAction = new UndoAction();
    protected RedoAction _redoAction = new RedoAction();
    protected SwiftlyEditor _editor;
    protected SwiftlyDocument _document;
    protected HashMap<String,Action> _actions = new HashMap<String,Action>();
    protected boolean _documentChanged;
}

