//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.swiftly.data.PathElement;

import com.threerings.msoy.swiftly.client.ProjectPanel;

/**
 * Present the contents of a Swiftly project as a tree model.
 */
public class ProjectTreeModel extends DefaultTreeModel
    implements SetListener
{
    public ProjectTreeModel (ProjectRoomObject roomObj, ProjectPanel panel)
    {
        super(null);

        _roomObj = roomObj;
        _roomObj.addListener(this);
        _projectPanel = panel;

        // Raise all path elements from the dead, re-binding transient
        // instance variables.
        for (PathElement element : _roomObj.pathElements) {
            element.lazarus(_roomObj.pathElements);
        }

        // construct our tree model based on the room object's contents
        PathElement root = roomObj.getRootElement();
        if (root == null) {
            return; // ack! log a warning
        }

        PathElementTreeNode node = new PathElementTreeNode(root);
        setRoot(node);
        addChildren(node, root);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            final PathElement element = (PathElement)event.getEntry();

            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);

            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == element.getParent().elementId;
                }
                public void update (PathElementTreeNode node) {
                    insertNodeInto(new PathElementTreeNode(element), node, node.getChildCount());
                }
            });
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            final PathElement element = (PathElement)event.getEntry();

            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);

            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == element.elementId;
                }
                public void update (PathElementTreeNode node) {
                    node.setUserObject(element);
                    nodeChanged(node);
                }
            });
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.PATH_ELEMENTS)) {
            final int elementId = (Integer)event.getKey();
            updateNodes(new NodeOp() {
                public boolean isMatch (PathElementTreeNode node) {
                    return node.getElement().elementId == elementId;
                }
                public void update (PathElementTreeNode node) {
                    removeNodeFromParent(node);
                }
            });
        }
    }

    @Override // from DefaultTreeModel
    public void valueForPathChanged (TreePath path, Object newValue)
    {
        PathElementTreeNode node = (PathElementTreeNode)path.getLastPathComponent();
        PathElement element = (PathElement)node.getUserObject();
        String newName = (String)newValue;
        _projectPanel.renamePathElement(element, newName, path);
    }

    // TODO: this might not need to stick around
    public void updateNodeName (PathElement element, TreePath path)
    {
        super.valueForPathChanged(path, element);
    }

    protected void addChildren (PathElementTreeNode node, PathElement parent)
    {
        for (PathElement element : _roomObj.pathElements) {
            if (element.getParent() != null && element.getParent() == parent) {
                PathElementTreeNode child = new PathElementTreeNode(element);
                node.add(child);
                if (element.getType() == PathElement.Type.DIRECTORY) {
                    addChildren(child, element);
                }
            }
        }
    }

    protected void updateNodes (NodeOp op)
    {
        Enumeration iter = ((DefaultMutableTreeNode)root).breadthFirstEnumeration();
        while (iter.hasMoreElements()) {
            PathElementTreeNode node = (PathElementTreeNode)iter.nextElement();
            if (op.isMatch(node)) {
                op.update(node);
            }
        }
    }

    protected interface NodeOp
    {
        public boolean isMatch (PathElementTreeNode node);
        public void update (PathElementTreeNode node);
    }

    protected ProjectRoomObject _roomObj;
    protected ProjectPanel _projectPanel;
}
