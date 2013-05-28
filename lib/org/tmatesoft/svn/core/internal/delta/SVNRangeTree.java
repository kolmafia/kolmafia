/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.delta;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;



/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNRangeTree {
    
    private SVNRangeTreeNode myRoot = null;
    
    private SVNRangeTreeNode myFreeTreeNodes;
    private SVNRangeTreeNode myAllocatedTreeNodes;
    private SVNRangeListNode myFreeListNodes;
    
    public static class SVNRangeTreeNode {
        
        public SVNRangeTreeNode(int offset, int limit, int target) {
            this.offset = offset;
            this.limit = limit;
            this.targetOffset = target;
        }
        
        public String toString() {
            String str = offset + ":" + limit + ":" + targetOffset;
            return str;
        }
        
        public int offset;
        public int limit;
        public int targetOffset;
        
        public SVNRangeTreeNode left;
        public SVNRangeTreeNode right;
        public SVNRangeTreeNode prev;
        public SVNRangeTreeNode next;
        
        public SVNRangeTreeNode nextFree;
    }
    
    private SVNRangeTreeNode allocateTreeNode(int offset, int limit, int target) {
        if (myFreeTreeNodes == null) {
            SVNRangeTreeNode node = new SVNRangeTreeNode(offset, limit, target);
            node.nextFree = myAllocatedTreeNodes;
            myAllocatedTreeNodes = node;
            return node;
        }
        SVNRangeTreeNode node = myFreeTreeNodes;
        myFreeTreeNodes = node.nextFree;
        node.left = node.right = node.next = node.prev = null;
        node.offset = offset;
        node.limit = limit;
        node.targetOffset = target;
        
        // make it head of the allocated list.
        node.nextFree = myAllocatedTreeNodes;
        myAllocatedTreeNodes = node;
        return node;
    }

    private void freeTreeNode(SVNRangeTreeNode node) {
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        node.next = null;
        node.prev = null;
        node.left = null;
        node.right = null;
        
        // remove if from the allocated list, it has to be there.
        if (myAllocatedTreeNodes == node) {
            myAllocatedTreeNodes = myAllocatedTreeNodes.nextFree;
        } else {
            SVNRangeTreeNode allocated = myAllocatedTreeNodes;
            while(allocated.nextFree != node) {
                allocated = allocated.nextFree;
            }
            allocated.nextFree = node.nextFree;
        }
        // make it head of the free nodes list.
        node.nextFree = myFreeTreeNodes;
        myFreeTreeNodes = node;
    }

    private SVNRangeListNode allocateListNode(int kind, int offset, int limit, int target) {
        if (myFreeListNodes == null) {
            return new SVNRangeListNode(kind, offset, limit, target);
        }
        SVNRangeListNode node = myFreeListNodes;
        myFreeListNodes = node.next;
        node.offset = offset;
        node.limit = limit;
        node.targetOffset = target;
        node.kind = kind;
        node.prev = node.next = null;
        node.head = node;
        return node;
    }
    
    public void disposeList(SVNRangeListNode head) {
        SVNRangeListNode n = head;
        while(head.next != null) {
            head = head.next;
        }
        head.next = myFreeListNodes;
        myFreeListNodes = n;
    }
    
    public void dispose() {
        SVNRangeTreeNode node = myFreeTreeNodes;
        if (node == null) {
            myFreeTreeNodes = myAllocatedTreeNodes;
        } else {
            while(node.nextFree != null) {
                node = node.nextFree;
            }
            node.nextFree = myAllocatedTreeNodes;
        }
        myAllocatedTreeNodes = null;
        myRoot = null;
    }

    public static class SVNRangeListNode {
        
        public static int FROM_SOURCE = 0;
        public static int FROM_TARGET = 1;
        
        public SVNRangeListNode(int kind, int offset, int limit, int target) {
            this.kind = kind;
            this.offset = offset;
            this.limit = limit;
            this.targetOffset = target;
            this.head = this;
        }
        
        public SVNRangeListNode append(SVNRangeListNode node) {
            this.next = node;
            node.prev = this;
            node.head = this.head;
            return node;
        }

        public int kind;
        public int offset;
        public int limit;
        public int targetOffset;
        
        public SVNRangeListNode prev;
        public SVNRangeListNode next;
        public SVNRangeListNode head;
    }
    
    public SVNRangeListNode buildRangeList(int offset, int limit) throws SVNException {
        SVNRangeListNode tail = null;
        SVNRangeTreeNode node = myRoot;
        
        while(offset < limit) {
            if (node == null) {
                return appendToRangeList(SVNRangeListNode.FROM_SOURCE, offset, limit, 0, tail);
            }

            if (offset < node.offset) {
                if (limit <= node.offset) {
                    return appendToRangeList(SVNRangeListNode.FROM_SOURCE, offset, limit, 0, tail);
                }
                tail = appendToRangeList(SVNRangeListNode.FROM_SOURCE, offset, node.offset, 0, tail);
                offset = node.offset;
            } else {
                if (offset >= node.limit) {
                    node = node.next;
                } else {
                    int targetOffset = offset - node.offset + node.targetOffset;
                    if (limit <= node.limit) {
                        return appendToRangeList(SVNRangeListNode.FROM_TARGET, offset, limit, targetOffset, tail);
                    }
                    tail = appendToRangeList(SVNRangeListNode.FROM_TARGET, offset, node.limit, targetOffset, tail);
                    offset = node.limit;
                    node = node.next;
                }
            }
        }
        SVNErrorManager.assertionFailure(false, null, SVNLogType.DEFAULT);
        return tail;
    }

    private SVNRangeListNode appendToRangeList(int kind, int offset, int limit, int tOffset, SVNRangeListNode tail) {
        if (tail == null) {
            return allocateListNode(kind, offset, limit, tOffset);
        }
        return tail.append(allocateListNode(kind, offset, limit, tOffset));
    }
    
    private SVNRangeTreeNode myScratchNode = new SVNRangeTreeNode(0,0,0); 
    
    public void splay(int offset) throws SVNException {
        if (myRoot == null) {
            return;
        }
        SVNRangeTreeNode root = myRoot;
        SVNRangeTreeNode scratch = myScratchNode;
        scratch.left = scratch.right = null;
        SVNRangeTreeNode left = scratch;
        SVNRangeTreeNode right = scratch;

        while(true) {
            if (offset < root.offset) {
                if (root.left != null && offset < root.left.offset) {
                    SVNRangeTreeNode node = root.left;
                    root.left = node.right;
                    node.right = root;
                    root = node;
                }
                if (root.left == null) {
                    break;
                }
                right.left = root;
                right = root;
                root = root.left;
            } else if (offset > root.offset) {
                if (root.right != null && offset > root.right.offset) {
                    SVNRangeTreeNode node = root.right;
                    root.right = node.left;
                    node.left = root;
                    root = node;
                }
                if (root.right == null) {
                    break;
                }
                left.right = root;
                left = root;
                root = root.right;
            } else {
                break;
            }
        }
        left.right = root.left;
        right.left = root.right;
        root.left = scratch.right;
        root.right = scratch.left;
        
        if (offset < root.offset && root.left != null) {
            if (root.left.right == null) {
                SVNRangeTreeNode node = root.left;
                root.left = node.right;
                SVNErrorManager.assertionFailure(root.left == null, null, SVNLogType.DEFAULT);
                node.right = root;
                root = node;
            } else {
                SVNRangeTreeNode nodePointer = root.left;
                SVNRangeTreeNode nodeOwner = root;
                boolean isLeft = true;
                while(nodePointer.right != null) {
                    nodeOwner = nodePointer;
                    nodePointer = nodePointer.right;
                    isLeft = false;
                }
                right = root;
                left = root.left;
                root = nodePointer;
                if (isLeft) {
                    nodeOwner.left = root.left;
                } else {
                    nodeOwner.right = root.left;
                }
                SVNErrorManager.assertionFailure(root.right == null, null, SVNLogType.DEFAULT);
                right.left = root.right;
                root.left = left;
                root.right = right;
            }
        }
        myRoot = root;
        SVNErrorManager.assertionFailure((offset >= root.offset) || (root.left == null && root.prev == null), null, SVNLogType.DEFAULT);
    }
    
    public void insert(int offset, int limit, int targetOffset) throws SVNException {
        if (myRoot == null) {
            myRoot = allocateTreeNode(offset, limit, targetOffset);
            return;
        }
        if (offset == myRoot.offset && limit > myRoot.limit) {
            myRoot.limit = limit;
            myRoot.targetOffset = targetOffset;
            cleanTree(limit);
        } else if (offset > myRoot.offset && limit > myRoot.limit) {
            boolean haveToInsertRange = myRoot.next == null ||
                    myRoot.limit < myRoot.next.offset ||
                    limit > myRoot.next.limit;
            if (haveToInsertRange) {
                if (myRoot.prev != null && myRoot.prev.limit > offset) {
                    myRoot.offset = offset;
                    myRoot.limit = limit;
                    myRoot.targetOffset = targetOffset;
                } else {
                    SVNRangeTreeNode node = allocateTreeNode(offset, limit, targetOffset);
                    node.next = myRoot.next;
                    if (node.next != null) {
                        node.next.prev = node;
                    }
                    myRoot.next = node;
                    node.prev = myRoot;

                    node.right = myRoot.right;
                    myRoot.right = null;
                    node.left = myRoot;
                    myRoot = node;
                }
                cleanTree(limit);
            }   
        } else if (offset < myRoot.offset) {
            SVNErrorManager.assertionFailure(myRoot.left == null, null, SVNLogType.DEFAULT);
            SVNRangeTreeNode node = allocateTreeNode(offset, limit, targetOffset);
            
            node.left = node.prev = null;
            node.right = node.next = myRoot;
            myRoot = node.next.prev = node;
            cleanTree(limit);
        }
    }
    
    private void cleanTree(int limit) {
        int topOffset = limit + 1;
        SVNRangeTreeNode rightNode = myRoot.right;
        SVNRangeTreeNode owner = myRoot;
        if (rightNode == null) {
            return;
        }
        boolean left = false;
        while(rightNode != null) {
            int offset = rightNode.right != null && rightNode.right.offset < topOffset ? rightNode.offset : topOffset;
            if (rightNode.limit <= limit || (rightNode.offset < limit && offset < limit)) {
                SVNRangeTreeNode rightRight = rightNode.right;
                rightNode.right = null;
                if (left) {
                    owner.left = rightRight;
                } else {
                    owner.right = rightRight;
                }
                deleteSubtree(rightNode);                
                rightNode = left ? owner.left : owner.right;
            } else {
                topOffset = rightNode.offset;
                owner = rightNode;
                rightNode = rightNode.left; 
                left = true;
            }
        }
    }
    
    private void deleteSubtree(SVNRangeTreeNode node) {
        if (node != null) {
            deleteSubtree(node.left);
            deleteSubtree(node.right);
            freeTreeNode(node);
        }
    }
}
