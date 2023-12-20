package com.kurguzkin.tempotask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>A <tt>Hierarchy</tt> stores an arbitrary <em>forest</em> (an ordered collection of ordered trees)
 * as an array indexed by DFS-order traversal.
 * A node is represented by a unique ID.
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Tree root has depth 0, immediate children have depth 1, their children have depth 2, etc.
 * </p>
 *
 * <p>Depth of the first element is 0. If the depth of a node is D, the depth of the next node in the array can be:</p>
 * <ul>
 *   <li>D + 1 if the next node is a child of this node;</li>
 *   <li>D if the next node is a sibling of this node;</li>
 *   <li>d < D - in this case the next node is not related to this node.</li>
 * </ul>
 *
 * <p>Example:</p>
 * <code>
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths: 0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * the forest can be visualized as follows:
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 * </code>
 * Note that the depth is equal to the number of hyphens for each node.
 * */
interface Hierarchy {
    int size();

    int nodeId(int index);

    int depth(int index);

    default String formatString() {
        return IntStream.range(0, size()).mapToObj(i -> "" + nodeId(i) + ":" + depth(i) ).collect(Collectors.joining(", ", "[", "]"));
    }
}

class Filter {

    protected static final String MSG_INCONSISTENT = "Hierarchy arrays are inconsistent";
    protected static final String MSG_NULL = "Hierarchy or IntPredicate arguments is null";

    /**
     * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
     * */
    static Hierarchy filter(Hierarchy hierarchy, IntPredicate nodeIdPredicate) {
        if (hierarchy == null || nodeIdPredicate == null) {
            throw new IllegalArgumentException(MSG_NULL);
        }
        try {
            var lastId = hierarchy.nodeId(hierarchy.size() - 1);
            var lastDepth = hierarchy.depth(hierarchy.size() - 1);
        } catch (NullPointerException | ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(MSG_INCONSISTENT);
        }

        var failedDepth = 0;
        var failed = false;
        var excludedIdSet = new HashSet<Integer>();
        for (int i = 0; i < hierarchy.size(); i++) {
            var id = hierarchy.nodeId(i);
            var depth = hierarchy.depth(i);
            if (failed && depth > failedDepth) { // marking the whole branch after a failed node
                excludedIdSet.add(id);
                continue;
            }
            if (nodeIdPredicate.test(id)) { // good sibling should reset the situation
                failed = false;
            } else {
                excludedIdSet.add(id);
                failed = true;
                failedDepth = depth;
            }
        }

        // building resulting arrays
        var filteredSize = hierarchy.size() - excludedIdSet.size();
        var filteredIndexes = new int[filteredSize];
        var filteredDepths = new int[filteredSize];

        int k = 0;
        for (int i = 0; i < hierarchy.size(); i++) {
            var id = hierarchy.nodeId(i);
            if (excludedIdSet.contains(id)) {
                continue;
            }
            var depth = hierarchy.depth(i);
            filteredIndexes[k] = id;
            filteredDepths[k] = depth;
            k++;
        }
        return new ArrayBasedHierarchy(filteredIndexes, filteredDepths);
    }
}

class ArrayBasedHierarchy implements Hierarchy {
    private final int[] myNodeIds;
    private final int[] myDepths;

    public ArrayBasedHierarchy(int[] nodeIds, int[] depths) {
        myNodeIds = nodeIds;
        myDepths = depths;
    }

    @Override
    public int size() {
        return myDepths.length;
    }

    @Override
    public int nodeId(int index) {
        return myNodeIds[index];
    }

    @Override
    public int depth(int index) {
        return myDepths[index];
    }
}

public class FilterTest {

    protected static final Hierarchy ORIGINAL_HIERARCHY = new ArrayBasedHierarchy(
            //         x  x     x  x     x  x     x   x
            new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
            new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
    );

    @Test
    @DisplayName("Original filter test should pass")
    void testFilter() {
        var filteredActual = Filter.filter(ORIGINAL_HIERARCHY, nodeId -> nodeId % 3 != 0);
        var filteredExpected = new ArrayBasedHierarchy(
                new int[] {1, 2, 5, 8, 10, 11},
                new int[] {0, 1, 1, 0, 1, 2}
        );

        assertEquals(filteredExpected.formatString(), filteredActual.formatString());
    }

    @Test
    @DisplayName("Null arguments should throw")
    void testFilterNullArgumentsShouldThrow() {
        var ex1 = assertThrows(IllegalArgumentException.class, () -> Filter.filter(null, v -> true));
        assertEquals(Filter.MSG_NULL, ex1.getMessage());
        var nullArgumentsHierarchy = new ArrayBasedHierarchy(null, null);
        var ex2 = assertThrows(IllegalArgumentException.class, () -> Filter.filter(nullArgumentsHierarchy, null));
        assertEquals(Filter.MSG_NULL, ex2.getMessage());
        var ex3 = assertThrows(IllegalArgumentException.class, () -> Filter.filter(nullArgumentsHierarchy, v -> true));
        assertEquals(Filter.MSG_INCONSISTENT, ex3.getMessage());
    }

    @Test
    @DisplayName("Inconsistent arrays should throw")
    void testFilterInconsistentArraysShouldThrow() {
        var differentSizesHierarchy = new ArrayBasedHierarchy(new int[]{0, 1}, new int[]{0, 0, 0});
        var ex = assertThrows(IllegalArgumentException.class, () -> Filter.filter(differentSizesHierarchy, v -> true));
        assertEquals(Filter.MSG_INCONSISTENT, ex.getMessage());
    }

    @Test
    @DisplayName("Always true predicate should not filter anything")
    void testFilterAlwaysTrue() {
        var filteredActual = Filter.filter(ORIGINAL_HIERARCHY, nodeId -> true);
        assertEquals(ORIGINAL_HIERARCHY.formatString(), filteredActual.formatString());
    }

    @Test
    @DisplayName("Always false predicate should filter everything")
    void testFilterAlwaysFalse() {
        var filteredActual = Filter.filter(ORIGINAL_HIERARCHY, nodeId -> false);
        var emptyHierarchy = new ArrayBasedHierarchy(new int[0], new int[0]);
        assertEquals(emptyHierarchy.formatString(), filteredActual.formatString());
    }
}
