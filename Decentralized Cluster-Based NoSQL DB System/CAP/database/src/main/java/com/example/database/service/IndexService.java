package com.example.database.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IndexService {
    private final Map<String, Map<Object, List<String>>> index = new HashMap<>();
    public void addToIndex(String property, Object value, String id) {
        index.computeIfAbsent(property, k -> new HashMap<>())
                .computeIfAbsent(value, k -> new ArrayList<>())
                .add(id);
    }

    public void removeFromIndex(String property, Object value, String id) {
        Map<Object, List<String>> propertyIndex = index.get(property);
        if (propertyIndex != null) {
            List<String> ids = propertyIndex.get(value);
            if (ids != null) {
                ids.remove(id);
            }
        }
    }

    public List<String> getIdsByProperty(String property, Object value) {
        return index.getOrDefault(property, Collections.emptyMap())
                .getOrDefault(value, Collections.emptyList());
    }

    public void updateIndex(String property, Object oldValue, Object newValue, String id) {
        removeFromIndex(property, oldValue, id);
        addToIndex(property, newValue, id);
    }

    public void removeIdFromAllIndexes(String id) {
        for (Map.Entry<String, Map<Object, List<String>>> entry : index.entrySet()) {
            String property = entry.getKey();
            for (Map.Entry<Object, List<String>> subEntry : entry.getValue().entrySet()) {
                Object value = subEntry.getKey();
                removeFromIndex(property, value, id);
            }
        }
    }
}

