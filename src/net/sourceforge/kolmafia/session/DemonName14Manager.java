package net.sourceforge.kolmafia.session;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Demon Name Solver - finds all valid demon names from syllable segments that satisfy specific
 * constraints (exactly 9 syllables, using all segments).
 */
public class DemonName14Manager {

  // List of all possible syllables
  private static final List<String> SYLLABLES =
      Arrays.asList(
          "Arg", "Bal", "Ball", "Bar", "Bob", "But", "Cak", "Cal", "Call", "Car", "Col", "Cor",
          "Cul", "Cur", "Cut", "Dak", "Dar", "Dor", "Gar", "Ger", "Gra", "Gur", "Har", "Hur", "Hut",
          "Kar", "Kil", "Kir", "Kru", "Kul", "Kur", "Lag", "Lar", "Mor", "Nar", "Nix", "Nut", "Pha",
          "Rog", "Yer");

  /**
   * Represents a syllable transition in a segment graph
   *
   * @param from null if segment starts a syllable
   * @param to null if segment ends a syllable
   */
  private record SyllableTransition(String from, String to) {}

  /** Mini graph representing possible transitions for a segment */
  private record SegmentGraph(String segment, List<SyllableTransition> transitions) {
    private SegmentGraph(String segment, List<SyllableTransition> transitions) {
      this.segment = segment;
      this.transitions = new ArrayList<>(transitions);
    }
  }

  /**
   * Node in the composed graph
   *
   * @param segments segments that reference this syllable
   */
  private record GraphNode(String syllable, List<String> segments) {
    private GraphNode(String syllable, List<String> segments) {
      this.syllable = syllable;
      this.segments = new ArrayList<>(segments);
    }
  }

  /**
   * Edge in the composed graph
   *
   * @param segments segments that create this transition
   */
  private record GraphEdge(String from, String to, List<String> segments) {
    private GraphEdge(String from, String to, List<String> segments) {
      this.from = from;
      this.to = to;
      this.segments = new ArrayList<>(segments);
    }
  }

  /** The unified composed graph */
  private record ComposedGraph(
      Map<String, GraphNode> nodes, List<GraphEdge> edges, Set<String> segments) {
    private ComposedGraph(
        Map<String, GraphNode> nodes, List<GraphEdge> edges, Set<String> segments) {
      this.nodes = new HashMap<>(nodes);
      this.edges = new ArrayList<>(edges);
      this.segments = new HashSet<>(segments);
    }
  }

  /** Path during solving */
  private record SolverPath(List<String> syllables, List<String> usedSegments) {
    private SolverPath(List<String> syllables, List<String> usedSegments) {
      this.syllables = new ArrayList<>(syllables);
      this.usedSegments = new ArrayList<>(usedSegments);
    }
  }

  /** Result of solving */
  private record SolverResult(String demonName, List<String> path, List<String> usedSegments) {
    private SolverResult(String demonName, List<String> path, List<String> usedSegments) {
      this.demonName = demonName;
      this.path = new ArrayList<>(path);
      this.usedSegments = new ArrayList<>(usedSegments);
    }

    @Override
    public int hashCode() {
      return this.demonName.hashCode();
    }
  }

  /** Creates a mini directed graph for a segment showing all possible syllable transitions */
  private static SegmentGraph createSegmentGraph(String segment) {
    List<SyllableTransition> transitions = new ArrayList<>();
    Set<String> seenTransitions = new HashSet<>();

    // Helper function to add unique transitions
    BiConsumer<String, String> addTransition =
        (from, to) -> {
          String key = (from != null ? from : "null") + "->" + (to != null ? to : "null");
          if (!seenTransitions.contains(key)) {
            seenTransitions.add(key);
            transitions.add(new SyllableTransition(from, to));
          }
        };

    // Check if segment is a complete 3-letter syllable
    if (SYLLABLES.contains(segment)) {
      addTransition.accept(null, null); // Complete syllable, no transition
    }

    // Check if segment could be part of a 4-letter syllable
    for (String syllable : SYLLABLES) {
      if (syllable.length() == 4 && syllable.startsWith(segment)) {
        addTransition.accept(null, null); // Partial syllable, no transition
      }
    }

    // Check all possible syllable-to-syllable transitions
    for (String fromSyllable : SYLLABLES) {
      for (String toSyllable : SYLLABLES) {
        // Check if we can form the segment by taking suffix of fromSyllable + prefix of toSyllable
        for (int splitPos = 1; splitPos < 3; splitPos++) {
          if (splitPos >= segment.length()) continue;

          String fromPart = segment.substring(0, splitPos);
          String toPart = segment.substring(splitPos);

          // Check if fromSyllable ends with fromPart and toSyllable starts with toPart
          if (fromSyllable.endsWith(fromPart) && toSyllable.startsWith(toPart)) {
            addTransition.accept(fromSyllable, toSyllable);
          }
        }
      }
    }

    return new SegmentGraph(segment, transitions);
  }

  /** Creates mini graphs for all segments */
  private static List<SegmentGraph> createSegmentGraphs(Set<String> segments) {
    return segments.stream()
        .map(DemonName14Manager::createSegmentGraph)
        .collect(Collectors.toList());
  }

