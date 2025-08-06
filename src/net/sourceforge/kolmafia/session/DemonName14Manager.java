package net.sourceforge.kolmafia.session;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Demon Name Solver - finds all valid demon names from syllable segments that satisfy specific
 * constraints (exactly 9 syllables, using all segment clues).
 */
public class DemonName14Manager {

  // List of all possible syllables
  private static final Set<String> SYLLABLES =
      Set.of(
          "Arg", "Bal", "Ball", "Bar", "Bob", "But", "Cak", "Cal", "Call", "Car", "Col", "Cor",
          "Cul", "Cur", "Cut", "Dak", "Dar", "Dor", "Gar", "Ger", "Gra", "Gur", "Har", "Hur", "Hut",
          "Kar", "Kil", "Kir", "Kru", "Kul", "Kur", "Lag", "Lar", "Mor", "Nar", "Nix", "Nut", "Pha",
          "Rog", "Yer");

  /** Unified graph structure that can represent both segment graphs and composed graphs */
  private record Graph(Map<String, GraphNode> nodes, Set<GraphEdge> edges, List<String> segments) {
    private Graph(Map<String, GraphNode> nodes, Set<GraphEdge> edges, List<String> segments) {
      this.nodes = new HashMap<>(nodes);
      this.edges = new HashSet<>(edges);
      this.segments = new ArrayList<>(segments);
    }
  }

  /**
   * Node in the graph. This represents a syllable and contains a set of segments that could
   * possibly reference it. e.g. the segment "rgB" would produce the nodes "Arg" and "Bal"
   *
   * @param segments segments that reference this syllable
   */
  private record GraphNode(String syllable, Set<String> segments) {
    private GraphNode(String syllable, Set<String> segments) {
      this.syllable = syllable;
      this.segments = new HashSet<>(segments);
    }
  }

  /**
   * Edge in the graph. This represents one syllable leading to another (and is thus directed). e.g.
   * the segment "rgB" would produce an edge from "Arg" to "Bal"
   *
   * @param segments segments that create this edge
   */
  private record GraphEdge(String from, String to, Set<String> segments) {
    private GraphEdge(String from, String to, Set<String> segments) {
      this.from = from;
      this.to = to;
      this.segments = new HashSet<>(segments);
    }
  }

  /** Path during solving */
  private record SolverPath(List<String> syllables, Set<String> usedSegments) {
    private SolverPath(List<String> syllables, Set<String> usedSegments) {
      this.syllables = new ArrayList<>(syllables);
      this.usedSegments = new HashSet<>(usedSegments);
    }
  }

  /** Result of solving */
  private record SolverResult(String demonName, List<String> path, Set<String> usedSegments) {
    private SolverResult(String demonName, List<String> path, Set<String> usedSegments) {
      this.demonName = demonName;
      this.path = new ArrayList<>(path);
      this.usedSegments = new HashSet<>(usedSegments);
    }

    @Override
    public int hashCode() {
      return this.demonName.hashCode();
    }
  }

  /** Creates a mini directed graph for a segment showing all possible syllable transitions */
  private static Graph createSubgraph(String segment) {
    Map<String, GraphNode> nodes = new HashMap<>();
    Set<GraphEdge> edges = new HashSet<>();

    // Check all possible syllable-to-syllable transitions
    for (String from : SYLLABLES) {
      for (String to : SYLLABLES) {
        // Check if we can form the segment by taking suffix of fromSyllable + prefix of toSyllable
        for (int splitPos = 1; splitPos < 3; splitPos++) {
          if (splitPos >= segment.length()) continue;

          String fromPart = segment.substring(0, splitPos);
          String toPart = segment.substring(splitPos);

          // Check if fromSyllable ends with fromPart and toSyllable starts with toPart
          if (from.endsWith(fromPart) && to.startsWith(toPart)) {
            nodes.putIfAbsent(from, new GraphNode(from, Set.of(segment)));
            nodes.putIfAbsent(to, new GraphNode(to, Set.of(segment)));
            edges.add(new GraphEdge(from, to, Set.of(segment)));
          }
        }
      }
    }

    return new Graph(nodes, edges, List.of(segment));
  }

  /** Creates mini graphs for all segments */
  private static List<Graph> createSubgraphs(Set<String> segments) {
    return segments.stream().map(DemonName14Manager::createSubgraph).collect(Collectors.toList());
  }

  /**
   * Composes individual segment graphs into one unified directed graph with metadata tracking which
   * segments contributed to each node/edge
   */
  private static Graph composeSubgraphs(List<Graph> segmentGraphs) {
    Map<String, GraphNode> nodes = new HashMap<>();
    Map<String, GraphEdge> edgeMap = new HashMap<>();

    // Helper function to add or update a node
    BiConsumer<String, String> addNode =
        (syllable, segment) ->
            nodes.compute(
                syllable,
                (key, node) -> {
                  if (node == null) node = new GraphNode(syllable, new HashSet<>());
                  node.segments.add(segment);
                  return node;
                });

    // Helper function to add or update an edge
    TriConsumer<String, String, String> addEdge =
        (from, to, segment) ->
            edgeMap.compute(
                from + "->" + to,
                (key, edge) -> {
                  if (edge == null) edge = new GraphEdge(from, to, new HashSet<>());
                  edge.segments.add(segment);
                  return edge;
                });

    // Process each segment graph
    for (Graph segmentGraph : segmentGraphs) {
      // For each segment in the graph (should be a single-element list)
      for (String segment : segmentGraph.segments) {
        // Add all edges from the segment graph
        for (GraphEdge edge : segmentGraph.edges) {
          addNode.accept(edge.from, segment);
          addNode.accept(edge.to, segment);
          addEdge.accept(edge.from, edge.to, segment);
        }
      }
    }

    return new Graph(
        nodes,
        new HashSet<>(edgeMap.values()),
        segmentGraphs.stream().flatMap(sg -> sg.segments.stream()).collect(Collectors.toList()));
  }

  /**
   * Finds all paths in a composed directed graph that satisfy the demon name constraints: - Exactly
   * 9 nodes (syllables) are visited - Edges from every subgraph (segment) are used at least once
   */
  private static Set<String> solveGraph(Graph graph) {
    Set<SolverResult> results = new HashSet<>();
    Set<String> allSegments = new HashSet<>(graph.segments);

    // Try starting from each node
    for (Map.Entry<String, GraphNode> entry : graph.nodes.entrySet()) {
      String startSyllable = entry.getKey();
      SolverPath initialPath = new SolverPath(List.of(startSyllable), new HashSet<>());

      dfs(graph, startSyllable, initialPath, allSegments, results);
    }

    return results.stream().map(r -> r.demonName).collect(Collectors.toSet());
  }

  /** Depth-first search to find valid paths */
  private static void dfs(
      Graph graph,
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
      results.add(new SolverResult(demonName, currentPath.syllables, currentPath.usedSegments));
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

      Set<String> newUsedSegments = new HashSet<>(currentPath.usedSegments);
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
    var segmentGraphs = createSubgraphs(segments);
    var composedGraph = composeSubgraphs(segmentGraphs);
    return solveGraph(composedGraph);
  }

  // Functional interface for three-parameter consumer (since Java doesn't have one built-in)
  @FunctionalInterface
  private interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
  }
}