/*package com.example.database.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.logging.Logger;


@Component
public class IndexService {

    private static final int T = 2; // Degree of the B-tree
    private static final Logger LOGGER = Logger.getLogger(IndexService.class.getName());  // Added Logger

    private class BTreeNode {
        int n;
        List<KeyValue> keys = new ArrayList<>(2 * T - 1);
        List<BTreeNode> children = new ArrayList<>(2 * T);
        boolean leaf = true;

        BTreeNode() {
            for (int i = 0; i < 2 * T; i++) {
                children.add(null);
            }
        }

        int find(Object key) {
            for (int i = 0; i < keys.size(); i++) {
                KeyValue kv = keys.get(i);
                if (kv != null && kv.key.equals(key)) return i;
            }
            return -1;
        }
    }

    private class KeyValue {
        Object key;
        List<String> values;

        KeyValue(Object key, String value) {
            this.key = key;
            this.values = new ArrayList<>();
            this.values.add(value);
        }
    }

    private final BTreeNode root = new BTreeNode();

    // To store B-trees for different properties
    private final Map<String, BTreeNode> propertyRoots = new HashMap<>();

    public void addToIndex(String property, Object key, String value) {
        BTreeNode rootNode = propertyRoots.computeIfAbsent(property, k -> new BTreeNode());
        addToTree(rootNode, key, value);
    }

    private void addToTree(BTreeNode root, Object key, String value) {
        BTreeNode r = root;
        if (r.n == 2 * T - 1) {
            BTreeNode s = new BTreeNode();
            root = s;
            s.leaf = false;
            s.n = 0;
            s.children.set(0, r);
            splitChild(s, 0);
            insertNonFull(s, key, value);
        } else {
            insertNonFull(r, key, value);
        }
    }

    private void splitChild(BTreeNode x, int i) {
        BTreeNode z = new BTreeNode();
        BTreeNode y = x.children.get(i);
        z.leaf = y.leaf;
        z.n = T - 1;

        for (int j = 0; j < T - 1; j++) {
            z.keys.add(y.keys.get(j + T));
        }
        if (!y.leaf) {
            for (int j = 0; j < T; j++) {
                z.children.add(y.children.get(j + T));
            }
        }

        y.n = T - 1;
        x.children.add(i + 1, z);
        x.keys.add(i, y.keys.get(T - 1));
        x.n = x.n + 1;
    }

    private void insertNonFull(BTreeNode x, Object k, String v) {
        int i = x.n - 1;

        LOGGER.info("Inserting key: " + k + " with value: " + v + " into node with n = " + x.n);

        if (x.leaf) {

            x.keys.add(null); // Ensure there's space to add

            LOGGER.info("Before shifting keys. Current keys: " + x.keys);
            if (x.n == 0) {
                x.keys.set(0, new KeyValue(k, v));
                x.n = x.n + 1;
                LOGGER.info("Inserted the first key. Current keys: " + x.keys);
                return;
            }
            while (i >= 0 && i < x.keys.size() && x.keys.get(i) != null && ((Comparable) k).compareTo(x.keys.get(i).key) < 0) {
                if(i + 1 < x.keys.size() && x.keys.get(i + 1) == null) {
                    x.keys.set(i + 1, x.keys.get(i));
                    x.keys.set(i, null);
                } else {
                    x.keys.add(i + 1, x.keys.get(i));
                }
                i--;
            }
            LOGGER.info("After shifting keys. Current keys: " + x.keys);

            int index = x.find(k);
            if (index != -1) {
                KeyValue kv = x.keys.get(index);
                if(kv == null) {
                    LOGGER.severe("KeyValue retrieved from index " + index + " is null.");
                    return;
                }
                kv.values.add(v);
            } else {
                LOGGER.info("Inserting new KeyValue at index: " + (i + 1));
                x.keys.set(i + 1, new KeyValue(k, v));
                x.n = x.n + 1;
            }

            x.n = x.n + 1;
        } else {
            while (i >= 0 && ((Comparable) k).compareTo(x.keys.get(i).key) < 0) {
                i--;
            }
            i++;
            if (x.children.get(i).n == 2 * T - 1) {
                splitChild(x, i);
                if (((Comparable) k).compareTo(x.keys.get(i).key) > 0) {
                    i++;
                }
            }
            insertNonFull(x.children.get(i), k, v);
        }
    }

    public List<String> search(Object k) {
        return search(root, k);
    }

    private List<String> search(BTreeNode x, Object k) {
        int i = 0;
        while (i < x.n && ((Comparable) k).compareTo(x.keys.get(i).key) > 0) {
            i++;
        }
        if (i < x.n && ((Comparable) k).compareTo(x.keys.get(i).key) == 0) {
            return x.keys.get(i).values;
        } else if (x.leaf) {
            return null;
        } else {
            return search(x.children.get(i), k);
        }
    }

    public List<String> getIdsByProperty(String property, Object key) {
        BTreeNode rootNode = propertyRoots.get(property);
        if (rootNode == null) {
            return Collections.emptyList();
        }
        return search(rootNode, key);
    }


    // Note: Removal is a bit more complex; this is a basic version.
    public void removeFromIndex(String property, Object key, String id) {
        BTreeNode rootNode = propertyRoots.get(property);
        if (rootNode != null) {
            remove(rootNode, key, id);
        }
    }
    private void remove(BTreeNode x, Object k, String id) {
        int index = x.find(k);
        if (index != -1) {
            KeyValue kv = x.keys.get(index);
            kv.values.remove(id);

            if (kv.values.isEmpty()) {
                x.keys.remove(index);
                x.n = x.n - 1;
                if (x.n < T - 1) {
                    handleUnderflow(x, index);
                }
            }
        }
    }

    private void handleUnderflow(BTreeNode x, int index) {
        // Borrow from left sibling if possible
        if (index > 0 && x.children.get(index - 1).n >= T) {
            BTreeNode leftSibling = x.children.get(index - 1);
            KeyValue borrowedKey = leftSibling.keys.remove(leftSibling.n - 1);
            leftSibling.n--;

            x.keys.add(index - 1, borrowedKey);
            x.n++;
            if (!leftSibling.leaf) {
                BTreeNode lastChildFromLeft = leftSibling.children.remove(leftSibling.n);
                x.children.add(index, lastChildFromLeft);
            }
        }
        // Borrow from right sibling if possible
        else if (index < x.n && x.children.get(index + 1).n >= T) {
            BTreeNode rightSibling = x.children.get(index + 1);
            KeyValue borrowedKey = rightSibling.keys.remove(0);
            rightSibling.n--;

            x.keys.add(index, borrowedKey);
            x.n++;
            if (!rightSibling.leaf) {
                BTreeNode firstChildFromRight = rightSibling.children.remove(0);
                x.children.add(index + 1, firstChildFromRight);
            }
        }
        // Merge nodes if borrowing isn't possible
        else {
            // Determine whether to merge with left or right sibling
            BTreeNode leftSibling = index > 0 ? x.children.get(index - 1) : null;
            BTreeNode rightSibling = index < x.n ? x.children.get(index + 1) : null;

            // If left sibling exists, it becomes the main node after merge
            if (leftSibling != null) {
                leftSibling.keys.add(x.keys.get(index - 1));
                leftSibling.keys.addAll(x.children.get(index).keys);
                if (!leftSibling.leaf) {
                    leftSibling.children.addAll(x.children.get(index).children);
                }
                x.children.remove(index);
                x.keys.remove(index - 1);
                leftSibling.n += x.children.get(index).n + 1;
            }
            // If right sibling exists, the current node becomes the main node after merge
            else if (rightSibling != null) {
                x.children.get(index).keys.add(x.keys.get(index));
                x.children.get(index).keys.addAll(rightSibling.keys);
                if (!x.children.get(index).leaf) {
                    x.children.get(index).children.addAll(rightSibling.children);
                }
                x.children.remove(index + 1);
                x.keys.remove(index);
                x.children.get(index).n += rightSibling.n + 1;
            }
            x.n--;
        }
    }

    public void updateIndex(String property, Object oldValue, Object newValue, String id) {
        removeFromIndex(property, oldValue, id);
        addToIndex(property, newValue, id);
    }

    public void removeIdFromAllIndexes(String id) {
        for (BTreeNode rootNode : propertyRoots.values()) {
            removeFromTree(rootNode, id);
        }
    }

    private void removeFromTree(BTreeNode x, String id) {
        for (KeyValue kv : x.keys) {
            kv.values.remove(id);
        }
        if (!x.leaf) {
            for (BTreeNode child : x.children) {
                if (child != null) {
                    removeFromTree(child, id);
                }
            }
        }
    }
}
 */