  /**
   * Composes individual segment graphs into one unified directed graph with metadata tracking which
   * segments contributed to each node/edge
   */
  private static ComposedGraph composeSegmentGraphs(List<SegmentGraph> segmentGraphs) {
    Map<String, GraphNode> nodes = new HashMap<>();
    Map<String, GraphEdge> edgeMap = new HashMap<>();

    // Helper function to add or update a node
    BiConsumer<String, String> addNode =
        (syllable, segment) -> {
          if (nodes.containsKey(syllable)) {
            GraphNode node = nodes.get(syllable);
            if (!node.segments.contains(segment)) {
              node.segments.add(segment);
            }
          } else {
            nodes.put(syllable, new GraphNode(syllable, List.of(segment)));
          }
        };

    // Helper function to add or update an edge
    TriConsumer<String, String, String> addEdge =
        (from, to, segment) -> {
          String edgeKey = from + "->" + to;
          if (edgeMap.containsKey(edgeKey)) {
            GraphEdge edge = edgeMap.get(edgeKey);
            if (!edge.segments.contains(segment)) {
              edge.segments.add(segment);
            }
          } else {
            edgeMap.put(edgeKey, new GraphEdge(from, to, List.of(segment)));
          }
        };

    // Process each segment graph
    for (SegmentGraph segmentGraph : segmentGraphs) {
      String segment = segmentGraph.segment;
      for (SyllableTransition transition : segmentGraph.transitions) {
        if (transition.from == null && transition.to == null) {
          // This segment represents a complete or partial syllable
          // Check for complete 3-letter syllables
          if (SYLLABLES.contains(segment)) {
            addNode.accept(segment, segment);
          }

          // Check for partial 4-letter syllables
          for (String syllable : SYLLABLES) {
            if (syllable.length() == 4 && syllable.startsWith(segment)) {
              addNode.accept(syllable, segment);
            }
          }
        } else if (transition.from != null && transition.to != null) {
          // This segment represents a transition between syllables
          addNode.accept(transition.from, segment);
          addNode.accept(transition.to, segment);
          addEdge.accept(transition.from, transition.to, segment);
        }
      }
    }

    return new ComposedGraph(
        nodes,
        new ArrayList<>(edgeMap.values()),
        segmentGraphs.stream().map(sg -> sg.segment).collect(Collectors.toSet()));
  }

  /**
   * Finds all paths in a composed directed graph that satisfy the demon name constraints: - Exactly
   * 9 nodes (syllables) are visited - Edges from every subgraph (segment) are used at least once
   */
  private static Set<String> solveGraph(ComposedGraph composedGraph) {
    Set<SolverResult> results = new HashSet<>();
    Set<String> allSegments = composedGraph.segments;

    // Try starting from each node
    for (Map.Entry<String, GraphNode> entry : composedGraph.nodes.entrySet()) {
      String startSyllable = entry.getKey();
      SolverPath initialPath = new SolverPath(List.of(startSyllable), new ArrayList<>());

      dfs(composedGraph, startSyllable, initialPath, allSegments, results);
    }

    return results.stream().map(r -> r.demonName).collect(Collectors.toSet());
  }

  /** Depth-first search to find valid paths */
  private static void dfs(
      ComposedGraph graph,
      String currentSyllable,
      SolverPath currentPath,
      Set<String> requiredSegments,
      Set<SolverResult> results) {
    // Base case: if we've visited exactly 9 syllables
    if (currentPath.syllables.size() == 9) {
      // Check if we've used edges from all segments. If a segment happens to be a node, count
      // visiting it as using it
      Set<String> allUsed = new HashSet<>(currentPath.usedSegments);
      allUsed.addAll(currentPath.syllables);

      if (!hasAllRequiredSegments(allUsed, requiredSegments)) {
        return;
      }

      String demonName = String.join("", currentPath.syllables);
      List<String> sortedUsedSegments = new ArrayList<>(currentPath.usedSegments);
      Collections.sort(sortedUsedSegments);

      results.add(new SolverResult(demonName, currentPath.syllables, sortedUsedSegments));
      return;
    }

    // Pruning: if we've already visited 9 or more syllables, stop
    if (currentPath.syllables.size() >= 9) return;

    // Find all outgoing edges from current syllable
    List<GraphEdge> outgoingEdges =
        graph.edges.stream().filter(edge -> edge.from.equals(currentSyllable)).toList();

    for (GraphEdge edge : outgoingEdges) {
      String nextSyllable = edge.to;

      List<String> newSyllables = new ArrayList<>(currentPath.syllables);
      newSyllables.add(nextSyllable);

      List<String> newUsedSegments = new ArrayList<>(currentPath.usedSegments);
      newUsedSegments.addAll(edge.segments);

      SolverPath newPath = new SolverPath(newSyllables, newUsedSegments);

      // Continue DFS
      dfs(graph, nextSyllable, newPath, requiredSegments, results);
    }
  }

  /** Check if all required segment indices are present in the used set */
  private static boolean hasAllRequiredSegments(
      Set<String> usedSegments, Set<String> requiredSegments) {
    return usedSegments.containsAll(requiredSegments);
  }

  /** Find all valid demon names from segments */
  public static Set<String> solve(Set<String> segments) {
    var segmentGraphs = createSegmentGraphs(segments);
    var composedGraph = composeSegmentGraphs(segmentGraphs);
    return solveGraph(composedGraph);
  }

  // Functional interface for three-parameter consumer (since Java doesn't have one built-in)
  @FunctionalInterface
  private interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
  }
}
